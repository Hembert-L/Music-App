# Music

- Proyecto independiente en Aiko. No modificar NAS Reels ni compartir sus preferencias, identificadores o servicios.
- Kotlin, Compose y Media3. `dev.aiko.music` es la identidad propia de esta app.
- El diseño de biblioteca sigue `docs/UI_REFERENCIA.md`: tema oscuro y degradados morado/azul. Centralizar tokens en `ui/MusicTheme.kt`; reutilizar `ui/MusicComponents.kt`.
- Las renovaciones visuales no deben alterar motor, DSP, escaneo ni persistencia. No crear pestañas ficticias para funciones aún pendientes.
- Music 0.2 incorpora playlists persistentes autorizadas por el usuario, sustituyendo Álbumes. Su almacenamiento independiente está en `PlaylistStore`; conservar el motor, escaneo y preferencias anteriores. No volver a añadir Álbumes sin petición.
- Toda prueba Android debe ejecutarse en el Android físico conectado. No iniciar ni configurar emuladores.
- Original es el modo por defecto. Debe carecer de DSP propio; no prometer bit-perfect a través del mezclador Android.
- Music 0.3 añade Recomendado por petición del usuario: documentado en `docs/AUDIO_RECOMENDADO.md`. Mantener las tres rutas (Original, Recomendado, Personalizado), sus ajustes y las pruebas de picos/mono/estéreo. Los controles remotos se delegan a MediaSession; Android controla el volumen y el enlace Bluetooth.
- Music 0.7 mejora únicamente Recomendado con graves suaves y nivelación fija por canción antes del limitador: `docs/AUDIO_EQUILIBRADO.md`. El análisis local/caché es exclusivo de este modo; no aplicarlo a Original o Personalizado. Mantener cancelación, prioridad de fondo, invalidación por perfil/tamaño/fecha y límites de ganancia/picos.
- Music 0.7.1 corrige voz/graves e inicio: `docs/AUDIO_CLARIDAD.md`. No volver a aplicar resultados de análisis a una reproducción ya iniciada. Elegir medida válida o nivel prudente fijo al empezar; guardar nuevas medidas para una reproducción futura. Mantener el caso físico de arranque sin caché y ausencia de cambios tardíos de ganancia.
- Music 0.7.2 es la base de Recomendado aprobada por el usuario con EJ-A6101: `docs/AUDIO_BLUETOOTH.md`. El usuario no desea más ajustes de sonido por ahora. Conservar voz, grave, apertura y nivel estable de esta base en tareas ajenas al DSP.
- Music 0.7.3 unifica saltos explícitos en `TrackNavigationPlayer`: Anterior siempre cambia de pista; Anterior/Siguiente reproducen el destino desde cero aunque hubiera pausa. Conservar esta política en app y controles multimedia; el seek de progreso mantiene la pausa. Ver `docs/CONTROLES_PISTAS.md`.
- Music 0.7.4 presenta sonido mediante `ui/SoundSettingsSheet.kt`: un modo confirmado, ayudas flotantes y Personalizado desplegable. Mantener rangos, valores, persistencia y algoritmos existentes. Ver `docs/AJUSTES_SONIDO.md`.
- Music 0.7.5 conserva el último borrador de los controles al cambiar de modo/restablecer, sin reenviar valores confirmados antiguos. Mantener la prueba física por fases y reaperturas descrita en `docs/PERSISTENCIA_PERSONALIZADO.md`.
- Music 0.7.6 actualiza con gesto descendente al inicio mediante `LibraryRefreshBox`, sin opción redundante en el menú. Conservar el contenido durante la lectura y la separación entre rueda manual y lecturas automáticas. Respetar movimiento reducido con una rueda indeterminada estática. Ver `docs/ACTUALIZAR_CON_GESTO.md`.
- Cualquier cambio DSP necesita pruebas de límites, transiciones y separación de canales. No añadir ganancia positiva sin compensación demostrable.
- Registrar lo realmente comprobado en `docs/VERIFICACION.md`; diferenciar pruebas automáticas, observación visual y evaluación auditiva.
- Compilar con `gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug`.
