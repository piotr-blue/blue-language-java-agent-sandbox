# Blue Language Java — vNext Sample Authoring Guide

This repository contains the Blue Language Java runtime plus sample DSL authoring for MyOS + PayNote flows.

## vNext sample authoring principles

- Author bootstrap documents via `MyOsDsl.bootstrap()` + `DocumentBuilder`.
- Keep channels abstract in contracts; bind concrete identities in `channelBindings`.
- Use typed event classes (`TypeRef` mapping) by default.
- Use structured JS builders (`JsProgram`, `JsOutputBuilder`, `JsPatchBuilder`, `JsArrayBuilder`, `JsObjectBuilder`, `JsCommon`) instead of large hand-written string blobs.
- Build PayNote flows with the vNext facade (`PayNotes` + `PayNoteBuilderVNext`) and fall back to generic DSL only when needed.

## Key vNext capabilities

### 1) Generic DSL

- document properties (scalar/object)
- contracts: channels, operations, workflows, lifecycle and triggered handlers
- policies: contracts change policy, changeset allow-lists, operation rate-limits

### 2) Channel hierarchy + bindings

- bulk abstract channels: `timelineChannels(...)`
- composite channels: `compositeTimelineChannel(...)`
- source binding contracts: `channelSourceBinding(...)`
- role bindings: `bindRole(...)`, `bindRoleAccount(...)`, `bindRoleEmail(...)`

### 3) Template specialize / instantiate pipeline

- immutable template wrapper: `DocTemplate`
- specialization: `.specialize(...)`
- instantiation: `.instantiate(...)`

This supports production-style flow:

template → specialization (e.g. EUR200/CHF + DHL) → final participant bindings (e.g. Alice/Bob/Bank).

### 4) PayNote SDK facade

`PayNoteBuilderVNext` exposes high-level methods for:

- reserve/capture/refund/release/cancel
- card lock/unlock workflows
- child issuance on events
- lifecycle hooks (`onFundsReserved`, `onCaptureRequested`, `onFundsCaptured`, `onReleased`)
- once/barrier helpers

## vNext examples

See `src/test/java/blue/language/samples/paynote/examples/vnext` for:

- iPhone shipment escrow
- shipment template chain
- subscription paynote
- marketplace split paynote
- agent budget paynote
- milestone contractor paynote
- reverse payment voucher paynote
- recruitment classifier vNext

All examples include tests asserting key type/channel/workflow structure and critical generated JS fragments.
