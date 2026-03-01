# SDK DSL Mapping Reference

This is the canonical reference for the current runtime DSL under:
- `blue.language.sdk`
- `blue.language.sdk.paynote`
- `blue.language.sdk.internal` (used indirectly through DSL methods)

It explains:
1. What each DSL method does.
2. Where it writes in the `Node` document.
3. How generated contract/workflow keys are named.
4. How to compose document, MyOS, AI, payment, and PayNote flows.

If you are new to this DSL, read sections in order.

---

## 1) Mental Model

A built document is a `Node` tree with three main areas:
- Document fields: root properties (for example `/counter`, `/status`, `/amount/total`)
- Contracts: `/contracts/*`
- Policies: `/policies/*`

The DSL is additive: you call fluent methods that write to these areas.

Core ideas:
- `field(...)` writes document data.
- `channel(...)`, `operation(...)`, `onEvent(...)`, etc. write contracts.
- Step lambdas use `StepsBuilder` to produce workflow steps.

---

## 2) Quick Start

### 2.1 Smallest useful doc

```java
Node doc = DocBuilder.doc()
    .name("Counter")
    .field("/counter", 0)
    .channel("ownerChannel")
    .operation("increment")
        .channel("ownerChannel")
        .requestType(Integer.class)
        .description("Increment counter")
        .steps(steps -> steps.replaceExpression(
            "Inc",
            "/counter",
            "document('/counter') + event.message.request"))
        .done()
    .buildDocument();
```

Equivalent shape:

```yaml
name: Counter
counter: 0
contracts:
  ownerChannel:
    type: Conversation/Timeline Channel
  increment:
    type: Conversation/Operation
    channel: ownerChannel
    description: Increment counter
    request:
      type: Integer
  incrementImpl:
    type: Conversation/Sequential Workflow Operation
    operation: increment
    steps:
      - name: Inc
        type: Conversation/Update Document
        changeset:
          - op: replace
            path: /counter
            val: ${document('/counter') + event.message.request}
```

---

## 3) API Surface Map

| Area | Main types |
|---|---|
| Generic document DSL | `DocBuilder`, `SimpleDocBuilder` |
| Step DSL (inside workflows/operations) | `StepsBuilder`, `ChangesetBuilder`, `NodeObjectBuilder` |
| MyOS extension | `MyOsSteps`, `MyOsPermissions` |
| PayNote DSL | `PayNoteBuilder`, `PayNotes` |

---

## 4) DocBuilder / SimpleDocBuilder

## 4.1 Entry points and edit mode

| DSL | Behavior |
|---|---|
| `DocBuilder.doc()` / `SimpleDocBuilder.doc()` | New empty document |
| `DocBuilder.edit(node)` / `SimpleDocBuilder.edit(node)` | Edit provided node in place (mutating) |
| `DocBuilder.from(node)` / `SimpleDocBuilder.from(node)` | Clone first, then edit clone |

Use:
- `edit(...)` when you intentionally mutate a template node.
- `from(...)` when you want template immutability.

## 4.2 Identity and type

| DSL | Mapping |
|---|---|
| `.name("X")` | `/name: X` |
| `.description("...")` | `/description: ...` |
| `.type("Alias")` | `/type: Alias` |
| `.type(SomeClass.class)` | `/type` from `TypeRef.of(SomeClass)` |

## 4.3 Public field API (no public `set`)

Public field writing is `field(...)`.

### Quick form

```java
.field("/counter", 0)
.field("/profile", profileBean)
.field("/meta", metaNode)
```

Behavior:
- Primitive/string/boolean/number -> `value`
- `Node` -> inserted as node
- Bean/map/list/array -> `Blue.objectToNode(...)`
- `java.time.*` values -> stored as `toString()` text

### Supported `field(...)` forms

```java
// 1) Value only
.field("/x", 1)

// 2) Schema/metadata only
.field("/x").type(Integer.class).description("Some desc").done()

// 3) Value + metadata
.field("/x").value(1).type(Integer.class).description("Some desc").done()

// 4) Whole node value
.field("/x", someNode)

// 5) Bean value
.field("/x", someBean) // converted through Blue.objectToNode(...)
```

Note:
- `.field("/x").done()` with no other builder calls does not create `/x` (no-op write), but still tracks `/x` if called inside an open section.

### Builder form

```java
.field("/x")
    .type(Integer.class)
    .description("Score")
    .required(true)
    .minimum(0)
    .maximum(100)
    .value(42)
    .done()
```

Supported builder methods:
- `.type(Class<?>)`
- `.type(String)`
- `.type(Node)`
- `.description(String)`
- `.value(Object)`
- `.required(boolean)`
- `.minimum(Number)`
- `.maximum(Number)`
- `.done()`

Constraints map to node constraints (`/field/constraints/*`).

## 4.4 Sections (`Conversation/Document Section`)

Sections group related fields and contracts for readability/LLM context.

```java
.section("counterOps", "Counter operations", "Increment/decrement flow")
    .field("/counter", 0)
    .operation("increment") ...
.endSection()
```

Generated contract:

```yaml
counterOps:
  type: Conversation/Document Section
  title: Counter operations
  summary: Increment/decrement flow
  relatedFields:
    - /counter
  relatedContracts:
    - increment
    - incrementImpl
```

Rules:
- One active section at a time.
- Nested sections are rejected.
- `buildDocument()` fails if section not closed.
- Outside a section, tracking is no-op.

### What gets auto-tracked in a section

| Method | Tracked keys |
|---|---|
| `channel("x")` | `x` |
| `channels("a","b")` | `a`, `b` |
| `compositeChannel("u",...)` | `u` |
| `operation("k",...)` | `k` and `kImpl` when impl exists |
| `operation("k").steps(...).done()` | `k`, `kImpl` |
| `onInit("wf",...)` | `wf` |
| `onEvent("wf",...)` | `wf` |
| `onChannelEvent("wf",...)` | `wf` |
| `onDocChange("wf",...)` | `wfDocUpdateChannel`, `wf` |
| `onMyOsResponse(...)` / `onTriggeredWith*` / `onSubscriptionUpdate(...)` | workflow key |
| `myOsAdmin(...)` | admin channel + generated emit operation/impl |
| `ai("name").done()` | generated AI workflow keys |
| `field("/path", ...)` or `field("/path")...done()` | `/path` |

## 4.5 Channels

| DSL | Mapping |
|---|---|
| `.channel("ownerChannel")` | `/contracts/ownerChannel/type: Conversation/Timeline Channel` |
| `.channel("key", channelContractObject)` | object converted with `Blue.objectToNode(...)`, then type normalized from object class alias |
| `.channels("a","b")` | repeated `.channel(...)` |
| `.compositeChannel("union", "a", "b")` | `Conversation/Composite Timeline Channel` + `/channels: [a,b]` |

Recommended:
- Pass a typed channel bean (for example `MyOsTimelineChannel`).
- Raw `Node` works syntactically but is not recommended here because type is still normalized from runtime class.

## 4.6 Operations

### Inline forms

| DSL | Mapping |
|---|---|
| `.operation(key, channel, description)` | only operation contract |
| `.operation(key, channel, reqType, description)` | operation contract + request type |
| `.operation(key, channel, description, steps)` | operation contract + `keyImpl` sequential workflow operation |
| `.operation(key, channel, reqType, description, steps)` | same + request type |

### Builder form

```java
.operation("confirmShipment")
    .channel("shipmentCompanyChannel")
    .description("Confirm shipment")
    .requestType(Integer.class)
    .requestDescription("Value to confirm")
    .steps(steps -> ...)
    .done()
```

Builder methods:
- `.channel(...)`
- `.description(...)`
- `.requestType(Class<?>)`
- `.request(Object)` (custom request schema object/node)
- `.requestDescription(...)`
- `.noRequest()`
- `.steps(...)`
- `.done()`

Implementation mapping (when steps exist):
- `/contracts/{key}Impl/type: Conversation/Sequential Workflow Operation`
- `/contracts/{key}Impl/operation: {key}`
- `/contracts/{key}Impl/steps: [...]`

## 4.7 Workflow contracts

| DSL | Mapping |
|---|---|
| `.onChannelEvent(wf, channel, Event.class, steps)` | `Conversation/Sequential Workflow` on explicit channel |
| `.onEvent(wf, Event.class, steps)` | ensures `triggeredEventChannel`; workflow on it |
| `.onDocChange(wf, "/path", steps)` | adds `{wf}DocUpdateChannel` + workflow keyed `{wf}` |
| `.onInit(wf, steps)` | ensures `initLifecycleChannel`; workflow on it |

Auto channels:
- `triggeredEventChannel`: `Triggered Event Channel`
- `initLifecycleChannel`: `Lifecycle Event Channel` with `Document Processing Initiated`
- `{wf}DocUpdateChannel`: `Document Update Channel` with `path`

## 4.8 Triggered-event matchers and MyOS response helpers

| DSL | Mapping |
|---|---|
| `.onMyOsResponse(wf, Resp.class, requestId, steps)` | typed triggered matcher + requestId correlation |
| `.onMyOsResponse(wf, Resp.class, steps)` | typed triggered matcher only |
| `.onTriggeredWithId(wf, Event.class, "requestId", id, steps)` | matcher with `requestId` + `inResponseTo.requestId` |
| `.onTriggeredWithId(wf, Event.class, "subscriptionId", id, steps)` | matcher with `subscriptionId` |
| `.onTriggeredWithMatcher(wf, Event.class, bean, steps)` | bean converted to matcher; type forced to `Event.class` |
| `.onSubscriptionUpdate(wf, subId, updateType, steps)` | `MyOS/Subscription Update` matcher + optional `update.type` |
| `.onSubscriptionUpdate(wf, subId, steps)` | same without update type |

`onTriggeredWithId` supports only `requestId` and `subscriptionId` field names.

## 4.9 MyOS admin and emission convenience

| DSL | Mapping |
|---|---|
| `.myOsAdmin()` | creates `myOsAdminChannel` (`MyOS/MyOS Timeline`) + `myOsEmit` operation + `myOsEmitImpl` |
| `.myOsAdmin("adminChannel")` | same behavior for provided key (`adminEmit`, etc.) |
| `.myOs()` | alias to `.myOsAdmin()` |
| `.canEmit("aliceChannel")` | creates `aliceEmit` + `aliceEmitImpl`, request type `List` |
| `.canEmit("bobChannel", Ev1.class, Ev2.class)` | same + request items typed from classes |
| `.canEmit("celineChannel", shape1, shape2)` | same + request items from provided object/node shapes |

Emit implementation step code:

```js
return { events: event };
```

Operation key derivation:
- If channel ends with `Channel`: `myOsAdminChannel -> myOsAdminEmit`
- Otherwise: `<channel>Emit`

## 4.10 Direct change policy helper

```java
.directChange("applyPatch", "ownerChannel", "Apply incoming changeset")
```

Adds:
- operation `applyPatch`
- `applyPatchImpl` with:
  1. JS step `CollectChangeset`
  2. Update-document step from `steps.CollectChangeset.changeset`
- policy:

```yaml
policies:
  contractsChangePolicy:
    mode: direct-change
    reason: operation applies request changeset
```

## 4.11 Pointer editing

| DSL | Behavior |
|---|---|
| `.field("/a/b", value)` | set/create deep path |
| `.replace("/a/b", value)` | same write semantics |
| `.remove("/a/b")` | remove property/array element if exists |

Rules:
- Root `/` is rejected for replace/remove/write internals.
- Missing containers are created for writes.
- Array segments must be numeric when traversing arrays.

## 4.12 Build and expression helper

| DSL | Behavior |
|---|---|
| `.buildDocument()` | returns current in-memory node |
| `DocBuilder.expr("...")` | wraps into `${...}` unless already wrapped |

---

## 5) AI Integration DSL

AI integration is a first-class `DocBuilder` primitive.

## 5.1 Define an AI integration

```java
.ai("provider")
    .sessionId(DocBuilder.expr("document('/llmProviderSessionId')"))
    .permissionFrom("ownerChannel")
    .statusPath("/provider/status")
    .contextPath("/provider/context")
    .requesterId("MEAL_PLANNER")
    .done()
```

Default builder values before override:
- `statusPath = /ai/<name>/status`
- `contextPath = /ai/<name>/context`
- `requesterId = <ALNUM-UPPER token from name>`

Auto-generated assets on `.done()`:
- `/statusPath = "pending"`
- `/contextPath = {}`
- `ai<TOKEN>RequestPermission` (`onInit`) -> `MyOS/Single Document Permission Grant Requested`
- `ai<TOKEN>Subscribe` (`onMyOsResponse` for granted)
- `ai<TOKEN>SubscriptionReady` (`onSubscriptionUpdate`) -> status `ready`
- `ai<TOKEN>PermissionRejected` (`onMyOsResponse`) -> status `revoked`

`TOKEN` = uppercase alphanumeric characters from integration name.

## 5.2 Ask AI from steps

```java
steps.askAI("provider", "Generate", prompt -> prompt
    .text(DocBuilder.expr("document('/prompt')"))
    .text("Max: ${document('/maxCalories')}")
    .text("Request: ${event.message.request}"));
```

Emits `MyOS/Call Operation Requested` with:
- `onBehalfOf` from `permissionFrom`
- `targetSessionId` from integration config
- `operation = provideInstructions`
- `request.requester` from integration config
- `request.context = ${document('<contextPath>')}`
- `request.instructions` as concatenated expression

Prompt behavior:
- Each `.text(...)` or `.expression(...)` appends a new line between segments.
- `${...}` interpolations in `.text(...)` are preserved as expressions.

## 5.3 Handle AI responses

```java
.onAIResponse("provider", "onPlan", steps -> ...)
.onAIResponse("provider", "onPlan", ResponseSubclass.class, steps -> ...)
```

Matcher behavior:
- Triggered event type: `MyOS/Subscription Update`
- `subscriptionId` bound to integration subscription id
- `update.type` defaults to `Conversation/Response` (or explicit class)
- `update.inResponseTo.incomingEvent.requester` must match integration requester id

Auto-inserted first step:
- `replaceExpression("_SaveAIContext", <contextPath>, "event.update.context")`

Then your custom steps run.

---

## 6) StepsBuilder (inside workflow/operation lambdas)

`StepsBuilder` is used in:
- `operation(..., steps -> ...)`
- `onInit(..., steps -> ...)`
- `onEvent(..., steps -> ...)`
- etc.

## 6.1 Core step constructors

| DSL | Step mapping |
|---|---|
| `.jsRaw(name, code)` | `Conversation/JavaScript Code` |
| `.updateDocument(name, changeset -> ...)` | `Conversation/Update Document` + array changeset |
| `.updateDocumentFromExpression(name, expr)` | `Conversation/Update Document` + expression changeset |
| `.triggerEvent(name, eventNode)` | `Conversation/Trigger Event` |
| `.emit(name, bean)` | bean -> event node |
| `.emitType(name, Event.class, payload)` | typed event with payload |
| `.emitAdHocEvent(name, eventName, payload)` | `Common/Named Event` |
| `.namedEvent(...)` | alias of `emitAdHocEvent` |
| `.replaceValue(...)` | convenience update-document replace |
| `.replaceExpression(...)` | convenience update-document replace expr |
| `.raw(stepNode)` | appends exact node |

## 6.2 ChangesetBuilder

Used by `.updateDocument(...)`.

| Method | Entry |
|---|---|
| `replaceValue(path, value)` | `{ op: replace, path, val }` |
| `replaceExpression(path, expr)` | `{ op: replace, path, val: ${...} }` |
| `addValue(path, value)` | `{ op: add, path, val }` |
| `remove(path)` | `{ op: remove, path }` |

Guardrail:
- Mutations under reserved processor-managed paths are rejected.

## 6.3 NodeObjectBuilder payload helper

Used by `emitType(...)` and other payload customizers.

| Method | Behavior |
|---|---|
| `type("Alias")` / `type(Class<?>)` | set node type |
| `put(key, value)` | set property (`Node` preserved if provided) |
| `putNode(key, node)` | set node property |
| `putExpression(key, expr)` | set `${...}` |
| `putStringMap(key, map)` | dictionary object from `Map<String,String>` |

## 6.4 Capture step namespace

`steps.capture()` methods:

| Method | Emits event type |
|---|---|
| `lock()` | `PayNote/Card Transaction Capture Lock Requested` |
| `unlock()` | `PayNote/Card Transaction Capture Unlock Requested` |
| `markLocked()` | `PayNote/Card Transaction Capture Locked` |
| `markUnlocked()` | `PayNote/Card Transaction Capture Unlocked` |
| `requestNow()` | `PayNote/Capture Funds Requested` (`amount = ${document('/amount/total')}`) |
| `requestPartial(expr)` | `PayNote/Capture Funds Requested` (`amount = ${expr}`) |
| `releaseFull()` | `PayNote/Reservation Release Requested` (`amount = ${document('/amount/total')}`) |

## 6.5 Bootstrap steps

| DSL | Emits |
|---|---|
| `bootstrapDocument(name, doc, bindings)` | `Conversation/Document Bootstrap Requested` |
| `bootstrapDocument(name, doc, bindings, options)` | same + options |
| `bootstrapDocumentExpr(name, expr, bindings, options)` | same with expression document |

Options (`BootstrapOptionsBuilder`):
- `.assignee(channelKey)` -> `bootstrapAssignee`
- `.defaultMessage(text)` -> `initialMessages.defaultMessage`
- `.channelMessage(key, text)` -> `initialMessages.perChannel[key]`

## 6.6 Extensions

| DSL | Behavior |
|---|---|
| `steps.ext(factory)` | generic extension hook (`Function<StepsBuilder,E>`) |
| `steps.myOs()` / `steps.myOs(adminChannel)` | returns `MyOsSteps` extension |

---

## 7) Payment Request DSL

Payment requests are emitted from `StepsBuilder`.

## 7.1 Entry methods

| DSL | Event type |
|---|---|
| `triggerPayment(name, PaymentType.class, payload -> ...)` | provided payment class |
| `requestBackwardPayment(name, payload -> ...)` | `PayNote/Backward Payment Requested` |

Required field:
- `processor(...)` must be set, otherwise build fails.

## 7.2 Core payload fields

| Payload DSL | Event field |
|---|---|
| `processor("...")` | `processor` |
| `payer("ref")` / `payer(node)` | `payer` |
| `payee("ref")` / `payee(node)` | `payee` |
| `from("ref")` / `from(node)` | `from` |
| `to("ref")` / `to(node)` | `to` |
| `currency("USD")` | `currency` |
| `amountMinor(1234)` | `amountMinor` |
| `amountMinorExpression("...")` | `amountMinor: ${...}` |
| `reason("...")` | `reason` |
| `attachPayNote(node)` | `attachedPayNote` |

## 7.3 Rail namespaces (`via*`)

| Namespace | Fields |
|---|---|
| `viaAch()` | routingNumber, accountNumber, accountType, network, companyEntryDescription |
| `viaSepa()` | ibanFrom, ibanTo, bicTo, remittanceInformation |
| `viaWire()` | bankSwift, bankName, accountNumber, beneficiaryName, beneficiaryAddress |
| `viaCard()` | cardOnFileRef, merchantDescriptor |
| `viaTokenizedCard()` | networkToken, tokenProvider, cryptogram |
| `viaCreditLine()` | creditLineId, merchantAccountId, cardholderAccountId |
| `viaLedger()` | ledgerAccountFrom, ledgerAccountTo, memo |
| `viaCrypto()` | asset, chain, fromWalletRef, toAddress, txPolicy |

Each namespace returns `.done()` back to the main payload builder.

Bean shortcuts are supported for each rail, for example:
- `.viaAch(AchPaymentFields bean)`
- `.viaCreditLine(CreditLinePaymentFields bean)`

Deprecated aliases still exist (`ach()`, `sepa()`, etc.) but `via*` is the preferred API.

## 7.4 Generic custom/extension fields

| DSL | Behavior |
|---|---|
| `rail(bean)` | merges bean fields into payload (skips `type`) |
| `putCustom(key, value)` | custom key-value |
| `putCustomExpression(key, expr)` | custom expression |
| `ext(factory)` | extension point over payload builder |

Guardrail:
- `processor` cannot be set through `rail(...)` / `putCustom(...)`; must use `processor(...)`.

---

## 8) MyOS Extension (`MyOsSteps`, `MyOsPermissions`)

Use inside steps:

```java
steps.myOs().requestSingleDocPermission(...)
```

## 8.1 `MyOsPermissions`

Builder methods:
- `create()`
- `read(boolean)`
- `write(boolean)`
- `allOps(boolean)`
- `singleOps(String...)`
- `build()` -> `Node`

Example:

```java
MyOsPermissions.create().read(true).singleOps("provideInstructions")
```

## 8.2 MyOS methods and emitted event types

| DSL | Emits type |
|---|---|
| `requestSingleDocPermission(...)` | `MyOS/Single Document Permission Grant Requested` |
| `requestLinkedDocsPermission(...)` | `MyOS/Linked Documents Permission Grant Requested` |
| `revokeSingleDocPermission(...)` | `MyOS/Single Document Permission Revoke Requested` |
| `revokeLinkedDocsPermission(...)` | `MyOS/Linked Documents Permission Revoke Requested` |
| `grantWorkerAgencyPermission(...)` | `MyOS/Worker Agency Permission Grant Requested` |
| `revokeWorkerAgencyPermission(...)` | `MyOS/Worker Agency Permission Revoke Requested` |
| `addParticipant(...)` | `MyOS/Adding Participant Requested` |
| `removeParticipant(...)` | `MyOS/Removing Participant Requested` |
| `callOperation(...)` | `MyOS/Call Operation Requested` |
| `subscribeToSession(...)` | `MyOS/Subscribe to Session Requested` |
| `startWorkerSession(...)` | `MyOS/Start Worker Session Requested` |
| `bootstrapDocument(...)` | `Conversation/Document Bootstrap Requested` |

Notes:
- Input validation enforces required non-blank strings.
- IDs/session refs are handled as text values (expression strings like `${...}` are allowed).
- `bootstrapDocument(...)` in `MyOsSteps` auto-sets assignee to its admin channel unless overridden in options.
- `callOperation(..., request = null)` currently serializes with an empty `request` container shape in emitted event payload (current behavior, covered by tests).

---

## 9) PayNote DSL (`PayNoteBuilder`, `PayNotes`)

## 9.1 Entry and defaults

| DSL | Mapping |
|---|---|
| `PayNotes.payNote("Name")` | creates paynote document |
| Defaults | `/type: PayNote/PayNote` and channels `payerChannel`, `payeeChannel`, `guarantorChannel` |

## 9.2 Money helpers

| DSL | Mapping |
|---|---|
| `.currency("usd")` | `/currency: USD` |
| `.amountMinor(1234)` | `/amount/total: 1234` |
| `.amountMajor("12.34")` / `.amountMajor(BigDecimal)` | major -> minor using ISO currency fraction digits |

Validation:
- negative `amountMinor` rejected
- `amountMajor` requires `currency()` first
- invalid scale rejected (`RoundingMode.UNNECESSARY`)

## 9.3 Action builders

`PayNoteBuilder` exposes three action namespaces:
- `capture()`
- `reserve()`
- `release()`

Each returns `ActionBuilder` with the same trigger API:
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

Event semantics:
- `capture` uses capture lock/unlock/request events
- `reserve` uses reserve lock/unlock and reserve funds requested
- `release` uses reservation-release lock/unlock and reservation release requested

Important:
- `release()` means reservation release/cancel flow, not captured-funds refund flow.

Build-time guardrail:
- For each action, if locked on init and no unlock path configured, `buildDocument()` throws.

---

## 10) End-to-End Example: Sectioned Counter

```java
Node counter = DocBuilder.doc()
    .name("Counter")
    .section("participants", "Participants", "Alice timeline")
        .channel("aliceTimeline")
    .endSection()
    .section("counterOps", "Counter operations", "Increment/decrement")
        .field("/counter", 0)
        .operation("increment")
            .channel("aliceTimeline")
            .requestType(Integer.class)
            .description("Increment")
            .steps(steps -> steps.replaceExpression("Inc", "/counter", "event.message.request + document('/counter')"))
            .done()
        .operation("decrement")
            .channel("aliceTimeline")
            .requestType(Integer.class)
            .description("Decrement")
            .steps(steps -> steps.replaceExpression("Dec", "/counter", "document('/counter') - event.message.request"))
            .done()
    .endSection()
    .buildDocument();
```

---

## 11) End-to-End Example: AI + MyOS + Response Handling

```java
Node agent = DocBuilder.doc()
    .name("Meal Planner")
    .type("MyOS/Agent")
    .section("participants", "Participants", "Owner and admin")
        .channel("ownerChannel")
        .myOsAdmin("myOsAdminChannel")
    .endSection()
    .section("provider", "Provider integration", "LLM setup")
        .field("/llmProviderSessionId", "session-001")
        .ai("provider")
            .sessionId(DocBuilder.expr("document('/llmProviderSessionId')"))
            .permissionFrom("ownerChannel")
            .statusPath("/provider/status")
            .contextPath("/provider/context")
            .requesterId("MEAL_PLANNER")
            .done()
    .endSection()
    .section("flow", "Meal planning", "Request + process")
        .field("/maxCalories", 3000)
        .operation("requestMealPlan")
            .channel("ownerChannel")
            .requestType(String.class)
            .steps(steps -> steps.askAI("provider", "Generate", prompt -> prompt
                .text("Max: ${document('/maxCalories')}")
                .text("Request: ${event.message.request}")))
            .done()
        .onAIResponse("provider", "onPlan", steps -> steps
            .replaceValue("MarkDone", "/status", "done"))
    .endSection()
    .buildDocument();
```

---

## 12) End-to-End Example: PayNote Armchair + Voucher + Reverse Payment

This pattern captures funds for the primary purchase, then requests a backward payment (payee -> payer) for a voucher after capture.

```java
Node armchairWithVoucher = PayNotes.payNote("Armchair Protection + Voucher")
    .description("Capture unlocks after buyer satisfaction, then voucher credit is requested.")
    .currency("USD")
    .amountMinor(10000)
    .capture()
        .lockOnInit()
        .unlockOnOperation(
            "confirmSatisfaction",
            "payerChannel",
            "Buyer confirms satisfaction.")
        .done()
    .onEvent("requestVoucherPayment", FundsCaptured.class, steps -> steps.requestBackwardPayment(
        "VoucherCredit",
        payload -> payload
            .processor("guarantorChannel")
            .from("payeeChannel")
            .to("payerChannel")
            .currency("USD")
            .amountMinor(10000)
            .reason("voucher-activation")
            .attachPayNote(
                PayNotes.payNote("Balanced Bowl Voucher")
                    .description("Child voucher paynote")
                    .currency("USD")
                    .amountMinor(10000)
                    .release()
                        .requestOnInit()
                        .done()
                    .buildDocument())))
    .buildDocument();
```

This is the default and preferred shape: abstract intent only.  
Processor decides execution rail (credit line, ACH credit, card refund, ledger entry, etc.).

If needed, you can still provide an optional rail hint:

```java
.requestBackwardPayment("VoucherCredit", payload -> payload
    .processor("guarantorChannel")
    .from("payeeChannel")
    .to("payerChannel")
    .currency("USD")
    .amountMinor(10000)
    .reason("voucher-activation")
    .viaCreditLine()
        .creditLineId("facility-001")
        .done())
```

---

## 13) End-to-End Example: Bootstrap a New Inline Document on Event

This shows inline child-document definition directly in `bootstrapDocument(...)`.
When funds are captured, the parent document bootstraps a new voucher document session.

```java
Node orchestrator = DocBuilder.doc()
    .name("Order Orchestrator")
    .channel("payerChannel")
    .channel("payeeChannel")
    .myOsAdmin("myOsAdminChannel")
    .onEvent("onFundsCaptured", FundsCaptured.class, steps -> steps
        .myOs().bootstrapDocument(
            "BootstrapVoucherDocument",
            DocBuilder.doc()
                .name("Balanced Bowl Voucher")
                .description("Auto-created voucher document.")
                .channel("voucherOwnerChannel")
                .channel("voucherMerchantChannel")
                .field("/voucher/status", "active")
                .field("/voucher/amountMinor", 10000)
                .operation("redeem")
                    .channel("voucherOwnerChannel")
                    .description("Redeem voucher.")
                    .steps(s -> s.replaceValue("MarkRedeemed", "/voucher/status", "redeemed"))
                    .done()
                .buildDocument(),
            Map.of(
                "voucherOwnerChannel", "payerChannel",
                "voucherMerchantChannel", "payeeChannel"),
            options -> options
                .defaultMessage("A voucher has been issued for your order.")
                .channelMessage("voucherOwnerChannel", "You received a new voucher.")))
    .onMyOsResponse("onVoucherBootstrapped",
        DocumentBootstrapCompleted.class,
        steps -> steps
            .replaceExpression("SaveVoucherSessionId", "/voucher/sessionId", "event.message.sessionId")
            .replaceValue("MarkVoucherLinked", "/voucher/linkStatus", "linked"))
    .buildDocument();
```

---

## 14) Common Pitfalls

1. Forgetting `endSection()`:
- `buildDocument()` throws when a section remains open.

2. Using internal setter:
- Public API for document data is `field(...)`.
- `set(...)` is protected for internal SDK composition.

3. Expecting `operation(..., description)` to auto-create impl:
- Impl contract appears only when steps are provided.

4. Misunderstanding `release()` in PayNote:
- It is reservation release semantics, not refund of captured funds.

5. Missing payment processor:
- `triggerPayment` and `requestBackwardPayment` require `processor(...)`.

6. Unknown AI integration in `askAI`/`onAIResponse`:
- Throws `Unknown AI integration: <name>` unless `.ai(name)...done()` was called first.

---

## 15) Migration Notes (current state)

- Prefer `field(...)` over older `set(...)` usage in user code.
- Prefer `viaAch()/viaSepa()/...` over deprecated payment aliases `ach()/sepa()/...`.
- Prefer sectioned authoring for large documents to produce `Conversation/Document Section` metadata.

---

## 16) Minimal Cheatsheet

- Data: `field("/x", value)`
- Grouping: `section(...).endSection()`
- Participants/channels: `channel(...)`, `compositeChannel(...)`
- Operations: `operation(...)` or `operation("k"). ... .done()`
- Workflows: `onInit`, `onEvent`, `onChannelEvent`, `onDocChange`
- Matchers: `onMyOsResponse`, `onSubscriptionUpdate`, `onTriggeredWithId/Matcher`
- AI: `ai(...).done()`, `steps.askAI(...)`, `onAIResponse(...)`
- MyOS requests: `steps.myOs().<method>(...)`
- Payments: `steps.triggerPayment(...)`, `steps.requestBackwardPayment(...)`
- PayNote: `PayNotes.payNote(...).capture()/reserve()/release()`
