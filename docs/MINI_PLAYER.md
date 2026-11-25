# Mini Player · Music 0.4.0

## Diseño y controles

Mini Player compartido de 64 dp, carátula de 44 dp, título de 14 sp, artista de 11 sp y progreso fino morado/azul bajo la información. Orden horizontal: carátula, información, X, anterior, play/pausa, siguiente. Play/pausa usa un círculo de 36 dp; cada control dispone de un área táctil de 48 × 48 dp. Se reutilizan paleta, portada y gradientes. En pantallas estrechas disminuye el espacio de texto; los cuatro controles permanecen visibles.

Solo la carátula y la información abren la pantalla completa. Los controles son hermanos con acciones independientes, no hijos de una tarjeta entera pulsable. Anterior reutiliza `seekToPrevious`, incluidos el reinicio de la pista si ya avanzó varios segundos y la navegación del orden aleatorio que ofrece Media3.

## Cierre completo

X envía un comando privado al servicio: pausa, stop y clearMediaItems sobre el ExoPlayer existente. Guarda sincrónicamente cola vacía, índice/posición cero y marca de reproducción cerrada; detiene el estado foreground del servicio. No se crea ni reemplaza el motor. Media3 retira la notificación al vaciar la cola. La UI oculta inmediatamente el Mini Player mientras espera la confirmación del estado vacío y libera el espacio del listado.

Restaurar la app o pedir reanudación desde un auricular no recupera una cola cerrada. Una selección explícita de canción o reproducción de una playlist establece una cola nueva y vuelve a mostrar el Mini Player. No se eliminan canciones, favoritos, playlists ni preferencias de sonido. El cierre no es una pausa.

## Título de una sola pasada

El ViewModel identifica las nuevas reproducciones y conserva la instancia de movimiento fuera del composable. Las actualizaciones de progreso, pausa/reanudación y cambios de pantalla no crean otro recorrido. La repetición real, cambio de canción o nueva selección explícita sí pueden crear uno nuevo. Una cola recuperada pausada no anima hasta empezar a reproducir.

Se mide el texto completo y solo se anima si desborda: espera de 1 s, avance suave hasta el final a una velocidad orientativa de 28 dp/s, retención de 650 ms y retorno suave de 600–2400 ms. Tras ello se muestra el título estático con elipsis. No existe un bucle infinito. El tiempo monotónico sigue contando mientras no se ve el Mini Player; al regresar se continúa desde ese punto o se muestra ya estático. La animación anterior se cancela cuando cambia la reproducción.

Se usa Canvas para desplazar únicamente los glifos dentro de un área recortada y fija; artista y controles no se mueven. El texto accesible completo se mantiene estable. Una animación se conserva durante la vida del ViewModel y los cambios de configuración; no se persiste una animación visual a disco tras morir el proceso.

## Archivos

- `ui/MiniPlayer.kt`: componente y texto de una pasada; sustituye el componente anterior de MusicComponents.
- `MiniTitleMotion.kt`: línea de tiempo independiente de Compose y registro de una pasada.
- `MusicViewModel.kt`: integración de cierre, anterior e identificación de reproducción.
- `PlaybackService.kt`: comando privado de cierre y restauración protegida de cola vacía.
- `MainActivity.kt` y `MusicTheme.kt`: conexión de acciones y altura compacta.
- `MiniTitleMotionTest.kt` y `MiniPlayerDeviceChecks.kt`: verificación de la línea de tiempo y de la integración física.

## Validación

Comprobado el **24 de noviembre de 2026** en Aiko:

- `assembleDebug`, **`assembleBenchmark`**, `assembleDebugAndroidTest`, `testDebugUnitTest` y `lintDebug`: **BUILD SUCCESSFUL**.
- **20 tests JVM, 0 fallos y 0 errores**. Los cuatro nuevos verifican recorrido finito, espera/retorno, continuidad al volver a reclamar la misma reproducción, títulos cortos y rechazo de reproducciones obsoletas.
- Android Lint: **0 errores y 12 advertencias**. Se conserva el informe completo; no se ocultan con baseline.
- **18 grupos de integración PASS** en el **Moto g84 5G físico, Android 15**, con **370 canciones reales**. Incluyen reproducción/pausa, anterior/siguiente, orden aleatorio, título largo de una pasada y título corto estático, continuidad al navegar entre pantallas y Canciones/Favoritos/Playlists, cierre con cola persistente vacía, retirada de notificación, ampliación del área del listado y nueva reproducción. Favoritos y archivo de playlists permanecieron iguales antes/después del cierre. También pasaron las regresiones existentes de biblioteca, playlists, servicio y sonido. No se utilizó emulador.
- Comprobación adicional sobre la **APK Benchmark instalada**: carátula abre el reproductor completo; X permanece cerrado tras detener el proceso y reabrir Music; Play remoto no recupera la cola cerrada; seleccionar una canción vuelve a mostrar el Mini Player. Se dejó la canción pausada.
- Inspección visual de la captura del teléfono: carátula a la izquierda, información y progreso, X/anterior/play/siguiente en ese orden, fondo morado oscuro y botón circular degradado; no se superponen controles. Capturas con Mini Player y después del cierre en `outputs/screenshots` de la entrega.

Esta verificación no constituye una auditoría de todas las escalas de fuente, dispositivos, orientación horizontal ni TalkBack. La calidad auditiva y el DSP no se modificaron en esta fase.

Evidencia de entrega: `verificacion/mini-compilacion.log`, `mini-android.log`, `mini-interfaz.log`, los XML de tests JVM y el informe Lint. El proyecto incluye el runner `MusicInstrumentation` y sus comprobaciones `MiniPlayerDeviceChecks` para repetir las pruebas con archivos reales.

La referencia se aplica al Mini Player: se conservan Music, Canciones/Favoritos/Playlists y la barra inferior existentes. No se añaden Carpetas, Álbumes o Artistas ficticios para imitar otras partes de la imagen. El texto se adapta al ancho real y a las áreas táctiles de 48 dp; no se comprime la información a costa de superponer botones.
