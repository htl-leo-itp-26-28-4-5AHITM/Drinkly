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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
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
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val MAX_SHARE = 1.25f

/**
 * The Today background: water rising from the sandy floor (empty = nothing drunk yet), a flower that
 * recovers as you drink, and a faint measuring scale on the right edge.
 *
 * [fill] is today's intake divided by the goal; [topInset] and [bottomInset] keep the scale clear
 * of the title and the bottom bar.
 */
@Composable
fun HydrationScene(fill: Float, goalMl: Int, water: Color, topInset: Dp, bottomInset: Dp, modifier: Modifier = Modifier) {
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
            floor = size.height - bottomInset.toPx() - 30.dp.toPx(),
            extra = 10.dp.toPx(),
            top = topInset.toPx() + 80.dp.toPx(),
        )
        drawRect(Brush.verticalGradient(listOf(Color.White, DrinklyColors.Sky)))

        val top = scale.y(min(fill, MAX_SHARE))
        drawPath(wave(top, phase + PI.toFloat() * 0.8f, 8.dp.toPx()), water.copy(alpha = 0.55f))
        drawPath(wave(top, phase, 10.dp.toPx()), water)

        drawSand(scale)
        drawFlower(
            base = Offset(size.width * 0.16f, scale.floor + 6.dp.toPx()),
            height = min(130.dp.toPx(), scale.zero - scale.top) * 0.9f,
            health = fill.coerceIn(0f, 1f),
        )
        drawRuler(scale, goalMl, measurer)
    }
}

/** Maps a share of the goal to a height: 0 sits on the sand, the goal at about 80 % of the free space. */
private class SceneScale(val floor: Float, extra: Float, val top: Float) {
    val zero = floor + extra // empty water hides behind the sand

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

private fun DrawScope.drawSand(scale: SceneScale) {
    val path = Path().apply {
        moveTo(0f, size.height)
        var x = 0f
        while (x <= size.width) {
            val t = x / size.width * 2f * PI.toFloat()
            lineTo(x, scale.floor + 6.dp.toPx() * sin(t * 1.3f + 0.6f) + 3.dp.toPx() * sin(t * 3.1f))
            x += 6f
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(
        path,
        Brush.verticalGradient(listOf(DrinklyColors.Sand, DrinklyColors.SandShade), startY = scale.floor - 10.dp.toPx(), endY = size.height),
    )
    // a few pebbles
    listOf(Triple(0.42f, 14f, 3f), Triple(0.47f, 20f, 2f), Triple(0.71f, 12f, 2.5f), Triple(0.88f, 22f, 3.5f)).forEach { (fx, dy, r) ->
        val rx = r.dp.toPx() * 1.4f
        val ry = r.dp.toPx()
        drawOval(
            DrinklyColors.SandShade,
            topLeft = Offset(size.width * fx - rx, scale.floor + dy.dp.toPx() - ry),
            size = Size(rx * 2, ry * 2),
        )
    }
}

/** Droops and fades to brown when you haven't drunk, stands up and blooms blue at the goal. */
private fun DrawScope.drawFlower(base: Offset, height: Float, health: Float) {
    val droop = 1f - health
    val length = height * (0.7f + 0.3f * health)
    val bend = Offset(base.x, base.y - length * 0.55f)
    val angle = droop * 1.9f // the top of the stem tips over by up to ~110°
    val head = Offset(bend.x + sin(angle) * length * 0.45f, bend.y - cos(angle) * length * 0.45f)
    val green = lerp(Color(0xFFB0A16C), Color(0xFF4FA36B), health)

    val stem = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(bend.x, bend.y, head.x, head.y)
    }
    drawPath(stem, green, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

    val leafAnchor = Offset(base.x, base.y - length * 0.28f)
    for (side in listOf(-1f, 1f)) {
        // up and out when healthy, hanging when wilted
        rotate(degrees = side * (0.75f + droop * 1.6f) * 180f / PI.toFloat(), pivot = leafAnchor) {
            drawOval(green, topLeft = leafAnchor + Offset(-5.dp.toPx(), -24.dp.toPx()), size = Size(10.dp.toPx(), 24.dp.toPx()))
        }
    }

    val radius = (7f + 6f * health).dp.toPx()
    val petal = lerp(Color(0xFFB8A99A), Color(0xFF4A8FE0), health)
    translate(head.x, head.y) {
        rotate(degrees = angle * 180f / PI.toFloat(), pivot = Offset.Zero) {
            for (i in 0 until 6) {
                rotate(degrees = i * 60f, pivot = Offset.Zero) {
                    drawOval(petal, topLeft = Offset(-radius * 0.45f, -radius * 1.6f), size = Size(radius * 0.9f, radius * 1.2f))
                }
            }
            drawCircle(lerp(Color(0xFF9C7F55), Color(0xFFF4C24E), health), radius = radius * 0.5f, center = Offset.Zero)
        }
    }
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
