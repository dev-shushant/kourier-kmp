package dev.shushant.kourier.scenarios

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** Strict bounded JSON with duplicate-key rejection before deserialization. */
object BoundedJson {
    private val json = Json { isLenient = false }

    fun parse(text: String, maxBytes: Int): JsonElement? {
        if (text.length > maxBytes || text.encodeToByteArray().size > maxBytes) return null
        return try {
            Scanner(text).validate()
            json.parseToJsonElement(text)
        } catch (_: Exception) { null }
    }

    private class Scanner(private val text: String) {
        private var index = 0
        private var values = 0
        fun validate() { value(0); whitespace(); require(index == text.length) }
        private fun whitespace() { while (index < text.length && text[index] in " \t\r\n") index++ }
        private fun expect(char: Char) { whitespace(); require(index < text.length && text[index++] == char) }
        private fun consume(char: Char): Boolean { whitespace(); return if (index < text.length && text[index] == char) { index++; true } else false }
        private fun string(): String {
            whitespace()
            val start = index
            expect('"')
            while (index < text.length) {
                val char = text[index++]
                require(char.code >= 0x20)
                when (char) {
                    '\\' -> { require(index < text.length); index++ }
                    '"' -> return json.decodeFromString<String>(text.substring(start, index))
                }
            }
            error("Unterminated string")
        }
        private fun value(depth: Int) {
            require(depth <= 64 && ++values <= 50_000)
            whitespace(); require(index < text.length)
            when (text[index]) {
                '{' -> {
                    index++
                    val keys = mutableSetOf<String>()
                    if (consume('}')) return
                    do {
                        require(keys.add(string()))
                        expect(':'); value(depth + 1)
                    } while (consume(','))
                    expect('}')
                }
                '[' -> {
                    index++
                    if (consume(']')) return
                    do { value(depth + 1) } while (consume(','))
                    expect(']')
                }
                '"' -> { string() }
                else -> {
                    val start = index
                    while (index < text.length && text[index] !in " \t\r\n,]}") index++
                    require(index > start)
                    val token = text.substring(start, index)
                    require(token in listOf("true", "false", "null") || Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?").matches(token))
                }
            }
        }
    }
}
