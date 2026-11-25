# Encabezado compacto

Revisión limitada a la parte superior de Music. Barra de herramientas de 52 dp y pestañas de 48 dp: 100 dp en total, sin contar los insets de Android. Se priorizan áreas táctiles de 48 dp sobre la orientación de pestañas de 40–44 dp de la referencia.

Se conservan el nombre Music, la paleta y las ondas nativas, reducidas al fondo del encabezado. Se retiran el icono grande, subtítulos, buscador permanente y cápsulas. El nombre combina blanco y lavanda y lleva una pequeña marca vertical degradada. Un indicador de 3 dp se desplaza durante 180 ms entre las pestañas.

La lupa transforma la primera fila en búsqueda con foco y teclado, acción de cierre y limpieza. Filtra canciones/favoritos o nombres de playlists según la pestaña. Cerrar limpia el filtro de la sección y restaura la barra. Nueva playlist sigue disponible en el menú de Playlists y en su estado vacío. No se añade una fila de búsqueda ni acciones permanente por debajo del encabezado.

Se conserva Favoritos en la segunda posición, ya existente. Carpetas no tiene implementación y añadirla implicaría ampliar el alcance más allá del encabezado y eliminar una función existente. El nombre tampoco se cambia a Nya Music, pues la referencia no solicita renombrar la aplicación.

Archivos: nuevo `ui/CompactLibraryHeader.kt`; integración en `ui/LibraryScreen.kt`; búsqueda externa y evento de creación en `ui/PlaylistsScreen.kt`; altura decorativa en `MainActivity.kt`. Se reutilizan colores, degradados y listas. Sin nuevas dependencias ni cambios de motor, persistencia o minirreproductor.

Compilación: `assembleBenchmark`, `assembleDebug`, `testDebugUnitTest`, `lintDebug`, finalizada correctamente. Las pruebas de navegación, búsqueda y menú se registran en `verificacion/encabezado-interfaz.log`. La comprobación física utiliza únicamente el Moto g84 conectado, sin emulador. No se afirma una auditoría completa de accesibilidad ni de todas las dimensiones de pantalla.

Resultado: 10 tests unitarios aprobados; Lint con 0 errores y 11 advertencias. Benchmark instalado y comprobado: teclado automático (`mInputShown=true`), búsqueda “Thousand” con dos canciones reales, limpieza/cierre con cola intacta, cambio entre pestañas, búsqueda de playlists sin coincidencias, y diálogo Nueva playlist abierto desde el menú y cancelado sin crear datos. Se conservan las opciones Actualizar biblioteca y Sonido. Captura real: `screenshots/encabezado-compacto.png`.
