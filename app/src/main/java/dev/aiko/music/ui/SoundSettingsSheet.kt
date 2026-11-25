package dev.aiko.music.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.aiko.music.playback.SoundMode
import dev.aiko.music.playback.SoundSettings

private enum class SoundInfo(val title: String, val explanation: String) {
    ORIGINAL("Original", "Sin ecualización, normalización ni ganancia añadida por Music. Android y tu dispositivo gestionan la salida final. Cambiar de modo puede causar una pausa breve."),
    RECOMMENDED("Recomendado", "Voz más clara, graves controlados y apertura sin ecos. Nivel fijo y protección de picos. Sin medida, empieza prudente y guarda el análisis para la próxima reproducción. El volumen sigue bajo tu control."),
    CUSTOM("Personalizado", "Ajusta graves, brillo y nivel adicional con margen automático para evitar saturación. No eleva el volumen por encima del margen seguro. Procesamiento PCM de 16 bits. Cambiar de modo puede causar una pausa breve."),
    HEADROOM("Margen automático", "Reduce el nivel para dejar espacio a los ajustes de tono y evitar saturación. No aumenta el volumen. El valor incluye el nivel adicional elegido.")
}

/** Settings are confirmed by the existing ViewModel; switches derive from that single mode. */
@Composable
fun SoundSettingsSheet(sound: SoundSettings, connected: Boolean, apply: (SoundSettings) -> Unit) {
    var draft by remember(sound) { mutableStateOf(sound) }
    var information by remember { mutableStateOf<SoundInfo?>(null) }
    val custom = sound.mode == SoundMode.CUSTOM
    fun choose(mode: SoundMode) {
        if (!connected || mode == sound.mode) return
        information = null
        apply(when (mode) {
            SoundMode.ORIGINAL -> draft.copy(enabled = false)
            SoundMode.RECOMMENDED -> draft.copy(enabled = true, recommended = true)
            SoundMode.CUSTOM -> draft.copy(enabled = true, recommended = false)
        })
    }
    Column(Modifier.fillMaxWidth().testTag("soundSettingsContent")
        .background(MusicStyle.SoundPanelGradient)
        .animateContentSize(tween(MusicStyle.SoundExpansionMillis))
        .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(MusicStyle.SoundIconGradient), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(28.dp)) {
                    val heights = listOf(.36f, .86f, .6f, .92f, .44f)
                    val colors = listOf(MusicStyle.Purple, MusicStyle.Violet, MusicStyle.AccentText, MusicStyle.Blue, MusicStyle.BrightBlue)
                    val bar = size.width / 8
                    heights.forEachIndexed { index, height ->
                        drawRoundRect(colors[index], Offset(index * size.width / 5, size.height * (1 - height) / 2),
                            Size(bar, size.height * height), CornerRadius(bar / 2))
                    }
                }
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("Ajustes de sonido", color = MusicStyle.Text, fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() })
                Text("Elige cómo quieres escuchar", fontSize = 13.sp, lineHeight = 18.sp, color = MusicStyle.Muted)
            }
        }
        @Composable fun infoButton(info: SoundInfo) {
            SoundInfoButton(info, information == info, { information = if (information == info) null else info }, { information = null })
        }
        SoundModeRow("Original", Icons.Rounded.Waves, sound.mode == SoundMode.ORIGINAL, connected,
            { choose(SoundMode.ORIGINAL) }, { infoButton(SoundInfo.ORIGINAL) })
        Spacer(Modifier.height(8.dp))
        SoundModeRow("Recomendado", Icons.Rounded.Headphones, sound.mode == SoundMode.RECOMMENDED, connected,
            { choose(SoundMode.RECOMMENDED) }, { infoButton(SoundInfo.RECOMMENDED) })
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(MusicStyle.SoundRowShape).background(MusicStyle.SoundRowBackground)
            .border(if (custom) 1.2.dp else 1.dp, if (custom) MusicStyle.Accent else Brush.linearGradient(listOf(MusicStyle.Divider, MusicStyle.Divider)), MusicStyle.SoundRowShape)) {
            SoundModeRow("Personalizado", Icons.Rounded.Tune, custom, connected,
                { choose(SoundMode.CUSTOM) }, { infoButton(SoundInfo.CUSTOM) }, framed = false)
            if (custom) {
                Column(Modifier.fillMaxWidth().testTag("soundCustomControls").padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 4.dp)) {
                    SoundAdjustment("Graves", draft.bass, 0f..6f, connected, { draft = draft.copy(bass = it) }, { apply(draft) })
                    SoundAdjustment("Brillo", draft.air, 0f..6f, connected, { draft = draft.copy(air = it) }, { apply(draft) })
                    SoundAdjustment("Nivel adicional", draft.trim, -12f..0f, connected, { draft = draft.copy(trim = it) }, { apply(draft) })
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MusicStyle.Secondary)
                        .border(1.dp, MusicStyle.Divider, RoundedCornerShape(12.dp)).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Margen automático: ${decibels(draft.headroomDb)}", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            color = MusicStyle.AccentText, modifier = Modifier.weight(1f).testTag("soundHeadroom"))
                        infoButton(SoundInfo.HEADROOM)
                    }
                    TextButton(onClick = { draft = SoundSettings(enabled = true); apply(draft) }, enabled = connected, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text("Restablecer mejora", fontSize = 13.sp, color = MusicStyle.AccentText)
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundModeRow(label: String, icon: ImageVector, selected: Boolean, enabled: Boolean, choose: () -> Unit,
    information: @Composable () -> Unit, framed: Boolean = true) {
    val shape = MusicStyle.SoundRowShape
    val frame = if (framed) Modifier.shadow(if (selected) 4.dp else 0.dp, shape, ambientColor = MusicStyle.Purple.copy(alpha = .15f), spotColor = MusicStyle.Blue.copy(alpha = .15f))
        .clip(shape).border(if (selected) 1.2.dp else 1.dp,
            if (selected) MusicStyle.Accent else Brush.linearGradient(listOf(MusicStyle.Divider, MusicStyle.Divider)), shape) else Modifier
    Row(Modifier.fillMaxWidth().then(frame).background(if (selected) MusicStyle.SoundSelectedGradient else MusicStyle.SoundInactiveGradient)
        .heightIn(min = MusicStyle.SoundRowHeight).padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = choose),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(24.dp), tint = MusicStyle.AccentText)
            Text(label, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = MusicStyle.Text,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 12.dp))
        }
        information()
        SoundModeSwitch(label, selected, enabled, choose)
    }
}

@Composable
private fun SoundModeSwitch(label: String, checked: Boolean, enabled: Boolean, select: () -> Unit) {
    val fraction by animateFloatAsState(if (checked) 1f else 0f, tween(150), label = "Sound switch")
    Canvas(Modifier.size(width = 52.dp, height = 48.dp).testTag("soundMode$label")
        .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = { if (!checked) select() })
        .semantics { contentDescription = "Modo $label" }) {
        val width = 44.dp.toPx(); val height = 24.dp.toPx()
        val start = Offset((size.width - width) / 2, (size.height - height) / 2)
        val off = Color(0xFF4B475E)
        drawRoundRect(Brush.horizontalGradient(listOf(lerp(off, MusicStyle.Purple, fraction), lerp(off, MusicStyle.Blue, fraction))), start, Size(width, height), CornerRadius(height / 2))
        drawCircle(MusicStyle.Text, 10.dp.toPx(), Offset(start.x + height / 2 + (width - height) * fraction, size.height / 2))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundAdjustment(label: String, value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean,
    change: (Float) -> Unit, finish: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = MusicStyle.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(decibels(value), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MusicStyle.AccentText)
    }
    Slider(value, change, enabled = enabled, onValueChangeFinished = finish, valueRange = range,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("soundSlider$label").semantics { contentDescription = label },
        thumb = { Box(Modifier.size(18.dp).clip(CircleShape).background(MusicStyle.AccentText).border(2.dp, MusicStyle.Text, CircleShape)) },
        track = {
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                drawRoundRect(MusicStyle.Divider, cornerRadius = CornerRadius(size.height / 2))
                val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
                drawRoundRect(Brush.horizontalGradient(listOf(MusicStyle.AccentText, MusicStyle.Violet)), size = Size(size.width * fraction, size.height), cornerRadius = CornerRadius(size.height / 2))
            }
        })
}

@Composable
private fun SoundInfoButton(info: SoundInfo, open: Boolean, toggle: () -> Unit, dismiss: () -> Unit) {
    val density = LocalDensity.current
    val popupWidth = minOf(288.dp, with(density) { LocalWindowInfo.current.containerSize.width.toDp() } - 24.dp)
    val margin = with(density) { 12.dp.roundToPx() }
    val top = WindowInsets.statusBars.getTop(density) + margin
    val bottom = WindowInsets.navigationBars.getBottom(density) + margin
    val position = remember(margin, top, bottom) { object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
            val x = (anchorBounds.right - popupContentSize.width).coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
            val above = anchorBounds.top - popupContentSize.height - margin
            val y = if (above >= top) above else anchorBounds.bottom + margin
            return IntOffset(x, y.coerceIn(top, (windowSize.height - popupContentSize.height - bottom).coerceAtLeast(top)))
        }
    } }
    Box {
        IconButton(onClick = toggle, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Outlined.Info, "Información de ${info.title}", Modifier.size(20.dp), tint = MusicStyle.Muted)
        }
        if (open) Popup(position, onDismissRequest = dismiss, properties = PopupProperties(focusable = true)) {
            Column(Modifier.widthIn(max = popupWidth)
                .shadow(8.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
                .background(MusicStyle.CardGradient).border(1.dp, MusicStyle.Accent, RoundedCornerShape(16.dp))
                .padding(start = 14.dp, end = 8.dp, bottom = 14.dp).testTag("soundInformation")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(info.title, color = MusicStyle.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        modifier = Modifier.weight(1f).semantics { heading() })
                    IconButton(onClick = dismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, "Cerrar información", Modifier.size(18.dp), tint = MusicStyle.Muted)
                    }
                }
                Text(info.explanation, fontSize = 13.sp, lineHeight = 18.sp, color = MusicStyle.Muted, modifier = Modifier.padding(end = 6.dp))
            }
        }
    }
}

private fun decibels(value: Float) = "%.1f dB".format(value)
