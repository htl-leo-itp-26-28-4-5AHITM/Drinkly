package at.drinkly.app.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Battery2Bar
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.automirrored.rounded.BatteryUnknown
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.drinkly.app.bottle.BottleLink
import at.drinkly.app.bottle.BottleState
import at.drinkly.app.ui.Dashboard
import at.drinkly.app.ui.components.BottleIllustration
import at.drinkly.app.ui.components.Chip
import at.drinkly.app.ui.components.InfoBadge
import at.drinkly.app.ui.components.ScreenTitle
import at.drinkly.app.ui.components.WaveBackground
import at.drinkly.app.ui.theme.DrinklyColors
import at.drinkly.app.ui.theme.accent
import at.drinkly.app.ui.theme.water
import java.time.Duration
import java.time.Instant

private val Flame = Color(0xFF45AEE6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(d: Dashboard, onAdd: (Int) -> Unit, onRefresh: () -> Unit, onBottle: () -> Unit) {
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    val accent by animateColorAsState(d.status.accent, label = "accent")
    val water by animateColorAsState(d.status.water, label = "water")
    val level by animateFloatAsState(
        0.36f + 0.4f * d.progress,
        spring(stiffness = Spring.StiffnessVeryLow),
        label = "level",
    )
    val shownMl by animateIntAsState(d.todayMl, label = "ml")

    Box(Modifier.fillMaxSize()) {
        WaveBackground(level, water, Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 100.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScreenTitle("Today", modifier = Modifier.weight(1f))
                SyncButton(d, onRefresh)
            }
            d.sync.error?.let {
                Text(it, fontSize = 13.sp, color = Color(0xFFD9572B))
            }

            Spacer(Modifier.weight(0.7f))

            Text(
                "$shownMl",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 136.sp,
                lineHeight = 136.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-6).sp,
                color = accent,
            )
            Text(
                "of ${d.settings.goalMl} ml goal",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = DrinklyColors.Muted,
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                Chip(Icons.Rounded.WaterDrop, d.status.label, accent)
                Chip(Icons.Rounded.LocalFireDepartment, "${d.streak}-day streak", Flame)
            }
            Spacer(Modifier.height(28.dp))
            Icon(
                Icons.Rounded.Add,
                contentDescription = "Add a drink",
                tint = DrinklyColors.Navy,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .shadow(12.dp, CircleShape, spotColor = DrinklyColors.Navy.copy(alpha = 0.3f))
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { showAddSheet = true }
                    .padding(20.dp)
                    .size(32.dp),
            )

            Spacer(Modifier.weight(1f))

            BottleRow(d.bottle, onBottle)
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }, containerColor = Color.White) {
            AddDrinkSheet(onAdd = { ml ->
                onAdd(ml)
                showAddSheet = false
            })
        }
    }
}

@Composable
private fun SyncButton(d: Dashboard, onRefresh: () -> Unit) {
    val (icon, description) = when {
        d.sync.error != null -> Icons.Rounded.CloudOff to "Backend offline, tap to retry"
        d.sync.live -> Icons.Rounded.CloudDone to "Live connection to the backend"
        else -> Icons.Rounded.CloudSync to "Connecting to the backend"
    }
    IconButton(onClick = onRefresh) {
        Icon(icon, contentDescription = description, tint = DrinklyColors.Blue)
    }
}

@Composable
private fun BottleRow(bottle: BottleState, onBottle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BottleIllustration(
            Modifier.size(width = 44.dp, height = 110.dp),
            dimmed = bottle.link != BottleLink.CONNECTED,
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(bottle.name, fontSize = 13.sp, color = DrinklyColors.Muted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoBadge(
                    icon = when (bottle.link) {
                        BottleLink.CONNECTED -> Icons.Rounded.BluetoothConnected
                        BottleLink.CONNECTING -> Icons.AutoMirrored.Rounded.BluetoothSearching
                        BottleLink.DISCONNECTED -> Icons.Rounded.BluetoothDisabled
                    },
                    label = "Connection",
                    value = when (bottle.link) {
                        BottleLink.CONNECTED -> "Paired"
                        BottleLink.CONNECTING -> "Pairing…"
                        BottleLink.DISCONNECTED -> "Off"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onBottle),
                )
                InfoBadge(
                    icon = batteryIcon(bottle.batteryPercent),
                    label = "Battery",
                    value = bottle.batteryPercent?.let { "$it%" } ?: "–",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(lastSyncText(bottle.lastSync), fontSize = 12.sp, color = DrinklyColors.Muted)
        }
    }
}

private fun batteryIcon(percent: Int?) = when {
    percent == null -> Icons.AutoMirrored.Rounded.BatteryUnknown
    percent < 15 -> Icons.Rounded.BatteryAlert
    percent < 50 -> Icons.Rounded.Battery2Bar
    percent < 90 -> Icons.Rounded.Battery5Bar
    else -> Icons.Rounded.BatteryFull
}

private fun lastSyncText(lastSync: Instant?): String {
    if (lastSync == null) return "Not synced yet"
    if (Duration.between(lastSync, Instant.now()).seconds < 60) return "Synced just now"
    val relative = DateUtils.getRelativeTimeSpanString(
        lastSync.toEpochMilli(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
    )
    return "Synced $relative"
}

private val presets = listOf("Cup" to 150, "Glass" to 250, "Can" to 330, "Bottle" to 500)

@Composable
private fun AddDrinkSheet(onAdd: (Int) -> Unit) {
    var custom by rememberSaveable { mutableIntStateOf(200) }

    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Text("Add a drink", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
        Text(
            "Sips from your bottle are added automatically. Log other drinks here.",
            fontSize = 14.sp, color = DrinklyColors.Muted,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            presets.forEach { (name, ml) ->
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(DrinklyColors.Sky)
                        .clickable { onAdd(ml) }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.LocalDrink, contentDescription = null, tint = DrinklyColors.Blue)
                    Text("$ml ml", fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
                    Text(name, fontSize = 12.sp, color = DrinklyColors.Muted)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { custom = (custom - 50).coerceAtLeast(50) }) {
                Icon(Icons.Rounded.Remove, contentDescription = "50 ml less")
            }
            Text(
                "$custom ml",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = DrinklyColors.Ink,
            )
            FilledTonalIconButton(onClick = { custom = (custom + 50).coerceAtMost(2000) }) {
                Icon(Icons.Rounded.Add, contentDescription = "50 ml more")
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(onClick = { onAdd(custom) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Add $custom ml", fontSize = 16.sp)
        }
    }
}
