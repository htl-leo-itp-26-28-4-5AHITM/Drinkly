package at.drinkly.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.drinkly.app.ui.theme.DrinklyColors
import at.drinkly.app.ui.theme.DrinklyType

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DrinklyColors.Card)
            .padding(16.dp),
    ) {
        if (title != null) {
            // Eyebrow label like the chapter marks in the corporate design manual
            Text(
                title.uppercase(),
                style = DrinklyType.Caption,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = DrinklyColors.AquaInk,
            )
            Spacer(Modifier.height(8.dp))
        }
        content()
    }
}

/** Shrinks slightly while pressed, so every tap gets visible feedback (on top of the ripple). */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "press")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Clickable [shape] with ripple and [pressScale]. */
@Composable
fun Modifier.pressable(shape: Shape, role: Role = Role.Button, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return pressScale(source)
        .clip(shape)
        .clickable(interactionSource = source, indication = ripple(), role = role, onClick = onClick)
}
