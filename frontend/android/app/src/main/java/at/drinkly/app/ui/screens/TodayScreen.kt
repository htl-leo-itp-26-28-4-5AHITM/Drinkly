package at.drinkly.app.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import at.drinkly.app.data.liters
import at.drinkly.app.data.litersValue
import at.drinkly.app.bottle.BottleLink
import at.drinkly.app.data.Hydration
import at.drinkly.app.data.HydrationMessage
import at.drinkly.app.data.HydrationStatus
import at.drinkly.app.ui.Dashboard
import at.drinkly.app.ui.components.BottleIllustration
import at.drinkly.app.ui.components.HydrationScene
import at.drinkly.app.ui.components.pressScale
import at.drinkly.app.ui.components.pressable
import at.drinkly.app.ui.theme.DrinklyColors
import at.drinkly.app.ui.theme.accent
import at.drinkly.app.ui.theme.water
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(d: Dashboard, onAdd: (Int) -> Unit, onRefresh: () -> Unit, onBottle: () -> Unit) {
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showConnections by rememberSaveable { mutableStateOf(false) }

    val accent by animateColorAsState(d.status.accent, label = "accent")
    val water by animateColorAsState(d.status.water, label = "water")
    val fill by animateFloatAsState(d.progress, spring(stiffness = Spring.StiffnessVeryLow), label = "fill")
    val shownMl by animateIntAsState(d.todayMl, label = "ml")

    Box(Modifier.fillMaxSize()) {
        HydrationScene(
            fill = fill,
            goalMl = d.settings.goalMl,
            water = water,
            topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 92.dp), // the floating bottom bar
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                ConnectionPill(d) { showConnections = true }
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    litersValue(shownMl),
                    Modifier.alignByBaseline(),
                    fontSize = 104.sp,
                    lineHeight = 104.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-4).sp,
                    color = accent,
                )
                Spacer(Modifier.width(6.dp))
                Text("L", Modifier.alignByBaseline(), fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = accent)
            }
            Spacer(Modifier.height(8.dp))
            StreakChip(d.streak)
            // The status card is hidden for now; bring it back with StatusCard(d.message, d.streak)

            Spacer(Modifier.height(32.dp))
            val source = remember { MutableInteractionSource() }
            Button(
                onClick = { showAddSheet = true },
                interactionSource = source,
                modifier = Modifier
                    .pressScale(source)
                    .size(64.dp)
                    .shadow(10.dp, CircleShape, spotColor = DrinklyColors.Blue.copy(alpha = 0.5f)),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add a drink", modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.weight(1f))
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }, containerColor = Color.White) {
            AddDrinkSheet(d.todayMl, d.settings.goalMl, d.lastHourMl, onAdd = { ml ->
                onAdd(ml)
                showAddSheet = false
            })
        }
    }
    if (showConnections) {
        ModalBottomSheet(onDismissRequest = { showConnections = false }, containerColor = Color.White) {
            ConnectionSheet(d, onBottle, onRefresh)
        }
    }
}

private val HydrationStatus.icon: ImageVector
    get() = when (this) {
        HydrationStatus.BEHIND -> Icons.Outlined.WaterDrop
        HydrationStatus.SLIGHTLY_BEHIND, HydrationStatus.ON_TRACK -> Icons.Rounded.WaterDrop
        HydrationStatus.GOAL_REACHED -> Icons.Rounded.Verified
        HydrationStatus.TOO_MUCH -> Icons.Rounded.PanTool
    }

/** What's going on right now plus the streak, as one pill (currently not shown on Today): status on the left, streak chip on the right. */
@Composable
private fun StatusCard(message: HydrationMessage, streak: Int) {
    val shape = RoundedCornerShape(50)
    val accent = message.status.accent
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = DrinklyColors.Navy.copy(alpha = 0.2f), spotColor = DrinklyColors.Navy.copy(alpha = 0.2f))
            .clip(shape)
            .background(Color.White.copy(alpha = 0.95f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            message.status.icon, contentDescription = null, tint = Color.White,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent)
                .padding(11.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(message.title, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
        StreakChip(streak)
    }
}

@Composable
private fun StreakChip(streak: Int) {
    val active = streak > 0
    val color = if (active) DrinklyColors.Blue else DrinklyColors.Muted
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) DrinklyColors.Sky else DrinklyColors.NoData.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = "$streak-day streak" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("$streak", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.width(3.dp))
        Text(if (streak == 1) "day" else "days", fontSize = 13.sp, color = DrinklyColors.Muted)
    }
}

/** One indicator for everything the app is connected to; tap for details. */
@Composable
private fun ConnectionPill(d: Dashboard, onClick: () -> Unit) {
    val allGood = d.sync.error == null && d.sync.live && d.bottle.link == BottleLink.CONNECTED
    val text = when {
        d.sync.error != null -> "Offline"
        d.bottle.link == BottleLink.CONNECTED -> d.bottle.batteryPercent?.let { "$it %" } ?: "Paired"
        d.bottle.link == BottleLink.CONNECTING -> "Pairing…"
        else -> "Bottle off"
    }
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .shadow(18.dp, shape, ambientColor = DrinklyColors.Navy.copy(alpha = 0.6f), spotColor = DrinklyColors.Navy.copy(alpha = 0.6f))
            .pressable(shape, onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .clearAndSetSemantics { contentDescription = "Connections: $text. Shows the bottle and server status" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BottleIllustration(Modifier.size(width = 9.dp, height = 20.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (allGood) DrinklyColors.Blue else DrinklyColors.NoData))
    }
}

@Composable
private fun ConnectionSheet(d: Dashboard, onBottle: () -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Connections", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)

        val bottleOk = d.bottle.link == BottleLink.CONNECTED
        ConnectionRow(
            icon = { BottleIllustration(Modifier.size(width = 14.dp, height = 30.dp)) },
            title = d.bottle.name,
            detail = when (d.bottle.link) {
                BottleLink.CONNECTED -> (d.bottle.batteryPercent?.let { "$it % battery" } ?: "Paired") + " · " + lastSyncText(d.bottle.lastSync)
                BottleLink.CONNECTING -> "Pairing…"
                BottleLink.DISCONNECTED -> "Not connected"
            },
            ok = bottleOk,
        ) {
            OutlinedButton(onClick = onBottle) { Text(if (d.bottle.link == BottleLink.DISCONNECTED) "Connect" else "Disconnect") }
        }

        HorizontalDivider(color = DrinklyColors.Line)

        val serverOk = d.sync.error == null && d.sync.live
        ConnectionRow(
            icon = { Icon(Icons.Rounded.Cloud, contentDescription = null, tint = if (serverOk) Color.White else DrinklyColors.Navy) },
            title = "Drinkly server",
            detail = if (serverOk) "Connected" else "Not connected",
            ok = serverOk,
            failed = !serverOk,
        ) {
            if (d.sync.error != null) OutlinedButton(onClick = onRefresh) { Text("Retry") }
        }
    }
}

@Composable
private fun ConnectionRow(
    icon: @Composable () -> Unit,
    title: String,
    detail: String,
    ok: Boolean,
    failed: Boolean = false, // red ✕ after the detail
    action: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(if (ok) DrinklyColors.Blue else DrinklyColors.Sky),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
            Text(title, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(detail, fontSize = 13.sp, color = DrinklyColors.Muted)
                if (failed) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = DrinklyColors.Red, modifier = Modifier.size(16.dp))
                }
            }
        }
        action()
    }
}

private fun lastSyncText(lastSync: Instant?): String {
    if (lastSync == null) return "not synced yet"
    if (Duration.between(lastSync, Instant.now()).seconds < 60) return "synced just now"
    val relative = DateUtils.getRelativeTimeSpanString(
        lastSync.toEpochMilli(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
    )
    return "synced $relative"
}

private val presets = listOf("Cup" to 150, "Glass" to 250, "Can" to 330, "Bottle" to 500)

@Composable
private fun AddDrinkSheet(todayMl: Int, goalMl: Int, lastHourMl: Int, onAdd: (Int) -> Unit) {
    var amount by rememberSaveable { mutableFloatStateOf(200f) }
    var showInfo by remember { mutableStateOf(false) }
    val custom = amount.roundToInt()

    // A gentle hint when the amount would be more than the body needs
    val warning = when {
        lastHourMl + custom > Hydration.MAX_PER_HOUR_ML -> "That's over 1 L within an hour. Better spread it out."
        todayMl + custom >= goalMl * Hydration.TOO_MUCH_FACTOR -> "That brings you to ${liters(todayMl + custom)}, well above your goal."
        else -> null
    }

    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Add a drink", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
            Box {
                IconButton(onClick = { showInfo = !showInfo }) {
                    Icon(Icons.Outlined.Info, contentDescription = "About adding drinks", tint = DrinklyColors.Blue)
                }
                if (showInfo) {
                    Popup(
                        offset = IntOffset(0, with(LocalDensity.current) { 48.dp.roundToPx() }),
                        onDismissRequest = { showInfo = false },
                        properties = PopupProperties(focusable = true),
                    ) {
                        Text(
                            "Sips from your Drinkly bottle are logged automatically. Add drinks from other glasses or bottles here.",
                            modifier = Modifier
                                .width(260.dp)
                                .shadow(12.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(14.dp),
                            fontSize = 14.sp,
                            color = DrinklyColors.Ink,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            presets.forEach { (name, ml) ->
                val shape = RoundedCornerShape(18.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .pressable(shape) { onAdd(ml) }
                        .background(DrinklyColors.Sky)
                        .border(1.dp, DrinklyColors.Water, shape)
                        .padding(vertical = 14.dp)
                        .clearAndSetSemantics { contentDescription = "Add $name, ${liters(ml)}" },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.LocalDrink, contentDescription = null, tint = DrinklyColors.Blue)
                    Text(liters(ml), fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
                    Text(name, fontSize = 12.sp, color = DrinklyColors.Muted)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Other amount", Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
            Text(liters(custom), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Blue)
        }
        Slider(
            value = amount,
            onValueChange = { amount = it },
            valueRange = 50f..1000f,
            steps = 18, // 50 ml steps
            modifier = Modifier.semantics { contentDescription = "Amount, ${liters(custom)}" },
        )
        Row {
            Text(liters(50), Modifier.weight(1f), fontSize = 12.sp, color = DrinklyColors.Muted)
            Text(liters(1000), fontSize = 12.sp, color = DrinklyColors.Muted)
        }

        AnimatedVisibility(warning != null) {
            Row(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DrinklyColors.Sky)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PanTool, contentDescription = null, tint = DrinklyColors.Navy, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(warning.orEmpty(), fontSize = 13.sp, color = DrinklyColors.Navy)
            }
        }

        Spacer(Modifier.height(16.dp))
        val source = remember { MutableInteractionSource() }
        Button(
            onClick = { onAdd(custom) },
            interactionSource = source,
            modifier = Modifier.pressScale(source).fillMaxWidth().height(54.dp),
        ) {
            Text("Add ${liters(custom)}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}
