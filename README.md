# Blue Language Java — Latest Authoring Surface

This repository contains the Blue Language Java runtime plus sample DSL authoring for MyOS + PayNote flows.

## Authoring principles

- Author documents with `MyOsDsl.bootstrap()` + `DocumentBuilder` (or wrappers built on top of it).
- Keep channels abstract in contracts; bind concrete identities in `channelBindings`.
- Use typed event classes by default.
- Use structured JS builders (`JsProgram`, `JsOutputBuilder`, `JsPatchBuilder`, `JsArrayBuilder`, `JsObjectBuilder`, `JsCommon`) for deterministic JavaScript steps.
- Keep one latest API surface (no versioned authoring packages).

## Generic docs

Use `BlueDoc` for any document type:

- `name/type/description`
- `participant(...)`, `participants(...)`
- `operation(...).channel(...).requestType(...).steps(...).done()`
- `participantsUnion(...)`
- build document separately from bootstrap bindings

## PayNotes

Use `PayNotes.payNote(name)` as the primary entry point.

Key behavior:

- implicit `payerChannel`, `payeeChannel`, `guarantorChannel`
- participant-first API (channel-key based; no role framework required)
- capture-first API:
  - `capture().lockOnInit()`
  - `capture().unlockWhenEventArrives(...)`
  - `capture().unlockOnOperation(...)`
  - atomic helpers: `captureLockedUntilOperation(...)`, `captureLockedUntilEvent(...)`, `captureLockedUntilDocPathChanges(...)`
- money ergonomics:
  - `currency(IsoCurrency)`
  - `amountTotalMinor(long)`
  - `amountTotalMajor(BigDecimal/String)` with strict scale validation
- fail-fast validation: locked capture must have at least one unlock path

## Examples

- Complexity ladder (tiny → medium → huge JS):
  - `src/test/java/blue/language/samples/paynote/examples/PayNoteComplexityLadderExamples.java`
- Class extension flow (`MyPayNote`):
  - `src/test/java/blue/language/samples/paynote/examples/MyPayNote.java`
- Template → specialize → instantiate shipment chain:
  - `src/test/java/blue/language/samples/paynote/examples/shipment/`
- Voucher scenarios:
  - `src/test/java/blue/language/samples/paynote/examples/voucher/`

## Operation invocation shape

Operations are invoked on timelines with an `Operation Request` message shape:

```yaml
message:
  type: Conversation/Operation Request
  operation: increment
  request: 5
  document:
    blueId: ...
```

Each example has tests asserting document type/name, contracts/workflows, and key JS fragments.
