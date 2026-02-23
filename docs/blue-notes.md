# Blue Spec Alignment Notes (internal)

## 1) Operation Request model

Operations are invoked through timeline messages carrying:

- operation name
- request payload
- target document reference

Reference shape:

```yaml
message:
  type: Conversation/Operation Request
  operation: <operationName>
  request: <payload>
  document:
    blueId: ...
```

SDK implication:

- operation DSL should expose request typing (`requestType(...)`) for clarity and runtime safety.

## 2) Contract typing and channels

Channel contracts are type-driven:

- `Core/Channel`
- `Conversation/Timeline Channel` (subtype)
- `MyOS/MyOS Timeline Channel` (subtype)

SDK implication:

- default participants should be abstract timeline channels.
- concrete identities should be introduced in bootstrap bindings or explicit overlays.

## 3) Trigger patterns supported

Important patterns used by the SDK:

- lifecycle-triggered workflows (`Core/Document Processing Initiated`)
- event-triggered workflows (`Core/Triggered Event Channel`)
- document-change-triggered workflows (`Core/Document Update Channel`)
- operation workflows (`Conversation/Sequential Workflow Operation`)

SDK implication:

- expose one-liner helpers for common trigger patterns (capture lock/unlock, reserve/capture, refund/release, child issuance).

## 4) Policies

Contracts-change policies can restrict broad mutation operations by path allow-lists.

SDK implication:

- `directChangeWithAllowList(...)` should be first-class for controlled mutation flows.

## 5) Deterministic JS

Structured JS builder output should avoid non-deterministic constructs.

SDK implication:

- use `JsProgram` + `JsOutputBuilder` + `JsPatchBuilder` + `JsArrayBuilder` + `JsObjectBuilder`.
- keep examples deterministic for testability.
