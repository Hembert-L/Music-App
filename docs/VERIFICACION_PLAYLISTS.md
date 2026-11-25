# Verificación · Playlists, Music 0.2.0

Fecha local: 23 de noviembre de 2026. Equipo Aiko. Dispositivo: **Moto g84 5G, Android 15**, conectado mediante ADB. Sin emulador.

## Compilación Benchmark

Ejecutado `:app:assembleBenchmark`, junto con `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` y `lintDebug`, en modo offline con las dependencias existentes. Resultado: **BUILD SUCCESSFUL**. Lint: **0 errores, 10 advertencias**. No se añadieron dependencias ni se desactivaron comprobaciones. La variante Benchmark es una compilación local no depurable y firmada con la clave debug de Aiko, no una publicación de tienda ni una medición automática de rendimiento.

## Pruebas unitarias

**10 tests aprobados, sin fallos ni errores**: cinco existentes de búsqueda/DSP y cinco nuevos de playlists. Las pruebas nuevas comprueban nombres válidos y duplicados con acentos/mayúsculas, límite de longitud, adición sin duplicados manteniendo identidades/orden, reordenación con límites y eliminación por identidad sin mutar la lista original.

## Persistencia y audio en Android físico

El runner físico finalizó con **11 grupos PASS**. Tres grupos nuevos comprobaron la escritura/lectura con una nueva instancia del repositorio; renombrado, orden manual y retirada persistentes; conservación de referencias no disponibles; recuperación tras simular una escritura interrumpida y eliminación persistente. Se utilizó un archivo temporal aislado y se retiró después, sin tocar playlists del usuario.

Los ocho grupos anteriores comprobaron MediaStore con 369 canciones reales, decodificación/avance, pausa, seek/anterior/siguiente, edición de cola/aleatorio/repetición, ruta de mejora y ajuste en vivo, retorno a Original y reproducción del servicio al ir a Inicio. El motor y el escaneo conservan sus hashes originales de esta fase.

## Validación de interfaz

El registro de acciones y sus resultados está en `verificacion/playlists-interfaz.log`, junto a los APK. Se ejercita el flujo real de creación con canciones existentes, reproducción, edición, búsqueda, renombrado, listas vacías y persistencia tras cerrar/reabrir el proceso. Los nombres de prueba y las canciones seleccionadas pertenecen únicamente a la validación; no se precargan en el producto.

Comprobaciones realizadas en la interfaz:

- Crear una lista y añadir dos canciones reales sin alterar la cola anterior.
- Reproducir la lista completa, guardar un nuevo orden manual y conservar la cola en curso al editar.
- Renombrar mediante el menú contextual y conservar nombre, referencias y orden al detener/reabrir el proceso.
- Buscar por nombre, mostrar el estado sin coincidencias y limpiar la búsqueda sin modificar la cola.
- Rechazar un nombre duplicado; mostrar una lista vacía con su reproducción inactiva.
- Navegar Canciones ↔ Playlists manteniendo cola y minirreproductor; confirmar la ausencia de la pestaña Álbumes.
- Quitar una entrada y actualizar contenido/contador sin cambiar la cola que sonaba.
- Eliminar una lista con confirmación, sin tocar los archivos de música.
- Instalar la variante Benchmark conservando las listas creadas en Debug, abrirla y comprobar el control sin acción de una playlist vacía.
- Retirar las playlists temporales de validación y dejar la sección vacía, lista para las listas del usuario. La versión final instalada es `0.2.0-benchmark`.

Las capturas de validación muestran datos realmente guardados durante la prueba, no contenido estático del diseño adjunto. La aplicación no incluye las listas Favoritas/Modo Noche/Estudio de la referencia ni sus cifras ilustrativas.

## Integridad y límites

Sin cambios en MusicViewModel, Library, PlaybackService, ToneEngine ni ToneProcessor respecto al inicio de esta fase. NAS Reels permanece fuera del proyecto y sin escrituras. La barra inferior y el minirreproductor siguen siendo los existentes; el encabezado solo adapta sus textos al estar en Playlists.

No se mide rendimiento con miles de playlists, no se hace una auditoría integral de accesibilidad ni se comprueban todos los tamaños físicos. La adaptación compacta de la barra está implementada por ancho y escala de fuente; la comprobación visual principal usa las dimensiones reales del Moto g84. Tampoco se reevalúa la calidad auditiva o fidelidad de la salida, ya que el motor no se modifica.

Importación/exportación, backups manuales y reconciliación de URI tras mover o reindexar música siguen pendientes. Una pista perdida se conserva como referencia no disponible y no impide reproducir las demás.
