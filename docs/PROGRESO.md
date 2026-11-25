# Music 0.6.1 · Barra de reproducción

La barra de Reproduciendo pasa de **4 a 6 dp** y su indicador blanco de **12 a 18 dp**, conservando el degradado morado/azul y los extremos redondeados. El indicador se dibuja dentro de una zona táctil directa de **48 dp de alto**; no es necesario acertar a la línea fina. Los botones de 72/68 dp y su distribución permanecen iguales.

## Precisión al buscar una posición

Se conserva el Slider accesible de Material3 y la integración `MusicViewModel.seek → Media3.seekTo`. Si la canción está recuperada pero todavía sin preparar, se utiliza el comando disponible de búsqueda por índice manteniendo el mismo elemento. Esto aplica el destino antes de reproducir, sin activar play, cambiar de canción ni reconstruir el motor.

La validación física detectó que el umbral de inicio del arrastre restaba parte del movimiento y dejaba el indicador algo desplazado respecto al dedo. Dentro de esta barra se aplica una respuesta de arrastre inmediata, sin descartar ese desplazamiento inicial. El cambio está limitado al Slider: la sensibilidad del paginador de biblioteca permanece en su configuración moderada anterior.

Mientras se arrastra se muestra el destino y su tiempo; la búsqueda real se envía al soltar. Se conserva el destino mientras Media3 confirma la nueva posición y después se recupera el avance normal. Una cancelación del arrastre resincroniza la previsualización con el estado real. Cambiar de canción reinicia los estados locales de la barra para no arrastrar un destino de la pista anterior.

No se cambia el motor, la decodificación, DSP, volumen, cola, Mini Player, biblioteca, favoritos, playlists ni el paginador.

## Archivos

- `ui/NowPlayingScreen.kt`: dimensiones de la barra/indicador, superficie de interacción, respuesta precisa y cancelación del gesto.
- `ui/MusicTheme.kt`: tokens de grosor, indicador y superficie táctil.
- `MusicViewModel.kt`: elige el comando Media3 disponible para buscar también en una sesión pausada sin preparar.
- `SeekBarDeviceChecks.kt`: pruebas con toques y arrastres reales en el Android.
- `MusicInstrumentation.kt`: modo enfocado `seekOnly` e integración en el runner.
- `app/build.gradle.kts`: versión 0.6.1, código 10.

No se añaden dependencias. Las etiquetas internas de prueba permiten medir el recorrido y el indicador sin añadir etiquetas visibles ni sustituir la interacción nativa.

## Verificación · 24 de noviembre de 2026

`assembleDebug`, **`assembleBenchmark`**, `assembleDebugAndroidTest`, `testDebugUnitTest` y `lintDebug`: **BUILD SUCCESSFUL**. **20 tests JVM sin fallos**; Lint: **0 errores, 11 advertencias**.

Prueba enfocada en el **Moto g84 5G físico, Android 15**, biblioteca de **370 canciones reales**: **6 grupos PASS**, `INSTRUMENTATION_CODE: -1`.

- Grosor medido 6 dp, indicador de 18 dp y superficie de 48 dp.
- En pausa: toque hacia el 75 %, arrastre de regreso al 20 % con previsualización y toque hacia el 60 %. Se probaron también puntos 14 dp por encima y por debajo de la línea, dentro de su zona táctil.
- Varios saltos rápidos alternando avance/retroceso: se mantiene el último destino, incluido después del tiempo de confirmación.
- Reproduciendo: avance al 70 %, retroceso al 30 %, cancelación y recuperación del estado real, conservando reproducción y cola.
- Inicio, final, límites y acción accesible de progreso; anterior/siguiente con reinicio del progreso y metadatos sincronizados.

En las pruebas se comparó la posición de Media3 con el destino de los gestos: tolerancia de **0,9 s de desviación posicional en pausa** y **1,6 s durante reproducción**, incluyendo el avance del reloj. No son medidas de latencia acústica. Se verificó además que la previsualización no vuelva a una posición anterior después de confirmar el salto.

La revisión geométrica y visual pasó en cuatro ventanas del mismo Android: **1080×2400**, **720×1280**, **720×1440 con fuente al 130 %** y **1600×900**. El círculo de play y los controles permanecieron completos, sin superponerse a la navegación. Se restauraron tamaño y fuente originales (densidad 340, fuente 100 %).

Los hashes de MainActivity, servicio, motores de sonido, biblioteca, paginador, playlists y Mini Player permanecen iguales al inicio. El ViewModel solo cambia la selección del comando de búsqueda temporal. El bloque de botones de Reproduciendo también se comparó con su versión anterior y es idéntico.

Evidencia junto al APK: `verificacion/progreso-compilacion.log`, `progreso-android.log`, `progreso-tamanos.log`, `progreso-integridad.json` y carpeta `verificacion/progreso` con informes JVM, Lint y capturas de geometría. Imágenes en `screenshots/progreso`.

**Music 0.6.1 Benchmark, código 10, quedó instalada**. La prueba final recuperó una sesión pausada sin preparar: toque al 75 % confirmado en **116/154 s**, retroceso al 25 % en **38/154 s** y arrastre al 65 % en **100/154 s**, antes de iniciar play. Después se reprodujo y se arrastró de vuelta al 35 %; al pausar se confirmó **55/154 s**, incluyendo el segundo de avance posterior. Se conservó la misma cola y Recomendado. Evidencia: `verificacion/progreso/benchmark-interfaz.log`, estado real de la sesión y captura `screenshots/progreso/benchmark.png`.

Toda la comprobación Android se realiza en el teléfono conectado, sin emulador. No se ha realizado una nueva evaluación auditiva ni se ha medido la latencia de la salida Bluetooth.
