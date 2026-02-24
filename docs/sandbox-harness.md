# Processor Harness Sandbox

The processor harness provides a lightweight runtime host around `DocumentProcessor` so DSL-authored documents can be executed with deterministic timelines.

## Main API

- `ProcessorHarness`
  - `start(Node documentOrBootstrap) -> ProcessorSession`
- `ProcessorSession`
  - `init()` / `initSession()`
  - `pushEvent(Node event)`
  - `pushConversationTimelineEntry(participantKey, message)`
  - `pushMyOsTimelineEntry(participantKey, message)`
  - `callOperation(participantKey, operationName, requestPayload)`
  - `runOne()` / `runUntilIdle()`
  - `document()`, `emittedEvents()`, `gasPerRun()`
  - `registerParticipant(key, timelineId)`
  - `timelineStore()`

## Bootstrap behavior

`ProcessorHarness.start(...)` accepts either:

1. Raw document node (contracts at root), or
2. MyOS-style bootstrap envelope (`document` + optional `channelBindings`).

When bootstrap is provided:

- The harness unwraps `document`.
- Participants are pre-registered from timeline contracts and bindings.
- Missing timeline IDs are synthesized so operation/timeline routing can execute in tests.

## Canonical event generation

Use `EventFactory` for envelope-safe test events:

- Conversation timeline entries,
- MyOS timeline entries,
- Operation request message envelopes.

This keeps event shape aligned with parity fixture semantics.
