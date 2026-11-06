package at.drinkly.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.drinkly.app.data.DrinkEntry
import at.drinkly.app.ui.Dashboard
import at.drinkly.app.ui.components.ScreenTitle
import at.drinkly.app.ui.components.SectionCard
import at.drinkly.app.ui.theme.DrinklyColors
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
fun ActivityScreen(d: Dashboard, onDelete: (DrinkEntry) -> Unit) {
    var range by rememberSaveable { mutableIntStateOf(7) }
    val days = (range - 1 downTo 0).map { d.today.minusDays(it.toLong()) }
    val values = days.map { d.totals[it] ?: 0 }
    val goal = d.settings.goalMl
    val activeDays = values.filter { it > 0 }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Activity", "Your hydration over time")

        RangePicker(range, onChange = { range = it })

        SectionCard("Daily intake") {
            BarChart(days, values, goal, Modifier.fillMaxWidth().height(190.dp))
            Spacer(Modifier.height(8.dp))
            Text("Dashed line: your $goal ml goal", fontSize = 12.sp, color = DrinklyColors.Muted)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatTile("Daily average", if (activeDays.isEmpty()) "–" else "${activeDays.average().roundToInt()} ml", Modifier.weight(1f))
            StatTile("Best day", "${values.maxOrNull() ?: 0} ml", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatTile("Goal reached", "${values.count { it >= goal }} of $range days", Modifier.weight(1f))
            StatTile("Longest streak", "${d.longestStreak} days", Modifier.weight(1f))
        }

        SectionCard("Recent drinks") {
            if (d.entries.isEmpty()) {
                Text("No drinks logged yet.", color = DrinklyColors.Muted)
            }
            d.entries.take(20).forEachIndexed { i, entry ->
                if (i > 0) HorizontalDivider(color = DrinklyColors.Line)
                DrinkRow(entry, onDelete)
            }
        }
    }
}

@Composable
private fun RangePicker(range: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        listOf(7 to "7 days", 30 to "30 days").forEach { (days, label) ->
            val selected = days == range
            Text(
                label,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) DrinklyColors.Blue else Color.Transparent)
                    .selectable(selected, role = Role.Tab, onClick = { onChange(days) })
                    .padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else DrinklyColors.Muted,
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        Text(label, fontSize = 13.sp, color = DrinklyColors.Muted)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
    }
}

@Composable
private fun BarChart(days: List<LocalDate>, values: List<Int>, goal: Int, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = DrinklyColors.Muted)
    val dayFormat = DateTimeFormatter.ofPattern("EEE")

    Canvas(modifier) {
        val labelHeight = 20.dp.toPx()
        val chartHeight = size.height - labelHeight
        val max = maxOf(goal, values.maxOrNull() ?: 0) * 1.1f
        val slot = size.width / days.size
        val barWidth = slot * if (days.size <= 7) 0.46f else 0.62f
        val radius = CornerRadius(barWidth / 2)

        days.forEachIndexed { i, day ->
            val x = i * slot + (slot - barWidth) / 2
            drawRoundRect(DrinklyColors.Sky, Offset(x, 0f), Size(barWidth, chartHeight), radius)
            val value = values[i]
            if (value > 0) {
                val h = (chartHeight * value / max).coerceAtLeast(barWidth)
                drawRoundRect(
                    color = if (value >= goal) DrinklyColors.Blue else DrinklyColors.Blue.copy(alpha = 0.45f),
                    topLeft = Offset(x, chartHeight - h),
                    size = Size(barWidth, h),
                    cornerRadius = radius,
                )
            }
            val showLabel = days.size <= 7 || (days.size - 1 - i) % 5 == 0
            if (showLabel) {
                val text = if (days.size <= 7) day.format(dayFormat) else day.dayOfMonth.toString()
                val layout = measurer.measure(text, labelStyle)
                drawText(layout, topLeft = Offset(i * slot + slot / 2 - layout.size.width / 2, chartHeight + 4.dp.toPx()))
            }
        }

        val goalY = chartHeight - chartHeight * goal / max
        drawLine(
            color = DrinklyColors.Navy.copy(alpha = 0.55f),
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
        )
    }
}

private val timeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

@Composable
private fun DrinkRow(entry: DrinkEntry, onDelete: (DrinkEntry) -> Unit) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.LocalDrink, contentDescription = null, tint = DrinklyColors.Blue,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(DrinklyColors.Sky)
                .padding(9.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${entry.milliliters} ml", fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
            Text(
                entry.timestamp.atZone(ZoneId.systemDefault()).format(timeFormat),
                fontSize = 12.sp, color = DrinklyColors.Muted,
            )
        }
        IconButton(onClick = { onDelete(entry) }) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${entry.milliliters} ml", tint = DrinklyColors.Muted)
        }
    }
}
