# Evolución por etapas

## Actualización 0.2 · Playlists (entregada)

Por petición del usuario se retira la navegación por Álbumes y se sustituye por Playlists. Se implementan creación, búsqueda, selección de canciones locales, reproducción en orden o aleatoria, renombrado, adición/retirada, orden manual y eliminación de listas sin borrar música. Se reutilizan el diseño y el motor existentes. Persistencia privada JSON versionada con AtomicFile; sin nueva dependencia. Detalles: [PLAYLISTS.md](PLAYLISTS.md).

Las etapas siguientes conservan la intención de evolución; importación/exportación, Room y guardado directo de la cola siguen pendientes. La referencia visual de Álbumes en 0.1 es histórica, no una sección vigente.

## 0.1 · Primera escucha local

Entregado: búsqueda por título/artista/álbum, canciones, agrupación por álbum, favoritos, recientes, actualización y carátulas. Minirreproductor y pantalla completa, cola editable, progreso, anterior/siguiente, aleatorio y repetir una/todas. Servicio Media3, notificación, audio focus y recuperación pausada. Original y mejora opcional con ganancia compensada.

## 0.2 · Biblioteca que escala

- Introducir Room y repositorios separados de la UI; índices de búsquedas y carga paginada para decenas de miles de canciones.
- Artistas, carpetas, discos, géneros, duración total, exclusiones y detección de duplicados sin borrar archivos.
- SAF para carpetas elegidas por el usuario, almacenamiento extraíble y permisos persistentes; mostrar ubicaciones perdidas y ofrecer reconexión.
- Identidad estable más allá del ID de MediaStore: volumen, URI, tamaño y metadatos para reconciliar reindexaciones. Favoritos actuales usan URI y pueden perder asociación al reindexar.
- Caché de carátulas y lectura embebida cuando MediaStore no ofrece imagen; accesibilidad, texto grande, paisaje y bibliotecas con metadatos incompletos.

Criterio de salida: búsqueda fluida con al menos 20.000 pistas, reindexación incremental, cero duplicados por refrescar, pruebas en almacenamiento externo y permiso revocado.

## 0.3 · Playlists propias

- Tablas Room: `Playlist(id, nombre, creada, modificada)` y `PlaylistEntry(id, playlistId, trackId, posicion)`; permitir una canción repetida con identidad propia de entrada.
- Crear, renombrar, guardar la cola como playlist, añadir/quitar y reordenar; eliminar una lista nunca elimina la música.
- Importar/exportar M3U8 con SAF, rutas portables y reporte de pistas no encontradas. Backups manuales locales.
- Últimas escuchadas y más reproducidas como vistas derivadas, con opción de borrar historial.

Criterio de salida: persistencia tras matar el proceso, importación/exportación circular, manejo de canciones movidas y migración desde favoritos 0.1.

## 0.4 · Sonido medido

Ver [SONIDO.md](SONIDO.md). Separar controles de tono, nivel y preferencias de salida. Empezar por mediciones y procesamiento float; luego EQ y normalización, con bypass siempre accesible y preganancia automática. Evitar presentar más volumen como mayor fidelidad.

Criterio de salida: respuesta y techo medidos, pruebas de transición, batería y salidas Bluetooth/cable/USB; comparación auditiva nivelada realizada por el usuario.

## 0.5 · Robustez y distribución

- Recuperación tras terminación del proceso y reinicio, integración MediaLibraryService para navegación de otros clientes si se decide soportarla.
- Instrumentación de interrupciones telefónicas, navegación, focus de otras apps, desconexión de auriculares y Bluetooth.
- Mejor soporte de accesibilidad, traducciones, tablet/paisaje y texto de 200 %.
- Actualizar AGP/SDK/Compose/Media3 con regresión física; firma release propia, versión, changelog y política de privacidad.

Una integración futura NAS debe ser un módulo explícito y opcional dentro de Music. No reutilizar ni modificar NAS Reels para construirla.
