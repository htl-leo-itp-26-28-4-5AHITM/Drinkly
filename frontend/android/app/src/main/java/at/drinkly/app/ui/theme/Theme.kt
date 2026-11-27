package at.drinkly.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import at.drinkly.app.data.HydrationStatus

object DrinklyColors {
    val Blue = Color(0xFF1F7AC6)
    val DeepBlue = Color(0xFF135A9C)
    val Sky = Color(0xFFEFF6FC)
    val Water = Color(0xFFD3E6F5)
    val Navy = Color(0xFF2B4775)
    val Ink = Color(0xFF1B2B3F)
    // Dark enough for small text on white (contrast about 5:1)
    val Muted = Color(0xFF627387)
    val Line = Color(0xFFE2EAF2)
    val NoData = Color(0xFFC9D2DC)
    val Red = Color(0xFFD64545)
    val Card = Color.White
}

/** Everything is blue: the less you drank, the paler it gets. Main color for the big number and the status icon. */
val HydrationStatus.accent: Color
    get() = when (this) {
        HydrationStatus.BEHIND -> Color(0xFF5AB0E6)
        HydrationStatus.SLIGHTLY_BEHIND -> Color(0xFF3796D6)
        HydrationStatus.ON_TRACK -> DrinklyColors.Blue
        HydrationStatus.GOAL_REACHED -> DrinklyColors.DeepBlue
        HydrationStatus.TOO_MUCH -> DrinklyColors.Navy
    }

/** Color of the water on the Today screen. */
val HydrationStatus.water: Color
    get() = when (this) {
        HydrationStatus.BEHIND -> Color(0xFFDDEFFA)
        HydrationStatus.SLIGHTLY_BEHIND -> Color(0xFFCFE6F7)
        HydrationStatus.ON_TRACK -> Color(0xFFBFDCF3)
        HydrationStatus.GOAL_REACHED -> Color(0xFFA6CDEE)
        HydrationStatus.TOO_MUCH -> Color(0xFF9CC2E6)
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
