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
Validado: node --check OK (3 bloques), 114 ids usados y ninguno falta, 0 referencias a loadDiscover/Descubre/mix-chip; cap sync OK; md5 igual entre www/index.html y la copia en android (e4796535). Pruebas Node con las funciones reales: makeFbTrack (forma completa de Facebook), alternancia A1→B1→A2→B2→A3, fetchSimilarTracks simulado (Miranda!, Fey, Thalía, una por artista, todas con recommended=true) y ensureYtSuggestions simulado (cola S,R,S,R,…). ZIP chico gurru-music-NUEVO18.zip: www/index.html, README.md (sin cambios en Java). Pendiente: subir NUEVO18 (Muse lo puede subir con push-files como NUEVO17, si Jonathan lo pide), esperar el verde, instalar y probar: (a) Facebook → pegar el link de "Por Ti" ya no da error y abre el diálogo Escuchar/Descargar; (b) YouTube → sonar Belanova y ver "A continuación" alternando "Sugerencia" (violeta, mismo artista) y "Recomendación" (verde, parecido) y que al terminar siga la radio igual; (c) Biblioteca → estante de mezclas con portadas, se puede plegar con el título y al tocar una suena la mezcla.
