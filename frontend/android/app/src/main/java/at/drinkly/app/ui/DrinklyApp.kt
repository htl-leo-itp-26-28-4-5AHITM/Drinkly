package at.drinkly.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.drinkly.app.data.liters
import at.drinkly.app.ui.screens.ActivityScreen
import at.drinkly.app.ui.screens.ProfileScreen
import at.drinkly.app.ui.screens.RemindersScreen
import at.drinkly.app.ui.screens.TodayScreen
import at.drinkly.app.ui.components.pressScale
import at.drinkly.app.ui.theme.DrinklyColors
import at.drinkly.app.ui.theme.DrinklyIcons
import at.drinkly.app.ui.theme.DrinklyType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", DrinklyIcons.Droplet),
    ACTIVITY("Activity", DrinklyIcons.ChartColumn),
    REMINDERS("Reminders", DrinklyIcons.Bell),
    PROFILE("Profile", DrinklyIcons.User),
}

@Composable
fun DrinklyApp(vm: DrinklyViewModel) {
    val dashboard by vm.dashboard.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val snackbar = remember { SnackbarHostState() }
    var added by remember { mutableStateOf<AddedDrink?>(null) }
    var addedVisible by remember { mutableStateOf(false) }

    LaunchedEffect(vm) {
        vm.messages.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            launch { snackbar.showSnackbar(message) }
        }
    }
    LaunchedEffect(vm) {
        vm.added.collectLatest {
            added = it
            addedVisible = true
            delay(4000)
            addedVisible = false
        }
    }

    Box(Modifier.fillMaxSize().background(DrinklyColors.Nebel)) {
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
        AnimatedVisibility(
            addedVisible,
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp),
            enter = slideInVertically { -it * 2 } + fadeIn(),
            exit = slideOutVertically { -it * 2 } + fadeOut(),
        ) {
            added?.let { drink ->
                AddedNotice(drink, onUndo = {
                    addedVisible = false
                    vm.undoDrink(drink.entry)
                })
            }
        }
        BottomBar(
            tab, onSelect = { tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/** Short confirmation like "Removed 0.25 L". */
@Composable
private fun Toast(message: String) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .shadow(16.dp, shape, ambientColor = DrinklyColors.Tiefsee, spotColor = DrinklyColors.Tiefsee.copy(alpha = 0.25f))
            .clip(shape)
            .background(Color.White)
            .border(1.dp, DrinklyColors.Line, shape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(DrinklyIcons.Droplet, contentDescription = null, tint = DrinklyColors.Tiefsee, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, style = DrinklyType.Small, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
    }
}

/** Small pill from the top after a drink was saved, with a plain-text "Undo". */
@Composable
private fun AddedNotice(drink: AddedDrink, onUndo: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val amount = liters(drink.entry.milliliters)
    Row(
        Modifier
            .shadow(8.dp, shape, ambientColor = DrinklyColors.Tiefsee.copy(alpha = 0.15f), spotColor = DrinklyColors.Tiefsee.copy(alpha = 0.15f))
            .clip(shape)
            .background(Color.White.copy(alpha = 0.96f))
            .padding(start = 16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(DrinklyIcons.Droplet, contentDescription = null, tint = DrinklyColors.Tiefsee, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            if (drink.fromBottle) "Synced $amount" else "Added $amount",
            style = DrinklyType.Small, fontWeight = FontWeight.Medium, color = DrinklyColors.Ink,
        )
        val source = remember { MutableInteractionSource() }
        val pressed by source.collectIsPressedAsState()
        Text(
            "Undo",
            Modifier
                .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onUndo)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            style = DrinklyType.Small,
            fontWeight = FontWeight.SemiBold,
            color = DrinklyColors.AquaInk.copy(alpha = if (pressed) 0.5f else 1f),
        )
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier
            .fillMaxWidth()
            .shadow(16.dp, shape, ambientColor = DrinklyColors.Tiefsee, spotColor = DrinklyColors.Tiefsee.copy(alpha = 0.3f))
            .clip(shape)
            .background(Color.White)
            .padding(8.dp)
            .selectableGroup(),
    ) {
        Tab.entries.forEach { tab ->
            val isSelected = tab == selected
            val color = if (isSelected) DrinklyColors.Tiefsee else DrinklyColors.Muted
            val source = remember { MutableInteractionSource() }
            Column(
                Modifier
                    .weight(1f)
                    .pressScale(source)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSelected) DrinklyColors.Nebel else Color.Transparent)
                    .selectable(isSelected, interactionSource = source, indication = ripple(), role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Text(
                    tab.label, style = DrinklyType.Caption, color = color,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}
