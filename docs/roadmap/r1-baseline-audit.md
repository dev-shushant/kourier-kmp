# R1 baseline audit

30 September 2026. This is a source inspection and local verification record, not a certification of the published binaries.

## Provenance

| Item | Observed baseline |
| --- | --- |
| Public source main | `48363c3ac84d07c4e318112ca4ce4941377d6ad5` |
| Local rebrand source | `5356b620bab45bddc3cc631352e149735fcbf4f9` |
| Distribution main checkout | `fcb107db185983da46963a3e84db4275439942cd` |
| Local distribution rebrand | `d64943ef9b433895a51e82c04d699b201185585f` |
| GitHub latest release | [v0.0.1](https://github.com/dev-shushant/kourier/releases/tag/v0.0.1), published 9 September 2026 |
| Source version / SPM artifact | 0.0.1 / v0.0.1 |
| Roadmap branch | `codex/resilience-r1-foundation`, based on local rebrand source |

Public source main and latest release were read through GitHub's API on the audit date. Distribution main SHA is the previously cloned checkout, not an assertion that the remote never changed afterward. Existing distribution README contains examples of 0.0.3; do not use those examples as proof of a published release.

## Extension points and coverage

| Layer | Source finding | R1 consequence |
| --- | --- | --- |
| Core | Atomic runtime replacement; capture queue runs asynchronously on IO | Scenario decisions must be independent of capture persistence; tests must flush before asserting stored results |
| OkHttp | Captures before `chain.proceed`; skips one-shot/duplex buffering; peeks response | Natural first injection point; do not read one-shot body for GraphQL matching |
| Ktor | `onRequest`, `Send` around `proceed`, `onResponse` calls `save()`/bodyAsText | Synthetic response needs a proved native client path; full response buffering/stream handling needs tests before parity |
| Darwin | Configured URLProtocol; ephemeral forwarding session with protocol classes empty; stopLoading cancels task | Preserve recursion prevention; own delayed/synthetic task lifecycle and cancellation |
| Storage | Whole transaction serialized as JSON; reader ignores unknown keys | Additive evidence can be persisted without a schema table change; verify old/new round trips |
| UI | Transactions, telemetry, export, settings; no fault state | Add explicit active-fault indication and disable action before user activation |
| Android facade | Core exposed through `api` for config DSL; interceptors re-exported | A scenario type in a public signature affects dependency surface and no-op parity |
| No-op | Android/iOS stub sources, Android config and telemetry mirrors; standalone KourierNoop framework | Source stubs do not prove a production-compatible KourierIos SPM replacement |
| iOS distribution | Real KourierIos XCFramework is the SPM binary target | Production exclusion/no-op packaging remains an R1 release blocker |

Only wired client instances are covered. No fault injection, scenario editor, pack import, GraphQL operation matching, or journey correlation currently exists.

## Privacy and reliability observations

- `DataMasker.maskPayload` uses regex replacement. A sensitive object containing deeper nested structures is not guaranteed to be removed as a whole. Do not call this a general parsed JSON sanitizer.
- URL masking compares raw query names; percent-encoded names are not decoded before comparison. Coverage needs encoded-key vectors before pack exports.
- Masking lives at adapter ingest. Storage persists the supplied transaction; callers can supply unmasked data directly. Fixture sanitization must be its own enforced boundary.
- OkHttp repeatable request bodies are written into a buffer before truncation. Capture size configuration does not cap that initial buffering allocation. Ktor saves a response before truncation. Record this limitation and measure/harden bounds before R1 performance claims.
- The capture channel drops oldest events under pressure. Fault evidence needs a documented loss indicator or separate bounded mechanism; counts are not a reliable occurrence engine.
- Existing OkHttp test reads storage immediately after response completion despite asynchronous persistence. It needs a flush barrier to avoid an intermittent baseline failure.
- SPM declares iOS 15; sample settings declare iOS 16. No minimum-target claim should be inferred from successful KMP compilation; inspect the actual XCFramework and run a linked host before release.

## Verification

The rebrand baseline already passed Android sample assembly, core Android/iOS tests (17 per platform), iOS facade compilation, Swift manifest parsing, and physical-device smoke capture of four demo requests. The screenshot is local build output; it is not a published demo. No changes to runtime code have been made for this audit.

Additional verification passed on 30 September 2026:

- `testDebugUnitTest` / `dev.shushant.kourier.interceptor.okhttp.KourierOkHttpInterceptorTest`: 1 tests, 0 failures, 0 errors.
- `iosSimulatorArm64Test` / `iosSimulatorArm64Test.dev.shushant.kourier.interceptor.ktor.KourierKtorPluginTest`: 2 tests, 0 failures, 0 errors.
- `testAndroidHostTest` / `dev.shushant.kourier.interceptor.ktor.KourierKtorPluginTest`: 2 tests, 0 failures, 0 errors.
- `:kourier-noop:assemble`: Android AAR and debug/release iOS device/simulator frameworks built successfully. This is assembly evidence, not host API interchangeability evidence.

Command: `./gradlew :kourier-interceptor-okhttp:testDebugUnitTest :kourier-interceptor-ktor:allTests :kourier-noop:assemble --console=plain`.

 XCFramework assembly, a linked iOS host run, production dependency switching, disabled-path overhead, and adversarial privacy/streaming/concurrency coverage are outstanding. Phase 0 has useful baseline evidence but the full platform release gate is not complete.

## Next increment

[Implementation plan](r1-implementation-plan.md): isolate the portable shared contract, then prove a 503-once/reset/disable path with Android OkHttp before expanding adapters or exposing host controls.
