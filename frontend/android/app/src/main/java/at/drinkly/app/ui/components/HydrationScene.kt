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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.drinkly.app.ui.theme.DrinklyColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

private const val MAX_SHARE = 1.25f

/**
 * The Today background: water rising from the bottom edge (empty = nothing drunk yet) and a faint
 * measuring scale on the right edge.
 *
 * [fill] is today's intake divided by the goal; [topInset] keeps the scale clear of the status bar.
 */
@Composable
fun HydrationScene(fill: Float, goalMl: Int, water: Color, topInset: Dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "phase",
    )
    val measurer = rememberTextMeasurer()

    Canvas(modifier) {
        val scale = SceneScale(
            zero = size.height + 12.dp.toPx(), // empty water hides below the screen
            top = topInset.toPx() + 80.dp.toPx(),
        )
        drawRect(Brush.verticalGradient(listOf(Color.White, DrinklyColors.Sky)))

        val top = scale.y(min(fill, MAX_SHARE))
        drawPath(wave(top, phase + PI.toFloat() * 0.8f, 8.dp.toPx()), water.copy(alpha = 0.55f))
        drawPath(wave(top, phase, 10.dp.toPx()), water)

        drawRuler(scale, goalMl, measurer)
    }
}

/** Maps a share of the goal to a height: 0 sits at [zero], the goal at about 80 % of the free space. */
private class SceneScale(val zero: Float, val top: Float) {
    fun y(share: Float) = zero - (zero - top) * 0.8f * share
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

/** Faint ticks every 250 ml (500 ml for big goals), labelled every 500 ml (1 l), with the goal highlighted. */
private fun DrawScope.drawRuler(scale: SceneScale, goalMl: Int, measurer: TextMeasurer) {
    val x = size.width - 12.dp.toPx()
    val step = if (goalMl > 2500) 500 else 250
    val labelEvery = step * 2
    val maxMl = (goalMl * MAX_SHARE).toInt()
    val color = DrinklyColors.Navy.copy(alpha = 0.28f)
    val labelStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = DrinklyColors.Muted.copy(alpha = 0.7f))

    fun label(text: String, y: Float, gap: Float, style: TextStyle) {
        val layout = measurer.measure(text, style)
        drawText(layout, topLeft = Offset(x - gap - layout.size.width, y - layout.size.height / 2f))
    }

    drawLine(color, Offset(x, scale.zero), Offset(x, scale.y(MAX_SHARE)), strokeWidth = 1.dp.toPx())
    for (ml in step..maxMl step step) {
        val y = scale.y(ml.toFloat() / goalMl)
        val major = ml % labelEvery == 0
        drawLine(color, Offset(x - (if (major) 12 else 6).dp.toPx(), y), Offset(x, y), strokeWidth = 1.dp.toPx())
        if (major && abs(ml - goalMl) >= step) label(liters(ml), y, 16.dp.toPx(), labelStyle)
    }

    val goalY = scale.y(1f)
    drawLine(DrinklyColors.Blue.copy(alpha = 0.6f), Offset(x - 18.dp.toPx(), goalY), Offset(x, goalY), strokeWidth = 2.dp.toPx())
    label(
        "Goal ${liters(goalMl)}", goalY, 22.dp.toPx(),
        labelStyle.copy(fontWeight = FontWeight.SemiBold, color = DrinklyColors.Blue.copy(alpha = 0.8f)),
    )
}
