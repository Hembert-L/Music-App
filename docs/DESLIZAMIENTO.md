# Music 0.6.0 · Categorías deslizables

Canciones, Favoritos y Playlists se conectan mediante `HorizontalPager` nativo de Compose. El contenido acompaña al dedo durante el arrastre; al soltar termina de encajar suavemente. Se conservan el estilo del encabezado, los toques en las pestañas y el resto de la navegación.

## Sensación del gesto

- Margen inicial horizontal de **1,4 veces el touch slop de Android**, para no reaccionar a pequeños movimientos involuntarios.
- Umbral del **42 %** de la página para un arrastre lento; los lanzamientos intencionados conservan la física nativa.
- Máximo de **una página por lanzamiento**. Los toques en pestañas pueden ir directamente a la elegida.
- Resorte amortiguado, razón **1** y rigidez **300**: entrada y salida progresivas sin oscilación repetida.
- El indicador degradado sigue la posición real de las páginas mediante colocación, sin otra animación que se quede atrás.

El margen ampliado se aplica solo al paginador. Las listas y botones internos recuperan su configuración táctil original. La búsqueda está fuera del área de arrastre; al empezar un cambio de página se oculta el teclado y se conserva el texto. Los gestos actúan sobre el contenido de la biblioteca; Mini Player y barra inferior mantienen sus acciones.

## Conservación de estado

`LibraryBrowser.pages` mantiene juntas las listas filtradas de canciones y favoritos. Se normaliza y ordena fuera del hilo principal cuando cambian los datos, la consulta, el orden o los favoritos; cambiar de página no repite ese trabajo. `results` sigue exponiendo la selección activa para los consumidores anteriores.

Cada listado conserva su propio desplazamiento. Canciones y Favoritos mantienen la consulta compartida que ya tenían; Playlists conserva su búsqueda independiente y el estado de sus pantallas. Se precarga una página vecina por cada lado. El estado guardado del paginador almacena una página entera de destino, para regresar desde Reproduciendo sin quedar a mitad de un arrastre.

El manejador de Atrás de una playlist solo actúa cuando esa página está activa. Así, una página vecina preparada fuera de pantalla no intercepta Atrás desde otra categoría. La reproducción y su cola siguen perteneciendo al servicio Media3 existente.

## Archivos

- `ui/LibraryScreen.kt`: paginador, estado de listas, sensibilidad local, navegación por toque/arrastre y presentación reutilizada por categoría.
- `ui/CompactLibraryHeader.kt`: indicador vinculado al desplazamiento real.
- `LibraryBrowser.kt`: resultados de canciones y favoritos preparados simultáneamente.
- `ui/PlaylistsScreen.kt`: condición de página activa para Atrás.
- `ui/MusicTheme.kt`: parámetros de movimiento centralizados.
- `LibraryPagerDeviceChecks.kt`: gestos físicos, sensibilidad, búsquedas y reproducción.
- `LibraryNavigationDeviceChecks.kt`: espera al final de la transición y distingue listas verticales del paginador horizontal.
- `MusicInstrumentation.kt`: modo de prueba enfocado y registro del nuevo grupo.
- `app/build.gradle.kts`: versión 0.6.0, código 9.

No se añaden dependencias ni categorías, ni se cambian el escaneo, almacenamiento, motor, sonido, Mini Player o diseño de Reproduciendo.

## Verificación · 24 de noviembre de 2026

Compilaciones Debug y **assembleBenchmark**, APK de instrumentación y **20 tests JVM sin fallos**, completados. Lint: **0 errores y 11 advertencias**, sin ocultarlas con baseline.

En el **Moto g84 5G físico con Android 15 y 370 canciones reales** se ejecutaron dos pruebas enfocadas:

- **Gestos: 4 grupos PASS**. Desplazamiento con el dedo todavía apoyado, ida y vuelta por las tres páginas, límite de una página y bordes sin bucle; desplazamiento mínimo de 5 dp y arrastre corto/lento sin cambio de categoría; scroll vertical/diagonal sin cambiar de página; ancla de la lista conservada. Toques en pestañas, consultas de canciones/playlists conservadas, regreso desde Reproduciendo y posición de audio avanzando con la misma cola.
- **Regresión de biblioteca: 5 grupos PASS**. Ocho regresos de Reproduciendo con **21 pre-draws** de biblioteca revisados: sin spinner, vacíos ni reconexión ficticia. Búsqueda, Más antiguos, Favoritos, Playlists y actualización de MediaStore sin cambios conservan el comportamiento anterior.

Ambas ejecuciones finalizaron con `INSTRUMENTATION_CODE: -1`. Se capturó el contenido a mitad de un arrastre para comprobar que se desplaza de forma continua junto al indicador. Los hashes de ViewModel, servicio, escáner, almacenamiento, Mini Player, Reproduciendo y componentes compartidos coinciden con los del inicio del trabajo.

**Music 0.6.0 Benchmark, código 9, quedó instalada**. La prueba final desde la interfaz real arrastró directamente sobre las filas para ir y volver entre las tres categorías, comprobó el retorno de un arrastre corto/lento y los toques de pestañas. La cola mantuvo la misma cantidad de canciones, sin disparar una selección al arrastrar. Se dejó Canciones activa y la reproducción pausada. Evidencia adicional: `verificacion/deslizamiento/benchmark-interfaz.log` y `screenshots/deslizamiento/benchmark.png`.

Evidencia junto al APK: `verificacion/deslizamiento-compilacion.log`, `deslizamiento-gestos.log`, `deslizamiento-regresion.log`, `deslizamiento-integridad.json` y carpeta `verificacion/deslizamiento` con informes JVM y Lint. Capturas en `screenshots/deslizamiento`.

Las comprobaciones se hicieron en el Android conectado, sin emulador. La sensibilidad es una configuración inicial moderada; no se ha medido FPS ni latencia de extremo a extremo en distintos dispositivos. El audio y sus parámetros no se modificaron en esta fase.
