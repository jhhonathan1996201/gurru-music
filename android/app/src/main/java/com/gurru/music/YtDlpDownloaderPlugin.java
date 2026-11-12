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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

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
    /* Contador para que cada petición de audio directo tenga su propio
       id de proceso: si se tocan dos canciones seguidas, el segundo
       yt-dlp no choca con el primero por usar el mismo id. */
    private static int streamSeq = 0;
    private static synchronized int nextStreamSeq() { return ++streamSeq; }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    /* El caché de YouTube online corre en su propio hilo: preparar una
       canción nunca se queda esperando detrás de una descarga (ni una
       descarga detrás de una preparación). */
    private final ExecutorService cacheExecutor = Executors.newSingleThreadExecutor();
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

    /** Progreso de la preparación del caché invisible (una canción a la
        vez): el evento "cacheProgress" lleva {videoId, progress}. */
    private void emitCacheProgress(final String videoId, final float progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                JSObject data = new JSObject();
                data.put("videoId", videoId);
                data.put("progress", (double) progress);
                notifyListeners("cacheProgress", data);
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

    /** Carpeta privada de la app para el caché invisible de YouTube:
        getCacheDir() no la ve MediaStore ni el usuario (no aparece en
        Música ni en Descargas) y el sistema puede limpiarla si falta
        espacio en el teléfono. */
    private File cacheDir() {
        File dir = new File(getContext().getCacheDir(), "ytcache");
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
                    // La versión se intenta leer, pero si falla NO se marca
                    // como no disponible: lo que importa es que Python +
                    // yt-dlp arrancaron. Antes, un fallo de version() aquí
                    // bloqueaba todas las descargas con el mensaje genérico
                    // de "solo funciona en el APK" sin decir el motivo real.
                    String version = "";
                    try { version = YoutubeDL.getInstance().version(getContext()); } catch (Exception ignored) {}
                    String versionName = "";
                    try { versionName = YoutubeDL.getInstance().versionName(getContext()); } catch (Exception ignored) {}
                    final String v = version;
                    final String vn = versionName;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("available", true);
                            ret.put("version", v);
                            ret.put("versionName", vn);
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
     * Busca canciones en YouTube Music con yt-dlp DENTRO del APK.
     * Parámetros JS: { query, limit } (limit por defecto 10, máx 20).
     * Resuelve: { results: [{ id, title, channel, duration, thumbnail }] }.
     * Sin API key y sin servidores de terceros: la búsqueda la hace yt-dlp,
     * igual que las descargas (mismo cliente Android para pasar el bloqueo
     * de YouTube). Se busca en YouTube Music (music.youtube.com) para que
     * salgan las canciones (audio oficial, mejor calidad) y no videoclips;
     * si esa búsqueda falla o no trae nada, se busca en YouTube normal
     * (ytsearch). Solo se devuelven videos (ids de 11 caracteres); la
     * miniatura es la oficial de YouTube (i.ytimg.com), como ya se usa en
     * el resto de la app.
     */
    @PluginMethod
    public void searchYouTube(final PluginCall call) {
        final String query = call.getString("query", "");
        final int limit = call.getInt("limit", 10);
        if (query == null || query.trim().isEmpty()) {
            call.reject("Escribe el nombre de una canción o artista para buscar.");
            return;
        }
        final int n = Math.max(1, Math.min(20, limit));
        final String q = query.trim();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureInitialized();
                    JSONArray results = null;
                    Exception lastErr = null;
                    try {
                        /* YouTube Music por su URL de búsqueda. El prefijo
                           ytmsearch: NO existe en yt-dlp (lo rechaza como
                           "Unsupported url scheme"), así que esa búsqueda
                           siempre fallaba y se caía al respaldo. */
                        String encQ;
                        try { encQ = URLEncoder.encode(q, "UTF-8"); }
                        catch (Exception ue) { encQ = q.replace(" ", "%20"); }
                        results = runYtSearch("https://music.youtube.com/search?q=" + encQ, n);
                    } catch (Exception e) { lastErr = e; }
                    if (results == null || results.length() == 0) {
                        try {
                            results = runYtSearch("ytsearch" + n + ":" + q, n);
                        } catch (Exception e) { lastErr = e; }
                    }
                    if (results == null) {
                        throw lastErr != null ? lastErr
                                : new Exception("yt-dlp no devolvió resultados");
                    }
                    final JSONArray resArr = results;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("results", resArr);
                            call.resolve(ret);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            call.reject(shortError(e.getMessage(), 300));
                        }
                    });
                }
            }
        });
    }

    /** Una búsqueda (URL de YouTube Music o ytsearch) con yt-dlp; devuelve los videos. */
    private JSONArray runYtSearch(String searchQuery, int maxResults) throws Exception {
        YoutubeDLRequest request = new YoutubeDLRequest(searchQuery);
        request.addOption("--flat-playlist");
        request.addOption("--dump-single-json");
        request.addOption("--no-playlist");
        request.addOption("--no-warnings");
        // Igual que en las descargas: el cliente Android pasa
        // mejor el bloqueo de YouTube que el cliente web.
        request.addOption("--extractor-args", "youtube:player_client=android");
        YoutubeDLResponse response = YoutubeDL.getInstance().execute(
                request, "gurru-search-" + nextStreamSeq(),
                new Function3<Float, Long, String, Unit>() {
                    @Override
                    public Unit invoke(Float progress, Long eta, String line) {
                        return Unit.INSTANCE;
                    }
                });
        if (response == null || response.getExitCode() != 0) {
            throw new Exception("yt-dlp no pudo buscar (código "
                    + (response == null ? "?" : response.getExitCode()) + "): "
                    + shortError(response == null ? "" : response.getErr(), 200));
        }
        String out = response.getOut();
        if (out == null) throw new Exception("yt-dlp no devolvió resultados");
        int start = out.indexOf('{');
        if (start >= 0) out = out.substring(start);
        JSONObject root = new JSONObject(out.trim());
        JSONArray entries = root.optJSONArray("entries");
        JSONArray results = new JSONArray();
        if (entries != null) {
            for (int i = 0; i < entries.length(); i++) {
                if (results.length() >= maxResults) break;
                JSONObject e = entries.optJSONObject(i);
                if (e == null) continue;
                /* Con --flat-playlist las entradas son _type "url", NO
                   "video": el filtro viejo ("video") tiraba TODOS los
                   resultados y la búsqueda siempre decía que no encontró
                   nada. Se aceptan video y url, pero solo si el id parece
                   un video de YouTube (11 caracteres): así se excluyen
                   los álbumes (listas) y los artistas (canales) que
                   también salen en YouTube Music, que no son canciones. */
                String type = e.optString("_type", "video");
                if (!"video".equals(type) && !"url".equals(type)) continue;
                String id = e.optString("id", "");
                if (id.isEmpty()) continue;
                if (!id.matches("[A-Za-z0-9_-]{11}")) continue;
                String title = e.optString("title", "");
                if (title.isEmpty()) continue;
                JSONObject r = new JSONObject();
                r.put("id", id);
                r.put("title", title);
                String channel = e.optString("uploader", "");
                if (channel.isEmpty()) channel = e.optString("channel", "YouTube");
                r.put("channel", channel);
                r.put("duration", e.optDouble("duration", 0));
                r.put("thumbnail", "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg");
                results.put(r);
            }
        }
        return results;
    }

    /** GET JSON por HTTP directo (sin CORS: esto corre en Java, no en
        el WebView). Usado por la API pública de Deezer. */
    private JSONObject fetchJson(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "GurruMusic/1.0 (Android)");
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) throw new Exception("Deezer respondió HTTP " + code);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONObject root = new JSONObject(sb.toString().trim());
            if (root.has("error")) {
                JSONObject err = root.optJSONObject("error");
                throw new Exception("Deezer: " + (err != null ? err.optString("message", "error") : "error"));
            }
            return root;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Busca canciones en Deezer con su API pública (sin clave):
     * GET https://api.deezer.com/search?q=...&limit=...
     * La búsqueda se hace en Java (HTTP directo) y NO en el WebView:
     * la API no manda Access-Control-Allow-Origin y el fetch del
     * WebView lo bloquearía por CORS. Devuelve { results: [{ id,
     * title, artist, album, duration, cover, preview }] }.
     * preview es la vista previa de 30 s (MP3 directo, con firma que
     * caduca en ~1 día): la canción completa NO está disponible sin
     * cuenta de Deezer con sesión.
     */
    @PluginMethod
    public void deezerSearch(final PluginCall call) {
        final String query = call.getString("query", "");
        final int limit = call.getInt("limit", 10);
        if (query == null || query.trim().isEmpty()) {
            call.reject("Escribe el nombre de una canción o artista para buscar.");
            return;
        }
        final int n = Math.max(1, Math.min(20, limit));
        final String q = query.trim();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String encQ;
                    try { encQ = URLEncoder.encode(q, "UTF-8"); }
                    catch (Exception ue) { encQ = q.replace(" ", "%20"); }
                    JSONObject root = fetchJson("https://api.deezer.com/search?q=" + encQ + "&limit=" + n);
                    JSONArray data = root.optJSONArray("data");
                    JSONArray results = new JSONArray();
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            if (results.length() >= n) break;
                            JSONObject e = data.optJSONObject(i);
                            if (e == null) continue;
                            String preview = e.optString("preview", "");
                            if (preview.isEmpty()) continue;
                            JSONObject r = new JSONObject();
                            r.put("id", e.optLong("id", 0));
                            r.put("title", e.optString("title", ""));
                            JSONObject artist = e.optJSONObject("artist");
                            r.put("artist", artist != null ? artist.optString("name", "Deezer") : "Deezer");
                            JSONObject album = e.optJSONObject("album");
                            r.put("album", album != null ? album.optString("title", "") : "");
                            r.put("cover", album != null ? album.optString("cover_medium", "") : "");
                            r.put("duration", e.optDouble("duration", 0));
                            r.put("preview", preview);
                            results.put(r);
                        }
                    }
                    final JSONArray resArr = results;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("results", resArr);
                            call.resolve(ret);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            call.reject(shortError(e.getMessage(), 300));
                        }
                    });
                }
            }
        });
    }

    /**
     * Una canción de Deezer por su id: GET https://api.deezer.com/track/{id}.
     * Devuelve la vista previa FRESCA (el link guardado caduca en ~1
     * día): { id, title, artist, album, duration, cover, preview }.
     * Se usa al reabrir una canción de Deezer guardada, antes de sonar
     * o de descargar.
     */
    @PluginMethod
    public void deezerTrack(final PluginCall call) {
        final String id = call.getString("id", "");
        if (id == null || id.trim().isEmpty()) {
            call.reject("Falta el id de Deezer.");
            return;
        }
        final String tid = id.trim();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject e = fetchJson("https://api.deezer.com/track/" + URLEncoder.encode(tid, "UTF-8"));
                    final JSObject ret = new JSObject();
                    ret.put("id", e.optLong("id", 0));
                    ret.put("title", e.optString("title", ""));
                    JSONObject artist = e.optJSONObject("artist");
                    ret.put("artist", artist != null ? artist.optString("name", "Deezer") : "Deezer");
                    JSONObject album = e.optJSONObject("album");
                    ret.put("album", album != null ? album.optString("title", "") : "");
                    ret.put("cover", album != null ? album.optString("cover_medium", "") : "");
                    ret.put("duration", e.optDouble("duration", 0));
                    ret.put("preview", e.optString("preview", ""));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { call.resolve(ret); }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            call.reject(shortError(e.getMessage(), 300));
                        }
                    });
                }
            }
        });
    }

    /**
     * Devuelve la URL directa del mejor audio de un video de YouTube,
     * para reproducirla en un <audio> normal.
     * El reproductor oficial de YouTube (iframe) pausa el audio cuando
     * la app pasa a segundo plano — es una restricción de YouTube para
     * no suscriptores de Premium —, mientras que el <audio> propio de
     * la app sigue sonando con el servicio en primer plano y la
     * MediaSession, igual que un MP3 local. La URL caduca en unas
     * horas: no se guarda, se vuelve a pedir al reproducir.
     * Se usa --dump-json (el mismo mecanismo que la búsqueda, que sí
     * funciona) y se elige el mejor formato solo-audio por bitrate, en
     * vez de -g: así se evita depender de la salida de -g y se suena
     * con la mejor calidad de audio disponible. Resuelve: { url }.
     * Parámetro opcional: { url, client } — client es el cliente de
     * YouTube que usa yt-dlp ("android" por defecto; también "ios",
     * "web" o "tv"). El cliente cambia qué formatos devuelve YouTube:
     * si uno no trae la dirección del audio, otro puede traerla.
     */
    @PluginMethod
    public void getAudioUrl(final PluginCall call) {
        final String url = call.getString("url", "");
        final String client = call.getString("client", "android");
        if (url == null || url.trim().isEmpty()) {
            call.reject("Falta la dirección del video de YouTube.");
            return;
        }
        // Solo clientes conocidos: cualquier otro valor vuelve a android.
        final String playerClient;
        if ("ios".equals(client) || "web".equals(client) || "tv".equals(client)) {
            playerClient = client;
        } else {
            playerClient = "android";
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureInitialized();
                    YoutubeDLRequest request = new YoutubeDLRequest(url.trim());
                    request.addOption("--dump-json");
                    /* Formato explícito: sin -f, yt-dlp usa su selector
                       por defecto y en algunos videos (p. ej. los
                       auto-generados de YouTube Music, que solo traen el
                       formato 18 combinado) falla con "Requested format
                       is not available". Con bestaudio/best devuelve ese
                       formato combinado, que trae audio. */
                    request.addOption("-f", "bestaudio/best");
                    request.addOption("--no-playlist");
                    request.addOption("--no-warnings");
                    // Igual que en las descargas: el cliente Android pasa
                    // mejor el bloqueo de YouTube que el cliente web.
                    request.addOption("--extractor-args", "youtube:player_client=" + playerClient);
                    YoutubeDLResponse response = YoutubeDL.getInstance().execute(
                            request, "gurru-stream-" + nextStreamSeq(),
                            new Function3<Float, Long, String, Unit>() {
                                @Override
                                public Unit invoke(Float progress, Long eta, String line) {
                                    return Unit.INSTANCE;
                                }
                            });
                    if (response == null || response.getExitCode() != 0) {
                        throw new Exception("yt-dlp no pudo obtener el audio (código "
                                + (response == null ? "?" : response.getExitCode()) + "): "
                                + shortError(response == null ? "" : response.getErr(), 200));
                    }
                    String out = response == null ? null : response.getOut();
                    if (out == null) throw new Exception("yt-dlp no devolvió información del video");
                    int start = out.indexOf('{');
                    if (start >= 0) out = out.substring(start);
                    JSONObject info = new JSONObject(out.trim());
                    String streamUrl = pickBestAudioUrl(info);
                    if (streamUrl == null || streamUrl.isEmpty()) {
                        throw new Exception("yt-dlp no devolvió la dirección del audio");
                    }
                    final String finalUrl = streamUrl;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("url", finalUrl);
                            call.resolve(ret);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            call.reject(shortError(e.getMessage(), 300));
                        }
                    });
                }
            }
        });
    }

    /** Metadatos reales del video (artista, canción, álbum) para las
        sugerencias: el buscador solo trae el canal que subió el video
        (a veces el sello o "BelanovaVEVO"), pero --dump-json devuelve
        el artista de verdad ("Belanova"). */
    @PluginMethod
    public void getVideoInfo(final PluginCall call) {
        final String url = call.getString("url", "");
        if (url == null || url.trim().isEmpty()) {
            call.reject("Falta la dirección del video de YouTube.");
            return;
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureInitialized();
                    YoutubeDLRequest request = new YoutubeDLRequest(url.trim());
                    request.addOption("--dump-json");
                    request.addOption("-f", "bestaudio/best");
                    request.addOption("--no-playlist");
                    request.addOption("--no-warnings");
                    request.addOption("--extractor-args", "youtube:player_client=android");
                    YoutubeDLResponse response = YoutubeDL.getInstance().execute(
                            request, "gurru-info-" + nextStreamSeq(),
                            new Function3<Float, Long, String, Unit>() {
                                @Override
                                public Unit invoke(Float progress, Long eta, String line) {
                                    return Unit.INSTANCE;
                                }
                            });
                    if (response == null || response.getExitCode() != 0) {
                        throw new Exception("yt-dlp no pudo leer el video (código "
                                + (response == null ? "?" : response.getExitCode()) + "): "
                                + shortError(response == null ? "" : response.getErr(), 200));
                    }
                    String out = response == null ? null : response.getOut();
                    if (out == null) throw new Exception("yt-dlp no devolvió información del video");
                    int start = out.indexOf('{');
                    if (start >= 0) out = out.substring(start);
                    JSONObject info = new JSONObject(out.trim());
                    final String id = info.optString("id", "");
                    final String title = info.optString("title", "");
                    final String artist = info.optString("artist", "");
                    final String track = info.optString("track", "");
                    final String album = info.optString("album", "");
                    final String uploader = info.optString("uploader", "");
                    final long duration = info.optLong("duration", 0);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("id", id);
                            ret.put("title", title);
                            ret.put("artist", artist);
                            ret.put("track", track);
                            ret.put("album", album);
                            ret.put("uploader", uploader);
                            ret.put("duration", duration);
                            call.resolve(ret);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            call.reject(shortError(e.getMessage(), 300));
                        }
                    });
                }
            }
        });
    }

    /** Elige la URL del mejor formato solo-audio (sin video), por bitrate. */
    private static String pickBestAudioUrl(JSONObject info) {
        JSONArray formats = info.optJSONArray("formats");
        if (formats == null) return null;
        String bestUrl = null;
        double bestAbr = -1;
        // Primero: solo audio (vcodec none), mayor bitrate.
        for (int i = 0; i < formats.length(); i++) {
            JSONObject f = formats.optJSONObject(i);
            if (f == null) continue;
            String vcodec = f.optString("vcodec", "none");
            String acodec = f.optString("acodec", "none");
            String u = f.optString("url", "");
            if (u.isEmpty() || "none".equals(acodec)) continue;
            if (!"none".equals(vcodec)) continue; // tiene video: no es solo audio
            double abr = f.optDouble("abr", 0);
            if (abr > bestAbr) { bestAbr = abr; bestUrl = u; }
        }
        if (bestUrl != null) return bestUrl;
        // Respaldo: cualquier formato con audio, mayor bitrate.
        for (int i = 0; i < formats.length(); i++) {
            JSONObject f = formats.optJSONObject(i);
            if (f == null) continue;
            String acodec = f.optString("acodec", "none");
            String u = f.optString("url", "");
            if (u.isEmpty() || "none".equals(acodec)) continue;
            double abr = f.optDouble("abr", 0);
            if (abr > bestAbr) { bestAbr = abr; bestUrl = u; }
        }
        return bestUrl;
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
                    /* Metadata original: --add-metadata escribe en el
                       archivo el título, artista y demás datos del video,
                       y --embed-thumbnail incrusta su portada. El audio no
                       se toca, solo sus etiquetas. */
                    request.addOption("--add-metadata");
                    request.addOption("--embed-thumbnail");
                    request.addOption("--no-playlist");
                    request.addOption("--no-warnings");
                    // YouTube bloquea cada vez más al cliente web con
                    // "Sign in to confirm you're not a bot" (403). Pedir el
                    // audio como cliente Android suele pasar ese bloqueo.
                    request.addOption("--extractor-args", "youtube:player_client=android");
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

    /** Una descarga de audio para el caché invisible. Sin cliente Android
        (primer intento): yt-dlp usa su cliente por defecto y ve los
        formatos DASH de solo-audio (itag 140 AAC ~130 kbps, 251 Opus
        ~137 kbps), mucho mejores que el formato 18 (~48 kbps) que es lo
        único que da el cliente Android. Con cliente Android (reintento):
        el comportamiento de siempre. El formato pedido es
        bestaudio[ext=m4a]/bestaudio para preferir el AAC 140 (reempaquetado
        a M4A sin transcodificar) y caer al mejor audio si no hay M4A. */
    private void runCacheRequest(String url, String template, final String vid, boolean androidClient) throws Exception {
        YoutubeDLRequest request = new YoutubeDLRequest(url.trim());
        request.addOption("-f", androidClient ? "bestaudio/best" : "bestaudio[ext=m4a]/bestaudio");
        /* Con --extract-audio a M4A (solo audio, sin video): el formato 18
           (el único con audio en muchos videos de YouTube Music) es un MP4
           con pista de video, y el WebView pausa el audio cuando la página
           se oculta si el archivo trae video (por eso la música local MP3
           seguía sonando en segundo plano y la de YouTube no). Con M4A
           solo-audio la canción sigue sonando al salir de la app, igual que
           un MP3 local. Para audio AAC (formato 18, itag 140) es solo un
           reempaquetado rápido. */
        request.addOption("--extract-audio");
        request.addOption("--audio-format", "m4a");
        request.addOption("--no-playlist");
        request.addOption("--no-warnings");
        if (androidClient) request.addOption("--extractor-args", "youtube:player_client=android");
        request.addOption("-o", template);
        YoutubeDLResponse response = YoutubeDL.getInstance().execute(
                request, "gurru-cache-" + nextStreamSeq(),
                new Function3<Float, Long, String, Unit>() {
                    @Override
                    public Unit invoke(Float progress, Long eta, String line) {
                        emitCacheProgress(vid, progress == null ? 0f : progress);
                        return Unit.INSTANCE;
                    }
                });
        if (response == null || response.getExitCode() != 0) {
            throw new Exception("yt-dlp no pudo preparar el audio (código "
                    + (response == null ? "?" : response.getExitCode()) + "): "
                    + shortError(response == null ? "" : response.getErr(), 300));
        }
    }

    /** Borra los archivos del caché de un video (incluidos .part a medias). */
    private static void deleteCacheFiles(File dir, String vid) {
        try {
            File[] files = dir.listFiles();
            if (files == null) return;
            for (File f : files) {
                if (f.getName().startsWith(vid + ".")) { try { f.delete(); } catch (Exception ignored) {} }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Caché invisible para escuchar YouTube online como si fuera local:
     * descarga el audio del video (sin convertir, sin metadatos: rápido)
     * a la carpeta privada de la app y devuelve su ruta. La canción
     * sigue siendo "YouTube" en la app (mismo botón, misma cola, mismas
     * sugerencias); solo su fuente de audio cambia por dentro. No toca
     * Descargas ni Música/Gurru Music: eso sigue siendo solo para el
     * botón Descargar. Resuelve: { path }.
     */
    @PluginMethod
    public void cacheAudio(final PluginCall call) {
        final String url = call.getString("url", "");
        final String videoId = call.getString("videoId", "");
        if (url == null || url.trim().isEmpty() || videoId == null || videoId.trim().isEmpty()) {
            call.reject("cacheAudio necesita url y videoId");
            return;
        }
        final String vid = videoId.trim();
        cacheExecutor.execute(new Runnable() {
            @Override
            public void run() {
                File produced = null;
                try {
                    ensureInitialized();
                    File dir = cacheDir();
                    /* Si ya está en caché de una preparación anterior, se
                       reutiliza sin descargar de nuevo. */
                    File[] existing = dir.listFiles();
                    if (existing != null) {
                        for (File f : existing) {
                            if (f.getName().startsWith(vid + ".")
                                        && !f.getName().endsWith(".part")) { produced = f; break; }
                        }
                    }
                    if (produced == null) {
                        emitCacheProgress(vid, 0f);
                        String template = new File(dir, vid + ".%(ext)s").getAbsolutePath();
                        try {
                            runCacheRequest(url.trim(), template, vid, false);
                        } catch (Exception firstTry) {
                            /* El cliente por defecto (formatos DASH de mejor
                               calidad) falló en este video/red: se borra lo
                               que haya dejado a medias y se reintenta con el
                               cliente Android, que es el que siempre ha
                               funcionado (formato 18). */
                            deleteCacheFiles(dir, vid);
                            runCacheRequest(url.trim(), template, vid, true);
                        }
                        File[] out = dir.listFiles();
                        if (out != null) {
                            for (File f : out) {
                                if (f.getName().startsWith(vid + ".")
                                        && !f.getName().endsWith(".part")) { produced = f; break; }
                            }
                        }
                        if (produced == null || !produced.exists() || produced.length() == 0) {
                            throw new Exception("yt-dlp no produjo el archivo de audio para este video");
                        }
                    }
                    final String path = produced.getAbsolutePath();
                    emitCacheProgress(vid, 100f);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("path", path);
                            call.resolve(ret);
                        }
                    });
                } catch (final Exception e) {
                    final String msg = shortError(e.getMessage(), 300);
                    if (produced != null) { try { produced.delete(); } catch (Exception ignored) {} }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            call.reject(msg);
                        }
                    });
                }
            }
        });
    }

    /** Borra el archivo en caché de un video (si existe). */
    @PluginMethod
    public void deleteCachedAudio(PluginCall call) {
        String videoId = call.getString("videoId", "");
        int deleted = 0;
        if (videoId != null && !videoId.trim().isEmpty()) {
            File dir = cacheDir();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().startsWith(videoId.trim() + ".")) {
                        if (f.delete()) deleted++;
                    }
                }
            }
        }
        JSObject ret = new JSObject();
        ret.put("deleted", deleted);
        call.resolve(ret);
    }

    /** Vacía todo el caché invisible de YouTube (al abrir la app). */
    @PluginMethod
    public void clearAudioCache(PluginCall call) {
        int deleted = 0;
        File dir = cacheDir();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.delete()) deleted++;
            }
        }
        JSObject ret = new JSObject();
        ret.put("deleted", deleted);
        call.resolve(ret);
    }

    /**
     * Guarda en Música/Gurru Music la canción que YA está descargada en
     * el caché invisible (NUEVO13): cuando suena una canción online, su
     * audio ya está en el teléfono, así que el botón Descargar del
     * reproductor solo mueve ese archivo a la carpeta pública en vez de
     * bajarlo de nuevo. Resuelve: { id, filePath, fileName, format, size }.
     * Si el caché ya se borró (al preparar otras canciones), rechaza y
     * el frontend usa la descarga normal.
     */
    @PluginMethod
    public void saveCachedAudio(final PluginCall call) {
        final String videoId = call.getString("videoId", "");
        final String title = call.getString("title", "");
        if (videoId == null || videoId.trim().isEmpty()) {
            call.reject("saveCachedAudio necesita videoId");
            return;
        }
        final String vid = videoId.trim();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = cacheDir();
                    File cached = null;
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.getName().startsWith(vid + ".")) { cached = f; break; }
                        }
                    }
                    if (cached == null || !cached.exists() || cached.length() == 0) {
                        throw new Exception("La canción ya no está en el caché (se borró al preparar otra). Descárgala desde la lista.");
                    }
                    String name = cached.getName();
                    int dot = name.lastIndexOf('.');
                    String ext = (dot > 0) ? name.substring(dot + 1).toLowerCase() : "m4a";
                    if (!"mp3".equals(ext) && !"m4a".equals(ext) && !"aac".equals(ext)
                            && !"opus".equals(ext) && !"ogg".equals(ext) && !"wav".equals(ext)
                            && !"flac".equals(ext)) ext = "m4a";
                    String base = (title != null && !title.trim().isEmpty()) ? title : vid;
                    String displayName = safeFileName(base, ext);
                    String finalPath = publishToPublicMusic(cached, displayName, ext);
                    final long size = cached.length();
                    final String fPath = finalPath;
                    final String fName = displayName;
                    final String fExt = ext;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("id", vid);
                            ret.put("filePath", fPath);
                            ret.put("fileName", fName);
                            ret.put("format", fExt);
                            ret.put("size", size);
                            call.resolve(ret);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            call.reject(shortError(e.getMessage(), 300));
                        }
                    });
                }
            }
        });
    }

}
