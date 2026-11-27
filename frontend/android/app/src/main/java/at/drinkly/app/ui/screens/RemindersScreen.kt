package at.drinkly.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import at.drinkly.app.data.AppSettings
import at.drinkly.app.data.ReminderPlan
import at.drinkly.app.ui.Dashboard
import at.drinkly.app.ui.components.SectionCard
import at.drinkly.app.ui.components.pressScale
import at.drinkly.app.ui.theme.DrinklyColors

@Composable
fun RemindersScreen(d: Dashboard, onUpdate: ((AppSettings) -> AppSettings) -> Unit, onTest: () -> Unit) {
    val s = d.settings
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun ensurePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ReminderPlan.entries.forEach { plan ->
                PlanCard(plan, selected = plan == s.reminderPlan) {
                    if (plan != ReminderPlan.OFF) ensurePermission()
                    onUpdate { it.copy(reminderPlan = plan) }
                }
            }
        }

        SectionCard("Active hours") {
            HourStepper("From", s.wakeHour, 4..(s.sleepHour - 1)) { h -> onUpdate { it.copy(wakeHour = h) } }
            HourStepper("Until", s.sleepHour, (s.wakeHour + 1)..24) { h -> onUpdate { it.copy(sleepHour = h) } }
            Spacer(Modifier.height(6.dp))
            Text(
                "Reminders only arrive in this window, and your daily goal is spread across it.",
                fontSize = 13.sp, color = DrinklyColors.Muted,
            )
        }

        SectionCard {
            Row(
                Modifier.toggleable(
                    value = s.startOnWakeUp,
                    role = Role.Switch,
                    onValueChange = { v -> onUpdate { it.copy(startOnWakeUp = v) } },
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Start when I get up", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
                Spacer(Modifier.width(12.dp))
                Switch(checked = s.startOnWakeUp, onCheckedChange = null)
            }
        }

        SectionCard {
            Row(
                Modifier.toggleable(
                    value = s.onlyWhenBehind,
                    role = Role.Switch,
                    onValueChange = { v -> onUpdate { it.copy(onlyWhenBehind = v) } },
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Only when I'm behind", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
                Spacer(Modifier.width(12.dp))
                Switch(checked = s.onlyWhenBehind, onCheckedChange = null)
            }
        }

        OutlinedButton(
            onClick = {
                ensurePermission()
                onTest()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Send a test reminder")
        }
    }
}

private val ReminderPlan.icon
    get() = when (this) {
        ReminderPlan.OFF -> Icons.Rounded.NotificationsOff
        ReminderPlan.RELAXED -> Icons.Rounded.SelfImprovement
        ReminderPlan.BALANCED -> Icons.Rounded.Balance
        ReminderPlan.STEADY -> Icons.Rounded.Bolt
    }

@Composable
private fun PlanCard(plan: ReminderPlan, selected: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(source)
            .clip(shape)
            .background(Color.White)
            .border(2.dp, if (selected) DrinklyColors.Blue else Color.Transparent, shape)
            .selectable(selected, interactionSource = source, indication = ripple(), role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            plan.icon, contentDescription = null,
            tint = if (selected) Color.White else DrinklyColors.Blue,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (selected) DrinklyColors.Blue else DrinklyColors.Sky)
                .padding(10.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(plan.title, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
            Text(plan.description, fontSize = 13.sp, color = DrinklyColors.Muted)
        }
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, allowed: IntRange, onChange: (Int) -> Unit) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = DrinklyColors.Ink)
        FilledTonalIconButton(onClick = { onChange(hour - 1) }, enabled = hour - 1 in allowed) {
            Icon(Icons.Rounded.Remove, contentDescription = "One hour earlier")
        }
        Text(
            "%02d:00".format(hour),
            Modifier.width(64.dp),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = DrinklyColors.Ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        FilledTonalIconButton(onClick = { onChange(hour + 1) }, enabled = hour + 1 in allowed) {
            Icon(Icons.Rounded.Add, contentDescription = "One hour later")
        }
    }
}
