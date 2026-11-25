# Persistencia de Personalizado · Music 0.7.5

## Resultado

En el Moto g84 5G físico con Android 15 se comprobaron valores distintos de los predeterminados: **Graves 4,0 dB, Brillo 3,0 dB y Nivel adicional −5,0 dB**. Se conservaron al cambiar de modo y al terminar completamente el proceso de Music y abrir de nuevo la aplicación.

La prueba encontró además un fallo al cambiar de modo inmediatamente después de terminar un ajuste: la selección copiaba los valores confirmados anteriormente, en lugar del último borrador que mostraba el panel. Esto podía sobrescribir el último valor mientras llegaba su confirmación. En 0.7.4 se observó al restaurar Nivel adicional a 0 dB y pasar inmediatamente a Recomendado: al volver a Personalizado figuraba −5 dB.

**0.7.5 corrige exclusivamente esa integración de interfaz.** La selección de modo conserva los valores del borrador actual. Restablecer actualiza el borrador antes de enviar los ajustes, para que un cambio inmediato de modo también use los valores restablecidos. El modo seleccionado continúa derivándose del estado confirmado.

Los nueve archivos de reproducción/audio y los otros archivos previos de producción mantienen sus hashes. De los 25 archivos previos de producción, solo cambió `ui/SoundSettingsSheet.kt` (cuatro líneas de comportamiento). No se modificaron Recomendado, el motor, la normalización, el cálculo de margen ni las protecciones. La versión pasa a 0.7.5, código 16.

## Prueba física

Se instaló Benchmark sin borrar datos. Las cinco fases dedicadas terminaron con código −1:

| Fase | Resultado |
|---|---|
| Preparar | Guardado de 4/3/−5 dB desde los deslizadores reales. |
| Reabrir en Personalizado | Modo y valores recuperados antes de cualquier edición. Personalizado → Original → Personalizado y Personalizado → Recomendado → Personalizado conservan los tres valores. |
| Reabrir en Recomendado | Recomendado seleccionado y controles personales ocultos; al volver a Personalizado aparecen 4/3/−5 dB. |
| Restaurar | Recuperados 2/1/0 dB originales y cambio inmediato a Recomendado, incluyendo el caso que había fallado. |
| Verificación final | Nueva reapertura con 2/1/0 dB guardados y Recomendado seleccionado. |

Hubo **tres cierres completos mediante Android**. Después de cada cierre se verificó ausencia del proceso, seguida de apertura normal de MainActivity; los PID observados fueron 27338, 27616 y 27936. También se comprobó la interfaz de las aperturas en Personalizado/Recomendado y, mediante instrumentación en procesos separados, las preferencias y el ViewModel antes de tocar los modos.

Los textos visibles, el estado confirmado y el almacenamiento coincidieron. En las reaperturas, los tres rangos nativos también informaron 4/3/−5 dB. El lector inicial de la prueba recibió temporalmente 0 en el nodo de progreso del nivel adicional mientras el texto ya mostraba −5; se ajustó el lector para verificar los valores visibles y confirmados. Esto se distinguió del fallo real de cambio rápido, cuya pérdida sí apareció en almacenamiento y en una reapertura posterior.

Se conservó la cola y el volumen multimedia **10 → 10**. Al finalizar se restauraron los valores originales **2/1/0 dB**, se dejó **Recomendado activo** y se volvió a abrir su panel real para confirmarlo. Se observó la captura de Personalizado tras reiniciar desde Recomendado. No se utilizó emulador ni se realizó una nueva valoración auditiva: el procesamiento aprobado se conserva por integridad de código.

## Archivos y comprobaciones

- `ui/SoundSettingsSheet.kt`: conserva el último borrador al cambiar de modo o restablecer.
- `SoundPersistenceDeviceChecks.kt` y `MusicInstrumentation.kt`: cinco fases independientes mediante `soundPersistencePhase`, conservando una copia de los valores originales para restauración.
- `app/build.gradle.kts`: versión 0.7.5/código 16.
- README, AGENTS y registros de verificación actualizados.

Debug, **assembleBenchmark**, instrumentación, **32 tests JVM sin fallos** y Lint (**0 errores, 11 advertencias**) completados. Evidencia en `outputs/verificacion/persistencia-personalizado`; capturas en `outputs/screenshots/persistencia-personalizado`. NAS Reels intacto.
