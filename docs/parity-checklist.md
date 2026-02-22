# Document Processor parity checklist

Pinned target: `blue-js` commit `bf9e1cfd200d35801d8237f7080895372c1572c6` (`libs/document-processor`)

Status legend:

- `DONE` — behavior implemented in Java and covered by tests
- `IN_PROGRESS` — partially aligned, gaps remain
- `TODO` — not yet implemented

---

## 1) Engine/runtime surface

| Area | JS reference | Java reference | Status |
|---|---|---|---|
| Processor engine orchestration | `src/engine/processor-engine.ts` | `processor/ProcessorEngine.java` | IN_PROGRESS |
| Scope executor lifecycle | `src/engine/scope-executor.ts` | `processor/ScopeExecutor.java` | IN_PROGRESS |
| Channel runner + checkpoints | `src/engine/channel-runner.ts` | `processor/ChannelRunner.java` | IN_PROGRESS |
| Contract loader validation | `src/engine/contract-loader.ts` | `processor/ContractLoader.java` | IN_PROGRESS |
| Contract bundle semantics | `src/engine/contract-bundle.ts` | `processor/ContractBundle.java` | IN_PROGRESS |
| Patch engine | `src/runtime/patch-engine.ts` | `processor/PatchEngine.java` | IN_PROGRESS |
| Runtime state + emissions | `src/runtime/document-processing-runtime.ts` | `processor/DocumentProcessingRuntime.java` | IN_PROGRESS |
| Termination service semantics | `src/engine/termination-service.ts` | `processor/TerminationService.java` | IN_PROGRESS |

## 2) Default processors and registry

| Area | JS reference | Java reference | Status |
|---|---|---|---|
| Registry lookup by BlueId/type chain | `src/registry/contract-processor-registry.ts` | `processor/ContractProcessorRegistry.java` | IN_PROGRESS |
| Default processor registration | `src/registry/contract-processor-registry-builder.ts` | `processor/ContractProcessorRegistryBuilder.java` | IN_PROGRESS |
| Timeline channel processor | `src/registry/processors/timeline-channel-processor.ts` | `processor/registry/processors/TimelineChannelProcessor.java` | IN_PROGRESS |
| Composite timeline channel processor | `src/registry/processors/composite-timeline-channel-processor.ts` | `processor/registry/processors/CompositeTimelineChannelProcessor.java` | IN_PROGRESS |
| MyOS timeline channel processor | `src/registry/processors/myos-timeline-channel-processor.ts` | `processor/registry/processors/MyOSTimelineChannelProcessor.java` | IN_PROGRESS |
| Sequential workflow handler processor | `src/registry/processors/sequential-workflow-processor.ts` | `processor/registry/processors/SequentialWorkflowHandlerProcessor.java` | IN_PROGRESS |
| Sequential workflow operation processor | `src/registry/processors/sequential-workflow-operation-processor.ts` | `processor/registry/processors/SequentialWorkflowOperationProcessor.java` | IN_PROGRESS |
| Operation marker processor | `src/registry/processors/operation-marker-processor.ts` | `processor/registry/processors/OperationMarkerProcessor.java` | IN_PROGRESS |

## 3) Expression + QuickJS runtime

| Area | JS reference | Java reference | Status |
|---|---|---|---|
| QuickJS evaluator | `src/util/expression/quickjs-evaluator.ts` | `processor/script/QuickJSEvaluator.java` | IN_PROGRESS |
| Expression utils/traversal | `src/util/expression/quickjs-expression-utils.ts` | `processor/script/QuickJsExpressionUtils.java` | IN_PROGRESS |
| QuickJS config exports | `src/util/expression/quickjs-config.ts` | `processor/script/QuickJsConfig.java` | DONE |
| Script runtime integration | JS runtime usage in evaluator/steps | `processor/script/*`, `tools/quickjs-sidecar/index.js` | IN_PROGRESS |
| JavaScript Code step | `src/registry/processors/steps/javascript-code-step-executor.ts` | `processor/workflow/steps/JavaScriptCodeStepExecutor.java` | IN_PROGRESS |

## 4) Workflow step runtime

| Area | JS reference | Java reference | Status |
|---|---|---|---|
| Step runner framework | `src/registry/processors/workflow/step-runner.ts` | `processor/workflow/WorkflowStepRunner.java` | IN_PROGRESS |
| Update Document step | `src/registry/processors/steps/update-document-step-executor.ts` | `processor/workflow/steps/UpdateDocumentStepExecutor.java` | IN_PROGRESS |
| Trigger Event step | `src/registry/processors/steps/trigger-event-step-executor.ts` | `processor/workflow/steps/TriggerEventStepExecutor.java` | IN_PROGRESS |
| Operation matcher helpers | `src/registry/processors/workflow/operation-matcher.ts` | `processor/registry/processors/SequentialWorkflowOperationProcessor.java` | IN_PROGRESS |

## 5) Merge behavior

| Area | JS reference | Java reference | Status |
|---|---|---|---|
| ExpressionPreserver | `src/merge/processors/ExpressionPreserver.ts` | `merge/processor/ExpressionPreserver.java` | IN_PROGRESS |
| Default merge pipeline ordering | `src/merge/utils/default.ts` | `Blue#createDefaultNodeProcessor` | IN_PROGRESS |

## 6) Constants, pointers, error/result model

| Area | JS reference | Java reference | Status |
|---|---|---|---|
| Processor constants | `src/constants/processor-contract-constants.ts` | `processor/util/ProcessorContractConstants.java` | DONE |
| Pointer constants | `src/constants/processor-pointer-constants.ts` | `processor/util/ProcessorPointerConstants.java` | DONE |
| Pointer utils | `src/util/pointer-utils.ts` | `processor/util/PointerUtils.java` | DONE |
| Processing result shape | `src/types/document-processing-result.ts` | `processor/DocumentProcessingResult.java` | DONE |
| Processor error factory model | `src/types/errors.ts` | `processor/types/*` | DONE |

## 7) Gas accounting + canonicalization

| Area | JS reference | Java reference | Status |
|---|---|---|---|
| Canonical signature/size helpers | `src/util/node-canonicalizer.ts` | `processor/util/NodeCanonicalizer.java` + `ProcessorEngine#canonicalSignature` | DONE |
| Gas meter methods | `src/runtime/gas-meter.ts` | `processor/GasMeter.java` | DONE |
| Gas schedule conversions | `src/runtime/gas-schedule.ts` | `processor/ProcessorGasSchedule.java` | DONE |

## 8) Test coverage mapping

| Area | JS tests | Java tests | Status |
|---|---|---|---|
| Constants/pointers | `src/constants/__tests__/*`, `src/util/__tests__/pointer-utils.test.ts` | `processor/util/*Test.java` | IN_PROGRESS |
| Runtime core | `src/runtime/__tests__/*` | `processor/DocumentProcessingRuntime*Test.java`, `EmissionRegistryTest.java` | IN_PROGRESS |
| Engine core | `src/engine/__tests__/*` | `processor/*Boundary*`, `CheckpointManagerTest`, `ChannelRunnerTest`, etc. | IN_PROGRESS |
| Registry/timeline/workflow | `src/registry/__tests__/*` | `ContractProcessorRegistryBuilderDefaultsTest`, `CompositeTimelineChannelProcessorTest`, `TimelineChannelProcessorTest`, `MyOSTimelineChannelProcessorTest`, `SequentialWorkflowProcessorTest`, `workflow/WorkflowStepRunnerTest` | IN_PROGRESS |
| Steps + expressions + quickjs | `src/registry/processors/steps/__tests__/*`, `src/util/expression/__tests__/*` | `SequentialWorkflowProcessorTest`, `QuickJsSidecarRuntimeTest`, `QuickJsExpressionUtilsTest`, `QuickJsConfigTest`, `CodeBlockEvaluationErrorTest` | IN_PROGRESS |
| Integration parity | `src/__tests__/integration/**/*` | partial processor integration tests | IN_PROGRESS |
| Golden parity fixtures | fixture/golden strategy in plan | `parity-fixtures/*`, `ParityFixturesTest` | IN_PROGRESS |

---

## Next updates

This checklist is updated after each parity feature group lands, with status transitions and Java test links.
Current in-flight work:
- Expression utility parity still needs closer picomatch-equivalent semantics for complex glob patterns (brace expansion and character-class matching now supported, but extglob and other advanced picomatch features remain partial).
- Registry/type-chain lookup now supports inline derived type-chain fallback during node→class resolution (workflow, operation handler, and operation marker contracts), but broader semantic subtype matching via repository type-extension graphs remains partial.
- QuickJS sidecar failures now preserve structured error names/messages (and stack availability marker), but evaluator-level error-shape parity still needs further hardening.
- Golden fixtures now cover sequential workflow happy path, operation mismatch scenarios (operation key mismatch, pinned document mismatch, operation-vs-handler channel conflict), direct/complex operation requests, handler event filtering, derived change workflow, dynamic workflow deferred activation across multi-event runs, derived workflow/operation/operation-marker type-chain fallback, operation-marker type preservation, JS emit callback/deterministic globals/document blueId/canonical reads/document.get aliases/eventCanonical helper behavior, Trigger Event nested-document expression preservation, Process Embedded protected-path removal fatal termination, Process Embedded multi-path independence/removal/fatal-write scenarios, embedded routing bridge propagation, shared-timeline duplicate checkpoint skipping across embedded/root scopes, embedded cross-triggering via document update channels, MyOS timeline, composite timeline, and expected initialization failures/cycle validation; broader integration fixture migration remains.
