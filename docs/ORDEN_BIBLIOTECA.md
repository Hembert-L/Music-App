# Orden de biblioteca · Music 0.3.1

Más recientes es el orden inicial cuando todavía no hay una preferencia guardada. El selector junto al contador abre tres opciones: Más recientes, Más antiguos y A–Z. La elección se guarda en preferencias privadas independientes (`library_ui/sort_order`) y se vuelve a leer al recrear la pantalla o el proceso. También se aplica al filtrar y a Favoritos.

La fecha utilizada es la de incorporación a MediaStore (`DATE_ADDED`), no la de lanzamiento de la canción ni la última modificación del archivo. Cuando dos pistas tienen la misma fecha se utiliza su ID para mantener un orden estable. Más antiguos invierte ese orden cronológico. A–Z conserva la clasificación por título de MediaStore.

Cambiar el orden no reemplaza la cola ya iniciada. Elegir una canción sigue creando la cola según el listado visible, como antes. No se modifican el escaneo, archivos, playlists, sonido ni minirreproductor.

Archivos modificados: `ui/LibraryScreen.kt` y versión en `app/build.gradle.kts`. Compilaciones Debug/Benchmark, tests existentes y Lint; validación física del orden y persistencia al detener/reabrir Music documentada en `verificacion/orden-interfaz.log`. Solo Android físico conectado, sin emulador.

Comprobado el 24 de noviembre de 2026 en Moto g84: inicio en Más recientes, comparación de la primera pista con la consulta real DATE_ADDED/ID descendente, selección de Más antiguos y conservación tras detener/reabrir el proceso, y selección de Más recientes con otra reapertura. Queda instalado y seleccionado Más recientes. Los 16 tests existentes pasan; Lint termina con 0 errores y 12 advertencias. No se añadieron pruebas que dupliquen el comparador: la comprobación de este cambio utiliza la biblioteca real y reinicios físicos de la app.
