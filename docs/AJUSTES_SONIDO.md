# Ajustes de sonido · Music 0.7.4

## Presentación

Panel inferior compacto con encabezado «Ajustes de sonido», icono dibujado con acentos morados/azules y subtítulo breve. Original, Recomendado y Personalizado usan filas de 52 dp, iconos de 24 dp, interruptores degradados y botones informativos con zonas de 48 dp. El modo activo se distingue con fondo y borde degradados.

El modo confirmado por el ViewModel es la única fuente de selección: siempre hay uno activo; pulsar su interruptor no lo deja apagado. No se añadieron tres booleanos independientes.

Original y Recomendado muestran únicamente las tres opciones. Elegir Personalizado despliega sus controles y adapta la altura, con una transición de tamaño de 220 ms. El contenido puede desplazarse si no cabe. Cambiar de modo conserva los valores personalizados.

Las ayudas son ventanas flotantes ancladas al icono, con posición ajustada al espacio de la ventana y las barras del sistema. Solo se abre una; se cierra mediante X, Atrás o una pulsación fuera, incluido el propio icono. No selecciona un modo ni cambia la altura del panel. La información de nivelación, volumen y protecciones está disponible en esas ayudas.

Los tres deslizadores conservan sus rangos continuos: **Graves 0–6 dB, Brillo 0–6 dB y Nivel adicional −12–0 dB**. Se guardan al terminar la interacción, como antes. El margen mostrado usa `SoundSettings.headroomDb` y se actualiza con el borrador real. Restablecer sigue aplicando exactamente `SoundSettings(enabled = true)`: Personalizado con 2/1/0 dB.

## Archivos y componentes

- `ui/SoundSettingsSheet.kt`: nuevo panel compartido, `SoundModeRow`, `SoundModeSwitch`, `SoundInfoButton` y `SoundAdjustment`. Componentes nativos de Compose, Canvas y Popup; sin recursos de demostración ni nuevas dependencias.
- `MainActivity.kt`: conecta el panel nuevo con el estado y la acción existentes. Configura altura adaptativa del ModalBottomSheet y color de contenido; retira los componentes antiguos de sonido.
- `ui/MusicTheme.kt`: añade exclusivamente tokens del panel de sonido. Conserva los estilos previos.
- `app/build.gradle.kts`: 0.7.4, código 15.
- `SoundSettingsDeviceChecks.kt` y `MusicInstrumentation.kt`: comprobación dedicada `soundUiOnly=true` en Android físico.
- README y registros de verificación: diseño, resultados y confirmación del usuario de los botones físicos de 0.7.3.

De los **24 archivos previos de producción**, únicamente cambiaron MainActivity y MusicTheme. Los otros 22 conservan SHA-256, incluidos **los nueve archivos de reproducción/audio**, MusicViewModel, escáner, playlists, Mini Player y Reproduciendo. No se modificó el motor Media3, el procesamiento, los perfiles, el cálculo del margen, la nivelación ni los controles multimedia.

## Verificación · 24 de noviembre de 2026

**Debug, assembleBenchmark, instrumentación, 32 tests JVM sin fallos y Lint con 0 errores/11 advertencias**. Se instaló 0.7.4-benchmark en el **Moto g84 5G físico con Android 15**, conservando datos.

La ejecución final de interfaz terminó con código **−1** y nueve grupos aprobados, incluido MediaStore con 370 canciones:

- Original compacto, exclusividad y pulsación del modo ya activo sin deselección.
- Ayudas independientes: X, Atrás y pulsación exterior; sin desplazamiento del encabezado.
- Recomendado compacto, expansión de Personalizado, alineación de los tres interruptores y zonas de 48 dp.
- Los tres deslizadores mediante accesibilidad, más una pulsación real sobre Graves. Se comprobó guardado de 4/3/−5 dB, actualización del margen y conservación al cambiar de modo.
- Restablecer devuelve los valores y el modo originales de esa acción.
- Las preferencias coinciden con el estado confirmado al cerrar y volver a abrir el panel.
- Contenido reutilizable limitado a **320 × 400 dp y fuente al 130 %**, en el mismo teléfono físico: los modos compactos caben; Personalizado se desplaza hasta el último control y Restablecer. No es una prueba en un segundo dispositivo ni emulador.
- Cola y volumen multimedia conservados (**10 → 10**); la reproducción de la cola existente pudo continuar durante la comprobación. Se restauraron los valores de sonido usados antes de la prueba.

La revisión visual observó capturas reales de Recomendado, Personalizado y ambas variantes estrechas. Se corrigió el contraste del encabezado durante la revisión. El lector de pruebas se ajustó para esperar el fin de la expansión y localizar el nodo nativo SeekBar que contiene la etiqueta; estos ajustes no cambiaron el audio.

Tras salir del entorno de prueba se volvió a abrir el panel real, se dejó **Recomendado seleccionado**, y se comprobó su interruptor activo con los otros dos apagados. Los valores personales siguen siendo 2/1/0 dB. No se realizó un nuevo ajuste de sonido ni una nueva evaluación auditiva.

Evidencia en `outputs/verificacion/ajustes-sonido`; capturas en `outputs/screenshots/ajustes-sonido`. NAS Reels intacto.

## Diferencias deliberadas frente a la imagen

- Solo aparece un modo activo a la vez, tal como exige el texto; la imagen ilustra dos interruptores encendidos.
- Se retiró la sección «Controles de auriculares» del panel según las indicaciones adjuntas, conservando toda su funcionalidad.
- La ayuda usa un globo rectangular redondeado sin punta decorativa; permanece anclado y no desplaza el contenido.
- En el caso estrecho con fuente al 130 %, los nombres largos usan puntos suspensivos; los interruptores conservan su nombre accesible completo. Todos los ajustes siguen disponibles por desplazamiento.
- Las posiciones y valores de los deslizadores corresponden a los ajustes reales, sin copiar posiciones ilustrativas de la referencia.

Referencia técnica: [PopupPositionProvider de Compose](https://developer.android.com/reference/kotlin/androidx/compose/ui/window/PopupPositionProvider) para colocar la ayuda respecto al icono y la ventana.
