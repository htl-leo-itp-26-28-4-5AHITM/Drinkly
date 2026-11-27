package at.drinkly.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Line icons as the corporate design asks: 2 px line, round ends, 24 px grid.
 * Paths from Lucide (lucide-static 0.460.0, ISC licence, https://lucide.dev). Tint them Ink or Tiefsee.
 */
object DrinklyIcons {
    val Droplet: ImageVector by lazy {
        lineIcon(
            "droplet",
            "M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z",
        )
    }

    val Plus: ImageVector by lazy {
        lineIcon(
            "plus",
            "M5 12h14",
            "M12 5v14",
        )
    }

    val Minus: ImageVector by lazy {
        lineIcon(
            "minus",
            "M5 12h14",
        )
    }

    val Hand: ImageVector by lazy {
        lineIcon(
            "hand",
            "M18 11V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2",
            "M14 10V4a2 2 0 0 0-2-2a2 2 0 0 0-2 2v2",
            "M10 10.5V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2v8",
            "M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.86-5.99-2.34l-3.6-3.6a2 2 0 0 1 2.83-2.82L7 15",
        )
    }

    val GlassWater: ImageVector by lazy {
        lineIcon(
            "glass-water",
            "M5.116 4.104A1 1 0 0 1 6.11 3h11.78a1 1 0 0 1 .994 1.105L17.19 20.21A2 2 0 0 1 15.2 22H8.8a2 2 0 0 1-2-1.79z",
            "M6 12a5 5 0 0 1 6 0 5 5 0 0 0 6 0",
        )
    }

    val Sunrise: ImageVector by lazy {
        lineIcon(
            "sunrise",
            "M12 2v8",
            "m4.93 10.93 1.41 1.41",
            "M2 18h2",
            "M20 18h2",
            "m19.07 10.93-1.41 1.41",
            "M22 22H2",
            "m8 6 4-4 4 4",
            "M16 18a4 4 0 0 0-8 0",
        )
    }

    val BadgeCheck: ImageVector by lazy {
        lineIcon(
            "badge-check",
            "M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z",
            "m9 12 2 2 4-4",
        )
    }

    val Leaf: ImageVector by lazy {
        lineIcon(
            "leaf",
            "M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z",
            "M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12",
        )
    }

    val User: ImageVector by lazy {
        lineIcon(
            "user",
            "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2",
            "M8 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0",
        )
    }

    val Bell: ImageVector by lazy {
        lineIcon(
            "bell",
            "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9",
            "M10.3 21a1.94 1.94 0 0 0 3.4 0",
        )
    }

    val BellOff: ImageVector by lazy {
        lineIcon(
            "bell-off",
            "M8.7 3A6 6 0 0 1 18 8a21.3 21.3 0 0 0 .6 5",
            "M17 17H3s3-2 3-9a4.67 4.67 0 0 1 .3-1.7",
            "M10.3 21a1.94 1.94 0 0 0 3.4 0",
            "m2 2 20 20",
        )
    }

    val Flame: ImageVector by lazy {
        lineIcon(
            "flame",
            "M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z",
        )
    }

    val Trash: ImageVector by lazy {
        lineIcon(
            "trash-2",
            "M3 6h18",
            "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6",
            "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2",
            "M10 11L10 17",
            "M14 11L14 17",
        )
    }

    val Cloud: ImageVector by lazy {
        lineIcon(
            "cloud",
            "M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z",
        )
    }

    val Close: ImageVector by lazy {
        lineIcon(
            "x",
            "M18 6 6 18",
            "m6 6 12 12",
        )
    }

    val Check: ImageVector by lazy {
        lineIcon(
            "check",
            "M20 6 9 17l-5-5",
        )
    }

    val Zap: ImageVector by lazy {
        lineIcon(
            "zap",
            "M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z",
        )
    }

    val ChartColumn: ImageVector by lazy {
        lineIcon(
            "chart-column",
            "M3 3v16a2 2 0 0 0 2 2h16",
            "M18 17V9",
            "M13 17V5",
            "M8 17v-3",
        )
    }

    val Scale: ImageVector by lazy {
        lineIcon(
            "scale",
            "m16 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z",
            "m2 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z",
            "M7 21h10",
            "M12 3v18",
            "M3 7h2c2 0 5-1 7-2 2 1 5 2 7 2h2",
        )
    }

    val ChevronLeft: ImageVector by lazy {
        lineIcon(
            "chevron-left",
            "m15 18-6-6 6-6",
        )
    }

    val ChevronRight: ImageVector by lazy {
        lineIcon(
            "chevron-right",
            "m9 18 6-6-6-6",
        )
    }

    val Info: ImageVector by lazy {
        lineIcon(
            "info",
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0",
            "M12 16v-4",
            "M12 8h.01",
        )
    }
}

private fun lineIcon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { d ->
            addPath(
                addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
