package dev.shushant.kourier.scenarios

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ScenarioDelayTest {
    private val scenario = Scenario("s", "Delay", listOf(ScenarioRule("r", RequestMatcher("GET", "/", "api"), ScenarioAction.HttpResponse(503), delayMs = 1000)))
    private val request = ScenarioRequest("api", "GET", "/")

    @Test fun delayUsesSuspensionAndWaitsUntilDeadline() = runTest {
        val engine = ScenarioEngine(); engine.load(scenario); engine.enable()
        val decision = assertNotNull(engine.decide(request))
        val wait = async { engine.awaitDelay(decision) }; runCurrent()
        advanceTimeBy(999); assertFalse(wait.isCompleted)
        advanceTimeBy(1); runCurrent(); assertTrue(wait.await())
        assertEquals(1000, testScheduler.currentTime.toInt())
    }

    @Test fun disableInterruptsWaitingAndInvalidatesAssignedFault() = runTest {
        val engine = ScenarioEngine(); engine.load(scenario); engine.enable()
        val decision = assertNotNull(engine.decide(request))
        val wait = async { engine.awaitDelay(decision) }; runCurrent()
        advanceTimeBy(50); engine.disable(); runCurrent()
        assertFalse(wait.await()); assertEquals(50, testScheduler.currentTime.toInt())
        engine.enable(); assertFalse(engine.isCurrent(decision))
    }

    @Test fun resetAndValidReplacementInterruptWaiting() = runTest {
        for (reset in listOf(true, false)) {
            val engine = ScenarioEngine(); engine.load(scenario); engine.enable()
            val decision = assertNotNull(engine.decide(request))
            val wait = async { engine.awaitDelay(decision) }; runCurrent()
            if (reset) engine.reset() else engine.load(scenario)
            runCurrent(); assertFalse(wait.await())
        }
    }

    @Test fun invalidReplacementDoesNotInterruptAndHostCancellationPropagates() = runTest {
        val engine = ScenarioEngine(); engine.load(scenario); engine.enable()
        val decision = assertNotNull(engine.decide(request))
        val wait = async { engine.awaitDelay(decision) }; runCurrent()
        assertTrue(engine.load(scenario.copy(schemaVersion = 2)).isNotEmpty())
        assertFalse(wait.isCompleted)
        wait.cancelAndJoin(); assertTrue(wait.isCancelled)
        assertTrue(engine.isCurrent(decision))
    }
}
