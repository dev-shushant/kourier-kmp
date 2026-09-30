package dev.shushant.kourier.scenarios

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScenarioStateTest {
    private val scenario = Scenario("s", "Scenario", listOf(ScenarioRule("r", RequestMatcher("GET", "/", "api"), ScenarioAction.HttpResponse(503))))

    @Test fun stateReflectsLoadActivationDecisionsResetAndDisable() = runTest {
        val engine = ScenarioEngine()
        assertEquals(ScenarioState(), engine.state.value)
        engine.load(scenario, mapOf("api" to "https://api.example"))
        assertEquals("Scenario", engine.state.value.name)
        assertTrue(engine.state.value.bindingsReady)
        assertFalse(engine.state.value.active)
        engine.enable(); assertTrue(engine.state.value.active)
        engine.decideUrl("https://api.example/", "GET")
        assertEquals(1, engine.state.value.decisionCount.toInt())
        engine.reset(); assertEquals(0, engine.state.value.decisionCount.toInt())
        assertTrue(engine.state.value.active)
        engine.disable(); assertFalse(engine.state.value.active)
        assertEquals("s", engine.state.value.id)
    }

    @Test fun invalidLoadCannotPublishNewStateAndUnboundStateIsExplicit() = runTest {
        val engine = ScenarioEngine()
        engine.load(scenario); assertFalse(engine.state.value.bindingsReady)
        engine.enable()
        val previous = engine.state.value
        engine.load(scenario.copy(name = "Invalid", schemaVersion = 9))
        assertEquals(previous, engine.state.value)
    }

    @Test fun capacityIsVisibleAndResetClearsWarning() = runTest {
        val engine = ScenarioEngine(); engine.load(scenario); engine.enable()
        repeat(ScenarioEngine.MAX_COUNTERS + 1) { i ->
            engine.decide(ScenarioRequest("api", "GET", "/", query = listOf("id" to i.toString())))
        }
        assertTrue(engine.state.value.counterCapacityReached)
        assertEquals(ScenarioEngine.MAX_COUNTERS, engine.state.value.decisionCount.toInt())
        engine.reset(); assertFalse(engine.state.value.counterCapacityReached)
    }
}
