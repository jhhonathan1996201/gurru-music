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
