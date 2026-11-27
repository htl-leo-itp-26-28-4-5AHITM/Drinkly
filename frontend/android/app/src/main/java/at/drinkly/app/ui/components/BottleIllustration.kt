package at.drinkly.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** A simple drawing of the Drinkly smart bottle. */
@Composable
fun BottleIllustration(modifier: Modifier = Modifier, dimmed: Boolean = false) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val alpha = if (dimmed) 0.45f else 1f
        val capH = h * 0.16f
        val capW = w * 0.62f

        // cap with a small loop handle
        drawRoundRect(
            color = Color(0xFF1E355C).copy(alpha = alpha),
            topLeft = Offset((w - capW) / 2, 0f),
            size = Size(capW, capH),
            cornerRadius = CornerRadius(w * 0.12f),
        )
        // body
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(Color(0xFF3B5D93), Color(0xFF2B4775), Color(0xFF203A63)),
            ),
            topLeft = Offset(0f, capH * 0.9f),
            size = Size(w, h - capH * 0.9f),
            cornerRadius = CornerRadius(w * 0.28f),
            alpha = alpha,
        )
        // highlight
        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f * alpha),
            topLeft = Offset(w * 0.18f, capH + h * 0.06f),
            size = Size(w * 0.12f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.06f),
        )
        // sensor ring
        drawRect(
            color = Color(0xFF8EC3EC).copy(alpha = 0.7f * alpha),
            topLeft = Offset(0f, h * 0.78f),
            size = Size(w, h * 0.025f),
        )
    }
}
