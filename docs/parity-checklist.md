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
| Registry/timeline/workflow | `src/registry/__tests__/*` | `ContractProcessorRegistryTest`, `ContractProcessorRegistryBuilderDefaultsTest`, `CompositeTimelineChannelProcessorTest`, `CompositeTimelineChannelIntegrationParityTest`, `TimelineChannelProcessorTest`, `TimelineChannelProcessorIntegrationParityTest`, `MyOSTimelineChannelProcessorTest`, `MyOSTimelineChannelIntegrationParityTest`, `SequentialWorkflowProcessorTest`, `SequentialWorkflowHandlerProcessorIntegrationParityTest`, `DeepEmbeddedInitializationPropagationTest`, `DeepEmbeddedPropagationIntegrationTest`, `CrossTriggeringIntegrationTest`, `ProcessMultiPathsIntegrationTest`, `ProcessProtectedPathRemovalTerminatesRootIntegrationTest`, `DynamicWorkflowRegistrationIntegrationTest`, `SharedTimelineCheckpointIntegrationTest`, `TriggerEventIntegrationTest`, `TriggerEventStepLeakageReproIntegrationTest`, `TriggerEventStepNoDocumentProcessingIntegrationTest`, `EmbeddedRoutingBridgeIntegrationTest`, `workflow/WorkflowStepRunnerTest` | IN_PROGRESS |
| Steps + expressions + quickjs | `src/registry/processors/steps/__tests__/*`, `src/util/expression/__tests__/*` | `SequentialWorkflowProcessorTest`, `UpdateDocumentStepExecutorIntegrationParityTest`, `UpdateDocumentStepExecutorDirectParityTest`, `TriggerEventStepExecutorIntegrationParityTest`, `TriggerEventStepExecutorDirectParityTest`, `JavaScriptCodeStepExecutorIntegrationParityTest`, `JavaScriptCodeStepExecutorDirectParityTest`, `QuickJsSidecarRuntimeTest`, `QuickJSEvaluatorTest`, `QuickJSEvaluatorGasTest`, `QuickJsFuelCalibrationTest`, `QuickJsExpressionUtilsTest`, `QuickJsConfigTest`, `CodeBlockEvaluationErrorTest` | IN_PROGRESS |
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
- Contract processor lookup now also supports node-driven `type`-chain resolution for handler/channel/marker processor matches, including provider-backed derived BlueIds and derived timeline channel types (`ContractProcessorRegistryTest#lookupByNodeTypeChainSupportsDerivedBlueIds`, `#lookupByNodeTypeChainSupportsProviderDerivedBlueIds`, `#lookupChannelByNodeSupportsProviderDerivedTimelineTypes`), reducing class-only lookup gaps.
- QuickJS sidecar failures now preserve structured error names/messages (and stack availability marker); evaluator now enforces supported binding-key validation plus host-handler binding shape validation for `document`/`emit`, with dedicated migration coverage (`QuickJSEvaluatorTest` + `QuickJSEvaluatorGasTest`) including out-of-gas timeout classification for tiny wasm budgets. Sidecar now reports deterministic non-zero gas-used estimates with bounded remaining fuel, but exact QuickJS wasm-fuel accounting parity remains partial.
- `QuickJSEvaluator` canonical helper parity now includes deep/shallow `canon.unwrap(...)` behavior for wrapped canonical objects/arrays (`value`/`items`) with direct migration assertions in `QuickJSEvaluatorTest#canonUnwrapSupportsDeepAndShallowModes`.
- JavaScript step parity now also verifies deep/shallow `canon.unwrap(...)` usage through workflow execution bindings (`SequentialWorkflowProcessorTest#javaScriptCodeStepCanonUnwrapSupportsDeepAndShallowModes`).
- JavaScript step parity coverage now also includes:
  - special document segment reads for `name`/`description`/`value`/`blueId` via both plain and canonical document helpers
  - previous-step result access from `steps.<name>.*`
  - deterministic rejection of async/await and runaway-loop fatal termination behavior in workflow processing
  (`SequentialWorkflowProcessorTest`).
- Update/Trigger step parity coverage now also includes:
  - template-path and expression-array `changeset` evaluation in Update Document steps
  - unsupported Update Document operation fatal termination behavior
  - Trigger Event payload emission assertions and missing-payload fatal termination behavior
  (`SequentialWorkflowProcessorTest` additions aligned with JS step-executor test scenarios).
- Update Document step integration parity now has dedicated lifecycle-init migration coverage for:
  - initialization-time document mutation
  - `document(...)` + previous-step result arithmetic
  - expression-generated multi-patch changesets
  - applying changesets produced by JavaScript step outputs
  (`UpdateDocumentStepExecutorIntegrationParityTest`).
- Update Document direct step parity now includes:
  - real context execution for expression-resolved value changes
  - expression-returned `changeset` array execution
  - base gas charging side-effect assertions
  (`UpdateDocumentStepExecutorDirectParityTest`).
- Trigger Event step integration parity now has dedicated lifecycle-init migration coverage for:
  - initialization-time payload emission
  - routed delivery to Triggered Event Channel consumers
  - expression resolution in emitted payloads
  - `currentContract` binding visibility in Trigger Event expressions
  (`TriggerEventStepExecutorIntegrationParityTest`).
- Trigger Event direct step parity now includes:
  - real context emission path with expression-resolved payload values
  - trigger-base gas charging side-effect assertions
  - nested embedded-document expression preservation in emitted payloads
  (`TriggerEventStepExecutorDirectParityTest`).
- JavaScript Code step integration parity now has dedicated lifecycle-init migration coverage for:
  - emitted payload composition across multiple JS steps
  - fatal termination wrapping for thrown JS errors
  - JS-emitted event delivery through Triggered Event Channel consumers
  (`JavaScriptCodeStepExecutorIntegrationParityTest`).
- JavaScript Code step direct parity now includes runtime-context execution coverage for:
  - direct executor invocation with real processor context
  - document/event binding evaluation (`document('/counter') + event.x`)
  - wasm gas charging side effect on processor runtime totals
  (`JavaScriptCodeStepExecutorDirectParityTest`).
- `JavaScriptCodeStepExecutor` now normalizes emitted event `type` payloads into semantic node-type metadata to preserve Triggered Event Channel routing parity for JS object emissions.
- Sequential Workflow handler integration parity now has dedicated migration coverage for:
  - timeline-triggered Trigger Event emissions
  - workflow-level event filters
  - channel-only matching when workflow filter is omitted
  - combined channel+workflow filter enforcement
  (`SequentialWorkflowHandlerProcessorIntegrationParityTest`).
- Composite timeline parity coverage now includes:
  - missing-child failure behavior
  - no-match fast path
  - multi-child declared-order deliveries
  - per-child checkpoint recency decisions
  - nested composite recency behavior
  - workflow JS access to `event.meta.compositeSourceChannelKey`
  (`CompositeTimelineChannelProcessorTest`, `CompositeTimelineChannelIntegrationParityTest`).
- MyOS timeline channel parity coverage now explicitly verifies:
  - matching for both MyOS and conversation timeline-entry event shapes
  - rejection of non-timeline or mismatched timeline-id events
  - recency comparison behavior against prior checkpointed events
  - document-processing integration behavior for MyOS vs conversation timeline entries and mismatch guards
  (`MyOSTimelineChannelProcessorTest`, `MyOSTimelineChannelIntegrationParityTest`).
- Timeline channel parity now includes:
  - non-timeline and mismatched timeline-id rejection
  - handler delivery + checkpointed event metadata assertions
  - duplicate event-id checkpoint gating
  - channel-level event-filter matching semantics
  (`TimelineChannelProcessorTest`, `TimelineChannelProcessorIntegrationParityTest`).
- `QuickJSEvaluator` now mirrors JS binding-default semantics for missing inputs (`event`, `eventCanonical`, `steps`, `currentContract`, `currentContractCanonical`) with direct migration tests for default/null behavior and canonical fallbacks (`QuickJSEvaluatorTest`).
- `QuickJSEvaluator` now supports host `emit` callback parity in direct evaluator usage by forwarding emitted events to a supplied Java callback and returning plain evaluation values (`QuickJSEvaluatorTest#forwardsEmitCallsToHostBindingAndReturnsPlainResult`), while retaining envelope behavior when no callback is supplied (workflow-step path).
- `QuickJSEvaluator` now supports direct function-backed `document` bindings (simple and canonical pointer reads for literal pointer calls) via Java callbacks, with migration coverage in `QuickJSEvaluatorTest` (`supportsFunctionDocumentBindingForPlainAndCanonicalReads`, `supportsSimpleFunctionDocumentBindingForLiteralPointers`).
- `QuickJSEvaluator` direct parity coverage now also includes:
  - explicit `currentContract` / `currentContractCanonical` binding visibility assertions
  - deterministic masking of `Date` and `process` globals in evaluator-only execution paths
  (`QuickJSEvaluatorTest`).
- QuickJS fuel calibration migration now has dedicated deterministic baseline coverage (`QuickJsFuelCalibrationTest`) for representative script complexity trends and repeated-run stability.
- Contract-model schema parity now includes MyOS marker contract mappings for `MyOS/Document Anchors`, `MyOS/Document Links`, `MyOS/MyOS Participants Orchestration`, `MyOS/MyOS Session Interaction`, and `MyOS/MyOS Worker Agency` (`ContractModelsParityTest`, `TypeClassResolverAliasTest`).
- Contract-loader parity now includes unsupported-contract MustUnderstand behavior, built-in contract loading coverage, and provider-derived handler loading via type-chain lookup (`ContractLoaderParityTest`) in addition to composite-cycle and MyOS-marker loader tests.
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
- `evaluateQuickJsExpression(...)` now wraps expressions as `return (<expr>);` before runtime evaluation (JS parity), so object-literal expressions evaluate as objects rather than statement blocks (`QuickJsExpressionUtilsTest` coverage).
- Deep embedded initialization parity now includes direct Java migration coverage ensuring initialization lifecycle events can drive nested document-update watchers across embedded scope boundaries (`DeepEmbeddedInitializationPropagationTest`).
- Sequential workflow operation parity now includes derived-channel `currentContract.channel` binding coverage when handler channel is inferred from the operation marker (`SequentialWorkflowProcessorTest#sequentialWorkflowOperationExposesDerivedChannelInCurrentContractBindings`).
