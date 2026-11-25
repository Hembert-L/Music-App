package dev.aiko.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp

/** 52 dp toolbar + 48 dp accessible tabs. No additional search row. */
@Composable
fun CompactLibraryHeader(tab: Int, select: (Int) -> Unit, searching: Boolean, openSearch: () -> Unit,
    closeSearch: () -> Unit, query: String, change: (String) -> Unit,
    sound: () -> Unit, createPlaylist: () -> Unit, pagePosition: () -> Float) {
    var menu by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    BackHandler(searching) { closeSearch(); keyboard?.hide() }
    LaunchedEffect(searching) { if (searching) { focus.requestFocus(); keyboard?.show() } }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = if (searching) 4.dp else 16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (searching) {
                IconButton(onClick = { closeSearch(); keyboard?.hide() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Cerrar búsqueda") }
                BasicTextField(query, change, singleLine = true,
                    modifier = Modifier.weight(1f).height(48.dp).focusRequester(focus).semantics { contentDescription = if (tab == 2) "Buscar playlists" else "Buscar canciones" },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MusicStyle.Text), cursorBrush = MusicStyle.Accent,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    decorationBox = { inner -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text(if (tab == 2) "Buscar playlists..." else "Buscar canciones...", color = MusicStyle.Muted, style = MaterialTheme.typography.bodyMedium)
                        inner()
                    } })
                IconButton(onClick = { change("") }, enabled = query.isNotEmpty()) { Icon(Icons.Rounded.Close, "Borrar búsqueda") }
            } else {
                Box(Modifier.width(3.dp).height(24.dp).clip(CircleShape).background(MusicStyle.Accent))
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = MusicStyle.Text)) { append("Mu") }
                    withStyle(SpanStyle(color = MusicStyle.AccentText)) { append("sic") }
                }, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp).weight(1f))
                IconButton(onClick = openSearch) { Icon(Icons.Rounded.Search, "Abrir búsqueda", tint = MusicStyle.Text) }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Opciones de biblioteca", tint = MusicStyle.Text) }
                    DropdownMenu(menu, { menu = false }) {
                        if (tab == 2) DropdownMenuItem(text = { Text("Nueva playlist") }, onClick = { menu = false; createPlaylist() }, leadingIcon = { Icon(Icons.Rounded.Add, null) })
                        DropdownMenuItem(text = { Text("Sonido") }, onClick = { menu = false; sound() })
                    }
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(48.dp)) {
            val third = maxWidth / 3
            Row(Modifier.fillMaxSize()) {
                HeaderTab("Canciones", Icons.Rounded.MusicNote, tab == 0, { select(0) }, Modifier.weight(1f))
                HeaderTab("Favoritos", Icons.Rounded.FavoriteBorder, tab == 1, { select(1) }, Modifier.weight(1f))
                HeaderTab("Playlists", Icons.AutoMirrored.Rounded.PlaylistPlay, tab == 2, { select(2) }, Modifier.weight(1f))
            }
            Box(Modifier.align(Alignment.BottomStart).offset { IntOffset((third * pagePosition() + 12.dp).roundToPx(), 0) }
                .width(third - 24.dp).height(3.dp).clip(CircleShape).background(MusicStyle.Accent))
            HorizontalDivider(Modifier.align(Alignment.BottomCenter), thickness = .5.dp, color = MusicStyle.Divider.copy(alpha = .6f))
        }
    }
}

@Composable
private fun HeaderTab(label: String, icon: ImageVector, active: Boolean, click: () -> Unit, modifier: Modifier) {
    Row(modifier.fillMaxHeight().clickable(role = Role.Tab, onClick = click).semantics { selected = active },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = if (active) MusicStyle.Text else MusicStyle.Muted, modifier = Modifier.size(18.dp))
        Text(label, color = if (active) MusicStyle.Text else MusicStyle.Muted, fontSize = 13.sp, maxLines = 1,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(start = 6.dp))
    }
}
