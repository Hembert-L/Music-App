# Sonido

> Esta descripción cubre Original y la mejora manual (ahora llamada Personalizado). El perfil actual Recomendado 0.7.2 aprobado en EJ-A6101 está en [AUDIO_BLUETOOTH.md](AUDIO_BLUETOOTH.md); la base de inicio estable, en [AUDIO_CLARIDAD.md](AUDIO_CLARIDAD.md). El historial está en [AUDIO_EQUILIBRADO.md](AUDIO_EQUILIBRADO.md) y [AUDIO_RECOMENDADO.md](AUDIO_RECOMENDADO.md). Las indicaciones de abajo de que no hay limitador corresponden a la ruta manual.

## Original

Modo inicial. ExoPlayer usa el renderer estándar con salida float habilitada cuando la ruta lo admite. Music no introduce procesador de tono, normalización, crossfade ni ganancia en esta ruta. No se fuerza desde Music una frecuencia de muestreo. El formato efectivo, el mezclador, el volumen del sistema, Bluetooth, el DAC y posibles efectos del fabricante dependen de Android. No se afirma una salida bit-perfect ni que todo hardware conserve la profundidad original.

## Mejora opcional

DSP propio en la cadena PCM de Media3, de 16 bits. No es restauración de información ni una mejora objetiva para toda música. Es un ajuste de balance con dos controles:

- Graves: 0 a +6 dB, banda suave mediante paso bajo de 180 Hz.
- Brillo: 0 a +6 dB, componente alta mediante complemento de paso bajo de 4.5 kHz.
- Nivel adicional: −12 a 0 dB. Nunca introduce ganancia positiva.

Para frecuencias de muestreo bajas, estos filtros simples cambian su respuesta efectiva; no se presentan como un ecualizador paramétrico de precisión. Los coeficientes se calculan según la frecuencia real del stream. El estado de cada canal es independiente y los ajustes se suavizan durante aproximadamente 15 ms.

El cambio Original ↔ Mejora reconstruye el reproductor con la ruta correspondiente, conservando la lista, la canción, la posición, la intención de reproducir, aleatorio y repetición. Hay una interrupción breve; no se promete cambio sin cortes. Cambiar los controles dentro de Mejora no reconstruye la ruta.

## Ganancia y límites

Sea `b = 10^(graves/20) − 1` y `a = 10^(brillo/20) − 1`. La señal es `x + b·LP180(x) + a·(x − LP4500(x))`. Los filtros son combinaciones convexas: si la entrada no supera escala completa, sus estados tampoco. Un límite conservador de magnitud es `1 + b + 2a`.

Se aplica `gain = 10^((−1 + nivel)/20) / (1 + b + 2a)`. Esto deja al menos 1 dB bajo escala completa antes de cuantizar. La compensación sigue los mismos valores instantáneos suavizados de los controles, evitando que un aumento repentino de tono adelante a la reducción de ganancia. Hay una cota final de conversión PCM como defensa numérica, no como compresor creativo. Se mantienen frecuencia y cantidad de canales.

El margen es deliberadamente conservador y puede bajar mucho el nivel con ambos controles al máximo. No compensar automáticamente esa reducción con una subida de volumen. No protege contra distorsión ya presente en el archivo, efectos posteriores de Android o la cadena analógica. No hay limitador true-peak ni medición de picos entre muestras en esta versión. No hay dither añadido al convertir la mejora a PCM16; evaluar dither y DSP float antes de ofrecer procesamiento de alta resolución.

## Siguientes etapas

1. Medir respuesta de frecuencia, cuantización, THD+N, picos entre muestras y coste de CPU con captura de la salida; comparar Original/Mejora a igual sonoridad.
2. DSP float y pruebas a 24 bits/96 kHz en hardware compatible, junto con información visible de la ruta efectiva.
3. EQ paramétrico con presets guardables y preganancia calculada para la suma de filtros. Añadir bypass y comparación nivelada.
4. Extender la nivelación propia por canción de Recomendado (0.7) a metadatos ReplayGain y otras rutas, sin modificar originales. Evaluar modos por colección solo si se solicitan.
5. Crossfade, transición sin huecos, limitador true-peak y perfiles por salida solo después de medir latencia, fase, techo y consumo.

Ninguna opción futura está expuesta como control ficticio en la app actual. Las pruebas numéricas no sustituyen una evaluación auditiva ni medición de la salida del teléfono.
