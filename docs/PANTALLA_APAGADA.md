# Pantalla apagada, controles Bluetooth y más referencias · 0.7.2

## Resultado · 24 de noviembre de 2026

Se probó **el APK Benchmark 0.7.2 aprobado**, instalado en el Moto g84 5G con EJ-A6101, sin reinstalar ni modificar la aplicación. Su SHA-256 coincide con el APK guardado: `9ECE3B242E4AA557B7781F0F6D44A765DCB4F5BE890F959ED9C86EE905695FA4`. Todos los archivos de producción bajo `app/src/main` conservaron sus hashes. **No se cambió el sonido ni se creó otra versión.**

El usuario confirmó la prueba de los controles físicos y la escucha: **«Todo responde y suena estable»**. No señaló una canción que necesitara otro equilibrio. Se conserva Recomendado 0.7.2 como base aprobada.

## Referencias reales y niveles

Se preparó una cola de siete archivos locales, con sus metadatos reales. La medición de cada canción completa terminó antes de la comparación, evitando valorar una pista nueva con el nivel provisional de su primera escucha. No se creó ni modificó una playlist almacenada.

| Referencia | Artista | Duración | Ganancia guardada |
|---|---|---:|---:|
| MONTAGEM NO VENTO (Slowed) | TRXVELER, TESSAI | 1:46 | −9,31 dB |
| MONTAGEM GENETICA | KPHK, SCHWVFTY | 1:14 | −9,77 dB |
| SEM DIREÇÃO | DJ FKU, NAOMI, Skorde | 1:42 | −9,98 dB |
| MONTAGEM CONTIGO | Repsaj | 1:37 | −8,13 dB |
| DANÇAR NO SALÃO (SLOWED) | DJ FKU, CHAXZER | 1:49 | −6,30 dB |
| PPPP (feat. Hatsune Miku, Kasane Teto) | TAK, Hatsune Miku, Kasane Teto | 2:35 | −6,96 dB |
| A Thousand Years | John Michael Howell, JVKE, ZVC | 3:00 | −4,15 dB |

Las duraciones de la tabla están redondeadas; el JSON conserva los milisegundos. El nivel integrado **previsto** tras aplicar esas ganancias es aproximadamente −16 LUFS en las siete. Estos datos proceden de la medición del balance antes de nivelar/limitar y del cálculo de ganancia; no son una nueva captura acústica ni una segunda medición completa de la salida PCM de cada referencia.

## Pantalla y reproducción

La cola se preparó mediante instrumentación firmada compatible con el propio Benchmark no depurable. La prueba de reproducción se realizó después desde la interfaz normal y con comandos de sistema, sin instrumentación ejecutándose en la app.

Se inició Music y se apagó la pantalla con la tecla de reposo. Android informó **Wakefulness=Dozing**; se verificó además en DisplayManager que el **panel integrado estaba OFF y committedState OFF**. Se suprimió temporalmente la pantalla ambiental con un token de prueba para comprobar el panel negro; el token se retiró automáticamente y se verificó que no quedaban supresiones. No se alteró la preferencia de pantalla ambiental del usuario.

La ventana de observación automática fue de **9 min 51 s**, con muestras discretas, pausas programadas y repetición de fragmentos de prueba. No representa una grabación continua de la salida ni nueve minutos de cada canción. Incluyó:

- Avance de reproducción con pantalla no interactiva.
- Pausa que mantiene la posición y reanudación.
- Siguiente y anterior mediante comandos multimedia Android.
- Transición natural de GENETICA a SEM DIREÇÃO y observación de la transición entre PPPP y A Thousand Years.
- Fragmentos de las siete referencias, incluyendo voces melódicas y material más denso.
- AudioTrack de Music iniciado por **EJ-A6101/Bluetooth multimedia** al principio y al final.
- Volumen del sistema **9 → 9** durante la fase automática: se conservó el valor que tenía el usuario.

El lector inicial comparaba posiciones crudas de PlaybackState, que Android publica periódicamente, y produjo un falso aviso en PPPP. Se corrigió la proyección con `updated`, velocidad y tiempo transcurrido del dispositivo, conservando también la posición cruda para comprobar que avanzaba. Se repitieron los fragmentos vocales desde el principio; no se cambió la aplicación para resolver ese aviso. Los registros originales y los corregidos se conservan.

## Controles físicos

Después de la fase automática se dejaron de inyectar comandos. Se pidió al usuario probar, con la pantalla en reposo, **pausa/reproducción, siguiente/anterior y volumen desde los EJ-A6101**, y terminar con música reproduciéndose. Su respuesta fue «Todo responde y suena estable».

El monitor pasivo tomó muestras durante unos 90 segundos y registró reproducción en NO VENTO, volumen 9 y pantalla no interactiva. No pretende haber capturado cada pulsación breve o cada cambio de volumen; **la confirmación de los gestos físicos procede del usuario**, separada de los comandos Android de la fase automática.

## Alcance y archivos

El teléfono estaba cargando, conectado a ADB y con la configuración previa de mantenerse despierto al cargar. Se verificó el panel apagado, pero **no se forzó Doze profundo ni se midió autonomía**, consumo térmico o estabilidad durante horas sin cable. Los muestreos y el avance de posición no sustituyen una captura de la salida Bluetooth; la ausencia de cortes/chasquidos percibidos se apoya en la confirmación auditiva del usuario. No se utilizó emulador.

Solo se añadieron preparación de referencias en `ScreenOffReferenceChecks.kt`, su entrada de instrumentación y documentación. Debug, assembleBenchmark, instrumentación, 28 tests JVM y Lint (0 errores, 11 advertencias) completados; el APK Benchmark y la instalación conservaron la identidad binaria aprobada.

Evidencia en `outputs/verificacion/pantalla-apagada`: referencias, preparación, observaciones NDJSON, resumen automático, rutas Bluetooth, panel interno, restauración de pantalla ambiental, monitor pasivo, confirmación del usuario y hashes. Las comprobaciones automáticas y la valoración auditiva se registran por separado.
