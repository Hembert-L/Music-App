@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.aiko.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import dev.aiko.music.playback.SoundMode
import dev.aiko.music.ui.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// Existing player and sound panels inherit the shared palette without changing their behavior.
private val Ink = MusicStyle.Background
private val Panel = MusicStyle.Surface
private val Lime = MusicStyle.AccentText
private val Cream = MusicStyle.Text
private val Muted = MusicStyle.Muted

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MusicTheme { MusicApp() } }
    }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun MusicApp(vm: MusicViewModel = viewModel()) {
    var granted by remember { mutableStateOf(vm.hasPermission()) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it; asked = true; vm.refresh()
    }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { granted = vm.hasPermission(); vm.refresh() }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    val presenceFlow = remember(vm) { vm.playback.map { it.item != null }.distinctUntilChanged() }
    val hasTrack by presenceFlow.collectAsStateWithLifecycle(initialValue = false)
    var full by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf("") }
    val screenState = rememberSaveableStateHolder()
    BackHandler(full && sheet.isEmpty()) { full = false }
    Surface(Modifier.fillMaxSize(), color = Ink) {
        Box(Modifier.fillMaxSize()) {
            if (full && hasTrack) NowPlayingBackdrop(Modifier.fillMaxSize())
            else HeaderWaves(Modifier.fillMaxWidth().height(132.dp))
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
                Box(Modifier.weight(1f)) {
                    if (full && hasTrack) screenState.SaveableStateProvider("player") {
                        PlayerRoute(vm, { full = false }, { sheet = "sound" })
                    } else screenState.SaveableStateProvider("library") {
                        LibraryScreen(vm, granted, asked, { permission.launch(vm.permission) }, { sheet = "sound" }, Modifier.fillMaxSize())
                    }
                }
                if (!full && hasTrack) MiniPlayerRoute(vm, { full = true })
                MusicBottomBar(full && hasTrack, sheet == "queue", hasTrack, { full = false }, { full = true }, { sheet = "queue" })
            }
        }
    }
    if (sheet == "queue") ModalBottomSheet(onDismissRequest = { sheet = "" }, containerColor = Panel) { QueueRoute(vm) }
    if (sheet == "sound") ModalBottomSheet(onDismissRequest = { sheet = "" },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MusicStyle.SoundPanel, contentColor = MusicStyle.Text,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)) { SoundRoute(vm) }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun MiniPlayerRoute(vm: MusicViewModel, open: () -> Unit) {
    val p by vm.playback.collectAsStateWithLifecycle()
    MiniPlayer(p, open, vm::closePlayback, vm::previous, vm::togglePlay, vm::next, vm.miniTitleMotion,
        Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp))
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun PlayerRoute(vm: MusicViewModel, close: () -> Unit, openSound: () -> Unit) {
    val playback = vm.playback.collectAsStateWithLifecycle()
    val ui by remember(playback) { derivedStateOf(structuralEqualityPolicy()) {
        val p = playback.value
        NowPlayingUi(p.item, p.playing, p.loading, p.shuffle, p.repeat, p.error)
    } }
    val sound by vm.sound.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    NowPlayingScreen(ui, when (sound.mode) { SoundMode.RECOMMENDED -> "Sonido recomendado"; SoundMode.CUSTOM -> "Sonido personalizado"; else -> "Sonido original" },
        ui.item?.mediaId in favorites, close, openSound, { ui.item?.let { vm.favorite(it.mediaId) } },
        vm::shuffle, vm::previous, vm::togglePlay, vm::next, vm::repeat) { PlayerProgressRoute(vm) }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun PlayerProgressRoute(vm: MusicViewModel) {
    val p by vm.playback.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()
    val metadataDuration = remember(library.songs, p.item?.mediaId) {
        library.songs.firstOrNull { it.uri.toString() == p.item?.mediaId }?.duration ?: 0
    }
    key(p.item?.mediaId) { NowPlayingProgress(p.position, p.duration.takeIf { it > 0 } ?: metadataDuration, p.playing, vm::seek) }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun QueueRoute(vm: MusicViewModel) {
    val flow = remember(vm) { vm.playback.map { PlaybackUi(queue = it.queue, index = it.index) }.distinctUntilChanged() }
    val p by flow.collectAsStateWithLifecycle(initialValue = PlaybackUi())
    QueueSheet(p, vm)
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun SoundRoute(vm: MusicViewModel) {
    val sound by vm.sound.collectAsStateWithLifecycle()
    val flow = remember(vm) { vm.playback.map { it.connected }.distinctUntilChanged() }
    val connected by flow.collectAsStateWithLifecycle(initialValue = false)
    SoundSettingsSheet(sound, connected, vm::setSound)
}


@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun QueueSheet(p: PlaybackUi, vm: MusicViewModel) {
    Text("A continuación", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
    Text("${p.queue.size} canciones · toca una para escuchar", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
    val state = rememberLazyListState(initialFirstVisibleItemIndex = p.index.coerceAtLeast(0))
    LazyColumn(state = state, modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp).navigationBarsPadding(), contentPadding = PaddingValues(16.dp)) {
        items(p.queue.size) { index ->
            val item = p.queue[index]
            var menu by remember(index, item.mediaId) { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (index == p.index) Ink else Color.Transparent)
                .clickable { vm.selectQueue(index) }.padding(start = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (index == p.index) "♫" else "${index + 1}", color = Lime, modifier = Modifier.width(30.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.mediaMetadata.title?.toString() ?: "Sin título", color = if (index == p.index) Lime else Cream, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                    Text(item.mediaMetadata.artist?.toString() ?: "", color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                }
                Box {
                    Control(Icons.Rounded.MoreVert, "Editar posición ${index + 1}", { menu = true })
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Subir una posición") }, enabled = index > 0, onClick = { menu = false; vm.move(index, index - 1) })
                        DropdownMenuItem(text = { Text("Bajar una posición") }, enabled = index < p.queue.lastIndex, onClick = { menu = false; vm.move(index, index + 1) })
                        DropdownMenuItem(text = { Text("Quitar de la cola") }, onClick = { menu = false; vm.remove(index) })
                    }
                }
            }
        }
    }
    if (p.queue.isEmpty()) Text("Elige una canción en la biblioteca.", color = Muted, modifier = Modifier.padding(24.dp))
}

@Composable
private fun Control(icon: ImageVector, description: String, action: () -> Unit, tint: Color = Cream, size: Int = 24) {
    IconButton(onClick = action, modifier = Modifier.size(48.dp)) { Icon(icon, description, Modifier.size(size.dp), tint = tint) }
}
