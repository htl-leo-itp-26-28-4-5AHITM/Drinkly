package at.drinkly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.drinkly.app.ui.theme.DrinklyColors

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

/** Round white icon next to a small label and a value, as in the bottle row on the Today screen. */
@Composable
fun InfoBadge(icon: ImageVector, label: String, value: String, tint: Color = DrinklyColors.Navy, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            icon, contentDescription = null, tint = tint,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(11.dp),
        )
        Column {
            Text(label, fontSize = 12.sp, color = DrinklyColors.Muted)
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
        }
    }
}

@Composable
fun Chip(icon: ImageVector, text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
