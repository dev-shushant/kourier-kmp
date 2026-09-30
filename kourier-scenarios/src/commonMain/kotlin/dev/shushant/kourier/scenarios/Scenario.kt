package dev.shushant.kourier.scenarios

import kotlinx.serialization.Serializable

/** Experimental R1 contract. No activation or networking occurs when constructing these values. */
@Serializable
data class Scenario(
    val id: String,
    val name: String,
    val rules: List<ScenarioRule>,
    val fixtures: List<ResponseFixture> = emptyList(),
    val schemaVersion: Int = 1,
    val description: String = ""
)

@Serializable
data class ScenarioRule(
    val id: String,
    val match: RequestMatcher,
    val action: ScenarioAction,
    val priority: Int = 0,
    val occurrence: Occurrence = Occurrence(),
    val delayMs: Long = 0,
    val fallthrough: Boolean = false
)

@Serializable
data class RequestMatcher(
    val method: String,
    val path: String,
    val hostAlias: String,
    val graphqlOperation: String? = null,
    val pathMode: PathMatchMode = PathMatchMode.EXACT,
    val query: List<ValuePredicate> = emptyList(),
    val headers: List<ValuePredicate> = emptyList()
)

/** Counts are per rule and normalized request identity, starting at one. */
@Serializable
data class Occurrence(
    val first: Int? = null,
    val every: Int? = null,
    val sequence: List<ScenarioAction> = emptyList()
)

@Serializable
sealed interface ScenarioAction {
    @Serializable data class HttpResponse(val status: Int, val fixtureId: String? = null) : ScenarioAction
    @Serializable data object Forward : ScenarioAction
    @Serializable data object Timeout : ScenarioAction
    @Serializable data object Disconnect : ScenarioAction
}

@Serializable
data class ResponseFixture(val id: String, val contentType: String, val body: String)

@Serializable
enum class PathMatchMode { EXACT, WILDCARD }

@Serializable
data class ValuePredicate(val name: String, val value: String)
