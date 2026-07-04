package com.neongrid.app.ui.fx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.neongrid.app.juice.JuiceController
import com.neongrid.app.juice.ScorePop
import com.neongrid.app.ui.theme.NeonTheme
import com.neongrid.engine.Board
import kotlin.math.roundToInt

/**
 * Frame-tick driven overlay aligned with the board: clear ghosts + particles.
 * Only invalidates while FX are alive.
 */
@Composable
fun FxOverlay(
    juice: JuiceController,
    theme: com.neongrid.app.meta.GameTheme,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        // Draw-phase read: redraws this layer per frame tick only.
        @Suppress("UNUSED_EXPRESSION")
        juice.frameTick.longValue
        val cell = size.width / Board.SIZE
        juice.clears.draw(this, cell, theme)
        juice.particles.draw(this, theme)
    }
}

/** Floating "+N" score texts rising from clear locations. */
@Composable
fun ScorePopups(juice: JuiceController, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        for (pop in juice.scorePops.toList()) {
            key(pop.id) {
                ScorePopText(pop) { juice.scorePops.remove(pop) }
            }
        }
    }
}

@Composable
private fun ScorePopText(pop: ScorePop, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(pop.id) {
        progress.animateTo(1f, animationSpec = tween(650))
        onDone()
    }
    val p = progress.value
    Text(
        text = pop.text,
        color = NeonTheme.Amber.copy(alpha = (1f - p).coerceIn(0f, 1f)),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            val cell = constraints.maxWidth / Board.SIZE.toFloat()
            val x = (pop.cellX * cell - placeable.width / 2f).roundToInt()
            val y = (pop.cellY * cell - placeable.height / 2f - 40f * p).roundToInt()
            layout(placeable.width, placeable.height) {
                placeable.place(IntOffset(x, y))
            }
        },
    )
}
