package com.gurru.music;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // El plugin nativo de descargas (yt-dlp dentro del APK) debe
        // registrarse ANTES de super.onCreate, que es cuando se crea el Bridge.
        registerPlugin(YtDlpDownloaderPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
