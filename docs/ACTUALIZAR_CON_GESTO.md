# Actualizar deslizando · Music 0.7.6

## Uso

En la biblioteca, vuelve al principio del contenido y tira hacia abajo. Aparece una rueda lavanda sobre una superficie oscura mientras se completa la actualización, y se retira al terminar. Un tirón corto vuelve a su posición sin actualizar. No hay avisos ni mensajes adicionales al completar la operación.

La rueda respeta la configuración de movimiento de Android. Si las animaciones están desactivadas, muestra un arco estático reconocible como carga indeterminada, sin porcentajes inventados. Se utiliza [ValueAnimator.areAnimatorsEnabled](https://developer.android.com/reference/android/animation/ValueAnimator) para elegir esa presentación; no se cambian los ajustes del teléfono.

El gesto está disponible en Canciones, Favoritos y Playlists, también con búsquedas o estados vacíos. En todos los casos vuelve a leer la música local; no edita playlists, favoritos ni la cola. La lista que ya estaba visible se conserva mientras se lee.

«Actualizar biblioteca» se retiró del menú de tres puntos de la cabecera compacta y de la cabecera anterior reutilizable. Sonido y Nueva playlist conservan sus accesos. Hay una acción accesible de actualización para lectores de pantalla, sin agregar un botón visual.

## Implementación

- `ui/LibraryRefreshBox.kt`: contenedor compartido con el desplazamiento anidado y el indicador nativos de Material 3 ya disponible en el proyecto. La lista recibe primero el movimiento; solo el desplazamiento que sobra al llegar arriba puede iniciar la actualización. No consume el gesto horizontal del pager.
- `ui/LibraryScreen.kt`: integra el contenedor alrededor de las tres páginas y conserva sus búsquedas, orden y desplazamiento. Evita duplicar el indicador central durante una actualización manual de una biblioteca vacía.
- `MusicViewModel.kt`: diferencia actualización manual de lecturas automáticas y rechaza solicitudes manuales duplicadas mientras ya hay una lectura activa. Usa la misma llamada existente `Library.read`. Un mínimo de **450 ms de feedback visual**, exclusivamente manual, permite percibir la rueda en lecturas muy rápidas. Las lecturas automáticas no añaden esa espera ni muestran esta rueda.
- `ui/CompactLibraryHeader.kt` y `ui/MusicComponents.kt`: retiran el elemento de menú y su callback innecesario.
- `ui/PlaylistsScreen.kt`: permite desplazamiento en sus estados vacíos para que el gesto llegue también desde ellos. No cambia almacenamiento ni comportamiento de playlists.
- `PullRefreshDeviceChecks.kt` y `MusicInstrumentation.kt`: comprobación física dedicada con `pullRefreshOnly=true`.
- Versión 0.7.6, código 17; sin dependencias nuevas.

El escáner MediaStore, LibraryBrowser, los nueve archivos de reproducción/audio, el panel de sonido, Mini Player y Reproduciendo conservan sus hashes. Los cambios de MusicViewModel pertenecen únicamente al estado y la entrada de actualización de biblioteca. Recomendado y la corrección de persistencia de 0.7.5 permanecen intactos.

Referencia técnica: [actualización mediante gesto en Compose](https://developer.android.com/develop/ui/compose/components/pull-to-refresh). Se utilizó la API disponible en Material 3 1.3.2, sin actualizar las dependencias.

## Verificación · 24 de noviembre de 2026

Debug, **assembleBenchmark**, instrumentación, **32 tests JVM sin fallos** y Lint (**0 errores, 11 advertencias**) completados. 0.7.6-benchmark instalada sin borrar datos en el **Moto g84 5G físico con Android 15**. No se utilizó emulador.

La ejecución final terminó con código **−1**, con 370 canciones reales y **seis actualizaciones manuales**:

- Menú: Sonido presente y Actualizar biblioteca ausente.
- Tirón corto desde arriba: se retrae y no lee de nuevo.
- Tirón completo en Canciones: una lectura real, rueda visible que desaparece y filas conservadas durante la actualización.
- Tirones sobre filas con reproducción pausada: no seleccionan una canción ni inician audio.
- Desplazamiento descendente a media lista: no actualiza antes de llegar al inicio.
- Navegación horizontal a Favoritos: no dispara actualización. Gesto de actualización correcto en Favoritos y Playlists.
- Búsqueda activa: actualiza conservando la consulta y el orden. También funciona con resultados vacíos.
- Acción accesible: inicia la misma actualización.
- La reproducción puede continuar durante la actualización. Volver de Reproduciendo no muestra una rueda de actualización manual.
- Cola, favoritos, archivo de playlists, ajustes de sonido y orden conservados; volumen multimedia **10 → 10**. Se recuperó el estado de reproducción/pausa encontrado antes de la prueba.

La rueda se capturó durante la operación con las canciones visibles. El teléfono tenía las tres escalas de animación del sistema en 0; la revisión visual detectó que el componente estándar quedaba casi imperceptible. Se añadió la rueda estática para ese caso y se repitió la comprobación, conservando dicha configuración. No se probó físicamente en esta tarea la variante animada con las animaciones activadas. El lector de prueba también se ajustó para distinguir la lista vertical del pager al preparar un desplazamiento; ese ajuste del lector no cambió la app.

No se probaron en esta tarea una revocación de permisos ni un fallo del proveedor MediaStore. Se conservan los accesos existentes de permiso y reintento. La lectura se basa en los archivos ya indexados por Android; no se agregó un escáner de archivos diferente.

Evidencia en `outputs/verificacion/actualizar-gesto`; capturas en `outputs/screenshots/actualizar-gesto`. NAS Reels intacto.
