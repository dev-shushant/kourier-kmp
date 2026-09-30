package dev.shushant.kourier.scenarios

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScenarioEngineTest {
    private val request = ScenarioRequest("api", "POST", "/sessions")
    private val rule = ScenarioRule("once", RequestMatcher("POST", "/sessions", "api"), ScenarioAction.HttpResponse(503), occurrence = Occurrence(first = 1))
    private fun scenario(rules: List<ScenarioRule> = listOf(rule)) = Scenario("test", "Test", rules)

    @Test fun disabledAndUnmatchedPathsDoNotConsumeOccurrences() = runTest {
        val engine = ScenarioEngine()
        assertFalse(engine.enable())
        assertEquals(emptyList(), engine.load(scenario()))
        assertNull(engine.decide(request))
        assertTrue(engine.enable())
        assertNull(engine.decide(request.copy(path = "/other")))
        assertEquals(1, engine.decide(request)?.occurrence?.toInt())
        assertNull(engine.decide(request))
        engine.reset()
        assertEquals(1, engine.decide(request)?.occurrence?.toInt())
        engine.disable()
        assertFalse(engine.isEnabled())
        assertNull(engine.decide(request))
    }

    @Test fun concurrentFirstOneHasExactlyOneWinner() = runTest {
        val engine = ScenarioEngine()
        engine.load(scenario()); engine.enable()
        val decisions = (1..100).map { async(Dispatchers.Default) { engine.decide(request) } }.awaitAll()
        assertEquals(1, decisions.count { it != null })
        assertEquals(1, decisions.filterNotNull().single().occurrence.toInt())
    }

    @Test fun invalidReplacementPreservesActiveScenario() = runTest {
        val engine = ScenarioEngine()
        engine.load(scenario()); engine.enable()
        assertTrue(engine.load(scenario().copy(schemaVersion = 9)).isNotEmpty())
        assertTrue(engine.isEnabled())
        assertEquals("test", engine.decide(request)?.scenarioId)
    }

    @Test fun priorityAndStableIdsOrderEligibleRules() = runTest {
        val engine = ScenarioEngine()
        val other = rule.copy(id = "a", action = ScenarioAction.HttpResponse(401))
        engine.load(scenario(listOf(rule, other))); engine.enable()
        assertEquals("a", engine.decide(request)?.ruleId)
        assertEquals("once", engine.decide(request)?.ruleId)
        engine.load(scenario(listOf(rule.copy(priority = 1), other))); engine.enable()
        assertEquals("once", engine.decide(request)?.ruleId)
    }

    @Test fun everyNthAndQueryOrderShareIdentity() = runTest {
        val engine = ScenarioEngine()
        engine.load(scenario(listOf(rule.copy(occurrence = Occurrence(every = 2))))); engine.enable()
        val first = request.copy(query = listOf("b" to "2", "a" to "1"))
        assertNull(engine.decide(first))
        assertEquals(2, engine.decide(first.copy(query = first.query.reversed()))?.occurrence?.toInt())
        assertNull(engine.decide(first))
        assertEquals(4, engine.decide(first)?.occurrence?.toInt())
    }

    @Test fun finiteSequenceExhaustsAndResets() = runTest {
        val engine = ScenarioEngine()
        val sequence = mutableListOf<ScenarioAction>(ScenarioAction.HttpResponse(503), ScenarioAction.Forward, ScenarioAction.Timeout)
        val ordered = rule.copy(action = ScenarioAction.Forward, occurrence = Occurrence(sequence = sequence))
        engine.load(scenario(listOf(ordered))); sequence.clear(); engine.enable()
        assertEquals(ScenarioAction.HttpResponse(503), engine.decide(request)?.action)
        assertEquals(ScenarioAction.Forward, engine.decide(request)?.action)
        assertEquals(ScenarioAction.Timeout, engine.decide(request)?.action)
        assertNull(engine.decide(request))
        engine.reset()
        assertEquals(ScenarioAction.HttpResponse(503), engine.decide(request)?.action)
    }

    @Test fun explicitFallthroughDiffersFromForward() = runTest {
        val engine = ScenarioEngine()
        val skip = rule.copy(id = "skip", priority = 10, action = ScenarioAction.Forward, occurrence = Occurrence(), fallthrough = true)
        engine.load(scenario(listOf(skip, rule))); engine.enable()
        assertEquals("once", engine.decide(request)?.ruleId)
        engine.load(scenario(listOf(skip.copy(fallthrough = false), rule))); engine.enable()
        assertEquals("skip", engine.decide(request)?.ruleId)
        assertEquals(ScenarioAction.Forward, engine.decide(request)?.action)
    }

    @Test fun graphqlOperationAndCounterLimitFailOpen() = runTest {
        val engine = ScenarioEngine()
        engine.load(scenario(listOf(rule.copy(match = rule.match.copy(graphqlOperation = "StartSession"))))); engine.enable()
        assertNull(engine.decide(request))
        assertNull(engine.decide(request.copy(graphqlOperation = "OtherOperation")))
        val matched = request.copy(graphqlOperation = "StartSession")
        repeat(ScenarioEngine.MAX_COUNTERS) { index ->
            assertEquals(1, engine.decide(matched.copy(query = listOf("id" to index.toString())))?.occurrence?.toInt())
        }
        assertNull(engine.decide(matched.copy(query = listOf("id" to "overflow"))))
        assertNull(engine.decide(matched.copy(query = listOf("id" to "0"))))
        engine.reset()
        assertEquals(1, engine.decide(matched.copy(query = listOf("id" to "overflow")))?.occurrence?.toInt())
    }

    @Test fun newLoadIsDisabledAndDetachedFromCaller() = runTest {
        val engine = ScenarioEngine()
        val rules = mutableListOf(rule)
        engine.load(scenario(rules)); rules.clear()
        assertFalse(engine.isEnabled()); engine.enable()
        assertEquals("once", engine.decide(request)?.ruleId)
        engine.load(scenario())
        assertFalse(engine.isEnabled())
        engine.enable()
        assertEquals(1, engine.decide(request)?.occurrence?.toInt())
    }
}
