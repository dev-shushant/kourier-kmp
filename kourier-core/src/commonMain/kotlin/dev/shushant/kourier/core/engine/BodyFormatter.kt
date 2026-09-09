package dev.shushant.kourier.core.engine

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

enum class BodyFormat { JSON, XML, TEXT, BINARY }

data class FormattedBody(
    val formatted: String,
    val raw: String,
    val format: BodyFormat
)

object BodyFormatter {
    private val prettyJson = Json { prettyPrint = true }

    fun format(contentType: String?, raw: String): FormattedBody {
        val mediaType = contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        return when {
            mediaType.endsWith("/json") || mediaType.endsWith("+json") -> formatJson(raw)
            mediaType.endsWith("/xml") || mediaType.endsWith("+xml") || raw.trimStart().startsWith('<') -> formatXml(raw)
            mediaType.startsWith("image/") || mediaType == "application/octet-stream" ->
                FormattedBody(raw, raw, BodyFormat.BINARY)
            else -> FormattedBody(raw, raw, BodyFormat.TEXT)
        }
    }

    private fun formatJson(raw: String): FormattedBody = try {
        val element = Json.parseToJsonElement(raw)
        FormattedBody(prettyJson.encodeToString<JsonElement>(element), raw, BodyFormat.JSON)
    } catch (_: Exception) {
        FormattedBody(raw, raw, BodyFormat.TEXT)
    }

    private fun formatXml(raw: String): FormattedBody {
        if (!hasBalancedTags(raw)) return FormattedBody(raw, raw, BodyFormat.TEXT)
        val lines = raw.trim().replace(Regex(">\\s*<"), ">\n<").lines()
        var depth = 0
        val formatted = buildString {
            lines.forEachIndexed { index, source ->
                val line = source.trim()
                if (line.startsWith("</")) depth = (depth - 1).coerceAtLeast(0)
                if (index > 0) append('\n')
                repeat(depth) { append("  ") }
                append(line)
                val containsInlineClose = Regex("^<[^/!?][^>]*>.*</[^>]+>$").matches(line)
                if (line.startsWith('<') && !line.startsWith("</") && !line.startsWith("<?") &&
                    !line.startsWith("<!") && !line.endsWith("/>") && !containsInlineClose
                ) depth++
            }
        }
        return FormattedBody(formatted, raw, BodyFormat.XML)
    }

    private fun hasBalancedTags(raw: String): Boolean {
        val stack = mutableListOf<String>()
        val tags = Regex("<\\s*(/?)\\s*([A-Za-z_][A-Za-z0-9_.:-]*)(?:\\s[^>]*)?(/?)\\s*>")
        for (match in tags.findAll(raw)) {
            val closing = match.groupValues[1] == "/"
            val selfClosing = match.groupValues[3] == "/"
            val name = match.groupValues[2]
            when {
                selfClosing -> Unit
                closing && stack.lastOrNull() == name -> stack.removeAt(stack.lastIndex)
                closing -> return false
                else -> stack += name
            }
        }
        return stack.isEmpty() && tags.containsMatchIn(raw)
    }
}
