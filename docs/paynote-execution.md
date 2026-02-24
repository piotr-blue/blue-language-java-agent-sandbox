# PayNote Execution with Processor Harness

This repository now includes behavioral harness tests that execute DSL-authored PayNotes end-to-end.

## Covered execution scenarios

Implemented in `PayNoteProcessorExecutionTest`:

1. **DocTemplate chain + executable extension**
   - Start from shipment template chain.
   - Extend with a typed operation.
   - Execute via timeline operation call.
   - Assert document state change and init capture-lock emission.

2. **Direct change mapping + execution**
   - Build with `.directChangeWithAllowList(...)`.
   - Assert generated contracts:
     - `Conversation/Change Operation`
     - `Conversation/Change Workflow`
   - Execute direct change request and assert document patch effect.

3. **JS-heavy voucher budget cap**
   - Use cookbook ticket 25.
   - Inject restaurant transaction through operation ingress.
   - Assert capture request amount is capped to remaining budget.

4. **Donation round-up after captured signal**
   - Use cookbook ticket 21.
   - Inject guarantor-like funds captured signal.
   - Assert round-up capture request amount.

## Breadth smoke coverage

`PayNoteCookbookHarnessSmokeTest` initializes all cookbook V2 tickets through harness runtime and verifies base document shape.

## Typical test flow

1. Build document from PayNote DSL (`PayNotes`, `DocTemplates`, cookbook).
2. `ProcessorHarness.start(...)`.
3. `session.initSession()`.
4. Add inbound actions (`callOperation`, timeline entries, or direct events).
5. `session.runUntilIdle()`.
6. Assert:
   - document state,
   - emitted events,
   - timeline store history.
