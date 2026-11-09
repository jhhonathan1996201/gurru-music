package com.gurru.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Reproducción en segundo plano para Gurru Music.
 *
 * El WebView de Capacitor ya sigue vivo cuando la app pasa a segundo plano
 * (KeepRunning), pero sin un servicio en primer plano Android — y sobre
 * todo ColorOS/OPPO — mata el proceso y la música se corta al apagar la
 * pantalla o cambiar de app. Este plugin arranca un servicio en primer
 * plano (BackgroundAudioService) con una MediaSession real y notificación
 * MediaStyle, así el sistema reconoce a Gurru Music como reproductor
 * (controles en la barra de notificaciones, en la pantalla de bloqueo y
 * en Bluetooth) y no mata la reproducción.
 *
 * La música en sí sigue sonando en el <audio> del WebView (archivos MP3
 * locales) o en el reproductor de YouTube; el servicio solo mantiene
 * vivo el proceso, publica la sesión de audio y da los controles.
 * Cada acción (reproducir/pausar, siguiente, anterior, buscar, detener)
 * vuelve a la página con window.__gurru* a través de este receptor.
 */
@CapacitorPlugin(name = "BackgroundAudio")
public class BackgroundAudioPlugin extends Plugin {

    public static final String ACTION_TOGGLE = "com.gurru.music.BG_TOGGLE";
    public static final String ACTION_NEXT = "com.gurru.music.BG_NEXT";
    public static final String ACTION_PREV = "com.gurru.music.BG_PREV";
    public static final String ACTION_SEEK = "com.gurru.music.BG_SEEK";
    public static final String ACTION_STOP = "com.gurru.music.BG_STOP";

    private BroadcastReceiver receiver;

    @Override
    public void load() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getBridge() == null) return;
                String action = intent.getAction();
                if (ACTION_TOGGLE.equals(action)) {
                    getBridge().eval("if (window.__gurruTogglePlay) { try { window.__gurruTogglePlay(); } catch (e) {} }", null);
                } else if (ACTION_NEXT.equals(action)) {
                    getBridge().eval("if (window.__gurruNext) { try { window.__gurruNext(); } catch (e) {} }", null);
                } else if (ACTION_PREV.equals(action)) {
                    getBridge().eval("if (window.__gurruPrev) { try { window.__gurruPrev(); } catch (e) {} }", null);
                } else if (ACTION_SEEK.equals(action)) {
                    long ms = intent.getLongExtra("positionMs", 0);
                    getBridge().eval("if (window.__gurruSeek) { try { window.__gurruSeek(" + ms + "); } catch (e) {} }", null);
                } else if (ACTION_STOP.equals(action)) {
                    getBridge().eval("if (window.__gurruStop) { try { window.__gurruStop(); } catch (e) {} }", null);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TOGGLE);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PREV);
        filter.addAction(ACTION_SEEK);
        filter.addAction(ACTION_STOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getContext().registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getContext().registerReceiver(receiver, filter);
        }
    }

    @Override
    protected void handleOnDestroy() {
        if (receiver != null) {
            try { getContext().unregisterReceiver(receiver); } catch (Exception ignored) {}
            receiver = null;
        }
    }

    /** Arranca el servicio y publica la sesión de audio con la canción actual. */
    @PluginMethod
    public void updateSession(PluginCall call) {
        Intent intent = new Intent(getContext(), BackgroundAudioService.class);
        intent.setAction(BackgroundAudioService.ACTION_SESSION);
        intent.putExtra("title", call.getString("title", "Gurru Music"));
        intent.putExtra("artist", call.getString("artist", ""));
        intent.putExtra("album", call.getString("album", ""));
        intent.putExtra("art", call.getString("art", ""));
        intent.putExtra("durationMs", call.getInt("duration", 0));
        intent.putExtra("positionMs", call.getInt("position", 0));
        intent.putExtra("playing", call.getBoolean("playing", true));
        startServiceCompat(intent);
        JSObject ret = new JSObject();
        ret.put("enabled", true);
        call.resolve(ret);
    }

    /** Detiene el servicio y quita la notificación y la sesión. */
    @PluginMethod
    public void disable(PluginCall call) {
        try {
            Intent intent = new Intent(getContext(), BackgroundAudioService.class);
            getContext().stopService(intent);
        } catch (Exception ignored) {}
        call.resolve();
    }

    private void startServiceCompat(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(intent);
        } else {
            getContext().startService(intent);
        }
    }
}
