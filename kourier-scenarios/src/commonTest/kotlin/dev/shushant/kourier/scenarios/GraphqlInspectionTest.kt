package dev.shushant.kourier.scenarios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GraphqlInspectionTest {
    @Test fun extractsOnlyExplicitOperationMetadata() {
        assertEquals(GraphqlInspection("StartSession"), GraphqlInspector.request("""{"operationName":"StartSession","variables":{"token":"secret"}}"""))
        assertEquals(GraphqlInspection(), GraphqlInspector.request("""{"query":"query Unnamed { test }"}"""))
        assertEquals(GraphqlInspection(), GraphqlInspector.request("""{"operationName":null}"""))
        for (body in listOf("broken", "[]", "{\"operationName\":42}", "{\"operationName\":\"bad name\"}")) assertTrue(GraphqlInspector.request(body).parseFailed)
    }

    @Test fun identifiesErrorsIndependentOfHttpStatus() {
        assertEquals(2, GraphqlInspector.response("""{"data":null,"errors":[{"message":"secret"},{"message":"other"}]}""").errorCount)
        assertEquals(GraphqlInspection(), GraphqlInspector.response("""{"data":{"ok":true}}"""))
        assertTrue(GraphqlInspector.response("""{"errors":"bad"}""").parseFailed)
    }

    @Test fun boundsBytesAndNesting() {
        assertTrue(GraphqlInspector.request("{\"padding\":\"" + "é".repeat(32768) + "\"}").parseFailed)
        assertTrue(GraphqlInspector.request("[".repeat(65) + "0" + "]".repeat(65)).parseFailed)
        assertEquals(GraphqlInspection("Safe"), GraphqlInspector.request("""{"operationName":"Safe","variables":{"text":"{{{{\\\""}}"""))
    }
}
