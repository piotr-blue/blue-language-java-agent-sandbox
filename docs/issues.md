# Known mapping gaps / open questions

This file tracks DSL-to-processor semantics that are currently unclear or intentionally deferred.

## 1) Policy objects are metadata (not enforced by processor core)

`policies` such as:

- `contractsChangePolicy`
- `changesetAllowList`

are authored and preserved in documents, but there is no built-in processor enforcement hook in current runtime flow.

Implication:

- `directChangeWithAllowList(...)` generates policy metadata and executable contracts.
- Runtime allow-list validation is currently out-of-band.

## 2) Triggered event workflows are internal-event driven

`onEvent(...)` helper methods compile to workflows bound to `triggeredEventChannel`.
That channel is processor-managed and does not consume arbitrary external events directly.

Implication:

- External stimuli should enter through non-managed channels/operations.
- Those handlers can emit events that then fan into triggered-event workflows.

## 3) Operation request schema is required for strict matching

Current operation matcher requires the operation contract to define a request shape (`request` node).

Implication:

- “No request schema” operations do not match strict operation envelopes.
- `directChangeWithAllowList(...)` now injects an empty request object schema to stay callable in strict mode.
- Other DSL helpers that intentionally use no-request operations may need the same treatment if they are expected to be runtime-callable.
