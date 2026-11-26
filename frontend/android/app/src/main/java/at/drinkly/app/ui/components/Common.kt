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
import java.text.DecimalFormat

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 12.dp, bottom = 16.dp)) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Blue)
        if (subtitle != null) {
            Text(subtitle, fontSize = 15.sp, color = DrinklyColors.Muted)
        }
    }
}

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DrinklyColors.Card)
            .padding(18.dp),
    ) {
        if (title != null) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Muted)
            Spacer(Modifier.height(10.dp))
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

private val litersFormat = DecimalFormat("0.##")

/** 1500 -> "1.5 l" */
fun liters(ml: Int): String = litersFormat.format(ml / 1000.0) + " l"
