package at.drinkly.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.drinkly.app.data.liters
import at.drinkly.app.data.DrinkEntry
import at.drinkly.app.ui.Dashboard
import at.drinkly.app.ui.components.SectionCard
import at.drinkly.app.ui.theme.DrinklyColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private enum class ActivityRange(val label: String, val days: Int) {
    WEEK("Week", 7), MONTH("30 days", 30), CALENDAR("Calendar", 0),
}

@Composable
fun ActivityScreen(d: Dashboard, onDelete: (DrinkEntry) -> Unit) {
    var range by rememberSaveable { mutableStateOf(ActivityRange.WEEK) }
    var month by rememberSaveable { mutableStateOf(YearMonth.from(d.today)) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (range == ActivityRange.CALENDAR) {
            CalendarCard(d, month, onMonth = { month = it }) {
                RangePicker(range, onChange = { range = it })
            }
        } else {
            val days = (range.days - 1 downTo 0).map { d.today.minusDays(it.toLong()) }
            val values = days.map { d.totals[it] ?: 0 }
            val goals = days.map(d::goalOn)
            val activeDays = values.filter { it > 0 }

            SectionCard {
                Text(periodTitle(days.first(), days.last()), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
                Spacer(Modifier.height(10.dp))
                BarChart(days, values, goals, Modifier.fillMaxWidth().height(220.dp))
                Spacer(Modifier.height(16.dp))
                RangePicker(range, onChange = { range = it })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatTile("Daily average", if (activeDays.isEmpty()) "–" else liters(activeDays.average().roundToInt()), Modifier.weight(1f))
                StatTile("Best day", liters(values.maxOrNull() ?: 0), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatTile("Goal reached", "${values.indices.count { values[it] >= goals[it] }} of ${range.days} days", Modifier.weight(1f))
                StatTile("Longest streak", "${d.longestStreak} days", Modifier.weight(1f))
            }
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

private fun periodTitle(first: LocalDate, last: LocalDate): String =
    if (first.month == last.month) {
        "${first.dayOfMonth} – ${last.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))}"
    } else {
        "${first.format(DateTimeFormatter.ofPattern("d MMM"))} – ${last.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
    }

@Composable
private fun RangePicker(range: ActivityRange, onChange: (ActivityRange) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(DrinklyColors.Sky)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        ActivityRange.entries.forEach { option ->
            val selected = option == range
            Text(
                option.label,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) DrinklyColors.Blue else Color.Transparent)
                    .selectable(selected, role = Role.Tab, onClick = { onChange(option) })
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else DrinklyColors.Muted,
            )
        }
    }
}

/** A month at a glance: the stronger the blue, the closer you got to your goal that day. */
@Composable
private fun CalendarCard(d: Dashboard, month: YearMonth, onMonth: (YearMonth) -> Unit, footer: @Composable () -> Unit) {
    val locale = Locale.getDefault()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - firstDay.value + 7) % 7
    val cells: List<LocalDate?> = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val past = cells.filterNotNull().filter { !it.isAfter(d.today) }
    val reached = past.count { (d.totals[it] ?: 0) >= d.goalOn(it) }

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonth(month.minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month", tint = DrinklyColors.Blue)
            }
            Text(
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = DrinklyColors.Ink,
            )
            IconButton(onClick = { onMonth(month.plusMonths(1)) }, enabled = month < YearMonth.from(d.today)) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
            }
        }
        Row(Modifier.padding(vertical = 6.dp)) {
            (0 until 7).forEach { i ->
                Text(
                    firstDay.plus(i.toLong()).getDisplayName(DateTextStyle.NARROW, locale),
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DrinklyColors.Muted,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until 7).forEach { i ->
                    val day = week.getOrNull(i)
                    Box(Modifier.weight(1f).height(38.dp), contentAlignment = Alignment.Center) {
                        if (day != null) DayCell(day, d.totals[day] ?: 0, d.goalOn(day), day == d.today, day.isAfter(d.today))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Goal reached on $reached of ${past.size} days", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
        Spacer(Modifier.height(16.dp))
        footer()
    }
}

@Composable
private fun DayCell(day: LocalDate, ml: Int, goal: Int, isToday: Boolean, isFuture: Boolean) {
    val share = (ml.toFloat() / goal).coerceIn(0f, 1f)
    val fill = when {
        isFuture -> Color.Transparent
        ml == 0 -> DrinklyColors.NoData.copy(alpha = 0.35f)
        else -> DrinklyColors.Blue.copy(alpha = 0.2f + 0.8f * share)
    }
    val text = when {
        isFuture -> DrinklyColors.Muted.copy(alpha = 0.5f)
        ml == 0 -> DrinklyColors.Muted
        share > 0.5f -> Color.White
        else -> DrinklyColors.Ink
    }
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(fill)
            .border(if (isToday) 2.dp else 0.dp, if (isToday) DrinklyColors.Navy else Color.Transparent, CircleShape)
            .clearAndSetSemantics {
                contentDescription = day.format(DateTimeFormatter.ofPattern("d MMMM")) + if (ml > 0) ", ${liters(ml)}" else ", no drinks"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("${day.dayOfMonth}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = text)
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
private fun BarChart(days: List<LocalDate>, values: List<Int>, goals: List<Int>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = DrinklyColors.Ink)
    val goalStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    val weekdayFormat = DateTimeFormatter.ofPattern("EEE")
    val dayMonthFormat = DateTimeFormatter.ofPattern("d MMM")

    Canvas(modifier) {
        val axisWidth = 34.dp.toPx()
        val labelHeight = if (days.size <= 7) 32.dp.toPx() else 18.dp.toPx()
        val chartHeight = size.height - labelHeight
        val yMax = maxOf(goals.maxOrNull() ?: 0, values.maxOrNull() ?: 0) * 1.1f
        val yStep = if (yMax > 3000) 1000 else 500
        val slot = (size.width - axisWidth) / days.size
        val barWidth = slot * if (days.size <= 7) 0.5f else 0.62f
        val radius = CornerRadius(minOf(barWidth / 2, 6.dp.toPx()))
        fun y(ml: Int) = chartHeight - chartHeight * ml / yMax

        // "Goal" tag on the y-axis where the goal line starts; axis labels it would cover are left out
        val goalLabel = measurer.measure("Goal", goalStyle)
        val pillHeight = goalLabel.size.height + 4.dp.toPx()
        val pillWidth = goalLabel.size.width + 10.dp.toPx()
        val pillTop = (y(goals.first()) - pillHeight / 2).coerceIn(0f, chartHeight - pillHeight)

        // y-axis with grid lines in litres
        for (ml in 0..yMax.toInt() step yStep) {
            drawLine(DrinklyColors.Line, Offset(axisWidth, y(ml)), Offset(size.width, y(ml)), strokeWidth = 1.dp.toPx())
            val layout = measurer.measure(liters(ml), labelStyle.copy(color = DrinklyColors.Muted))
            val top = y(ml) - layout.size.height / 2f
            if (top + layout.size.height < pillTop || top > pillTop + pillHeight) {
                drawText(layout, topLeft = Offset(axisWidth - 6.dp.toPx() - layout.size.width, top))
            }
        }

        days.forEachIndexed { i, day ->
            val x = axisWidth + i * slot + (slot - barWidth) / 2
            val value = values[i]
            if (value > 0) {
                val top = y(value).coerceAtMost(chartHeight - 4.dp.toPx())
                drawRoundRect(
                    color = if (value >= goals[i]) DrinklyColors.Blue else DrinklyColors.Blue.copy(alpha = 0.45f),
                    topLeft = Offset(x, top),
                    size = Size(barWidth, chartHeight - top),
                    cornerRadius = radius,
                )
            } else {
                // Grayed-out column: the bottle wasn't used and nothing was logged
                drawRoundRect(DrinklyColors.NoData.copy(alpha = 0.3f), Offset(x, 0f), Size(barWidth, chartHeight), radius)
            }

            val showLabel = days.size <= 7 || (days.size - 1 - i) % 5 == 0
            if (showLabel) {
                val style = labelStyle.copy(color = if (value > 0) DrinklyColors.Ink else DrinklyColors.Muted.copy(alpha = 0.6f))
                val lines = if (days.size <= 7) listOf(day.format(weekdayFormat), "${day.dayOfMonth}") else listOf(day.format(dayMonthFormat))
                var top = chartHeight + 4.dp.toPx()
                lines.forEach { line ->
                    val layout = measurer.measure(line, style)
                    drawText(layout, topLeft = Offset(axisWidth + i * slot + slot / 2 - layout.size.width / 2, top))
                    top += layout.size.height
                }
            }
        }

        // Goal as a step line, so earlier goals show where they applied
        val goalPath = Path()
        goals.forEachIndexed { i, goal ->
            val left = axisWidth + i * slot
            if (i == 0) goalPath.moveTo(left, y(goal)) else goalPath.lineTo(left, y(goal))
            goalPath.lineTo(left + slot, y(goal))
        }
        drawPath(
            goalPath,
            color = DrinklyColors.Navy.copy(alpha = 0.7f),
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
        )
        val pillLeft = axisWidth - 2.dp.toPx() - pillWidth
        drawRoundRect(DrinklyColors.Navy, Offset(pillLeft, pillTop), Size(pillWidth, pillHeight), CornerRadius(pillHeight / 2))
        drawText(goalLabel, topLeft = Offset(pillLeft + 5.dp.toPx(), pillTop + 2.dp.toPx()))
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
            Text(liters(entry.milliliters), fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
            Text(
                entry.timestamp.atZone(ZoneId.systemDefault()).format(timeFormat),
                fontSize = 12.sp, color = DrinklyColors.Muted,
            )
        }
        IconButton(onClick = { onDelete(entry) }) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${liters(entry.milliliters)}", tint = DrinklyColors.Muted)
        }
    }
}
