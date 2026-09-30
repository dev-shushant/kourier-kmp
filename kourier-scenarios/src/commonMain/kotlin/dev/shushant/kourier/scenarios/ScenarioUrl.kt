package dev.shushant.kourier.scenarios

/** Origin bindings stay local; exported scenarios contain aliases rather than private origins. */
data class ScenarioOrigin(val scheme: String, val host: String, val port: Int)
data class ScenarioUrl(val origin: ScenarioOrigin, val path: String, val query: List<Pair<String, String>>) {
    companion object {
        const val MAX_URL_LENGTH = 8192

        /** Strict HTTP subset. Invalid or unsupported input fails open at the adapter boundary. */
        fun parse(value: String): ScenarioUrl? {
            if (value.length > MAX_URL_LENGTH || value.any { it.isWhitespace() || it.code < 32 || it == '\\' }) return null
            val scheme = value.substringBefore("://", "").lowercase()
            if (scheme != "http" && scheme != "https") return null
            val rest = value.substringAfter("://")
            val authority = rest.takeWhile { it !in "/?#" }
            if (authority.isEmpty() || '@' in authority) return null
            val host: String
            val portText: String?
            if (authority.startsWith('[')) {
                val end = authority.indexOf(']')
                if (end < 0) return null
                host = authority.substring(1, end).lowercase()
                if (!validIpv6(host)) return null
                val suffix = authority.substring(end + 1)
                if (suffix.isNotEmpty() && !suffix.startsWith(':')) return null
                portText = if (suffix.isEmpty()) null else suffix.drop(1)
            } else {
                if (authority.count { it == ':' } > 1) return null
                host = authority.substringBefore(':').lowercase().removeSuffix(".")
                if (host.isEmpty() || host.any { it !in "abcdefghijklmnopqrstuvwxyz0123456789.-" } || host.split('.').any { it.isEmpty() || it.startsWith('-') || it.endsWith('-') }) return null
                portText = if (':' in authority) authority.substringAfter(':') else null
            }
            val port = if (portText == null) { if (scheme == "https") 443 else 80 } else {
                if (portText.isEmpty() || portText.any { !it.isDigit() }) return null
                portText.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            }
            val tail = rest.drop(authority.length).substringBefore('#')
            val rawPath = tail.substringBefore('?').ifEmpty { "/" }
            if (!rawPath.startsWith('/')) return null
            val path = normalizePath(rawPath) ?: return null
            val query = if ('?' !in tail || tail.substringAfter('?').isEmpty()) emptyList() else {
                val parts = tail.substringAfter('?').split('&')
                if (parts.size > 128) return null
                parts.map { part ->
                    val name = decode(part.substringBefore('=')) ?: return null
                    val valuePart = decode(part.substringAfter('=', "")) ?: return null
                    name to valuePart
                }
            }
            return ScenarioUrl(ScenarioOrigin(scheme, host, port), path, query)
        }

        private fun validIpv6(host: String): Boolean {
            if (host.windowed(2).count { it == "::" } > 1) return false
            val compressed = "::" in host
            val groups = host.split(':').filter { it.isNotEmpty() }
            if (groups.any { it.length !in 1..4 || it.any { char -> char.digitToIntOrNull(16) == null } }) return false
            return if (compressed) groups.size < 8 else groups.size == 8 && !host.startsWith(':') && !host.endsWith(':')
        }

        private fun normalizePath(raw: String): String? {
            if (raw.length > 2048) return null
            val escaped = StringBuilder()
            var i = 0
            while (i < raw.length) {
                val c = raw[i++]
                if (c == '%') {
                    if (i + 1 >= raw.length || raw[i].digitToIntOrNull(16) == null || raw[i + 1].digitToIntOrNull(16) == null) return null
                    escaped.append('%').append(raw[i++].uppercaseChar()).append(raw[i++].uppercaseChar())
                } else escaped.append(c)
            }
            // Encoded separators stay encoded; literal dot segments are resolved.
            val result = mutableListOf<String>()
            val segments = escaped.toString().drop(1).split('/')
            for ((index, segment) in segments.withIndex()) {
                when (segment) {
                    "." -> if (index == segments.lastIndex) result.add("")
                    ".." -> { if (result.isNotEmpty()) result.removeAt(result.lastIndex); if (index == segments.lastIndex) result.add("") }
                    else -> result.add(segment)
                }
            }
            return "/" + result.joinToString("/")
        }

        private fun decode(raw: String): String? {
            val bytes = mutableListOf<Byte>()
            var i = 0
            while (i < raw.length) {
                when (val c = raw[i++]) {
                    '%' -> {
                        if (i + 1 >= raw.length) return null
                        val hi = raw[i++].digitToIntOrNull(16) ?: return null
                        val lo = raw[i++].digitToIntOrNull(16) ?: return null
                        bytes.add((hi * 16 + lo).toByte())
                    }
                    '+' -> bytes.add(32)
                    else -> {
                        val end = if (c.isHighSurrogate() && i < raw.length && raw[i].isLowSurrogate()) i + 1 else i
                        bytes.addAll(raw.substring(i - 1, end).encodeToByteArray().toList()); i = end
                    }
                }
            }
            return try { bytes.toByteArray().decodeToString(throwOnInvalidSequence = true) } catch (_: Exception) { null }
        }
    }
}
