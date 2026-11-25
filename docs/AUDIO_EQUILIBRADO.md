# Recomendado · Music 0.7.0

> Historial de 0.7.0. El usuario detectó voz opacada, grave borroso y un bajón causado por aplicar el análisis durante la escucha. La corrección actual está en [AUDIO_CLARIDAD.md](AUDIO_CLARIDAD.md); los parámetros y el comportamiento descritos aquí corresponden a la versión anterior.

Esta revisión mejora el mismo perfil Recomendado. Busca graves suaves y presentes, voces claras, amplitud estéreo y menos diferencias de nivel entre canciones. Original sigue sin DSP propio y Personalizado conserva sus controles y preferencias. La biblioteca, las playlists, el Mini Player y Reproduciendo mantienen su diseño.

## Balance e inmersión

El refuerzo grave nominal baja de +4,5 a +3,5 dB y su transición pasa de 120 a 95 Hz. Un paso alto suave a 18 Hz reduce energía subsónica; el realce efectivo medido a 40 Hz frente a 1 kHz es aproximadamente +2,23 dB a señal baja. El brillo nominal pasa de +1 a +0,6 dB para evitar añadir aspereza a grabaciones ya intensas.

Se mantiene la apertura lateral del 30 % por encima de la transición de 180 Hz. Las reflexiones de 9/13 ms tienen pesos +0,055/−0,035, más discretos que antes. El centro y las señales mono no reciben espacialidad artificial. La suma mono de la componente lateral se conserva antes del limitador. No hay reverb larga, realimentación ni generación de canales envolventes. En altavoces de teléfono no puede evaluarse la inmersión de unos auriculares.

Preganancia de −3 dB, entrada gradual de 15 ms y limitador enlazado con anticipación de 5 ms/ataque de 0,3 ms. La recuperación pasa a 180 ms. El techo de muestra sigue en −1,5 dBFS. El drenaje incluye 120 ms adicionales para dejar decaer el nuevo filtro subsónico.

## Consistencia de nivel

Se decodifica **la canción completa** en segundo plano, sin reproducir una segunda salida audible. El análisis mide la salida del balance Recomendado **antes del limitador**, con ponderación K y bloques de 400 ms solapados al 75 %, puerta absoluta −70 LUFS y relativa −10 LU. La ganancia se aplica antes del limitador en la reproducción, para evitar limitar innecesariamente una pista que ya se va a atenuar. Es una implementación propia para mono/estéreo, contrastada con señales conocidas; no un medidor certificado.

El objetivo elegido para Music es **−16 LUFS**. No se atribuye este valor a Spotify ni se usa audio obtenido de Spotify. Se calcula una ganancia fija por pista: objetivo menos nivel medido, limitada a −18/+6 dB y al margen que permite un pico de muestra de −2 dBFS. Las canciones con mucha dinámica pueden quedar por debajo del objetivo para conservar sus picos. No se aplastan las secciones suaves ni se persigue el bombo con un control automático de volumen.

La ganancia entra suavemente, con una constante de 1,2 s, cuando llega un análisis o cambia la pista. La primera escucha usa una atenuación provisional de −3 dB adicionales hasta obtener la medida; no se espera al análisis para iniciar la canción. Las siguientes escuchas reutilizan la medida guardada. No se garantiza identidad de volumen desde la primera muestra de una transición, especialmente al escuchar por primera vez un archivo.

La última protección usa una ganancia común para todos los canales. Incluso ante datos inesperados o durante la transición, no permite superar el techo de muestra de −1,5 dBFS. El margen de −2 dB de la medida permite normalmente conservar la forma de onda sin que esa protección intervenga. No es un limitador true-peak y no mide la salida analógica, los efectos del fabricante ni la recodificación Bluetooth.

## Rendimiento y almacenamiento

Un solo trabajador de prioridad de fondo analiza la pista actual y, después, la siguiente de la cola. Se cancela al cambiar de canción, salir de Recomendado, cerrar la sesión o destruir el servicio. Se limita cada análisis a 60 s de trabajo; formatos no admitidos, permisos revocados o errores conservan la reproducción con el nivel provisional seguro.

Se guardan hasta 384 registros pequeños en `files/track-levels-v1.json`, con escritura atómica. La clave incluye versión del balance, URI, tamaño y fecha de modificación. Cambiar el archivo o el perfil invalida la medida. No se almacenan copias de audio, no se editan etiquetas ni archivos musicales y no se escanea toda la biblioteca para esta función. El análisis completo inicial implica CPU y lectura adicionales; no se declara una prueba prolongada de batería.

Se mantiene Media3/ExoPlayer y su ruta procesada PCM de 16 bits, cálculo flotante interno y frecuencia original del stream. No se agregan dependencias, permisos, servicios de red ni captura de micrófono. No se recupera información ausente en los archivos ni se fuerza un códec Bluetooth. El volumen de Android permanece bajo control del usuario.

## Referencias y validación

Base de medición: [ITU-R BS.1770-5](https://www.itu.int/rec/R-REC-BS.1770-5-202311-I). Decodificación local: [MediaExtractor](https://developer.android.com/reference/android/media/MediaExtractor) y [MediaCodec](https://developer.android.com/reference/android/media/MediaCodec).

El usuario proporcionó su [playlist Phonk](https://open.spotify.com/playlist/0RjvXGqxw43wMyvIKioT8w). La vista pública insertada expuso 31 títulos. Se eligieron tres coincidencias por título, artista y duración: **MONTAGEM POUCO (Slowed)** de Repsaj (79,754 s local/79,741 s Spotify), **MONTAGEM HIKARI** de BellyJay (90,000/89,985 s) y **MONTAGEM NO VENTO (Slowed)** de TRXVELER, TESSAI (105,694/105,682 s).

Se procesan únicamente los archivos locales. No se descargó ni analizó audio de Spotify, ni se afirma que ambos servicios tengan el mismo máster o codificación. Se descartaron como referencia algunas copias con otra duración, como DELÍRIO de 335 s frente a la versión de 125 s mostrada en Spotify. Los metadatos originales del usuario permanecen intactos.

Las medidas internas y la comprobación de reproducción se distinguen de la aprobación auditiva del usuario. Los resultados de esta entrega se registran en [VERIFICACION.md](VERIFICACION.md).

## Archivos

- `playback/RecommendedEngine.kt`: balance más suave, filtrado subsónico y protección existente.
- `playback/TrackLoudness.kt`: medidor, cálculo de ganancia fija y transiciones/protección enlazada.
- `playback/LoudnessAnalysis.kt`: decodificación local, cancelación y caché privada.
- `playback/RecommendedProcessor.kt` y `PlaybackService.kt`: aplicación del nivel y conexión con las transiciones de Media3.
- `MainActivity.kt`: explicación actualizada dentro de Sonido; tres modos conservados.
- `RecommendedEngineTest.kt`, `TrackLoudnessTest.kt`, `AudioQualityDeviceChecks.kt` y `MusicInstrumentation.kt`: comprobaciones numéricas y físicas.
