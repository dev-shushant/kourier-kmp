package dev.shushant.kourier.scenarios

import kotlinx.serialization.json.*
import kotlin.test.*

class FixtureSanitizerTest {
    @Test fun redactsNestedCompositeValuesAndPreservesSafeStructure() {
        val input = """{"password":{"nested":{"value":"secret"}},"rows":[{"TOKEN":["a","b"]},{"safe":"✓"}],"count":2}"""
        val result = assertNotNull(FixtureSanitizer.sanitizeJson(input))
        assertEquals(2, result.redactedFieldCount)
        val json = Json.parseToJsonElement(result.body).jsonObject
        assertEquals("••••••••", json["password"]?.jsonPrimitive?.content)
        assertFalse("secret" in result.body)
        assertFalse("\"a\"" in result.body)
        assertEquals("✓", json["rows"]?.jsonArray?.get(1)?.jsonObject?.get("safe")?.jsonPrimitive?.content)
        assertEquals(2, json["count"]?.jsonPrimitive?.int)
    }

    @Test fun customPolicyCannotRemoveBaselineProtection() {
        val result = assertNotNull(FixtureSanitizer.sanitizeJson("""{"CUSTOM":"private","password":123,"token":true,"secret":null}""", setOf("custom")))
        assertEquals(4, result.redactedFieldCount)
        assertFalse("private" in result.body)
        assertFalse(":123" in result.body)
        assertFalse(":true" in result.body)
        assertFalse(":null" in result.body)
    }

    @Test fun escapedDuplicateKeysAreRejectedRecursively() {
        for (body in listOf("""{"token":"a","token":"b"}""", """{"token":"a","\u0074oken":"b"}""", """{"safe":{"x":1,"x":2}}""")) {
            assertNull(FixtureSanitizer.sanitizeJson(body))
        }
        assertNotNull(FixtureSanitizer.sanitizeJson("""[{"x":1},{"x":2}]"""))
    }

    @Test fun boundedInputAndOutputRejectUnsafeCreation() {
        assertNull(FixtureSanitizer.sanitizeJson("{"))
        assertNull(FixtureSanitizer.sanitizeJson("not JSON"))
        assertNull(FixtureSanitizer.sanitizeJson("""{"x":"${"é".repeat(ScenarioValidation.MAX_FIXTURE_BYTES / 2)}"}"""))
        assertNull(FixtureSanitizer.sanitizeJson("[".repeat(65) + "0" + "]".repeat(65)))
        assertNull(FixtureSanitizer.sanitizeJson("""{"token":1}""", mask = "x".repeat(ScenarioValidation.MAX_FIXTURE_BYTES)))
    }

    @Test fun strictParserRejectsMalformedGrammarWithoutEchoingData() {
        for (body in listOf("{x:1}", "[1,]", "{\"a\":1,}", "true false", "{\"a\":01}", "{\"a\":\"bad\ntext\"}")) {
            assertNull(BoundedJson.parse(body, 1024), body)
        }
        for (body in listOf("{}", "[]", "null", "true", "123", "\"escaped\\\"quote\"", "{\"a\":1,\"b\":[true,false,null]}")) {
            assertNotNull(BoundedJson.parse(body, 1024), body)
        }
    }
}
