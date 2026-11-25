package dev.aiko.music.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared visual tokens. Gradients are drawn natively; no bitmap background. */
object MusicStyle {
    val Background = Color(0xFF0D0D1C)
    val Secondary = Color(0xFF151426)
    val Surface = Color(0xFF201D38)
    val Purple = Color(0xFFA132F5)
    val Violet = Color(0xFF7037E8)
    val Blue = Color(0xFF3075F5)
    val BrightBlue = Color(0xFF258FFF)
    val Text = Color(0xFFF5F4FC)
    val Muted = Color(0xFFB8B6CE)
    val Divider = Color(0xFF302D43)
    val AccentText = Color(0xFFCDA8FF)
    val Accent = Brush.horizontalGradient(listOf(Purple, Violet, Blue, BrightBlue))
    val CardGradient = Brush.linearGradient(listOf(Surface, Secondary))
    val SelectedGradient = Brush.linearGradient(listOf(Color(0xFF302052), Color(0xFF211C42)))
    val SoundPanel = Color(0xFF1C1830)
    val SoundIconGradient = Brush.linearGradient(listOf(Color(0xFF362551), Color(0xFF203E60)))
    val SoundPanelGradient = Brush.verticalGradient(listOf(SoundPanel, Secondary))
    val SoundRowBackground = Color(0xFF19162B)
    val SoundInactiveGradient = Brush.horizontalGradient(listOf(Color(0xFF242039), Color(0xFF211E35)))
    val SoundSelectedGradient = Brush.horizontalGradient(listOf(Color(0xFF582475), Color(0xFF322363), Color(0xFF173D6D)))
    val SoundRowShape = RoundedCornerShape(16.dp)
    val SoundRowHeight = 52.dp
    const val SoundExpansionMillis = 220
    val PageMargin = 16.dp
    val RowHeight = 66.dp
    val ArtworkSize = 50.dp
    val TouchTarget = 48.dp
    val MiniPlayerHeight = 64.dp
    val NowPlayingMargin = 20.dp
    val NowPlayingArtworkMax = 380.dp
    val NowPlayingPlaySize = 72.dp
    val NowPlayingCompactPlaySize = 68.dp
    val NowPlayingControlTarget = 56.dp
    val NowPlayingCompactControlTarget = 52.dp
    val NowPlayingTransportIcon = 34.dp
    val NowPlayingModeIcon = 30.dp
    val NowPlayingPlayIcon = 38.dp
    val NowPlayingSeekTrack = 6.dp
    val NowPlayingSeekThumb = 18.dp
    val NowPlayingSeekTouch = 48.dp
    val LibraryPagerTouchSlop = 1.4f
    val LibraryPagerThreshold = .42f
    val LibraryPagerStiffness = 300f
    val ArtworkShape = RoundedCornerShape(9.dp)
    val CardShape = RoundedCornerShape(22.dp)
    val CapsuleShape = RoundedCornerShape(50)
    val Type = Typography(
        headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
        bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 18.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
    )
}

@Composable
fun MusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = MusicStyle.AccentText, onPrimary = MusicStyle.Background,
            secondary = MusicStyle.AccentText, background = MusicStyle.Background, surface = MusicStyle.Secondary,
            surfaceVariant = MusicStyle.Surface, onSurface = MusicStyle.Text, onSurfaceVariant = MusicStyle.Muted,
            secondaryContainer = MusicStyle.Surface, onSecondaryContainer = MusicStyle.Text,
            outline = MusicStyle.Divider),
        typography = MusicStyle.Type,
        content = content
    )
}
