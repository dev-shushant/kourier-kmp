# AppArmorX Resilience R1 implementation plan

Status: implementation preparation, 30 September 2026. Owner: Shushant Tiwari.

## Outcome

A mobile engineer can make a supported request fail once, inspect why it failed, reset the experiment, and share the same scenario with a teammate. Existing inspection integrations continue working when experiments are disabled.

The first demonstrable increment is a fictional charging-start request: activate **503 once**, send `POST /charging/sessions`, observe the host's error state, retry to get a normal response, reset to reproduce the 503, then disable all faults. The same scenario must run through Android OkHttp, Ktor Android/iOS, and configured iOS URLSession before the milestone is complete. A separate delay preset verifies loading and cancellation. Use a deterministic local backend in automated tests so results do not depend on httpbin availability. Do not automatically retry a non-idempotent POST in the sample.

## Branch and release boundary

Work on `codex/resilience-r1-foundation`, based on source rebrand commit `5356b620bab45bddc3cc631352e149735fcbf4f9`. Keep `main`, version 0.0.1, Maven coordinates, interceptor construction, and `KourierIos` imports stable. Rebrand work remains on its own branch. No roadmap release or publishing action is authorized by this document.

All R1 work stays on this branch. Each implementation increment gets focused tests on Android and iOS, applicable sample builds, and a separate commit. Documentation must say which adapter actually executes faults. R1 is incomplete until adapter parity, packaging, and the sharing workflow pass their gates.

## Architecture decisions

1. Add `kourier-scenarios` as an independently testable KMP module. It owns data, validation, normalized request matching, and deterministic state; it depends on neither clients, Compose, nor storage. Its portable models should not reuse `HttpRequest`, whose stored URL/body are already masked. Keep URL parsing at adapter boundaries and test normalization against shared vectors.
2. The engine returns a decision; adapters execute it. Matching must never perform I/O or buffer a one-shot body. Keep raw matching input ephemeral. Metadata contains IDs and outcome, never raw matcher headers, variables, or URL credentials.
3. Activation is disabled initially, local, and process-scoped. Separate loaded scenario from active scenario. Replace the whole validated scenario atomically. Disabled and unsupported paths forward normally. No persisted auto-activation.
4. Occurrence counters are claimed atomically with rule selection. Reset and activation changes share the same synchronization boundary. Cancelled attempts remain consumed once assigned; they record cancellation. Disabling all prevents new decisions and cancels injected delays already waiting; it does not invent an HTTP response or cancel unrelated forwarded requests.
5. Add optional injection evidence to the transaction model only after the adapter decision contract is reviewed. SQLite's serialized transaction field can hold additive metadata without a table migration, but readers and exporters need round-trip tests. Preserve existing constructors at the source level and explicitly check JVM binary compatibility before release.
6. Avoid adding experimental controls to the public Kotlin/Swift facades until Android no-op parity and iOS release packaging are designed. First exercise the engine through tests and the development sample. Use a module exclusion or a separately shipped inert framework for production iOS; a mutable boolean alone is insufficient for the final release claim.

## Increment sequence

| Increment | Implementation | Acceptance evidence |
| --- | --- | --- |
| 1. Contract | Scenario/rule/action models, strict validation, normalized request input, format document and example pack | Invalid IDs/statuses/limits rejected; no networking or facade changes |
| 2. Decisions | Exact HTTP matching, first-N/every-N/finite sequence, deterministic priority, atomic counters, reset/disable | Shared Android/iOS tests plus concurrent first-one test; unmatched requests consume no counters |
| 3. OkHttp | Pre-forward decision, 503 synthesis and cancellable delay, effective transaction evidence | MockWebServer receives zero requests for synthetic response; receives normal requests when disabled/exhausted; body still readable |
| 4. Demonstration | Charging-start sample, visible active scenario, reset, disable all, injected badge and rule detail | Device run demonstrates first 503, next normal result, reset, delay cancellation, disabled normal path |
| 5. Matching expansion | Explicit wildcard, query/header predicates, GraphQL operationName, bounded parser | Shared URL/GraphQL vectors; malformed input fails open with diagnostic; secrets absent from evidence |
| 6. Adapter parity | Ktor Android/iOS and configured URLSession decisions | Equivalent pack outcomes; native errors, cancellation, redirect/retry behavior documented; iOS recursion test |
| 7. Sharing | Sanitized fixture creation and preview, persistent packs, explicit import/export, environment alias mapping | Create → export → import → repeat; invalid/oversized packs leave active state unchanged |
| 8. Release gate | API/no-op parity, iOS production artifact, measured bounds, docs and runnable demo | Release host compiles and cannot activate scenarios; regression/packaging checks pass |

Timeout/disconnect actions enter after the 503/delay path, with adapter-specific exception mapping tests. Saved response substitution enters with the sanitized fixture workflow. Do not represent these as shipped in earlier increments.

## Proposed portable format

Use `.resilience.json` for the draft version 1 format, replacing the blueprint's placeholder `.kourier.json`. Keep existing SDK namespaces unchanged. This extension and the following choices become stable only when contract tests and import/export ship.

- One scenario per pack; named host aliases resolved locally. No literal private origin required in an exported matcher. Local bindings are separate and excluded from export.
- IDs: ASCII letters/digits plus `-`, `_`, `.`, 1–64 characters, unique in their collection. Rule and fixture references must resolve.
- Limits to validate initially: 128 rules, 64 fixtures, 256 KiB per fixture, 2 MiB pack, 30 seconds injected delay, 64 KiB GraphQL matching input. These are engineering caps, not measured performance promises.
- Synthetic responses: final HTTP statuses 200–599; no interim 1xx/upgrade responses. Exactly one terminal action with an optional delay. Enforce body semantics for HEAD, 204, 205, and 304 at the adapter boundary. Reject unsupported transfer/content encoding and framing headers rather than emitting inconsistent responses.
- Match schemes/hosts case-insensitively; method uppercase; path case-sensitive. Normalize default ports, preserve encoded path separators, and ignore fragments. Query order is immaterial; duplicate query keys use explicitly defined any-value matching. Do not silently decode `%2F` to `/`.
- Wildcard is an explicit match mode with `*` for zero or more characters; no arbitrary regex. Define escaping and whether it crosses path segments in the contract before implementation. Exact mode ships first internally; broader matching remains gated.
- Priority descending then rule ID ascending. Only the first eligible terminal rule applies. Exhausted rules allow evaluation of subsequent rules. A finite sequence contains explicit outcomes including forward; no hidden fallback behavior.
- GraphQL matching initially reads an explicit JSON `operationName`; no full variables in evidence. Document inference and batch handling as later supported cases only after parser tests.
- Schema version is required. Initially reject unknown fields and duplicate JSON keys instead of silently retaining input; add optional extension support deliberately in a later version. Checksum is SHA-256 over a specified canonical representation excluding the checksum field; it detects corruption, not trusted authorship. Canonicalization and test vectors are a prerequisite for the sample pack, not an ad hoc string hash.

## Privacy prerequisites

Captured logs are not automatically safe fixtures. Existing masking is a best-effort regex/key policy. Add parsed JSON sanitization for fixture creation, tests for nested composite values and encoded query keys, and an explicit preview. Do not include arbitrary error stack traces, query literals, or raw sensitive matching headers in packs. Stream/binary/truncated capture cannot become a fixture in R1.

## Review points before implementation expands

- Shared contract review: normalization, occurrence identity, canonicalization, and delay cancellation semantics.
- Host API review: sample-proven activation surface, binary compatibility, and paired no-op APIs.
- Platform review: Ktor interception order and iOS cancellation/background-session limits.
- Release review: Android dependency switching and verified iOS production packaging.

These are engineering checkpoints against concrete code and test evidence, not approvals to publish. The shared contract is now implemented in `kourier-scenarios`; exact matching and atomic first-N/every-N decisions are also implemented. Finite sequences, explicit fallthrough, wildcard/query/header predicates are implemented and tested on both platforms. URL normalization, portable packs, and native adapter execution remain outstanding. A milestone is complete only after both Android and iOS validation. The contract is experimental and not yet a published pack format.
