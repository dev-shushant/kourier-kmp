package dev.shushant.kourier.core

import dev.shushant.kourier.scenarios.ScenarioDecision
import dev.shushant.kourier.scenarios.ScenarioEngine
import dev.shushant.kourier.scenarios.ScenarioAction

/** Development-only runtime. Host-facing activation/production packaging is not finalized. */
object ResilienceRuntime {
    val engine = ScenarioEngine()

    fun evidence(decision: ScenarioDecision, adapter: String): Map<String, String> = mapOf(
        "resilience.source" to "injected",
        "resilience.scenarioId" to decision.scenarioId,
        "resilience.ruleId" to decision.ruleId,
        "resilience.occurrence" to decision.occurrence.toString(),
        "resilience.adapter" to adapter,
        "resilience.delayMs" to decision.delayMs.toString(),
        "resilience.action" to when (decision.action) {
            is ScenarioAction.HttpResponse -> "httpResponse"
            ScenarioAction.Forward -> "forward"
            ScenarioAction.Timeout -> "timeout"
            ScenarioAction.Disconnect -> "disconnect"
        }
    )
}
