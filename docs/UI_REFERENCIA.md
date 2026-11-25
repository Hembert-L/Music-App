# Interfaz de biblioteca · referencia morado/azul

> Registro histórico de la revisión 0.1. En 0.2, por petición del usuario, Playlists sustituye a Álbumes. El resto de este diseño se conserva. Estado actual y alcance: [PLAYLISTS.md](PLAYLISTS.md).

## Alcance

Renovación de la capa de presentación solicitada el 23 de noviembre de 2026. Se inspeccionó la implementación: una Activity Compose, MusicViewModel con flujos de biblioteca/reproducción/favoritos/sonido y un servicio Media3 independiente. La biblioteca anterior y sus componentes visuales estaban dentro de MainActivity. No había navegación inferior ni carpetas o playlists guardadas.

El motor, DSP, MusicViewModel, permisos, consulta de MediaStore, persistencia y dependencias se mantienen sin cambios en esta revisión. La pantalla completa de reproducción ya existía: se reutiliza, hereda la paleta común y sigue accesible; no se construyó otra. Los hashes de los cinco archivos funcionales se compararon antes y después del rediseño.

## Composición implementada

- Fondo `#0D0D1C`; superficies `#151426` y `#201D38`; textos `#F5F4FC` y `#B8B6CE`; divisores `#302D43`.
- Degradado nativo `#A132F5 → #7037E8 → #3075F5 → #258FFF` en icono, categoría activa, aleatorio y botón del minirreproductor. La barra activa inferior usa una variación oscura.
- Encabezado de 100 dp con icono de 50 dp, Music a 26 sp y subtítulo de 14 sp. Las ondas se dibujan con Canvas y curvas Bézier y se funden al fondo. No se usa la imagen de referencia como fondo ni ningún dato de demostración.
- Búsqueda de 48 dp, con lupa, limpieza e IME Search. Filtra título, artista y álbum conservando la búsqueda anterior de la aplicación. Mensaje específico sin coincidencias.
- Cápsulas distribuidas uniformemente, 48 dp de altura. Filas de al menos 66 dp, carátulas de 50 dp, título de 16 sp, artista/duración de 13 sp; separadores desde el texto, sin tarjetas grandes. La pista activa usa una tinta morada discreta.
- Botón de aleatorio de 56 dp sobre la lista; 88 dp de espacio desplazable al final permite sacar las últimas canciones de debajo del botón.
- Minirreproductor reutilizable de 80 dp sobre la navegación: carátula, título, artista, progreso degradado de 3 dp, play/pausa y siguiente. Su cuerpo abre el reproductor completo.
- Navegación inferior con tres accesos reales; estado seleccionado redondeado. Insets del sistema y teclado respetados. Si no hay pista, se conserva la ausencia del minirreproductor y Reproduciendo queda inactivo.

## Rendimiento y estado

LazyColumn utiliza claves por ID y tipo de contenido. La búsqueda genera una vez por escaneo un índice normalizado en memoria; normalización, filtro y orden se ejecutan en Dispatchers.Default. Se agrupan escrituras rápidas con una espera cancelable de 100 ms. No hay nueva base de datos ni cambios al escaneo o almacenamiento.

La biblioteca recoge únicamente conexión, canción activa y error mediante `distinctUntilChanged`. El progreso de 400 ms se observa dentro del minirreproductor, no en el contenedor de biblioteca ni en sus filas. La cola observa solo lista e índice. SaveableStateHolder conserva búsqueda, categoría, orden y scroll al alternar Biblioteca/Reproduciendo. No se reconstruye la cola por escribir, cambiar pestaña o navegar: solo al elegir una pista o pulsar aleatorio se llaman las acciones de reproducción existentes.

AsyncImage usa el ImageLoader compartido y la caché de memoria de Coil, con decodificación según restricciones de tamaño. Las URI de carátula por canción y su mecanismo de lectura permanecen iguales. No se añadieron bibliotecas ni procesos de extracción de imágenes.

## Diferencias respecto a la imagen

1. Se conserva el nombre actual **Music**, en lugar de “Nya Music”. No se autorizó un cambio de nombre.
2. **Favoritos** ocupa la posición de Carpetas, porque ya existía y no hay navegación por carpetas implementada. Álbumes sigue funcionando con datos reales.
3. **Cola** ocupa el tercer acceso inferior, en lugar de Playlists. Abre la cola editable existente; no simula playlists guardadas ni añade esa funcionalidad fuera de fase.
4. Se conserva el contador y el selector A–Z/Recientes. Actualizar biblioteca y Sonido se agrupan en el menú superior para mantener limpio el encabezado.
5. Las canciones, artistas, duraciones y carátulas son los del dispositivo. Pueden faltar metadatos, tener títulos largos o imágenes diferentes de las ilustraciones de referencia. Se usan elipsis y un respaldo gráfico cuando no hay carátula.
6. Las barras y proporciones finales se adaptan al Moto g84 real; en este equipo está activa la navegación Android de tres botones. No se añade el marco de teléfono ni se fuerza una hora o batería ficticias.

## Archivos de la revisión

- Modificado `app/src/main/java/dev/aiko/music/MainActivity.kt`.
- Creados `ui/MusicTheme.kt`, `ui/MusicComponents.kt` y `ui/LibraryScreen.kt` bajo el mismo paquete.
- Actualizados `app/src/main/res/values/styles.xml` e `app/src/main/res/drawable/ic_music.xml`.
- Actualizados README y documentación de diseño/verificación. Sin nuevas dependencias.

La evaluación física y sus límites están en [VERIFICACION.md](VERIFICACION.md). No se afirma una validación de rendimiento con decenas de miles de pistas ni una auditoría completa de accesibilidad: siguen pendientes para bibliotecas mayores y otros tamaños físicos.
