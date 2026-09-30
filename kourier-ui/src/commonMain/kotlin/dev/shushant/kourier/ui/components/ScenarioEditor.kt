package dev.shushant.kourier.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.ResilienceRuntime
import dev.shushant.kourier.core.platform.PlatformUtils
import dev.shushant.kourier.scenarios.*
import dev.shushant.kourier.ui.theme.LocalKourierColors
import kotlinx.coroutines.launch

@Composable
fun ScenarioEditor(targetUrl: String, method: String, onDismiss: () -> Unit, onLoaded: () -> Unit) {
    val colors = LocalKourierColors.current
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf(ScenarioDraft("API fault test", targetUrl.substringBefore('?').substringBefore('#'), method)) }
    var errors by remember { mutableStateOf(emptyList<String>()) }
    var loading by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create scenario") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(draft.name, { draft = draft.copy(name = it) }, label = { Text("Scenario name") }, singleLine = true)
                OutlinedTextField(draft.targetUrl, { draft = draft.copy(targetUrl = it) }, label = { Text("Target URL") }, singleLine = true)
                OutlinedTextField(draft.method, { draft = draft.copy(method = it) }, label = { Text("HTTP method") }, singleLine = true)
                OutlinedTextField(draft.graphqlOperation, { draft = draft.copy(graphqlOperation = it) }, label = { Text("GraphQL operation (optional)") }, singleLine = true)
                Text("Matches this origin, method and exact path. Query parameters are ignored.")
                val choices = listOf(
                    DraftAction.HTTP_RESPONSE to "Return HTTP status",
                    DraftAction.DELAY to "Delay then forward",
                    DraftAction.TIMEOUT to "Timeout",
                    DraftAction.DISCONNECT to "Disconnect"
                )
                choices.forEach { (action, label) ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(draft.action == action, onClick = { draft = draft.copy(action = action, delayMs = if (action == DraftAction.DELAY && draft.delayMs == "0") "3000" else draft.delayMs) })
                        Text(label)
                    }
                }
                if (draft.action == DraftAction.HTTP_RESPONSE) OutlinedTextField(draft.status, { draft = draft.copy(status = it) }, label = { Text("HTTP status") }, singleLine = true)
                OutlinedTextField(draft.delayMs, { draft = draft.copy(delayMs = it) }, label = { Text("Delay in milliseconds (0–30000)") }, singleLine = true)
                OutlinedTextField(draft.firstOccurrences, { draft = draft.copy(firstOccurrences = it) }, label = { Text("First N occurrences (blank = every match)") }, singleLine = true)
                Text("Loading replaces the current scenario and disables faults. Enable it separately after reviewing the target.")
                errors.forEach { Text(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = !loading, onClick = {
                val result = ScenarioDraftCompiler.compile(draft, PlatformUtils.randomUuid())
                errors = result.errors
                val scenario = result.scenario
                if (scenario != null) scope.launch {
                    loading = true
                    try {
                        errors = ResilienceRuntime.engine.load(scenario, result.hostBindings)
                        if (errors.isEmpty()) onLoaded()
                    } finally { loading = false }
                }
            }) { Text("Load scenario") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        backgroundColor = colors.surface,
        contentColor = colors.textPrimary
    )
}
