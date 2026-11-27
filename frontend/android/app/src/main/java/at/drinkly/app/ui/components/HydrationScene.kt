package at.drinkly.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import at.drinkly.app.ui.theme.DrinklyColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * The Today background: water rising from the bottom edge (empty = nothing drunk yet) to just below
 * the status bar once the goal is reached, so the wave stays visible.
 *
 * [fill] is today's intake divided by the goal.
 */
@Composable
fun HydrationScene(fill: Float, water: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "phase",
    )

    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Canvas(modifier) {
        val scale = SceneScale(
            zero = size.height + 12.dp.toPx(), // empty water hides below the screen
            full = statusBar.toPx() + 24.dp.toPx(), // at the goal the wave still shows just below the status bar
        )
        drawRect(Brush.verticalGradient(listOf(Color.White, DrinklyColors.Sky)))

        val top = scale.y(fill)
        drawPath(wave(top, phase + PI.toFloat() * 0.8f, 8.dp.toPx()), water.copy(alpha = 0.55f))
        // Clear at the surface, a little deeper towards the bottom
        drawPath(
            wave(top, phase, 10.dp.toPx()),
            Brush.verticalGradient(listOf(water, lerp(water, DrinklyColors.Blue, 0.22f)), startY = top, endY = size.height),
        )
    }
}

/** Maps a share of the goal to a height: 0 sits at [zero], the goal (and anything above it) at [full]. */
private class SceneScale(val zero: Float, val full: Float) {
    fun y(share: Float) = zero - (zero - full) * share.coerceIn(0f, 1f)
}

private fun DrawScope.wave(top: Float, phase: Float, amplitude: Float) = Path().apply {
    moveTo(0f, size.height)
    var x = 0f
    while (x <= size.width) {
        lineTo(x, top + amplitude * sin(x / size.width * 2f * PI.toFloat() + phase))
        x += 6f
    }
    lineTo(size.width, top + amplitude * sin(2f * PI.toFloat() + phase))
    lineTo(size.width, size.height)
    close()
}
