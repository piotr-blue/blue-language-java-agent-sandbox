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
| ExpressionPreserver | `src/merge/processors/ExpressionPreserver.ts` | `merge/processor/ExpressionPreserver.java` | DONE |
| Default merge pipeline ordering | `src/merge/utils/default.ts` | `Blue#createDefaultNodeProcessor` | DONE |

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
| Constants/pointers | `src/constants/__tests__/*`, `src/util/__tests__/pointer-utils.test.ts` | `processor/util/*Test.java` | DONE |
| API facade | `src/api/__tests__/document-processor.test.ts` | `DocumentProcessorApiParityTest`, `DocumentProcessorInitializationTest`, `DocumentProcessorCapabilityTest` | DONE |
| Runtime core | `src/runtime/__tests__/*` | `processor/DocumentProcessingRuntime*Test.java`, `DocumentProcessingRuntimeParityTest`, `PatchEngineParityTest`, `ScopeRuntimeContextTest`, `EmissionRegistryTest.java` | DONE |
| Engine core | `src/engine/__tests__/*` | `processor/*Boundary*`, `CheckpointManagerTest`, `ChannelRunnerTest`, `ContractBundleParityTest`, `ContractLoaderParityTest`, `ScopeExecutorParityTest`, `ScopeExecutorDerivedChannelParityTest`, `TerminationServiceParityTest`, etc. | DONE |
| Contract model schemas | `src/model/__tests__/contract-models.test.ts` | `ContractModelsParityTest`, `ContractMappingIntegrationTest` | DONE |
| Registry/timeline/workflow | `src/registry/__tests__/*` | `ContractProcessorRegistryBuilderDefaultsTest`, `CompositeTimelineChannelProcessorTest`, `TimelineChannelProcessorTest`, `MyOSTimelineChannelProcessorTest`, `SequentialWorkflowProcessorTest`, `DeepEmbeddedInitializationPropagationTest`, `DeepEmbeddedPropagationIntegrationTest`, `CrossTriggeringIntegrationTest`, `ProcessMultiPathsIntegrationTest`, `ProcessProtectedPathRemovalTerminatesRootIntegrationTest`, `DynamicWorkflowRegistrationIntegrationTest`, `SharedTimelineCheckpointIntegrationTest`, `TriggerEventIntegrationTest`, `TriggerEventStepLeakageReproIntegrationTest`, `TriggerEventStepNoDocumentProcessingIntegrationTest`, `EmbeddedRoutingBridgeIntegrationTest`, `workflow/WorkflowStepRunnerTest` | IN_PROGRESS |
| Steps + expressions + quickjs | `src/registry/processors/steps/__tests__/*`, `src/util/expression/__tests__/*` | `SequentialWorkflowProcessorTest`, `QuickJsSidecarRuntimeTest`, `QuickJSEvaluatorTest`, `QuickJSEvaluatorGasTest`, `QuickJsFuelCalibrationTest`, `QuickJsExpressionUtilsTest`, `QuickJsConfigTest`, `CodeBlockEvaluationErrorTest` | IN_PROGRESS |
| Integration parity | `src/__tests__/integration/**/*` | `DynamicWorkflowRegistrationIntegrationTest`, `TriggerEventStepLeakageReproIntegrationTest`, `TriggerEventStepNoDocumentProcessingIntegrationTest`, `DeepEmbeddedPropagationIntegrationTest`, `CrossTriggeringIntegrationTest`, `ProcessMultiPathsIntegrationTest`, `ProcessProtectedPathRemovalTerminatesRootIntegrationTest`, `SharedTimelineCheckpointIntegrationTest`, `EmbeddedRoutingBridgeIntegrationTest` | DONE |
| Golden parity fixtures | fixture/golden strategy in plan | `parity-fixtures/*`, `ParityFixturesTest` | IN_PROGRESS |

---

## Next updates

This checklist is updated after each parity feature group lands, with status transitions and Java test links.
Current in-flight work:
- Test-migration status: constants/pointers, API facade, runtime core, engine core, contract-model schemas, merge behavior, and integration suites are now marked `DONE` in coverage mapping based on direct Java parity tests.
- JS integration directory parity migration is complete with direct Java integration tests for every `src/__tests__/integration/**/*` scenario in the pinned target commit.
- Merge parity baseline is complete: `ExpressionPreserver` is wired into the default merge pipeline before type assignment, and Java tests cover expression preservation plus regular-value passthrough behavior.
- Expression utility parity still needs closer picomatch-equivalent semantics for complex glob patterns (brace expansion, character classes, and extglob `@(...)`, `?(...)`, `+(...)`, `*(...)`, `!(...)` are supported, but advanced nested/edge picomatch semantics remain partial).
- Registry/type-chain lookup now supports inline derived type-chain fallback and provider-backed repository type-chain fallback during node→class resolution (workflow, operation handler, and operation marker contracts), but broader semantic subtype matching outside class-resolution paths remains partial.
- QuickJS sidecar failures now preserve structured error names/messages (and stack availability marker); evaluator now enforces supported binding-key validation and has dedicated migration coverage (`QuickJSEvaluatorTest` + `QuickJSEvaluatorGasTest`) including out-of-gas timeout classification for tiny wasm budgets. Sidecar now reports deterministic non-zero gas-used estimates with bounded remaining fuel, but exact QuickJS wasm-fuel accounting parity remains partial.
- QuickJS fuel calibration migration now has dedicated deterministic baseline coverage (`QuickJsFuelCalibrationTest`) for representative script complexity trends and repeated-run stability.
- Contract-model schema parity now includes MyOS marker contract mappings for `MyOS/Document Anchors`, `MyOS/Document Links`, `MyOS/MyOS Participants Orchestration`, `MyOS/MyOS Session Interaction`, and `MyOS/MyOS Worker Agency` (`ContractModelsParityTest`, `TypeClassResolverAliasTest`).
- Contract-loader parity now includes unsupported-contract MustUnderstand behavior and built-in contract loading coverage (`ContractLoaderParityTest`) in addition to composite-cycle and MyOS-marker loader tests.
- Contract-bundle/runtime parity now includes JS-equivalent bundle ordering/filtering/checkpoint tests (`ContractBundleParityTest`) plus direct patch-engine parity coverage (`PatchEngineParityTest`).
- Runtime parity coverage now includes JS-equivalent emission-registry ordering/scope-clear semantics (`EmissionRegistryTest`) and gas-meter scenario parity for initialization/scope depth, patch-size charging, and emit+cascade charging (`GasMeterParityTest`).
- Trigger Event integration parity now also includes the direct no-nested-processing scenario from JS integration tests (`TriggerEventIntegrationTest#triggerEventNestedDocumentPayloadIsNotProcessedAsDocument`).
- Processor execution-context parity now includes active/inactive patch behavior, allow-terminated patch flow, gas consumption, root emission recording, and termination delegation scenarios (`ProcessorExecutionContextTest`).
- Dynamic contract registration parity now also has direct Java integration coverage for deferred workflow activation across processing cycles (`DynamicWorkflowRegistrationIntegrationTest`).
- Golden fixtures now cover sequential workflow happy path, operation mismatch scenarios (operation key mismatch, pinned document mismatch, operation-vs-handler channel conflict), direct/complex operation requests, handler event filters, derived change workflows, dynamic workflow deferred activation across multi-event runs, derived workflow/operation/operation-marker type-chain fallback, operation-marker type preservation, JS emit callback/deterministic globals/document blueId/canonical reads/document.get aliases/eventCanonical helper behavior, Trigger Event nested-document non-processing + snapshot-expression preservation, Process Embedded protected-path removal fatal termination, Process Embedded multi-path independence/removal/fatal-write scenarios (including child-B fatal write), embedded routing bridge propagation, shared-timeline duplicate checkpoint skipping plus multi-counter processing and replay-after-checkpoint-reset flows, deep embedded document-update propagation for both initialization and external-event triggers, embedded cross-triggering via document update channels, MyOS timeline, composite timeline, and expected initialization failures/cycle validation.
- Trigger Event leakage parity from lifecycle initialization path is now also covered by fixtures for both literal and snapshot-sourced payloads (`trigger-event-init-nested-document-expression-preserved.yaml`, `trigger-event-init-snapshot-expression-preserved.yaml`) with any-triggered-event path assertions.
- Shared-timeline checkpoint integration parity now additionally includes direct Java migration tests for checkpoint clearing/replay behavior at root scope and child-only checkpoint resets on shared timelines (`SharedTimelineCheckpointIntegrationTest`).
- Trigger Event integration parity now includes direct Java migration tests that verify nested payload documents are not processed and nested expressions remain preserved for literal and snapshot-sourced payloads (`TriggerEventIntegrationTest`).
- Trigger Event leakage repro integration parity (`contracts/trigger-event-step-leakage-repro.test.ts`) now has direct Java migration coverage (`TriggerEventStepLeakageReproIntegrationTest`) for both literal nested payload documents and snapshot-sourced payloads during initialization workflows.
- Trigger Event no-document-processing integration parity (`contracts/trigger-event-step-no-document-processing.test.ts`) now has direct Java migration coverage in `TriggerEventStepNoDocumentProcessingIntegrationTest`, validating that nested payload contracts are not recursively executed.
- Embedded routing bridge integration parity (`timeline/embedded-routing-bridge.test.ts`) now has direct Java migration coverage in `EmbeddedRoutingBridgeIntegrationTest`, including embedded timeline routing, root bridge channel handling, and emitted event assertions.
- Deep embedded propagation integration parity (`doc-update/deep-embedded-propagation.test.ts`) now has direct Java migration coverage in `DeepEmbeddedPropagationIntegrationTest`, including initialization lifecycle emission assertions and full root→branch→sub→leaf propagation checks.
- Embedded cross-triggering integration parity (`embedded/cross-triggering.test.ts`) now has direct Java migration coverage in `CrossTriggeringIntegrationTest`, including sequential sub-a/nested-b timeline event runs, nested update propagation, and root/embedded termination-marker non-presence checks.
- Embedded multi-path integration parity (`embedded/process.multi-paths.test.ts`) now has direct Java migration coverage in `ProcessMultiPathsIntegrationTest`, including independent child timeline processing, root fatal termination on protected subtree writes, and safe embedded-child root removal behavior.
- Protected embedded-path removal integration parity (`embedded/process.protected-path-removal-terminates-root.test.ts`) now has direct Java migration coverage in `ProcessProtectedPathRemovalTerminatesRootIntegrationTest`, asserting root fatal termination marker semantics and protected child non-mutation after removal attempt.
- Termination service parity now has direct Java migration coverage in `TerminationServiceParityTest`, including scope marker persistence/gas charging plus both root-fatal and root-graceful run-termination lifecycle emission semantics.
- Scope executor parity now has direct Java migration coverage in `ScopeExecutorDerivedChannelParityTest`, including derived lifecycle-channel handler execution, derived document-update routing through initialization patch cascades, and external-event dispatch skipping for derived processor-managed channels.
- Channel-runner parity now includes inactive-scope handler gating semantics from JS (`allowTerminatedWork`), covered in `ChannelRunnerTest#allowsHandlersToRunWhenScopeInactiveOnlyIfAllowTerminatedWorkIsTrue`.
- Scope executor parity now also includes direct core-flow coverage in `ScopeExecutorParityTest` for initialization marker/lifecycle emission, unmanaged external-event routing, and boundary-violation fatal termination behavior.
- ExpressionPreserver merge parity now includes the non-expression passthrough scenario from JS (`doesNotAlterRegularValuesWithoutExpressions`), confirming regular typed values are left intact while expression preservation remains active.
- QuickJS expression helper parity now aligns standalone/template detection with JS multiline/nested-brace semantics (`isExpression`/`containsExpression`) and traversal semantics by honoring `shouldDescend` on the current pointer with root-pointer `"/"` defaults before evaluating node values (`QuickJsExpressionUtilsTest` regression coverage).
- Deep embedded initialization parity now includes direct Java migration coverage ensuring initialization lifecycle events can drive nested document-update watchers across embedded scope boundaries (`DeepEmbeddedInitializationPropagationTest`).
- Sequential workflow operation parity now includes derived-channel `currentContract.channel` binding coverage when handler channel is inferred from the operation marker (`SequentialWorkflowProcessorTest#sequentialWorkflowOperationExposesDerivedChannelInCurrentContractBindings`).
