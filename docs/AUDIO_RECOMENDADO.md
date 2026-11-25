# Audio recomendado y auriculares · Music 0.3.0

> Historial de la primera implementación. El perfil actual 0.7.2 y su aprobación con EJ-A6101 están en [AUDIO_BLUETOOTH.md](AUDIO_BLUETOOTH.md). La base de claridad/inicio estable está en [AUDIO_CLARIDAD.md](AUDIO_CLARIDAD.md), y la etapa 0.7.0 en [AUDIO_EQUILIBRADO.md](AUDIO_EQUILIBRADO.md). Los parámetros y resultados de abajo corresponden a 0.3.0.

## Uso y alcance

Abrir el menú superior → Sonido → **Recomendado**. Es una opción de una pulsación con balance fijo, independiente de los controles de Personalizado. Original sigue siendo el valor inicial en instalaciones nuevas y no contiene procesadores de Music. Elegir un perfil no eleva el volumen de Android ni cambia automáticamente de códec o dispositivo de salida. Los ajustes manuales anteriores permanecen guardados al alternar de modo.

Recomendado busca un balance con graves presentes, voces centradas y un espacio estéreo amplio; es un punto de partida, no una afirmación de ser el mejor ajuste para todo auricular o canción. Tras escuchar la primera versión, el usuario confirmó los controles físicos y pidió más espacio; se elevó la apertura sin aumentar graves ni volumen del sistema. La percepción depende del ajuste físico de los auriculares y de su respuesta. No se prometen más aislamiento, ANC, Atmos, seguimiento de cabeza ni reconstrucción de información ausente.

## Procesamiento

- Graves: refuerzo suave de +4,5 dB nominales mediante una banda de paso bajo de 120 Hz.
- Brillo: +1 dB nominal mediante complemento de paso bajo de 4,5 kHz.
- Preganancia fija: −3 dB y entrada gradual de unos 15 ms.
- Espacio solo en estéreo: refuerzo del 30 % de la componente lateral por encima de una transición de 180 Hz; reflexiones laterales a 9 y 13 ms, con pesos +0,075 y −0,045. Sin realimentación ni reverb larga. La componente central se conserva antes del limitador; las señales mono no se ensanchan artificialmente. Para otros números de canales se omite la espacialidad.
- Limitador enlazado: misma ganancia para todos los canales, anticipación de 5 ms, ataque de 0,3 ms y recuperación de 120 ms. Techo de muestra de −1,5 dBFS. Una guarda común cubre el redondeo del ataque, sin recortar L/R de forma independiente. No se implementa normalización de sonoridad ni un amplificador de volumen.
- PCM de 16 bits, frecuencia y número de canales del stream. Cálculo flotante interno, sin asignaciones de memoria por frame. Se drenan la anticipación y las reflexiones al final del stream para no truncar el último sonido. El modo no es procesamiento de alta resolución ni salida bit-perfect; tampoco es un limitador true-peak.

La ruta procesada fuerza decodificación PCM y desactiva offload para impedir que se evite el DSP. Usa `USAGE_MEDIA`/`CONTENT_TYPE_MUSIC`, audio focus gestionado por ExoPlayer y pausa al desconectar auriculares (`audio becoming noisy`). No abre micrófono, no activa modo de llamada ni fuerza SCO. Android negocia A2DP o LE Audio según la salida disponible; Music no fuerza parámetros de radio, códec o tasa de enlace. No se puede eliminar interferencia o cortes de conexión solo con ecualización.

El cambio entre Original, Recomendado y Personalizado reconstruye el reproductor conservando la cola, posición, repetición y opción de aleatorio. Puede introducir una pausa breve. Las actualizaciones de tono dentro de Personalizado continúan en vivo. El limitador no corrige distorsión ya contenida en el archivo ni protege de todo pico generado posteriormente por el códec, el DAC o efectos del fabricante.

## Botones de auriculares

Se utiliza la MediaSession ya existente para play/pausa, anterior y siguiente, incluidos eventos Android de auriculares Bluetooth y cableados. Se añade MediaButtonReceiver y callback de reanudación desde la cola guardada cuando el proceso haya sido reclamado. Abrir la app sigue recuperando la cola pausada: no arranca música por sí sola. Forzar la detención desde Ajustes Android puede impedir la recepción hasta volver a abrir Music.

La Activity selecciona `STREAM_MUSIC` para las teclas de volumen. Los comandos de volumen Bluetooth/AVRCP los resuelve Android; no se interceptan ni se cambia el volumen global desde el DSP. Los gestos (doble toque, mantener pulsado, sensor de retirada) dependen del firmware y de si el accesorio envía un evento estándar. No se asigna un gesto nuevo al auricular desde la app.

## Verificación numérica

16 tests unitarios aprobados: los 10 anteriores más seis de Recomendado. El conjunto nuevo comprueba señales a escala completa (DC, cuadrada, ruido, seno grave, contrafase e impulsos), valores no finitos, 8/44,1/48/96 kHz y 1/2/6 canales; centro mono, suma mono de la componente lateral, reflexiones cortas, balance de graves, drenaje final y conservación de ajustes manuales.

Pico máximo observado: 0,8413952 frente a un techo calculado de 0,84139514 (diferencia de redondeo flotante). No superó el margen de tolerancia de 0,000002. A señal baja y en régimen, el seno de 40 Hz tuvo aproximadamente +4,03 dB respecto al de 1 kHz. Son mediciones internas del algoritmo, no de la salida analógica ni del auricular.

Compilación offline Debug y Benchmark, instrumentación y Lint finalizadas. Lint: 0 errores, 11 advertencias. No se añadieron bibliotecas ni permisos Bluetooth/micrófono. Benchmark es una variante local no depurable, firmada con la clave de desarrollo de Aiko; no significa haber realizado un benchmark de consumo.

## Verificación física y límites

Solo Android físico: Moto g84 5G, Android 15; auriculares EJ-A6101 conectados. En el runner se verificó que Recomendado procesa PCM, conserva el modo en preferencias y mantiene la posición avanzando durante ocho segundos sin error del reproductor. También pasaron los comandos Android play, pause, next y previous, retorno a Original y reproducción en segundo plano, junto con las pruebas anteriores de biblioteca, playlists y audio (13 grupos PASS).

La reproducción iniciada desde la UI quedó asociada al dispositivo de salida ID 14371, que AudioPolicy identifica como «BT A2DP Out», EJ-A6101; AudioPlaybackConfiguration informó AudioTrack iniciado con `USAGE_MEDIA`, dos canales y 48 kHz. Esto acredita la ruta real de reproducción Bluetooth observada, no una prueba de todas las salidas o códecs. Los eventos inyectados desde Android comprueban la integración de la app; el tacto físico de cada auricular y la impresión auditiva se verifican por separado con el usuario.

El usuario confirmó que los controles físicos respondían y solicitó más espacio y amplitud. Se amplió el perfil como se indica arriba; no se atribuye a esa respuesta una aprobación auditiva de la versión ampliada aún no escuchada en ese momento. La prueba de teclas de volumen en la Activity verificó STREAM_MUSIC 8 → 7 → 8 (rango 0–15), restaurando el nivel previo. Se terminó de forma controlada únicamente el proceso de Music después de pausar/guardar y enviarlo a Inicio, sin forzar detención: un evento multimedia Play reanudó la cola sin abrir la Activity. El receptor quedó registrado como último receptor de botones multimedia de Music.

Pendientes: captura de salida, respuesta real del auricular, THD+N, picos entre muestras y tras codificación Bluetooth, consumo térmico/batería durante horas, pruebas con llamadas/reconexión y múltiples modelos. No se declara una prueba prolongada sin cortes ni calidad subjetiva validada por el asistente.

## Archivos y referencias

Nuevos RecommendedEngine y RecommendedProcessor; SoundSettings añade selección persistente; PlaybackService integra ruta PCM y reanudación; MainActivity incorpora la opción y el canal de volumen; Manifest declara el receptor estándar. No se modifican playlists, biblioteca, NAS Reels ni el diseño del encabezado o minirreproductor.

Referencias primarias: [control con MediaSession](https://developer.android.com/media/media3/session/control-playback), [reanudación y MediaButtonReceiver](https://developer.android.com/media/media3/session/background-playback#resumption), [salidas y volumen de música](https://developer.android.com/media/platform/output).
