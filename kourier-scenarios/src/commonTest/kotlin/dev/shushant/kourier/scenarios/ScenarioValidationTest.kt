package dev.shushant.kourier.scenarios

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScenarioValidationTest {
    private val rule = ScenarioRule(
        id = "start-once",
        match = RequestMatcher("POST", "/charging/sessions", "chargingApi"),
        action = ScenarioAction.HttpResponse(503, "unavailable"),
        occurrence = Occurrence(first = 1)
    )
    private val scenario = Scenario(
        "charging-start", "Charging start fails once", listOf(rule),
        listOf(ResponseFixture("unavailable", "application/json", "{\"message\":\"Unavailable\"}"))
    )

    @Test fun validContractRoundTrips() {
        assertEquals(emptyList(), ScenarioValidation.errors(scenario))
        assertEquals(scenario, Json.decodeFromString<Scenario>(Json.encodeToString(scenario)))
    }

    @Test fun rejectsUnknownVersionAndDuplicateIds() {
        val errors = ScenarioValidation.errors(scenario.copy(schemaVersion = 2, rules = listOf(rule, rule)))
        assertTrue("Unsupported schemaVersion" in errors)
        assertTrue("Duplicate rule id" in errors)
        assertTrue("Duplicate fixture id" in ScenarioValidation.errors(scenario.copy(fixtures = scenario.fixtures + scenario.fixtures)))
    }

    @Test fun rejectsInvalidActionsAndReferences() {
        for (status in listOf(101, 199, 600)) {
            assertTrue("Invalid synthetic status" in errors(rule.copy(action = ScenarioAction.HttpResponse(status))))
        }
        assertTrue("Missing fixture" in errors(rule.copy(action = ScenarioAction.HttpResponse(200, "absent"))))
        for (status in listOf(204, 205, 304)) {
            assertTrue("Body prohibited for synthetic response" in errors(rule.copy(action = ScenarioAction.HttpResponse(status, "unavailable"))))
        }
        assertTrue("Body prohibited for synthetic response" in errors(rule.copy(match = rule.match.copy(method = "HEAD"))))
    }

    @Test fun occurrenceAndDelayAreBounded() {
        assertTrue("Delay exceeds limit" in errors(rule.copy(delayMs = -1)))
        assertTrue("Delay exceeds limit" in errors(rule.copy(delayMs = 30_001)))
        assertEquals(emptyList(), errors(rule.copy(delayMs = 30_000)))
        assertTrue("Invalid first occurrence" in errors(rule.copy(occurrence = Occurrence(first = 0))))
        assertTrue("Invalid every occurrence" in errors(rule.copy(occurrence = Occurrence(every = -1))))
        assertTrue("Conflicting occurrence modes" in errors(rule.copy(occurrence = Occurrence(first = 1, every = 2))))
    }

    @Test fun countsUtf8BytesRatherThanCharacters() {
        val oversized = scenario.fixtures.single().copy(body = "é".repeat(ScenarioValidation.MAX_FIXTURE_BYTES / 2 + 1))
        assertTrue("Fixture exceeds byte limit" in ScenarioValidation.errors(scenario.copy(fixtures = listOf(oversized))))
        assertEquals(emptyList(), ScenarioValidation.errors(scenario.copy(fixtures = listOf(oversized.copy(body = "é".repeat(ScenarioValidation.MAX_FIXTURE_BYTES / 2))))))
    }

    @Test fun rejectsMalformedMatchersAndUnboundedCollections() {
        for (path in listOf("relative", "/path?token=secret", "/path#fragment", "/line\nbreak")) {
            assertTrue("Invalid exact path" in errors(rule.copy(match = rule.match.copy(path = path))))
        }
        assertTrue("Invalid HTTP method" in errors(rule.copy(match = rule.match.copy(method = "post"))))
        assertTrue("Invalid host alias" in errors(rule.copy(match = rule.match.copy(hostAlias = "https://private.example"))))
        assertTrue("Invalid GraphQL operation" in errors(rule.copy(match = rule.match.copy(graphqlOperation = "bad operation"))))
        assertTrue("Invalid rule count" in ScenarioValidation.errors(scenario.copy(rules = emptyList())))
        val manyRules = (1..129).map { rule.copy(id = "rule-$it") }
        assertTrue("Invalid rule count" in ScenarioValidation.errors(scenario.copy(rules = manyRules)))
    }

    @Test fun validatesSequenceActionsAndFallthrough() {
        assertTrue("Conflicting sequence mode" in errors(rule.copy(occurrence = Occurrence(sequence = listOf(ScenarioAction.Timeout)))))
        val sequenceRule = rule.copy(action = ScenarioAction.Forward, occurrence = Occurrence(sequence = listOf(ScenarioAction.HttpResponse(600))))
        assertTrue("Invalid synthetic status" in errors(sequenceRule))
        assertTrue("Sequence exceeds limit" in errors(sequenceRule.copy(occurrence = Occurrence(sequence = List(129) { ScenarioAction.Forward }))))
        assertTrue("Invalid fallthrough action" in errors(rule.copy(fallthrough = true)))
        assertEquals(emptyList(), errors(rule.copy(action = ScenarioAction.Forward, fallthrough = true)))
    }

    private fun errors(rule: ScenarioRule) = ScenarioValidation.errors(scenario.copy(rules = listOf(rule)))
}
