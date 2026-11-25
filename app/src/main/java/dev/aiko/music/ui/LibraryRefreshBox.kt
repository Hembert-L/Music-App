package dev.aiko.music.ui

import android.animation.ValueAnimator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.pullToRefreshIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Nested scrolling gives each list the gesture first; only its unconsumed top pull refreshes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryRefreshBox(refreshing: Boolean, enabled: Boolean, refresh: () -> Unit,
    modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val state = rememberPullToRefreshState()
    Box(modifier.testTag("libraryRefreshContainer")
        .pullToRefresh(isRefreshing = refreshing, state = state, enabled = enabled, onRefresh = refresh)
        .semantics {
            customActions = listOf(CustomAccessibilityAction("Actualizar biblioteca") {
                if (enabled) { refresh(); true } else false
            })
        }) {
        content()
        if (refreshing || state.distanceFraction > 0f) {
            val indicator = Modifier.align(Alignment.TopCenter).testTag("libraryRefreshIndicator").semantics {
                contentDescription = if (refreshing) "Actualizando biblioteca" else "Desliza hacia abajo para actualizar"
            }
            if (refreshing && !ValueAnimator.areAnimatorsEnabled()) {
                // A clear indeterminate wheel when Android disables infinite animations.
                Box(indicator.pullToRefreshIndicator(state, true, containerColor = MusicStyle.Surface),
                    contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(16.dp).progressSemantics()) {
                        drawArc(MusicStyle.AccentText, -90f, 270f, false, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            } else {
                PullToRefreshDefaults.Indicator(state, refreshing, indicator,
                    containerColor = MusicStyle.Surface, color = MusicStyle.AccentText)
            }
        }
    }
}
