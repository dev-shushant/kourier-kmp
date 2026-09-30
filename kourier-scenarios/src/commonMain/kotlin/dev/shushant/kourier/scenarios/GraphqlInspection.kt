package dev.shushant.kourier.scenarios

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Metadata only. Variables, query text and error messages are never retained in this result. */
data class GraphqlInspection(val operationName: String? = null, val errorCount: Int = 0, val parseFailed: Boolean = false)

object GraphqlInspector {
    const val MAX_BODY_BYTES = 64 * 1024
    private val operationPattern = Regex("[_A-Za-z][_0-9A-Za-z]{0,127}")

    fun request(body: String?): GraphqlInspection {
        val objectValue = parseObject(body) ?: return GraphqlInspection(parseFailed = true)
        val value = objectValue["operationName"] ?: return GraphqlInspection()
        if (value.toString() == "null") return GraphqlInspection()
        val primitive = value as? JsonPrimitive ?: return GraphqlInspection(parseFailed = true)
        val name = primitive.content
        if (!primitive.isString || !operationPattern.matches(name)) return GraphqlInspection(parseFailed = true)
        return GraphqlInspection(operationName = name)
    }

    fun response(body: String?): GraphqlInspection {
        val objectValue = parseObject(body) ?: return GraphqlInspection(parseFailed = true)
        val value = objectValue["errors"] ?: return GraphqlInspection()
        if (value.toString() == "null") return GraphqlInspection()
        val errors = value as? JsonArray ?: return GraphqlInspection(parseFailed = true)
        if (errors.any { it !is JsonObject }) return GraphqlInspection(parseFailed = true)
        return GraphqlInspection(errorCount = errors.size)
    }

    private fun parseObject(body: String?): JsonObject? {
        if (body == null || body.length > MAX_BODY_BYTES || body.encodeToByteArray().size > MAX_BODY_BYTES) return null
        return BoundedJson.parse(body, MAX_BODY_BYTES) as? JsonObject
    }
}
