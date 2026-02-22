# QuickJS integration (Java parity track)

This document describes the Java QuickJS integration architecture used by the document processor parity work.

## Runtime components

- `ScriptRuntime` (`blue.language.processor.script.ScriptRuntime`)
  - Runtime SPI for script evaluation.
  - Accepts a `ScriptRuntimeRequest` (`code`, `bindings`, `wasmGasLimit`).
  - Returns `ScriptRuntimeResult` (`value`, `wasmGasUsed`, `wasmGasRemaining`).
- `QuickJsSidecarRuntime`
  - Default SPI implementation backed by a managed Node sidecar process.
  - Uses line-delimited JSON request/response messages over stdio.
- `QuickJSEvaluator`
  - Java-facing evaluator wrapper that maps sidecar failures to `CodeBlockEvaluationError`.
- `QuickJsExpressionUtils`
  - Expression helpers and traversal utilities used by workflow step executors.
  - Supports:
    - standalone expression detection `${...}`
    - template substitution with embedded expressions
    - expression evaluation wrapped as `return (<expr>);` for JS-equivalent object-literal semantics
    - include/exclude pointer predicate matching

## Sidecar protocol

Current sidecar entrypoint: `tools/quickjs-sidecar/index.js`.

Message format:

- Request:
  - `id` (uuid string)
  - `code` (string)
  - `bindings` (json object)
  - `wasmGasLimit` (string|number|null)
- Response:
  - `id`
  - `ok` (boolean)
  - `resultDefined` (boolean, whether script returned a defined value; distinguishes `undefined` from explicit `null`)
  - `result` (any, when ok)
  - `error` (object|string, when not ok)
  - `wasmGasUsed` (string|number|null)
  - `wasmGasRemaining` (string|number|null)

`QuickJsSidecarRuntime` validates `id` correspondence and surfaces protocol/runtime errors as `ScriptRuntimeException`.

`ScriptRuntimeResult` now carries:

- `value` (evaluated value or `null`)
- `valueDefined` (true when script produced a defined value, false for `undefined`)
- gas fields (`wasmGasUsed`, `wasmGasRemaining`)

Timeout/error normalization parity:

- Sidecar classifies VM timeout failures under tiny `wasmGasLimit` budgets as:
  - `name: "OutOfGasError"`
  - `message: "OutOfGas: execution exceeded wasm gas limit"`
- `QuickJSEvaluator`/`QuickJsSidecarRuntime` preserve this in surfaced exception text.
- Successful sidecar evaluations now report deterministic non-zero `wasmGasUsed` estimates and bounded `wasmGasRemaining` values derived from code complexity and numeric literals (parity-friendly metering shape while full QuickJS fuel parity is still pending).

## Gas mapping

QuickJS gas constants are exported via `QuickJsConfig` and backed by `ProcessorGasSchedule`.

- `WASM_FUEL_PER_HOST_GAS_UNIT`
- `DEFAULT_JS_STEP_HOST_GAS_LIMIT`
- `DEFAULT_EXPRESSION_HOST_GAS_LIMIT`
- `DEFAULT_WASM_GAS_LIMIT`
- `DEFAULT_EXPRESSION_WASM_GAS_LIMIT`
- `hostGasToWasmFuel(...)`

Workflow expression/step evaluation charges gas through:

- `ProcessorExecutionContext#chargeWasmGas(...)`
- `GasMeter#chargeWasmGas(...)`

## Step executor bindings

`QuickJSStepBindings` provides default bindings used by step executors:

- `event` (simple/plain JSON snapshot of current event)
- `eventCanonical` (official/canonical JSON event snapshot)
- `steps` (results from prior workflow steps)
- `currentContract` (simple contract snapshot read from `/contracts/<key>`)
- `currentContractCanonical` (official contract snapshot read from `/contracts/<key>`)
- internal document snapshots used by evaluator prelude:
  - `__documentDataSimple`
  - `__documentDataCanonical`
  - `__scopePath`

Evaluator prelude host APIs:

- `document(pointer?)`
  - resolves relative pointers against `__scopePath`
  - reads from simple snapshot
  - unwraps scalar node wrappers
  - for raw terminal segments (`/blueId`, `/name`, `/description`, `/value`) falls back to canonical snapshot when needed
- `document.canonical(pointer?)`
  - resolves from canonical snapshot (preserves metadata/type wrappers)
- `document.get(pointer?)` / `document.getCanonical(pointer?)`
  - alias helpers for parity with host-handler style API names used in JS workflows
- `canon.at(value, pointer)` and `canon.unwrap(value)`
  - `canon.unwrap(value, deep?)` now supports:
    - deep unwrapping (default) across canonical wrappers (`{ value: ... }`, `{ items: [...] }`) and nested objects
    - shallow mode (`deep=false`) that only unwraps the top-level wrapper

QuickJS evaluator binding defaults/fallbacks:

- missing `event` defaults to `null`
- missing `eventCanonical` defaults to `event`
- missing `steps` defaults to `[]`
- missing `currentContract` defaults to `null`
- missing `currentContractCanonical` defaults to `currentContract`

Emit callback behavior:

- when no host emit callback is supplied, sidecar `emit(...)` values are returned via the existing `result.events[]` envelope used by workflow steps.
- when a host emit callback is supplied to `QuickJSEvaluator` (direct evaluator usage), emitted values are forwarded to the callback and the evaluator returns the plain script result value (JS parity behavior).
- evaluator now preserves `undefined` vs explicit `null` semantics in this callback path as well (using sidecar `resultDefined` / envelope `__resultDefined` metadata), enabling workflow step-runner parity where only `undefined` skips step-result registration.

Document callback behavior (direct evaluator usage):

- `QuickJSEvaluator` accepts:
  - `document` as `Function<Object, Object>` (simple callback), or
  - `QuickJSEvaluator.DocumentBinding` (simple + canonical callbacks).
- for literal pointer calls in script code (`document('/...')`, `document.get('/...')`, `document.canonical('/...')`, `document.getCanonical('/...')`), evaluator materializes pointer snapshots from callback reads and feeds them into prelude document helpers.
- existing workflow-step path (`QuickJSStepBindings` with `__documentDataSimple` / `__documentDataCanonical`) remains the default and is unchanged.

Document snapshots are charged via `chargeDocumentSnapshot`.
WASM usage is charged via `chargeWasmGas`.

## Failure handling

- Sidecar startup, protocol, or evaluation failures => `ScriptRuntimeException`.
  - sidecar error payloads (`name`, `message`, optional `stack`) are normalized into readable exception text.
  - structured error metadata is exposed for downstream parity/error handling:
    - `ScriptRuntimeException#errorName()`
    - `ScriptRuntimeException#runtimeMessage()`
    - `ScriptRuntimeException#stackAvailable()`
- Evaluator wraps script failures => `CodeBlockEvaluationError` with:
  - original source code available via `code()`
  - truncated code snippet in message (`Failed to evaluate code block: ...`)
  - structured runtime error metadata passthrough for wrapped sidecar failures:
    - `CodeBlockEvaluationError#runtimeErrorName()`
    - `CodeBlockEvaluationError#runtimeErrorMessage()`
    - `CodeBlockEvaluationError#runtimeStackAvailable()`
- Evaluator validates binding keys against supported runtime bindings and rejects unsupported keys up-front (`Unsupported QuickJS binding: "<key>"`).
- Evaluator also validates host-handler binding shapes for parity:
  - `document` must be function-shaped (non-null non-function values are rejected)
  - `emit` must be function-shaped (non-null non-function values are rejected)
- Step processors convert fatal script usage issues to processor fatal termination through `ProcessorExecutionContext#throwFatal`.
- Sidecar supports `emit(...)` callback parity by collecting emitted payloads and returning them in `result.events[]`; JavaScript step executor emits these events through processor runtime.
- JavaScript step event emission now promotes payload `type` values (string or `{ blueId }`) into node semantic type metadata before enqueueing, preserving Triggered Event Channel routing behavior for JS-produced events.
- Sidecar masks non-deterministic globals (`Date`, `process`) to align workflow execution with deterministic runtime expectations.

