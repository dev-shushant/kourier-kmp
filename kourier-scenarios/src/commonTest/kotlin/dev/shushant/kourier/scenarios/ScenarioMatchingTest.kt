package dev.shushant.kourier.scenarios

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull

class ScenarioMatchingTest {
    private val request = ScenarioRequest("api", "GET", "/users/42")
    private val match = RequestMatcher("GET", "/users/*", "api", pathMode = PathMatchMode.WILDCARD)

    @Test fun wildcardIsExplicitAnchoredAndEscapable() {
        val cases = listOf(
            "/users/*" to "/users/42", "/users/*" to "/users/", "/users/*" to "/users/42/orders",
            "/u*s/4*" to "/users/42", "/literal/\\*" to "/literal/*", "/users/**" to "/users/42"
        )
        cases.forEach { (pattern, path) -> assertTrue(ScenarioMatching.matches(match.copy(path = pattern), request.copy(path = path)), "$pattern $path") }
        for (path in listOf("/other/users/42", "/users", "/Users/42")) assertFalse(ScenarioMatching.matches(match, request.copy(path = path)))
        assertFalse(ScenarioMatching.matches(match.copy(pathMode = PathMatchMode.EXACT), request))
        assertFalse(ScenarioMatching.matches(match.copy(path = "/literal/\\*"), request.copy(path = "/literal/anything")))
        assertTrue(ScenarioMatching.matches(match.copy(path = "/x/[a].+"), request.copy(path = "/x/[a].+")))
        assertFalse(ScenarioMatching.matches(match.copy(path = "/x/[a].+"), request.copy(path = "/x/aaa")))
    }

    @Test fun predicatesAreAndAcrossFieldsAnyAcrossDuplicateValues() {
        val guarded = match.copy(query = listOf(ValuePredicate("id", "42")), headers = listOf(ValuePredicate("X-Environment", "qa")))
        val input = request.copy(query = listOf("id" to "1", "id" to "42"), headers = listOf("x-environment" to "qa"))
        assertTrue(ScenarioMatching.matches(guarded, input))
        assertFalse(ScenarioMatching.matches(guarded, input.copy(headers = listOf("x-environment" to "QA"))))
        assertFalse(ScenarioMatching.matches(guarded, input.copy(query = listOf("ID" to "42"))))
        assertFalse(ScenarioMatching.matches(guarded, input.copy(hostAlias = "other")))
        assertFalse(ScenarioMatching.matches(guarded, input.copy(method = "POST")))
    }

    @Test fun rotatingHeadersDoNotChangeOccurrenceIdentity() = runTest {
        val engine = ScenarioEngine()
        engine.load(Scenario("once", "Once", listOf(ScenarioRule("r", match, ScenarioAction.HttpResponse(503), occurrence = Occurrence(first = 1)))))
        engine.enable()
        assertEquals(1, engine.decide(request.copy(headers = listOf("Authorization" to "first")))?.occurrence?.toInt())
        assertNull(engine.decide(request.copy(headers = listOf("Authorization" to "second"))))
    }

    @Test fun wildcardAndPredicateCostsAreBounded() {
        val rule = ScenarioRule("r", match.copy(path = "/" + "*".repeat(9)), ScenarioAction.Forward)
        assertTrue("Invalid wildcard path" in ScenarioValidation.errors(Scenario("s", "Test", listOf(rule))))
        val many = rule.copy(match = match.copy(query = List(33) { ValuePredicate("x", "y") }))
        assertTrue("Too many predicates" in ScenarioValidation.errors(Scenario("s", "Test", listOf(many))))
        assertFalse(ScenarioMatching.matches(match, request.copy(path = "/users/" + "x".repeat(2048))))
    }
}
