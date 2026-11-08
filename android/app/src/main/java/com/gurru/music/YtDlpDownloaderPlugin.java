package com.gurru.music;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Unit;
import kotlin.jvm.functions.Function3;

/**
 * Plugin nativo de Gurru Music para descargar audio de YouTube con yt-dlp
 * empaquetado DENTRO del APK (sin Termux ni servidor externo).
 *
 * Usa youtubedl-android (fork JunkFood02, 0.17.x) + su artefacto ffmpeg:
 *  - YoutubeDL empaqueta CPython y el binario yt-dlp como .so nativos
 *    dentro del APK (jniLibs por ABI) y los extrae en el primer uso.
 *  - FFmpeg es necesario para --extract-audio (convertir a MP3/M4A).
 *  - YoutubeDL.updateYoutubeDL() actualiza yt-dlp en runtime desde los
 *    releases de GitHub, porque YouTube rompe los extractores cada meses.
 *
 * Flujo por canción (la playlist la descarga el frontend canción por
 * canción, llamando a downloadAudio una vez por canción):
 *   1. yt-dlp descarga solo el mejor audio a un archivo temporal privado.
 *   2. El archivo se copia a la biblioteca pública de música del teléfono
 *      (Music/Gurru Music) con MediaStore, para que quede visible en el
 *      explorador de archivos y en otros reproductores.
 *   3. El progreso real (0-100%) se emite con el evento "downloadProgress".
 *
 * Distribución: NO es apta para Google Play (Play Store prohíbe apps
 * descargadoras y la actualización de binarios en runtime). Se distribuye
 * por sideload / GitHub Releases. Uso previsto: contenido propio, con
 * permiso del autor, o con licencia Creative Commons.
 */
@CapacitorPlugin(name = "YtDlpDownloader")
public class YtDlpDownloaderPlugin extends Plugin {

    private static final String TAG = "YtDlpDownloader";

    /** Carpeta pública donde quedan las canciones: Música/Gurru Music. */
    private static final String PUBLIC_MUSIC_DIR = "Gurru Music";

    /** Prefijo del id de proceso de yt-dlp (sirve para cancelar). */
    private static final String PROCESS_PREFIX = "gurru-";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ConcurrentHashMap<String, String> runningProcesses = new ConcurrentHashMap<>();

    private volatile boolean initialized = false;
    private volatile String lastProgressId = null;
    private volatile float lastProgressSent = -1f;
    private volatile long lastProgressSentAt = 0L;

    // ------------------------------------------------------------------
    // Inicialización (extrae Python + yt-dlp + ffmpeg del APK, una vez)
    // ------------------------------------------------------------------

    private synchronized void ensureInitialized() throws YoutubeDLException {
        if (initialized) return;
        Context ctx = getContext();
        // FFmpeg primero: yt-dlp lo necesita para --extract-audio.
        FFmpeg.getInstance().init(ctx);
        YoutubeDL.getInstance().init(ctx);
        initialized = true;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private void runOnUiThread(Runnable r) {
        try {
            if (getActivity() != null) getActivity().runOnUiThread(r);
            else r.run();
        } catch (Exception e) {
            r.run();
        }
    }

    private void emitProgress(final String id, final float progress, final long etaSeconds,
                              final String status, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                JSObject data = new JSObject();
                data.put("id", id);
                data.put("progress", (double) progress);
                data.put("eta", etaSeconds);
                data.put("status", status);
                if (message != null) data.put("message", message);
                notifyListeners("downloadProgress", data);
            }
        });
    }

    /** Nombre de archivo seguro para Android/MediaStore. */
    private static String safeFileName(String base, String ext) {
        if (base == null || base.trim().isEmpty()) base = "Gurru Music";
        String s = base.trim().replaceAll("[\\\\/:*?\"<>|\\n\\r\\t]+", "_").trim();
        if (s.length() > 120) s = s.substring(0, 120).trim();
        if (s.isEmpty()) s = "Gurru Music";
        return s + "." + ext;
    }

    private static String shortError(String err, int max) {
        if (err == null) return "";
        String s = err.replaceAll("\\s+", " ").trim();
        if (s.length() > max) s = s.substring(s.length() - max);
        return s;
    }

    private File tmpDir() {
        File dir = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC), ".gurru-tmp");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /**
     * Copia el archivo descargado a la música pública del teléfono
     * (Music/Gurru Music) y devuelve la ruta final. En Android 10+ usa
     * MediaStore (sin permiso extra); en Android 9 o menos intenta la
     * carpeta pública directa y, si no hay permiso, deja el archivo en la
     * carpeta privada de la app (sigue sonando dentro de Gurru Music).
     */
    private String publishToPublicMusic(File tmpFile, String displayName, String format) throws Exception {
        Context ctx = getContext();
        String mime = "mp3".equalsIgnoreCase(format) ? "audio/mpeg" : "audio/mp4";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, displayName);
            values.put(MediaStore.Audio.Media.MIME_TYPE, mime);
            values.put(MediaStore.Audio.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MUSIC + "/" + PUBLIC_MUSIC_DIR);
            values.put(MediaStore.Audio.Media.IS_PENDING, 1);
            ContentResolver resolver = ctx.getContentResolver();
            Uri uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("MediaStore no aceptó el archivo");
            try {
                try (InputStream in = new FileInputStream(tmpFile);
                     OutputStream out = resolver.openOutputStream(uri)) {
                    if (out == null) throw new Exception("No se pudo escribir en MediaStore");
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                }
                ContentValues done = new ContentValues();
                done.put(MediaStore.Audio.Media.IS_PENDING, 0);
                resolver.update(uri, done, null, null);
            } catch (Exception e) {
                try { resolver.delete(uri, null, null); } catch (Exception ignored) {}
                throw e;
            }
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    + "/" + PUBLIC_MUSIC_DIR + "/" + displayName;
        }

        // Android 9 o menos: carpeta pública directa (permiso WRITE_EXTERNAL_STORAGE).
        File publicDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                PUBLIC_MUSIC_DIR);
        if (!publicDir.exists()) publicDir.mkdirs();
        File dest = new File(publicDir, displayName);
        try {
            try (InputStream in = new FileInputStream(tmpFile);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            return dest.getAbsolutePath();
        } catch (Exception e) {
            // Sin permiso de escritura: queda en la carpeta privada de la app.
            File privateDir = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                    PUBLIC_MUSIC_DIR);
            if (!privateDir.exists()) privateDir.mkdirs();
            File fallback = new File(privateDir, displayName);
            try (InputStream in = new FileInputStream(tmpFile);
                 OutputStream out = new FileOutputStream(fallback)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            return fallback.getAbsolutePath();
        }
    }

    // ------------------------------------------------------------------
    // Métodos expuestos a JavaScript
    // ------------------------------------------------------------------

    /**
     * ¿Está disponible la descarga nativa? Inicializa yt-dlp si hace falta
     * (la primera vez tarda unos segundos: extrae Python + yt-dlp del APK).
     * Resuelve: { available, version, versionName }.
     */
    @PluginMethod
    public void isAvailable(final PluginCall call) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureInitialized();
                    final String version = YoutubeDL.getInstance().version(getContext());
                    final String versionName = YoutubeDL.getInstance().versionName(getContext());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("available", true);
                            ret.put("version", version);
                            ret.put("versionName", versionName);
                            call.resolve(ret);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            call.reject("yt-dlp no se pudo iniciar: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    /** Versión de yt-dlp empaquetada (sin inicializar si no hace falta). */
    @PluginMethod
    public void getVersion(final PluginCall call) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureInitialized();
                    final String version = YoutubeDL.getInstance().version(getContext());
                    final String versionName = YoutubeDL.getInstance().versionName(getContext());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("version", version);
                            ret.put("versionName", versionName);
                            call.resolve(ret);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            call.reject("No se pudo leer la versión de yt-dlp: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    /**
     * Actualiza yt-dlp en runtime desde GitHub (canal "stable" por defecto,
     * o "nightly" / "master"). Necesario cuando YouTube rompe extractores.
     * Resuelve: { status: "DONE" | "ALREADY_UP_TO_DATE", version }.
     */
    @PluginMethod
    public void updateYtDlp(final PluginCall call) {
        final String channelName = call.getString("channel", "stable");
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureInitialized();
                    YoutubeDL.UpdateChannel channel = YoutubeDL.UpdateChannel.STABLE.INSTANCE;
                    if ("nightly".equalsIgnoreCase(channelName)) {
                        channel = YoutubeDL.UpdateChannel.NIGHTLY.INSTANCE;
                    } else if ("master".equalsIgnoreCase(channelName)) {
                        channel = YoutubeDL.UpdateChannel.MASTER.INSTANCE;
                    }
                    final YoutubeDL.UpdateStatus status =
                            YoutubeDL.getInstance().updateYoutubeDL(getContext(), channel);
                    final String version = YoutubeDL.getInstance().version(getContext());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("status", status == null ? "DONE" : status.name());
                            ret.put("version", version);
                            call.resolve(ret);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            call.reject("No se pudo actualizar yt-dlp: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    /**
     * Descarga UNA canción como audio.
     * Parámetros JS: { id, url, format: "mp3"|"m4a", title }.
     * - format "mp3": re-codifica a MP3 VBR máxima calidad (audio-quality 0).
     * - format "m4a": extrae el audio AAC a .m4a (mejor si la fuente ya es AAC).
     * Progreso real por el evento "downloadProgress": { id, progress 0-100,
     * eta (segundos), status }.
     * Resuelve: { id, filePath, fileName, format, size }.
     */
    @PluginMethod
    public void downloadAudio(final PluginCall call) {
        final String id = call.getString("id");
        final String url = call.getString("url");
        final String format = call.getString("format", "mp3");
        final String title = call.getString("title", "");
        if (id == null || id.isEmpty() || url == null || url.isEmpty()) {
            call.reject("downloadAudio necesita id y url");
            return;
        }
        final String fmt = "m4a".equalsIgnoreCase(format) ? "m4a" : "mp3";
        final String processId = PROCESS_PREFIX + id;
        runningProcesses.put(id, processId);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                File tmpOut = null;
                try {
                    ensureInitialized();
                    emitProgress(id, 0f, -1L, "resolving", "Preparando descarga…");

                    File dir = tmpDir();
                    String tmpTemplate = new File(dir, "dl_" + id + ".%(ext)s").getAbsolutePath();

                    YoutubeDLRequest request = new YoutubeDLRequest(url);
                    request.addOption("-f", "bestaudio/best");
                    request.addOption("--extract-audio");
                    request.addOption("--audio-format", fmt);
                    request.addOption("--audio-quality", "0");
                    request.addOption("--no-playlist");
                    request.addOption("--no-warnings");
                    request.addOption("-o", tmpTemplate);

                    lastProgressId = null;
                    lastProgressSent = -1f;
                    lastProgressSentAt = 0L;
                    YoutubeDLResponse response = YoutubeDL.getInstance().execute(
                            request, processId,
                            new Function3<Float, Long, String, Unit>() {
                                @Override
                                public Unit invoke(Float progress, Long eta, String line) {
                                    long now = System.currentTimeMillis();
                                    boolean changed = lastProgressId == null
                                            || !lastProgressId.equals(id)
                                            || Math.abs(progress - lastProgressSent) >= 1f
                                            || now - lastProgressSentAt > 1000;
                                    if (changed) {
                                        lastProgressId = id;
                                        lastProgressSent = progress;
                                        lastProgressSentAt = now;
                                        emitProgress(id, progress, eta == null ? -1L : eta,
                                                "downloading", line);
                                    }
                                    return Unit.INSTANCE;
                                }
                            });

                    if (response == null || response.getExitCode() != 0) {
                        String err = response == null ? "sin respuesta de yt-dlp"
                                : shortError(response.getErr(), 300);
                        throw new Exception("yt-dlp terminó con error (código "
                                + (response == null ? "?" : response.getExitCode()) + "): " + err);
                    }

                    // El archivo temporal producido: dl_<id>.<ext>
                    File[] produced = dir.listFiles();
                    if (produced != null) {
                        for (File f : produced) {
                            if (f.getName().startsWith("dl_" + id + ".")) { tmpOut = f; break; }
                        }
                    }
                    if (tmpOut == null || !tmpOut.exists() || tmpOut.length() == 0) {
                        throw new Exception("yt-dlp no produjo ningún archivo de audio "
                                + "(puede que YouTube haya bloqueado este video: prueba "
                                + "actualizar yt-dlp o escúchalo en solo-audio).");
                    }

                    String base = (title != null && !title.trim().isEmpty()) ? title : id;
                    String displayName = safeFileName(base, fmt);
                    String finalPath = publishToPublicMusic(tmpOut, displayName, fmt);
                    long size = tmpOut.length();
                    try { tmpOut.delete(); } catch (Exception ignored) {}

                    final String fPath = finalPath;
                    final String fName = displayName;
                    final long fSize = size;
                    emitProgress(id, 100f, 0L, "done", "Guardada: " + displayName);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("id", id);
                            ret.put("filePath", fPath);
                            ret.put("fileName", fName);
                            ret.put("format", fmt);
                            ret.put("size", fSize);
                            call.resolve(ret);
                        }
                    });
                } catch (final Exception e) {
                    final String msg = shortError(e.getMessage(), 300);
                    if (tmpOut != null) { try { tmpOut.delete(); } catch (Exception ignored) {} }
                    emitProgress(id, 0f, -1L, "error", msg);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            call.reject(msg);
                        }
                    });
                } finally {
                    runningProcesses.remove(id);
                }
            }
        });
    }

    /** Detiene la descarga en curso con ese id (si la hay). */
    @PluginMethod
    public void cancelDownload(PluginCall call) {
        String id = call.getString("id", "");
        String processId = runningProcesses.get(id);
        boolean cancelled = false;
        if (processId != null) {
            try {
                cancelled = YoutubeDL.getInstance().destroyProcessById(processId);
            } catch (Exception ignored) {}
        }
        JSObject ret = new JSObject();
        ret.put("cancelled", cancelled);
        call.resolve(ret);
    }
}
