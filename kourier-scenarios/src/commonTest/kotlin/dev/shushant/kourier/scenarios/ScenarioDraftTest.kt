package dev.shushant.kourier.scenarios

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class ScenarioDraftTest {
    private val draft = ScenarioDraft("503 once", "https://private.example:443/start?token=secret", method = "post")

    @Test fun compileKeepsPrivateOriginAndQueryOutOfPortableScenario() {
        val result = ScenarioDraftCompiler.compile(draft, "local")
        assertEquals(emptyList(), result.errors)
        val scenario = assertNotNull(result.scenario)
        assertEquals("POST", scenario.rules.single().match.method)
        assertEquals("/start", scenario.rules.single().match.path)
        assertEquals(mapOf("target" to "https://private.example:443"), result.hostBindings)
        val portable = Json.encodeToString(scenario)
        assertFalse("private.example" in portable)
        assertFalse("secret" in portable)
        assertFalse("token" in portable)
    }

    @Test fun compiledScenarioLoadsDisabledAndRunsOnceAfterActivation() = runTest {
        val result = ScenarioDraftCompiler.compile(draft, "local")
        val engine = ScenarioEngine()
        assertEquals(emptyList(), engine.load(result.scenario!!, result.hostBindings))
        assertFalse(engine.isActive())
        assertNull(engine.decideUrl(draft.targetUrl, "POST"))
        engine.enable()
        assertEquals(ScenarioAction.HttpResponse(503), engine.decideUrl(draft.targetUrl, "POST")?.action)
        assertNull(engine.decideUrl(draft.targetUrl, "POST"))
        engine.reset()
        assertNotNull(engine.decideUrl(draft.targetUrl, "POST"))
    }

    @Test fun invalidNumbersNeverBecomeUnlimitedFaults() {
        for (value in listOf("oops", "0", "-1", "9999999999999999999")) {
            val result = ScenarioDraftCompiler.compile(draft.copy(firstOccurrences = value), "local")
            assertNull(result.scenario); assertTrue(result.errors.isNotEmpty())
        }
        assertEquals(emptyList(), ScenarioDraftCompiler.compile(draft.copy(firstOccurrences = ""), "local").errors)
        for (value in listOf("oops", "-1", "30001")) assertNull(ScenarioDraftCompiler.compile(draft.copy(delayMs = value), "local").scenario)
        for (value in listOf("oops", "101", "600")) assertNull(ScenarioDraftCompiler.compile(draft.copy(status = value), "local").scenario)
    }

    @Test fun allActionsAndOptionalOperationValidate() {
        for ((action, expected) in listOf(DraftAction.DELAY to ScenarioAction.Forward, DraftAction.TIMEOUT to ScenarioAction.Timeout, DraftAction.DISCONNECT to ScenarioAction.Disconnect)) {
            val result = ScenarioDraftCompiler.compile(draft.copy(action = action, delayMs = "1000", graphqlOperation = "StartSession"), "local")
            assertEquals(expected, result.scenario?.rules?.single()?.action)
            assertEquals("StartSession", result.scenario?.rules?.single()?.match?.graphqlOperation)
        }
        assertNull(ScenarioDraftCompiler.compile(draft.copy(action = DraftAction.DELAY), "local").scenario)
        assertNull(ScenarioDraftCompiler.compile(draft.copy(graphqlOperation = "bad operation"), "local").scenario)
        assertNull(ScenarioDraftCompiler.compile(draft.copy(targetUrl = "https://user:secret@private.example/"), "local").scenario)
        assertEquals(mapOf("target" to "http://[::1]:8080"), ScenarioDraftCompiler.compile(draft.copy(targetUrl = "http://[::1]:8080/test"), "local").hostBindings)
    }
}
