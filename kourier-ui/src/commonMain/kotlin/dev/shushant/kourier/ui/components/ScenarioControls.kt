package dev.shushant.kourier.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.ResilienceRuntime
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors
import kotlinx.coroutines.launch

/** Shared inspector controls; activation is explicit and never restored automatically. */
@Composable
fun ScenarioControls(transactions: List<HttpTransaction> = emptyList()) {
    val engine = ResilienceRuntime.engine
    val state by engine.state.collectAsState()
    val scope = rememberCoroutineScope()
    val colors = LocalKourierColors.current
    var showDetails by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var savedTarget by remember { mutableStateOf<String?>(null) }
    if (showEditor) ScenarioEditor(
        transactions = transactions,
        onDismiss = { showEditor = false },
        onLoaded = { savedTarget = it; showEditor = false; showDetails = true })
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
        ScenarioDialog(state.name ?: "Scenarios", onDismiss = { showDetails = false }, footer = {
            TextButton(onClick = { showDetails = false }) { Text("Close") }
            if (state.id != null) Button(
                enabled = state.active || state.bindingsReady,
                onClick = { scope.launch { if (state.active) engine.disable() else engine.enable() } }
            ) { Text(if (state.active) "Disable all" else "Enable faults") }
        }) {
            Text(if (state.active) "Faults active" else "Faults disabled", style = MaterialTheme.typography.subtitle1)
            if (state.id == null) {
                Text("Choose an endpoint from captured traffic and decide which failure to test.")
                Button(onClick = { showDetails = false; showEditor = true }) { Text("Create scenario") }
            } else {
                savedTarget?.let { Text(it, style = MaterialTheme.typography.body2) }
                Text("${state.ruleCount} rules · ${state.decisionCount} decisions")
                if (!state.bindingsReady) Text("Map the scenario's host aliases before enabling faults.")
                Text("Only configured clients are affected. Counters reset when the app restarts.", style = MaterialTheme.typography.body2)
                if (state.counterCapacityReached) Text("Occurrence capacity reached. New request identities are forwarded normally. Reset to clear counters.")
                TextButton(onClick = { scope.launch { engine.reset() } }) { Text("Reset occurrence counters") }
                TextButton(onClick = { showDetails = false; showEditor = true }) { Text("Replace scenario") }
            }
        }
    }
}
