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

- En la pestaña **YouTube** se escribe la canción o el artista.
  La búsqueda la hace yt-dlp dentro del APK (método `searchYouTube` del
  plugin `YtDlpDownloader`, con `ytsearch`, sin API key ni servidores de
  terceros); devuelve hasta 20 resultados con miniatura, título, canal y
  duración.
- Al tocar un resultado se abre la ventana para elegir **Escuchar**,
  **Descargar** o **Solo agregarlo a la Biblioteca**. Al descargar, la
  canción se guarda en Música/Gurru Music (MP3 o M4A, máxima calidad)
  con su metadata original (título, artista y portada, con
  `--add-metadata` y `--embed-thumbnail`) y el avance se ve en
  **Descargas**, canción por canción.
- Al escuchar, el audio NO pasa por el reproductor oficial de YouTube
  (iframe): ese reproductor pausa el audio cuando la app pasa a
  segundo plano (restricción de YouTube para no suscriptores de
  Premium). El audio se pide directo a yt-dlp dentro del APK (método
  `getAudioUrl`, `yt-dlp -g -f bestaudio`) y suena en un `<audio>`
  normal, igual que un MP3 local: sigue en segundo plano, se adelanta
  con la barra y la MediaSession lo controla. La URL del audio caduca
  en unas horas y no se guarda: se vuelve a pedir cada vez que suena
  la canción (si caducó a media reproducción, se pide una nueva una
  sola vez). Cada petición de audio usa su propio id de proceso
  (`gurru-stream-N`) para que dos canciones seguidas no choquen.
- **Respaldo:** si el audio directo falla, la canción suena con el
  reproductor oficial de YouTube (iframe, `playYtIframe`) en vez de
  quedarse en silencio; arriba se avisa el motivo del fallo. Ahí sí
  se pausa al salir de la app, pero al menos suena. El tiempo, el
  adelanto, el volumen y play/pausa funcionan con los dos
  reproductores (las funciones revisan primero si hay iframe).
- La barra de progreso en YouTube funciona con `currentTime` del
  `<audio>` (el tiempo se lee cada medio segundo); el ecualizador se
  anima también en online con ondas generadas (mismo dibujo que en
  local); en pausa quedan en la línea base, igual que en local.

## Reproducción en segundo plano

- Al sonar una canción, el plugin nativo `BackgroundAudio` arranca un
  servicio en primer plano (`BackgroundAudioService`, tipo
  `mediaPlayback`) que publica una **MediaSession real** (título,
  artista, álbum, portada, duración y posición) con notificación
  **MediaStyle** y botones de **Anterior / Pausa / Siguiente /
  Detener**. Así el sistema reconoce a Gurru Music como reproductor:
  controles en la barra de notificaciones, en la pantalla de bloqueo
  y en Bluetooth, con la barra de progreso. El servicio sostiene un
  wake lock parcial para que la CPU no se duerma con la pantalla
  apagada. El audio en sí sigue sonando en el WebView (`<audio>` para
  MP3 locales, reproductor de YouTube para online). La página
  sincroniza la sesión con `updateSession` al cambiar de canción,
  al pausar/reproducir, al buscar y cada 10 s mientras suena; las
  acciones de la sesión vuelven a la página con `window.__gurru*`
  (reproducir/pausar, siguiente, anterior, buscar, detener).
- El servicio se detiene con el botón Detener de la notificación o con
  el botón de parar del reproductor. Si la música se pausa, la
  notificación queda con el botón de reproducir para retomarla.
- **Importante en OPPO/ColorOS:** el sistema puede matar la app en
  segundo plano aunque tenga el servicio. Si la música se corta al
  apagar la pantalla, ve a **Ajustes > Batería > Gurru Music** y
  permite la actividad en segundo plano / sin restricciones de batería.

## La biblioteca se guarda al cerrar la app

La biblioteca (canciones de YouTube y descargadas), la canción actual
y su posición, y el historial de Descargas se guardan en
`localStorage` y se restauran al abrir la app. OJO: las canciones de
YouTube no tienen `url` (su identidad es el `videoId`), así que al
principio no se guardaban y la lista de YouTube se borraba al cerrar;
ahora `serializeTrack` guarda las de YouTube por su `videoId` (la URL
del audio no se guarda porque caduca, se vuelve a pedir al sonar).
Las canciones locales del escaneo se vuelven a buscar solas; las del
selector de carpetas usan URLs `blob:` que no sobreviven al cierre y
no se guardan (el escaneo las vuelve a encontrar). En Descargas hay
un botón **Borrar historial** que borra solo el historial: las
canciones guardadas siguen en el teléfono y en la Biblioteca. Si la
app se cerró a media descarga, esa entrada queda como detenida al
reabrir, no como "descargando" para siempre.

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

## Música local automática (sin elegir carpetas)

Al abrir la app, el APK busca solo todas las canciones del teléfono con
el plugin nativo `LocalMusic` (lee MediaStore: título, artista, álbum,
duración y portada). La primera vez Android pregunta el permiso para leer
la música; si se niega, la app lo dice y se puede reintentar con el botón
**Actualizar** de Biblioteca. OJO: `Plugin.hasPermission(alias)` está
obsoleto y recibe el nombre del permiso, no el alias — `hasPermission("media")`
siempre daba falso, así que tras conceder el permiso el escaneo se
rechazaba como "sin permiso"; ahora el plugin revisa el permiso real
(`READ_MEDIA_AUDIO` en Android 13+ / `READ_EXTERNAL_STORAGE` antes) con
`Context.checkSelfPermission`, y expone `checkPermission()` para que la
página reintente el escaneo al abrir Biblioteca si el primero se atascó.
El filtro de MediaStore acepta canciones marcadas como música o audios
de 60 s o más aunque no estén marcados. Los audios de WhatsApp quedan
fuera: se descartan los archivos cuyo nombre empieza con "aud" o "ptt"
y los que viven en una ruta con "whatsapp". Si el escaneo no encuentra nada,
queda el botón **Elegir carpeta** como respaldo. Las canciones de YouTube
ya agregadas y las descargadas se conservan; las locales se reemplazan
por el resultado nuevo (las descargadas no se duplican: se detectan por
título + artista aunque el archivo se llame "Artista - Título"). En la
versión web no hay MediaStore y se sigue eligiendo carpeta o archivos a
mano.

## Pestaña YouTube

La búsqueda de YouTube vive en su propia pestaña, junto a Descargas y
Biblioteca (la misma fila de pestañas está en las tres vistas). Ahí la
lista de resultados tiene todo el espacio y se puede deslizar por ella.
Tocar un resultado lo agrega a la Biblioteca y abre la ventana para
elegir Escuchar, Descargar o Solo agregar, como antes.

## Radio: seguir con sugeridos de YouTube

Cuando un tema online (YouTube) termina y no queda nada en la cola, la
app sigue sola con música sugerida: busca en YouTube más música del mismo
artista (o del título, si no hay artista) y suena el primer sugerido;
al terminar ese, el siguiente, y así. YouTube no entrega su lista de
"A continuación" a yt-dlp, así que la radio se arma con la búsqueda de
YouTube del artista. La música local se detiene al final de su cola como
siempre; el modo repetir tampoco cambia.
