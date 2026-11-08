# Gurru Music — Descargas con yt-dlp DENTRO del APK

> Fecha: 2026-11-08. Esto es lo que se empaquetó para que las descargas
> funcionen sin Termux y sin servidor externo.

## Qué se empaquetó

**youtubedl-android 0.17.x** (fork JunkFood02, librería open source) +
su artefacto **ffmpeg 0.17.x**, como dependencias Gradle en
`android/app/build.gradle`:

```gradle
implementation "io.github.junkfood02.youtubedl-android:library:0.17.+"
implementation "io.github.junkfood02.youtubedl-android:ffmpeg:0.17.+"
```

Es el enfoque que usan apps como Seal, YTDLnis y SealPlus: un wrapper
Kotlin/Java que empaqueta **CPython y el binario yt-dlp como librerías
nativas (.so) dentro del APK**, en `jniLibs` por ABI (armeabi-v7a,
arm64-v8a, x86, x86_64). En el primer uso, la app extrae Python + yt-dlp
del APK (tarda unos segundos) y luego ya quedan instalados en el teléfono.

- Serie verificada: **0.17.x** (Python 3.11.10 en 0.17.1). Existe 0.18.x,
  pero no se adopta en este paso.
- `android:extractNativeLibs="true"` en el Manifest, para que el sistema
  extraiga esos .so al instalar.
- FFmpeg es necesario para `--extract-audio` (convertir a MP3/M4A).

## El plugin nativo

**Clase:** `android/app/src/main/java/com/gurru/music/YtDlpDownloaderPlugin.java`
**Nombre para JavaScript:** `YtDlpDownloader`
**Registro:** `MainActivity.java` lo registra con `registerPlugin(...)`
antes de crear el Bridge.

Métodos que expone a JavaScript:

| Método | Qué hace |
|---|---|
| `isAvailable()` | Inicializa yt-dlp si hace falta. Devuelve `{ available, version, versionName }`. |
| `downloadAudio({ id, url, format, title })` | Descarga UNA canción. `format` es `"mp3"` o `"m4a"`. Devuelve `{ id, filePath, fileName, format, size }`. |
| `cancelDownload({ id })` | Detiene la descarga en curso con ese id. |
| `updateYtDlp({ channel })` | Actualiza yt-dlp en runtime desde GitHub (`"stable"` por defecto, o `"nightly"` / `"master"`). Devuelve `{ status: "DONE" \| "ALREADY_UP_TO_DATE", version }`. |
| `getVersion()` | Versión de yt-dlp empaquetada. |

**Progreso real:** el plugin emite el evento `downloadProgress`
(`{ id, progress 0-100, eta, status }`) y el frontend lo muestra en el
apartado **Descargas**, canción por canción. La playlist se descarga
canción por canción (el frontend llama a `downloadAudio` una vez por
canción, en orden) y Descargas muestra también el total de la lista.

**Cómo descarga cada canción (yt-dlp):**
`-f bestaudio/best --extract-audio --audio-format <mp3|m4a>
--audio-quality 0 --no-playlist`. Calidad máxima disponible.

- **MP3:** re-codifica a MP3 VBR de máxima calidad. Compatible con todo.
- **M4A:** extrae el audio AAC a .m4a. Mejor calidad/tamaño cuando la
  fuente ya es AAC; si la fuente es Opus, se re-codifica (pierde un poco).

## Dónde se guardan las canciones

**Música/Gurru Music** del teléfono (carpeta pública de música):

- Android 10+: con MediaStore, sin permiso extra. El archivo queda
  visible en el explorador de archivos y en otros reproductores.
- Android 9 o menos: carpeta pública directa (permiso
  `WRITE_EXTERNAL_STORAGE`); si no hay permiso, queda en la carpeta
  privada de la app y sigue sonando dentro de Gurru Music.
- Nombre: `Artista - Título.mp3` (o `.m4a`), con caracteres seguros.
- Al terminar, la canción se vuelve pista **local** en Biblioteca: suena
  con el reproductor normal y entra en la cola, ya no depende de YouTube.

## Qué cambió en el reproductor web (`www/index.html`)

- El botón **Descargar** ahora usa el plugin nativo cuando estamos en
  el APK, con selector de formato **MP3 / M4A** en el diálogo
  (se recuerda tu última elección).
- El apartado **Descargas** sigue igual: porcentaje real por canción,
  total de la playlist, reintento, y botón **Actualizar yt-dlp**.
- **En la versión web (sin APK)** no hay plugin: al tocar Descargar se
  muestra un mensaje claro de que la descarga completa solo funciona en
  el APK, y la canción queda en solo-audio por internet. El streaming
  solo-audio sigue funcionando igual.
- Se quitó el código viejo de cobalt/Piped de esta copia del proyecto
  (ya no se usa: la descarga es nativa).

## Tamaño estimado del APK

Los AAR descargados miden: librería yt-dlp ~46 MB y ffmpeg ~71 MB
(repartidos entre 4 ABIs). Estimado:

- **Por ABI (por ejemplo, solo arm64-v8a): ~50 MB** de APK.
- **APK universal (las 4 ABIs juntas, como se compila ahora): ~120 MB.**

No se activó la división por ABI en este paso porque el workflow de
GitHub Actions espera un solo archivo `app-debug.apk`. Si luego se
quiere un APK más chico por ABI, hay que activar `splits` en
`build.gradle` **y** actualizar la ruta de subida del workflow.

## Cómo probar en el OPPO Reno10 5G

1. Compila el APK con GitHub Actions (abajo) e instálalo. La primera
   vez, permite instalar desde fuentes desconocidas.
2. Abre Gurru Music. La primera descarga tarda unos segundos extra:
   es yt-dlp extrayéndose del APK.
3. Ve a **Biblioteca → Agregar lista por link**, pega un link de
   YouTube (video o playlist) y toca **Agregar**.
4. En el diálogo elige **Descargar** y el formato **MP3** o **M4A**.
5. Se abre **Descargas**: ahí ves el porcentaje real de cada canción
   y el total de la lista. Al terminar, la canción aparece en
   Biblioteca como local y suena sin internet.
6. Comprueba en el explorador de archivos: `Música/Gurru Music/`.
7. Si una descarga falla con error de YouTube, en Descargas toca
   **Actualizar yt-dlp** y reintenta.

## Cómo compilar el APK (desde el celular, GitHub Actions)

El workflow ya existe en `.github/workflows/build-apk.yml`. No se
cambió en este paso.

1. Descarga el ZIP nuevo de este proyecto y súbelo a un repo **privado**
   de GitHub (incluye la carpeta oculta `.github`).
2. En GitHub: **Actions → Build APK → Run workflow**. Espera unos
   minutos (la primera vez descarga Gradle y los .so de yt-dlp/ffmpeg).
3. Descarga el artefacto `gurru-music-apk` → `app-debug.apk` e
   instálalo en el OPPO Reno10 5G.

## Limitaciones honestas

- **YouTube rompe los extractores cada meses.** Por eso existe el
  botón **Actualizar yt-dlp**: actualiza yt-dlp en runtime desde
  GitHub sin reinstalar la app. Si un video falla, actualiza y reintenta.
- **No todos los videos van a funcionar siempre.** yt-dlp 2025.11.12
  deprecó su intérprete de JavaScript para YouTube y algunos videos
  pueden dar error 403 o `missing-format`. No se promete que todo video
  funcione siempre.
- **No es para Google Play.** Play Store prohíbe las apps
  descargadoras y la actualización de binarios en runtime. Esta app se
  distribuye por sideload / GitHub Releases.
- **Uso:** solo contenido propio, con permiso del autor, o con
  licencia Creative Commons. Descargar música con derechos de autor
  de YouTube va contra sus términos.
- **La app debe estar abierta** mientras descarga. Si se cierra, la
  descarga en curso se detiene (no hay servicio en segundo plano en
  esta versión).
- La primera inicialización de yt-dlp necesita internet y unos segundos.

## Qué quedó verificado y qué NO se pudo probar

Verificado en este entorno:

- Las librerías existen en Maven Central: `library 0.17.1` (46 MB) y
  `ffmpeg 0.17.1` (71 MB), con .so para las 4 ABIs. Sus clases y
  métodos se comprobaron contra los AAR reales (`YoutubeDL.init`,
  `execute` con progreso, `updateYoutubeDL`, `destroyProcessById`,
  `FFmpeg.init`).
- La API de Capacitor 7 se comprobó contra el código fuente local
  (`Plugin`, `PluginCall`, `BridgeActivity.registerPlugin`,
  `notifyListeners`, `Capacitor.registerPlugin` en JS).
- **El plugin Java compila limpio** (`javac` contra android.jar API 35
  + AARs reales + kotlin-stdlib). En la revisión se encontró y se
  corrigió un detalle real: `UpdateChannel` es una clase sellada de
  Kotlin, así que en Java se usa `STABLE.INSTANCE` (no `STABLE`).
- Sintaxis del JavaScript de `www/index.html` verificada con
  `node --check`.
- `npx cap sync` terminó bien y `www/index.html` coincide con
  `android/app/src/main/assets/public/index.html` (mismo md5).

NO se pudo probar aquí (hay que probarlo con el APK real):

- **La compilación completa del APK** (Gradle no puede hablar con su
  daemon en este sandbox: bloqueo conocido de TCP en Java). Se compila
  vía GitHub Actions.
- **Una descarga real de YouTube** con yt-dlp dentro del APK.
  Tampoco se pudo probar el primer arranque/extraction en un teléfono.

## Plan B (no implementado): Chaquopy

Existe la alternativa Chaquopy (como AquaSeal: app Capacitor 7 con
Python 3.12 + yt-dlp + FFmpeg empaquetados, APK ~47 MB, con un plugin
que ejecuta scripts Python). No se usó porque **no tiene ruta de
actualización de yt-dlp en runtime**, y esa actualización es justo lo
que mantiene vivas las descargas cuando YouTube cambia cosas. Si
youtubedl-android llegara a no ser viable en este proyecto, Chaquopy
sería el plan B, pero no se implementan ambos.
