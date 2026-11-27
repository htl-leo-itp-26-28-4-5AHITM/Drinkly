package at.drinkly.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.drinkly.app.data.liters
import at.drinkly.app.bottle.BottleLink
import at.drinkly.app.data.AppSettings
import at.drinkly.app.data.Hydration
import at.drinkly.app.ui.Dashboard
import at.drinkly.app.ui.components.BottleIllustration
import at.drinkly.app.ui.components.SectionCard
import at.drinkly.app.ui.theme.DrinklyColors
import at.drinkly.app.ui.theme.DrinklyIcons
import at.drinkly.app.ui.theme.DrinklyType
import at.drinkly.app.ui.theme.Inter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    d: Dashboard,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onToggleBottle: () -> Unit,
    onSimulateSip: (Int) -> Unit,
) {
    val s = d.settings

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(DrinklyColors.Tiefsee),
                contentAlignment = Alignment.Center,
            ) {
                Text(s.username.take(2).uppercase(), style = DrinklyType.H2, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(s.username, style = DrinklyType.H2, color = DrinklyColors.Tiefsee)
                Text("${d.streak}-day streak · ${liters(s.goalMl)} goal", style = DrinklyType.Small, color = DrinklyColors.Muted)
            }
        }

        SectionCard("Name") {
            CommitTextField(s.username, onCommit = { name -> onUpdate { it.copy(username = name) } })
            Spacer(Modifier.height(8.dp))
            Text("Your drinks are stored under this name in the backend.", style = DrinklyType.Small, color = DrinklyColors.Muted)
        }

        SectionCard("Daily goal") {
            Text(liters(s.goalMl), style = DrinklyType.Number, color = DrinklyColors.Tiefsee)
            Slider(
                value = s.goalMl.toFloat(),
                onValueChange = { v -> onUpdate { it.copy(goalMl = (v / 50).roundToInt() * 50) } },
                valueRange = 1000f..4000f,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Body weight", Modifier.weight(1f), color = DrinklyColors.Ink)
                FilledTonalIconButton(
                    onClick = { onUpdate { it.copy(weightKg = (it.weightKg - 1).coerceAtLeast(30)) } },
                ) { Icon(DrinklyIcons.Minus, contentDescription = "Less weight") }
                Text(
                    "${s.weightKg} kg", Modifier.width(72.dp), textAlign = TextAlign.Center,
                    style = DrinklyType.H2, fontWeight = FontWeight.ExtraBold, color = DrinklyColors.Tiefsee,
                )
                FilledTonalIconButton(
                    onClick = { onUpdate { it.copy(weightKg = (it.weightKg + 1).coerceAtMost(200)) } },
                ) { Icon(DrinklyIcons.Plus, contentDescription = "More weight") }
            }
            val suggested = Hydration.suggestedGoal(s.weightKg)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Suggested: ${liters(suggested)}", Modifier.weight(1f), style = DrinklyType.Small, color = DrinklyColors.Muted)
                TextButton(
                    onClick = { onUpdate { it.copy(goalMl = suggested.coerceIn(1000, 4000)) } },
                    enabled = suggested != s.goalMl,
                ) { Text("Use as goal") }
            }
        }

        SectionCard("Goal over time") {
            GoalHistoryChart(d, Modifier.fillMaxWidth().height(130.dp))
            Spacer(Modifier.height(8.dp))
            val history = d.goalHistory
            Text(
                if (history.size > 1) {
                    "Changed from ${liters(history[history.size - 2].goalMl)} to ${liters(history.last().goalMl)} on " +
                        history.last().day.format(DateTimeFormatter.ofPattern("d MMMM")) + "."
                } else {
                    "Your goal hasn't changed yet. When you change it, this line shows what it was before."
                },
                style = DrinklyType.Small, color = DrinklyColors.Muted,
            )
        }

        SectionCard("Bottle") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BottleIllustration(Modifier.size(width = 28.dp, height = 70.dp), dimmed = d.bottle.link != BottleLink.CONNECTED)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.bottle.name, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
                    Text(
                        when (d.bottle.link) {
                            BottleLink.CONNECTED -> "Paired · battery ${d.bottle.batteryPercent ?: 0}%"
                            BottleLink.CONNECTING -> "Pairing…"
                            BottleLink.DISCONNECTED -> "Not connected"
                        },
                        style = DrinklyType.Small, color = DrinklyColors.Muted,
                    )
                }
                Button(onClick = onToggleBottle) {
                    Text(if (d.bottle.link == BottleLink.DISCONNECTED) "Connect" else "Disconnect")
                }
            }
            if (d.bottle.link == BottleLink.CONNECTED) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onSimulateSip(250) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Simulate a ${liters(250)} sip")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Demo mode: the bottle is simulated until its Bluetooth profile is available.",
                style = DrinklyType.Caption, color = DrinklyColors.Muted,
            )
        }

        SectionCard("Server") {
            CommitTextField(s.backendUrl, keyboardType = KeyboardType.Uri, onCommit = { url -> onUpdate { it.copy(backendUrl = url) } })
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (color, text) = when {
                    d.sync.error != null -> DrinklyColors.Line to "Not reachable"
                    d.sync.live -> DrinklyColors.Aqua to "Live connection"
                    else -> DrinklyColors.Muted to "Connecting…"
                }
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(8.dp))
                Text(text, style = DrinklyType.Small, color = DrinklyColors.Muted)
            }
        }
    }
}

/** Step line of the daily goal over the last weeks, so you can see what it used to be. */
@Composable
private fun GoalHistoryChart(d: Dashboard, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontFamily = Inter, fontSize = 11.sp, color = DrinklyColors.Muted)
    // Last 30 days, or back to the first real change (the first entry is the "since always" goal)
    val start = minOf(d.today.minusDays(30), d.goalHistory.getOrNull(1)?.day ?: d.today)
    val totalDays = ChronoUnit.DAYS.between(start, d.today).toFloat()
    val points = listOf(start to d.goalOn(start)) +
        d.goalHistory.filter { it.day.isAfter(start) && it.day.isBefore(d.today) }.map { it.day to it.goalMl } +
        listOf(d.today to d.settings.goalMl)
    val top = (points.maxOf { it.second } * 1.25f)
    val dateFormat = DateTimeFormatter.ofPattern("d MMM")

    Canvas(modifier.semantics { contentDescription = "Daily goal over time, currently ${liters(d.settings.goalMl)}" }) {
        val axisWidth = 34.dp.toPx()
        val labelHeight = 18.dp.toPx()
        val h = size.height - labelHeight
        val w = size.width - axisWidth
        fun x(day: LocalDate) = axisWidth + w * ChronoUnit.DAYS.between(start, day) / totalDays
        fun y(ml: Int) = h - h * ml / top

        for (ml in 0..top.toInt() step 1000) {
            drawLine(DrinklyColors.Line, Offset(axisWidth, y(ml)), Offset(size.width, y(ml)), strokeWidth = 1.dp.toPx())
            val layout = measurer.measure(liters(ml), style)
            drawText(layout, topLeft = Offset(axisWidth - 6.dp.toPx() - layout.size.width, y(ml) - layout.size.height / 2f))
        }

        val line = Path()
        points.forEachIndexed { i, (day, goal) ->
            if (i == 0) line.moveTo(x(day), y(goal)) else {
                line.lineTo(x(day), y(points[i - 1].second))
                line.lineTo(x(day), y(goal))
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(x(d.today), h)
            lineTo(x(start), h)
            close()
        }
        drawPath(area, DrinklyColors.Aqua.copy(alpha = 0.2f))
        drawPath(line, DrinklyColors.AquaInk, style = Stroke(width = 2.dp.toPx()))

        listOf(start, start.plusDays((totalDays / 2).toLong()), d.today).forEachIndexed { i, day ->
            val layout = measurer.measure(day.format(dateFormat), style)
            val left = when (i) {
                0 -> x(day)
                2 -> x(day) - layout.size.width
                else -> x(day) - layout.size.width / 2
            }
            drawText(layout, topLeft = Offset(left, h + 4.dp.toPx()))
        }
    }
}

/** Text field that only saves when confirmed, so the backend isn't re-queried on every keystroke. */
@Composable
private fun CommitTextField(
    value: String,
    onCommit: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var text by rememberSaveable(value) { mutableStateOf(value) }
    val changed = text.trim().isNotEmpty() && text.trim() != value
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (changed) onCommit(text.trim()) }),
        trailingIcon = {
            if (changed) {
                IconButton(onClick = { onCommit(text.trim()) }) {
                    Icon(DrinklyIcons.Check, contentDescription = "Save", tint = DrinklyColors.Tiefsee)
                }
            }
        },
    )
}
