package dev.shushant.kourier.scenarios

/** Validation is explicit so invalid input cannot replace the currently active scenario. */
object ScenarioValidation {
    const val MAX_RULES = 128
    const val MAX_FIXTURES = 64
    const val MAX_FIXTURE_BYTES = 256 * 1024
    const val MAX_DELAY_MS = 30_000L
    private val idPattern = Regex("[A-Za-z0-9_.-]{1,64}")
    private val methodPattern = Regex("[A-Z]{1,20}")
    private val operationPattern = Regex("[_A-Za-z][_0-9A-Za-z]{0,127}")

    fun errors(scenario: Scenario): List<String> = buildList {
        fun check(ok: Boolean, message: String) { if (!ok) add(message) }
        check(scenario.schemaVersion == 1, "Unsupported schemaVersion")
        check(idPattern.matches(scenario.id), "Invalid scenario id")
        check(scenario.name.isNotBlank() && scenario.name.length <= 200, "Invalid scenario name")
        check(scenario.description.length <= 4096, "Description exceeds limit")
        check(scenario.rules.size in 1..MAX_RULES, "Invalid rule count")
        check(scenario.fixtures.size <= MAX_FIXTURES, "Too many fixtures")
        check(scenario.rules.map { it.id }.distinct().size == scenario.rules.size, "Duplicate rule id")
        check(scenario.fixtures.map { it.id }.distinct().size == scenario.fixtures.size, "Duplicate fixture id")
        val fixtureIds = scenario.fixtures.map { it.id }.toSet()
        scenario.fixtures.forEach { fixture ->
            check(idPattern.matches(fixture.id), "Invalid fixture id")
            check(fixture.body.encodeToByteArray().size <= MAX_FIXTURE_BYTES, "Fixture exceeds byte limit")
            check(fixture.contentType.length in 1..128 && !fixture.contentType.any { it == '\r' || it == '\n' }, "Invalid fixture content type")
        }
        scenario.rules.forEach { rule ->
            check(idPattern.matches(rule.id), "Invalid rule id")
            check(methodPattern.matches(rule.match.method), "Invalid HTTP method")
            check(idPattern.matches(rule.match.hostAlias), "Invalid host alias")
            check(rule.match.path.startsWith('/') && rule.match.path.length <= 2048 &&
                !rule.match.path.any { it in "?#" || it.isWhitespace() || it.code < 32 }, "Invalid exact path")
            check(rule.match.pathMode == PathMatchMode.EXACT || (rule.match.path.length <= 256 && rule.match.path.count { it == '*' } <= 8 && !rule.match.path.endsWith("\\")), "Invalid wildcard path")
            (listOf(rule.match.query, rule.match.headers)).forEach { predicates ->
                check(predicates.size <= 32, "Too many predicates")
                predicates.forEach { predicate ->
                    check(predicate.name.length in 1..128 && predicate.value.length <= 512 &&
                        !predicate.name.any { it.isWhitespace() || it.code < 32 } &&
                        !predicate.value.any { it == '\r' || it == '\n' }, "Invalid value predicate")
                }
            }
            val operation = rule.match.graphqlOperation
            check(operation == null || operationPattern.matches(operation), "Invalid GraphQL operation")
            check(rule.delayMs in 0..MAX_DELAY_MS, "Delay exceeds limit")
            check(rule.occurrence.first == null || rule.occurrence.first > 0, "Invalid first occurrence")
            check(rule.occurrence.every == null || rule.occurrence.every > 0, "Invalid every occurrence")
            check(rule.occurrence.first == null || rule.occurrence.every == null, "Conflicting occurrence modes")
            check(rule.occurrence.sequence.size <= 128, "Sequence exceeds limit")
            check(rule.occurrence.sequence.isEmpty() || (rule.occurrence.first == null && rule.occurrence.every == null && rule.action == ScenarioAction.Forward), "Conflicting sequence mode")
            check(!rule.fallthrough || (rule.action == ScenarioAction.Forward && rule.delayMs == 0L && rule.occurrence.sequence.isEmpty()), "Invalid fallthrough action")
            (listOf(rule.action) + rule.occurrence.sequence).forEach { action ->
                if (action is ScenarioAction.HttpResponse) {
                    check(action.status in 200..599, "Invalid synthetic status")
                    check(action.fixtureId == null || action.fixtureId in fixtureIds, "Missing fixture")
                    check(action.fixtureId == null || (rule.match.method != "HEAD" && action.status !in listOf(204, 205, 304)), "Body prohibited for synthetic response")
                }
            }
        }
    }
}
