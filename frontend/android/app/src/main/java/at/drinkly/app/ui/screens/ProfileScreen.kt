package at.drinkly.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.drinkly.app.bottle.BottleLink
import at.drinkly.app.data.AppSettings
import at.drinkly.app.data.Hydration
import at.drinkly.app.ui.Dashboard
import at.drinkly.app.ui.components.BottleIllustration
import at.drinkly.app.ui.components.ScreenTitle
import at.drinkly.app.ui.components.SectionCard
import at.drinkly.app.ui.theme.DrinklyColors
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
            .padding(horizontal = 20.dp)
            .padding(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Profile")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(DrinklyColors.Blue),
                contentAlignment = Alignment.Center,
            ) {
                Text(s.username.take(2).uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(s.username, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
                Text("${d.streak}-day streak · ${s.goalMl} ml goal", color = DrinklyColors.Muted)
            }
        }

        SectionCard("Name") {
            CommitTextField(s.username, onCommit = { name -> onUpdate { it.copy(username = name) } })
            Spacer(Modifier.height(6.dp))
            Text("Your drinks are stored under this name in the backend.", fontSize = 13.sp, color = DrinklyColors.Muted)
        }

        SectionCard("Daily goal") {
            Text("${s.goalMl} ml", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Blue)
            Slider(
                value = s.goalMl.toFloat(),
                onValueChange = { v -> onUpdate { it.copy(goalMl = (v / 50).roundToInt() * 50) } },
                valueRange = 1000f..4000f,
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = DrinklyColors.Line)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Body weight", Modifier.weight(1f), color = DrinklyColors.Ink)
                FilledTonalIconButton(
                    onClick = { onUpdate { it.copy(weightKg = (it.weightKg - 1).coerceAtLeast(30)) } },
                ) { Icon(Icons.Rounded.Remove, contentDescription = "Less weight") }
                Text(
                    "${s.weightKg} kg", Modifier.width(72.dp), textAlign = TextAlign.Center,
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink,
                )
                FilledTonalIconButton(
                    onClick = { onUpdate { it.copy(weightKg = (it.weightKg + 1).coerceAtMost(200)) } },
                ) { Icon(Icons.Rounded.Add, contentDescription = "More weight") }
            }
            val suggested = Hydration.suggestedGoal(s.weightKg)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Suggested: $suggested ml", Modifier.weight(1f), fontSize = 13.sp, color = DrinklyColors.Muted)
                TextButton(
                    onClick = { onUpdate { it.copy(goalMl = suggested.coerceIn(1000, 4000)) } },
                    enabled = suggested != s.goalMl,
                ) { Text("Use as goal") }
            }
        }

        SectionCard("Bottle") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BottleIllustration(Modifier.size(width = 28.dp, height = 70.dp), dimmed = d.bottle.link != BottleLink.CONNECTED)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.bottle.name, fontWeight = FontWeight.SemiBold, color = DrinklyColors.Ink)
                    Text(
                        when (d.bottle.link) {
                            BottleLink.CONNECTED -> "Paired · battery ${d.bottle.batteryPercent ?: 0}%"
                            BottleLink.CONNECTING -> "Pairing…"
                            BottleLink.DISCONNECTED -> "Not connected"
                        },
                        fontSize = 13.sp, color = DrinklyColors.Muted,
                    )
                }
                Button(onClick = onToggleBottle) {
                    Text(if (d.bottle.link == BottleLink.DISCONNECTED) "Connect" else "Disconnect")
                }
            }
            if (d.bottle.link == BottleLink.CONNECTED) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { onSimulateSip(250) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Simulate a 250 ml sip")
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Demo mode: the bottle is simulated until its Bluetooth profile is available.",
                fontSize = 12.sp, color = DrinklyColors.Muted,
            )
        }

        SectionCard("Server") {
            CommitTextField(s.backendUrl, keyboardType = KeyboardType.Uri, onCommit = { url -> onUpdate { it.copy(backendUrl = url) } })
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (color, text) = when {
                    d.sync.error != null -> Color(0xFFD9572B) to "Not reachable"
                    d.sync.live -> Color(0xFF169C86) to "Live connection"
                    else -> DrinklyColors.Muted to "Connecting…"
                }
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(8.dp))
                Text(text, fontSize = 13.sp, color = DrinklyColors.Muted)
            }
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
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (changed) onCommit(text.trim()) }),
        trailingIcon = {
            if (changed) {
                IconButton(onClick = { onCommit(text.trim()) }) {
                    Icon(Icons.Rounded.Check, contentDescription = "Save", tint = DrinklyColors.Blue)
                }
            }
        },
    )
}
