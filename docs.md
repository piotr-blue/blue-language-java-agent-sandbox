# Blue Java SDK — Latest DX Guide

This repo now keeps **one latest SDK surface** (no `v1` / `v2` / `vnext` package usage for authoring).

## Where to start

### 1) Generic document builder (non-PayNote)

Use `BlueDoc`:

- `src/test/java/blue/language/samples/paynote/sdk/BlueDoc.java`
- `src/test/java/blue/language/samples/paynote/sdk/BlueDocTest.java`

Example shape:

```java
Node doc = BlueDoc.name("Counter #1")
  .type("MyCompany/Counter")
  .description("Demo counter")
  .participants("owner", "observer")
  .set("/count", 0)
  .operation("increment")
    .channel("owner")
    .requestType(Integer.class)
    .steps(steps -> steps.replaceExpression("Inc", "/count", "document('/count') + event.message.request"))
    .done()
  .buildDocument();
```

Then bind identities at bootstrap time:

```java
Node bootstrap = MyOsDsl.bootstrap(doc)
  .bind("owner").email("alice@gmail.com")
  .bind("observer").accountId("acc_observer_123")
  .build();
```

---

## PayNote SDK (capture-first, rail-agnostic)

Use:

- `src/test/java/blue/language/samples/paynote/sdk/PayNotes.java`
- `src/test/java/blue/language/samples/paynote/sdk/PayNoteBuilder.java`
- `src/test/java/blue/language/samples/paynote/sdk/PayNoteBuilderTest.java`

### Key DX features

- `PayNotes.payNote("...")` is primary.
- Currency is enum: `IsoCurrency`.
- Amount ergonomics:
  - `amountTotalMinor(long)`
  - `amountTotalMajor(BigDecimal/String)` with strict scale validation.
- Payer/Payee/Guarantor are implicit.
- Participants are channel keys:
  - `participant("shipmentCompanyChannel")`
  - `participant("shipmentCompanyChannel", "Shipment company")`
  - `participant("shipmentCompanyChannel", "Shipment company", myOsChannelNode)`
  - `participants("a", "b", "c")`
- Capture helpers use capture terminology only:
  - `capture().lockOnInit()`
  - `capture().unlockWhenEventArrives(...)`
  - `capture().unlockOnOperation(...)`
  - atomic one-liners like `captureLockedUntilOperation(...)`.
- Document authoring is separate from bootstrap bindings.
- Lock plans fail fast if no unlock path is configured.

---

## Complexity ladder

See:

- `src/test/java/blue/language/samples/paynote/examples/PayNoteComplexityLadderExamples.java`
- `src/test/java/blue/language/samples/paynote/examples/PayNoteComplexityLadderExamplesTest.java`

### Step 1 — tiny useful paynotes (5–10 lines)

- `tinyCaptureAfterShipmentOp()`
- `tinyCaptureAfterBuyerApprovalOp()`
- `tinyCaptureAfterTrackingChange()`
- `tinyCaptureAfterEvent()`
- `tinyReserveThenCaptureOnEvent()`
- `tinyRefundOperation()`
- `tinyReleaseOperation()`
- `tinyCancellationOperation()`

### Step 2 — medium paynote with custom operations

- `mediumShipmentEscrow()`

### Step 3 — large deterministic JS step (~100+ lines)

- `hugeJsRiskReview()`

---

## Class extension flow (`MyPayNote`)

See:

- `src/test/java/blue/language/samples/paynote/examples/MyPayNote.java`

It shows:

1. Define a base PayNote with shared fields/ops (`base(...)`).
2. Reuse and extend it (`withExtraOperations(...)`).

---

## Template → specialize → instantiate chain

Shipment chain examples:

- `src/test/java/blue/language/samples/paynote/examples/shipment/ShipmentPayNote.java`
- `src/test/java/blue/language/samples/paynote/examples/shipment/DHLShipmentPayNote.java`
- `src/test/java/blue/language/samples/paynote/examples/shipment/AliceBobShipmentPayNote.java`
- `src/test/java/blue/language/samples/paynote/examples/shipment/ShipmentPayNoteChainTest.java`

This demonstrates:

1. Base template (abstract channels + core behavior),
2. Specialization (EUR 200 from CHF + DHL),
3. Final instance bindings (Alice/Bob/guarantor) plus extension.
