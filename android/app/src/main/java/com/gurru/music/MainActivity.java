package com.gurru.music;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // El plugin nativo de descargas (yt-dlp dentro del APK) debe
        // registrarse ANTES de super.onCreate, que es cuando se crea el Bridge.
        registerPlugin(YtDlpDownloaderPlugin.class);
        // Inicio de sesión en Facebook dentro de la app (NUEVO13, prueba).
        registerPlugin(FacebookAuthPlugin.class);
        // Búsqueda automática de la música del teléfono (MediaStore).
        registerPlugin(LocalMusicPlugin.class);
        // Servicio en primer plano para que la música siga sonando
        // con la app en segundo plano o la pantalla apagada.
        registerPlugin(BackgroundAudioPlugin.class);
        super.onCreate(savedInstanceState);
        // Pantalla completa: oculta la barra de estado del sistema (hora,
        // señal y batería) mientras la app está abierta. Se vuelve a ver
        // deslizando hacia abajo desde arriba, como en los videos.
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                getWindow().setDecorFitsSystemWindows(false);
                android.view.WindowInsetsController c = getWindow().getInsetsController();
                if (c != null) {
                    c.hide(android.view.WindowInsets.Type.statusBars());
                    c.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        } catch (Exception ignored) {}
    }
}
