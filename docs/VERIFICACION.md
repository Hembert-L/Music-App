# Verificación · Music

## Limpieza para repositorio · 24 de noviembre de 2026

Se retiraron compilaciones, cachés locales y `local.properties`. Se eliminaron los componentes antiguos sin referencias `MusicHeader`, `CategoryPill` y `PlaylistActions`, junto con sus imports exclusivos. El README quedó centrado en el estado actual, se añadió un índice de documentación y una guía de pruebas, y se completaron `.gitignore`, `.gitattributes` y `.editorconfig`.

La comprobación partió de las carpetas de compilación eliminadas, sin `local.properties`, usando el SDK mediante `ANDROID_HOME`: **Debug, assembleBenchmark, instrumentación, 32 tests JVM y Lint (0 errores, 11 advertencias)** completados; las 123 tareas se ejecutaron desde cero. Los archivos generados se retiraron nuevamente al terminar. Wrapper, código, recursos, pruebas y registros técnicos necesarios permanecen en el repositorio.

Los nueve archivos de reproducción/audio y el escáner conservaron sus hashes. No se cambió el comportamiento de la app, no se instaló otro APK en el teléfono y no hubo una nueva prueba física en esta tarea de limpieza. Versión conservada: 0.7.6. Evidencia de compilación e integridad externa en `outputs/verificacion/limpieza-github`.

## Music 0.7.6 · Actualizar deslizando · 24 de noviembre de 2026

Instalada 0.7.6-benchmark, código 17. El gesto descendente desde el inicio de la biblioteca reemplaza el elemento Actualizar biblioteca del menú. Muestra una rueda nativa y conserva el contenido durante la lectura. Integrado en Canciones, Favoritos y Playlists sin cambiar el escáner ni añadir dependencias.

**Debug, assembleBenchmark, instrumentación, 32 tests JVM y Lint (0 errores, 11 advertencias)** completados. En el Moto g84 5G físico, la ejecución final terminó con −1: seis actualizaciones reales, rueda visible/oculta, tirones cortos, desplazamiento intermedio sin actualizar, navegación horizontal, búsqueda, resultados vacíos y acción accesible. Los tirones sobre canciones pausadas no seleccionaron pistas ni iniciaron audio. Se conservó la reproducción durante el resto de la prueba y no apareció la rueda manual al volver de Reproduciendo.

Cola, favoritos, playlists, sonido y orden intactos; volumen **10 → 10**. Los nueve archivos de reproducción/audio y el escáner conservaron hashes. El teléfono tiene las animaciones del sistema desactivadas: se verificó una rueda estática reconocible, sin cambiar sus ajustes. Detalles y límites: [ACTUALIZAR_CON_GESTO.md](ACTUALIZAR_CON_GESTO.md). Evidencia en `outputs/verificacion/actualizar-gesto` y `outputs/screenshots/actualizar-gesto`.

## Music 0.7.5 · Persistencia de Personalizado y cambio rápido · 24 de noviembre de 2026

La prueba con **4/3/−5 dB** confirmó persistencia entre modos y tras cerrar el proceso. Detectó un caso adicional: cambiar de modo inmediatamente después de terminar un ajuste podía reenviar un valor confirmado anterior. Se corrigió en el panel usando el último borrador al cambiar de modo y al restablecer. **Solo cambió SoundSettingsSheet entre los 25 archivos previos de producción; los nueve de reproducción/audio conservan hashes.**

Instalada 0.7.5-benchmark, código 16, sin borrar datos. En el Moto g84 5G físico, cinco fases terminaron con −1 y hubo tres cierres completos con ausencia del proceso y apertura normal. Se verificaron Personalizado → Original/Recomendado → Personalizado, apertura en ambos modos, el cambio rápido corregido y una reapertura final con valores restaurados. Cola conservada, volumen **10 → 10**, valores originales **2/1/0 dB** recuperados y **Recomendado seleccionado**.

Debug, assembleBenchmark, instrumentación, **32 tests JVM** y Lint (**0 errores, 11 advertencias**) completados. No se ajustó el sonido. Detalles y distinción entre el lector de pruebas y el fallo real: [PERSISTENCIA_PERSONALIZADO.md](PERSISTENCIA_PERSONALIZADO.md). Evidencia: `outputs/verificacion/persistencia-personalizado`.

## Music 0.7.4 · Panel de ajustes de sonido · 24 de noviembre de 2026

Rediseño limitado al panel: tres filas con interruptores excluyentes, ayudas flotantes y Personalizado desplegable con transición de 220 ms. Rangos, valores predeterminados, persistencia y cálculo real del margen conservados. Se retiró únicamente la información fija de auriculares de este panel.

**Debug, assembleBenchmark, instrumentación, 32 tests JVM y Lint (0 errores, 11 advertencias)** completados. Instalada 0.7.4-benchmark, código 15, en el Moto g84 5G físico. La ejecución dedicada de interfaz pasó nueve grupos (−1): modos, ayudas, expansión/contracción, deslizadores, margen, restablecimiento, persistencia, cola/volumen y contenido en 320 × 400 dp con fuente al 130 %. Se usó el teléfono físico para el caso restringido; no fue un emulador ni otro dispositivo.

De 24 archivos previos de producción, solo cambiaron MainActivity y MusicTheme; **los otros 22, incluidos los nueve de reproducción/audio, conservan hashes**. Sin cambios DSP ni de controles. Volumen **10 → 10**. Se dejó Recomendado activo tras la comprobación final del panel. Tu confirmación de los botones físicos de 0.7.3 quedó registrada por separado.

Capturas reales observadas y límites documentados en [AJUSTES_SONIDO.md](AJUSTES_SONIDO.md). Evidencia: `outputs/verificacion/ajustes-sonido` y `outputs/screenshots/ajustes-sonido`.

## Music 0.7.3 · Anterior/siguiente desde pausa · 24 de noviembre de 2026

Instalada **0.7.3-benchmark, código 14**, en el Moto g84 5G físico sin borrar datos. Anterior pasa directamente al predecesor; ambos saltos inician el destino desde cero y reproduciendo, incluso desde pausa. Se unificó la política en la sesión Media3 para Mini Player, Reproduciendo y controles multimedia Android. Aleatorio, repetición y límites conservan el orden de la cola. El progreso dentro de una pista sigue respetando la pausa.

**Debug, assembleBenchmark, instrumentación, 32 tests JVM y Lint (0 errores, 11 advertencias)** completados. La prueba dedicada pasó con 370 canciones reales: 18 saltos registrados más extremos y búsqueda temporal. Incluye Anterior tras 17 s, ambos controles desde pausa y teclas Android con pantalla apagada. La primera ejecución detectó una discrepancia predictiva de posición al inicio de la cola; se corrigió y la ejecución final completa terminó con código −1.

Los **siete archivos previos de audio conservaron sus hashes** y los ajustes se mantuvieron. Volumen **9 → 9**. Se comprobó después la salida activa de Music en **EJ-A6101/A2DP**. Los eventos automáticos fueron inyectados por Android; posteriormente el usuario confirmó que los botones físicos cambian de pista y reproducen desde cero incluso desde pausa. Se conservó el sonido 0.7.2 aprobado, sin nueva evaluación auditiva. Detalles: [CONTROLES_PISTAS.md](CONTROLES_PISTAS.md). Evidencia: `outputs/verificacion/saltos-pista`.

## Music 0.7.2 · Pantalla apagada y más referencias · 24 de noviembre de 2026

Se conservó **exactamente el APK Benchmark aprobado** (SHA-256 coincidente con el instalado) y los hashes de todos los archivos de producción. No hubo cambios de sonido ni incremento de versión. Debug, assembleBenchmark, instrumentación, **28 tests JVM** y Lint (**0 errores, 11 advertencias**) completados para la nueva preparación de referencias.

En el Moto g84 5G físico con EJ-A6101 se prepararon siete archivos locales con sus niveles guardados: NO VENTO, GENETICA, SEM DIREÇÃO, CONTIGO, DANÇAR NO SALÃO, PPPP y A Thousand Years. Se verificaron reproducción en reposo, pausa/reanudación, siguiente/anterior, una transición natural y fragmentos de las siete canciones. La salida de Music siguió siendo Bluetooth multimedia. Volumen automático **9 → 9**.

La ventana de observación fue de **9 min 51 s con muestras discretas**, pausas programadas y repetición de fragmentos. PowerManager informó Dozing y el **panel integrado se verificó OFF/committedState OFF**. La supresión temporal de pantalla ambiental usada en la prueba se retiró y se confirmó su restauración.

Se corrigió un falso aviso del lector de posiciones de PlaybackState incorporando su marca temporal y se repitió el tramo vocal; no fue una modificación de Music. Después se realizó un monitor pasivo sin comandos inyectados y el usuario probó los controles físicos de los auriculares. Confirmó: **«Todo responde y suena estable»**. No pidió ajustes del equilibrio, por lo que se conserva la base aprobada.

El teléfono estaba cargando y conectado a ADB. No se declara prueba prolongada de autonomía, Doze profundo o captura acústica. Detalles y distinción de mediciones/escucha: [PANTALLA_APAGADA.md](PANTALLA_APAGADA.md). Evidencia en `outputs/verificacion/pantalla-apagada`.

## Music 0.7.2 · Bluetooth y sonido aprobado · 24 de noviembre de 2026

La base **0.7.1** se reprodujo en los EJ-A6101. El AudioTrack de Music apareció iniciado en el puerto **15068**, que AudioPolicy identificó como **BT A2DP Out, EJ-A6101**, con USAGE_MEDIA/CONTENT_TYPE_MUSIC y PCM estéreo de 48 kHz. STREAM_MUSIC Bluetooth se encontró en **8** y no se modificó. El usuario confirmó bajo más equilibrado y voz estable, solicitando más presencia de voz respecto al bajo y un último ajuste de limpieza.

Se instaló **0.7.2-benchmark, código 13**, sin borrar datos: grave nominal +0,4 dB, recorte de medios bajos −1,5 dB, presencia +1,4 dB y apertura lateral del 16 % sobre una transición de 1 kHz. PlaybackService, TrackLoudness, RecommendedProcessor, Personalizado y las pantallas principales conservaron sus hashes. La clave de caché cambia para medir el balance nuevo.

**Debug, assembleBenchmark, instrumentación, 28 tests JVM y Lint (0 errores, 11 advertencias) completados**. Respuesta calculada a señal baja: 60 Hz/1 kHz **−0,38 dB** y 2,4 kHz/240 Hz **+2,85 dB**. Pico máximo del estrés 0,8413952, dentro de la tolerancia respecto al techo.

En el **Moto g84 5G físico, Android 15**, pasaron **13 grupos Bluetooth** (instrumentación −1), con 370 canciones reales. Se comprobó el AudioTrack propio iniciado en EJ-A6101/A2DP, inicio sin caché, uso posterior del nivel guardado, PCM que avanza sin aplicar el análisis a la canción en curso, pausa/seek/anterior/siguiente, reutilización de caché, aleatorio/repetición, cambios Original/Personalizado/Recomendado, teclas multimedia Android y **12 s en segundo plano**. STREAM_MUSIC permaneció en **8**. Los eventos de teclas fueron inyectados por Android, no pulsaciones físicas de los auriculares.

El caso sin caché mantuvo **−9 → −9 dB**, con **0 cambios tardíos de ganancia al DSP** tras completar la medida. El ajuste de −6,8977833 dB quedó guardado y fue aplicado al volver a iniciar la pista, conservando el nivel durante esa reproducción. Las tres referencias quedaron medidas para la escucha comparativa.

| Referencia local | Ganancia fija (dB) | Calibrada (LUFS) | Pico de muestra (dBFS) |
|---|---:|---:|---:|
| POUCO (Slowed) · Repsaj | −6,90 | −16,001 | −3,21 |
| HIKARI · BellyJay | −8,32 | −16,001 | −5,05 |
| NO VENTO (Slowed) · TRXVELER, TESSAI | −9,31 | −16,001 | −5,28 |

La comprobación final en Benchmark conservó la cola de tres referencias y Recomendado marcado; volvió a relacionar el AudioTrack iniciado con el puerto 15068 de **EJ-A6101, BT A2DP Out**. Se inició NO VENTO a nivel calibrado para escuchar, manteniendo el volumen 8. El usuario confirmó que sonaba excelente, totalmente a su gusto y que **no ajustaría nada más por ahora**. Se registra **0.7.2 como base aprobada en los EJ-A6101**; no se modificó más el DSP tras esa respuesta.

No se usó emulador. La aprobación auditiva se limita a la escucha indicada; las medidas son internas y no sustituyen una captura acústica, medición del códec negociado o una prueba prolongada de cortes/batería. Detalles: [AUDIO_BLUETOOTH.md](AUDIO_BLUETOOTH.md). Evidencia en `outputs/verificacion/audio-bluetooth`, incluida `aprobacion-auditiva.json`, y capturas en `outputs/screenshots/audio-bluetooth`.

## Music 0.7.1 · Voz, bajo e inicio estable · 24 de noviembre de 2026

**Debug, assembleBenchmark, instrumentación, 28 tests JVM sin fallos y Lint (0 errores, 11 advertencias)** completados offline. Se conserva Media3/ExoPlayer. Los hashes de biblioteca, Mini Player, Reproduciendo, escáner, PlaylistStore y DSP de Personalizado coinciden con los anteriores.

Los tests nuevos/revisados comprobaron grave contenido, relación presencia/medios bajos, ausencia de reflexiones retardadas, mono centrado y suma mono, canales independientes, picos, señales no finitas, ganancias positivas/negativas y transiciones a 8/44,1/48/96 kHz y 1/2/6 canales según el caso. Respuesta observada a señal baja: **60 Hz/1 kHz +0,32 dB; 2,4 kHz/240 Hz +2,06 dB**. Pico máximo del estrés: 0,8413952, dentro de la tolerancia de 0,000002 respecto al techo −1,5 dBFS.

### Inicio sin caché y nivel guardado

En el **Moto g84 5G físico, Android 15**, con 370 canciones reales y sin auriculares, pasaron **11 grupos de instrumentación** (código −1). El caso principal abrió una URI temporal del mismo archivo POUCO para asegurar una caché vacía sin borrar datos del usuario.

Ganancia inicial seleccionada: **−9 dB**. Tras terminar el análisis completo: **−9 dB**. Cambios tardíos de ganancia enviados al DSP: **0**. La medida calculada fue **−6,972468 dB**, guardada para una reproducción futura. Al salir de esa pista y volver a iniciarla, la prueba confirmó el uso de esa medida desde el comienzo, manteniéndose estable durante los tres segundos siguientes.

También se verificó reproducción con la cola real, que el análisis no cambie el nivel de la pista en curso, pausa, seek, anterior/siguiente, reutilización de caché, aleatorio/repetición, transiciones Original/Personalizado/Recomendado, teclas multimedia Android y cinco segundos en segundo plano. **STREAM_MUSIC se mantuvo en 10**, el valor encontrado al empezar la prueba.

### Referencias locales de Phonk

Dos decodificaciones completas por archivo: balance nuevo sin nivelación y salida PCM16 con el nivel calculado desde el inicio. Estos valores corresponden a la reproducción ya calibrada; la primera escucha sin medida usa el nivel prudente fijo.

| Canción local | Antes de nivelar (LUFS) | Ganancia fija (dB) | Calibrada (LUFS) | Pico de muestra (dBFS) |
|---|---:|---:|---:|---:|
| POUCO (Slowed) · Repsaj | −9,03 | −6,97 | −16,001 | −3,40 |
| HIKARI · BellyJay | −7,83 | −8,17 | −16,001 | −4,83 |
| NO VENTO (Slowed) · TRXVELER, TESSAI | −6,76 | −9,24 | −16,001 | −5,39 |

Las dos pasadas de la prueba tardaron 24,3/32,3/60,4 s respectivamente. Producción mide una vez y guarda el resultado; no bloquea el inicio de la música esperando esa medición. La clave de perfil cambió para no reutilizar medidas del balance anterior.

### Versión instalada y límites

**0.7.1-benchmark, código 12**, instalada sin borrar datos. Desde la interfaz se comprobó Recomendado marcado, la explicación nueva, reproducción/pausa y la cola conservada con las tres referencias. La salida observada fue altavoz. Se dejó **NO VENTO pausada**, con Recomendado activo. La captura real de los ajustes se observó para revisar su presentación.

El defecto de sustitución tardía del nivel quedó cubierto por la prueba física. Las curvas y los picos son mediciones digitales internas; **la valoración auditiva de la nueva voz y el grave queda pendiente del usuario**. No se afirma ausencia de distorsión acústica, aprobación del timbre, true-peak ni prueba prolongada de Bluetooth o batería. No se utilizaron emuladores ni se modificó NAS Reels.

Archivos, comportamiento y límites: [AUDIO_CLARIDAD.md](AUDIO_CLARIDAD.md). Evidencia en `outputs/verificacion/audio-claridad` (incluido `inicio-estable.json`) y capturas en `outputs/screenshots/audio-claridad`.

## Music 0.7.0 · Recomendado y nivel por canción · 24 de noviembre de 2026

Compilación offline **Debug, assembleBenchmark e instrumentación** completada. **26 tests JVM sin fallos** y Lint **0 errores, 11 advertencias**. Se comprobaron señales de referencia para el medidor, puertas de silencio, límites de ganancia, cambios de nivel, conservación de transitorios, graves, centro/suma mono y picos a 8/44,1/48/96 kHz y 1/2/6 canales, según la prueba. La medición de sonoridad admite mono/estéreo; otros formatos mantienen la atenuación provisional.

Solo **Moto g84 5G físico, Android 15, 370 canciones reales y sin auriculares**. Primera ronda de audio: **10 grupos PASS** con cuatro archivos completos. Segunda ronda, usando coincidencias de la playlist Phonk del usuario: **9 grupos PASS**. Regresión de barra/seek: **6 grupos PASS**. Códigos de instrumentación −1. Se comprobaron PCM de Recomendado, ganancia por canción aplicada/reutilizada desde caché, reproducción/pausa, seek, anterior/siguiente, aleatorio/repetición, retorno a Original/Personalizado y cinco segundos de reproducción en segundo plano. Los comandos multimedia Android respondieron; **STREAM_MUSIC se mantuvo en 5**, sin ajustar el volumen global.

### Referencias de la playlist Phonk

La vista pública de [Phonk](https://open.spotify.com/playlist/0RjvXGqxw43wMyvIKioT8w) expuso 31 títulos. Se compararon metadatos/duraciones con la biblioteca; no se obtuvo audio de Spotify ni se afirma identidad de máster. Estos resultados proceden de dos decodificaciones completas de **cada archivo local**: balance previo a nivelación y salida PCM16 con la ganancia calculada desde el inicio.

| Archivo local | Balance previo (LUFS) | Ganancia fija (dB) | Resultado (LUFS) | Pico de muestra (dBFS) |
|---|---:|---:|---:|---:|
| MONTAGEM POUCO (Slowed) · Repsaj | −7,83 | −8,17 | −16,001 | −6,63 |
| MONTAGEM HIKARI · BellyJay | −7,07 | −8,93 | −16,001 | −5,84 |
| MONTAGEM NO VENTO (Slowed) · TRXVELER, TESSAI | −5,85 | −10,15 | −16,001 | −8,15 |

Las dos pasadas de cada referencia tardaron **37,1–51,2 s** en esta prueba. Producción realiza una pasada cuando falta la medida y la guarda; la reproducción empieza mientras se analiza y el nivel entra gradualmente. No se presenta el análisis inicial como instantáneo. Las pruebas de integración comprobaron la aplicación de la medida y su reutilización al regresar a la pista.

### Otras canciones locales y controles

MONTAGEM NEBLINA pasó de −5,83 a −16,001 LUFS; MONTAGEM ECLIPSE, de −5,11 a −16,001; SAD FUNK - Slowed, de −9,15 a −16,001; A Thousand Years, de −10,48 a −16,001. Sus picos normalizados estuvieron entre **−8,09 y −4,01 dBFS**. Los niveles previos corresponden al balance nuevo antes del limitador/nivelación, no al archivo Original ni a Spotify.

La regresión física del progreso volvió a comprobar el tamaño 6/18/48 dp, toque/arrastre hacia delante y atrás en pausa/reproducción, previsualización/cancelación, saltos rápidos, límites accesibles y reinicio al cambiar de canción. No hubo cambios de diseño en biblioteca, Mini Player o Reproduciendo. Los hashes de esas pantallas, escáner, PlaylistStore y DSP de Personalizado coinciden con los anteriores.

### Alcance de la comprobación

Son mediciones digitales internas y pruebas de funcionamiento. **No equivalen a una medición acústica del altavoz**, aprobación auditiva, prueba prolongada sin cortes, medición de batería o garantía de picos tras el códec/DAC. El medidor no está certificado y el techo no es true-peak. Falta escuchar el nuevo balance con el usuario y, en una fase posterior, probarlo con sus auriculares Bluetooth.

La comprobación final desde la interfaz dejó instalada **0.7.0-benchmark, código 11**, sin depuración ni borrado de datos. Se conservó la cola con las tres referencias, se seleccionó NO VENTO desde Cola y se comprobó reproducción/pausa, la explicación nueva y Recomendado marcado. AudioPolicy/AudioManager mostraron salida de altavoz. La sesión quedó **pausada**, con Recomendado activo. Se observaron las capturas reales de los ajustes; no se atribuye a esto aprobación auditiva.

Detalles, parámetros y archivos: [AUDIO_EQUILIBRADO.md](AUDIO_EQUILIBRADO.md). Evidencia en `outputs/verificacion/audio-equilibrado`: medidas JSON, coincidencias Spotify, informes de instrumentación, compilación, Lint e integridad. Capturas en `outputs/screenshots/audio-equilibrado`.

## Music 0.6.1 · Barra y búsqueda temporal · 24 de noviembre de 2026

Debug, **assembleBenchmark**, instrumentación, **20 tests JVM sin fallos** y Lint (**0 errores, 11 advertencias**) completados. Prueba enfocada en el Moto g84 5G físico, Android 15 y 370 canciones reales: **6 grupos PASS**, código de instrumentación −1.

La barra mide 6 dp, el indicador 18 dp y la zona táctil 48 dp. Se verificaron avances/retrocesos por toque y arrastre, previsualización antes de soltar, saltos rápidos, cancelación, inicio/final, límites accesibles y botones anterior/siguiente. Se probó tanto en pausa como reproduciendo, manteniendo la cola y sin volver a un destino anterior después de confirmar. La comparación usó tolerancias posicionales de 0,9 s en pausa y 1,6 s con reproducción en marcha.

La distribución y las capturas pasaron en cuatro ventanas del mismo Android: 1080×2400, 720×1280, 720×1440/fuente 130 % y 1600×900; ajustes de pantalla restaurados. Motor, sonido, biblioteca, paginador, Mini Player y botones actuales conservaron su implementación. Archivos, evidencia y límites: [PROGRESO.md](PROGRESO.md). Ningún emulador.

Benchmark 0.6.1 (código 10) instalada: toques y arrastres hacia delante/atrás confirmados también al recuperar una sesión pausada sin preparar, usando el comando Media3 disponible. Destinos del 75/25/65 % confirmados en 116/38/100 s de 154 s; retroceso durante reproducción confirmado en 55 s tras el avance del reloj. Cola intacta, Recomendado conservado y reproducción pausada al terminar.

## Music 0.6.0 · Categorías deslizables · 24 de noviembre de 2026

Debug, **assembleBenchmark**, instrumentación, **20 tests JVM sin fallos** y Lint (**0 errores, 11 advertencias**) completados. En el Moto g84 5G físico con Android 15 y 370 canciones reales se verificaron **4 grupos de gestos y 5 grupos de regresión de biblioteca**, ambos runners con código −1.

Se comprobó el movimiento con el dedo apoyado, ida/vuelta por las tres categorías, una página por gesto, bordes sin bucle, tolerancia a movimientos mínimos y arrastres cortos, scroll vertical independiente, conservación de posición/búsquedas y reproducción que avanza con la misma cola. La regresión revisó **21 pre-draws** durante ocho regresos desde Reproduciendo sin cargas ficticias. No se usó emulador.

ViewModel, servicio, escáner, almacenamiento, Mini Player y Reproduciendo conservaron sus hashes. Archivos, parámetros y evidencia: [DESLIZAMIENTO.md](DESLIZAMIENTO.md).

Benchmark 0.6.0 (código 9) instalada y comprobada con gestos reales sobre las filas, retorno de un arrastre corto y toques de pestañas, conservando la cola. Canciones quedó seleccionada y la pista pausada.

## Music 0.5.1 · Controles ampliados · 24 de noviembre de 2026

**assembleBenchmark**, Debug, instrumentación, **20 tests JVM sin fallos** y Lint (**0 errores, 12 advertencias**) completados. La prueba enfocada en Reproduciendo pasó **6 grupos físicos** sobre el Moto g84 5G con 370 canciones reales.

Se verificaron controles, favoritos, seek, modos, navegación y sincronización. La revisión final de geometría y capturas pasó en cuatro ventanas del mismo Android: **1080×2400, 720×1280, 720×1440/fuente 130 % y 1600×900**. Medidas observadas: play/área secundaria **72/56 dp** en vertical y **68/52 dp** en horizontal, con el círculo completo, sin superposiciones ni recortes. Se restauraron los ajustes de pantalla originales. Motor, sonido, Mini Player, biblioteca y playlists conservaron sus hashes.

Dimensiones, archivos y evidencia: [REPRODUCIENDO.md](REPRODUCIENDO.md). No se utilizó emulador. Los resultados de 27 grupos de 0.5.0 permanecen como historial de esa entrega.

## Music 0.5.0 · Reproduciendo · 24 de noviembre de 2026

Debug, **assembleBenchmark**, APK de instrumentación, **20 tests JVM sin fallos** y Lint (**0 errores, 12 advertencias**) completados. **27 grupos de integración física PASS** con **370 canciones reales** en el Moto g84 5G, Android 15; ningún emulador.

Se comprobaron carátula circular estática mediante píxeles idénticos durante reproducción y pausa, títulos/artistas largos y respaldo real, favoritos, seek, play/pausa, anterior/siguiente, aleatorio, repetición, ajustes de sonido, cola inferior y sincronización de pantallas. Se retiró únicamente el acceso horizontal redundante a la cola.

La prueba final de geometría pasó en ventanas **1080×2400, 720×1280, 720×1440 con fuente 130 % y 1600×900** del mismo teléfono. Se confirmó el círculo completo de play y controles de 48 dp sin superposiciones; se inspeccionaron las capturas. Se restauraron tamaño y fuente originales. La APK Benchmark 0.5.0, código 7, quedó instalada, en Recomendado y pausada; también se probaron reproducción, seek táctil y navegación en esa APK.

El motor, DSP, almacenamiento, ViewModel, biblioteca, playlists y Mini Player conservaron sus hashes. Los paneles de cola/sonido no cambiaron; la barra compartida solo añadió elipsis para etiquetas que no caben con fuente ampliada. Archivos, diferencias de referencia, comandos, evidencia y límites: [REPRODUCIENDO.md](REPRODUCIENDO.md).

## Music 0.4.1 · Navegación sin recarga · 24 de noviembre de 2026

Compilaciones Debug y **Benchmark**, APK de instrumentación, **20 tests JVM sin fallos** y Lint (**0 errores, 12 advertencias**) completados. En el Moto g84 5G físico con Android 15 y **370 canciones reales**, la prueba enfocada finalizó con **5 grupos PASS** y código de instrumentación **−1**.

Se revisaron **24 pre-draws** durante las primeras ocho vueltas Reproduciendo → Biblioteca: no aparecieron spinner, lista vacía o reconexión ficticia; se conservaron los resultados y el desplazamiento. Cinco vueltas adicionales verificaron búsqueda real, Más antiguos, Favoritos y Playlists. Actualizar una biblioteca sin cambios conservó los resultados. Se volvió a Más recientes y se dejó la reproducción pausada. Los hashes del escáner, servicio, DSP y Mini Player no cambiaron durante esta corrección.

Detalles, archivos y límites: [NAVEGACION_BIBLIOTECA.md](NAVEGACION_BIBLIOTECA.md). Evidencia junto al APK en `verificacion/navegacion-*`. No se usó emulador ni se repitió una evaluación auditiva.

## Music 0.4.0 · Mini Player · 24 de noviembre de 2026

Compilación Debug, **assembleBenchmark**, APK de instrumentación, **20 tests JVM sin fallos** y Lint (**0 errores, 12 advertencias**) completados. **18 grupos de integración PASS** en el **Moto g84 5G físico con Android 15**, biblioteca de **370 canciones reales**; ningún emulador.

Se comprobaron los cuatro controles, anterior/siguiente con aleatorio, cierre que detiene y vacía la cola, retirada de notificación, espacio recuperado por la biblioteca, permanencia del cierre al navegar y nueva reproducción explícita. Favoritos y playlists no cambiaron al cerrar. El título largo conserva una única línea de tiempo al pausar/reanudar o navegar; el corto permanece estático. La APK Benchmark también mantuvo el cierre tras detener el proceso/reabrir y recibir Play remoto, y restauró el Mini Player al seleccionar otra canción. Se observó la composición real en una captura del teléfono y se comprobó el acceso al reproductor completo desde la carátula. La app quedó instalada y pausada.

Archivos, escenarios y límites: [MINI_PLAYER.md](MINI_PLAYER.md). Evidencia completa junto al APK en `verificacion/mini-*`. No se cambió el DSP ni se hizo una evaluación auditiva en esta fase. Se conservan las pantallas y categorías existentes.

---

# Historial · Music 0.1.0, revisión visual

Fecha local: 23 de noviembre de 2026. Equipo: Aiko. Único dispositivo de pruebas Android: **Moto g84 5G, Android 15 (API 35)**, conectado mediante ADB. No se inició ni usó emulador.

## Compilación y análisis estático

Comando ejecutado en el proyecto:

```powershell
.\gradlew.bat --offline :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
```

Resultado: **BUILD SUCCESSFUL**. APK de app y APK de instrumentación generados. Android Lint: **0 errores, 8 advertencias**. Las advertencias incluyen target SDK fijado en 35, servicio exportado para clientes multimedia, reglas de extracción de datos y sugerencias de uso de extensiones KTX. No se desactivaron las comprobaciones ni se añadió baseline para ocultar errores.

La aplicación final es una compilación **debug**, no una publicación firmada para distribución en tienda. Se instaló sobre la primera versión conservando permisos y datos privados.

## Tests unitarios JVM

**5 tests, 0 fallos y 0 errores**:

- Búsqueda independiente de mayúsculas, espacios exteriores y acentos.
- Cota de pico con DC, señal cuadrada, ruido e impulsos a escala completa; cambios de controles en vivo; 8/44.1/48/96 kHz; 1/2/6 canales. Pico máximo observado en este conjunto: **0.5708582** de escala completa, por debajo del techo de −1 dB. Ese valor corresponde al conjunto de prueba, no es una medición de la salida del teléfono.
- Cambio de balance de frecuencias al aplicar graves/brillo.
- Separación entre canales.
- Límites de controles y aumento del margen al elevar el tono.

## Integración en el Android físico

Ejecutado el runner `dev.aiko.music.MusicInstrumentation` contra la versión rediseñada, con archivos existentes en el teléfono. Los ocho grupos finalizaron **PASS**:

1. Consulta MediaStore: **369 canciones** indexadas.
2. Decodificación local y posición que avanza durante la reproducción.
3. Pausa que mantiene la posición.
4. Búsqueda temporal dentro de la pista y anterior/siguiente.
5. Inserción, reordenación y retirada de entradas de la cola, aleatorio y repetición.
6. Ruta PCM de mejora funcionando: contador de muestras del procesador aumenta; ajustes de tono en vivo sin error de reproductor.
7. Retorno a Original conservando cola y reproducción.
8. Reproducción del servicio que sigue avanzando después de enviar la Activity a Inicio.

El runner inicia la reproducción con la Activity visible, como el usuario, para respetar las reglas de audio focus de Android 15; después prueba el segundo plano. Se deja la reproducción pausada al terminar. La ejecución no modifica archivos musicales.

## Interfaz en el Android físico

Comprobaciones a través de la interfaz real, con capturas y árbol de accesibilidad de Android:

- Encabezado, degradados, fondo con ondas fundidas, cápsulas, filas compactas con separadores y carátulas reales.
- Búsqueda en vivo “Thousand”: dos coincidencias reales. Comparación de la cola privada antes/después: **sin cambios**.
- Biblioteca → Reproduciendo → Biblioteca conserva la búsqueda y sus resultados.
- Búsqueda sin coincidencias: mensaje específico, limpieza disponible y cola intacta.
- Al limpiar la búsqueda, la lista vuelve al inicio de A–Z después de recibir los resultados, en lugar de anclarse a la primera pista del filtro anterior.
- Favoritos muestra las canciones ya guardadas.
- Álbumes abre su lista real de canciones.
- Cambiar de categorías conserva la cola.
- Desplazar la biblioteca, abrir Reproduciendo y volver conserva la posición del listado.
- El botón flotante inicia reproducción aleatoria con las 369 canciones visibles.
- Play/pausa y siguiente del minirreproductor cambian el estado y los metadatos de la sesión real.
- El acceso inferior Cola abre la cola existente; el menú superior conserva Actualizar biblioteca y Sonido.
- Tocar el cuerpo del minirreproductor abre la pantalla completa existente. El control visual de progreso cambia la posición real de reproducción.
- Reproducción iniciada desde la interfaz y enviada a Inicio: servicio foreground activo, notificación multimedia con tres acciones y estado PLAYING.
- El comando multimedia de Android pausa la misma sesión y el cambio se refleja al volver a Music.

El resto de las comprobaciones de controles y las capturas finales se registran junto al APK en la carpeta de evidencia de esta entrega.

## Integridad del alcance

Los hashes SHA-256 de `MusicViewModel.kt`, `data/Library.kt`, `playback/PlaybackService.kt`, `playback/ToneEngine.kt` y `playback/ToneProcessor.kt` permanecen idénticos al inicio de la renovación visual. No se cambiaron el escaneo, almacenamiento, motor, DSP ni dependencias en esta fase.

No se escribieron archivos dentro del proyecto NAS Reels. Music mantiene su carpeta, package y servicio independientes. Los dos archivos Gradle de NAS Reels consultados como referencia al crear el proyecto se comprobaron sin cambios.

## Pendiente y límites de lo comprobado

- No se ha evaluado auditivamente la fidelidad ni capturado la salida analógica/digital del teléfono. Una posición que avanza y un procesador activo verifican funcionamiento, no calidad subjetiva o ausencia de distorsión en toda la cadena.
- No se ha medido THD+N, true-peak, respuesta de salida, consumo sostenido, térmicas o latencia. No se afirma bit-perfect.
- Falta probar desconexión física de auriculares, llamadas, varias salidas Bluetooth/USB, batería durante horas, reinicio completo y todos los formatos admitidos por Media3. El manejo de audio focus y desconexión está implementado en el motor existente, pero esta entrega no convierte esos escenarios en comprobados.
- La biblioteca real probada tiene 369 canciones. Se implementaron índice en memoria, filtros fuera del hilo principal y lista perezosa; no se ha medido el rendimiento con decenas de miles de pistas.
- No se ha hecho auditoría completa de TalkBack, texto al 200 %, varios tamaños físicos, paisaje o idiomas RTL. Se añadieron etiquetas semánticas y controles táctiles de 48 dp como base.
- Carpetas y playlists guardadas con nombre continúan pendientes. Diferencias de composición y decisiones: [UI_REFERENCIA.md](UI_REFERENCIA.md).
