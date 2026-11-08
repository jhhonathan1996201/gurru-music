# Gurru Music — Proyecto Capacitor para Android

Reproductor de música estilo Nokia XpressMusic, con tus archivos MP3 locales.
Este proyecto envuelve la versión web del reproductor (la que dice **Gurru Music**)
dentro de una app Android nativa con Capacitor, para que puedas generar el APK
en Android Studio.

> El APK **no** está incluido aquí. Este proyecto está listo para abrirlo en
> Android Studio y generar el APK ahí. Abajo están los pasos.

## Qué trae

- `www/index.html`: el reproductor completo (estilo azul tipo XpressMusic de la
  foto, con el nombre **Gurru Music**, selector de carpeta para cargar todos
  tus MP3 de una vez, y cola "A continuación" con las canciones que siguen).
- `capacitor.config.json`: id de la app `com.gurru.music`, nombre `Gurru Music`,
  carpeta web `www`.
- `package.json`: dependencias de Capacitor 7.
- `android/`: proyecto Android generado por Capacitor (se crea con
  `npx cap add android` y se actualiza con `npx cap sync`).
- **Descargas de YouTube con yt-dlp DENTRO del APK** (sin Termux ni
  servidor): plugin nativo `YtDlpDownloaderPlugin` (youtubedl-android
  0.17.x + FFmpeg empaquetados). En el APK, el botón Descargar guarda
  la canción en **Música/Gurru Music** en MP3 o M4A, a máxima calidad,
  con el porcentaje real en el apartado Descargas. Detalles, tamaño y
  limitaciones en `NOTAS-YTDLP.md`. La app **no** es apta para Google
  Play: se distribuye por sideload / GitHub Releases.

## Compilar sin computadora: GitHub Actions

También se puede compilar en la nube desde el celular, sin Node.js ni
Android Studio: sube este proyecto a un repo privado de GitHub
(incluye `.github/`) y usa **Actions → Build APK → Run workflow**.
El APK queda como artefacto `gurru-music-apk`. El workflow está en
`.github/workflows/build-apk.yml`.

## Qué necesitas en tu computadora

1. **Node.js** (versión 20 o más reciente). Descárgalo en https://nodejs.org
   y comprueba con `node -v` y `npm -v` en la terminal.
2. **Android Studio** con el SDK de Android instalado.
   Descárgalo en https://developer.android.com/studio
3. Un celular Android con **depuración USB** activada si quieres probar la app
   directo en tu teléfono (Ajustes > Opciones de desarrollador > Depuración USB).
   Si no ves "Opciones de desarrollador", toca 7 veces "Número de compilación"
   en Ajustes > Acerca del teléfono.

## Pasos para generar el APK

Desde la carpeta de este proyecto, en la terminal:

```bash
# 1. Instalar las dependencias (solo la primera vez)
npm install

# 2. Sincronizar la parte web con el proyecto Android
#    (hazlo cada vez que cambies algo en www/)
npx cap sync

# 3. Abrir el proyecto en Android Studio
npx cap open android
```

En Android Studio:

- Espera a que termine la sincronización de Gradle (la primera vez tarda
  varios minutos y necesita internet).
- Para probar en tu celular: conéctalo por USB con depuración USB activada
  y pulsa el botón de reproducir (Run).
- Para generar el APK instalable: ve a **Build > Build App Bundle(s) / APK(s) >
  Build APK(s)**. El archivo queda en `android/app/build/outputs/apk/debug/`.
- Para publicarlo en Google Play: usa **Build > Generate Signed Bundle / APK**,
  elige Android App Bundle (AAB) y firma con tu keystore. Guarda bien ese
  keystore y su contraseña: sin él no podrás actualizar la app después.

## Cómo se usa la app

1. Abre Gurru Music en tu celular.
2. Pulsa **Elegir carpeta** y selecciona la carpeta donde guardas tu música.
   Se cargan todas las canciones y subcarpetas de una vez, en orden.
3. También puedes elegir archivos sueltos si prefieres.
4. En la pantalla de reproducción ves, bajo el volumen, la cola
   "A continuación" con las canciones que siguen. Toca cualquiera para
   saltar a ella.

Tus archivos MP3 no se suben a ningún lado: se reproducen directo desde
tu celular.

## Nota honesta sobre el volumen y las carpetas

- El deslizador de volumen de esta versión controla el **volumen de
  reproducción de la música dentro de la app**, y se mantiene sincronizado
  con el audio. No mueve el volumen del sistema del teléfono (timbres y
  avisos): ese se sigue ajustando con los botones físicos del celular.
- Para controlar el volumen del sistema y para que la app recuerde tu
  carpeta de música entre sesiones sin tener que elegirla cada vez, harán
  falta **plugins nativos de Capacitor** después. El envoltorio web por sí
  solo no resuelve eso: es el siguiente paso, no está incluido en este
  proyecto.
- La app usa las fuentes de Google Fonts de internet la primera vez; sin
  conexión usa las fuentes de respaldo del sistema y se sigue viendo bien.

## Si cambias algo del reproductor

Edita `www/index.html` y luego corre `npx cap sync` para copiar los cambios
al proyecto Android. No edites los archivos dentro de
`android/app/src/main/assets/public/` a mano: se sobrescriben en cada sync.
