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
- La búsqueda es en **YouTube Music** (`ytmsearch`): salen las
  canciones (audio oficial, mejor calidad) y no videoclips. Si esa
  búsqueda falla o no trae nada, se busca en YouTube normal
  (`ytsearch`). En los resultados solo se muestran videos (los
  álbumes y artistas de YouTube Music, que son listas, se filtran).
  Al tocar un resultado se abre la ventana para elegir **Escuchar**,
  **Descargar** o **Solo agregarlo a la Biblioteca**. Al descargar, la
  canción se guarda en Música/Gurru Music (MP3 o M4A, máxima calidad)
  con su metadata original (título, artista y portada, con
  `--add-metadata` y `--embed-thumbnail`) y el avance se ve en
  **Descargas**, canción por canción.
- Al escuchar, el audio NO pasa por el reproductor oficial de YouTube
  (iframe): ese reproductor pausa el audio cuando la app pasa a
  segundo plano (restricción de YouTube para no suscriptores de
  Premium). El audio se pide directo a yt-dlp dentro del APK (método
  `getAudioUrl`, `yt-dlp --dump-json` con cliente Android) y se elige
  el mejor formato solo-audio por bitrate; suena en un `<audio>`
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

## Responsivo: la app se adapta a cada teléfono

Los botones, las portadas y las letras usan `clamp()` con el ancho de
la pantalla: en un teléfono chico nada se desborda, y en uno grande o
una tablet nada se ve diminuto. En pantallas bajas (teléfono
apaisado) todo se compacta para que nada se tape; en pantallas
anchas (tablet, ≥700px) la app aprovecha el ancho (hasta 560px) sin
perder el estilo. Las filas de Biblioteca tenían una columna de 28px
heredada del diseño con números que recortaba la portada; ahora la
portada se ve completa (4 columnas: portada, datos, play, menú). Al
tocar una fila de la lista suena la canción de inmediato (y el botón
de play de la fila hace lo mismo); al tocar un resultado de YouTube
se pregunta Escuchar o Descargar.

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

La búsqueda de YouTube vive en su propia sección, junto a Descargas y
Biblioteca, en la barra inferior fija. Ahí la lista de resultados tiene
todo el espacio y se puede deslizar por ella. Tocar un resultado lo
agrega a la Biblioteca y abre la ventana para elegir Escuchar, Descargar
o Solo agregar, como antes.

## Radio: seguir con sugeridos de YouTube

Cuando un tema online (YouTube) termina y no queda nada en la cola, la
app sigue sola con música sugerida: busca en YouTube más música del mismo
artista (o del título, si no hay artista) y suena el primer sugerido;
al terminar ese, el siguiente, y así. YouTube no entrega su lista de
"A continuación" a yt-dlp, así que la radio se arma con la búsqueda de
YouTube del artista. La música local se detiene al final de su cola como
siempre; el modo repetir tampoco cambia.

## NUEVO7 — Rediseño UX/UI (2026-11-09)

Jonathan dio el diseño y levantó el hold el 2026-11-09 ~20:16 CST.
Todo se aplicó sobre NUEVO6, solo en `www/index.html` (sin cambios en
los plugins). Decisiones que él no contestó y se tomaron con criterio:
la barra inferior **reemplaza** las pestañas de arriba (no hay dos
navegaciones); las tarjetas de la cuadrícula muestran portada + título
+ artista, 2 columnas (3 en tablet); reordenar la cola es con botones
↑ ↓ por fila (un toque por movimiento, sin gestos que choquen con el
scroll).

- **Barra inferior fija:** Descargas / Biblioteca / YouTube siempre
  visibles, para el pulgar; la sección activa se marca en cian. Las
  pestañas de arriba se quitaron. El botón "Vaciar" pasó al menú de
  filtro (⋮ filtro → "Vaciar biblioteca").
- **Toasts (avisos flotantes):** los errores de descarga, búsqueda,
  audio directo y actualización de yt-dlp ya no son solo texto
  estático: aparece un aviso flotante con botón **Reintentar** y se
  puede **tocar para expandir** y leer el motivo completo. El error
  de una descarga también se puede tocar en el apartado Descargas
  para ver el motivo completo.
- **Controles 48 dp:** los botones de transporte miden mínimo 48×48
  px y el pulgar de la barra de progreso es de 26 px (antes 18 px).
  Los textos secundarios subieron de tamaño (9.5–11.5 px → 11–13 px);
  el contraste ya pasaba WCAG AA (13.35:1), el problema era el tamaño.
- **Cola por origen:** "A continuación" ya no mezcla YouTube con la
  Biblioteca: si suena YouTube, solo salen las de YouTube; si suena
  la Biblioteca, solo las locales. Siguiente/Anterior y aleatorio
  también se mueven solo dentro de la cola actual. El contador
  (12/305) y el de arriba son la posición dentro de esa cola.
- **Cola con número y reorden:** cada fila lleva su número (1, 2, 3…)
  y botones ↑ ↓ para subirla o bajarla con un toque.
- **Biblioteca:** ordenar por **álbum** (nuevo) y **vista de
  cuadrícula** (botón nuevo junto a filtro y orden): tarjetas con
  portada, título y artista; se recuerda la vista elegida.
- **Limpieza de metadatos:** los títulos se muestran sin el ruido
  técnico ("[Official Video]", "(1080p)", "HD"…); los datos guardados
  no se tocan.
- **Botón Letra visible:** salió del grupo de transporte (ahí quedaba
  escondido) y ahora es un botón centrado "♪ Letra" debajo de los
  controles, de 44 px de alto.
- **Carátulas:** la canción sin portada muestra el logo de Gurru Music
  (la patita con GM) en el reproductor, en las filas y en los
  resultados de búsqueda, en vez de un hueco vacío.
- **Editar datos:** en el menú ⋮ de cada canción, "Editar datos /
  carátula" abre un diálogo para corregir título, artista y álbum, y
  un botón "Buscar carátula" que busca la portada en iTunes (gratis,
  sin clave) por artista + título. Los cambios se guardan en el
  teléfono y se aplican cada vez que la canción vuelve a aparecer
  (escaneo, restauración o nueva búsqueda); el archivo original no se
  modifica.
- **Distorsión al salir/entrar (mitigación):** el visualizador crea
  el contexto de audio con `latencyHint: "playback"` y al volver a la
  app, si el contexto quedó suspendido por el sistema, se reanuda
  (también al darle play). Es una mitigación: si la distorsión sigue,
  hay que saber si pasa con música local, con YouTube o con las dos.

Validado: JS `node --check` OK (2 bloques), todos los ids
referenciados existen, lógica de cola y de limpieza probada en Node
(8/8 casos de títulos; cola YouTube [1,3,5] y local [0,2,4] con 6
canciones mezcladas; subir una canción en la cola la intercambia bien),
`npx cap sync` OK, md5 de `www/index.html` == copia en assets.

## NUEVO8 — El error rojo ya se puede leer y tocar + sugerencias en la cola (2026-11-09)

Jonathan reportó ~21:21 CST que el YouTube online sigue cortándose
en segundo plano y que la cola se sigue mezclando, y ~21:23 CST que
el error rojo ("no se puede cargar el YouTube") se queda fijo, no
desaparece y no se puede tocar para verlo de nuevo. No confirmó si
instaló el APK de NUEVO7, así que este ZIP trae también todo lo de
NUEVO7 (la separación de la cola por origen ya estaba en el código y
se verificó: al sonar YouTube la cola solo muestra YouTube).

- **La barra roja ya se puede tocar:** el aviso "Audio directo falló
  (…)" se cortaba con "…" porque la barra es de una sola línea y no
  tenía forma de cerrarse ni de verse completo. Ahora al tocar la
  barra vuelve a salir el error completo con su detalle y su botón
  Reintentar, y tiene una ✕ para cerrarla. Lo mismo con el mensaje
  de error de la búsqueda de YouTube: al tocarlo vuelve a mostrarse
  completo. El aviso flotante sigue cerrándose solo a los 10 s, pero
  la barra queda como recordatorio para volver a verlo.
- **Sugerencias en "A continuación":** al sonar un tema de YouTube,
  si quedan menos de 3 canciones por delante en la cola, la app busca
  sugerencias del mismo artista (igual que la radio) y las agrega a
  la cola con la marca "Sugerencia". Se pueden tocar para saltar a
  ellas como cualquier canción de la cola; al sonar una sugerencia se
  buscan más si hacen falta. En música local no se agregan.
- **Audio directo con segundo intento:** `getAudioUrl` ahora acepta
  el cliente de YouTube (`android` por defecto; también `ios`, `web`
  o `tv`). Al reproducir, la app pide el audio primero con el
  cliente Android y si falla reintenta con el cliente iOS antes de
  caer al reproductor oficial, porque un cliente puede no devolver
  la dirección del audio para un video y el otro sí. Si los dos
  fallan, el aviso muestra los dos motivos (Android: … · iOS: …).
- **Para diagnosticar el segundo plano:** con este cambio, al tocar
  la barra roja se lee el motivo exacto del fallo. Ese texto es el
  que hace falta para arreglar de raíz que se corte al salir de la
  app; si el audio directo ya funciona con el segundo intento, la
  música seguirá sonando al salir.

Validado: JS `node --check` OK (2 bloques), todos los ids
referenciados existen, lógica de sugerencias probada en Node (cola
YouTube [1,3] + 5 sugerencias = [1,3,4,5,6,7,8]; no duplica al
repetir; con 5 por delante no agrega más; en local no agrega),
plugins compilan con javac, `npx cap sync` OK, md5 de
`www/index.html` == copia en assets.

## NUEVO9 — El buscador de YouTube ya encuentra canciones (2026-11-09)

Jonathan probó el APK de NUEVO8 y reportó que el buscador de YouTube
decía "No encontré nada para … Prueba con otro nombre" con cualquier
búsqueda. Causa encontrada y comprobada con yt-dlp real: dos errores
juntos en el plugin. (1) El filtro de resultados pedía `_type ==
"video"`, pero con `--flat-playlist` yt-dlp devuelve las entradas
con `_type == "url"`, así que se tiraban TODOS los resultados (5 de
5 en la prueba con "Belanova Rosa Pastel"). (2) La primera búsqueda
usaba el prefijo `ytmsearch:`, que no existe en yt-dlp (lo rechaza
como "Unsupported url scheme"), así que siempre fallaba y se caía
al respaldo, que también se vaciaba por el filtro. Por eso el
mensaje era "no encontré nada" y no un error: las dos búsquedas
devolvían cero sin romperse. La búsqueda funcionaba antes de NUEVO6
porque no existía ese filtro; NUEVO6 lo agregó y nunca se probó
hasta ahora.

- **Filtro arreglado:** se aceptan entradas `video` y `url`, pero
  solo si el id parece un video de YouTube (11 caracteres) y el
  título no está vacío. Así se siguen excluyendo los álbumes
  (listas), los artistas (canales) y las radios que salen en
  YouTube Music, que no son canciones.
- **YouTube Music por su URL:** la primera búsqueda ya no usa el
  prefijo `ytmsearch:`; usa la URL de búsqueda de
  `music.youtube.com/search?q=…` (con la búsqueda codificada). Si
  falla o no trae nada, se busca en YouTube normal (ytsearch), como
  antes. La radio y las sugerencias usan el mismo buscador, así que
  también se arreglan.
- Los resultados se limitan a 20 como antes.

Validado: búsqueda real con yt-dlp 2026.10.26 en esta sesión —
con el filtro viejo: 0 resultados en ytsearch y 0 en YouTube
Music; con el filtro nuevo: 5 resultados en ytsearch y 11 en
YouTube Music (canciones de Belanova), y los 17 excluidos en
YouTube Music son artistas, listas y radios, ninguna canción.
Plugin compila con javac, `npx cap sync` OK, md5 de
`www/index.html` == copia en assets (sin cambios en la web).

## Ronda NUEVO10 (2026-11-09 ~22:20 CST)

Jonathan confirmó a las 22:05 CST que NUEVO9 arregló el buscador
("Ok ya se arregló") y levantó el hold del caché invisible: "ahora
implementemos esto porfa". En el mismo mensaje pidió dos arreglos
(cola "A continuación" mal centrada en local y ecualizador distinto
en online). Después mandó la captura con el error exacto del audio
directo, pidió sugerencias por ARTISTA, que la última búsqueda de
YouTube permanezca al salir y entrar, y más fuentes de letras.

### 1) Caché invisible para YouTube online (lo principal)
- **El error que lo motivó** (captura de Jonathan, video
  `324UfrjhL9Q` "Por Esta Vez" de Belanova): "Audio directo falló.
  Android: ERROR: [youtube] 324UfrjhL9Q: Requested format is not
  available. Use --list-formats for a list of available formats ·
  iOS: ERROR: [youtube] 324UfrjhL9Q: Requested format is not
  available…". Reproducido y comprobado con yt-dlp real en esta
  sesión: con yt-dlp 2026.10.26 `--dump-json` (sin `-f`) devuelve
  el video bien (formatos: storyboards + formato 18 combinado
  360p, sin formatos solo-audio); con las versiones viejas
  (2023.10.07, 2024.10.07) el mismo video da "Sign in to confirm
  you're not a bot". El yt-dlp del APK es viejo, así que el audio
  directo falla según el video y la versión. El caché usa el camino
  de la descarga (`-f bestaudio/best`, que sí funciona) y por eso
  lo esquiva.
- **Cómo funciona:** al darle play a una canción de YouTube, la app
  muestra "Preparando «…»… N%" y descarga el audio en silencio a la
  carpeta privada de la app (`getCacheDir()/ytcache`, invisible para
  MediaStore y para el usuario) con un método nuevo del plugin
  `cacheAudio({url, videoId})` (sin `--extract-audio`: el archivo
  original m4a/mp4 suena directo y la preparación es rápida) en su
  propio hilo (`cacheExecutor`, separado del de Descargas, para que
  no se estorben). Al terminar, la canción suena en el `<audio>`
  local como un MP3: sigue en segundo plano, se adelanta con la
  barra, la sesión del sistema la controla y el ecualizador usa el
  audio real. Si el caché falla, suena por el camino de siempre
  (audio directo o reproductor oficial).
- **Sigue pareciendo YouTube online:** el tipo de la canción no
  cambia (`"youtube"`), así que conserva su botón rojo, su cola, sus
  sugerencias y su pestaña. El caché NO aparece en Descargas ni en
  Música/Gurru Music (eso sigue siendo solo para el botón Descargar,
  que guarda para siempre). `cacheUrl`/`cachePath` viven solo en
  memoria (`serializeTrack` no los guarda): al cerrar la app se
  pierden y el caché se vacía al abrir (`clearAudioCache()` en el
  inicio). Solo se guardan 3 canciones (LRU con `registerCache`):
  al preparar la cuarta, la más vieja se borra (`deleteCachedAudio`).
  Si el usuario pausa durante "Preparando…", se cancela la
  reproducción automática pero el caché sigue preparándose y queda
  guardado para cuando le dé play. Si el archivo en caché se pierde
  (el sistema lo borró), al fallar el `<audio>` se prepara de nuevo
  una sola vez por canción.
- **Audio directo también se endureció:** `getAudioUrl` ahora pide
  `--dump-json` con `-f bestaudio/best` explícito (verificado con
  yt-dlp real: devuelve el formato 18 con su URL de audio), en vez
  de confiar en el selector por defecto que fallaba con "Requested
  format is not available" en estos videos.

### 2) Sugerencias por ARTISTA, no por canción ni por canal
- Antes se buscaba por el artista de la pista, que en YouTube es el
  CANAL que subió el video ("armxndo", "LatinHype"…), o por el título
  cuando el canal era "YouTube". Por eso no salían más canciones de
  Belanova al buscar "Rosa Pastel".
- Ahora `artistForSuggestions(t)` saca el artista del TÍTULO
  ("Belanova - Rosa Pastel" → "Belanova", también con "—", "|", y
  con el artista después del guion si coincide), ignora palabras
  genéricas ("Mix", "Official Video"…), y si la canción se encontró
  buscando ("rosa pastel belanova" + título "Rosa Pastel"), lo que
  sobra de la búsqueda es el artista. La búsqueda con la que se
  encontró cada canción se guarda en la pista (`track.searchQuery`).
  La radio y las sugerencias de "A continuación" usan lo mismo.
  Validado en Node con 8 casos (incluido el ejemplo exacto de
  Jonathan), 8/8.

### 3) "A continuación" mal centrada en local
- Causa: `.queue-head` no tenía reglas de encogimiento y el contador
  era largo en local ("12/305 · 293 siguen"), así que el botón
  "Ocultar ▴" se salía de la pantalla. Arreglo: `.q-title` y
  `.q-toggle` no se encogen, `.q-right` ocupa el espacio libre,
  `.q-count` se recorta con "…" si falta sitio, y la cabecera puede
  partirse en dos líneas si hace falta. Además el contador ya no
  lleva "· N siguen" (las que siguen ya se ven en la lista): ahora
  es solo "12/305" (y "· aleatorio" si está el aleatorio).

### 4) Ecualizador igual en online y en local
- Causa: el analizador solo estaba conectado al `<audio>` local; en
  online se dibujaban ondas generadas (se veían distintas). Arreglo:
  `ensureViz` también conecta `ytAudioEl` al mismo analizador y
  `drawViz` usa los datos reales cuando suena el audio directo (sin
  iframe). El caché ya sonaba por el `<audio>` local, así que ahí el
  ecualizador es real automáticamente. Las ondas generadas solo
  quedan para el reproductor oficial (iframe), que no se puede
  analizar.

### 5) La última búsqueda de YouTube permanece
- Al salir y entrar a YouTube (o al reabrir la app, si el sistema
  la cerró en segundo plano) los resultados se borraban porque solo
  vivían en memoria. Ahora la última búsqueda con sus resultados se
  guarda en `localStorage` (`gurru-ytsearch-v1`) y se restaura al
  abrir la app y al entrar a la pestaña YouTube si la vista se
  vació (sin pisar una búsqueda a medias distinta).

### 6) Más fuentes de letras
- Antes solo LRCLIB (muy limitado). Ahora la letra se busca en
  cadena, sin clave: **LRCLIB** primero (la única con tiempos para
  resaltar la línea que suena), si no la tiene **lyrics.ovh** y
  luego **some-random-api** (texto plano). El artista para buscar se
  saca del título con el mismo parseo de las sugerencias. Abajo de
  la letra se ve de qué fuente salió ("Letra: LRCLIB" / "lyrics.ovh"
  / "some-random-api").

Validado: JS `node --check` OK (los 2 bloques), todos los
`$("...")` existen (96 ids), javac OK (los 5 archivos), `npx cap
sync` OK, md5 de `www/index.html` == copia en assets. Lógica
probada en Node: artista 8/8, LRU del caché OK (guarda 3, la cuarta
saca la más vieja y limpia sus campos). Descarga estilo caché
verificada con yt-dlp real: `-f bestaudio/best` produce un .mp4 de
1.67 MB para "Por Esta Vez" (324UfrjhL9Q).

## Ronda NUEVO11 (2026-11-09 ~23:00 CST)

Jonathan actualizó yt-dlp desde Descargas y confirmó: ya aparece
"Preparando…" y ya no hay error, pero la música se sigue apagando en
segundo plano. Pidió 4 cosas:

### 1) El caché es la ÚNICA opción online
- `playYouTubeOnline` ya no cae al audio directo ni al reproductor
  oficial cuando el caché falla o cuando otra canción se está
  preparando: esos dos caminos se pausan al salir de la app y daban
  los errores ("Requested format is not available"). Ahora, si otra
  canción se estaba preparando, se cancela su reproducción
  automática y se prepara la nueva; si el caché falla, sale un aviso
  con el motivo, botón Reintentar (vuelve a preparar) y la pista de
  actualizar yt-dlp en Descargas. `playHiddenYouTube` y el
  reproductor oficial quedan en el código sin usarse para nuevas
  reproducciones.
- Además se corrigió una carrera: al cancelar una preparación, su
  `.then`/`.catch` ya no apaga `cacheBusy` (solo lo apaga la
  preparación con el token vigente).

### 2) Sugerencias por artista en TODAS las canciones, y cola nueva al abrir un audio online
- El parseo por artista (`artistForSuggestions`) ya era general,
  pero `ensureYtSuggestions` solo se llamaba en la rama online de
  `loadTrack`: con el caché, la canción suena por el `<audio>` local
  y las sugerencias nunca se sembraban. Ahora también se siembran
  en esa rama, para cualquier canción online, no solo "Rosa Pastel".
- `clearYtQueueExcept(idx)`: al abrir una canción nueva desde
  online (resultado de búsqueda → Escuchar, o fila de Biblioteca /
  cola), la cola "A continuación" se borra y se genera una nueva
  desde esa canción: salen las de YouTube anteriores (las locales y
  las descargadas no se tocan) y se siembran sugerencias frescas de
  su artista. Siguiente/anterior y la radio NO borran la cola (se
  mueven dentro de ella). Un contador `suggestGen` descarta las
  búsquedas de sugerencias viejas que terminen después del cambio.

### 3) yt-dlp se actualiza solo al instalar la app
- Al abrir la app por primera vez (sin la marca
  `gurru-ytdlp-auto` en `localStorage`), se llama en segundo plano
  a `updateYtDlp({channel:"stable"})` y se guarda la marca al
  terminar. Si no hay internet, falla en silencio y se reintenta en
  la próxima apertura. El botón manual de Descargas sigue igual.

### 4) Segundo plano: el código ya está bien; lo que falta es el ajuste de batería de OPPO
- Se revisó todo el camino: el WebView NO se pausa al salir
  (`KeepRunning` es `true` por defecto en Capacitor, verificado en
  el código de `Bridge.java`), el servicio en primer plano arranca
  bien (`startForeground` + WakeLock + `START_STICKY`, tipo
  `mediaPlayback`) y nada en JS pausa el audio al salir. El que
  mata el proceso es **ColorOS** (OPPO): aunque haya servicio en
  primer plano, apaga las apps en segundo plano si no están en "Sin
  restricciones". Por eso la app ahora muestra un aviso único al
  abrirla (marca `gurru-batt-tip`): Ajustes → Batería → Gurru Music
  → "Sin restricciones". Preguntas pendientes para Jonathan: ¿ya
  puso ese ajuste? ¿la música se apaga al salir de la app o al
  apagar la pantalla / después de un rato? ¿la música local también
  se apaga en segundo plano o solo la de YouTube?

### Sobre ytmusicapi (pregunta de Jonathan)
- Sí se puede usar: es una librería que habla con la API interna
  de YouTube Music y saca el audio por otra ruta. Pero se probó esa
  API desde aquí y YouTube bloqueó hasta un video normal
  (`UNPLAYABLE` en `music.youtube.com/youtubei/v1/player` y en el
  cliente web), así que esa ruta no está verificada y podría chocar
  con el mismo muro; además meter la librería Python dentro del APK
  es un cambio grande y se rompe cada que YouTube cambia algo. Se
  quedó como plan B: si con el caché + yt-dlp actualizado sigue
  fallando, se agrega la llamada directa a la API de YouTube Music
  (sin la librería) como otro respaldo.

Validado: JS `node --check` OK (los 2 bloques), todos los
`$("...")` existen (96 ids), `npx cap sync` OK, md5 de
`www/index.html` == copia en assets. Sin cambios en Java (no hace
falta javac). Lógica probada en Node con las funciones reales:
abrir ytC en una cola mixta deja solo [locales + ytC] con current
correcto y gen subido; abrir un tema local no toca nada; abrir ytA
quita las sugerencias viejas sembradas.

## Ronda NUEVO12 (2026-11-10)

Jonathan reportó (12:53 CST) 4 cosas, todas aplicadas:

### 1) La música local sí sigue en segundo plano; solo se para la de YouTube (causa raíz encontrada)
- Dato clave de Jonathan: la música local **siempre** siguió funcionando en segundo plano; solo la de YouTube se paraba al salir (se preparaba bien, pero al salir se pausaba y solo volvía al regresar a la app). Eso descarta que ColorOS mate el proceso (si lo matara, la local también se cortaría) y apunta al archivo de audio en sí.
- Causa raíz, verificada con yt-dlp real (2026.10.26): el caché descarga `-f bestaudio/best`, y en estos videos de YouTube Music el único formato con audio es el **18** (360p MP4, `avc1.42001E mp4a.40.2`): un archivo con pista de **video** además de audio. El WebView pausa el audio cuando la página se oculta si el archivo trae video (por eso el MP3 local, solo-audio, seguía sonando).
- Arreglo: `cacheAudio` ahora usa `--extract-audio --audio-format m4a` (solo audio, sin video). Verificado con yt-dlp real: el M4A resultante (1.31 MB para "Baila Mi Corazón") **no tiene pista de video** (`vide` ausente, `soun` presente). Para audio AAC (formato 18, itag 140) es solo un reempaquetado rápido; la preparación tarda un poco más por la conversión. El caché se limpia al arrancar, así que no quedan archivos .mp4 viejos con video. La ruta de Descargas (permanente) ya usaba `--extract-audio`, sin cambio.

### 2) Sugerencias por el ARTISTA REAL, leído de la metadata del video
- El buscador solo trae el canal que subió el video (a veces el sello o "BelanovaVEVO"), y el parseo del título no siempre acierta. Ahora el plugin tiene `getVideoInfo({url})` (`--dump-json`, mismo patrón que `getAudioUrl`) que devuelve `artist`/`track`/`album` reales. Verificado con yt-dlp real: "Baila Mi Corazón" → artist **Belanova**, track "Baila Mi Corazón", album **Fantasía Pop**; "Por Esta Vez" → artist **Belanova**.
- En JS, `resolveArtist(t)` pide esa metadata una sola vez por canción (se guarda la promesa en `t._metaPromise` para no repetirla), corrige `t.artist` (y `t.album`) en la canción, refresca la lista y la letra, y devuelve el artista real. `fetchSuggestions` ahora espera primero a `resolveArtist` y busca canciones de **ese** artista; si la metadata falla o no trae artista, cae al parseo del título (lo de antes). Al abrir una canción online también se llama `resolveArtist` (en la rama online y en la de caché), así el artista que se ve es el real y la letra se busca con él. Validado en Node con plugin simulado: canal "BelanovaVEVO" → "Belanova" (y corrige el artista en la canción); si `getVideoInfo` falla → fallback al parseo; canal ya correcto → "Belanova".

### 3) Vista de cuadrícula en Biblioteca: las tarjetas se estiraban y se veían vacías
- Causa: la cuadrícula (`.playlist.grid`) es `display:grid` dentro de un contenedor con altura fija; por defecto `align-content` estira las filas para llenar el contenedor, así cada tarjeta se estiraba a la altura de la fila. Reproducido en el navegador con el CSS y la estructura exactos de la app: las tarjetas medían ~161×458 px con ~230 px de espacio vacío dentro (solo cabían 3 por pantalla).
- Arreglo: `.playlist.grid{ align-content:start; grid-auto-rows:max-content; }` y `.playlist.grid li{ align-self:start; }`. Verificado en el navegador con el mismo repro: las tarjetas ahora miden ~162×219 px (la portada 161×161 + título/artista + ~14 px de relleno), caben las 4 canciones en la primera pantalla. (Las portadas se veían como imagen rota solo en esa vista previa porque el archivo local no se servía; en la app real `logo.png` sí está.)

### 4) Configuración: color de la app + créditos
- Nueva vista **Ajustes** (4º botón en la barra inferior, engranaje). El sistema de temas ya existía en el CSS (`html[data-accent]` con azul/rojo/naranja) pero no había forma de cambiarlo.
- Colores para elegir (todos neón, conforme al estilo de la app): **Azul neón** (el de siempre), Verde neón, Rosa neón, Violeta, Rojo, Naranja y Dorado. El color se aplica a toda la app (título, barra de progreso, botones activos, navegación) y se guarda en `localStorage` (`gurru-accent`); un script chico en `<head>` lo restaura antes de pintar para que no parpadee el azul al abrir.
- Créditos en Ajustes: "App diseñada con el poder de Muse" y "Desarrollador: Jonathan Aguilar Lopez".

Validado: JS `node --check` OK (los 3 bloques, el 3º es el script de restauración del tema en `<head>`), todos los `$("...")` existen (97 ids, nuevo `viewSettings`), `npx cap sync` OK, md5 de `www/index.html` == copia en assets. Java: `javac` OK (5 archivos, con los stubs de `hidden_files/capstubs/`; el primer intento falló porque `cp` sin `-r` no copió los directorios de stubs).

## Ronda NUEVO13 (2026-11-10)

Jonathan pidió probar Facebook dentro de la app (su plan trae Facebook ilimitado), con 3 cosas:

### 1) Sección Facebook en la barra inferior (prueba)
- 5º botón en la barra inferior (Descargas / Biblioteca / YouTube / **Facebook** / Ajustes), con la "f" azul de Facebook. La sección es aparte de YouTube para no revolverlas.
- Nuevo plugin nativo `FacebookAuthPlugin` (registrado en `MainActivity`): Facebook no tiene buscador público ni API de música, y yt-dlp solo baja videos con su link (verificado: no hay extractor `facebook:search` y la URL de búsqueda da "Unsupported URL"). Por eso la búsqueda usa la sesión del usuario:
  - `login()`: abre Facebook en una ventana WebView dentro de la app; el usuario inicia sesión (Facebook puede pedirle verificación, como en el navegador) y toca «Listo». La contraseña solo la ve Facebook. Las cookies se guardan en un archivo privado (`fb_cookies.txt`, formato Netscape) para yt-dlp.
  - `isLoggedIn()` / `logout()`: la sesión vive en el CookieManager del WebView y persiste entre aperturas.
  - `searchVideos({query, limit})`: pide `m.facebook.com/search/videos/?q=...` con las cookies y saca los videos del HTML (enlaces `/watch/?v=…`, `/<página>/videos/…`, `/reel/…` con título, página y duración). Devuelve `needsLogin: true` si Facebook pide iniciar sesión. El parseo es por expresiones regulares: es la parte frágil y experimental (Facebook cambia su página seguido).
- En JS: vista `viewFacebook` con su caja de login, buscador y resultados (igual que YouTube: toca uno y elige Escuchar o Descargar). La última búsqueda se guarda (`gurru-fbsearch-v1`) y se restaura al entrar. Las canciones de Facebook son `type: "facebook"` (tercer origen: su cola no se mezcla con YouTube ni con la local; filtro "Solo Facebook" en Biblioteca; insignia azul "Facebook" en la cola y en la ventana de elección; sugerencias en "A continuación" y radio con más videos del mismo artista buscados en Facebook).
- Escuchar una de Facebook usa el mismo camino que YouTube online: `cacheAudio` con la URL del video (yt-dlp le pasa `--cookies` cuando la URL es de facebook.com). Descargar usa `downloadAudio` con la URL del video (también con cookies).
- Validado: `javac` OK (6 archivos); el parseo de la búsqueda se probó con HTML de muestra (3 resultados, sin duplicados, página y duración correctas). **Sin probar en el teléfono**: la búsqueda real con su sesión solo se puede verificar en su teléfono con su login (desde aquí no hay cuenta de Facebook para probar).

### 2) Botón Descargar en el reproductor
- Junto a "♪ Letra" ahora hay un botón "⬇ Descargar" (solo se ve cuando suena una canción online y aún no está descargada). Como el audio ya está en el teléfono (caché invisible), no se baja de nuevo: el nuevo método `saveCachedAudio({videoId, title})` mueve el archivo del caché a Música/Gurru Music con MediaStore (la misma publicación que usa la descarga normal) y la canción pasa a ser local (tipo "local", descargada, con su nombre guardado). Si el caché ya no tiene la canción, usa la descarga normal de yt-dlp.
- El archivo del caché es M4A, así que se guarda como M4A (aunque el formato elegido en Descargas sea MP3: convertirlo sería bajarlo de nuevo).

### 3) Mini reproductor en la parte de abajo
- Fuera de la vista del reproductor (Biblioteca, Descargas, YouTube, Facebook, Ajustes) aparece una barra compacta sobre la barra inferior con la portada, el título, el artista y play/pausa de la canción que suena. Tocarla vuelve al reproductor principal; el botón ▶/❚❚ pausa o sigue sin salir de la vista. Solo aparece cuando hay una canción cargada.

Validado: JS `node --check` OK (los 3 bloques), todos los `$("...")` existen (113 ids, nuevos `viewFacebook`, `navFacebook`, `fbSearchInput`, `fbSearchBtn`, `fbSearchResults`, `fbMsg`, `fbLoginBtn`, `fbLogoutBtn`, `fbAuthMsg`, `miniPlayer`, `miniArt`, `miniTitle`, `miniArtist`, `miniPlayBtn`, `btnDownload`, `choiceBadge`), `npx cap sync` OK, md5 de `www/index.html` == copia en assets. Java: `javac` OK (6 archivos). Lógica probada en Node con las funciones reales: colas por origen (YouTube [0,3], Facebook [2,4], local [1,5]); abrir una de Facebook desde la cola quita solo las otras de Facebook; `makeFbTrack` con los datos del video probado ("Por Ti", 216 s).

## Ronda NUEVO14 (pedida 2026-11-10 23:16 CST, aplicada ~23:20 CST)
Jonathan instaló NUEVO13 y reportó 3 cosas con captura: (a) al tocar "Iniciar sesión en Facebook" sale el diálogo con el texto y los botones CANCELAR / LISTO, YA ENTRÉ, pero no aparece ninguna ventana donde escribir la cuenta y la contraseña; (b) los botones de abajo (Descargas / Biblioteca / YouTube / Facebook / Ajustes) quedaron muy pegados y las etiquetas se sobreponen unas con otras; (c) en los créditos, poner primero lo de Desarrollador y al final, más discreto, lo de "Powered by Muse".
Aplicado en NUEVO14: (1) Login de Facebook: causa raíz — el diálogo de Android mide su ventana como WRAP_CONTENT por defecto; el layout raíz MATCH_PARENT se encoge al contenido y el WebView, con altura 0 + peso 1, quedaba en 0 px, así que la página de Facebook nunca se veía (el diálogo solo mostraba el texto y los botones, tal como la captura). Arreglo: después de dialog.show() se fija la ventana a todo el ancho y el 92% del alto de la pantalla, con fondo azul oscuro de la app; además el WebView tiene altura mínima de respaldo (420 dp) por si el peso fallara, y se activaron useWideViewPort/loadWithOverviewMode para que la página de Facebook quepa bien. El texto del diálogo y los botones ahora usan la fuente normal y color claro sobre el fondo oscuro. (2) Barra inferior: las etiquetas eran de 13 px en botones de ~78 px ("Biblioteca" no cabía y se salía sobre el botón de al lado). Ahora son de 11 px con menos relleno lateral (2 px), y cada etiqueta recorta con puntos suspensivos si no cabe (overflow:hidden + text-overflow:ellipsis), así los 5 botones ya no se pisan. (3) Créditos: ahora el orden es "Desarrollador: Jonathan Aguilar Lopez" primero y grande, "Gurru Music · Reproductor de música" en medio, y al final, chico y tenue (11 px, azul claro al 55%), "Powered by Muse".
Validado: node --check OK (3 bloques), javac OK (6 archivos), npx cap sync OK, md5 iguales (322884fa…). ZIP chico gurru-music-NUEVO14.zip: www/index.html, FacebookAuthPlugin.java, README.md. Pendiente: Jonathan sube NUEVO14 (www/index.html dentro de www/; FacebookAuthPlugin.java dentro de android/app/src/main/java/com/gurru/music/; README.md en la raíz), espera el verde, instala y prueba: (a) Facebook → Iniciar sesión → AHORA sí se abre la ventana de Facebook donde escribir cuenta y contraseña → tocar «Listo, ya entré» → buscar "Belanova" → ¿trae resultados?; (b) la barra de abajo se lee bien: Descargas, Biblioteca, YouTube, Facebook, Ajustes sin pisarse; (c) Ajustes → Créditos: primero Desarrollador y al final "Powered by Muse" chiquito.

## Ronda NUEVO15 (pedida 2026-11-11 06:00 CST, aplicada ~06:05 CST)
Jonathan instaló NUEVO14, el login de Facebook ya abrió bien, pero al buscar le salió "Facebook devolvió error http 400 en la búsqueda". Probado en la sesión: sin login, m.facebook.com/search/videos/ redirige (302) a login.php y esa página devuelve 400 hasta para un navegador normal; mbasic.facebook.com/search/videos/ devuelve 400 directo (ese sitio está muerto); www.facebook.com/search/videos/ redirige a m.facebook.com. O sea: el 400 de Jonathan NO fue la redirección a login (esa se detecta y pide iniciar sesión), sino que la propia búsqueda devolvió 400 con su sesión puesta.
Aplicado en NUEVO15 (solo FacebookAuthPlugin.java): (1) Causa más probable — cookies duplicadas: currentCookies() concatenaba las cookies de www.facebook.com y las de m.facebook.com, y como las de .facebook.com aparecen en las dos, cada cookie (c_user, xs, sb, datr, fr…) iba DOS VECES en el header Cookie; Facebook puede rechazar eso con 400. Ahora se quitan los duplicados por nombre (gana el valor de m.facebook.com). De paso también se arregla fb_cookies.txt, que se escribía con las cookies duplicadas (eso podía afectar las descargas con sesión después). (2) La petición ahora se parece a navegar con el navegador: Referer, Upgrade-Insecure-Requests y cabeceras Sec-Fetch (Site/Mode/User/Dest). (3) Diagnóstico: si vuelve a fallar, el error ya no es solo "HTTP 400" — nuevo httpErrorDetail lee la respuesta de Facebook y el aviso muestra el código, la página final y lo que Facebook dice (si pide verificación de seguridad o si es la sesión, ahí sale). El aviso de error ya era tocable con Reintentar, así que el motivo completo se ve al tocarlo.
Validado: javac OK (6 archivos). Sin cambios en la web (no hace falta cap sync). ZIP chico gurru-music-NUEVO15.zip: FacebookAuthPlugin.java, README.md. Pendiente: Jonathan sube NUEVO15 (FacebookAuthPlugin.java dentro de android/app/src/main/java/com/gurru/music/; README.md en la raíz), espera el verde, instala y prueba: Facebook → buscar "Belanova" → ¿ya trae resultados? Si vuelve a fallar, que toque el aviso de error para ver el motivo completo y lo copie (o captura): ahí dirá si Facebook pide verificación, si es la sesión o qué página devolvió el error.
Actualización 2026-11-11 ~12:49 CST: Jonathan subió NUEVO15 él mismo a las 12:43-12:44 CST (commits 92400afd y da93d31c), pero el plugin quedó en la RAÍZ del repo otra vez y el README sí quedó bien; las corridas #26 y #27 salieron en verde pero compilaron el código viejo. A su petición, Muse subió el plugin a su ruta correcta con push-files (commit c1601ea3, verificado byte-idéntico, md5 abcd8ad1); la corrida #28 estaba en curso. Falta: esperar el verde de la #28, instalar ese APK y probar la búsqueda de Facebook.
Actualización 2026-11-11 ~13:04 CST: la corrida #28 salió en verde y Jonathan probó la búsqueda: ya no da error 400, pero NO arroja nada (cero resultados). Decisión: se abandona la búsqueda de Facebook; Facebook queda solo por links (eso pasa a la ronda NUEVO17, ver abajo).

## Ronda NUEVO16 (pedida 2026-11-11 ~20:24 CST, aplicada ~20:30 CST)
Jonathan comparó el audio con YouTube Music y siente que Gurru Music suena más bajo y con menos calidad. Investigado con yt-dlp real (2026.10.26) y el código: (1) el volumen de la app quedaba en 80% al arrancar (YouTube Music suena al 100%) — esa es la causa de que se escuche más bajo; (2) el caché de YouTube usaba el cliente Android de yt-dlp, que para estos videos solo ofrece el formato 18, cuyo audio es de ~48 kbps (el M4A extraído de "Baila Mi Corazón" mide 1,31 MB en 3:36); YouTube Music sirve la misma canción a ~128-160 kbps. Con el cliente por defecto de yt-dlp (en la práctica usa el cliente visionOS) sí aparecen los formatos DASH de solo-audio: itag 140 (AAC ~130 kbps, 44,1 kHz) y 251 (Opus ~137 kbps). Verificado en la sesión: con `-f bestaudio[ext=m4a]/bestaudio` baja el 140 y el M4A sale de 3,49 MB a 128 kbps (reempaquetado, sin transcodificar).
Aplicado en NUEVO16: (1) volumen al 100% al arrancar (audio.volume, ytAudioEl.volume y lastVol; el botón de silencio y las flechas ↑/↓ siguen igual). (2) cacheAudio ahora intenta primero el cliente por defecto con `-f bestaudio[ext=m4a]/bestaudio` (prefiere el AAC 140; si no hay M4A cae al mejor audio) y, si ese intento falla, reintenta con el cliente Android de siempre (formato 18) — así la calidad mejora sin romper la reproducción si el cliente por defecto se bloquea en su red. Nuevo helper runCacheRequest + deleteCacheFiles (borra los .part a medias entre intentos); los globs que buscan el archivo producido ya no aceptan archivos .part. Las descargas (botón Descargar) siguen con el cliente Android de siempre; si la mejora se confirma escuchando, se puede aplicar ahí también. (3) Pantalla principal: la app ahora abre en Biblioteca (antes abría en el reproductor); la sección del reproductor nace oculta y la de Biblioteca visible para que no haya flash al arrancar. El historial del navegador y el gesto atrás ahora vuelven a la Biblioteca (variable renombrada a navInHistory) y Escape también. (4) Gesto atrás en la Biblioteca: ya NO se sale de la app (antes exitApp cerraba el WebView y se apagaba la música); ahora se minimiza (capApp.minimizeApp, con exitApp solo si el plugin no lo trae), así la app queda en segundo plano y la música sigue sonando, como YouTube Music. Salir del todo solo desde Recientes o con Detener en la notificación. (5) Quitado el aviso único de batería de OPPO del primer arranque (el toast de "Sin restricciones" de NUEVO11).
Validado: node --check OK (3 bloques), 112 ids usados y ninguno falta; javac OK (6 archivos); npx cap sync OK; md5 igual entre www/index.html y la copia en android (dada55a1). ZIP chico gurru-music-NUEVO16.zip: www/index.html, YtDlpDownloaderPlugin.java, README.md. Nota de numeración: el plan de recomendaciones (Descubre + Mezclas + Facebook por links) que se había llamado NUEVO16 pasa a llamarse NUEVO17 — mismo plan, solo cambia el número, y sigue en hold hasta que Jonathan diga "haz tu magia". Pendiente: Jonathan sube NUEVO16 (index.html dentro de www/; el plugin dentro de android/app/src/main/java/com/gurru/music/; README.md en la raíz), espera el verde, instala y prueba: (a) abre la app → debe abrir en Biblioteca; (b) pone una canción de YouTube → debe sonar igual de fuerte y con mejor calidad que antes (comparar con YouTube Music); (c) en Biblioteca, gesto atrás → la app se minimiza y la música sigue sonando; (d) ya no aparece el aviso de batería al instalar.

## Ronda NUEVO17 (plan 2026-11-11 17:03 CST; hold levantado 2026-11-11 ~20:34 CST con "haz la magia", aplicada ~20:40 CST)
El plan NUEVO16 de recomendaciones pasó a llamarse NUEVO17 (la ronda de audio/pantalla ocupó el NUEVO16). Decisiones de Jonathan (2026-11-11 20:21 CST): (1) SIN llave — MusicBrainz + iTunes, sin Last.fm; (2) el login de Facebook se quita TODO, plugin incluido; (3) las dos juntas en una sola ronda. Aplicado:
(1) **Descubre** (tarjeta arriba de la vista YouTube): artistas parecidos al que suena (o al más escuchado de la biblioteca si no suena nada), sin llave. Primero MusicBrainz — artista → etiquetas (inc=tags) → artistas con las mismas etiquetas — con pausa de 1,1 s entre llamadas (límite de MusicBrainz) y si no alcanza o falla (a veces devuelve "server busy"), iTunes — artista → primaryGenreName → artistas de ese género. Verificado en la sesión con Belanova: MusicBrainz da MBID + etiquetas (electropop, latin, pop…), tag "latin pop" da Thalía, Fito Páez, Jennifer Lopez, Celia Cruz, Juan Gabriel; iTunes da género "Pop Latino" y artistas KAROL G, Luis Miguel, Enrique Iglesias, Shakira, Myke Towers, Tainy, J Balvin, y sus canciones top (Rosa Pastel, Baila Mi Corazón, Cada Que..., Por Ti, Me Pregunto). Tocar un artista busca sus canciones en YouTube con el buscador de siempre. Resultado guardado por artista en localStorage (gurru-discover-v1, 24 h).
(2) **Mezclas para ti** (tarjeta arriba de la lista de Biblioteca): contador local gurru-plays-v1 — canción escuchada completa +1, salto antes de la mitad −1 (límites −5…+50). Mezclas: "Lo más de <artista>" (top 3 artistas por puntos, canciones ordenadas por lo más escuchado), "Álbum: <nombre>" (top 2 álbumes con ≥2 canciones) y "Mezcla sorpresa" (todo lo local revuelto, máx. 30). La mezcla se toca como una cola propia: queueIndices devuelve la mezcla mientras suena (siguiente/anterior/aleatorio y "A continuación" la siguen; el contador de la cola muestra el nombre de la mezcla); tocar una canción fuera de la mezcla, abrir una online, reordenar con ↑ ↓ (la mezcla sigue el intercambio), quitar una canción o que la mezcla termine, la cierran. Por género/época NO se puede todavía: LocalMusicPlugin solo lee título/artista/álbum/portada de MediaStore (sin género ni año) — haría falta ampliar el plugin.
(3) **Facebook solo por links + login quitado**: se borró FacebookAuthPlugin.java (local) y su registro en MainActivity; en YtDlpDownloaderPlugin se quitó addFbCookies y sus llamadas (ya no hay cookies que pasar). La vista Facebook ahora es solo una caja "Pega el link del video" + Agregar: getVideoInfo lee título/duración/página (verificado en la sesión con el video "Por Ti": id 1124302223084539, título limpio de "121K views · 1.3K reactions | …", uploader "Tu Music", 216 s) y la canción se agrega como type facebook (insignia azul, cola propia) con Escuchar (caché invisible) y Descargar; si el link ya está, abre su diálogo. Debajo se listan los videos de Facebook ya agregados. Sin login, los videos privados o de amigos no funcionan. Las canciones de Facebook ya no tienen radio ni sugerencias automáticas (su búsqueda era la que las alimentaba y nunca devolvió nada en el teléfono): al terminar su cola paran, como las locales. La búsqueda, el login/logout y la persistencia gurru-fbsearch-v1 quedaron fuera (el dato viejo guardado en el teléfono es inofensivo).
Validado: node --check OK (3 bloques), 116 ids usados y ninguno falta, 0 referencias a FacebookAuth en Java y web; javac OK (5 archivos, sin FacebookAuthPlugin); npx cap sync OK; md5 igual entre www/index.html y la copia en android (6c872f40). Pruebas Node con las funciones reales: contador (+1+1−1=1, tope 50), buildMixes (Belanova ordenado por escuchas, álbum Fantasía Pop, sorpresa), queueIndices con mezcla activa y fuera de ella, fetchSimilarArtists simulado (Miranda!, Fey, Thalía, Paulina Rubio, KAROL G, Shakira, sin la semilla) y fbLinkIdFromUrl (videos/watch/reel). ZIP chico gurru-music-NUEVO17.zip: www/index.html, YtDlpDownloaderPlugin.java, MainActivity.java, README.md. **OJO: el ZIP no puede borrar archivos: Jonathan debe borrar a mano android/app/src/main/java/com/gurru/music/FacebookAuthPlugin.java en el repo de GitHub (abrir el archivo → ⋮ → Delete file) para que el login salga del todo; si se le olvida, la compilación igual sale en verde porque el plugin ya no está registrado en MainActivity, solo quedaría código muerto.**
Actualización 2026-11-11 ~20:39 CST: Jonathan subió NUEVO16 él mismo en dos commits — 846551cf (20:37:51 CST, los archivos en la RAÍZ otra vez) y c788c57b (20:39:22 CST, index.html y el plugin ya en sus carpetas correctas); el remoto www/index.html quedó byte-idéntico a NUEVO16 (md5 dada55a1) y las corridas #29 y #30 salieron en verde. Actualización 2026-11-11 ~20:47 CST: a su petición ("Puedes subirlo a git-hub porfa"), Muse subió NUEVO17 con push-files (commit 32871a89, verificado byte-idéntico en los 4 archivos: index.html 6c872f40, plugin ac7b07a2, MainActivity 39fc8ef5, README 60280e67); la corrida #31 salió en verde (completed/success) — el APK NUEVO17 ya está listo para descargar. Pendiente: Jonathan borra FacebookAuthPlugin.java en GitHub, descarga el APK de la corrida #31 (Actions → run #31 → artifact gurru-music-apk), instala y prueba: (a) YouTube → tarjeta Descubre con artistas parecidos a Belanova (o al que suene) y al tocar uno busca sus canciones; (b) Biblioteca → tarjeta Mezclas para ti (Lo más de Belanova, Álbum Fantasía Pop, Mezcla sorpresa) y al tocar una suena la mezcla; (c) Facebook → pega el link del video "Por Ti" y escucha/descarga; ya no hay botón de iniciar sesión.

## Ronda NUEVO18 (correcciones de Jonathan tras probar NUEVO17, 2026-11-11 ~21:01 CST; aplicada ~21:15 CST)
Tres cosas que reportó al probar el APK de la corrida #31:
(1) **Links de Facebook daban "makeFbTrack is not defined"**: en NUEVO17 la función se borró sin querer junto con el bloque viejo de Facebook (el resumen decía que se había conservado, pero no estaba definida en ningún lado). Se reescribió junto a addFacebookLink, con la misma forma que makeYtTrack (id interno numérico, videoId = id numérico de Facebook, artista = página, álbum "Facebook · Solo audio", portada logo.png si no hay). De paso: dlTrackFor ahora reconstruye las de Facebook como Facebook (antes las reconstruía como YouTube) y la marca de recomendación se guarda en localStorage (serializeTrack/loadSavedState).
(2) **Descubre ya no es una tarjeta aparte: las recomendaciones van DENTRO de "A continuación" de YouTube, alternadas** — una del mismo artista, una recomendación (artista parecido), y así en continuo. Se quitó la tarjeta Descubre (HTML, CSS, loadDiscover/renderDiscoverChips/discoverSeedArtist/topLocalArtist y la llamada en showView) y su lógica alimenta la cola: dos depósitos (suggestPool del mismo artista, recommendPool de parecidos vía fetchSimilarTracks — 1 canción por artista parecido, máx. 3 por ronda, marca verde "Recomendación" distinta de la violeta "Sugerencia") y takeNextSuggestion alterna entre ellos; la radio al final de la cola sigue el mismo patrón. Detalle importante: en modo estricto, si el depósito que toca está vacío pero su búsqueda sigue en camino, la cola espera en vez de llenarse con el otro depósito (si no, las búsquedas rápidas llenaban la cola y las lentas quedaban todas al final). fetchSimilarArtists guarda su caché por artista (24 h) directamente, ya no dependía de loadDiscover.
(3) **Mezclas para ti, integradas mejor a Biblioteca**: la tarjeta grande con botones apilados se cambió por un estante horizontal con portada (estilo Spotify: tarjeta de 128 px con la portada de la primera canción de la mezcla o el logo, nombre y conteo, desplazamiento lateral con snap) y un encabezado plegable ("Mezclas para ti · N mezclas ▾/▸") que se recuerda plegado o abierto en el teléfono (gurru-mixes-open); plegado solo queda una línea y la lista de canciones queda pegada. Además startMix revalida los índices al tocar (si la lista cambió, solo suenan las que siguen ahí).
Validado: node --check OK (3 bloques), 114 ids usados y ninguno falta, 0 referencias a loadDiscover/Descubre/mix-chip; cap sync OK; md5 igual entre www/index.html y la copia en android (e4796535). Pruebas Node con las funciones reales: makeFbTrack (forma completa de Facebook), alternancia A1→B1→A2→B2→A3, fetchSimilarTracks simulado (Miranda!, Fey, Thalía, una por artista, todas con recommended=true) y ensureYtSuggestions simulado (cola S,R,S,R,…). ZIP chico gurru-music-NUEVO18.zip: www/index.html, README.md (sin cambios en Java).
Actualización 2026-11-11 ~21:12 CST: Jonathan eligió "Sí, súbelo a GitHub como NUEVO17" y Muse lo subió con push-files (commit 77898f06, verificado byte-idéntico en los 2 archivos: index.html e4796535, README b3226ba3); la corrida #32 salió en verde (completed/success) — el APK NUEVO18 ya está listo para descargar. Pendiente: Jonathan descarga el APK de la corrida #32 (Actions → run #32 → artifact gurru-music-apk), instala y prueba: (a) Facebook → pegar el link de "Por Ti" ya no da error y abre el diálogo Escuchar/Descargar; (b) YouTube → sonar Belanova y ver "A continuación" alternando "Sugerencia" (violeta, mismo artista) y "Recomendación" (verde, parecido) y que al terminar siga la radio igual; (c) Biblioteca → estante de mezclas con portadas, se puede plegar con el título y al tocar una suena la mezcla. Sigue pendiente borrar FacebookAuthPlugin.java en GitHub a mano.

## Ronda NUEVO19 (Facebook fuera, Deezer, arreglo de Mezclas y principal estilo YouTube Music; aplicada 2026-11-11 ~21:30 CST)
Jonathan probó NUEVO18 (corrida #32) y mandó dos capturas con cuatro pedidos.
(1) **Facebook se quita por completo**: nunca despegó. Se quitó el botón de la barra inferior, la vista, la caja de pegar link (addFacebookLink/renderFbLinks/fbLinkIdFromUrl), el filtro "Solo Facebook" y el CSS (.fb-box/.fb-add/.fb-auth/.fb-btn/.nav-btn.fb). Las canciones de Facebook que Jonathan ya tenía en su Biblioteca SIGUEN sonando (su tipo "facebook", su caché invisible y su radio apagada se conservan; makeFbTrack se conserva para reconstruirlas). El archivo FacebookAuthPlugin.java ya estaba borrado localmente en NUEVO17; en el repo de GitHub SIGUE PENDIENTE borrarlo a mano (abrir el archivo → ⋯ → Delete file) — ahora con más razón, porque Facebook ya no existe en la app.
(2) **Deezer en pestaña aparte, con su API**: la API pública de Deezer (sin clave) da búsqueda, portada, duración y una VISTA PREVIA de 30 segundos por canción. La canción completa NO se puede escuchar ni bajar sin una cuenta de Deezer con sesión — eso se dice en la vista, en los resultados ("Vista previa 30 s") y en la ventana de elegir. Verificado en vivo antes de construir: la API responde sin clave (búsqueda "Rosa Pastel Belanova" → 9 resultados con id/título/artista/álbum/portada/duración/preview; /track/{id} igual) pero NO manda Access-Control-Allow-Origin, así que el WebView bloquearía el fetch: la búsqueda la hace el plugin nativo (deezerSearch/deezerTrack con HTTP en Java, sin CORS). La vista previa es un MP3 directo: suena por el <audio> local, sin caché invisible, y se descarga con yt-dlp como cualquier MP3 (la descarga guarda la muestra de 30 s, no la canción completa). Su link lleva una firma con vencimiento (~1 día): al reabrir una canción guardada se refresca con deezerTrack por su deezerId (guardado en serializeTrack/loadSavedState) antes de sonar o de descargar. Deezer es un cuarto origen ("dz"): su cola solo mezcla canciones de Deezer, sin radio (como las locales). La última búsqueda se guarda y se restaura al entrar, igual que YouTube.
(3) **Mezclas estiradas**: en la captura las tarjetas ocupaban toda la pantalla y la lista no se veía. El estante era flex con align-items:stretch por defecto, así las tarjetas se estiraban a la altura del estante y la portada (aspect-ratio) no fijaba la altura. Arreglo: .mixes-shelf con align-items:flex-start + flex:0 0 auto, .mix-card con align-self:flex-start, .mix-cover con altura fija de 128 px (sin aspect-ratio) y .mixes-card con flex:0 0 auto. La tarjeta vuelve a medir solo su contenido (~200 px) y la lista queda a la vista.
(4) **Principal estilo YouTube Music**: "Accesos directos" — rejilla de 3 columnas con los 6 artistas locales que más escucha (por reproducciones y cantidad de canciones), portada y nombre encima abajo, como en la captura de YouTube Music; tocar uno suena lo más de ese artista (reusa la mezcla). Se esconde al buscar o filtrar, para que los resultados tengan todo el espacio. Y "Selección rápida" — título de la lista con el botón "Reproducir todo" (suena lo que se está viendo, desde la primera canción, respetando orden y filtro).
Validado: node --check OK (3 bloques), 118 ids usados y ninguno falta, 0 referencias a lo quitado de Facebook; javac OK en los 5 archivos Java; cap sync OK; md5 igual entre www/index.html y la copia en android (507a4eb4). Pruebas Node con las funciones reales: makeDzTrack (forma completa de Deezer), originOf (dz/yt/fb/local), serializeTrack (deezerId persiste, _previewFresh no), buildShortcuts (6 artistas en orden por reproducciones: Vicente, Belanova, Joan…), refreshDeezerPreview simulado (cambia el link viejo por uno fresco) y el guard de queueDownload (Deezer pasa, local bloqueado, YouTube pasa). Contrato de la API de Deezer verificado en vivo (búsqueda y /track con los campos que lee el plugin). ZIP chico gurru-music-NUEVO19.zip: www/index.html, YtDlpDownloaderPlugin.java, README.md.
Actualización 2026-11-11 ~21:48 CST: Jonathan eligió "Sí, súbelo a GitHub como NUEVO17" y Muse lo subió con push-files (commit aa207463, verificado byte-idéntico en los 3 archivos: index.html 507a4eb4, plugin ef050901, README 017c4ac0); la corrida #33 salió en verde (completed/success) — el APK NUEVO19 ya está listo para descargar. Pendiente: Jonathan descarga el APK de la corrida #33 (Actions → run #33 → artifact gurru-music-apk), instala y prueba: (a) ya no hay botón ni vista de Facebook, y las canciones de Facebook guardadas siguen sonando; (b) Deezer → buscar "Belanova", escuchar una vista previa de 30 s y descargarla; (c) Biblioteca → las mezclas ya no se estiran y la lista se ve; (d) Accesos directos en rejilla de 3 columnas y "Reproducir todo" en Selección rápida. Sigue pendiente borrar FacebookAuthPlugin.java en GitHub a mano.

## NUEVO20 (2026-11-11 ~22:20 CST)
Jonathan pidió: quitar Deezer, Mezclas para ti en su propia pestaña, accesos directos que se quiten al hacer scroll en la lista de la Biblioteca, y mejorar el rendimiento. (NUEVO19 se subió a GitHub y la corrida #33 salió en verde, pero Jonathan no llegó a instalarlo: no hay canciones de Deezer en su Biblioteca, así que la retirada es limpia, sin migraciones.)
(1) **Deezer quitado POR COMPLETO**: botón de la barra inferior, vista, buscador, resultados, filtro "Solo Deezer", insignias y CSS (violeta). En Java se quitaron deezerSearch, deezerTrack y el helper fetchJson con sus 4 imports (BufferedReader, InputStreamReader, HttpURLConnection, URL); en JS se quitaron makeDzTrack, refreshDeezerPreview, searchDeezer, renderDzSearchResults, restoreLastDzSearch y la clave gurru-dzsearch-v1, la rama de Deezer en loadTrack, el origen "dz" en originOf, deezerId en serializeTrack/loadSavedState y las ramas de Deezer en la cola, el menú de fila, la ventana de elegir y la guardia de descarga. Quedan 0 referencias a Deezer en todo el proyecto. Las canciones de Facebook ya guardadas siguen sonando (no se tocó nada de Facebook).
(2) **Mezclas para ti en su propia pestaña**: el botón de la barra inferior ahora es "Mezclas" (icono shuffle) y abre la vista #viewMixes: título, contador y una rejilla de 2 columnas con las mismas tarjetas (portada, nombre, conteo). La tarjeta plegable desapareció de la Biblioteca (se quitaron MIXES_OPEN_KEY, mixesOpen, syncMixesOpen y el encabezado plegable). La rejilla usa align-items:start para que ninguna tarjeta se estire (la lección del estiramiento de NUEVO19) y renderMixesGrid() se llama al entrar a la pestaña y al reconstruir la Biblioteca (con el mismo dedupe por firma). Además: la "Mezcla sorpresa" ya no se crea cuando no hay música local (antes aparecía una tarjeta vacía que no sonaba nada).
(3) **Accesos directos que se repliegan al hacer scroll**: al bajar más de 24 px en la lista de la Biblioteca, #shortcutsWrap se colapsa con animación (max-height + opacidad, 0.35 s) y la lista gana toda la pantalla; al volver arriba reaparece. Al reconstruir la lista (cambio de canción, búsqueda, filtro) se vuelve arriba y se muestran de nuevo.
(4) **Rendimiento** (medido en el código, no adivinado): (a) el visualizador corría requestAnimationFrame a 60 fps SIEMPRE, incluso en pausa y con la app en segundo plano, y leía getComputedStyle en cada fotograma. Ahora el bucle solo corre mientras suena algo (vizShouldRun: audio local, stream de YouTube o reproductor oculto sonando); en pausa dibuja la línea base una vez y se para, y se reanuda con kickViz() al dar play o al entrar al reproductor. El contexto 2D y el color del tema quedan en caché (el color solo se relee al cambiar de tema). (b) La búsqueda reconstruía TODA la lista en cada tecla: ahora espera 150 ms después de la última tecla (debounce). (c) yt-dlp emite progreso varias veces por segundo y cada evento reconstruía la vista de Descargas: ahora se reconstruye como mucho cada 500 ms (el % se actualiza igual en cada evento). (d) Las portadas de la lista y de las mezclas cargan con loading="lazy", para que el scroll no decodifique todas las imágenes a la vez. Lo que NO se cambió a propósito: la lista se sigue reconstruyendo al cambiar de canción (es lo que mueve la fila activa), y el guardado de posición cada 10 s se queda como está (es una escritura pequeña y es lo que permite volver donde quedó).
Validado: node --check OK (3 bloques), 126 ids y 0 faltantes, 0 referencias a Deezer; javac OK en los 5 archivos Java (el plugin ya sin Deezer); cap sync OK; md5 igual entre www/index.html y la copia en android (5d87d3bb). Pruebas Node con las funciones reales: renderMixesGrid (dibuja las mezclas en orden y cuenta; sin música local muestra el mensaje vacío; tocar una tarjeta llama startMix), vizShouldRun/kickViz (en pausa no corre; al dar play arranca el bucle una sola vez; con YouTube oculto sonando también corre) y el throttle de progreso (10 eventos en 900 ms reconstruyen 2 veces, no 10; el % sí se actualiza en cada evento). ZIP chico gurru-music-NUEVO20.zip: www/index.html, YtDlpDownloaderPlugin.java, README.md.
Actualización 2026-11-11 ~22:25 CST: Jonathan eligió "Sí, súbelo a GitHub como NUEVO17" y Muse lo subió con push-files (commit f7825b88, verificado byte-idéntico en los 3 archivos: index.html 5d87d3bb, plugin ac7b07a2, README c8434028); la corrida #34 salió en verde (completed/success) — el APK NUEVO20 ya está listo para descargar. Pendiente: Jonathan descarga el APK de la corrida #34 (Actions → run #34 → artifact gurru-music-apk), instala y prueba: (a) ya no hay Deezer en ningún lado; (b) la pestaña Mezclas con la rejilla de 2 columnas; (c) en Biblioteca, bajar en la lista esconde los accesos directos y volver arriba los trae de vuelta; (d) la app se siente más fluida al buscar, al descargar y al escuchar en segundo plano (menos batería). Sigue pendiente borrar FacebookAuthPlugin.java en GitHub a mano.

## NUEVO21 (2026-11-11 ~22:47 CST)
Jonathan mandó la captura de otro reproductor y pidió su diseño para la Biblioteca (pistas, álbumes, listas, artistas, carpetas) con la parte de abajo acomodada igual pero con las opciones de Gurru Music, conservando el diseño neón. Y aparte: que el APK se pueda instalar como actualización, porque al intentarlo le salía "conflicto con el paquete".
(0) **Firma estable del APK (el conflicto con el paquete)**: causa raíz — el workflow compilaba `assembleDebug` y cada corrida de GitHub genera una llave debug NUEVA, así que cada APK venía firmado con una llave distinta y Android rechaza instalarlo encima del anterior (INSTALL_FAILED_UPDATE_INCOMPATIBLE). Arreglo: se generó UNA llave release (RSA 2048, validez 25 años, alias `gurru`, huella SHA-256 DA:5E:98:A7:45:1A:EB:BA:17:08:44:A5:24:7A:12:21:5D:7F:53:4B:FC:05:7F:D6:1F:0D:D1:B7:9F:93:29:40) que vive en los secretos de GitHub (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD) y NUNCA en el repo; el workflow decodifica la llave, compila `assembleRelease` con signingConfig release y sube `app-release.apk` (ya no `app-debug.apk`). `versionCode` ahora es el número de corrida de GitHub (35, 36, 37…) y `versionName` es `1.0.<corrida>`, así Android siempre acepta la nueva versión como actualización. Si falta el secreto, el workflow falla a propósito con un mensaje claro (hay que agregar los secretos ANTES de subir esta ronda). La llave y las instrucciones están en `gurru-release.keystore` + `LEEME-LLAVE-DE-FIRMA.txt` (van en el ZIP, NUNCA se suben al repo, que es público). LA PRIMERA VEZ hay que desinstalar la app vieja una sola vez, porque la instalada trae la llave debug temporal (que ya no existe): al desinstalar se pierden las canciones de YouTube guardadas en la app y el historial, pero NO los MP3 ni las descargas de Música/Gurru Music. Después de esa vez, cada versión nueva se instala encima sin desinstalar.
(1) **Biblioteca con sub-pestañas**: Pistas / Álbumes / Listas / Artistas / Carpetas, debajo de la búsqueda, con subrayado cian en la activa (como en la captura). Pistas es la lista de siempre (accesos directos, Reproducir todo, lista). Álbumes es una rejilla de 2 columnas con portada, nombre, artista y conteo. Artistas, Carpetas y Listas son filas con portada, nombre, conteo y flecha. Tocar un grupo entra a sus canciones (drill) con encabezado (volver ←, nombre, conteo, Reproducir todo) y la cola sigue al grupo (reusa activeMix, como las mezclas); el gesto atrás y Escape cierran el drill antes de salir. La búsqueda y el filtro aplican también a los grupos, y la sub-pestaña elegida se recuerda (`gurru-lib-subtab`). `visibleItems()` es ahora la única fuente de "canciones visibles" (búsqueda + filtro, sin ordenar) para la lista y los grupos.
(2) **Listas de reproducción v1**: en la pestaña Listas hay un campo para crear una lista por nombre; desde el menú ⋮ de cualquier canción, "＋ Agregar a lista…" abre el menú con las listas (✓ si ya está) y la agrega o la quita (toggle, con aviso). Tocar una lista entra a sus canciones; en el drill, el menú ⋮ ofrece "Quitar de esta lista" y el encabezado tiene "Eliminar" (con Deshacer en el aviso). Una lista guarda solo los ids de sus canciones (`gurru-playlists-v1`): si una canción se quita de la Biblioteca, desaparece de la lista sola.
(3) **Carpetas**: `LocalMusicPlugin` ahora devuelve `folder` (el nombre de la carpeta padre de la ruta del archivo, ej. "Espinoza Paz", "Music", "Download"); en JS, `folderOf()` lo usa y, sin carpeta, cae a "Gurru Music" (descargadas), "YouTube", "Facebook" u "Otras".
(4) **Parte inferior flotante (como en la captura)**: el mini reproductor y la barra inferior ya no van pegados al borde: son píldoras flotantes redondeadas con sombra, con el diseño neón de Gurru Music y las mismas 5 secciones (Descargas, Biblioteca, YouTube, Mezclas, Ajustes). El mini reproductor ganó los botones ⏭ (siguiente) y ☰ (ver la cola en el reproductor).
(5) **Fila Reproducir todo (como en la captura)**: botón circular grande cian (suena lo que se ve desde la primera), título "Reproducir todo", contador "N canciones" y un botón de aleatorio que suena una canción al azar con el modo aleatorio prendido.
Validado: node --check OK (3 bloques), 145 ids y 0 faltantes, 0 referencias a Deezer; javac OK en los 6 archivos Java (plugin de música local con folder); cap sync OK; md5 igual entre www/index.html y la copia en android (232d4c3f). Pruebas Node con las funciones reales (43 pasaron, 0 fallaron): folderOf, groupBy (álbumes/artistas), visibleItems (búsqueda y filtro), crear/agregar con toggle/quitar de lista, persistencia en localStorage, drillIndices (álbum, artista, carpeta, lista), renderGroups (rejilla de álbumes, filas de artistas/listas, dedupe por firma) y drill (abrir, nombre/contador, cerrar). ZIP chico gurru-music-NUEVO21.zip: www/index.html, LocalMusicPlugin.java, build.gradle, build-apk.yml, README.md + (solo en el ZIP, NO para GitHub) gurru-release.keystore y LEEME-LLAVE-DE-FIRMA.txt.
Pendiente: (a) los 4 secretos ya quedaron creados en GitHub por el navegador 2026-11-11 23:03-23:04 CST (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD; Jonathan inició sesión en GitHub con Google él mismo) — ya NO hay que agregarlos a mano; (b) SUBIDO 2026-11-11 ~23:05 CST: el archivo del workflow NO se pudo subir con push-files (GitHub no deja que la app toque .github/workflows/* sin el permiso workflows, error 403), así que el navegador lo actualizó en la web (commit 7847fe38, verificado byte-idéntico) y Muse subió los otros 4 archivos con push-files (commit 57bf54f0, verificado byte-idéntico: index.html 232d4c3f, plugin 7724dcc2, build.gradle 2a45d3e0, README 9a93c99b). OJO: la corrida #35 (solo el workflow) salió en verde pero NO probó la firma, porque el build.gradle viejo no tenía signingConfig y el APK salió sin firmar; (c) la corrida #36 FALLÓ en la evaluación de Gradle: build.gradle línea 10, "Value is null" en la línea del versionCode con getenv dentro de defaultConfig. Arreglo (commit 55487e9f, verificado byte-idéntico): el número de corrida se calcula FUERA del bloque android (con asignación directa versionCode = / versionName =) y solo ahí se usa; (d) la corrida #37 FALLÓ al final, en :app:packageRelease: "KeytoolException: Failed to read key gurru from store /tmp/gurru-release.keystore: null" — el build compila completo (158 tareas) y solo falla al firmar. La llave local está verificada (abre con su contraseña, alias gurru existe, huella DA:5E:98:…), así que el problema era uno de los valores guardados en los secretos; los 4 secretos se volvieron a actualizar con los valores verificados el 2026-11-11 23:19 CST. ACTUALIZACIÓN 2026-11-12 ~09:15 CST: la firma NO funcionó — el secreto KEYSTORE_BASE64 guardado en GitHub quedó en 3588 caracteres (faltaban los últimos 4, "AicQ") y el pegado por navegador falló 3 veces (corridas #39-#42). Jonathan, ocupado, pidió quitar las llaves y sacar el APK como antes: se quitó la firma (build.gradle sin signingConfigs, commit 75f685f1; workflow vuelto a assembleDebug, commit 440c45c, sin paso de llave ni de diagnóstico) y las corridas #43 y #44 salieron en VERDE: el APK debug de NUEVO21 (app-debug.apk) está listo. Al ser debug hay que desinstalar la app vieja para instalarlo, y cada actualización futura también pedirá desinstalar hasta que se restaure la firma estable. Los secretos y la llave siguen guardados, sin uso. (e) probar: sub-pestañas, drill de un álbum, crear una lista y agregar canciones, mini reproductor flotante con ⏭ y ☰. Sigue pendiente borrar FacebookAuthPlugin.java en GitHub a mano.

## NUEVO22 (2026-11-12 ~13:02 CST)
Jonathan probó el APK debug de NUEVO21 y pidió 8 cambios con dos capturas (Artistas y Álbumes).
(1) **Contador del encabezado quitado**: el "309/309" de arriba a la derecha ya no se muestra (era la posición dentro de la cola actual; se quitó el elemento `#tPos`, su CSS y la función `updateScreenMeta` con sus 4 llamadas).
(2) **"A continuación" oculta al abrir**: al abrir la app, la cola del reproductor empieza colapsada (botón "Mostrar ▾" para verla); el botón ☰ del mini reproductor la abre y baja hasta ella.
(3) **Descargar en online no te saca de YouTube**: al tocar Descargar en un resultado de YouTube, la descarga arranca en segundo plano y te quedas en la pestaña YouTube; un aviso flotante confirma y tiene el botón "Ver Descargas" si quieres ver el porcentaje (antes saltaba directo a Descargas).
(4) **Letras estilo karaoke**: la ventana de la letra muestra 7 líneas — la que suena en el centro, 3 arriba y 3 abajo — con desplazamiento suave, la línea actual más grande con brillo cian y los bordes superior/inferior difuminados.
(5) **Accesos directos con animación fluida**: el repliegue al bajar en la Biblioteca ahora anima la altura REAL medida con una curva suave (antes animaba un max-height fijo de 320px, así que la primera parte no se veía y luego se cerraba de golpe: eso era lo tosco).
(6) **Álbumes y Artistas ocupan toda la pantalla**: causa raíz — las reglas CSS de `.pl-create` (display:flex), `.grp-grid` (display:grid) y `.grp-rows` (display:flex) anulaban el atributo `hidden`, así que la fila "Nombre de la lista… / Crear" se veía en TODAS las sub-pestañas y la rejilla y las filas vacías se repartían la mitad de la pantalla cada una. Arreglo: reglas `.pl-create[hidden], .grp-grid[hidden], .grp-rows[hidden]{ display:none; }` (mismo patrón que ya usaban `.view[hidden]` y `#libPistas[hidden]`). Además la rejilla de álbumes ahora usa `repeat(2,minmax(0,1fr))` y las tarjetas `min-width:0` para que las 2 columnas no se estiren a una sola por el contenido.
(7) **Recomendaciones solo música**: las sugerencias de YouTube ("A continuación" y radio) y las búsquedas ya NO usan el respaldo a YouTube normal (`ytsearch`), que era el que metía videos; ahora es solo YouTube Music (music.youtube.com) en los tres caminos (búsqueda, mismo artista y artistas parecidos).
(8) **Búsquedas con más resultados**: el límite subió de 20 a 30 en JS y en el plugin (máx 30); se muestran todos los que devuelva YouTube Music (más de 15 cuando sea posible).
Validado: node --check OK (3 bloques), 144 ids y 0 faltantes, 0 referencias a tPos/updateScreenMeta; javac OK; cap sync OK; md5 igual entre www/index.html y la copia en android (4320e958). Pruebas Node con las funciones reales (22 pasaron, 0 fallaron): setShortcutsHidden (ocultar/mostrar, doble llamada), setQueueCollapsed (colapsada al inicio, botón Mostrar/Ocultar), objetivo de scroll de la letra (centra la línea, primera línea sin scroll negativo), estáticos (sin tPos, reglas [hidden], límite 30, sin cambio de vista al descargar, ventana de 7 líneas). ZIP chico gurru-music-NUEVO22.zip: www/index.html, YtDlpDownloaderPlugin.java, README.md.

## NUEVO23 (2026-11-12 ~20:18 CST)
Jonathan mandó 2 videos y 4 capturas con 6 pedidos, todos del estilo de YouTube Music (dijo "haz tu magia" y se hizo la ronda completa).
(1) **Accesos directos en su propia pestaña**: el gesto de esconderlos al bajar se veía feo (se encimaban con "Reproducir todo" y con las canciones, como se ve en su video). Ahora viven en su propia sub-pestaña "Accesos" (Pistas / Álbumes / Listas / Artistas / Carpetas / Accesos), siempre visibles, sin gesto ni animación de repliegue. Se quitó setShortcutsHidden, el listener de scroll de la lista y el CSS .sc-hidden; renderShortcuts ahora filtra los artistas por el texto de búsqueda si hay uno escrito. La rejilla muestra hasta 12 artistas (antes 6) y hay un mensaje vacío si aún no hay nada que escuchar.
(2) **Descargar la canción que suena no cambia la cola**: al darle a Descargar en el reproductor con una canción online, la cola saltaba de "1/1" a "311/311" (la de la Biblioteca), como se ve en su video. Causa: al descargar, la canción pasa de type "youtube" a "local" y queueIndices() arma la cola según el tipo de la actual. Arreglo: una variable queueOrigin que se fija cuando empieza a sonar una canción (en loadTrack y al restaurar el estado); queueIndices usa ese origen, y si la actual ya no está en esa cola (porque se descargó), se pone primera como la que está sonando. Al poner otra canción (local u online) el origen cambia con ella. Los botones ↑ ↓ de la fila actual se deshabilitan cuando la actual está añadida así, para no corromper el puntero de reproducción.
(3) **Letras estilo YouTube Music con la esencia de Gurru**: la letra ya no es una cajita de 7 líneas: es a pantalla completa, con la portada y el título/artista arriba, la letra en tipografía grande itálica serif, la línea que suena en blanco brillante con brillo cian y las demás apagadas, y abajo un botón "Compartir" (comparte el texto con la hoja de compartir de Android; si no está disponible, lo copia al portapapeles). El resaltado sigue centrado en la línea que suena con desplazamiento suave (el cálculo usa la altura real del cuerpo, así que funciona con la pantalla completa). Las letras sin tiempos se parten por línea para que se vean igual de grandes.
(4) **"A continuación" como hoja inferior**: en el reproductor, la cola es una hoja con una barrita con tirador en la parte de abajo: con el gesto hacia arriba se abre, hacia abajo se cierra, y un toque en la barrita o en el encabezado la alterna (el botón Mostrar/Ocultar sigue). La apertura y el cierre animan la altura real medida (el patrón de NUEVO22, sin el salto del max-height fijo); al abrir la app sigue empezando colapsada. El mini reproductor ☰ la sigue abriendo.
(5) **Búsquedas de YouTube con recientes e historial**: al entrar a YouTube con el buscador vacío se ven las "Búsquedas recientes" (tarjetas con portada de las canciones online que se pusieron, máx. 10) y el "Historial" (términos buscados con icono de reloj; tocar uno busca de nuevo, la flecha ↗ lo pone en el buscador sin buscar; botón "Borrar" limpia el historial). Claves nuevas: gurru-yt-history-v1 (términos, sin duplicados, máx. 10) y gurru-yt-recent-v1 (canciones, por videoId, máx. 10). Al vaciar el buscador vuelven las recientes y el historial. La última búsqueda con sus resultados ya solo se restaura si el buscador sigue teniendo esa búsqueda escrita; si está vacío se ve el historial (la última búsqueda queda primera en él, a un toque).
(6) **Página del artista**: tras una búsqueda, la ventana de elegir tiene el botón "Ver artista" (también se abre tocando el nombre del artista en el reproductor). La página muestra el artista, sus 10 canciones más populares y sus álbumes. Verificado antes de construir con yt-dlp real: el canal del artista en music.youtube.com no tiene pestañas de videos ni de listas (el extractor redirige y falla), y los ids de lista VLPL/MPRE de la búsqueda dan 404 al resolverlos, así que los álbumes NO salen de YouTube: salen de iTunes (sin clave, ya usado en NUEVO17/18 — búsqueda entity=album da título, portada y año; lookup por collectionId da las canciones del álbum). Las 10 más populares salen de la búsqueda de YouTube Music del artista (ordenada por relevancia, como en YouTube Music) con el plugin que ya existe. Tocar una canción la pone a sonar directo (la descarga queda en el botón ⬇ del reproductor); tocar un álbum muestra sus canciones y tocar una busca en YouTube Music "canción artista" y suena el primer resultado. Botones "Aleatorio" (una del top 10 al azar con aleatorio prendido) y "Mix" (suena el top 10 desde la primera y la radio sigue con sugerencias). Atrás (gesto, Escape y botón ←) vuelve a YouTube; dentro de un álbum, atrás vuelve al artista.
Validado: node --check OK (3 bloques), 175 ids y 0 faltantes, 0 referencias a setShortcutsHidden/sc-hidden; javac OK (sin cambios en Java en esta ronda); cap sync OK; md5 igual entre www/index.html y la copia en android (6ebb1027). Pruebas Node con las funciones reales (51 pasaron, 0 fallaron): queueIndices con queueOrigin (cola online tras descargar, cola local al poner una local, sin queueOrigin cae al origen de la actual), setQueueCollapsed (colapsada al inicio sin animar, abrir/cerrar con altura medida y limpieza del temporizador), renderShortcuts (filtro por búsqueda, mensaje vacío, dedupe por firma), syncLibPanes con "accesos", recordYtSearch/recordYtRecent (sin duplicados, tope 10, la más reciente primera), renderYtHistory (visible sin resultados, oculta con resultados o sin nada), restoreLastYtSearch (no restaura con el buscador vacío, sí con la misma búsqueda), openArtist (top 10 de 25 resultados, álbumes sin duplicados, portada 600px, tocar el top 1 suena, álbum con lookup y sus canciones, cerrar el álbum vuelve al artista). ZIP chico gurru-music-NUEVO23.zip: www/index.html, README.md (sin cambios en Java en esta ronda: el plugin sigue siendo el de NUEVO22).
Actualización 2026-11-12 ~20:40 CST: Jonathan eligió "Sí, súbelo a GitHub como NUEVO17" y Muse lo subió con push-files (commit 24663ce6, verificado byte-idéntico en los 2 archivos: index.html 6ebb1027, README 2df80962); la corrida #46 salió en verde (completed/success) — el APK NUEVO23 ya está listo para descargar. Pendiente: Jonathan descarga el APK de la corrida #46 (Actions → run #46 → artifact gurru-music-apk), desinstala la app vieja una vez e instala app-debug.apk, y prueba: (a) pestaña Accesos con los artistas y sin gesto feo; (b) descargar la canción que suena en online y que la cola siga en el online; (c) letras a pantalla completa con Compartir; (d) la cola con la barrita y el gesto; (e) recientes e historial en YouTube; (f) página de Belanova con top 10 y álbumes (Fantasía Pop) y una canción de un álbum.

## NUEVO24 (2026-11-12 ~21:28 CST)
Jonathan mandó un video y dos capturas con 4 pedidos y un bug, y dijo "haz tu magia".
(1) **Letra como hoja inferior suave, estilo YouTube Music**: la letra ya no es a pantalla completa: ahora es una hoja que SUBE suave desde abajo (transición de 0.45 s con la misma curva que la cola) y BAJA suave al cerrarla. Cuando está abierta, arriba queda el REPRODUCTOR MINIMIZADO: una barrita con la portada chica, el título y el artista, y el botón de pausa (que pausa y reanuda, y se mantiene al día con syncLyricsMini desde syncMiniPlayer). Un toque en esa barrita vuelve al reproductor. Debajo va el encabezado "Letra" con la X y la fuente de la letra. La letra sigue siendo grande itálica serif, con la línea que suena en blanco brillante y las demás apagadas, y se desplaza sola. El botón Compartir ahora es una píldora FLOTANTE abajo al centro (como en YouTube Music), no una barra al pie; el cuerpo tiene espacio abajo para que la última línea no quede tapada. La hoja se puede BAJAR CON EL DEDO desde el reproductor minimizado o desde el encabezado "Letra": sigue al dedo y al soltar, si bajó más de 90 px, se cierra; si no, vuelve sola. El botón de pausa no arrastra (un toque en él solo pausa). Cerrar con la X, con atrás o con Escape usa la misma bajada animada (se quita "open" y a los 460 ms se oculta el overlay).
(2) **Barrita de "A continuación" quitada**: se quitó el tirador (.q-handle/.q-grip) de la hoja de la cola y su gesto; la hoja se abre y se cierra con el botón Mostrar/Ocultar o tocando el encabezado. La altura colapsada se mide solo con el encabezado.
(3) **Pestaña de YouTube a toda la pantalla, sin marco**: se quitó la tarjeta .yt-box (borde rojo, fondo y márgenes); el buscador, las recientes, el historial y los resultados van directos sobre el fondo, como la Biblioteca. El diseño de recientes e historial no cambió (a Jonathan le gustó como se ve).
(4) **"Ver artista" en la lista de resultados + arreglo del bug**: cada resultado de la búsqueda tiene ahora su botón "Ver artista" (píldora cian a la derecha de la fila) que abre la página del artista directo, sin tocar la canción ni pasar por la ventana de elegir. La fila tiene dos toques: la zona principal (portada + título + canal) abre la ventana de elegir como antes, y el botón abre el artista de ESE resultado. **Bug encontrado y arreglado**: al tocar "Ver artista" no salía nada. Causa raíz, verificada con yt-dlp real: la búsqueda de YouTube Music (`music.youtube.com/search`, flat-playlist) NO trae `uploader` ni `channel` en sus entradas (ambos None), así que el plugin ponía `channel = "YouTube"`, la canción quedaba con artista "YouTube" y el botón tenía la guarda `if (name && name !== "YouTube")` que lo bloqueaba en silencio. Arreglo: nuevo `openArtistForTrack(t)` que lee el artista REAL de la metadata con `resolveArtist` (getVideoInfo, ya usado en NUEVO12 para las sugerencias); si no hay metadata, usa lo que el usuario buscó (`searchQuery`); si tampoco hay nada, avisa "No pude saber el artista de esta canción" en vez de no hacer nada. El botón de la ventana de elegir y el toque en el artista del reproductor usan el mismo camino. `openArtistForResult(r, query)` hace lo mismo desde un resultado sin agregarlo a la Biblioteca.
Validado: node --check OK (3 bloques), 178 ids y 0 faltantes, 0 referencias a queueHandle/q-grip/q-handle/lyricsCover/lyricsTitle/lyricsArtist/yr-go/yt-box/lyrics-foot; javac OK (sin cambios en Java en esta ronda); cap sync OK; md5 igual entre www/index.html y la copia en android (47652c51). Pruebas Node con las funciones reales (45 pasaron, 0 fallaron): setQueueCollapsed sin barrita (altura colapsada = encabezado + 8), openArtistForTrack (metadata real gana, sin metadata usa la búsqueda, si solo hay título usa la búsqueda, sin artista avisa y no abre, sin videoId no hace nada), openArtistForResult, renderYtSearchResults (fila con zona principal + botón, cada toque abre lo suyo sin cruzarse), hoja de la letra (abrir con show+open, mini con título/artista/portada/pausa, cerrar animado con show hasta los 460 ms, reabrir cancela el cierre, arrastre corto que vuelve sola, arrastre largo que cierra, toque en el mini que cierra, pausa que no arrastra, icono en pausa). ZIP chico gurru-music-NUEVO24.zip: www/index.html, README.md (sin cambios en Java en esta ronda: el plugin sigue siendo el de NUEVO22).
Actualización 2026-11-12 ~21:55 CST: Jonathan eligió "Sí, súbelo a GitHub como NUEVO17" y Muse lo subió con push-files (commit df2ca957, verificado byte-idéntico en los 2 archivos: index.html 47652c51, README 684bc4cb); la corrida #47 salió en verde (completed/success) — el APK NUEVO24 ya está listo para descargar. Pendiente: Jonathan descarga el APK de la corrida #47 (Actions → run #47 → artifact gurru-music-apk), desinstala la app vieja una vez e instala app-debug.apk, y prueba: (a) la letra sube suave con el mini reproductor arriba y baja con el dedo; (b) ya no hay barrita en "A continuación"; (c) la pestaña YouTube a toda la pantalla sin marco; (d) el botón "Ver artista" en cada resultado abre la página de Moenia/Belanova con top 10 y álbumes.

## NUEVO25 (2026-11-12 ~22:09-22:14 CST)
Ronda pedida por Jonathan en 4 mensajes seguidos (sin hold: dio las instrucciones directas y Muse las aplicó). 7 cambios, todos en www/index.html (sin cambios en Java en esta ronda: el plugin sigue siendo el de NUEVO22).
(1) **Búsquedas recientes sin mezclarse con "A continuación"**: antes, CUALQUIER canción online que sonaba (cola, radio, sugerencias, artista) entraba a las recientes. Ahora solo entran las canciones que se ponen DESDE la lista de búsqueda: al abrir un resultado se marca la canción (`fromYtSearch`), `loadTrack` la registra una sola vez al sonar y consume la marca; Descargar o "Solo agregarlo" la quitan para que al ponerla después desde la Biblioteca no entre. La cola, la radio y el artista ya no se mezclan ahí.
(2) **Cola del artista: el top 10 o el álbum completo**: al elegir una canción del top 10, "A continuación" muestra las 10 del top (suena la que se tocó, las demás quedan en la cola); al abrir una canción de un álbum, la cola se rellena con las canciones de ESE álbum (la que suena primero, las demás se van agregando en orden mientras se buscan en segundo plano). Nuevo `playArtistQueue(newTracks, startPos, mixName)` con `activeMix` (la cola es la mezcla): no se pasa por `playTrack` porque su `clearOnlineQueueExcept` borraría la cola y la mezcla; las online viejas se quitan de la lista (las locales y descargadas se quedan) y no se duplican. Al terminar el top 10 o el álbum, la radio sigue con sugerencias como siempre.
(3) **Atrás desde el reproductor vuelve al artista**: al entrar al reproductor desde la página del artista, el botón atrás (y Escape en web) vuelve al artista, siguiendo la ruta con la que se llegó. `showView` guarda `playerReturnTo = "artist"` cuando se entra al reproductor desde el artista (si se entra desde Biblioteca o YouTube, no guarda nada y el atrás va a Biblioteca como antes).
(4) **Canciones del artista desde su perfil oficial**: verificado con yt-dlp real que el top 10 de Moenia y Belanova SÍ sale del canal oficial (uploader `moeniamx` / `Belanova`, channel_id UCHPy9LFAXSGiSi2w70tKGOQ / UC6G7SY8FN_71DHrfq8f0VZQ), así que el top 10 no cambió; el riesgo estaba en las canciones del álbum (la búsqueda "canción + artista" puede traer primero un video de letra o un cover). Nuevo `pickOriginalTrack(list, artistName)`: descarta títulos con "(Letra)", lyrics, cover, karaoke, remix, en vivo/live o instrumental, y entre las que quedan lee quién subió el video (`getVideoInfo`, campo `uploader`, ya lo devolvía el plugin) y prefiere la subida desde el perfil del artista (normalizado: "moeniamx" contiene "moenia"); si ninguna es del perfil, se queda con la primera filtrada. Se usa al abrir una canción del álbum y al rellenar su cola.
(5) **Letra: sin Compartir y nota musical cuando no hay letra**: se quitó el botón flotante "Compartir" de la hoja de la letra (botón, CSS y manejador; el relleno inferior del cuerpo vuelve de 84px a 30px). Cuando una canción solo tiene sonido sin letra (ninguna fuente la encontró), en lugar del texto "No se encontró la letra..." ahora se ve un dibujo grande de una nota musical ♪ (110px, brillo cian, flotando suave) con el botón Reintentar debajo.
(6) **La letra siempre marcada (el seguimiento nunca se pierde)**: antes, con letra sin tiempos (texto plano de lyrics.ovh / some-random-api) NUNCA se marcaba ninguna línea, y al inicio (antes de la primera línea) no había ninguna marcada. Ahora: con tiempos, la primera línea queda marcada desde el inicio (nunca se desmarca todo); sin tiempos, la línea se estima por el avance de la canción (`lyricsLineTime`: línea i de N en una canción de D segundos suena en (i+0.5)/N*D) y se marca y se centra igual que con tiempos.
(7) **Tocar una línea de la letra busca ese momento**: tocar cualquier línea de la letra adelanta o atrasa la música hasta donde está indexada esa parte (`lyricsLineTime` + `seekToFraction`; con tiempos, el de la línea; sin tiempos, el estimado por el avance). Funciona en audio local y en online.
(3b) **"Ver artista" en UNA sola tarjeta, no en cada canción** (corrección de Jonathan al diseño de NUEVO24): se quitó el botón "Ver artista" de cada fila de resultados y también de la ventana de elegir (al tocar una canción ya no sale esa opción). Ahora hay UNA sola tarjeta "Ver artista" arriba de los resultados de la búsqueda, aparte de la música, con la portada del primer resultado y lo que se buscó ("moenia", "belanova"...); al tocarla abre la página del artista con el artista real de ese video (mismo camino de NUEVO24: metadata, si no, la búsqueda). El toque en el nombre del artista del reproductor sigue abriendo el artista.
Validado: node --check OK (3 bloques), 176 ids y 0 faltantes (se quitaron choiceArtistBtn y lyricsShareBtn), 0 referencias a choiceArtistBtn/lyricsShareBtn/yr-artist/yr-main/lyrics-share; javac OK (sin cambios en Java en esta ronda); cap sync OK; md5 igual entre www/index.html y la copia en android (b2cb4d58). Pruebas Node con las funciones reales (6 + 20 pasaron, 0 fallaron): recientes solo desde la lista de búsqueda (entra una vez, la cola no entra, la local no entra, sin duplicados, la última primero); top 10 completo como cola (10 en "A continuación", suena la que se tocó con el artista real, la online vieja fuera, la local se queda); regreso al artista (desde artista guarda, desde biblioteca/YouTube no); tarjeta única (1 tarjeta + N filas, la tarjeta abre el artista del primer resultado, la fila abre su canción); letra marcada siempre (con tiempos marca la primera al inicio y la correcta a mitad/final; sin tiempos estima por avance; sin líneas no marca); tocar línea busca (con tiempos 15s, plano 125s de 200s, seek real a 125s); original del álbum (descarta "(Letra)"/cover y prefiere moeniamx, sin perfil el primero filtrado, sin plugin el primero filtrado); cola del álbum en orden (ALB1, ALB2, ALB3, ALB4 con la que suena en su lugar). ZIP chico gurru-music-NUEVO25.zip: www/index.html, README.md (sin cambios en Java en esta ronda: el plugin sigue siendo el de NUEVO22).
Actualización 2026-11-12 ~22:25 CST: Jonathan eligió "Sí, súbelo a GitHub como NUEVO17" y Muse lo subió con push-files (commit 981e0411, verificado byte-idéntico en los 2 archivos: index.html b2cb4d58, README e09d0751); la corrida #48 salió en verde (completed/success) — el APK NUEVO25 ya está listo para descargar. Pendiente: Jonathan descarga el APK de la corrida #48 (Actions → run #48 → artifact gurru-music-apk), desinstala la app vieja una vez e instala app-debug.apk, y prueba: (a) las recientes solo tienen canciones puestas desde la lista de búsqueda; (b) del top 10 la cola muestra las 10, y del álbum las del álbum; (c) atrás desde el reproductor vuelve al artista; (d) las del álbum suenan las originales del perfil; (e) sin letra se ve la nota musical y no hay Compartir; (f) la letra siempre tiene una línea marcada; (g) tocar una línea adelanta/atrasa la música; (h) una sola tarjeta "Ver artista" arriba de los resultados.

## NUEVO26 (2026-11-12 ~22:46-22:51 CST)
Ronda pedida por Jonathan en un mensaje largo con dos capturas (sin hold: dio las instrucciones directas y Muse las aplicó), más un refinamiento: la animación de la letra es muy rápida al pasar de una línea a otra y necesita una transición más suave. 6 cambios, en www/index.html y en el plugin (YtDlpDownloaderPlugin.java).
(1) **Búsqueda de YouTube: tocar suena directo, icono de descarga por fila, sin ventana de elegir**: al tocar una canción en los resultados de YouTube ya no sale la ventana "Escuchar / Descargar": la canción se abre directo en el reproductor. Cada fila tiene a la derecha un icono de descarga ⬇ que al tocarlo baja la canción en segundo plano (sin salir de la búsqueda, con el aviso "Descargando…: el avance está en Descargas" y el botón "Ver Descargas"). Las tarjetas de búsquedas recientes también suenan directo. La ventana de elegir se quitó POR COMPLETO (HTML, CSS, openChoice/closeChoice, sus 3 manejadores, la rama de atrás y la actualización del oembed). Nuevo `ytTrackForResult` (reusa la canción si ya está en la Biblioteca, sin duplicarla) + `playYtSearchResult` (marca fromYtSearch, así que sigue entrando a recientes solo desde la lista; si la que se toca ya es la que suena, solo pausa/reanuda) + `downloadYtSearchResult` (no entra a recientes). El selector de formato MP3/M4A, que vivía dentro de la ventana, se mudó a Configuración.
(2) **Configuración: calidad de descarga Baja / Media / Alta, por defecto Alta**: nueva sección "Descargas" en Configuración con el formato (MP3/M4A, mudado de la ventana) y la calidad (Baja / Media / Alta; la predeterminada es Alta). Se guarda en `gurru-dl-quality` y se pasa a `downloadAudio` como `quality`. En Java, la calidad se mapea al selector de formato de yt-dlp (Alta: `bestaudio/best`; Media: `bestaudio[abr<=96]/bestaudio/best`; Baja: `bestaudio[abr<=48]/bestaudio/best`). Además, la descarga ahora prueba primero con el cliente por defecto de yt-dlp (el que ofrece el audio DASH de alta calidad, itag 140 ~130 kbps, y el que hace reales las calidades media/baja — con el cliente Android solo existe el formato 18 ~48 kbps y las tres calidades saldrían iguales) y si YouTube lo bloquea con "Sign in to confirm you're not a bot" reintenta con el cliente Android, igual que ya hace la caché desde NUEVO16 (se borran los parciales dl_<id>.* entre intentos). Los avisos ya no dicen "máxima calidad" fijo: dicen la calidad elegida.
(3) **Letra: UX más fluida y transición más suave entre líneas**: Jonathan dijo que la animación al pasar de una línea a otra era muy rápida. La transición de la línea que suena pasa de 0.35 s a 0.8 s con ease-in-out (opacidad, color, tamaño y brillo se deslizan en vez de cambiar de golpe). El desplazamiento también cambia: en vez del scroll "smooth" del navegador (rápido y que se corta si se llama seguido), nuevo `lyricsScrollTo` anima ~900 ms con ease-in-out y se cancela si llega otro antes de terminar; y solo se mueve cuando la línea que suena sale de la banda central (35%-65% de la ventana): si ya se ve, no se pelea con el dedo del usuario. Las líneas entran con un fundido suave al cargar la letra (keyframes lyrIn) y responden al toque con un parpadeo (cursor pointer + :active).
(4) **Cola del álbum: una sola búsqueda, no una por canción**: Jonathan reportó que al elegir una canción de un álbum, "A continuación" no se llenaba con la música del álbum, solo salía esa canción. Causa: NUEVO25 buscaba cada canción del álbum una por una en secuencia (10 búsquedas de varios segundos cada una), así que la cola se quedaba en una sola canción mucho rato. Arreglo: nuevo `resolveAlbumVideos(album)` — UNA sola búsqueda de "álbum artista" en YouTube Music devuelve las canciones del álbum — y `matchAlbumResults` las empareja por título normalizado (`normTitle`: minúsculas, sin acentos ni signos; se quitan "(Live)", "(Demo Version)", " - Artista..." y se descartan en vivo/letra/cover/remix/demo; cada video se usa una sola vez). Verificado con yt-dlp real: la búsqueda "Fantasía Pop Belanova" devuelve Baila Mi Corazón, Rockstar, Toma Mi Mano, Cada Que..., Aún (y versiones Live/Demo que se descartan); "Stereo Hits Moenia" devuelve Manto Estelar, Tren al Sur, En Algún Lugar, Lobo Hombre en París. `playAlbumTrack` ahora busca primero el álbum completo (la que se tocó suele salir ahí) y `fillAlbumQueue` mete las emparejadas directo y busca las que faltan EN PARALELO (no en secuencia), siempre en orden del álbum y con el token que detiene un relleno viejo si se abre otro álbum.
(5) **"Ver artista" por el artista, no por la canción**: Jonathan mandó capturas: buscar "belanova" y tocar "Ver artista" mostraba a otro artista ("Peso Pluma, Tito Double P, Jesús Roberto Laija García, Daniel Candia") porque la tarjeta abría el artista de la PRIMERA canción de la búsqueda, y esa canción se llama "BELANOVA" (de Peso Pluma). Arreglo: la tarjeta ahora abre `openArtist(query)` directo — la página es del artista buscado. Además, nuevo `verifyArtistTop`: en segundo plano lee el artista real de los primeros 12 resultados (getVideoInfo, con caché en memoria `artistInfoCache`), toma el canal del primer resultado cuyo artista real sea el buscado como oficial, y sube arriba las canciones de ese canal (las demás quedan después, en su orden). Si no se puede verificar, el orden queda como lo dio la búsqueda; si se cambia de artista a mitad, no se reordena. `renderArtistTop` ahora pinta el top 10 aparte (lo usan la búsqueda y la verificación). `openArtistForResult` se quitó (la tarjeta ya no lo usa).
Validado: node --check OK (3 bloques), 168 ids y 0 faltantes (se quitaron 9 ids de la ventana y se agregó qualityRow), 0 referencias a openChoice/closeChoice/choiceCtx/choiceOverlay/openYtResult/openArtistForResult/yr-go; javac OK; cap sync OK; md5 igual entre www/index.html y la copia en android (629bf884). Pruebas Node con las funciones reales (27 + 7 pasaron, 0 fallaron): normTitle (acentos, mayúsculas); matchAlbumResults con los datos reales de yt-dlp (Belanova: exacta gana, Aún por contención, Rockstar/Toma Mi Mano/Cada Que... no emparejan porque solo hay Live/Demo/Con Letra, sin video repetido; Moenia: exactas ganan, la En Vivo no se usa); verifyArtistTop (las oficiales de Belanova suben y la de Peso Pluma baja, se repinta, no reordena si se cambió de artista); tocar resultado (abre el reproductor, carga, entra a recientes, no duplica, la que suena solo pausa/reanuda); icono de descarga (arranca la descarga, avisa, no entra a recientes); scroll de letra (llega al destino, ~900 ms, distancia mínima salta directo); cola del álbum (las 4 en orden, la que falta se buscó una sola vez, la que suena en su lugar, el relleno viejo no mete canciones de otro álbum); resolveAlbumVideos (una sola búsqueda). ZIP chico gurru-music-NUEVO26.zip: www/index.html, YtDlpDownloaderPlugin.java, README.md.
Actualización 2026-11-12 ~23:02 CST: Jonathan eligió "Sí, súbelo a GitHub como NUEVO17" y Muse lo subió con push-files (commit c1a043d2, verificado byte-idéntico en los 3 archivos: index.html 629bf884, plugin 844f22fd, README ded3ed16); la corrida #49 salió en verde (completed/success) — el APK NUEVO26 ya está listo para descargar. Pendiente: Jonathan descarga el APK de la corrida #49 (Actions → run #49 → artifact gurru-music-apk), desinstala la app vieja una vez e instala app-debug.apk, y prueba: (a) tocar una canción en YouTube suena directo sin ventana; (b) el icono ⬇ de cada fila descarga sin salir de la búsqueda; (c) en Configuración, cambiar la calidad a Baja/Media/Alta y que las descargas salgan con ese tamaño/calidad (Alta es la predeterminada); (d) la letra pasa de una línea a otra con una transición lenta y suave y no pelea con el dedo; (e) al elegir una canción de un álbum, "A continuación" se llena con las del álbum en unos segundos; (f) buscar "belanova" y tocar "Ver artista" muestra Belanova (no Peso Pluma) con sus canciones arriba.

## NUEVO27 — 2026-11-12 ~23:20 CST (top 10 solo del perfil oficial, carátula oficial, álbum sin esperar, precarga)

Pedidos por Jonathan 2026-11-12 23:15 CST tras instalar NUEVO26 y probar "Ver artista" con Belanova (capturas: el top 10 mezclaba "Por Esta Vez" con en vivos, el episodio "Loretta y Más... Episodio 3" y "¿BELANOVA REGRESA?"; la carátula mostraba la miniatura de la canción "BELANOVA" de Peso Pluma; la cola del álbum no se llenaba; y abrir canciones tardaba demasiado).

Verificación previa (yt-dlp 2026.10.26, sandbox):
- El canal de Belanova (UCHPy9LFAXSGiSi2w70tKGOQ) NO tiene pestaña de videos ni en youtube.com ("This channel does not have a videos tab", también con cliente android): es un canal de artista autogenerado. El top 10 NO puede sacarse del perfil directamente; tiene que salir de la búsqueda de YouTube Music + filtro estricto. Confirmado también que --dump-json del canal no devuelve nada útil.
- La búsqueda "belanova canciones" SÍ devuelve más canciones oficiales (Toma Mi Mano, No Me Voy A Morir, Por Ti, Soñar, Cada Que...), así que sirve para completar el top 10 cuando la primera búsqueda da pocas oficiales.
- iTunes NO devuelve foto del artista (musicArtist "belanova" → artworkUrl100 None): la carátula de "Ver artista" no puede ser la foto del artista; tiene que ser la portada de la primera canción oficial.

1. **Top 10 solo con música del perfil oficial (`buildArtistTop`, reemplaza `verifyArtistTop` de NUEVO26)**: se verifican los primeros 15 resultados por metadata (artista → canal oficial); se quedan solo los videos subidos por ese canal Y con título de canción (filtro `SONG_BAD`: en vivo/live, episodio, podcast, entrevista, regresa, documental, detrás/behind, reacción, cover, karaoke, remix, instrumental, letra/lyrics, "mix 20", recopilación, "mejores canciones"). Si quedan menos de 10, se hace una segunda búsqueda "<artista> canciones" y se completa (sin duplicar). Corre en segundo plano: el top se pinta con la búsqueda y se corrige al terminar. Con los datos reales de Belanova: 5 oficiales de la 1a búsqueda + 3 de la 2a = 8 canciones limpias (la de Peso Pluma, los 3 en vivo, el episodio y el video de noticias quedan fuera).
2. **Carátula de "Ver artista"**: `renderArtistTop` solo pone `top[0].thumbnail` cuando `artistState.topVerified` es true; antes queda el logo. La carátula es la de la primera canción OFICIAL (p. ej. "Por Esta Vez"), no la de la canción de Peso Pluma.
3. **Cola del álbum (NUEVO27)**:
   - `playAlbumTrack` ya NO espera la búsqueda del álbum para que suene la canción: la búsqueda por canción y la búsqueda del álbum corren en paralelo; la que se tocó suena en cuanto sale su búsqueda, y la cola se rellena cuando llega la del álbum (`maybeFill`: solo cuando ya empezó la canción Y ya llegó la búsqueda del álbum). Antes la canción tardaba porque esperaba el álbum completo.
   - `resolveAlbumVideos` verifica que las emparejadas por título sean del perfil oficial (metadata: artista → canal oficial); las que no lo son se caen del emparejado y se buscan una por una con `pickOriginalTrack` (que ya prefiere el perfil).
   - La guardia de `fillAlbumQueue`/`insertAt` ya no frena si el panel del álbum se cerró: solo se detiene si hay OTRO álbum abierto (la mezcla y el token ya cuidan lo viejo).
4. **Velocidad: precarga de la siguiente canción (`prefetchNext`)**: cuando empieza a sonar una canción ya en caché, se descarga en segundo plano la siguiente canción online de la cola que aún no tiene caché, para que abra al instante al tocarla o al pasar a ella. Solo una por vez y nunca mientras se está preparando la actual (`cacheBusy`/`prefetchBusy`). La precarga usa el mismo caché invisible (LRU 3) y no aparece en Descargas. Si falla, no pasa nada: la siguiente se prepara normal al tocarla.

Archivos: solo `www/index.html` (337,171 B, md5 145f5d18d8aaa851d4cacb50d9851b0e). Sin cambios en Java.

Validado: node --check OK (3 scripts), 168 ids / 152 refs / 0 faltantes, javac OK, cap sync OK (md5 igual en android/assets). Pruebas Node: 3 en test A (top 10 oficial con datos reales, carátula oficial, 2a búsqueda) + 5 en test B (el álbum ya no bloquea el inicio, la cola se rellena al llegar la búsqueda, precarga de la siguiente con sus guardias) — todas pasaron.

Pendiente: subir a GitHub (www/index.html + README.md) y verificar la corrida #50 en verde (APK debug: desinstalar la app vieja para instalarlo). Probar: (a) "Ver artista" con Belanova muestra solo canciones de estudio de Belanova (sin en vivo, sin episodio, sin "¿BELANOVA REGRESA?", sin la de Peso Pluma); (b) la carátula de "Ver artista" es una portada de Belanova, no la de Peso Pluma; (c) al tocar una canción de un álbum, la canción suena rápido y "A continuación" se llena con las del álbum; (d) las canciones del álbum son del perfil original; (e) al escuchar una lista, la siguiente canción abre casi al instante (precarga).
