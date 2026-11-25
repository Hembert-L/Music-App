# Music 0.5.1 · Controles más cómodos

Se amplían de forma moderada los controles de Reproduciendo y sus áreas táctiles, conservando su orden, degradados, estilo y comportamiento.

| Elemento | Antes | Ahora |
|---|---|---|
| Círculo play/pausa | 64 dp | 72 dp; 68 dp en distribución compacta |
| Icono play/pausa | 32 dp | 38 dp; 36 dp en compacto |
| Anterior/siguiente | 27 dp | 34 dp |
| Aleatorio/repetir | 27 dp | 30 dp |
| Área táctil secundaria | 48 dp | 56 dp; 52 dp en compacto |
| Favoritos | 48 dp, icono 25 dp | 52 dp, icono 28 dp |

El encabezado aumenta ligeramente sus iconos: bajar a 32 dp y sonido a 28 dp. Sus áreas de 48 dp conservan la altura compacta del encabezado. Se ajusta la altura de la fila musical a 88/80/68 dp según el espacio vertical disponible. En poco ancho o altura se usa la variante compacta para mantener el círculo completo y los cuatro controles secundarios visibles.

Archivos funcionales modificados: `ui/NowPlayingScreen.kt` y los tokens de `ui/MusicTheme.kt`. Se actualizan versión 0.5.1/código 8 y las expectativas geométricas de la prueba física existente. Biblioteca, playlists, Mini Player, ViewModel, motor y sonido conservan su implementación.

## Verificación de 0.5.1 · 24 de noviembre de 2026

- Compilaciones Debug y **assembleBenchmark**, APK de instrumentación, **20 tests JVM sin fallos** y Lint (**0 errores, 12 advertencias**) completados.
- **6 grupos físicos PASS** en el Moto g84 5G con Android 15 y 370 canciones reales: controles y modos de reproducción, favoritos, progreso, navegación, carátula estática y metadatos. Instrumentación finaliza con código −1.
- Comprobación geométrica e inspección de capturas en **1080×2400, 720×1280, 720×1440 con fuente 130 % y 1600×900** del mismo teléfono. En las tres ventanas verticales se midieron **72 dp de play y 56 dp de área secundaria**; en horizontal **68/52 dp**. Todos los botones permanecen completos, ordenados, sin solaparse y por encima de la navegación. Se restauraron tamaño, densidad y fuente originales.
- ViewModel, servicio, motores de audio, biblioteca, playlists, componentes compartidos y Mini Player conservaron sus hashes. Se limita el cambio visual a los controles del reproductor completo.
- **Music 0.5.1 Benchmark quedó instalada** (código 8). Se repitieron desde la interfaz real play/pausa, seek táctil y Biblioteca/Mini Player/Reproduciendo. Se mantuvo Recomendado y se dejó la pista pausada. Captura final: `screenshots/controles/benchmark.png`.

Evidencia: `verificacion/controles-compilacion.log`, `controles-android.log`, `controles-tamanos.log`, `controles-integridad.json` y carpeta `verificacion/controles`. Capturas: `screenshots/controles`. Solo se utilizó el Android físico conectado; ningún emulador.

# Historial · Music 0.5.0 · Reproduciendo

## Interfaz

Pantalla nativa Compose basada en la referencia: fondo oscuro con ondas y luz morada/azul estáticas, encabezado compacto centrado, carátula circular centrada con borde degradado fino y resplandor suave, título de hasta dos líneas, artista lavanda y favorito en una superficie oscura discreta. La imagen real se recorta con `CircleShape` y `ContentScale.Crop`, sin rotación, agujero, líneas interiores ni elementos superpuestos.

Se mantienen los cinco controles: aleatorio, anterior, play/pausa, siguiente y repetición. El principal tiene 64 dp y degradado; los secundarios usan iconos simples sin fondo, con objetivos táctiles de 48 dp. Los modos activos cambian de color. Solo se retira el acceso horizontal redundante a la cola; la navegación inferior compartida conserva Biblioteca, Reproduciendo y Cola.

La distribución mide los componentes reales antes de asignar espacio. La carátula busca el 76 % del ancho interior, con un máximo de 380 dp; se reduce si falta altura. El espacio libre se reparte proporcionalmente entre cinco separaciones, en lugar de acumularse debajo de los controles. La barra inferior permanece fuera de este cálculo, anclada en el mismo lugar. Las ventanas anchas y bajas usan carátula a la izquierda e información/controles a la derecha. En ventanas extremadamente cortas o texto grande puede ser necesario desplazarse dentro del contenido; la navegación inferior permanece visible.

El progreso usa el Slider accesible de Material3 con track nativo degradado, punta blanca y superficie táctil de 48 dp. Conserva el valor arrastrado mientras se confirma la búsqueda temporal, y suaviza las actualizaciones normales. Una sesión recuperada pausada usa la duración real de la biblioteca hasta disponer de la duración de Media3. La proyección de información excluye la posición, de modo que el avance actualiza solo el componente de progreso. Fondo y carátula no tienen animación continua.

## Alcance y diferencias

- Se conserva el diseño y el código de biblioteca, playlists, Mini Player, motor, DSP, escáner y almacenamiento.
- El subtítulo muestra el modo de sonido real (Original, Recomendado o Personalizado), como en la referencia; la sesión actual no guarda una etiqueta de procedencia de la cola.
- Se mantienen los apartados reales de navegación, incluida Cola; no se duplica Playlists abajo.
- El respaldo antiguo es un vinilo dibujado. Para cumplir la prohibición de discos y agujeros, esta pantalla adapta su paleta degradada y usa el icono musical de la app. Las otras pantallas conservan el respaldo anterior.
- No se usa la captura como recurso ni se sustituye la carátula real. Su nitidez depende de la resolución incrustada en cada archivo.
- La adaptación horizontal usa dos columnas para conservar controles y áreas táctiles, en lugar de estirar la composición vertical.
- En texto ampliado, las etiquetas de la navegación compartida usan elipsis si no caben; se conserva el texto accesible completo, las dimensiones, los colores y las rutas. Es el único ajuste de ese componente compartido.

## Archivos y componentes

- `ui/NowPlayingScreen.kt`: fondo, layout adaptable, carátula, metadatos, Slider y controles.
- `MainActivity.kt`: sustituye la presentación anterior y conecta las mismas acciones del ViewModel; progreso aislado en su ruta. No crea otra sesión.
- `ui/MusicTheme.kt`: nuevos tokens de margen, máximo de carátula y diámetro del botón principal; no cambia los tokens anteriores.
- `ui/MusicComponents.kt`: elipsis para etiquetas de navegación con fuente ampliada; los demás componentes permanecen iguales.
- `app/build.gradle.kts`: versión 0.5.0, código 7.
- `NowPlayingDeviceChecks.kt`: controles reales, geometría, comparación de píxeles de la carátula y capturas.
- `MusicInstrumentation.kt`, `MiniPlayerDeviceChecks.kt`, `LibraryNavigationDeviceChecks.kt`: integración de la prueba y actualización del marcador de pantalla al nuevo componente circular.

Se reutilizan la paleta, los degradados, la tipografía, `MusicBottomBar`, la caché compartida de Coil y las acciones existentes de Media3/ViewModel. No se añaden dependencias.

## Verificación · 24 de noviembre de 2026

En Aiko, se completó:

```powershell
.\gradlew.bat --offline :app:assembleDebug :app:assembleBenchmark :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
```

**BUILD SUCCESSFUL**, **20 tests JVM sin fallos ni errores**. Lint: **0 errores y 12 advertencias**, conservadas en el informe completo sin baseline.

En el **Moto g84 5G físico con Android 15**, biblioteca de **370 canciones reales**, la integración completa terminó con **27 grupos PASS** y `INSTRUMENTATION_CODE: -1`. Se comprobó:

- Carátula de la canción real circular y estática: comparación de píxeles idénticos en capturas separadas durante reproducción y pausa.
- Título y artista reales largos, cambio de información al avanzar/retroceder y respaldo de una pista real sin carátula. Inspección de sus capturas.
- Favorito activado y devuelto a su estado anterior.
- Seek de 60 s desde la acción accesible del Slider, reproducción/pausa, anterior/siguiente, aleatorio y los tres modos de repetición.
- Acceso a Sonido y a Cola desde la barra inferior; ausencia del botón horizontal redundante y del Mini Player en pantalla completa.
- Navegación y sincronización biblioteca/Mini Player/reproductor completo. La regresión de biblioteca conservó resultados y desplazamiento sin spinner ni reconexión en 45 pre-draws de ocho regresos; también pasaron búsqueda, orden, Favoritos y Playlists.
- Regresiones existentes de reproducción en segundo plano, controles multimedia Android, DSP y almacenamiento de playlists.

La geometría final se volvió a comprobar en **cuatro ventanas del mismo Android físico**, con capturas e instrumentación. Se verificaron carátula cuadrada recortada circularmente, botón principal de 64 dp completo, áreas de control de al menos 48 dp, orden y separación de botones, ausencia de superposiciones con la navegación y encabezado sin recorte vertical:

| Ventana en píxeles | Fuente | Resultado |
|---|---|---|
| 1080 × 2400 | 100 % | PASS |
| 720 × 1280 | 100 % | PASS |
| 720 × 1440 | 130 % | PASS |
| 1600 × 900 | 100 % | PASS, dos columnas |

Se restauraron **1080 × 2400 sin override de tamaño, densidad original de la sesión 340 y fuente 100 %**. No se usó emulador. En horizontal se ajustaron la tipografía y separación para evitar cortar el círculo principal; el criterio final exige que sus dos diámetros coincidan, además de comprobar el área táctil.

La **APK 0.5.0 Benchmark**, código 7, quedó instalada. Prueba final desde la interfaz real: play, pausa, seek mediante toque, regreso a Biblioteca/Mini Player y apertura de Reproduciendo. Se restauró el modo **Recomendado** observado al inicio y se dejó la pista pausada. Captura: `screenshots/reproduciendo/benchmark.png` junto a la entrega.

Los hashes de ViewModel, navegador de biblioteca, escáner, almacenamiento de playlists, servicio, motores/procesadores de audio, pantallas de biblioteca/playlists y Mini Player coinciden con el inicio del trabajo. Cola y paneles de sonido también se compararon con sus versiones anteriores y permanecen iguales. En `MusicComponents.kt` solo cambió el desbordamiento de las etiquetas de navegación a elipsis. No se escribieron archivos de NAS Reels.

Evidencia junto al APK: `verificacion/reproduciendo-compilacion.log`, `reproduciendo-android.log`, `reproduciendo-tamanos.log`, `reproduciendo-integridad.json` y carpeta `verificacion/reproduciendo` con tests JVM, Lint, accesibilidad, versión instalada y prueba Benchmark.

Límites: los tamaños probados son ventanas de un solo teléfono, no una auditoría de varios dispositivos o de todas las escalas de fuente. No se han medido FPS, consumo sostenido ni GPU, ni se ha realizado una nueva evaluación auditiva. La carátula puede verse poco nítida si su imagen original es pequeña. El texto largo se trunca con elipsis de forma controlada.
