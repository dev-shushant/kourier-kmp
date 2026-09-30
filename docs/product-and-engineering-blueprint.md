# AppArmorX Resilience — Next Journey

## Product and engineering blueprint

**Version:** 1.1 · **Date:** 30 September 2026 · **Owner:** Shushant Tiwari · **Status:** Working product specification for review and implementation

The [R1 implementation plan](roadmap/r1-implementation-plan.md) and [baseline audit](roadmap/r1-baseline-audit.md) translate this vision into the next engineering increments.

## 1. Decision and purpose

AppArmorX Resilience, formerly Kourier, should grow from an on-device network inspector into a **mobile API reliability toolkit** within the AppArmorX brand. The first major release should let a developer or QA engineer reproduce a failure on an actual Android or iOS app, alter a supported request at runtime, save the setup as a portable scenario, and share a redacted result. Its promise is: **“Reproduce, break, and explain mobile API behaviour on the device.”**

> **Brand transition:** AppArmorX is the umbrella brand; AppArmorX Resilience is the product. Current Kourier Maven coordinates, Kotlin/Swift APIs, framework/module names, repository URLs, and releases stay compatible. All roadmap work proceeds on separate feature branches. The `.kourier.json` extension below is a draft contract, not a shipped format; finalize its name in phase 1.

The investment thesis is a workflow, not a count of network protocols. AppArmorX Resilience already observes and exports traffic. The next journey is **observe → experiment → reproduce → explain → share → verify**. We will ship these steps incrementally. We should not present all future ideas as current capabilities.

**Immediate product boundary:** controlled development and QA builds, supported HTTP clients, explicit local activation, deterministic fault injection, capture-to-mock, portable scenario packs, and GraphQL operation matching, GraphQL subscriptions, WebSocket, SSE and gRPC workflows. Production builds retain a no-op path. Broader auto-instrumentation, journey inference, OpenAPI checks, incident capsules, OTel, and agent access are subsequent milestones with separate acceptance gates.

## 2. Evidence and current baseline

The public source repository describes a shared KMP core, SQLDelight/in-memory storage, OkHttp, Ktor, and Darwin `NSURLProtocol` interception, a Compose Multiplatform inspector, Android/iOS facades, and an Android no-op artifact. It documents request and response inspection, call stacks, bounded capture, redaction, live telemetry, HAR/cURL/text export, and opening the UI via bubble, notification, shake, or API. Capture applies to clients that are wired to an AppArmorX Resilience integration. The source and consumer distribution are separate repositories. [S1][S2]

The source README is the authority for this baseline, not a line-by-line audit of all implementations. Before code changes, clone the latest source and distribution branches, record commit SHAs, run the sample and focused tests, and inspect the relevant interceptors, model, masking, and export paths. The public repository's `AGENTS.md` records platform-specific build and iOS interception constraints. [S1][S3]

### Competitive reality

| Tool | What its own documentation shows | Implication for AppArmorX Resilience |
| --- | --- | --- |
| Chucker | Established Android/OkHttp on-device HTTP inspection | Inspection alone has weak differentiation. [S4] |
| WiretapKMP | Android/iOS/KMP inspection, HTTP mocking, throttling, WebSocket and SSE support, with no-op variants | “Add mocking and more protocols” is insufficient as a positioning claim. [S5] |
| Mockzilla | Cross-platform local HTTP mocking, live changes without rebuilding, presets and offline use | Runtime response switching is validated, but not uniquely ours. [S6] |
| Requestly Android debugger | The vendor sunset its Android SDK after adoption fell below expectations | Treat adoption as a measured hypothesis; a broad debugger can be costly to maintain. [S7] |

These sources demonstrate active solutions and an identifiable workflow; they do **not** prove AppArmorX Resilience has paying demand or that the proposed differentiation wins. We need direct user evidence before building the later platform layers.

## 3. Target users and job to be done

**Primary user:** mobile developer or QA engineer testing error handling in a real app build. They need to provoke a precise API condition without waiting for the backend, changing server state, or publishing another test build.

**Secondary user:** mobile lead or backend engineer receiving a bug report. They need to know which request was altered, what the app did, and enough redacted evidence to reproduce the result.

**Initial target teams:** Android and iOS teams with multiple environments, flaky backend dependencies, GraphQL/REST mixes, or recurring QA-to-backend handoffs. KMP adopters are a natural entry audience because shared scenario semantics across Android and iOS are useful, but the product should work in native host apps too.

**Top use cases:** timeout and retry, HTTP 401 refresh loop, 429 backoff, transient 503, malformed or missing JSON fields, GraphQL `errors` inside HTTP 200, duplicate submission, and long loading state. A reproducible checkout or EV charging example will make the value concrete; demos must use fictional endpoints and data.

## 4. Product principles and explicit limits

1. **Same scenario, same observable outcome.** A pack has a stable schema and predictable matching/order across supported adapters. Timing and OS scheduling cannot be made bit-for-bit identical; record deviations.
2. **The real app remains the test subject.** AppArmorX Resilience changes a supported network exchange and records the app's response. It does not claim to test entire business workflows by itself.
3. **Safe by default.** Fault controls and stored bodies are limited to explicitly enabled development/QA builds. No remote control endpoint is open by default. Redaction happens before persistence and export.
4. **Honest coverage.** Show which clients were instrumented and which were not. Never label traffic capture as universal.
5. **Small integration surface.** Preserve current interceptors and facades; add configuration once. Explore Android build-time instrumentation only after adapters are stable and measurable.
6. **Local first.** No account or hosted backend is needed for core experiments or pack sharing. File import/export is deliberate.
7. **Evidence before intelligence.** Rule ID, matched request, action, attempt index, outcome, and timestamps should be machine-readable before adding heuristics or AI.

**Excluded from the first release:** generic offline replay of an entire logged session, production fault injection, automatic screen/journey discovery, arbitrary traffic interception, remote team control, CI orchestration, OpenAPI validation, LLM diagnosis, and a desktop application.

## 5. Release R1 AppArmorX Resilience

### 5.1 End-to-end experience

1. An engineer adds the AppArmorX Resilience development dependency and the appropriate OkHttp/Ktor/URLSession integration. The inspector shows a coverage/activation status.
2. QA opens the inspector in an allowed build, selects a captured request or creates a rule, and chooses **Force 503**, **Delay**, **Timeout**, **Disconnect**, or **Return saved response**.
3. QA retries the app action. AppArmorX Resilience marks the transaction `injected`, displays the matched rule, original target, effective response/error, elapsed time and attempt number, and distinguishes injected failure from server failure.
4. QA taps **Save scenario**, gives it a name, and exports a versioned `.kourier.json` file with redacted fixtures. A teammate imports it on Android or iOS and can activate it locally.
5. QA shares the scenario plus a small redacted result summary. The recipient can repeat it and mark the observed app behaviour pass/fail.

**Demo:** start a charging session, inject one 503 on the start request, observe retry or error UX, then use a delayed response to test loading/cancellation. A second demo uses `StartChargingSession` GraphQL operation rather than matching every `POST /graphql`.

### 5.2 Fault engine specification

**Rule matching, R1:** normalized scheme/host/path with exact or explicit wildcard semantics; HTTP method; optional query key/value; optional header key/value; optional GraphQL `operationName`. AND across configured fields. Sensitive header values are not shown in UI and cannot be exported as raw literals. Query ordering does not affect matches. An unparseable GraphQL body falls back to HTTP matching and records `graphql_parse_failed`.

**Actions, R1:** fixed delay before forwarding; synthetic HTTP status and optional headers/body without a network call; transport timeout; transport disconnect; substitute a saved HTTP response. A rule has exactly one terminal response action and may have one delay. Unsupported combinations fail validation before activation. Synthetic results must use the adapter's native error/response path so app behaviour remains realistic; tests define exception mapping for each platform.

**Execution:** explicit enabled/disabled state; scope to the current app process/session; first N occurrences, every Nth occurrence, or a finite ordered sequence; deterministic counter keyed by scenario + rule + normalized request identity; seeded probability is deferred unless a real use case requires it. Rule ordering is priority descending then stable rule ID; only the first matching terminal rule applies. When a rule has `fallthrough`, the next rule can be considered. A clear **Reset scenario** action zeros counters. Restarts reset counters in R1 and the UI says so.

**Guardrails:** cap delays and fixture size; reject rules that target AppArmorX Resilience internal traffic; avoid recursion on iOS forwarding; record rule evaluation failures without changing the host request unless the rule explicitly matches and executes. Import rejects invalid schemas and oversized content. The engine cannot mutate TLS below the supported client layer or simulate OS-wide airplane mode.

### 5.3 Capture to mock

A captured response becomes a proposed fixture only after applying the existing redaction policy plus a preview. The user chooses a stable endpoint/operation matcher, edits status and safe body fields, names the fixture, and saves it locally. Sensitive payloads may be declined or require explicit field removal before saving. Binary/streaming bodies are out of scope in R1. Do not claim a fixture reproduces server state, authentication, cache, or dependent requests.

### 5.4 Scenario pack format

A pack is JSON, validated before import, with `schemaVersion`, `id`, `name`, `description`, `createdBy` (optional), `rules`, `fixtures`, `redactionMetadata`, and `checksum`. No device ID, raw auth token, cookie, secret, or absolute private server URL is required. Environment-specific host aliases can be mapped locally at import. Pack export is always explicit and lists potentially sensitive fields before share. Unknown required versions are rejected; unknown optional fields are preserved only if harmless and documented.

Illustrative contract, subject to code-level review:

```json
{
  "schemaVersion": 1,
  "id": "charging-start-503-once",
  "name": "Start charging returns 503 once",
  "hostAlias": "chargingApi",
  "rules": [{
    "id": "start-once",
    "priority": 100,
    "match": {"method": "POST", "path": "/graphql", "graphqlOperation": "StartChargingSession"},
    "occurrence": {"first": 1},
    "action": {"type": "httpResponse", "status": 503, "bodyFixture": "service-unavailable"}
  }],
  "fixtures": [{"id": "service-unavailable", "contentType": "application/json", "body": "{\"message\":\"Service unavailable\"}"}]
}
```

The published schema must define URL normalization, wildcard escaping, duplicate IDs, maximum sizes, status ranges, sequence reset, and schema migration. Commit sample packs to Git so a team can review changes. Files are data, not executable scripts.

### 5.5 GraphQL R1

For JSON HTTP bodies, extract `operationName` when present; for single-operation documents without it, a bounded parser may infer the name only if reliable. Record GraphQL `errors` separately from HTTP status, including HTTP 200 with application errors. Do not capture full variables by default. A synthetic GraphQL error response is a fixture with HTTP status and a GraphQL body, rather than a special transport error. GraphQL subscriptions over supported WebSocket protocols are required by the expanded scope; Apollo-specific transport hooks require explicit adapter coverage.

### 5.6 Roles and access

R1 has **no identity or server-side role system**. A host app chooses build variant and optionally supplies a local activation gate (for example an internal QA menu or short-lived local unlock). A claim of “QA role-based remote controls” would require authentication, authorization, audit, revocation, and deployment design; plan it separately. Default release artifact is no-op on Android. Verify iOS distribution/production packaging strategy and ensure the fault engine is absent or irreversibly disabled in release configurations before describing it as production safe.

## 6. Architecture

### 6.1 Proposed module boundaries

| Component                                        | Responsibility                                                                                          | Dependency direction              |
|--------------------------------------------------|---------------------------------------------------------------------------------------------------------|-----------------------------------|
| `kourier-core`                                   | Existing transaction model, configuration, masking, event flow; add minimal injection metadata contract | No platform adapter dependency    |
| `kourier-scenarios`                              | Shared rule model, schema validation, matching, counters, pack import/export                            | Core models; no UI/network client |
| `kourier-interceptor-okhttp`                     | Convert OkHttp request/response/errors to shared decision and back                                      | Core + scenarios                  |
| `kourier-interceptor-ktor`                       | Same semantics at Ktor client boundary for Android/iOS                                                  | Core + scenarios                  |
| `kourier-interceptor-darwin`                     | Apply supported decisions in URLProtocol forwarding safely                                              | Core + scenarios                  |
| `kourier-storage`                                | Existing transactions plus versioned scenario/fixture persistence                                       | Core + scenarios                  |
| `kourier-ui`                                     | Rule editor, capture-to-mock, activation, import/export, coverage and result view                       | Public scenario facade            |
| `kourier-android`, `kourier-ios`, `KourierSwift` | Stable host-facing initialization and controls                                                          | Hide implementation details       |
| `kourier-noop`                                   | API-compatible inert calls in Android release                                                           | Mirrors public facade             |

These are proposed boundaries, not a claim that these files already exist. Add the new module only if the latest checkout supports clean shared dependencies; otherwise keep a focused package in core until extraction is warranted.

### 6.2 Request flow and correctness

`host client → adapter → normalize/match → decide action → forward or synthesize → record effective outcome → storage/event flow → inspector/export`.

Matching must occur before sending the request. Capture must distinguish original request metadata from effective synthetic result. A fault event has `scenarioId`, `ruleId`, `decision`, `occurrence`, `adapter`, `startedAt`, `duration`, and `source=injected`. Avoid storing raw match secrets. Concurrency must define counter atomicity: simultaneous requests claiming “first occurrence” cannot both win. Once a decision is assigned, cancellation should cancel delayed work and record cancellation, without manufacturing a response.

**Adapter parity contract:** tests run shared rule vectors through all applicable adapters. For unsupported request types, fail open and show coverage. Ktor and OkHttp interception order, redirects, retries, caching, compressed bodies, streaming, and one-shot bodies require explicit tests. On iOS, forwarding uses an inner session with custom protocol classes disabled to prevent recursive interception, per the repository guidance. [S3]

### 6.3 Privacy and reliability

Redact at ingest, before SQL storage, inspector display, pack creation, HAR/cURL export, and diagnostic handoff. Because a saved mock needs actual data to reproduce a state, a fixture must be separately previewed and sanitized; existing log redaction alone does not guarantee a safe mock. Apply bounds to number of transactions, rules, fixture bytes, and evaluation cost. A malformed rule must never crash the host app. Add a prominent **Active faults** indicator and one-tap **Disable all**. No auto-upload.

## 7. Subsequent milestones and decision gates

### R2 Evidence and explanation

**Journey correlation:** begin with opt-in explicit `screen`/`journey` markers plus Android Activity/Fragment and supported navigation hooks where reliable; infer only when confidence is shown. A request may be “screen unknown.” Background calls should not be falsely assigned to the foreground screen. Build a timeline joining app session, screen events, HTTP/GraphQL exchange, rule activation, and user supplied notes. Android Compose navigation, native fragments, SwiftUI and UIKit need separate coverage statements.

**Network Doctor:** ship a few deterministic findings with evidence and suppressions: duplicate non-idempotent calls, 401-refresh loop, burst/retry storm, and 429 with no visible backoff. Label findings as patterns, not root cause. Show exact transactions and threshold that triggered them.

**Contract Guardian:** import an OpenAPI 3.1 document and map operations to captured method/path/status/content type. Validate JSON response shape, required fields and nullability on bounded captured bodies; clearly distinguish missing spec coverage, parse failure, and actual mismatch. Start as opt-in on-device or build-time fixture validation; avoid a huge parser in the hot request path. OpenAPI 3.1 has explicit Schema Object semantics; choose a conforming validator and document unsupported dialect features. [S8]

**Incident capsule:** versioned export of selected redacted timeline, app/build metadata, active scenario, observed result, and warnings, with consent preview. Initially import for viewing and scenario extraction; only offer replay for cases fully represented by fixtures. A capsule must never promise full UI/session replay from network logs.

### R3 Integration and interoperability

**Android Gradle plugin:** first a feasibility spike, then opt-in build-time integration for one known client pattern. Report discovered clients and injected hooks, support an escape hatch, measure build time and bytecode compatibility, and never silently claim 100% coverage. Android Gradle instrumentation APIs exist, but version compatibility and interception order require proof on real sample apps. [S9]

**iOS:** keep explicit URLSession configuration/Swift helpers as the reliable path. Apple documents that custom `URLProtocol` subclasses cannot be used with background sessions; do not promise universal capture through this adapter. [S10]

**OTel bridge:** optional emission of sanitized spans/events and correlation IDs, conforming to current mobile and session conventions, with no required hosted service. Avoid duplicating the app's existing tracing SDK or leaking payloads. [S11]

**MCP/agent bridge:** only after stable, consented incident capsules. A local read-only tool can inspect structured evidence first; mutation tools need explicit local activation and scope. Do not put an LLM in the SDK.

**Protocol coverage:** WebSocket, SSE, GraphQL subscriptions, and gRPC are now required by the user for Android and iOS on the same roadmap branch. Their adapters and streaming scenario semantics must be implemented and tested before claiming completion. See [expanded protocol scope](roadmap/protocol-coverage.md).

## 8. Delivery plan and reviewable gates

| Phase | Deliverable | Exit gate |
| --- | --- | --- |
| 0. Baseline audit | Latest source/distribution SHAs; module map, integration matrix, current test results, sensitive-data review | Existing Android sample builds; iOS build on macOS; baseline limitations recorded |
| 1. Shared contract | Versioned scenario schema, rule model, matching and counter tests; sample pack | Deterministic shared test vectors; invalid imports rejected |
| 2. One platform vertical slice | Android OkHttp: inject 503/delay, log metadata, disable all, sample UI | Real sample demonstrates first-call 503 and reset; host unaffected when disabled |
| 3. Parity | Ktor Android/iOS and URLSession adapters, iOS sample and packaging | Same portable pack produces equivalent documented result on each supported path |
| 4. Workflow | Capture-to-mock, inspector editor, pack import/export, redaction preview | QA completes create → share → import → repeat without rebuilding |
| 5. Release hardening | No-op parity, size limits, concurrency/cancellation tests, documentation and demo | Release variants have no active fault controls; regression suite and package checks pass |
| 6. Field validation | Interviews, two external pilots, observed time-to-reproduce | Continue R2 only if users repeat the workflow and request evidence features |

These are ordered gates, not date commitments. A solo-maintainer estimate is **6–10 focused weeks** for R1 after the audit, with iOS packaging and adapter parity the largest uncertainty. Re-estimate after phase 0. Keep each phase independently demonstrable and review implementation decisions together before changing public APIs.

### Definition of done for R1

- A QA user can activate a named scenario, force a one-time 503 and a delay, reset, disable all, and see which rule affected each request.
- A saved response can become a sanitized fixture and be shared/imported via a documented pack format.
- The same pack runs on supported Android/iOS HTTP integrations with documented exceptions.
- GraphQL `operationName` matching and HTTP 200/GraphQL error labeling work on sample traffic.
- No-op/release builds cannot activate fault injection; invalid packs and adapter errors leave host networking stable.
- Source and consumer READMEs, samples, migration notes, release notes, and limitations match the shipped behaviour.

## 9. Quality and security test matrix

| Test class      | Required cases                                                                                                            |
|-----------------|---------------------------------------------------------------------------------------------------------------------------|
| Matcher         | URL normalization, query order, case rules, wildcards, overlapping rules, GraphQL operation missing/malformed             |
| Determinism     | Concurrent first-N, sequence exhaustion, reset, process restart, duplicate requests                                       |
| Adapters        | Normal response, 4xx/5xx, redirect, timeout, cancellation, one-shot/streamed body, interceptor order, retry behaviour     |
| Data protection | Tokens/cookies/query PII, nested JSON, Unicode, binary, truncated content, fixture preview, pack/HAR/cURL exports         |
| Pack            | Invalid JSON, unknown version, duplicates, oversized fixture, invalid status, checksum mismatch, path/host alias          |
| Host safety     | Disabled path, no-op parity, app startup before initialization, storage failure, malformed rule, iOS forwarding recursion |
| Performance     | Additional request latency when disabled/active, memory and disk bounds, inspector responsiveness under burst load        |

Establish budgets during phase 0 from baseline measurements. Proposed target: disabled-path p95 overhead under 2 ms on the sample's small JSON requests, excluding storage and UI scheduling; no rule evaluation should block the main thread. Do not market performance numbers until measured across devices and adapters.

## 10. Market validation and distribution

Run 10–15 structured conversations across Android, iOS, KMP, QA, and mobile platform leads. Ask for the last specific API failure they struggled to reproduce, the current workaround, build frequency, security restrictions, and whether a portable scenario would have shortened the handoff. Observe at least five teams attempting the R1 workflow with their own app or a realistic sample. Collect install completion, time to first injected fault, share/import success, repeat use in a second session, and unprompted requests for journey/contract evidence.

**Go signal:** at least two external teams use a pack in a real debugging or QA task more than once, and can describe the time saved or an otherwise hard-to-reach state. **Pause signal:** users only inspect traffic, cannot integrate, or consistently prefer their existing mock server/proxy. These are decision heuristics, not statistically conclusive market proof.

Distribution should lead with a 30–45 second before/after video and a single runnable sample: “Force a charging start failure on Android and iOS without rebuilding.” Provide exact install steps, a compatibility/coverage matrix, one sample pack, a threat model, and issue templates for integration failures. Invite the KMP community post's engaged engineers to try the scenario workflow, but do not confuse likes or stars with adoption. Explore GitHub Sponsors or paid support only after repeated usage; avoid cloud billing before it serves a real collaboration need.

## 11. Positioning and naming

Use **AppArmorX** as the umbrella brand and **AppArmorX Resilience** as the product name, with **Mobile API Reliability Toolkit** as the descriptor. During the transition, introduce it as **AppArmorX Resilience, formerly Kourier**. The landing page should make the task explicit: “Inject an API failure on a device, save the scenario, and hand a reproducible case to your team.” That experiment workflow is planned for R1; current messaging must lead with the existing inspector capabilities.

Preserve existing SDK coordinates, public APIs, framework names, and repository links. Any later technical namespace migration needs its own compatibility plan. AppArmorX security offerings and Resilience development/QA experiments have separate product responsibilities. Avoid claims such as “zero integration,” “captures everything,” “automatic journeys,” or “production-safe iOS no-op” until verified and documented.

## 12. Open decisions for the first implementation session

1. Verify the exact latest source/distribution commits and current published version; inspect actual model and interceptor extension points.
2. Choose whether scenario state lives in a new shared module immediately or a focused core package first.
3. Fix canonical pack JSON schema and validation limits after inspecting current redaction/storage behaviour.
4. Select the first Android OkHttp sample journey and record expected host-side UI behaviour.
5. Decide the iOS release packaging/no-op strategy before exposing a public activation API.

**Next concrete step together:** complete phase 0 against the current checkout, then write a narrow implementation plan for the shared contract and Android OkHttp vertical slice. Review that slice on a device before expanding to Ktor and URLSession.

## Sources and evidence notes

- **[S1]** [Kourier source README](https://github.com/dev-shushant/kourier-kmp), accessed 29 September 2026. Public documented baseline, architecture, build and release flow.
- **[S2]** [Kourier consumer distribution](https://github.com/dev-shushant/kourier), accessed 29 September 2026. Consumer documentation and distribution repository.
- **[S3]** [Kourier repository agent guidance](https://github.com/dev-shushant/kourier-kmp/blob/main/AGENTS.md), accessed 29 September 2026. Current module and platform cautions; recheck against latest checkout.
- **[S4]** [Chucker repository](https://github.com/ChuckerTeam/chucker), accessed 29 September 2026.
- **[S5]** [WiretapKMP repository](https://github.com/skymansandy/wiretapKMP), accessed 29 September 2026.
- **[S6]** [Mockzilla repository](https://github.com/Apadmi-Engineering/Mockzilla), accessed 29 September 2026.
- **[S7]** [Requestly Android Debugger sunset](https://requestly.com/blog/saying-goodbye-to-the-requestly-android-debugger/), vendor account of its own adoption decision.
- **[S8]** [OpenAPI Specification 3.1.1](https://spec.openapis.org/oas/v3.1.1.html).
- **[S9]** [Android Gradle Instrumentation API](https://developer.android.com/reference/tools/gradle-api/8.8/com/android/build/api/variant/Instrumentation).
- **[S10]** [Apple URLSessionConfiguration protocolClasses](https://developer.apple.com/documentation/foundation/urlsessionconfiguration/protocolclasses).
- **[S11]** [OpenTelemetry mobile conventions](https://opentelemetry.io/docs/specs/semconv/mobile/) and [session conventions](https://opentelemetry.io/docs/specs/semconv/general/session/).

**Evidence distinction:** repository and vendor documentation support descriptions of existing tools. All Kourier roadmap, architecture, prioritization, timelines, and market gates above are proposals or estimates, to be verified through implementation and user observation.
