package dev.shushant.kourier.scenarios

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Adapters supply parsed, normalized values. Raw bodies and headers are never retained here. */
data class ScenarioRequest(
    val hostAlias: String,
    val method: String,
    val path: String,
    val graphqlOperation: String? = null,
    val query: List<Pair<String, String>> = emptyList(),
    val headers: List<Pair<String, String>> = emptyList()
)

data class ScenarioDecision(
    val scenarioId: String,
    val ruleId: String,
    val occurrence: Long,
    val action: ScenarioAction,
    val delayMs: Long,
    val fixture: ResponseFixture?,
    val executionVersion: Long = 0
)

/** Per-process engine. Loading does not activate faults. Decision assignment is atomic. */
class ScenarioEngine {
    private val mutex = Mutex()
    private var loaded: Scenario? = null
    private val activation = MutableStateFlow(false)
    private var enabled: Boolean
        get() = activation.value
        set(value) { activation.value = value }
    private val executionVersion = MutableStateFlow(0L)
    private var origins: Map<String, ScenarioOrigin> = emptyMap()
    private val counts = mutableMapOf<CounterKey, Long>()
    private data class CounterKey(val ruleId: String, val request: ScenarioRequest)

    suspend fun load(scenario: Scenario, hostBindings: Map<String, String> = emptyMap()): List<String> {
        // Detach caller-owned collections before validating and retaining the candidate.
        val candidate = scenario.copy(rules = scenario.rules.map { it.copy(occurrence = it.occurrence.copy(sequence = it.occurrence.sequence.toList()), match = it.match.copy(query = it.match.query.toList(), headers = it.match.headers.toList())) }, fixtures = scenario.fixtures.toList())
        val errors = ScenarioValidation.errors(candidate).toMutableList()
        val bindings = hostBindings.toMap()
        val parsed = mutableMapOf<String, ScenarioOrigin>()
        for ((alias, origin) in bindings) {
            val url = ScenarioUrl.parse(origin)
            if (url == null || url.path != "/" || url.query.isNotEmpty() || '?' in origin || '#' in origin) errors.add("Invalid host binding")
            else parsed[alias] = url.origin
        }
        val requiredAliases = candidate.rules.map { it.match.hostAlias }.toSet()
        if (bindings.isNotEmpty() && bindings.keys != requiredAliases) errors.add("Host bindings must match scenario aliases")
        if (parsed.values.distinct().size != parsed.size) errors.add("Ambiguous host bindings")
        if (errors.isNotEmpty()) return errors
        mutex.withLock {
            loaded = candidate.copy(rules = candidate.rules.sortedWith(compareByDescending<ScenarioRule> { it.priority }.thenBy { it.id }))
            enabled = false
            origins = parsed.toMap()
            counts.clear()
            invalidateDecisions()
        }
        return emptyList()
    }

    suspend fun enable(): Boolean = mutex.withLock {
        val wasEnabled = enabled
        enabled = loaded != null
        if (enabled != wasEnabled) invalidateDecisions()
        enabled
    }

    suspend fun disable() = mutex.withLock { enabled = false; invalidateDecisions() }
    fun isActive(): Boolean = activation.value
    suspend fun isEnabled(): Boolean = mutex.withLock { enabled }
    suspend fun reset() = mutex.withLock { counts.clear(); invalidateDecisions() }

    /** False means the pending fault was invalidated; the adapter should forward normally. */
    suspend fun awaitDelay(decision: ScenarioDecision): Boolean {
        if (!isCurrent(decision)) return false
        if (decision.delayMs > 0) {
            withTimeoutOrNull(decision.delayMs) { executionVersion.first { it != decision.executionVersion } }
        }
        return isCurrent(decision)
    }

    suspend fun isCurrent(decision: ScenarioDecision): Boolean = mutex.withLock {
        enabled && executionVersion.value == decision.executionVersion
    }

    private fun invalidateDecisions() { executionVersion.value = executionVersion.value + 1L }


    suspend fun decide(request: ScenarioRequest): ScenarioDecision? = mutex.withLock { decideLocked(request) }

    /** Real adapter entry point: unrelated origins and invalid URLs always forward unchanged. */
    suspend fun decideUrl(
        url: String,
        method: String,
        headers: List<Pair<String, String>> = emptyList(),
        graphqlOperation: String? = null
    ): ScenarioDecision? {
        val parsed = ScenarioUrl.parse(url) ?: return null
        return mutex.withLock {
            val alias = origins.entries.firstOrNull { it.value == parsed.origin }?.key ?: return@withLock null
            decideLocked(ScenarioRequest(alias, method.uppercase(), parsed.path, graphqlOperation, parsed.query, headers))
        }
    }

    private fun decideLocked(request: ScenarioRequest): ScenarioDecision? {
        if (!enabled) return null
        val scenario = loaded ?: return null
        // Query ordering cannot create a new occurrence identity; duplicate keys are preserved.
        val identity = request.copy(query = request.query.sortedWith(compareBy<Pair<String, String>> { it.first }.thenBy { it.second }), headers = emptyList())
        for (rule in scenario.rules) {
            if (!ScenarioMatching.matches(rule.match, request)) continue
            val key = CounterKey(rule.id, identity)
            // Never evict counters and accidentally repeat a first-occurrence fault.
            if (key !in counts && counts.size >= MAX_COUNTERS) continue
            val previous = counts[key] ?: 0L
            val occurrence = if (previous == Long.MAX_VALUE) previous else previous + 1L
            counts[key] = occurrence
            val first = rule.occurrence.first
            val every = rule.occurrence.every
            if (first != null && occurrence > first) continue
            if (every != null && occurrence % every != 0L) continue
            if (rule.fallthrough) continue
            val sequence = rule.occurrence.sequence
            if (sequence.isNotEmpty() && occurrence > sequence.size) continue
            val action = if (sequence.isEmpty()) rule.action else sequence[(occurrence - 1L).toInt()]
            val fixture = if (action is ScenarioAction.HttpResponse) scenario.fixtures.firstOrNull { it.id == action.fixtureId } else null
            return ScenarioDecision(scenario.id, rule.id, occurrence, action, rule.delayMs, fixture, executionVersion.value)
        }
        return null
    }

    companion object { const val MAX_COUNTERS = 4096 }
}
