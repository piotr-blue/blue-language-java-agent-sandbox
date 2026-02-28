# SDK DSL Mapping Reference

This document describes how the current SDK DSL maps to the produced `Node` document shape.

Scope:
- `blue.language.sdk.DocBuilder`
- `blue.language.sdk.SimpleDocBuilder`
- `blue.language.sdk.internal.StepsBuilder` (used inside workflow/operation lambdas)
- `blue.language.sdk.MyOsSteps`
- `blue.language.sdk.MyOsPermissions`
- `blue.language.sdk.paynote.PayNoteBuilder`
- `blue.language.sdk.paynote.PayNotes`

## Conventions

- `type(Class<?>)` uses type resolution (`TypeRef`) and writes a typed node.
- `expr("...")` wraps to `${...}` unless already wrapped.
- Most DSL methods are additive and write under `/contracts`, `/policies`, or document paths.
- `buildDocument()` returns the current in-memory node (no extra clone at build time).

## 1) Generic Document DSL (`DocBuilder` / `SimpleDocBuilder`)

### 1.1 Entry points and edit modes

| DSL | Mapping / behavior |
|---|---|
| `DocBuilder.doc()` / `SimpleDocBuilder.doc()` | Creates a new empty document node |
| `DocBuilder.edit(node)` / `SimpleDocBuilder.edit(node)` | Edits the provided node in place (mutating mode) |
| `DocBuilder.from(node)` / `SimpleDocBuilder.from(node)` | Clones `node` first, then edits clone (non-mutating mode) |

### 1.2 Identity and type

| DSL | Mapping |
|---|---|
| `.name("Counter")` | `/name: Counter` |
| `.description("...")` | `/description: ...` |
| `.type("Custom/Type")` | `/type: Custom/Type` |
| `.type(MyType.class)` | `/type` set from class type metadata (`TypeRef`) |

### 1.3 Channels

| DSL | Mapping |
|---|---|
| `.channel("ownerChannel")` | `/contracts/ownerChannel/type: Conversation/Timeline Channel` |
| `.channel("ownerChannel", channelBean)` | `channelBean -> Blue.objectToNode(...)`, stored at `/contracts/ownerChannel`; type normalized to bean alias; empty `event: {}` removed if present |
| `.channels("a","b")` | Equivalent to `.channel("a").channel("b")` |
| `.compositeChannel("union","a","b")` | `/contracts/union/type: Conversation/Composite Timeline Channel`; `/contracts/union/channels: [a,b]` |

### 1.4 Operations

| DSL | Mapping |
|---|---|
| `.operation(key, channel, description, steps)` | `/contracts/{key}` as `Conversation/Operation`; `/contracts/{key}Impl` as `Conversation/Sequential Workflow Operation` |
| `.operation(key, channel, requestType, description, steps)` | Same as above + `/contracts/{key}/request/type` from class |
| `.operation(key)` builder | Fluent equivalent of above |
| `.requestDescription("increment", "...")` | `/contracts/increment/request/description` (creates `request` object if missing) |
| `.operation(...).requestDescription("...").done()` | Applies the same request description mapping on completion |
| `.operation(...).noRequest()` | Forces no request type on operation contract |

`operation(...)` implementation steps always map into:
- `/contracts/{key}Impl/type: Conversation/Sequential Workflow Operation`
- `/contracts/{key}Impl/operation: {key}`
- `/contracts/{key}Impl/steps: [...]`

Note:
- `requestDescription(operationKey, ...)` is ignored if `{operationKey}` does not exist under `/contracts`.

### 1.5 Workflows and triggers

| DSL | Mapping |
|---|---|
| `.onChannelEvent(wf, channel, Event.class, steps)` | Adds `/contracts/{wf}` as `Conversation/Sequential Workflow` on `channel`, event type = `Event.class` |
| `.onEvent(wf, Event.class, steps)` | Ensures `/contracts/triggeredEventChannel` (`Core/Triggered Event Channel`) + workflow on that channel |
| `.onDocChange(wf, "/path", steps)` | Adds `/contracts/{wf}DocUpdateChannel` (`Core/Document Update Channel`, `path`) + `/contracts/{wf}` workflow with event `Core/Document Update` |
| `.onInit(wf, steps)` | Ensures `/contracts/initLifecycleChannel` (`Core/Lifecycle Event Channel`, event `Core/Document Processing Initiated`) + workflow on that channel |

### 1.6 MyOS-oriented document helpers

| DSL | Mapping |
|---|---|
| `.myOsAdmin("myOsAdminChannel")` | Adds timeline channel + operation `{prefix}Update` + impl step `EmitAdminEvents` returning `event.message.request` as emitted events |
| `.onMyOsResponse(wf, Response.class, requestId, steps)` | Wrapper over triggered matcher: matches type `Response.class` + `requestId` and `inResponseTo.requestId` |
| `.onMyOsResponse(wf, Response.class, steps)` | Matches only by event type |
| `.onTriggeredWithId(wf, Event.class, "requestId"/"subscriptionId", id, steps)` | Builds typed matcher for request/subscription correlation |
| `.onTriggeredWithMatcher(wf, Event.class, matcherBean, steps)` | Uses bean-to-node matcher and enforces event type to `Event.class` alias |
| `.onSubscriptionUpdate(wf, subId, UpdateClass, steps)` | Matches `MyOS/Subscription Update` + `subscriptionId`; optionally `update.type` |
| `.onSubscriptionUpdate(wf, subId, steps)` | Same without `update.type` filter |

`onTriggeredWithId(...)` currently supports only:
- `"requestId"`
- `"subscriptionId"`

### 1.7 Policies and direct change

| DSL | Mapping |
|---|---|
| `.directChange(op, channel, description)` | Creates operation + impl with: JS step `CollectChangeset`, then `Update Document` from `steps.CollectChangeset.changeset`; sets `/policies/contractsChangePolicy/mode: direct-change` |

### 1.8 Pointer operations

| DSL | Mapping / behavior |
|---|---|
| `.set("/a/b", value)` | Writes or creates deep path |
| `.replace("/a/b", value)` | Same implementation as `set` |
| `.remove("/a/b")` | Removes property/array item if present |

Pointer rules:
- Root pointer `/` is rejected for `set/replace/remove`.
- Arrays are supported by numeric segments.
- Missing intermediate objects/arrays are created for `set/replace`.
- `remove` is no-op if target path does not exist.

### 1.9 Build and expression helper

| DSL | Mapping |
|---|---|
| `.buildDocument()` | Returns current document node |
| `DocBuilder.expr("document('/x')")` | `${document('/x')}` |

## 2) Step DSL (`StepsBuilder`)

`StepsBuilder` is used inside operation/workflow lambdas.

## 2.1 Core step constructors

| DSL | Mapping |
|---|---|
| `.jsRaw(name, code)` | Step type `Conversation/JavaScript Code`, with `code` |
| `.updateDocument(name, changeset -> ...)` | Step type `Conversation/Update Document`, with explicit changeset array |
| `.updateDocumentFromExpression(name, expr)` | Step type `Conversation/Update Document`, `changeset: ${...}` |
| `.triggerEvent(name, eventNode)` | Step type `Conversation/Trigger Event`, with `event` |
| `.emit(name, typedBean)` | Bean converted with `Blue.objectToNode`, then trigger event |
| `.emitType(name, Event.class, payloadBuilder)` | Typed event by class + payload fields |
| `.emitAdHocEvent(name, eventName, payload)` | Event type `Common/Named Event`, fields `name`, optional `payload` |
| `.namedEvent(...)` | Alias to `emitAdHocEvent(...)` |
| `.replaceValue(name, path, value)` | Convenience wrapper for update-document `replace` |
| `.replaceExpression(name, path, expr)` | Convenience wrapper for update-document `replace` with expression |
| `.raw(stepNode)` | Appends provided step node as-is |

## 2.2 Changeset builder (`ChangesetBuilder`)

Used by `.updateDocument(...)`.

| DSL | Mapping |
|---|---|
| `.replaceValue(path, value)` | `{"op":"replace","path":path,"val":value}` |
| `.replaceExpression(path, expr)` | `{"op":"replace","path":path,"val":"${...}"}` |
| `.addValue(path, value)` | `{"op":"add","path":path,"val":value}` |
| `.remove(path)` | `{"op":"remove","path":path}` |

Guardrail:
- Reserved processor contract paths (checkpoint/embedded/initialized/terminated internals) are rejected.

## 2.3 Typed payload builder (`NodeObjectBuilder`)

Used in `.emitType(...)` and similar places.

| DSL | Mapping |
|---|---|
| `.type("Alias")` / `.type(Class<?>)` | Sets node type |
| `.put(key, value)` | Adds value or node property |
| `.putNode(key, node)` | Adds raw node property |
| `.putExpression(key, expr)` | Adds `${...}` value |

## 2.4 Payment trigger DSL

`triggerPayment(...)` always emits one trigger-event step whose event type is the provided payment class.
`requestBackwardPayment(...)` emits `PayNote/Backward Payment Requested` and keeps the same payload builder style.

Required:
- `payload.processor(...)` must be set; otherwise an exception is thrown.

### 2.4.1 Core payment fields

| DSL | Event payload key |
|---|---|
| `.processor("...")` | `processor` |
| `.payer("ref")` / `.payer(node)` | `payer` |
| `.payee("ref")` / `.payee(node)` | `payee` |
| `.from("ref")` / `.from(node)` | `from` |
| `.to("ref")` / `.to(node)` | `to` |
| `.currency("USD")` | `currency` |
| `.amountMinor(1500)` | `amountMinor` |
| `.amountMinorExpression("...")` | `amountMinor: ${...}` |
| `.reason("...")` | `reason` |
| `.attachPayNote(node)` | `attachedPayNote` |

### 2.4.2 Rail namespaces

| Namespace | Fields mapped |
|---|---|
| `.viaAch()` | `routingNumber`, `accountNumber`, `accountType`, `network`, `companyEntryDescription` |
| `.viaSepa()` | `ibanFrom`, `ibanTo`, `bicTo`, `remittanceInformation` |
| `.viaWire()` | `bankSwift`, `bankName`, `accountNumber`, `beneficiaryName`, `beneficiaryAddress` |
| `.viaCard()` | `cardOnFileRef`, `merchantDescriptor` |
| `.viaTokenizedCard()` | `networkToken`, `tokenProvider`, `cryptogram` |
| `.viaCreditLine()` | `creditLineId`, `merchantAccountId`, `cardholderAccountId` |
| `.viaLedger()` | `ledgerAccountFrom`, `ledgerAccountTo`, `memo` |
| `.viaCrypto()` | `asset`, `chain`, `fromWalletRef`, `toAddress`, `txPolicy` |

Each namespace has `.done()` returning `PaymentRequestPayloadBuilder`.

### 2.4.3 Bean-based rail injection and extension

| DSL | Mapping |
|---|---|
| `.viaAch(new AchPaymentFields(...))` (same for other rails) | Bean properties merged to payload |
| `.rail(bean)` | Generic bean merge (ignores `type`, forbids `processor`) |
| `.putCustom(key, value)` | Adds arbitrary payload field (`processor` key forbidden) |
| `.putCustomExpression(key, expr)` | Adds arbitrary `${...}` payload field |
| `.ext(factory)` | Extension point on payment payload builder |

## 2.5 Capture step namespace

`steps.capture()` returns `CaptureStepBuilder`.

| DSL | Emitted event type |
|---|---|
| `.lock()` | `PayNote/Card Transaction Capture Lock Requested` |
| `.unlock()` | `PayNote/Card Transaction Capture Unlock Requested` |
| `.markLocked()` | `PayNote/Card Transaction Capture Locked` |
| `.markUnlocked()` | `PayNote/Card Transaction Capture Unlocked` |
| `.requestNow()` | `PayNote/Capture Funds Requested` with `amount: ${document('/amount/total')}` |
| `.requestPartial(expr)` | `PayNote/Capture Funds Requested` with `amount: ${expr}` |
| `.releaseFull()` | `PayNote/Reservation Release Requested` with `amount: ${document('/amount/total')}` |

## 2.6 Step-level extensions

| DSL | Mapping |
|---|---|
| `steps.ext(factory)` | Generic extension mechanism (`Function<StepsBuilder,E>`) |
| `steps.myOs()` | Built-in convenience extension returning `MyOsSteps` |

## 2.7 Bootstrap DSL

| DSL | Emits | Key payload |
|---|---|---|
| `steps.bootstrapDocument(name, doc, bindings)` | `Conversation/Document Bootstrap Requested` | `document`, `channelBindings` |
| `steps.bootstrapDocument(name, doc, bindings, options)` | `Conversation/Document Bootstrap Requested` | + `bootstrapAssignee`, `initialMessages` |
| `steps.bootstrapDocumentExpr(name, expr, bindings, options)` | `Conversation/Document Bootstrap Requested` | `document: ${expr}`, `channelBindings`, options |

Bootstrap options:

| Options method | Payload key |
|---|---|
| `.assignee(channelKey)` | `bootstrapAssignee` |
| `.defaultMessage(text)` | `initialMessages/defaultMessage` |
| `.channelMessage(key, text)` | `initialMessages/perChannel[key]` |

Channel bindings mapping:

| Input | Output |
|---|---|
| `Map.of("payerChannel", "aliceChannel")` | `channelBindings: { payerChannel: aliceChannel }` |

## 3) MyOS step extension (`MyOsSteps` + `MyOsPermissions`)

`steps.myOs()` provides typed request emitters.

## 3.1 Permission helper

`MyOsPermissions` fields:
- `read`, `write`, `allOps`, `singleOps`

Builder:
- `MyOsPermissions.create().read(true).singleOps("opA","opB")`

`build()` returns node via `Blue.objectToNode(...)`.

## 3.2 MyOS methods and emitted event classes

| DSL | Emits type | Key payload mapping |
|---|---|---|
| `requestSingleDocPermission(onBehalfOf, requestId, targetSessionId, permissions)` | `SingleDocumentPermissionGrantRequested` | `onBehalfOf`, `requestId`, `targetSessionId`, `permissions` |
| `requestLinkedDocsPermission(onBehalfOf, requestId, targetSessionId, links)` | `LinkedDocumentsPermissionGrantRequested` | `onBehalfOf`, `requestId`, `targetSessionId`, `links` |
| `revokeSingleDocPermission(onBehalfOf, requestId, targetSessionId)` | `SingleDocumentPermissionRevokeRequested` | `onBehalfOf`, `requestId`, `targetSessionId` |
| `revokeLinkedDocsPermission(onBehalfOf, requestId, targetSessionId)` | `LinkedDocumentsPermissionRevokeRequested` | `onBehalfOf`, `requestId`, `targetSessionId` |
| `grantWorkerAgencyPermission(onBehalfOf, requestId, targetSessionId, workerAgencyPermissions)` | `WorkerAgencyPermissionGrantRequested` | `onBehalfOf`, `requestId`, `targetSessionId`, `workerAgencyPermissions` |
| `revokeWorkerAgencyPermission(onBehalfOf, requestId, targetSessionId)` | `WorkerAgencyPermissionRevokeRequested` | `onBehalfOf`, `requestId`, `targetSessionId` |
| `addParticipant(channelKey, email)` | `AddingParticipantRequested` | `channelKey`, `email` |
| `removeParticipant(channelKey)` | `RemovingParticipantRequested` | `channelKey` |
| `callOperation(onBehalfOf, targetSessionId, operation, request)` | `CallOperationRequested` | `onBehalfOf`, `targetSessionId`, `operation`, optional `request` |
| `subscribeToSession(targetSessionId, subscriptionId)` | `SubscribeToSessionRequested` | `targetSessionId`, `subscription: { id, events: [] }` |
| `startWorkerSession(agentChannelKey, config)` | `StartWorkerSessionRequested` | `agentChannelKey`, `config` |
| `bootstrapDocument(name, doc, bindings)` | `DocumentBootstrapRequested` | `document`, `channelBindings`, `bootstrapAssignee` (auto=`myOsAdminChannel`) |
| `bootstrapDocument(name, doc, bindings, options)` | `DocumentBootstrapRequested` | `document`, `channelBindings`, `bootstrapAssignee`, optional `initialMessages` |

Notes:
- Request payload beans/nodes are converted with `Blue.objectToNode(...)`.
- Some text arguments are normalized with strict non-empty checks.
- `targetSessionId` and similar ID fields are written as text values; passing `${...}` expression strings is supported.

## 4) PayNote DSL (`PayNoteBuilder` / `PayNotes`)

## 4.1 Entry and defaults

| DSL | Mapping |
|---|---|
| `PayNotes.payNote("Name")` | New paynote document via `PayNoteBuilder.payNote(...)` |
| Constructor defaults | `/type: PayNote/PayNote` and channels `payerChannel`, `payeeChannel`, `guarantorChannel` |

## 4.2 Money helpers

| DSL | Mapping |
|---|---|
| `.currency("usd")` | `/currency: USD` (ISO code normalized upper-case) |
| `.amountMinor(1234)` | `/amount/total: 1234` |
| `.amountMajor("12.34")` / `.amountMajor(BigDecimal)` | Converted using `java.util.Currency.getDefaultFractionDigits()` |

Validation:
- `amountMinor < 0` is rejected.
- `amountMajor` requires `currency()` first.
- scale mismatches throw via `RoundingMode.UNNECESSARY`.

## 4.3 Action builders: `capture()`, `reserve()`, `release()`

Each returns `ActionBuilder` with identical trigger API:
- `lockOnInit()`
- `unlockOnEvent(...)`
- `unlockOnDocPathChange(...)`
- `unlockOnOperation(...)`
- `requestOnInit()`
- `requestOnEvent(...)`
- `requestOnDocPathChange(...)`
- `requestOnOperation(...)`
- `requestPartialOnOperation(...)`
- `requestPartialOnEvent(...)`
- `done()`

### 4.3.1 Naming pattern for generated workflow keys

For action prefix `capture|reserve|release`:
- `lockOnInit()` -> `{prefix}LockOnInit`
- `unlockOnEvent(FundsCaptured.class)` -> `{prefix}UnlockOnFundsCaptured`
- `unlockOnDocPathChange("/a/b")` -> `{prefix}UnlockOnDocab` (non-alnum stripped)
- `requestOnInit()` -> `{prefix}RequestOnInit`
- `requestOnEvent(E.class)` -> `{prefix}RequestOn{E}`
- `requestOnDocPathChange("/x")` -> `{prefix}RequestOnDocx`
- `requestPartialOnEvent(E.class, ...)` -> `{prefix}PartialOn{E}`

### 4.3.2 Underlying emitted events per action

`capture`:
- lock/unlock use `steps.capture().lock()/unlock()`
- request uses `steps.capture().requestNow()/requestPartial(...)`

`reserve`:
- lock/unlock emit typed aliases:
  - `PayNote/Reserve Lock Requested`
  - `PayNote/Reserve Unlock Requested`
- request emits `PayNote/Reserve Funds Requested` with amount expression

`release` (reservation release semantics):
- lock/unlock emit typed aliases:
  - `PayNote/Reservation Release Lock Requested`
  - `PayNote/Reservation Release Unlock Requested`
- request emits `PayNote/Reservation Release Requested` with amount expression

Important:
- `release()` represents reservation release/cancel semantics, not captured-funds refund semantics.

### 4.3.3 Build-time validation

`buildDocument()` fails if any of `capture|reserve|release` is locked on init but has no unlock path:
- `"capture locked on init but no unlock path configured"`
- `"reserve locked on init but no unlock path configured"`
- `"release locked on init but no unlock path configured"`

## 5) Extension patterns

## 5.1 Custom workflow-step extension

```java
final class DemoSteps {
    private final StepsBuilder parent;
    DemoSteps(StepsBuilder parent) { this.parent = parent; }
    StepsBuilder emitDemo(String signal) { return parent.namedEvent("Demo", signal); }
}

DocBuilder.doc()
    .onInit("init", steps -> steps.ext(DemoSteps::new).emitDemo("READY"))
    .buildDocument();
```

## 5.2 Custom payment payload extension

```java
final class DemoBankFields {
    private final StepsBuilder.PaymentRequestPayloadBuilder payload;
    DemoBankFields(StepsBuilder.PaymentRequestPayloadBuilder payload) { this.payload = payload; }
    DemoBankFields creditFacilityId(String id) { payload.putCustom("creditFacilityId", id); return this; }
}

steps.triggerPayment(PaymentRequested.class, p -> p
    .processor("bankChannel")
    .currency("USD")
    .amountMinor(1000)
    .ext(DemoBankFields::new)
    .creditFacilityId("FAC-1"));
```

## 6) Quick mapping cheatsheet

- `channel(...)` -> `/contracts/{key}` channel contract.
- `operation(...)` -> `/contracts/{key}` + `/contracts/{key}Impl`.
- `onEvent(...)` -> workflow on `triggeredEventChannel`.
- `onDocChange(...)` -> auto `{wf}DocUpdateChannel` + workflow.
- `onInit(...)` -> auto `initLifecycleChannel` + workflow.
- `directChange(...)` -> operation + JS changeset collector + update-document + policy.
- `steps.capture()` -> capture/release event helpers.
- `steps.triggerPayment(...)` -> typed payment event with base + rail fields.
- `steps.myOs()` -> typed MyOS request emitters.
- `PayNotes.payNote(...)` -> paynote defaults + action builders (`capture/reserve/release`).
