package dev.shushant.kourier.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.ResilienceRuntime
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors
import kotlinx.coroutines.launch

/** Shared inspector controls; activation is explicit and never restored automatically. */
@Composable
fun ScenarioControls(targetUrl: String = "", method: String = "GET") {
    val engine = ResilienceRuntime.engine
    val state by engine.state.collectAsState()
    val scope = rememberCoroutineScope()
    val colors = LocalKourierColors.current
    var showDetails by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    if (showEditor) ScenarioEditor(targetUrl, method, onDismiss = { showEditor = false }, onLoaded = { showEditor = false; showDetails = true })
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            if (state.active) "Active faults · ${state.name}" else state.name?.let { "Scenario disabled · $it" } ?: "Faults disabled",
            style = KourierTypography.caption,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f).padding(top = 12.dp),
            maxLines = 2
        )
        if (state.active) TextButton(onClick = { scope.launch { engine.disable() } }) { Text("Disable all") }
        TextButton(onClick = { showDetails = true }) { Text("Scenarios") }
    }
    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text(state.name ?: "Scenarios") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showDetails = false; showEditor = true }) { Text("Create scenario") }
                    Text(if (state.id == null) "No scenario loaded." else "${state.ruleCount} rules · ${state.decisionCount} decisions")
                    if (state.id != null && !state.bindingsReady) Text("Map the scenario's host aliases before enabling faults.")
                    Text("Only configured clients are affected. Counters reset when the app restarts.")
                    if (state.counterCapacityReached) Text("Occurrence capacity reached. New request identities are forwarded normally. Reset the scenario to clear counters.")
                    if (state.id != null) {
                        TextButton(onClick = { scope.launch { engine.reset() } }) { Text("Reset scenario") }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = state.id != null && (state.active || state.bindingsReady),
                    onClick = { scope.launch { if (state.active) engine.disable() else engine.enable() } }
                ) { Text(if (state.active) "Disable all" else "Enable faults") }
            },
            dismissButton = { TextButton(onClick = { showDetails = false }) { Text("Close") } },
            backgroundColor = colors.surface,
            contentColor = colors.textPrimary
        )
    }
}
