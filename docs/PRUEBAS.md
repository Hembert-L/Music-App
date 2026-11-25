# Pruebas

## JVM y análisis estático

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
```

En Linux/macOS usa `sh gradlew` en lugar de `.\gradlew.bat`. Los resultados se generan dentro de `app/build/`; están excluidos de Git.

Las pruebas JVM cubren búsqueda, playlists, títulos del Mini Player, saltos de pista, procesamiento de audio y nivelación. No necesitan música personal ni un teléfono.

## Integración en Android físico

Requiere un Android físico conectado a ADB, Music con permiso de audio y una biblioteca local real de al menos tres canciones. No se utiliza emulador. Estas comprobaciones interactúan con la app y algunas cambian temporalmente ajustes o la cola de reproducción.

Compila e instala las variantes de prueba:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
```

Abre Music, concede el permiso y prepara los archivos locales antes de ejecutar una comprobación. Ejemplo para la actualización con gesto:

```powershell
adb shell am instrument -w -r -e pullRefreshOnly true dev.aiko.music.test/dev.aiko.music.MusicInstrumentation
```

Opciones del ejecutor:

| Opción | Comprobación |
|---|---|
| `navigationOnly=true` | Estado y retorno a la biblioteca |
| `pagerOnly=true` | Gestos entre categorías |
| `nowPlayingOnly=true` | Reproductor completo |
| `nowPlayingLayoutOnly=true` | Distribución del reproductor |
| `seekOnly=true` | Interacción con el progreso |
| `trackControlsOnly=true` | Anterior/siguiente y pausa |
| `soundUiOnly=true` | Panel y controles de sonido |
| `pullRefreshOnly=true` | Actualización con gesto |
| `audioOnly=true` | Procesamiento y referencias locales |
| `bluetoothOnly=true` | Audio y controles con auriculares conectados |
| `screenOffPrepOnly=true` | Preparación de referencias para pantalla apagada |

Las comprobaciones de audio usan referencias reales específicas. Revisa `AudioQualityDeviceChecks.kt` y `ScreenOffReferenceChecks.kt` para conocer las canciones necesarias; los archivos musicales no se incluyen en el repositorio. Las comprobaciones de interfaz también pueden requerir títulos largos/cortos, una cola abierta o suficientes canciones para desplazarse.

## Persistencia entre procesos

`soundPersistencePhase` se ejecuta por etapas: `prepare`, `afterCustomRestart`, `afterRecommendedRestart`, `restore` y `finalVerify`. Consulta [la prueba de persistencia](PERSISTENCIA_PERSONALIZADO.md) antes de usarla. `prepare` guarda los ajustes originales y establece valores de prueba; `restore` recupera los valores personales y selecciona Recomendado. Debe ejecutarse también si se interrumpe la prueba.

El ejecutor devuelve `INSTRUMENTATION_CODE: -1` al completar una comprobación correctamente. Las medidas y la escucha confirmadas durante el desarrollo están en [VERIFICACION.md](VERIFICACION.md); no se presentan como pruebas de todos los modelos de teléfono o auriculares.
