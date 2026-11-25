package dev.aiko.music.ui

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aiko.music.MiniTitleMotion
import dev.aiko.music.PlaybackUi

@Composable
fun MiniPlayer(p: PlaybackUi, open: () -> Unit, close: () -> Unit, previous: () -> Unit,
    toggle: () -> Unit, next: () -> Unit, motion: MiniTitleMotion, modifier: Modifier = Modifier) {
    val item = p.item ?: return
    val title = item.mediaMetadata.title?.toString() ?: "Sin título"
    Row(modifier.fillMaxWidth().height(MusicStyle.MiniPlayerHeight).clip(MusicStyle.CardShape)
        .background(MusicStyle.CardGradient).border(1.dp, MusicStyle.Divider, MusicStyle.CardShape)
        .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        MusicArtwork(item.mediaMetadata.artworkUri, title, Modifier.size(44.dp).clickable(role = Role.Button, onClick = open))
        Column(Modifier.weight(1f).padding(start = 8.dp, end = 4.dp).clickable(role = Role.Button, onClick = open)) {
            OnePassTitle(title, p.titleToken, motion)
            Text(item.mediaMetadata.artist?.toString() ?: "Artista desconocido", color = MusicStyle.Muted,
                fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            GradientProgress(if (p.duration > 0) p.position.toFloat() / p.duration else 0f,
                Modifier.fillMaxWidth().padding(top = 3.dp).height(2.dp))
        }
        IconButton(onClick = close, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Close, "Cerrar reproducción", tint = MusicStyle.Muted, modifier = Modifier.size(20.dp)) }
        IconButton(onClick = previous, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.SkipPrevious, "Canción anterior", tint = MusicStyle.Text, modifier = Modifier.size(22.dp)) }
        Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = toggle)
            .semantics { contentDescription = if (p.playing) "Pausar" else "Reproducir" }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(MusicStyle.Accent)
                .border(1.dp, Color.White.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (p.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = MusicStyle.Text, modifier = Modifier.size(24.dp))
            }
        }
        IconButton(onClick = next, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.SkipNext, "Siguiente canción", tint = MusicStyle.Text, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun OnePassTitle(title: String, token: Long, motion: MiniTitleMotion) {
    val density = LocalDensity.current.density
    val measurer = rememberTextMeasurer()
    val style = TextStyle(color = MusicStyle.Text, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
    val layout = remember(title, measurer, style) { measurer.measure(AnnotatedString(title), style, softWrap = false, maxLines = 1) }
    BoxWithConstraints(Modifier.fillMaxWidth().height(20.dp).clipToBounds().clearAndSetSemantics { text = AnnotatedString(title) }) {
        val overflow = (layout.size.width - constraints.maxWidth).toFloat().coerceAtLeast(0f)
        val run = remember(token, title, overflow) { motion.claim(token, overflow, density, SystemClock.elapsedRealtime()) }
        var finished by remember(run) { mutableStateOf(run == null || run.finished(SystemClock.elapsedRealtime())) }
        val offset = remember(run) { mutableFloatStateOf(run?.offset(SystemClock.elapsedRealtime()) ?: 0f) }
        LaunchedEffect(run) {
            if (run != null) {
                while (!run.finished(SystemClock.elapsedRealtime())) {
                    withFrameMillis { offset.floatValue = run.offset(SystemClock.elapsedRealtime()) }
                }
                offset.floatValue = 0f
                finished = true
            }
        }
        if (finished) Text(title, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
        else Canvas(Modifier.fillMaxSize()) { drawText(layout, topLeft = Offset(-offset.floatValue.coerceAtMost(overflow), 0f)) }
    }
}
