# Recomendado · Ajuste fino con EJ-A6101

## Escucha de base · 24 de noviembre de 2026

Se inició NO VENTO en Music 0.7.1 sobre el Moto g84 5G. AudioPlaybackConfiguration identificó el AudioTrack de Music con USAGE_MEDIA/CONTENT_TYPE_MUSIC, dos canales y PCM de 48 kHz, iniciado en el dispositivo 15068. AudioPolicy identificó ese puerto como **BT A2DP Out, EJ-A6101**. El nivel Bluetooth de STREAM_MUSIC encontrado era 8 y no se modificó.

Esto verifica la ruta de reproducción observada, no el códec Bluetooth negociado, una captura de la salida acústica ni un sistema envolvente. El indicador de espacialización Android no describe la apertura estéreo propia de Music.

El usuario escuchó la base y confirmó bajo más equilibrado y voz estable. Pidió todavía más presencia de voz respecto al grave y terminar de limpiar el bajo. No se atribuye a esa respuesta aprobación del siguiente ajuste.

## Ajuste 0.7.2

- Estante a 70 Hz: **+1,2 → +0,4 dB**.
- Recorte de medios bajos a 240 Hz: **−1,3 → −1,5 dB**, mismo Q 0,85.
- Presencia a 2,4 kHz: **+0,8 → +1,4 dB**, mismo Q 0,75.
- Apertura lateral: **12 → 16 %**, concentrada por encima de una transición de **1 kHz** en lugar de 700 Hz. Se conserva el centro y se limita la expansión de graves/medios bajos. No se añaden ecos, reverb ni retardos de sala.

Se mantienen filtro subsónico, preganancia, limitador enlazado, techo de muestra y comportamiento de inicio de 0.7.1. La selección de nivel fijo, su aplicación al DSP y la política de guardar nuevas medidas para otra reproducción conservan su código. Solo cambia la clave de caché a `clear-bt-v1` para medir el balance nuevo.

El ajuste pertenece al mismo perfil global Recomendado; no activa rutas o modos automáticamente al conectar Bluetooth. Original, Personalizado y sus controles se conservan. No se fuerza códec, volumen o salida, ni se añaden permisos/dependencias.

Respuesta observada en tests a 48 kHz: **60 Hz/1 kHz −0,38 dB** y **2,4 kHz/240 Hz +2,85 dB**, frente a +0,32/+2,06 dB de 0.7.1. Son mediciones del procesamiento digital: menos peso grave relativo y más presencia relativa, sin afirmar todavía el resultado auditivo con los auriculares.

## Validación completada

Debug, assembleBenchmark, instrumentación, **28 tests JVM aprobados** y Lint (**0 errores, 11 advertencias**) completados. Se mantuvieron las pruebas de picos, mono/estéreo, canales, ausencia de ecos, transitorios y cambios de ganancia. En el **Moto g84 5G físico, Android 15**, pasaron **13 grupos de instrumentación** (código −1): salida real del AudioTrack a EJ-A6101, inicio sin caché y con medida guardada, reproducción/pausa, seek, anterior/siguiente, aleatorio/repetición, Original/Personalizado/Recomendado, teclas multimedia Android y doce segundos en segundo plano. Las tres referencias quedaron medidas para comparar a nivel calibrado. STREAM_MUSIC permaneció en 8.

El caso sin caché mantuvo **−9 dB antes y después de completar el análisis**, con **0 cambios tardíos de ganancia enviados al DSP**. La ganancia calculada de −6,8977833 dB se guardó para una reproducción futura y se comprobó su uso al volver a entrar en la pista.

| Referencia local | Balance antes de nivelar (LUFS) | Ganancia (dB) | Calibrada (LUFS) | Pico de muestra (dBFS) |
|---|---:|---:|---:|---:|
| POUCO (Slowed) · Repsaj | −9,10 | −6,90 | −16,001 | −3,21 |
| HIKARI · BellyJay | −7,68 | −8,32 | −16,001 | −5,05 |
| NO VENTO (Slowed) · TRXVELER, TESSAI | −6,69 | −9,31 | −16,001 | −5,28 |

Son medidas digitales por decodificación completa de cada archivo, no una captura acústica del auricular. Los picos respetaron el margen previsto. Los tiempos de las dos pasadas de prueba fueron 33,7/32,4/60,6 s. Producción guarda una sola medición y no espera a terminarla para comenzar la primera escucha.

La prueba de teclas inyectadas comprueba la integración Android, no demuestra que se hayan pulsado físicamente los auriculares. La calidad subjetiva requiere la escucha del usuario; no se declara aislamiento, Atmos, respuesta de frecuencia medida del auricular ni prueba prolongada sin cortes.

## Instalación y aprobación auditiva

Se instaló **0.7.2-benchmark, código 13**, sin borrar datos, manteniendo Recomendado y la cola de tres referencias. La comprobación final volvió a identificar el AudioTrack iniciado de Music en el puerto 15068, **BT A2DP Out, EJ-A6101**, con PCM estéreo de 48 kHz y atributos multimedia. Se inició NO VENTO con su nivel guardado para la comparación, conservando el volumen del teléfono en 8. No se usó emulador.

El usuario escuchó la **0.7.2 en los EJ-A6101** y confirmó: «Suena muy bien, quedó excelente y totalmente a mi gusto; no le ajustaría nada por el momento» (acentuación normalizada). Se registra como **base de Recomendado aprobada por el usuario en esta combinación de canción, auriculares y nivel**. No se harán más cambios de sonido en esta fase. No se extrapola esa aprobación a todos los auriculares, grabaciones o volúmenes.

Evidencia: `outputs/verificacion/audio-bluetooth` (informes físicos, rutas, medidas, inicio estable, paquete instalado y aprobación auditiva). Capturas: `outputs/screenshots/audio-bluetooth`. APK actual: `Music-0.7.2-benchmark.apk`. La primera escucha de base de este documento corresponde a 0.7.1; la validación y aprobación de esta sección corresponden a 0.7.2.
