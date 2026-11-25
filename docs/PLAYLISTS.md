# Playlists · Music 0.2.0

> La revisión posterior del encabezado sustituye la fila permanente de búsqueda/creación por la lupa y el menú superiores, conservando las tarjetas y todas las operaciones descritas aquí. Véase [ENCABEZADO.md](ENCABEZADO.md).

## Cambio solicitado

Playlists ocupa la antigua pestaña Álbumes. Se conserva la composición aprobada de Canciones y Favoritos, la navegación inferior Biblioteca/Reproduciendo/Cola, el reproductor completo, sonido y minirreproductor compartido. No se añadieron Carpetas, accesos inferiores duplicados, listas de demostración ni canciones ficticias.

La referencia adjunta se aplica únicamente a esta sección: encabezado existente con textos Playlists/Organiza tu música; cápsula morado/azul; buscador y botón Nueva playlist en una misma fila; tarjetas compactas de 72 dp, portada representativa de la primera canción disponible, contador, botón degradado de 36 dp con área táctil de 48 dp y menú contextual. En pantallas anchas las acciones usan aproximadamente 65/35; por debajo de 420 dp o con fuente mayor de 130 %, el botón muestra solo + con su etiqueta accesible. La altura visible de las acciones es 40 dp, con superficie táctil de 48 dp.

## Funcionalidad

- Crear listas con nombre, con validación de nombres vacíos, longitud máxima de 60 y duplicados normalizados.
- Seleccionar varias canciones reales; la selección no inicia audio. Se añaden siguiendo el orden en que se seleccionaron, sin duplicar referencias ya presentes.
- Buscar playlists por nombre, sin modificar la cola. Las búsquedas no distinguen mayúsculas ni acentos.
- Abrir la tarjeta para ver su contenido. El botón circular reproduce la lista completa en su orden guardado; la acción aleatoria del detalle usa las reglas existentes de Music.
- Renombrar, añadir canciones, mover entradas arriba/abajo, quitar una entrada y eliminar la lista con confirmación.
- Listas vacías conservadas y representadas explícitamente; play desactivado cuando no hay canciones disponibles.
- Si una pista deja de estar en MediaStore, su referencia se conserva y se muestra como no disponible; se omite al reproducir. No se borra silenciosamente de la playlist.
- El resaltado identifica la última playlist iniciada desde esta sección cuando su secuencia de canciones disponibles coincide con la cola actual.
- Editar o eliminar una playlist no reconstruye ni detiene una cola que ya está reproduciéndose. El motor y los archivos de audio no se modifican.

## Persistencia

No existía un mecanismo de playlists en 0.1. Esta versión añade un módulo independiente, sin cambiar las preferencias de biblioteca, favoritos, cola ni sonido. `filesDir/playlists.json` usa esquema 1, UUID estable por playlist y por entrada, y referencias URI a canciones. `AtomicFile` protege la sustitución del archivo; operaciones serializadas mediante Mutex y E/S fuera del hilo principal. Un fallo de lectura impide sobreescribir el archivo hasta recuperarlo. Se usa un pequeño archivo de preferencias independiente para recordar la última playlist iniciada.

No se copia, mueve ni elimina música. La referencia por URI puede quedar inválida tras reindexar o mover archivos; la reconciliación avanzada y los backups manuales son etapas posteriores. Borrar datos o desinstalar Music elimina las listas privadas.

## Archivos y componentes

- Nuevos: `data/Playlist.kt`, `data/PlaylistStore.kt`, `PlaylistViewModel.kt`, `ui/PlaylistsScreen.kt`.
- Integración: `ui/LibraryScreen.kt` reemplaza Álbumes; `MusicHeader` admite textos de sección y `MusicSearchField` admite texto/altura compacta, conservando sus valores anteriores por defecto.
- Componentes reutilizados: MusicHeader, HeaderWaves, CategoryPill, MusicSearchField, MusicArtwork, MusicStyle y el MiniPlayer compartido. No hay otro reproductor ni nuevas dependencias.
- Pruebas nuevas: PlaylistTest y PlaylistDeviceChecks. El runner físico existente incorpora comprobaciones de persistencia y rollback con un archivo temporal aislado.
- Gradle: versión 0.2.0 y variante real `benchmark` no depurable, con la misma identidad y clave local que Music para conservar datos al instalar. `assembleBenchmark` compila una variante; no implica haber medido benchmarks de rendimiento. No es una firma de publicación en tienda.

## Límites

No se incluyen importación/exportación, playlists inteligentes, edición manual de portadas ni guardar la cola directamente como playlist. El almacenamiento no usa Room todavía. Estos límites no se presentan como botones ficticios en la interfaz.
