package com.gurru.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

/**
 * Servicio en primer plano que mantiene viva la reproducción de
 * Gurru Music cuando la app pasa a segundo plano o se apaga la
 * pantalla. Muestra una notificación con la canción actual y
 * acciones de pausa/reproducir, siguiente y detener, y sostiene
 * un wake lock parcial para que la CPU no se duerma mientras suena.
 *
 * No reproduce audio por sí mismo: el audio sigue en el WebView
 * (<audio> para MP3 locales, reproductor de YouTube para online).
 */
public class BackgroundAudioService extends Service {

    public static final String ACTION_START = "com.gurru.music.BG_START";
    public static final String ACTION_UPDATE = "com.gurru.music.BG_UPDATE";
    public static final String ACTION_STOP_SERVICE = "com.gurru.music.BG_STOP_SERVICE";

    private static final String CHANNEL_ID = "gurru-playback";
    private static final int NOTIF_ID = 1001;

    private PowerManager.WakeLock wakeLock;
    private String title = "Gurru Music";
    private String artist = "";
    private boolean playing = true;
    private boolean inForeground = false;

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GurruMusic:playback");
            wakeLock.setReferenceCounted(false);
        }
        createChannel();
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
            if (intent.hasExtra("playing")) playing = intent.getBooleanExtra("playing", playing);
        }
        if (wakeLock != null && !wakeLock.isHeld()) {
            try { wakeLock.acquire(); } catch (Exception ignored) {}
        }
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

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reproducción en curso",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Muestra la canción que está sonando en Gurru Music y permite pausarla desde la notificación.");
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
        return builder.build();
    }
}
