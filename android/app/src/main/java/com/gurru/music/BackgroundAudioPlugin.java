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
 * plano (BackgroundAudioService) con notificación (pausa / siguiente /
 * detener) y un wake lock parcial, así el sistema sabe que la app está
 * reproduciendo y no la mata.
 *
 * La música en sí sigue sonando en el <audio> del WebView (archivos MP3
 * locales) o en el reproductor de YouTube; el servicio solo mantiene
 * vivo el proceso y da controles en la notificación y en la pantalla de
 * bloqueo (a través de las acciones de la notificación).
 */
@CapacitorPlugin(name = "BackgroundAudio")
public class BackgroundAudioPlugin extends Plugin {

    public static final String ACTION_TOGGLE = "com.gurru.music.BG_TOGGLE";
    public static final String ACTION_NEXT = "com.gurru.music.BG_NEXT";
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
                } else if (ACTION_STOP.equals(action)) {
                    getBridge().eval("if (window.__gurruStop) { try { window.__gurruStop(); } catch (e) {} }", null);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TOGGLE);
        filter.addAction(ACTION_NEXT);
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

    /** Arranca el servicio en primer plano con la canción actual. */
    @PluginMethod
    public void enable(PluginCall call) {
        Intent intent = new Intent(getContext(), BackgroundAudioService.class);
        intent.setAction(BackgroundAudioService.ACTION_START);
        intent.putExtra("title", call.getString("title", "Gurru Music"));
        intent.putExtra("artist", call.getString("artist", ""));
        intent.putExtra("playing", call.getBoolean("playing", true));
        startServiceCompat(intent);
        JSObject ret = new JSObject();
        ret.put("enabled", true);
        call.resolve(ret);
    }

    /** Actualiza la canción y el estado (sonando/pausa) de la notificación. */
    @PluginMethod
    public void updateTrack(PluginCall call) {
        Intent intent = new Intent(getContext(), BackgroundAudioService.class);
        intent.setAction(BackgroundAudioService.ACTION_UPDATE);
        intent.putExtra("title", call.getString("title", "Gurru Music"));
        intent.putExtra("artist", call.getString("artist", ""));
        intent.putExtra("playing", call.getBoolean("playing", true));
        startServiceCompat(intent);
        call.resolve();
    }

    /** Detiene el servicio y quita la notificación. */
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
