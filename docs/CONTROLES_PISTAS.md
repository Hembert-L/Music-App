# Anterior y siguiente · Music 0.7.3

## Comportamiento

Anterior pasa a la pista anterior de la cola, aunque la actual lleve más de unos segundos. Anterior y Siguiente comienzan la pista elegida en **0 ms y reproduciendo**, también si la actual estaba pausada. El mismo criterio se aplica en Mini Player, Reproduciendo y la sesión multimedia de Android utilizada por notificaciones y auriculares.

Se conserva el orden real de Media3, incluido el recorrido aleatorio. Repetir una canción mantiene su funcionamiento automático, pero un salto manual permite salir de ella. Repetir toda la cola permite pasar del principio al final y viceversa. Sin repetición, cuando no hay destino, el botón no reinicia ni reanuda la pista actual.

Mover el progreso dentro de una canción conserva la pausa. Abrir Music sigue recuperando la cola pausada. El cierre X conserva su funcionamiento.

## Integración

- `playback/TrackNavigationPlayer.kt`: adaptador de la sesión sobre el ExoPlayer existente. Unifica las cuatro variantes de salto, prepara y reproduce el destino desde cero. `ForwardingSimpleBasePlayer` conserva la coherencia de eventos y comandos disponibles. El umbral de Anterior se publica sin límite de tiempo para evitar que el controlador prediga un reinicio de la pista actual. En los extremos se deshabilita el salto que carece de destino.
- `playback/PlaybackService.kt`: aplica el adaptador al crear la sesión y al sustituir el reproductor por un cambio de modo de sonido. Mantiene el ExoPlayer interno, su configuración, análisis y DSP.
- `MusicViewModel.kt`: Anterior solicita la pista anterior; los cambios reales de pista actualizan el título y su animación.
- `TrackNavigationPlayerTest.kt` y `TrackNavigationDeviceChecks.kt`: pruebas del contrato y de su integración. La instrumentación admite `trackControlsOnly=true`. Se actualizó la prueba existente del Mini Player para no pulsar Reproducir después de un salto que ahora ya inicia audio.

No se añadieron dependencias ni se cambió el diseño. **Los siete archivos previos de audio conservan sus SHA-256**, incluidos Recomendado, Personalizado, análisis y nivelación. El perfil aprobado 0.7.2 permanece intacto en la 0.7.3.

La separación entre reiniciar una pista y saltar a otra está documentada por [Player de Media3](https://developer.android.com/reference/androidx/media3/common/Player). El adaptador sigue el mecanismo de [ForwardingSimpleBasePlayer](https://developer.android.com/reference/androidx/media3/common/ForwardingSimpleBasePlayer), disponible en la versión 1.7.1 ya utilizada por el proyecto.

## Verificación · 24 de noviembre de 2026

Compilación offline de Debug, **assembleBenchmark**, instrumentación, **32 tests JVM sin fallos** y Lint (**0 errores, 11 advertencias**).

En el **Moto g84 5G físico, Android 15**, se instaló **0.7.3-benchmark, código 14**, sin borrar datos. La prueba dedicada pasó con 370 archivos locales y auriculares Bluetooth conectados:

- Mini Player: Anterior desde pausa a 17 s y Siguiente desde pausa.
- Reproduciendo: Anterior reproduciendo desde 17 s y Siguiente desde pausa.
- Las cuatro variantes de salto de MediaController, sin heredar pausa ni posición.
- Aleatorio: siguiente real y regreso al predecesor del mismo recorrido.
- Repetir una: los saltos manuales cambian de pista. Repetir toda: ambos extremos enlazan correctamente.
- Límites sin repetición: conservan pista, posición y pausa. La primera ejecución detectó una discrepancia de posición predictiva del controlador en el extremo inicial; se corrigió la disponibilidad de comandos y se repitió la prueba completa con éxito.
- La búsqueda temporal dentro de una pista conserva la pausa.
- Teclas multimedia Android Anterior y Siguiente desde pausa, también después de apagar la pantalla mediante Android.
- Configuración de sonido conservada y volumen multimedia **9 → 9**. Se restauró la cola previa al terminar la prueba.

Los 18 saltos registrados terminaron reproduciendo el destino, con posiciones observadas de **181–282 ms** tras esperar aproximadamente 220 ms; esto comprueba el inicio desde cero con el avance normal de audio. La instrumentación finalizó con código **−1** (éxito).

La comprobación posterior a la instrumentación relacionó el AudioTrack iniciado de Music con **EJ-A6101, BT A2DP**, y confirmó versión y volumen. Los eventos multimedia automáticos fueron **inyectados por Android**. Después el usuario confirmó la prueba con los botones físicos: **«Anterior cambia de canción y tanto anterior como siguiente empiezan a sonar desde cero aunque la canción estuviera pausada»**. Los dos casos quedan aprobados en 0.7.3. No se realizó un nuevo ajuste ni una nueva evaluación acústica del sonido.

Evidencia: `outputs/verificacion/saltos-pista`. No se utilizó emulador ni se modificó NAS Reels.
