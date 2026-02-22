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
  - `result` (any, when ok)
  - `error` (object|string, when not ok)
  - `wasmGasUsed` (string|number|null)
  - `wasmGasRemaining` (string|number|null)

`QuickJsSidecarRuntime` validates `id` correspondence and surfaces protocol/runtime errors as `ScriptRuntimeException`.

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

- `event` (json snapshot of current event)
- `steps` (results from prior workflow steps)
- `document` (snapshot of current document)

Document reads also charge snapshot gas via `chargeDocumentSnapshot`.

## Failure handling

- Sidecar startup, protocol, or evaluation failures => `ScriptRuntimeException`.
- Evaluator wraps script failures => `CodeBlockEvaluationError`.
- Step processors convert fatal script usage issues to processor fatal termination through `ProcessorExecutionContext#throwFatal`.

