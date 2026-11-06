package at.drinkly.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import at.drinkly.app.data.HydrationStatus

object DrinklyColors {
    val Blue = Color(0xFF1F7AC6)
    val Sky = Color(0xFFEFF6FC)
    val Water = Color(0xFFD3E6F5)
    val Navy = Color(0xFF2B4775)
    val Ink = Color(0xFF1B2B3F)
    val Muted = Color(0xFF7E8FA2)
    val Line = Color(0xFFE2EAF2)
    val Card = Color.White
}

/** Main color for a hydration status, used for the big number and the chips. */
val HydrationStatus.accent: Color
    get() = when (this) {
        HydrationStatus.GOAL_REACHED -> Color(0xFF169C86)
        HydrationStatus.ON_TRACK -> DrinklyColors.Blue
        HydrationStatus.SLIGHTLY_BEHIND -> Color(0xFF2E96D8)
        HydrationStatus.BEHIND -> Color(0xFF45AEE6)
    }

/** Color of the water wave on the Today screen. */
val HydrationStatus.water: Color
    get() = when (this) {
        HydrationStatus.GOAL_REACHED -> Color(0xFFCBEDE5)
        HydrationStatus.ON_TRACK -> DrinklyColors.Water
        HydrationStatus.SLIGHTLY_BEHIND -> Color(0xFFD5EBF9)
        HydrationStatus.BEHIND -> Color(0xFFD9F0FC)
    }

@Composable
fun DrinklyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = DrinklyColors.Blue,
            onPrimary = Color.White,
            primaryContainer = DrinklyColors.Water,
            secondary = DrinklyColors.Navy,
            secondaryContainer = DrinklyColors.Water,
            onSecondaryContainer = DrinklyColors.Navy,
            surfaceContainerHighest = DrinklyColors.Water,
            background = DrinklyColors.Sky,
            surface = DrinklyColors.Card,
            onSurface = DrinklyColors.Ink,
            onSurfaceVariant = DrinklyColors.Muted,
            outlineVariant = DrinklyColors.Line,
        ),
        content = content,
    )
}
