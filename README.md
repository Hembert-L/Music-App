# Music

Reproductor de música local para Android, desarrollado con Kotlin, Jetpack Compose y Media3. Versión actual: **0.7.6**.

## Funciones

- Biblioteca con Canciones, Favoritos y Playlists; búsqueda por título, artista y álbum.
- Orden por recientes, antiguos o título, guardado entre sesiones. Actualización deslizando hacia abajo desde el inicio.
- Mini Player, reproductor completo con carátula circular y cola editable.
- Anterior/siguiente desde cero, reproducción aleatoria y repetición de una canción o de la cola.
- Reproducción en segundo plano, notificación y controles multimedia de auriculares Bluetooth.
- Modos Original, Recomendado y Personalizado, con preferencias persistentes y protección de ganancia.

La música y las preferencias permanecen en el dispositivo. No necesita cuenta ni servicio de streaming. Eliminar una playlist no elimina sus archivos musicales.

## Abrir y compilar

Requisitos: **JDK 17 o 21**, Android SDK 35 y acceso a los repositorios de dependencias durante la primera compilación. Android mínimo: **8.0 / API 26**.

Abre esta carpeta en Android Studio y selecciona tu SDK local. Android Studio puede generar `local.properties`; ese archivo pertenece a cada equipo y está excluido de Git. También se puede configurar el SDK mediante `ANDROID_HOME`.

Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleBenchmark
```

Linux/macOS:

```sh
sh gradlew :app:assembleDebug :app:assembleBenchmark
```

Los APK se generan en:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/benchmark/app-benchmark.apk`

Benchmark es una variante local no depurable que usa la firma de desarrollo. Las versiones de Gradle y las dependencias están fijadas en el proyecto; `--offline` puede usarse cuando ya estén descargadas.

## Comprobar el proyecto

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleBenchmark :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
```

Las pruebas JVM no necesitan un teléfono. Las pruebas de integración se ejecutan **exclusivamente en un Android físico conectado**, con archivos locales reales. Algunas pruebas de audio requieren las canciones utilizadas durante su calibración. Instrucciones y opciones: [Pruebas](docs/PRUEBAS.md).

## Estructura

```text
app/src/main/         Aplicación, recursos y manifiesto
app/src/test/         Pruebas JVM
app/src/androidTest/  Integración en Android físico
docs/                 Diseño, audio, decisiones y verificación
gradle/wrapper/       Gradle Wrapper
```

`MusicViewModel` conecta MediaStore y la sesión multimedia con la interfaz. `LibraryBrowser` mantiene el índice y los filtros en memoria. `PlaybackService` posee el ExoPlayer y mantiene la reproducción fuera de la Activity. Las playlists se guardan mediante `PlaylistStore` en el almacenamiento privado.

## Documentación

- [Índice de documentación](docs/README.md)
- [Sonido Original y Personalizado](docs/SONIDO.md)
- [Base aprobada de Recomendado](docs/AUDIO_BLUETOOTH.md)
- [Persistencia de ajustes](docs/PERSISTENCIA_PERSONALIZADO.md)
- [Verificación y límites de las pruebas](docs/VERIFICACION.md)
- [Evolución prevista](docs/ROADMAP.md)

## Contenido del repositorio

Esta carpeta contiene el código fuente, los recursos, el Wrapper, las pruebas y la documentación. `.gitignore` excluye compilaciones, cachés, APK, archivos temporales, configuración local y claves de firma. Los resultados generados se pueden recrear compilando el proyecto.
