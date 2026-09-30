package dev.shushant.kourier.scenarios

enum class DraftAction { HTTP_RESPONSE, DELAY, TIMEOUT, DISCONNECT }

data class ScenarioDraft(
    val name: String,
    val targetUrl: String,
    val method: String = "GET",
    val graphqlOperation: String = "",
    val action: DraftAction = DraftAction.HTTP_RESPONSE,
    val status: String = "503",
    val delayMs: String = "0",
    val firstOccurrences: String = "1"
)

data class ScenarioDraftResult(
    val scenario: Scenario? = null,
    val hostBindings: Map<String, String> = emptyMap(),
    val errors: List<String> = emptyList()
)

/** Editor input is validated before replacing scenario state; origins stay in local bindings. */
object ScenarioDraftCompiler {
    fun compile(draft: ScenarioDraft, id: String): ScenarioDraftResult {
        val errors = mutableListOf<String>()
        val url = ScenarioUrl.parse(draft.targetUrl.trim())
        if (url == null) errors.add("Enter a valid HTTP or HTTPS target without credentials.")
        val delay = draft.delayMs.trim().toLongOrNull()
        if (delay == null || delay !in 0..ScenarioValidation.MAX_DELAY_MS) errors.add("Delay must be between 0 and 30000 milliseconds.")
        if (draft.action == DraftAction.DELAY && delay == 0L) errors.add("Choose a positive delay.")
        val occurrenceText = draft.firstOccurrences.trim()
        val first = occurrenceText.toIntOrNull()
        if (occurrenceText.isNotEmpty() && (first == null || first <= 0)) errors.add("Occurrences must be positive; leave blank for every match.")
        val status = draft.status.trim().toIntOrNull()
        if (draft.action == DraftAction.HTTP_RESPONSE && (status == null || status !in 200..599)) errors.add("HTTP status must be between 200 and 599.")
        if (errors.isNotEmpty() || url == null || delay == null) return ScenarioDraftResult(errors = errors)
        val action = when (draft.action) {
            DraftAction.HTTP_RESPONSE -> ScenarioAction.HttpResponse(status!!)
            DraftAction.DELAY -> ScenarioAction.Forward
            DraftAction.TIMEOUT -> ScenarioAction.Timeout
            DraftAction.DISCONNECT -> ScenarioAction.Disconnect
        }
        val scenario = Scenario(id, draft.name.trim(), listOf(ScenarioRule(
            "rule-1", RequestMatcher(draft.method.trim().uppercase(), url.path, "target",
                graphqlOperation = draft.graphqlOperation.trim().ifEmpty { null }),
            action, occurrence = Occurrence(first = first), delayMs = delay
        )))
        val validation = ScenarioValidation.errors(scenario)
        if (validation.isNotEmpty()) return ScenarioDraftResult(errors = validation)
        val origin = url.origin
        val host = if (':' in origin.host) "[${origin.host}]" else origin.host
        return ScenarioDraftResult(scenario, mapOf("target" to "${origin.scheme}://$host:${origin.port}"))
    }
}
