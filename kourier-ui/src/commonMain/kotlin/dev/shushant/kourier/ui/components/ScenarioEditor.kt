package dev.shushant.kourier.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.window.Dialog
import dev.shushant.kourier.core.ResilienceRuntime
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.core.platform.PlatformUtils
import dev.shushant.kourier.scenarios.*
import dev.shushant.kourier.ui.theme.KourierTheme
import dev.shushant.kourier.ui.theme.LocalKourierColors
import kotlinx.coroutines.launch

/** A captured endpoint is selected explicitly; query strings never enter the draft. */
@Composable
fun ScenarioEditor(transactions: List<HttpTransaction>, onDismiss: () -> Unit, onLoaded: (String) -> Unit) {
    val endpoints = remember(transactions) {
        transactions.map { it.request.method to it.request.url.substringBefore('?').substringBefore('#') }.distinct()
    }
    var draft by remember { mutableStateOf(ScenarioDraft("API fault test", "", "GET")) }
    var choosing by remember { mutableStateOf(true) }
    var manual by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var advanced by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(emptyList<String>()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    ScenarioDialog("Create scenario", onDismiss, footer = {
        TextButton(onClick = onDismiss) { Text("Cancel") }
        Button(enabled = !loading && draft.targetUrl.isNotBlank() && !choosing, onClick = {
            val result = ScenarioDraftCompiler.compile(draft, PlatformUtils.randomUuid())
            errors = result.errors
            val scenario = result.scenario
            if (scenario != null) scope.launch {
                loading = true
                try {
                    errors = ResilienceRuntime.engine.load(scenario, result.hostBindings)
                    if (errors.isEmpty()) onLoaded("${draft.method} ${draft.targetUrl.substringBefore('?').substringBefore('#')}\n${when (draft.action) { DraftAction.HTTP_RESPONSE -> "HTTP ${draft.status}"; DraftAction.DELAY -> "Delay ${draft.delayMs} ms"; DraftAction.TIMEOUT -> "Timeout"; DraftAction.DISCONNECT -> "Disconnect" }} · ${if (draft.firstOccurrences.isEmpty()) "Every matching request" else "Next request only"}")
                } finally { loading = false }
            }
        }) { Text(if (loading) "Saving…" else "Save scenario") }
    }) {
        if (choosing) {
            Text("Choose an endpoint", style = MaterialTheme.typography.subtitle1)
            Text("Use a request your app already made.", style = MaterialTheme.typography.body2)
            OutlinedTextField(search, { search = it }, label = { Text("Search host or path") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            val matches = endpoints.filter { (method, url) -> "$method $url".contains(search, ignoreCase = true) }
            if (endpoints.isEmpty()) Text("Make a request in your app, then return here to select it.")
            else if (matches.isEmpty()) Text("No matching endpoints.")
            matches.forEach { (method, url) ->
                ScenarioChoice("$method  $url", false) {
                    draft = draft.copy(targetUrl = url, method = method)
                    choosing = false
                    manual = false
                }
            }
            TextButton(onClick = { choosing = false; manual = true }) { Text("Enter an endpoint manually") }
        } else {
            Text("Endpoint", style = MaterialTheme.typography.subtitle1)
            if (manual) {
                OutlinedTextField(draft.targetUrl, { draft = draft.copy(targetUrl = it) }, label = { Text("https://api.example.com/path") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(draft.method, { draft = draft.copy(method = it) }, label = { Text("HTTP method") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            } else Text("${draft.method}  ${draft.targetUrl}", style = MaterialTheme.typography.body2)
            TextButton(onClick = { choosing = true }) { Text("Choose another endpoint") }
            Text("Matches this host, method and exact path. Query parameters are ignored.", style = MaterialTheme.typography.caption)
            Text("What should happen?", style = MaterialTheme.typography.subtitle1)
            listOf(
                DraftAction.HTTP_RESPONSE to "Return HTTP ${draft.status}",
                DraftAction.DELAY to "Slow response · ${draft.delayMs} ms delay",
                DraftAction.TIMEOUT to "Request times out",
                DraftAction.DISCONNECT to "Connection drops"
            ).forEach { (action, label) ->
                ScenarioChoice(label, draft.action == action) {
                    draft = draft.copy(action = action, delayMs = if (action == DraftAction.DELAY) "3000" else "0")
                }
            }
            Text("Repeat", style = MaterialTheme.typography.subtitle1)
            Row {
                TextButton(onClick = { draft = draft.copy(firstOccurrences = "1") }) { Text(if (draft.firstOccurrences == "1") "✓ Next request only" else "Next request only") }
                TextButton(onClick = { draft = draft.copy(firstOccurrences = "") }) { Text(if (draft.firstOccurrences.isEmpty()) "✓ Every request" else "Every request") }
            }
            TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "Hide custom settings" else "Custom settings") }
            if (advanced) {
                OutlinedTextField(draft.name, { draft = draft.copy(name = it) }, label = { Text("Scenario name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (draft.action == DraftAction.HTTP_RESPONSE) OutlinedTextField(draft.status, { draft = draft.copy(status = it) }, label = { Text("HTTP status") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(draft.delayMs, { draft = draft.copy(delayMs = it) }, label = { Text("Delay in milliseconds · maximum 30000") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(draft.graphqlOperation, { draft = draft.copy(graphqlOperation = it) }, label = { Text("GraphQL operation name (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            Text("Saved with faults disabled. Review it before enabling.", style = MaterialTheme.typography.caption)
        }
        errors.forEach { Text(it, color = MaterialTheme.colors.error) }
    }
}

@Composable
private fun ScenarioChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalKourierColors.current
    Row(Modifier.fillMaxWidth().border(1.dp, if (selected) MaterialTheme.colors.primary else colors.border, RoundedCornerShape(10.dp)).semantics { this.selected = selected }.clickable(role = Role.RadioButton, onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (selected) "✓  $label" else label, color = colors.textPrimary, style = MaterialTheme.typography.body2)
    }
}

/** Fixed actions and a bounded scrolling body, shared by Android and iOS. */
@Composable
internal fun ScenarioDialog(title: String, onDismiss: () -> Unit, footer: @Composable RowScope.() -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val dark = LocalKourierColors.current.isDark
    Dialog(onDismissRequest = onDismiss) {
        KourierTheme(darkTheme = dark) {
            val colors = LocalKourierColors.current
            Surface(shape = RoundedCornerShape(18.dp), color = colors.surface, contentColor = colors.textPrimary, modifier = Modifier.fillMaxWidth().border(1.dp, colors.border, RoundedCornerShape(18.dp))) {
                Column(Modifier.heightIn(max = 600.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(title, style = MaterialTheme.typography.h6)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, content = footer)
                }
            }
        }
    }
}
