package at.drinkly.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import at.drinkly.app.data.HydrationStatus

/**
 * The app's colors, light or dark like the system. [DrinklyTheme] picks the set; switching the
 * system mode recreates the activity, so everything is drawn again with the new colors.
 */
object DrinklyColors {
    var dark = false
        internal set

    private fun pick(light: Long, night: Long) = Color(if (dark) night else light)

    val Blue get() = pick(0xFF1F7AC6, 0xFF3D91DA)
    val DeepBlue get() = pick(0xFF135A9C, 0xFF8CC4F0)
    // Page background
    val Sky get() = pick(0xFFEFF6FC, 0xFF0E1621)
    val Water get() = pick(0xFFD3E6F5, 0xFF1F3651)
    // Strong blue for headings and marks; in dark mode a light blue so it stands out
    val Navy get() = pick(0xFF2B4775, 0xFFB4CCEB)
    val Ink get() = pick(0xFF1B2B3F, 0xFFE6EDF5)
    // Slightly blue grey, dark enough for small text on white (contrast about 5:1)
    val Muted get() = pick(0xFF5D6F86, 0xFF97A6B8)
    val Line get() = pick(0xFFE2EAF2, 0xFF253241)
    // Borders of inputs: visible, but light and blue-tinted instead of Material's dark grey
    val Outline get() = pick(0xFFC3D3E2, 0xFF3A4B60)
    val NoData get() = pick(0xFFC9D2DC, 0xFF3A4654)
    val Red get() = pick(0xFFD64545, 0xFFF07272)
    // Cards, sheets and pills on top of the page
    val Card get() = pick(0xFFFFFFFF, 0xFF18222E)
    val Shadow get() = pick(0xFF2B4775, 0xFF000000)
}

/** Everything is blue: the less you drank, the paler it gets. Main color for the big number and the status icon. */
val HydrationStatus.accent: Color
    get() = if (DrinklyColors.dark) {
        // On dark water it works the other way round: the more you drank, the brighter
        when (this) {
            HydrationStatus.BEHIND -> Color(0xFF6E9FCB)
            HydrationStatus.SLIGHTLY_BEHIND -> Color(0xFF7FB5E4)
            HydrationStatus.ON_TRACK -> Color(0xFF94C8F2)
            HydrationStatus.GOAL_REACHED -> Color(0xFFB8DCFA)
            HydrationStatus.TOO_MUCH -> Color(0xFFD6EAFC)
        }
    } else {
        when (this) {
            HydrationStatus.BEHIND -> Color(0xFF4A9EDB)
            HydrationStatus.SLIGHTLY_BEHIND -> Color(0xFF2F8AD0)
            HydrationStatus.ON_TRACK -> DrinklyColors.Blue
            HydrationStatus.GOAL_REACHED -> DrinklyColors.DeepBlue
            HydrationStatus.TOO_MUCH -> DrinklyColors.Navy
        }
    }

/** Color of the water surface on the Today screen; it gets deeper towards the bottom. */
val HydrationStatus.water: Color
    get() = if (DrinklyColors.dark) {
        when (this) {
            HydrationStatus.BEHIND -> Color(0xFF132C46)
            HydrationStatus.SLIGHTLY_BEHIND -> Color(0xFF15324F)
            HydrationStatus.ON_TRACK -> Color(0xFF173959)
            HydrationStatus.GOAL_REACHED -> Color(0xFF1A4064)
            HydrationStatus.TOO_MUCH -> Color(0xFF1D4770)
        }
    } else {
        when (this) {
            HydrationStatus.BEHIND -> Color(0xFFD4ECFA)
            HydrationStatus.SLIGHTLY_BEHIND -> Color(0xFFC2E3F8)
            HydrationStatus.ON_TRACK -> Color(0xFFADD7F4)
            HydrationStatus.GOAL_REACHED -> Color(0xFF9ACDF1)
            HydrationStatus.TOO_MUCH -> Color(0xFF8DC1EA)
        }
    }

@Composable
fun DrinklyTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    DrinklyColors.dark = dark
    // Dark mode: Material's own surfaces (dialogs, menus, pickers) in the app's dark blue-grey
    val scheme = if (dark) {
        darkColorScheme(
            onBackground = DrinklyColors.Ink,
            onPrimaryContainer = DrinklyColors.Navy,
            surfaceContainerLowest = DrinklyColors.Sky,
            surfaceContainerLow = DrinklyColors.Card,
            surfaceContainer = DrinklyColors.Card,
            surfaceContainerHigh = DrinklyColors.Card,
            inverseSurface = DrinklyColors.Ink,
            inverseOnSurface = DrinklyColors.Sky,
        )
    } else {
        lightColorScheme()
    }
    MaterialTheme(
        colorScheme = scheme.copy(
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
            outline = DrinklyColors.Outline,
            outlineVariant = DrinklyColors.Line,
        ),
        content = content,
    )
}
