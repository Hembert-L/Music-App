# Recomendado · Claridad e inicio estable · Music 0.7.1

> Base histórica de claridad e inicio estable. El ajuste actual 0.7.2 y la escucha aprobada con EJ-A6101 están en [AUDIO_BLUETOOTH.md](AUDIO_BLUETOOTH.md). La política de nivel fijo descrita aquí se conserva.

El usuario señaló voz opacada, graves borrosos y un inicio fuerte seguido de un bajón. Esta revisión modifica únicamente Recomendado y su selección de nivel. Original y Personalizado conservan sus rutas y preferencias; no se rediseñan biblioteca, playlists, Mini Player o Reproduciendo.

## Balance de voz y grave

- Paso alto de segundo orden a **28 Hz**, Q 0,707, para reducir energía subsónica.
- Estante grave de **+1,2 dB a 70 Hz**, pendiente S=1. Sustituye el refuerzo nominal anterior de +3,5 dB mediante paso bajo de 95 Hz.
- Campana de **−1,3 dB a 240 Hz**, Q 0,85, para reducir acumulación de medios bajos.
- Campana de presencia de **+0,8 dB a 2,4 kHz**, Q 0,75. Afecta a la mezcla completa: no separa ni reconstruye la voz.
- Se retira el realce adicional de brillo anterior.
- Apertura lateral **12 %** por encima de una transición de **700 Hz**. Se eliminan completamente las copias retardadas a 9 y 13 ms; no hay buffer de sala ni ecos añadidos. El grave apenas se ensancha y la señal mono conserva su centro.

Los cuatro biquads utilizan estados independientes por canal y precisión doble interna. Se calculan al configurar el stream, sin asignaciones por frame. Fórmulas de referencia: [Audio EQ Cookbook, W3C/RBJ](https://www.w3.org/TR/audio-eq-cookbook/). La respuesta se verifica con señales conocidas; no se presenta como una medición acústica del teléfono.

En los tests a 48 kHz y señal baja, **60 Hz/1 kHz = +0,32 dB** y **2,4 kHz/240 Hz = +2,06 dB**. Son diferencias de respuesta del algoritmo: menos predominio del grave y una relación mayor de presencia frente a medios bajos. La percepción de voz y bajo se valida después mediante escucha.

Se mantienen preganancia de −3 dB, entrada suave de 15 ms y anticipación de 5 ms. El limitador enlazado conserva el techo de muestra de −1,5 dBFS; su recuperación pasa de 180 a 100 ms. No es true-peak ni corrige distorsión ya contenida en el archivo o añadida por el dispositivo de salida.

## Corrección del bajón inicial

En 0.7.0 el nivel comenzaba con −3 dB adicionales provisionales. Cuando terminaba el análisis completo se sustituía por la ganancia calculada y se interpolaba durante segundos. Esa sustitución era la causa del cambio tardío denunciado por el usuario.

Ahora se elige una ganancia **al iniciar cada reproducción**:

1. Si existe una medida válida del archivo para este balance, se selecciona directamente su nivel calculado.
2. Si todavía no existe, se utiliza **−9 dB adicionales fijos** durante esa reproducción. Es un valor prudente, no una afirmación de que una canción desconocida ya está normalizada a −16 LUFS.
3. El trabajador mide y guarda el resultado para una **reproducción futura**. No escribe la ganancia del procesador que está sonando. Pausa, reanudación, seek y navegación no sustituyen el nivel seleccionado.

Así se conserva el inicio inmediato sin esperar a decodificar toda la canción, evitando la sustitución de nivel a mitad de la escucha. La primera reproducción y una futura reproducción ya calibrada pueden tener distinto nivel. La dinámica propia del archivo y la protección de picos siguen existiendo; no se promete una amplitud constante dentro de la música.

Los cambios de ganancia entre pistas tienen un suavizado de solo **5 ms** para evitar discontinuidades, sustituyendo el barrido de 1,2 s. Al crear el procesador, la ganancia elegida se utiliza desde su inicialización. Se elimina el restablecimiento incondicional al nivel provisional en cada transición. La frecuencia de lectura del formato del decodificador también se reduce: solo al inicio o cuando cambia, en lugar de consultarlo por cada bloque.

## Medición y caché

El objetivo de las pistas ya medidas sigue siendo −16 LUFS, con ganancia limitada a −18/+6 dB y al margen de pico de muestra de −2 dBFS. La ganancia precede al limitador. Se conserva medición integrada de canción completa con ponderación K y puertas de silencio, caché atómica limitada a 384 registros, un trabajador de fondo y cancelación al cambiar de pista/modo/cerrar.

La clave pasa a **clear-v1**, invalidando las medidas del balance anterior. Incluye URI, tamaño y fecha de modificación. La selección consulta una sola fila de metadatos y el pequeño índice privado; no vuelve a escanear la biblioteca ni bloquea esperando el análisis completo. Las escrituras de archivo ocurren fuera del bloqueo del índice. No se cambia el volumen de Android ni se editan archivos musicales o playlists.

El modo procesado conserva PCM de 16 bits y la frecuencia del stream. No se añaden dependencias, permisos, micrófono, funciones de streaming ni negociación Bluetooth propia. Las referencias siguen siendo los archivos locales que coinciden por metadatos/duración con POUCO, HIKARI y NO VENTO de la [playlist Phonk del usuario](https://open.spotify.com/playlist/0RjvXGqxw43wMyvIKioT8w).

## Verificación

Los resultados físicos y el estado instalado se registran en [VERIFICACION.md](VERIFICACION.md). La prueba específica de inicio usa una URI temporal del **mismo archivo real**, comprobada con MediaStore, para crear un caso sin caché sin borrar los datos del usuario. Comprueba que la ganancia seleccionada y la ganancia enviada al DSP no cambian cuando finaliza el análisis; después vuelve a iniciar el archivo y verifica que utiliza la medida guardada.

Los tests incluyen respuesta de bandas, ausencia de reflexiones retardadas, centro/suma mono, separación multicanal, señales patológicas, picos, cambios de ganancia y drenaje. El modo Original mantiene ausencia de DSP propio. La aprobación auditiva del nuevo timbre queda diferenciada de estas comprobaciones.

## Archivos principales

`RecommendedEngine.kt`, nuevo `ToneBiquad.kt`, `TrackLoudness.kt`, `RecommendedProcessor.kt`, `LoudnessAnalysis.kt` y `PlaybackService.kt`; texto de Recomendado en `MainActivity.kt`; versión en `app/build.gradle.kts`; tests de DSP y `AudioQualityDeviceChecks.kt`. No se modifica el diseño de las otras pantallas.
