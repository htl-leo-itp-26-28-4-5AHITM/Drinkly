package at.drinkly.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import at.drinkly.app.ui.theme.DrinklyColors
import kotlin.math.PI
import kotlin.math.sin

/** Light sky with a gently moving water surface; [level] is the share of the height covered by water. */
@Composable
fun WaveBackground(level: Float, water: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "phase",
    )

    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color.White, DrinklyColors.Sky)))

        val top = size.height * (1f - level)
        val amplitude = 10.dp.toPx()

        fun surface(shift: Float, amp: Float) = Path().apply {
            moveTo(0f, size.height)
            var x = 0f
            while (x <= size.width) {
                lineTo(x, top + amp * sin(x / size.width * 2f * PI.toFloat() + phase + shift))
                x += 6f
            }
            lineTo(size.width, top + amp * sin(2f * PI.toFloat() + phase + shift))
            lineTo(size.width, size.height)
            close()
        }

        drawPath(surface(PI.toFloat() * 0.8f, amplitude * 0.8f), water.copy(alpha = 0.55f))
        drawPath(surface(0f, amplitude), water)
    }
}
