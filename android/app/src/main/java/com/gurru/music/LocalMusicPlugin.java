package com.gurru.music;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plugin nativo de Gurru Music para buscar la música del teléfono
 * automáticamente, sin que el usuario tenga que elegir carpetas a mano.
 *
 * Lee la biblioteca de audio de Android (MediaStore): título, artista,
 * álbum, duración y la URI de cada canción, más la portada del álbum
 * cuando existe. La página reproduce las canciones con
 * Capacitor.convertFileSrc(uri): el servidor local sirve las URIs
 * content:// como https://localhost/_capacitor_content_/...
 *
 * Permiso en runtime: READ_MEDIA_AUDIO en Android 13+ y
 * READ_EXTERNAL_STORAGE en anteriores (ambos ya están declarados en
 * AndroidManifest.xml). La primera vez el sistema pregunta al usuario.
 */
@CapacitorPlugin(
    name = "LocalMusic",
    permissions = {
        @Permission(alias = "media", strings = { android.Manifest.permission.READ_MEDIA_AUDIO }),
        @Permission(alias = "storage", strings = { android.Manifest.permission.READ_EXTERNAL_STORAGE })
    }
)
public class LocalMusicPlugin extends Plugin {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @PluginMethod
    public void scanLibrary(final PluginCall call) {
        String alias = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? "media" : "storage";
        if (!hasPermission(alias)) {
            requestPermissionForAlias(alias, call, "scanPermissionCallback");
            return;
        }
        runScan(call);
    }

    @PermissionCallback
    private void scanPermissionCallback(PluginCall call) {
        if (hasPermission("media") || hasPermission("storage")) {
            runScan(call);
        } else {
            call.reject("Sin permiso para leer la música del teléfono. Actívalo en Ajustes > Aplicaciones > Gurru Music > Permisos.");
        }
    }

    private void runScan(final PluginCall call) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Context ctx = getContext();
                    Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    String[] projection = {
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.ALBUM_ID,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DISPLAY_NAME
                    };
                    String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
                    Cursor cursor = ctx.getContentResolver().query(
                            collection, projection, selection, null,
                            MediaStore.Audio.Media.TITLE + " ASC");
                    JSONArray songs = new JSONArray();
                    if (cursor != null) {
                        int iId = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                        int iTitle = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                        int iArtist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                        int iAlbum = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                        int iAlbumId = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                        int iDur = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                        int iName = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                        while (cursor.moveToNext()) {
                            long id = iId >= 0 ? cursor.getLong(iId) : -1;
                            if (id < 0) continue;
                            String title = iTitle >= 0 ? cursor.getString(iTitle) : null;
                            String name = iName >= 0 ? cursor.getString(iName) : null;
                            if (title == null || title.trim().isEmpty()) {
                                title = name != null ? name.replaceAll("\\.[^.]+$", "") : "Canción";
                            }
                            String artist = iArtist >= 0 ? cursor.getString(iArtist) : null;
                            if (artist == null || artist.trim().isEmpty() || "<unknown>".equals(artist)) {
                                artist = "Artista desconocido";
                            }
                            String album = iAlbum >= 0 ? cursor.getString(iAlbum) : null;
                            if (album == null) album = "";
                            long albumId = iAlbumId >= 0 ? cursor.getLong(iAlbumId) : -1;
                            long durMs = iDur >= 0 ? cursor.getLong(iDur) : 0;
                            JSONObject s = new JSONObject();
                            s.put("id", id);
                            s.put("title", title);
                            s.put("artist", artist);
                            s.put("album", album);
                            s.put("duration", durMs / 1000.0);
                            s.put("uri", ContentUris.withAppendedId(collection, id).toString());
                            if (albumId > 0) {
                                s.put("albumArt", ContentUris.withAppendedId(
                                        Uri.parse("content://media/external/audio/albumart"), albumId).toString());
                            }
                            if (name != null) s.put("fileName", name);
                            songs.put(s);
                        }
                        cursor.close();
                    }
                    final JSONArray result = songs;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSObject ret = new JSObject();
                            ret.put("songs", result);
                            call.resolve(ret);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            call.reject("No se pudo buscar la música: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void runOnUiThread(Runnable r) {
        try {
            if (getActivity() != null) getActivity().runOnUiThread(r);
            else r.run();
        } catch (Exception e) {
            r.run();
        }
    }
}
