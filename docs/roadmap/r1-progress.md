# R1 delivery evidence

Branch: `codex/resilience-r1-foundation`. All roadmap implementation stays on this branch. Android and iOS parity is required for a completed feature milestone.

## Implemented foundation

- `kourier-scenarios`: isolated KMP module for Android, iOS device, and iOS simulator targets. No dependency from released networking adapters yet.
- Experimental serializable scenario, matcher, occurrence, action, and fixture models.
- Validation for version, identifiers, counts, UTF-8 fixture byte sizes, HTTP status/body rules, paths, operation names, delays, and references.
- Disabled-by-default process-local engine. Loading disables activation and resets counters. Invalid replacement retains existing state. Caller collections are copied before retention.
- Alias/method/exact-or-wildcard path/optional-operation matching, AND query/header predicates, priority descending then stable rule ID, atomic first-N/every-N assignment, reset and disable.
- Finite action sequences exhaust without looping; reset restarts them. Sequence mode cannot combine with first-N/every-N or a second terminal action. Explicit fallthrough evaluates the next rule; Forward terminates selection and lets the adapter forward.
- Wildcards use only `*` (including across path segments) and backslash escaping, with bounded pattern length and star count. Query names and values are case-sensitive; header names are case-insensitive and values case-sensitive. Duplicate values match if any value equals the predicate.
- Query ordering is normalized for counter identity. The engine expects parsed adapter input and local alias resolution. Shared URL and GraphQL helpers are implemented but not yet wired into native adapters.
- Native adapter entry point resolves a real URL against explicit local origin bindings under the same synchronization boundary as decision selection. Invalid/ambiguous/missing bindings fail open; invalid replacements preserve activation.
- Decisions carry a session execution version. Suspended delays return promptly when disabled, reset, or replaced, allowing adapters to forward normally. Re-enabling cannot resurrect stale decisions. Invalid replacements preserve pending decisions; host coroutine cancellation propagates. Native adapters must check the permit before applying the action.
- Counter bound of 4096 identities. New identities fail open at capacity, without evicting and accidentally repeating earlier first-call faults. A user-facing capacity diagnostic is still required.

## Verification

Shared validation and decision tests execute on Android host and iOS simulator. They cover round trips, invalid replacements, bounds, rule ordering, reset, activation, caller mutation, query ordering, GraphQL operation matching, capacity exhaustion, and concurrent first-one assignment. The 30 September 2026 run passed 40 tests on each platform (80 executions), alongside Android sample assembly and iOS facade compilation. Consult Gradle test XML for the latest run; successful engine tests prove decisions, not native fault execution.

## Expanded transport requirement

The user added WebSocket, SSE, GraphQL subscriptions, and gRPC to the goal on both platforms. See [protocol coverage](protocol-coverage.md). These are required deliverables, not deferred protocol research.

## Native execution in progress

Android OkHttp now evaluates bound URLs, applies synthetic HTTP responses or native timeout/disconnect exceptions, awaits cancellable delays, and records safe evidence in transaction tags. Synthetic outcomes avoid network forwarding and report zero sent bytes. Disabled and exhausted rules forward unchanged. The existing constructor and facade APIs are unchanged. `ResilienceRuntime` is an experimental development runtime; user activation UI, production packaging and the full URLSession forwarding/parity suite are not complete. This is an intermediate adapter increment, not a completed cross-platform feature.

OkHttp MockWebServer tests prove a synthetic 503 avoids the network, the next call forwards after exhaustion, native transport errors are recorded, and disabled scenarios preserve the server response. Existing capture assertions now flush asynchronous storage before reading results. Verification passed: 66 shared Android/iOS executions and 4 OkHttp tests, Android sample assembly, and iOS facade compilation. Selected native iOS faults are now verified through actual URLSession callbacks (see below).

Ktor shared execution now passes on Android and iOS: synthetic response/fixture, delay permit, timeout and disconnect mapping, disabled/exhausted forwarding, GraphQL operation-specific matching, and safe transaction evidence. Synthetic responses run the native receive pipeline, including body preservation, capture, and host `expectSuccess` validation. The adapter uses the pinned Ktor 3.1.3 internal HttpClientCall constructor; version upgrades require repeating these compatibility tests. Received response metadata is retained when host validation subsequently throws. Twelve Ktor test executions passed (six per platform), alongside Android sample assembly and iOS facade compilation. Configured URLSession execution is now implemented; full forwarding/retry/redirect/cache parity remains outstanding.

Configured URLSession now executes shared bound decisions. It preserves the ephemeral inner session with custom protocols disabled, serializes lifecycle changes/callback delivery, maps timeout/disconnect to NSURLErrorDomain native codes, emits synthetic status/body through URLProtocol callbacks, and cancels pending work on host cancellation. Three iOS simulator tests passed through actual Foundation URLSession callbacks: Unicode fixture/503 evidence, both native error mappings, and cancellation of a pending 30-second fault. iOS facade compilation passed. Disabled/forwarded local-backend behavior, redirects, cache, release packaging, and a linked sample still need verification.

Shared inspector controls now observe a read-only scenario summary, show active/disabled status, allow explicit enable/reset/disable, and surface counter-capacity warnings. Private origin bindings and matcher secrets are excluded from this summary. UI activation requires complete local alias bindings. State tests passed on Android and iOS; Android sample build and iOS facade compilation passed. The controls row was visually inspected on a physical Android device. UI Automator could not obtain an idle inspector tree, so modal interaction was not claimed as verified. iOS visual verification, the rule editor, capture-to-mock, and pack workflow remain incomplete.

Shared single-rule editor now creates HTTP status, delay/forward, timeout, and disconnect scenarios from entered or captured targets, with optional explicit GraphQL operation and first-N controls. Shared draft validation prevents invalid numbers from becoming unlimited faults, normalizes the local binding, and excludes private origin/query values from the portable scenario. Load remains disabled; enable is separate. Eight new draft test executions passed across Android/iOS; Android assembly and iOS compilation passed. End-to-end modal interaction remains unverified: the device’s legacy UI Automator runner aborted because an Android test annotation was unavailable. This runner failure is not an app test pass. Multi-rule editing, fixtures, packs, iOS visual checks, and a supported UI automation runner remain outstanding.

## Remaining R1 requirements

- [x] Finite ordered sequences, explicit fallthrough, wildcard/query/header matching with shared tests.
- [x] Shared strict HTTP URL parser: origin case/default-port normalization, encoded-separator preservation, literal dot segments, query decoding/duplicates/Unicode, and invalid-input rejection.
- [x] Bounded explicit GraphQL operation/error-count parsing with depth/byte limits and no retained variables/error messages.
- [ ] Adapter normalization parity, GraphQL inference/batch cases, and native HTTP-200 GraphQL error labeling.
- [ ] Stable versioned pack schema, canonical checksum, duplicate-key rejection, strict bounded import/export, example pack.
- [ ] WebSocket/SSE/GraphQL-subscription/gRPC native adapters, message-level scenarios, controls, sanitized evidence, and Android/iOS parity tests.
- [ ] GraphQL bounded operation parsing and HTTP-200 error labeling; no default variables capture.
- [ ] Android OkHttp, Ktor Android/iOS, and configured iOS URLSession execution of response/delay/timeout/disconnect/fixture decisions.
- [ ] Native cancellation, pending-delay disable behavior, recursion prevention, retries/redirects/caching/streaming/one-shot tests.
- [ ] Sanitized injection evidence persisted and displayed, with machine-readable export and explicit capture-loss warnings.
- [ ] Both-platform active controls, rule editor, reset/disable, coverage status, sample charging journey and fault result display.
- [ ] Capture-to-mock with parsed sanitization and preview; bounded fixture persistence.
- [ ] File sharing/import, local host alias binding, redaction metadata and result handoff.
- [ ] Android no-op parity and host release dependency verification; iOS inert/excluded production packaging and host verification.
- [ ] Regression/privacy/concurrency/performance checks, linked iOS sample/device validation, updated install and migration docs, final runnable demo.
- [ ] External pilots and field-validation evidence before R2 investment.

R2 journey markers, deterministic findings, OpenAPI validation and incident capsules, plus R3 instrumentation/interoperability/protocol work remain in the blueprint with their decision gates. This foundation does not satisfy or replace those milestones. No roadmap capabilities are published.
