package dev.shushant.kourier.scenarios

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScenarioBindingTest {
    private val scenario = Scenario("bound", "Bound", listOf(ScenarioRule("r", RequestMatcher("POST", "/sessions", "api"), ScenarioAction.HttpResponse(503), occurrence = Occurrence(first = 1))))

    @Test fun originMatchingIsNormalizedAndBounded() = runTest {
        val engine = ScenarioEngine()
        assertEquals(emptyList(), engine.load(scenario, mapOf("api" to "https://API.EXAMPLE:443")))
        engine.enable()
        assertNull(engine.decideUrl("https://other.example/sessions", "POST"))
        assertNull(engine.decideUrl("http://api.example/sessions", "POST"))
        assertNull(engine.decideUrl("https://api.example:444/sessions", "POST"))
        assertEquals("r", engine.decideUrl("https://api.example/sessions", "post")?.ruleId)
        assertNull(engine.decideUrl("https://api.example/sessions", "POST"))
    }

    @Test fun badBindingsCannotReplaceActiveState() = runTest {
        val engine = ScenarioEngine()
        engine.load(scenario, mapOf("api" to "https://api.example")); engine.enable()
        for (binding in listOf("https://user:secret@api.example", "https://api.example/private", "https://api.example?token=secret", "https://api.example#fragment")) {
            assertTrue(engine.load(scenario, mapOf("api" to binding)).isNotEmpty())
        }
        assertTrue(engine.load(scenario, mapOf("other" to "https://api.example")).isNotEmpty())
        assertEquals("r", engine.decideUrl("https://api.example/sessions", "POST")?.ruleId)
    }

    @Test fun ambiguousOriginsAndAbsentBindingsFailOpen() = runTest {
        val engine = ScenarioEngine()
        val two = scenario.copy(rules = scenario.rules + scenario.rules.single().copy(id = "s", match = scenario.rules.single().match.copy(hostAlias = "second")))
        assertTrue(engine.load(two, mapOf("api" to "https://api.example", "second" to "https://API.EXAMPLE:443")).isNotEmpty())
        engine.load(scenario); engine.enable()
        assertNull(engine.decideUrl("https://api.example/sessions", "POST"))
    }
}
