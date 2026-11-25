@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.aiko.music.ui

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlin.math.abs
import kotlin.math.min

private val progressColors = listOf(MusicStyle.Purple, MusicStyle.Violet, MusicStyle.Blue, MusicStyle.BrightBlue)

/** Position is deliberately excluded so artwork, metadata and controls do not tick with it. */
data class NowPlayingUi(val item: MediaItem?, val playing: Boolean, val loading: Boolean,
    val shuffle: Boolean, val repeat: Int, val error: String?)

@Composable
fun NowPlayingBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.drawWithCache {
        val purple = Brush.radialGradient(listOf(MusicStyle.Purple.copy(alpha = .20f), Color.Transparent),
            Offset(0f, size.height * .23f), size.width * .83f)
        val blue = Brush.radialGradient(listOf(MusicStyle.Blue.copy(alpha = .20f), Color.Transparent),
            Offset(size.width, size.height * .22f), size.width * .86f)
        val lower = Brush.radialGradient(listOf(MusicStyle.Violet.copy(alpha = .20f), Color.Transparent),
            Offset(size.width * .15f, size.height), size.width * .9f)
        fun wave(top: Float, rise: Float) = Path().apply {
            moveTo(0f, size.height * top)
            cubicTo(size.width * .27f, size.height * (top - rise), size.width * .42f, size.height * (top + rise), size.width * .66f, size.height * top)
            cubicTo(size.width * .84f, size.height * (top - rise), size.width * .92f, size.height * (top - rise), size.width, size.height * (top - rise * .6f))
            val band = .035f
            lineTo(size.width, size.height * (top - rise * .6f + band))
            cubicTo(size.width * .92f, size.height * (top - rise + band), size.width * .84f, size.height * (top - rise + band), size.width * .66f, size.height * (top + band))
            cubicTo(size.width * .42f, size.height * (top + rise + band), size.width * .27f, size.height * (top - rise + band), 0f, size.height * (top + band)); close()
        }
        val topWave = wave(.19f, .045f)
        val bottomWave = wave(.86f, .045f)
        val waveBrush = Brush.horizontalGradient(listOf(MusicStyle.Purple.copy(alpha = .16f), MusicStyle.Violet.copy(alpha = .035f), MusicStyle.BrightBlue.copy(alpha = .17f)))
        onDrawBehind {
            drawRect(MusicStyle.Background); drawRect(purple); drawRect(blue); drawRect(lower)
            drawPath(topWave, waveBrush); drawPath(bottomWave, waveBrush)
        }
    })
}

@Composable
fun NowPlayingScreen(ui: NowPlayingUi, subtitle: String, favorite: Boolean, close: () -> Unit,
    sound: () -> Unit, toggleFavorite: () -> Unit, shuffle: () -> Unit, previous: () -> Unit,
    toggle: () -> Unit, next: () -> Unit, repeat: () -> Unit, progress: @Composable () -> Unit) {
    val item = ui.item ?: return
    val title = item.mediaMetadata.title?.toString() ?: "Sin título"
    val artist = item.mediaMetadata.artist?.toString() ?: "Artista desconocido"
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fontScale = LocalDensity.current.fontScale
        val wide = maxWidth > maxHeight && maxWidth >= 600.dp
        val compact = maxHeight < 600.dp
        val headerHeight = maxOf(if (compact || wide) 52.dp else 60.dp, (43 * fontScale + 8).dp)
        val header: @Composable () -> Unit = { NowPlayingHeader(subtitle, close, sound, headerHeight) }
        val artwork: @Composable () -> Unit = { CircularMusicArtwork(item.mediaMetadata.artworkUri, title, Modifier.fillMaxSize()) }
        val metadata: @Composable () -> Unit = { NowPlayingMetadata(title, artist, favorite, toggleFavorite, compact, wide) }
        val controlHeight = if (wide) 68 else if (compact) 80 else 88
        val controls: @Composable () -> Unit = { NowPlayingControls(ui, shuffle, previous, toggle, next, repeat, controlHeight) }
        if (wide) {
            Column(Modifier.fillMaxSize()) {
                header()
                Row(Modifier.weight(1f).padding(horizontal = MusicStyle.NowPlayingMargin, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                    BoxWithConstraints(Modifier.weight(.45f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        CircularMusicArtwork(item.mediaMetadata.artworkUri, title, Modifier.size(minOf(maxWidth, maxHeight, MusicStyle.NowPlayingArtworkMax)))
                    }
                    Column(Modifier.weight(.55f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.SpaceEvenly) {
                        metadata(); progress(); controls()
                    }
                }
            }
        } else {
            // Only exceptional short windows/large text need scrolling; the bottom bar remains outside.
            val minimum = (headerHeight.value + ((if (compact) 54 else 58) + 40) * fontScale + 5 + 48 + 18 * fontScale + controlHeight + (if (compact) 32 else 56) + 80).dp
            val height = maxOf(maxHeight, minimum)
            val scroll = rememberScrollState()
            Box(Modifier.fillMaxSize().then(if (height > maxHeight) Modifier.verticalScroll(scroll) else Modifier)) {
                NowPlayingPortrait(Modifier.fillMaxWidth().height(height), compact, header, artwork, metadata, progress, controls)
            }
        }
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.BottomCenter).background(MusicStyle.Background).padding(12.dp)) }
    }
}

/** Measures the real text first, then allocates artwork and five controlled proportional gaps. */
@Composable
private fun NowPlayingPortrait(modifier: Modifier, compact: Boolean, header: @Composable () -> Unit, artwork: @Composable () -> Unit,
    metadata: @Composable () -> Unit, progress: @Composable () -> Unit, controls: @Composable () -> Unit) {
    Layout(content = { header(); artwork(); metadata(); progress(); controls() }, modifier = modifier) { children, constraints ->
        val w = constraints.maxWidth; val h = constraints.maxHeight
        val margin = MusicStyle.NowPlayingMargin.roundToPx()
        val contentWidth = (w - margin * 2).coerceAtLeast(0)
        val headerPlace = children[0].measure(Constraints.fixedWidth(w))
        val infoPlace = children[2].measure(Constraints.fixedWidth(contentWidth))
        val progressPlace = children[3].measure(Constraints.fixedWidth(contentWidth))
        val controlsPlace = children[4].measure(Constraints.fixedWidth(contentWidth))
        val fixed = headerPlace.height + infoPlace.height + progressPlace.height + controlsPlace.height
        val base = (if (compact) listOf(4.dp, 12.dp, 4.dp, 4.dp, 8.dp) else listOf(8.dp, 20.dp, 8.dp, 8.dp, 12.dp)).map { it.roundToPx() }
        val diameter = minOf((contentWidth * .76f).toInt(), MusicStyle.NowPlayingArtworkMax.roundToPx(), (h - fixed - base.sum()).coerceAtLeast(0))
        val cover = children[1].measure(Constraints.fixed(diameter, diameter))
        val free = (h - fixed - diameter - base.sum()).coerceAtLeast(0)
        val weights = listOf(.25f, .25f, .15f, .15f, .20f)
        val gaps = base.mapIndexed { i, value -> value + (free * weights[i]).toInt() }
        layout(w, h) {
            headerPlace.placeRelative(0, 0)
            var y = headerPlace.height + gaps[0]
            cover.placeRelative((w - diameter) / 2, y); y += diameter + gaps[1]
            infoPlace.placeRelative(margin, y); y += infoPlace.height + gaps[2]
            progressPlace.placeRelative(margin, y); y += progressPlace.height + gaps[3]
            controlsPlace.placeRelative(margin, y)
        }
    }
}

@Composable
private fun NowPlayingHeader(subtitle: String, close: () -> Unit, sound: () -> Unit, height: Dp) {
    Row(Modifier.fillMaxWidth().height(height).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        PlainPlayerControl(Icons.Rounded.KeyboardArrowDown, "Volver a la biblioteca", close, iconSize = 32.dp)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Reproduciendo", color = MusicStyle.Text, fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MusicStyle.AccentText, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        PlainPlayerControl(Icons.Rounded.Tune, "Opciones de sonido", sound, iconSize = 28.dp)
    }
}

@Composable
fun CircularMusicArtwork(uri: Uri?, title: String, modifier: Modifier = Modifier) {
    Box(modifier.semantics { contentDescription = "Carátula circular" }.drawWithCache {
        val r = size.minDimension / 2 - 2.dp.toPx()
        val haloRadius = r + 12.dp.toPx()
        val halo = Brush.radialGradient(0f to Color.Transparent, .82f to Color.Transparent,
            .90f to MusicStyle.Violet.copy(alpha = .18f), .95f to MusicStyle.Blue.copy(alpha = .07f), 1f to Color.Transparent,
            radius = haloRadius)
        onDrawBehind {
            drawCircle(halo, haloRadius)
            drawCircle(MusicStyle.Accent, r, alpha = .18f, style = Stroke(4.dp.toPx()))
        }
    }, contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().padding(3.dp).clip(CircleShape).background(MusicStyle.CardGradient), contentAlignment = Alignment.Center) {
            // Same palette and app music-note icon; no record/hole in the full player's fallback.
            Icon(Icons.Rounded.MusicNote, null, tint = MusicStyle.AccentText.copy(alpha = .65f), modifier = Modifier.fillMaxSize(.30f))
            AsyncImage(model = uri, contentDescription = "Carátula de $title", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Canvas(Modifier.matchParentSize()) {
            val r = size.minDimension / 2 - 2.dp.toPx()
            drawCircle(MusicStyle.Accent, r, style = Stroke(1.5.dp.toPx()))
            drawCircle(Color.White.copy(alpha = .28f), r - .6.dp.toPx(), style = Stroke(.5.dp.toPx()))
        }
    }
}

@Composable
private fun NowPlayingMetadata(title: String, artist: String, favorite: Boolean, toggle: () -> Unit, compact: Boolean, wide: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = MusicStyle.Text, fontSize = if (wide) 20.sp else if (compact) 21.sp else 23.sp,
                lineHeight = if (wide) 24.sp else if (compact) 27.sp else 29.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(artist, color = MusicStyle.Muted, fontSize = 14.sp, lineHeight = if (wide) 18.sp else 20.sp, maxLines = if (wide) 1 else 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = if (wide) 4.dp else 5.dp))
        }
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(MusicStyle.Surface.copy(alpha = .65f))
            .clickable(role = Role.Button, onClick = toggle).semantics { selected = favorite }, contentAlignment = Alignment.Center) {
            Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (favorite) "Quitar de favoritos" else "Añadir a favoritos", tint = if (favorite) MusicStyle.AccentText else MusicStyle.Text,
                modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun NowPlayingProgress(position: Long, duration: Long, playing: Boolean, seek: (Long) -> Unit) {
    var drag by remember { mutableStateOf<Float?>(null) }
    var pending by remember { mutableStateOf<Float?>(null) }
    val interactions = remember { MutableInteractionSource() }
    val originalConfiguration = LocalViewConfiguration.current
    val seekConfiguration = remember(originalConfiguration) {
        object : ViewConfiguration by originalConfiguration {
            // Seeking maps the final finger X to time; discarding the initial drag delta offsets it.
            // This immediate response is scoped to the slider, separate from library paging.
            override val touchSlop = 0f
        }
    }
    LaunchedEffect(interactions) {
        interactions.interactions.collect { if (it is DragInteraction.Cancel) drag = null }
    }
    LaunchedEffect(position, pending) { if (pending != null && abs(position - pending!!.toLong()) < 1200) pending = null }
    LaunchedEffect(pending) { if (pending != null) { delay(1500); pending = null } }
    val maximum = duration.coerceAtLeast(1).toFloat()
    val target = if (duration > 0) (drag ?: pending ?: position.toFloat()).coerceIn(0f, maximum) else 0f
    val displayed by animateFloatAsState(target, if (drag != null || pending != null || !playing) snap() else tween(380, easing = LinearEasing), label = "Progreso musical")
    Column(Modifier.fillMaxWidth()) {
      CompositionLocalProvider(LocalViewConfiguration provides seekConfiguration) {
        Slider(value = displayed, onValueChange = { drag = it }, onValueChangeFinished = {
            drag?.let { pending = it; seek(it.toLong()) }; drag = null
        }, enabled = duration > 0, valueRange = 0f..maximum,
            modifier = Modifier.fillMaxWidth().height(MusicStyle.NowPlayingSeekTouch).semantics { contentDescription = "Progreso de reproducción" },
            interactionSource = interactions,
            thumb = { Box(Modifier.width(MusicStyle.NowPlayingSeekThumb).height(MusicStyle.NowPlayingSeekTouch)
                .testTag("playerSeekThumb").drawWithCache {
                onDrawBehind {
                    drawCircle(MusicStyle.AccentText.copy(alpha = .10f), radius = size.width * .85f)
                    drawCircle(MusicStyle.Text, radius = size.width / 2)
                }
            }) },
            track = { state -> Canvas(Modifier.fillMaxWidth().height(MusicStyle.NowPlayingSeekTrack).testTag("playerSeekTrack")) {
                val y = size.height / 2
                drawLine(MusicStyle.Divider, Offset(0f, y), Offset(size.width, y), strokeWidth = size.height, cap = StrokeCap.Round)
                val fraction = (state.value / maximum).coerceIn(0f, 1f)
                val activeWidth = size.width * fraction
                if (fraction > 0) drawLine(Brush.horizontalGradient(progressColors, endX = activeWidth.coerceAtLeast(1f)), Offset(0f, y), Offset(activeWidth, y), strokeWidth = size.height, cap = StrokeCap.Round)
            } })
      }
        Row(Modifier.fillMaxWidth()) {
            Text(playerTime(target.toLong()), color = MusicStyle.Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
            Text(playerTime(duration), color = MusicStyle.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun NowPlayingControls(ui: NowPlayingUi, shuffle: () -> Unit, previous: () -> Unit,
    toggle: () -> Unit, next: () -> Unit, repeat: () -> Unit, height: Int) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(height.dp)) {
      val regularWidth = MusicStyle.NowPlayingControlTarget * 4 + MusicStyle.NowPlayingPlaySize
      val compact = maxWidth < regularWidth || maxHeight < MusicStyle.NowPlayingPlaySize
      val target = if (compact) MusicStyle.NowPlayingCompactControlTarget else MusicStyle.NowPlayingControlTarget
      val playSize = if (compact) MusicStyle.NowPlayingCompactPlaySize else MusicStyle.NowPlayingPlaySize
      Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        PlainPlayerControl(Icons.Rounded.Shuffle, if (ui.shuffle) "Desactivar aleatorio" else "Activar aleatorio", shuffle, ui.shuffle,
            iconSize = MusicStyle.NowPlayingModeIcon, target = target)
        PlainPlayerControl(Icons.Rounded.SkipPrevious, "Canción anterior", previous,
            iconSize = MusicStyle.NowPlayingTransportIcon, target = target)
        Box(Modifier.size(playSize).drawWithCache {
            val glow = Brush.radialGradient(listOf(MusicStyle.Violet.copy(alpha = .2f), Color.Transparent), radius = size.width * .72f)
            onDrawBehind { drawCircle(glow, radius = size.width * .72f) }
        }.clip(CircleShape).background(MusicStyle.Accent).border(1.dp, Color.White.copy(alpha = .12f), CircleShape)
            .clickable(role = Role.Button, onClick = toggle).semantics { contentDescription = if (ui.playing) "Pausar" else "Reproducir" }, contentAlignment = Alignment.Center) {
            if (ui.loading) CircularProgressIndicator(Modifier.size(24.dp), color = MusicStyle.Text, strokeWidth = 2.dp)
            else Icon(if (ui.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = MusicStyle.Text,
                modifier = Modifier.size(if (compact) 36.dp else MusicStyle.NowPlayingPlayIcon))
        }
        PlainPlayerControl(Icons.Rounded.SkipNext, "Canción siguiente", next,
            iconSize = MusicStyle.NowPlayingTransportIcon, target = target)
        PlainPlayerControl(if (ui.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            "Repetir: ${when (ui.repeat) { Player.REPEAT_MODE_ONE -> "una canción"; Player.REPEAT_MODE_ALL -> "toda la cola"; else -> "desactivado" }}", repeat, ui.repeat != Player.REPEAT_MODE_OFF,
            iconSize = MusicStyle.NowPlayingModeIcon, target = target)
      }
    }
}

@Composable
private fun PlainPlayerControl(icon: ImageVector, label: String, click: () -> Unit, active: Boolean = false,
    iconSize: Dp = 30.dp, target: Dp = MusicStyle.TouchTarget) {
    IconButton(onClick = click, modifier = Modifier.size(target).semantics { selected = active }) {
        Icon(icon, label, tint = if (active) MusicStyle.AccentText else MusicStyle.Text, modifier = Modifier.size(iconSize))
    }
}

private fun playerTime(ms: Long) = "%d:%02d".format(ms.coerceAtLeast(0) / 60000, ms.coerceAtLeast(0) / 1000 % 60)
