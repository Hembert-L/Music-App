package dev.aiko.music.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@Composable
fun HeaderWaves(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Brush.radialGradient(listOf(MusicStyle.Purple.copy(alpha = .28f), Color.Transparent),
            center = Offset(size.width * .65f, 0f), radius = size.width * .65f))
        drawRect(Brush.radialGradient(listOf(MusicStyle.Blue.copy(alpha = .36f), Color.Transparent),
            center = Offset(size.width, size.height * .12f), radius = size.width * .65f))
        val wave = Path().apply {
            moveTo(size.width * .25f, size.height * .60f)
            cubicTo(size.width * .55f, size.height * .17f, size.width * .63f, size.height * .75f, size.width, size.height * .18f)
            lineTo(size.width, size.height * .50f)
            cubicTo(size.width * .65f, size.height * .30f, size.width * .63f, size.height * .83f, size.width * .25f, size.height * .60f)
            close()
        }
        drawPath(wave, Brush.horizontalGradient(listOf(Color.Transparent, MusicStyle.Purple.copy(alpha = .20f), MusicStyle.BrightBlue.copy(alpha = .22f))))
        val ribbon = Path().apply {
            moveTo(size.width * .52f, size.height * .72f)
            cubicTo(size.width * .82f, size.height * .55f, size.width * .71f, size.height * .12f, size.width, size.height * .10f)
            lineTo(size.width, size.height * .42f)
            cubicTo(size.width * .83f, size.height * .24f, size.width * .77f, size.height * .72f, size.width * .52f, size.height * .72f)
            close()
        }
        drawPath(ribbon, Brush.verticalGradient(listOf(MusicStyle.BrightBlue.copy(alpha = .23f), MusicStyle.Violet.copy(alpha = .04f))))
        drawRect(Brush.verticalGradient(0f to Color.Transparent, .60f to Color.Transparent, 1f to MusicStyle.Background))
    }
}

@Composable
fun GradientIconButton(icon: ImageVector, description: String, action: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, floating: Boolean = false, iconSize: Int = 25) {
    Box(modifier.size(if (floating) 56.dp else 48.dp)
        .then(if (floating) Modifier.shadow(6.dp, CircleShape) else Modifier)
        .clip(CircleShape).background(if (enabled) MusicStyle.Accent else Brush.horizontalGradient(listOf(MusicStyle.Surface, MusicStyle.Surface)))
        .border(1.dp, Color.White.copy(alpha = .14f), CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = action)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (enabled) Color.White else MusicStyle.Muted, modifier = Modifier.size(iconSize.dp))
    }
}

@Composable
fun MusicArtwork(uri: Uri?, title: String, modifier: Modifier = Modifier) {
    // AsyncImage uses Coil's shared ImageLoader and memory cache, with constraint-sized decoding.
    Box(modifier.clip(MusicStyle.ArtworkShape).background(MusicStyle.CardGradient)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.linearGradient(listOf(MusicStyle.Purple.copy(alpha = .32f), MusicStyle.Blue.copy(alpha = .22f))))
            val radius = size.minDimension * .31f
            drawCircle(Color.White.copy(alpha = .13f), radius)
            drawCircle(MusicStyle.Background.copy(alpha = .55f), radius * .8f)
            drawCircle(MusicStyle.AccentText, radius * .23f)
            drawCircle(MusicStyle.Background, radius * .07f)
        }
        AsyncImage(model = uri, contentDescription = "Carátula de $title", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun GradientProgress(progress: Float, modifier: Modifier = Modifier) {
    val value = progress.coerceIn(0f, 1f)
    Canvas(modifier.height(3.dp).clip(CircleShape).background(MusicStyle.Divider)
        .semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) }) {
        drawRect(MusicStyle.Accent, size = Size(size.width * value, size.height))
    }
}

@Composable
fun MusicBottomBar(player: Boolean, queue: Boolean, hasTrack: Boolean, library: () -> Unit, nowPlaying: () -> Unit, openQueue: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(MusicStyle.Background).padding(horizontal = MusicStyle.PageMargin, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val tabs = listOf(Triple("Biblioteca", Icons.Rounded.MusicNote, library),
            Triple("Reproduciendo", Icons.Rounded.PlayArrow, nowPlaying),
            Triple("Cola", Icons.AutoMirrored.Rounded.QueueMusic, openQueue))
        tabs.forEachIndexed { index, (label, icon, click) ->
            val selected = if (queue) index == 2 else if (player) index == 1 else index == 0
            val enabled = index != 1 || hasTrack
            Column(Modifier.weight(1f).height(64.dp).clip(MusicStyle.CardShape)
                .then(if (selected) Modifier.background(MusicStyle.SelectedGradient).border(1.dp, MusicStyle.Violet.copy(alpha = .34f), MusicStyle.CardShape) else Modifier)
                .clickable(enabled = enabled, role = Role.Tab, onClick = click)
                .semantics { this.selected = selected }, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = if (selected) MusicStyle.Text else MusicStyle.Muted.copy(alpha = if (enabled) 1f else .5f), modifier = Modifier.size(25.dp))
                Text(label, color = if (selected) MusicStyle.Text else MusicStyle.Muted.copy(alpha = if (enabled) 1f else .5f),
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
