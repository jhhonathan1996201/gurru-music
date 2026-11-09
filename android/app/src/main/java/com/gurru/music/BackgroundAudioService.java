package com.gurru.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio en primer plano que mantiene viva la reproducción de
 * Gurru Music cuando la app pasa a segundo plano o se apaga la
 * pantalla, y publica una MediaSession real para que el sistema
 * reconozca a la app como reproductor: controles en la barra de
 * notificaciones, en la pantalla de bloqueo y en Bluetooth, con la
 * portada, el título, el artista y la barra de progreso.
 *
 * No reproduce audio por sí mismo: el audio sigue en el WebView
 * (<audio> para MP3 locales, reproductor de YouTube para online).
 * Las acciones de la sesión (reproducir/pausar, siguiente, anterior,
 * buscar, detener) vuelven a la página con broadcasts que el plugin
 * BackgroundAudio convierte en llamadas a window.__gurru*.
 */
public class BackgroundAudioService extends Service {

    public static final String ACTION_SESSION = "com.gurru.music.BG_SESSION";
    public static final String ACTION_STOP_SERVICE = "com.gurru.music.BG_STOP_SERVICE";

    private static final String CHANNEL_ID = "gurru-playback";
    private static final int NOTIF_ID = 1001;

    private PowerManager.WakeLock wakeLock;
    private MediaSession session;
    private final ExecutorService artExecutor = Executors.newSingleThreadExecutor();

    private String title = "Gurru Music";
    private String artist = "";
    private String album = "";
    private String art = "";
    private long durationMs = 0;
    private long positionMs = 0;
    private boolean playing = true;
    private boolean inForeground = false;
    private Bitmap lastArtBitmap;
    private String lastArtLoaded = "";

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GurruMusic:playback");
            wakeLock.setReferenceCounted(false);
        }
        createChannel();
        session = new MediaSession(this, "GurruMusic");
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { sendAction(BackgroundAudioPlugin.ACTION_TOGGLE); }
            @Override public void onPause() { sendAction(BackgroundAudioPlugin.ACTION_TOGGLE); }
            @Override public void onSkipToNext() { sendAction(BackgroundAudioPlugin.ACTION_NEXT); }
            @Override public void onSkipToPrevious() { sendAction(BackgroundAudioPlugin.ACTION_PREV); }
            @Override public void onSeekTo(long pos) {
                Intent intent = new Intent(BackgroundAudioPlugin.ACTION_SEEK);
                intent.setPackage(getPackageName());
                intent.putExtra("positionMs", pos);
                sendBroadcast(intent);
            }
            @Override public void onStop() { sendAction(BackgroundAudioPlugin.ACTION_STOP); }
        });
        session.setActive(true);
        updateSessionState();
    }

    private void sendAction(String action) {
        Intent intent = new Intent(action);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP_SERVICE.equals(action)) {
                stopSelf();
                return START_NOT_STICKY;
            }
            if (intent.hasExtra("title")) {
                String t = intent.getStringExtra("title");
                if (t != null) title = t;
            }
            if (intent.hasExtra("artist")) {
                String a = intent.getStringExtra("artist");
                if (a != null) artist = a;
            }
            if (intent.hasExtra("album")) {
                String a = intent.getStringExtra("album");
                if (a != null) album = a;
            }
            if (intent.hasExtra("art")) {
                String a = intent.getStringExtra("art");
                if (a != null) art = a;
            }
            if (intent.hasExtra("durationMs")) durationMs = intent.getLongExtra("durationMs", durationMs);
            if (intent.hasExtra("positionMs")) positionMs = intent.getLongExtra("positionMs", positionMs);
            if (intent.hasExtra("playing")) playing = intent.getBooleanExtra("playing", playing);
        }
        if (wakeLock != null && !wakeLock.isHeld()) {
            try { wakeLock.acquire(); } catch (Exception ignored) {}
        }
        updateSessionState();
        loadArtworkIfNeeded();
        Notification notification = buildNotification();
        if (!inForeground) {
            startForeground(NOTIF_ID, notification);
            inForeground = true;
        } else {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(NOTIF_ID, notification);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (session != null) {
            try { session.setActive(false); session.release(); } catch (Exception ignored) {}
            session = null;
        }
        artExecutor.shutdownNow();
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (Exception ignored) {}
        }
        inForeground = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** Publica en la sesión el tema actual, su estado y su posición. */
    private void updateSessionState() {
        if (session == null) return;
        MediaMetadata.Builder meta = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs);
        if (lastArtBitmap != null) {
            meta.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, lastArtBitmap);
        }
        session.setMetadata(meta.build());
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                | PlaybackState.ACTION_PLAY_PAUSE
                | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                | PlaybackState.ACTION_SEEK_TO | PlaybackState.ACTION_STOP;
        int state = playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
        PlaybackState ps = new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, positionMs, 1.0f)
                .build();
        session.setPlaybackState(ps);
    }

    /** Carga la portada solo cuando cambia (URL https, content:// o archivo). */
    private void loadArtworkIfNeeded() {
        final String artSrc = art == null ? "" : art;
        if (artSrc.equals(lastArtLoaded)) return;
        lastArtLoaded = artSrc;
        if (artSrc.isEmpty()) {
            lastArtBitmap = null;
            updateSessionState();
            return;
        }
        artExecutor.execute(new Runnable() {
            @Override public void run() {
                Bitmap bmp = null;
                try {
                    InputStream in = null;
                    HttpURLConnection conn = null;
                    if (artSrc.startsWith("http://") || artSrc.startsWith("https://")) {
                        URL url = new URL(artSrc);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(4000);
                        conn.setReadTimeout(4000);
                        in = conn.getInputStream();
                    } else if (artSrc.startsWith("content://")) {
                        in = getContentResolver().openInputStream(Uri.parse(artSrc));
                    } else if (artSrc.startsWith("file://")) {
                        in = new java.io.FileInputStream(new File(Uri.parse(artSrc).getPath()));
                    } else {
                        in = new java.io.FileInputStream(new File(artSrc));
                    }
                    if (in != null) {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inSampleSize = 2;
                        bmp = BitmapFactory.decodeStream(in, null, opts);
                        try { in.close(); } catch (Exception ignored) {}
                    }
                    if (conn != null) conn.disconnect();
                } catch (Exception ignored) {}
                if (bmp != null) {
                    lastArtBitmap = bmp;
                    updateSessionState();
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null && inForeground) nm.notify(NOTIF_ID, buildNotification());
                }
            }
        });
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reproducción en curso",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Muestra la canción que está sonando en Gurru Music y permite controlarla desde la notificación y la pantalla de bloqueo.");
            channel.setShowBadge(false);
            nm.createNotificationChannel(channel);
        }
    }

    private PendingIntent actionIntent(String action) {
        Intent intent = new Intent(action);
        intent.setPackage(getPackageName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(this, action.hashCode(), intent, flags);
    }

    private Notification buildNotification() {
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openApp, flags);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
            builder.setPriority(Notification.PRIORITY_LOW);
        }
        builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(artist == null || artist.isEmpty() ? "Gurru Music" : artist)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .addAction(
                        android.R.drawable.ic_media_previous,
                        "Anterior",
                        actionIntent(BackgroundAudioPlugin.ACTION_PREV))
                .addAction(
                        playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        playing ? "Pausa" : "Reproducir",
                        actionIntent(BackgroundAudioPlugin.ACTION_TOGGLE))
                .addAction(
                        android.R.drawable.ic_media_next,
                        "Siguiente",
                        actionIntent(BackgroundAudioPlugin.ACTION_NEXT))
                .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Detener",
                        actionIntent(BackgroundAudioPlugin.ACTION_STOP));
        if (session != null) {
            builder.setStyle(new Notification.MediaStyle()
                    .setMediaSession(session.getSessionToken())
                    .setShowActionsInCompactView(0, 1, 2));
        }
        return builder.build();
    }
}
