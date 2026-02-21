# Document Processor Java Parity Plan (vs `blue-js`)

## Scope

Bring Java `document-processor` behavior closer to `blue-js/libs/document-processor`, while keeping changes incremental and compatible with current Java tests.

## Iteration chunks

1. **Core contract/type compatibility**
   - Add support for JS/Core-prefixed contract BlueIds alongside existing Java BlueIds.
   - Keep existing Java IDs working.
   - **Status:** done

2. **Capability-failure behavior parity**
   - Unknown contract BlueIds should return capability failure (`must-understand`) instead of runtime conversion crashes.
   - **Status:** done

3. **QuickJS integration architecture**
   - Define Java integration path for `blue-quickjs` parity (deterministic execution + gas accounting + ergonomics).
   - **Status:** done (design documented; implementation staged)

4. **Workflow/timeline processor parity (next)**
   - Port missing JS processors (timeline, composite timeline, sequential workflow, operation workflow).
   - Add step execution pipeline (trigger event / update document / javascript code).
   - **Status:** pending

5. **Expression/runtime parity (next)**
   - Add QuickJS-backed expression evaluation (`${...}`), document bindings, and host-call gas charging.
   - Port integration tests around embedded routing, deep document-update propagation, and workflow registration timing.
   - **Status:** pending

---

## QuickJS integration architecture decisions

### Decision A — introduce a script runtime SPI (accepted)

Define an internal Java SPI rather than binding processor logic directly to one engine.

Suggested internal contract:

- `ScriptRuntime.evaluate(code, bindings, options) -> ScriptResult`
- `ScriptResult` includes:
  - `value` (JSON-compatible)
  - `wasmGasUsed` (or generic runtime gas units)
  - optional diagnostics

Why:

- Keeps processor code independent from transport details.
- Makes sidecar vs embedded runtime swappable.
- Enables deterministic testing with fakes.

### Decision B — two runtime adapters (accepted)

Implement two adapters behind the SPI:

1. **Preferred adapter: sidecar QuickJS host process**
   - Run a dedicated worker process that uses `@blue-quickjs/quickjs-runtime`.
   - Exchange strict JSON envelopes over stdio (or unix socket).
   - Keep one long-lived process per JVM/service instance for throughput.

2. **Fallback adapter: in-memory Java JS engine**
   - Optional compatibility fallback where sidecar is unavailable.
   - Explicitly marked as non-parity mode.

Why sidecar first:

- `blue-quickjs` is already canonical in JS.
- Behavioral drift is smaller than re-implementing runtime semantics in Java.
- Easier to keep ABI/manifest parity.

### Decision C — canonical JSON boundary for bindings/results (accepted)

Bindings (`event`, `eventCanonical`, `steps`, `document(...)`, `currentContract`) and results cross runtime boundary as canonical JSON values only.

Rules:

- No Java object leakage across boundary.
- Convert `Node` <-> JSON at boundary.
- Treat `null` and missing distinctly, matching JS behavior.

### Decision D — gas accounting integration (accepted)

Map runtime gas to existing `GasMeter`:

- Continue charging Java-side fixed schedule (handler overhead, patch, routing, bridge, checkpoint).
- Add runtime-provided gas usage (`wasmGasUsed`) through dedicated meter hook.
- Keep charge points explicit in step executors for reproducibility.

### Decision E — safe defaults and operability (accepted)

- Default runtime mode should fail closed for JS step execution when QuickJS adapter is not configured.
- Add clear startup diagnostics for missing runtime executable/dependency.
- Expose configuration through `DocumentProcessor.builder()`:
  - runtime mode (`SIDECAR`, `FALLBACK`, `DISABLED`)
  - process path/command
  - runtime timeout
  - max message size
  - gas limits

### Decision F — parity-driven test migration order (accepted)

Port test sets in this order:

1. Unit parity for loader/runner/scope/checkpoint/termination.
2. Workflow step unit tests (`Update Document`, `Trigger Event`, `JavaScript Code`).
3. Embedded integration scenarios:
   - deep document-update propagation
   - protected path removal termination
   - dynamic workflow registration deferred execution
   - trigger-event nested document non-leakage
4. Timeline/composite timeline behavior and per-scope checkpoint isolation.

---

## Notes

- Current commits intentionally focused on low-risk parity increments first.
- Full JS processor parity is larger and should continue in small commits following chunks 4-5 above.
