# Document processor parity gaps (pinned target)

Pinned JS target:

- Repository: `bluecontract/blue-js`
- Commit: `bf9e1cfd200d35801d8237f7080895372c1572c6`
- Package: `libs/document-processor`

This document tracks the remaining high-risk parity gaps and the implementation decision for each area.

---

## 1) Runtime JSON patch pointer semantics

- **JS references**
  - `src/__tests__/DocumentProcessingRuntimeJsonPatchTest.test.ts`
  - `src/runtime/patch-engine.ts`
- **Current Java behavior**
  - Patch paths go through strict JSON Pointer escape validation/unescape in runtime patch traversal.
  - Additional strict mixed object/array path rules beyond JS patch runtime behavior.
- **Target behavior**
  - Match JS runtime patch engine semantics for patch-path token handling and traversal.
  - Keep pointer utility strictness for non-patch APIs unless explicitly required otherwise.
- **Resolution**
  - Align `PatchEngine` patch-path traversal and tests to JS behavior.
  - Keep contract explicit in tests.

---

## 2) Sequential workflow operation request matching

- **JS references**
  - `src/registry/__tests__/sequential-workflow-operation-processor.test.ts`
  - `src/registry/processors/sequential-workflow-operation-processor.ts`
  - `src/registry/processors/workflow/operation-matcher.ts`
- **Current Java behavior**
  - Matcher accepts broader direct event shape when `operation` and `request` properties are present.
- **Target behavior**
  - JS-strict request gating by default (timeline/envelope/type rules).
- **Resolution**
  - Default Java matcher to strict JS-compatible gate.
  - Provide explicit compatibility mode to allow direct-shape matching for legacy callers.
  - Cover both strict and compat behavior in tests.

---

## 3) Result helper API

- **JS references**
  - `src/types/result.ts`
  - `src/types/__tests__/result.test.ts`
- **Current Java behavior**
  - `DocumentProcessingResult` provides immutable run-result factories/accessors only.
- **Target behavior**
  - Provide full functional helpers equivalent to JS result helpers:
    - `ok`, `err`, `isOk`, `isErr`, `map`, `mapErr`, `andThen`, `unwrapOr`, `unwrapOrElse`, `match`.
- **Resolution**
  - Add generic Java Result helper type under processor types.
  - Add dedicated parity tests mirroring JS helper semantics.

---

## 4) Node canonicalizer parity suite

- **JS references**
  - `src/util/node-canonicalizer.ts`
  - `src/util/__tests__/node-canonicalizer.test.ts`
- **Current Java behavior**
  - Canonical size exists and is used in gas accounting.
  - No dedicated 1:1 canonicalizer parity suite; canonical signature logic duplicated in engine.
- **Target behavior**
  - Dedicated Java canonicalizer parity tests for signature/size semantics.
  - Single canonicalizer utility for signature and size.
- **Resolution**
  - Add canonical signature helper to `NodeCanonicalizer`.
  - Delegate engine canonical signature generation to utility.
  - Add dedicated parity test class.

---

## 5) QuickJS runtime architecture

- **JS references**
  - `src/util/expression/quickjs-evaluator.ts`
  - `src/util/expression/quickjs-expression-utils.ts`
  - `src/registry/processors/steps/javascript-code-step-executor.ts`
  - JS QuickJS tests under `src/util/expression/__tests__/*` and step executor tests.
- **Current Java behavior**
  - Sidecar uses Node `vm` execution with timeout-based interruption and heuristic gas estimation.
- **Target behavior**
  - Sidecar executes with `@blue-quickjs/quickjs-runtime` (same runtime family as JS).
  - Align error classification, fuel accounting, and result protocol shape with JS expectations.
- **Resolution**
  - Replace sidecar execution backend with quickjs-runtime.
  - Update Java runtime/evaluator integration and parity tests accordingly.
  - Update architecture documentation with final design decisions and constraints.
