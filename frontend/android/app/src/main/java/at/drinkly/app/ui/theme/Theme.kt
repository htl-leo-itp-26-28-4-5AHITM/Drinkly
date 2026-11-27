package at.drinkly.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import at.drinkly.app.R
import at.drinkly.app.data.HydrationStatus

/** Colours from the Drinkly Corporate Design Manual 1.0 (chapter 03). */
object DrinklyColors {
    // House colours
    /** Logo, headlines, large areas, primary buttons. */
    val Tiefsee = Color(0xFF0B3C5D)
    /** Wave, progress, accent areas. Graphics only: too light for text on white. */
    val Aqua = Color(0xFF19B5D8)
    /** Backgrounds. */
    val Nebel = Color(0xFFE8F6FA)
    /** Signal, only for "goal reached". */
    val Mint = Color(0xFF3ED6A0)
    /** Signal, only for reminders. */
    val Coral = Color(0xFFFF6B4A)

    // Darker variants for text (at least 4.5:1 on white)
    val AquaInk = Color(0xFF0A7896)
    val MintInk = Color(0xFF0F7A55)
    val CoralInk = Color(0xFFB8381C)

    // Text and lines, as used throughout the manual
    val Ink = Color(0xFF0B2A3F)
    val Muted = Color(0xFF4A6273)
    val Line = Color(0xFFD3E6EE)
    val Card = Color.White
}

/** Headlines, numbers and the wordmark. */
val Nunito = FontFamily(
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

/** Body text, interface and labels. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** The type scale from chapter 04. */
object DrinklyType {
    val Display = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 52.sp)
    val H1 = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp)
    val H2 = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp)
    val Number = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 44.sp)
    val Text = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val Small = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val Caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
}

/** Signal colour for graphics: Aqua while drinking, Mint once the goal is reached. */
val HydrationStatus.accent: Color
    get() = when (this) {
        HydrationStatus.GOAL_REACHED -> DrinklyColors.Mint
        HydrationStatus.TOO_MUCH -> DrinklyColors.Tiefsee
        else -> DrinklyColors.Aqua
    }

/** [accent] dark enough for text and line icons on white. */
val HydrationStatus.accentInk: Color
    get() = when (this) {
        HydrationStatus.GOAL_REACHED -> DrinklyColors.MintInk
        HydrationStatus.TOO_MUCH -> DrinklyColors.Tiefsee
        else -> DrinklyColors.AquaInk
    }

private val typography = Typography().run {
    fun TextStyle.inter() = copy(fontFamily = Inter)
    fun TextStyle.nunito() = copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold)
    Typography(
        displayLarge = displayLarge.nunito(),
        displayMedium = displayMedium.nunito(),
        displaySmall = displaySmall.nunito(),
        headlineLarge = headlineLarge.nunito(),
        headlineMedium = headlineMedium.nunito(),
        headlineSmall = headlineSmall.nunito(),
        titleLarge = DrinklyType.H2,
        titleMedium = titleMedium.inter(),
        titleSmall = titleSmall.inter(),
        bodyLarge = DrinklyType.Text,
        bodyMedium = DrinklyType.Small,
        bodySmall = DrinklyType.Caption,
        labelLarge = labelLarge.inter().copy(fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.inter(),
        labelSmall = labelSmall.inter(),
    )
}

@Composable
fun DrinklyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = DrinklyColors.Tiefsee,
            onPrimary = Color.White,
            primaryContainer = DrinklyColors.Nebel,
            onPrimaryContainer = DrinklyColors.Tiefsee,
            secondary = DrinklyColors.AquaInk,
            secondaryContainer = DrinklyColors.Nebel,
            onSecondaryContainer = DrinklyColors.Tiefsee,
            surfaceContainerHighest = DrinklyColors.Line,
            background = DrinklyColors.Nebel,
            surface = DrinklyColors.Card,
            onSurface = DrinklyColors.Ink,
            onSurfaceVariant = DrinklyColors.Muted,
            outline = DrinklyColors.Line,
            outlineVariant = DrinklyColors.Line,
        ),
        typography = typography,
        content = content,
    )
}
