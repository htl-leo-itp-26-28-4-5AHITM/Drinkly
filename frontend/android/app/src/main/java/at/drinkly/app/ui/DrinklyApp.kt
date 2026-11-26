package at.drinkly.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.drinkly.app.ui.screens.ActivityScreen
import at.drinkly.app.ui.screens.ProfileScreen
import at.drinkly.app.ui.screens.RemindersScreen
import at.drinkly.app.ui.screens.TodayScreen
import at.drinkly.app.ui.components.pressScale
import at.drinkly.app.ui.theme.DrinklyColors
import kotlinx.coroutines.launch

enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Rounded.WaterDrop),
    ACTIVITY("Activity", Icons.Rounded.BarChart),
    REMINDERS("Reminders", Icons.Rounded.NotificationsActive),
    PROFILE("Profile", Icons.Rounded.Person),
}

@Composable
fun DrinklyApp(vm: DrinklyViewModel) {
    val dashboard by vm.dashboard.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm) {
        vm.messages.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            launch { snackbar.showSnackbar(message) }
        }
    }

    Box(Modifier.fillMaxSize().background(DrinklyColors.Sky)) {
        Crossfade(targetState = tab, label = "tab") { current ->
            when (current) {
                Tab.TODAY -> TodayScreen(dashboard, onAdd = vm::addDrink, onRefresh = vm::refresh, onBottle = vm::toggleBottle)
                Tab.ACTIVITY -> ActivityScreen(dashboard, onDelete = vm::deleteDrink)
                Tab.REMINDERS -> RemindersScreen(dashboard, onUpdate = vm::updateSettings, onTest = vm::sendTestReminder)
                Tab.PROFILE -> ProfileScreen(
                    dashboard,
                    onUpdate = vm::updateSettings,
                    onToggleBottle = vm::toggleBottle,
                    onSimulateSip = vm::simulateSip,
                )
            }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 92.dp),
        ) { data -> Toast(data.visuals.message) }
        BottomBar(
            tab, onSelect = { tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

/** Short confirmation like "Added 250 ml". */
@Composable
private fun Toast(message: String) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .shadow(12.dp, shape, ambientColor = DrinklyColors.Navy, spotColor = DrinklyColors.Navy.copy(alpha = 0.25f))
            .clip(shape)
            .background(Color.White)
            .border(1.dp, DrinklyColors.Water, shape)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = DrinklyColors.Blue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier
            .fillMaxWidth()
            .shadow(16.dp, shape, ambientColor = DrinklyColors.Navy, spotColor = DrinklyColors.Navy.copy(alpha = 0.3f))
            .clip(shape)
            .background(Color.White)
            .padding(6.dp)
            .selectableGroup(),
    ) {
        Tab.entries.forEach { tab ->
            val isSelected = tab == selected
            val color = if (isSelected) DrinklyColors.Blue else DrinklyColors.Muted
            val source = remember { MutableInteractionSource() }
            Column(
                Modifier
                    .weight(1f)
                    .pressScale(source)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (isSelected) DrinklyColors.Sky else Color.Transparent)
                    .selectable(isSelected, interactionSource = source, indication = ripple(), role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Text(
                    tab.label, fontSize = 11.sp, color = color,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
