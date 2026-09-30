# Android and iOS protocol coverage

User scope expansion, 30 September 2026: include GraphQL, WebSocket and streaming protocols in the vision. Work stays on `codex/resilience-r1-foundation`. This expands the blueprint's earlier exclusions. The user additionally confirmed gRPC. Required protocols are REST/HTTP, GraphQL including subscriptions, WebSocket, SSE, and gRPC. Any additional transport needs its own supported integration; do not interpret “everything” as proof of universal instrumentation.

## Required outcomes

| Protocol | Shared semantics | Android integration | iOS integration |
| --- | --- | --- | --- |
| REST/HTTP | Deterministic response/delay/timeout/disconnect/fixtures; redacted evidence | Wired OkHttp and Ktor clients | Wired Ktor and configured URLSession |
| GraphQL HTTP | Operation matching, variables redaction, HTTP-200 errors, operation-specific fixtures | HTTP adapters with bounded JSON parsing | Same parser and behavior through HTTP adapters |
| WebSocket | Connection lifecycle, text/binary metadata, direction, close codes, bounded redacted messages; drop/delay/replace/disconnect experiments | Explicit OkHttp WebSocket listener/wrapper and Ktor integration | Explicit URLSession WebSocket task wrapper and Ktor integration |
| GraphQL subscriptions | `graphql-transport-ws` operation lifecycle, ack/start/next/error/complete, operation-aware scenarios; legacy protocol identified separately | Supported WebSocket integrations | Same shared parser and event semantics |
| SSE | Incremental event parsing, event type/id/retry, reconnect and cancellation, bounded messages; delay/drop/replacement experiments | Streaming OkHttp/Ktor adapter | Streaming URLSession/Ktor adapter |

A native host can use the supported explicit integration without adopting KMP in its app code. Any client-specific hook is documented and included in its sample. No automatic coverage claim for sockets outside these hooks.

| gRPC | Unary/server-stream/client-stream/bidirectional lifecycle, service/method matching, deadlines, status/trailers, bounded redacted metadata/messages; injected status/delay/disconnect and fixtures | Explicit grpc-java/grpc-kotlin client interception; serialized message sanitization requires a configured descriptor or host sanitizer | Explicit supported grpc-swift client middleware/interceptor; descriptor/sanitizer parity and native Swift tests |

## Stream correctness gates

- Inspection does not steal frames/events from the host or wait for the whole stream to finish.
- Preserve ordering, close/error delivery, host cancellation, and backpressure; injected delays are cancellable.
- UTF-8 decoding handles chunk boundaries. SSE handles CR/LF/CRLF, comments, multiline data, persistent event IDs, retry hints and reconnects.
- GraphQL variables and subscription payloads pass redaction before evidence persistence/export. Authentication handshake content is not saved as raw fixtures.
- Explicit binary limits; unsupported binary mutation forwards unchanged and reports coverage.
- Bounds on frame/event size, history, rule evaluation and counters. Over-limit input produces visible diagnostics and preserves host traffic.
- The same scenario vectors run on Android and iOS. Native connection/reconnect/cancellation tests are also required; common parser tests alone do not prove adapter behavior.
- gRPC must report native RPC status rather than equating HTTP 200 to success; verify trailers, deadline/cancellation, all four RPC shapes, and message ordering. Binary protobuf without a supplied schema/sanitizer is metadata-only; do not pretend opaque bytes are sanitized fixtures.
- Production builds cannot activate experiments. Android/iOS inert API parity includes streaming entry points.

## Current evidence

Shared HTTP rules and deterministic counters are implemented. URL/GraphQL common parsers are being tested. Android OkHttp native HTTP execution is implemented with focused tests; Ktor execution is now tested on Android and iOS. Configured iOS URLSession selected faults now pass Foundation callback tests. Full forwarding/parity validation and streaming adapters are still incomplete. This document is the delivery contract, not a feature announcement.
