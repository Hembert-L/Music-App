# Music 0.4.1 · Regreso inmediato a la biblioteca

## Causa y corrección

La pantalla de biblioteca salía de la composición al abrir Reproduciendo. Sus dos `produceState` comenzaban otra vez con un índice vacío y una lista vacía al regresar; se repetían la normalización, búsqueda y ordenación. Además, los flujos locales de estado de reproducción comenzaban con `connected = false`, aunque la sesión seguía conectada.

`LibraryBrowser`, propiedad del `MusicViewModel`, conserva en memoria el índice y el último resultado mediante un `StateFlow` compartido de forma continua. La pantalla recibe el resultado actual desde su primera composición. El índice solo se reconstruye cuando cambia la lista real de canciones; los cambios de búsqueda, favoritos u orden recalculan el filtro fuera del hilo principal. Ni navegar ni las actualizaciones de progreso invalidan los datos.

Las proyecciones de reproducción de biblioteca y playlists también viven con el ViewModel. Al volver se conserva la conexión y la canción activa, sin un estado inicial ficticio de reconexión. `rememberSaveableStateHolder` y `LazyListState` siguen conservando la búsqueda, la pestaña y la posición del listado; no se mantiene una biblioteca invisible dibujándose detrás del reproductor.

La carga inicial de una biblioteca y sus cambios reales siguen siendo asíncronos. El menú Actualizar y la observación de MediaStore continúan funcionando. No se cambian el escaneo, almacenamiento, motor Media3, DSP, Mini Player ni diseño.

## Archivos

- `LibraryBrowser.kt`: índice, filtro y resultados con vida independiente del composable.
- `MusicViewModel.kt`: propiedad del navegador y proyecciones estables de reproducción.
- `ui/LibraryScreen.kt`: consume el resultado retenido y elimina los cálculos que se reiniciaban al volver.
- `ui/PlaylistsScreen.kt`: consume el estado real retenido de la sesión.
- `LibraryNavigationDeviceChecks.kt` y `MusicInstrumentation.kt`: regresión sobre el teléfono, incluidos los primeros fotogramas.
- `app/build.gradle.kts`: versión 0.4.1, código 6.

## Verificación

La prueba de regresión observa los nodos semánticos de Compose en cada `OnPreDraw`, incluido el primer fotograma después de volver desde Reproduciendo. Comprueba que no aparezcan spinner indeterminado, lista vacía o «Conectando reproductor…» cuando ya existen datos, y que se conserve el ancla del desplazamiento. La identidad del resultado confirma que navegar no vuelve a calcularlo.

También ejercita búsqueda con metadatos reales, orden Más antiguos/Más recientes, Favoritos, Playlists y actualización de MediaStore sin cambios. Se usa únicamente el Moto g84 5G físico conectado a Aiko, nunca un emulador.

Resultados del **24 de noviembre de 2026**:

- `assembleDebug`, **`assembleBenchmark`**, `assembleDebugAndroidTest`, `testDebugUnitTest` y `lintDebug`: **BUILD SUCCESSFUL**. **20 tests JVM, 0 fallos**, Lint con **0 errores y 12 advertencias**.
- Regresión física enfocada: **5 grupos PASS**, incluyendo lectura de **370 canciones reales**. Instrumentación termina con **INSTRUMENTATION_CODE: -1**.
- Primeras **8 idas y vueltas**: **24 pre-draws de biblioteca** revisados, sin spinner, lista vacía ni reconexión ficticia. Mismo resultado y ancla de desplazamiento.
- **3 idas y vueltas adicionales** con búsqueda real y orden Más antiguos; otras 2 desde Favoritos y Playlists. Se conserva el resultado y no hay falsos estados de carga. Se vuelve a Más recientes y se deja la reproducción pausada.
- Actualizar MediaStore sin cambios conserva el mismo resultado, sin reconstruir el índice. El escáner, servicio, DSP y Mini Player mantienen sus hashes respecto al inicio de esta corrección.
- **Music 0.4.1 Benchmark instalada** (código 6): comprobado desde la interfaz Biblioteca → Reproduciendo → Biblioteca, con las mismas filas visibles, orden Más recientes y sesión pausada. Captura real: `screenshots/biblioteca-navegacion.png` junto a la entrega. La comprobación por pre-draw se ejecutó en la variante Debug de este mismo código.

Esta prueba analiza la presencia del contenido y los estados de carga por fotograma; no es una medición general de FPS o latencia, ni una validación de todos los tamaños de biblioteca. Los 18 grupos físicos de Music 0.4.0 quedan como historial de esa entrega, no se contabilizan como una nueva ejecución en 0.4.1.

Evidencia de entrega: `verificacion/navegacion-compilacion.log`, `navegacion-android.log`, `navegacion-integridad.json` y carpeta `verificacion/navegacion` con tests JVM, informe Lint y comprobación de la APK instalada.
