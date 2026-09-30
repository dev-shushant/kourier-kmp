package dev.shushant.kourier.scenarios

internal object ScenarioMatching {
    fun matches(match: RequestMatcher, request: ScenarioRequest): Boolean {
        if (request.path.length > 2048 || match.hostAlias != request.hostAlias || match.method != request.method) return false
        val pathMatches = when (match.pathMode) {
            PathMatchMode.EXACT -> match.path == request.path
            PathMatchMode.WILDCARD -> wildcard(match.path, request.path)
        }
        return pathMatches && (match.graphqlOperation == null || match.graphqlOperation == request.graphqlOperation) &&
            match.query.all { predicate -> request.query.any { it.first == predicate.name && it.second == predicate.value } } &&
            match.headers.all { predicate -> request.headers.any { it.first.equals(predicate.name, ignoreCase = true) && it.second == predicate.value } }
    }

    /** '*' crosses path segments; backslash escapes the next character. No regex interpretation. */
    private fun wildcard(pattern: String, input: String): Boolean {
        val tokens = mutableListOf<Char?>()
        var i = 0
        while (i < pattern.length) {
            val char = pattern[i++]
            tokens.add(when {
                char == '\\' && i < pattern.length -> pattern[i++]
                char == '*' -> null
                else -> char
            })
        }
        var p = 0
        var s = 0
        var star = -1
        var restart = 0
        while (s < input.length) {
            when {
                p < tokens.size && tokens[p] == input[s] -> { p++; s++ }
                p < tokens.size && tokens[p] == null -> { star = p++; restart = s }
                star >= 0 -> { p = star + 1; s = ++restart }
                else -> return false
            }
        }
        while (p < tokens.size && tokens[p] == null) p++
        return p == tokens.size
    }
}
