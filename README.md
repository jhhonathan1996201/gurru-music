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
4. Para música de YouTube, en **Biblioteca → Buscar música** escribe el
   nombre de una canción o un artista: salen los resultados y al tocar uno
   eliges **Escuchar** (solo audio, sin video) o **Descargar** (se guarda
   en Música/Gurru Music con yt-dlp dentro del APK). Nada suena ni se
   descarga solo. Ya no se usa el link de YouTube.
5. En la pantalla de reproducción ves la cola "A continuación" con las
   canciones que siguen. Toca cualquiera para saltar a ella. La barra de
   progreso sirve para adelantar o retroceder tanto en canciones locales
   como en las de YouTube, y las barritas del ecualizador se animan en
   las dos.

Tus archivos MP3 no se suben a ningún lado: se reproducen directo desde
tu celular.

## Nota honesta sobre el volumen y las carpetas

- La barra de volumen se quitó de la pantalla: el volumen de la música
  queda en 80% al arrancar. Se puede silenciar con el botón de la bocina
  del reproductor, y el volumen del sistema del teléfono (timbres y
  avisos) se sigue ajustando con los botones físicos del celular.
- Para que la app recuerde tu carpeta de música entre sesiones sin tener
  que elegirla cada vez, hará falta un **plugin nativo de Capacitor**
  después. El envoltorio web por sí solo no resuelve eso: es el siguiente
  paso, no está incluido en este proyecto.
- La app usa las fuentes de Google Fonts de internet la primera vez; sin
  conexión usa las fuentes de respaldo del sistema y se sigue viendo bien.

## Buscador de YouTube (sin link)

- En **Biblioteca → Buscar música** se escribe la canción o el artista.
  La búsqueda la hace yt-dlp dentro del APK (método `searchYouTube` del
  plugin `YtDlpDownloader`, con `ytsearch`, sin API key ni servidores de
  terceros); devuelve hasta 10 resultados con miniatura, título, canal y
  duración.
- Al tocar un resultado se abre la ventana para elegir **Escuchar**,
  **Descargar** o **Solo agregarlo a la Biblioteca**. Al descargar, la
  canción se guarda en Música/Gurru Music (MP3 o M4A, máxima calidad) y
  el avance se ve en **Descargas**, canción por canción.
- La barra de progreso en YouTube funciona con `seekTo` del reproductor
  oficial: el tiempo se lee cada medio segundo (`getCurrentTime` /
  `getDuration`) porque el iframe no dispara `timeupdate`.
- El ecualizador se anima también en online: como en YouTube no hay un
  `<audio>` al que conectar el analizador, las barritas se dibujan con
  ondas generadas (mismo dibujo que en local); en pausa quedan en la
  línea base, igual que en local.

## Reproducción en segundo plano

- Al sonar una canción, el plugin nativo `BackgroundAudio` arranca un
  servicio en primer plano (`BackgroundAudioService`, tipo
  `mediaPlayback`) con una notificación que muestra la canción actual
  y botones de **Pausa / Siguiente / Detener**. El servicio sostiene
  un wake lock parcial para que la CPU no se duerma con la pantalla
  apagada. El audio en sí sigue sonando en el WebView (`<audio>` para
  MP3 locales, reproductor de YouTube para online).
- El servicio se detiene con el botón Detener de la notificación o con
  el botón de parar del reproductor. Si la música se pausa, la
  notificación queda con el botón de reproducir para retomarla.
- **Importante en OPPO/ColorOS:** el sistema puede matar la app en
  segundo plano aunque tenga el servicio. Si la música se corta al
  apagar la pantalla, ve a **Ajustes > Batería > Gurru Music** y
  permite la actividad en segundo plano / sin restricciones de batería.

## Si cambias algo del reproductor

Edita `www/index.html` y luego corre `npx cap sync` para copiar los cambios
al proyecto Android. No edites los archivos dentro de
`android/app/src/main/assets/public/` a mano: se sobrescriben en cada sync.

## El puente nativo: por qué hace falta `www/capacitor.js`

La app es HTML sin empaquetador (sin bundler). El runtime que Android
inyecta en la página (`native-bridge.js`) trae `getPlatform`,
`toNative` y `Capacitor.Plugins.*`, pero **no trae `registerPlugin`**
(esa función vive en `@capacitor/core` y hay que cargarla en la página).
Sin `www/capacitor.js` la app no podía usar ningún plugin nativo: la
búsqueda y las descargas de YouTube decían "no es la app nativa", el
audio en segundo plano no arrancaba y el botón atrás no cerraba la app
desde el reproductor. El archivo `www/capacitor.js` es el UMD oficial
de `@capacitor/core` (8 KB) y se carga en el `<head>` después del script
inyectado: completa `window.Capacitor` con `registerPlugin` sin tocar
el resto del runtime. No lo borres. Comprobado en simulación: con él,
`isAvailable`, `searchYouTube`, `addListener("backButton")` y
`BackgroundAudio.start` llegan al lado nativo.

## Letra de la canción

El botón **Letra** del reproductor abre la letra de la canción que suena,
buscada en LRCLIB (`https://lrclib.net`, gratis y sin clave) por artista +
título. Cuando la letra trae tiempos, la línea que suena se resalta y se
desplaza sola (sirve para MP3 locales y para YouTube). Si no se encuentra,
la ventana lo dice y ofrece reintentar. Las letras se guardan en memoria
por canción, así que abrirla de nuevo es instantáneo. El botón atrás del
teléfono cierra primero la letra y luego vuelve al reproductor.
