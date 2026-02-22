# PayNote SDK vNext (Sample DSL)

This repository includes a vNext sample SDK that layers ergonomic PayNote authoring on top of generic document authoring primitives.

## Generic document authoring

Use `MyOsDsl.bootstrap()` + `DocumentBuilder`:

- set scalar or object properties
- define abstract channels/contracts/workflows
- add policy slots (allow-lists, limits)
- bind concrete identities in `channelBindings`

## Channel hierarchy and binding

- Abstract channels are declared in contracts (typically timeline channels).
- Concrete MyOS identity bindings (`accountId` / `email`) are attached in bootstrap.
- Helpers include:
  - bulk abstract channel creation (`timelineChannels(...)`)
  - composite channels (`compositeTimelineChannel(...)`)
  - in-document source-binding contracts (`channelSourceBinding(...)`)
  - role binding (`bindRole(...)`, `bindRoleAccount(...)`, `bindRoleEmail(...)`)

## Typed events by default

Meaningful domain events are authored via typed classes and `TypeRef` mapping, e.g.:

- `Shipping/Shipment Confirmed`
- `Recruitment/CV Classification Requested`
- `PayNote/Card Transaction Capture Lock Requested`

`Common/Named Event` remains available for ad-hoc cases.

## Template specialization pipeline

Use first-class immutable templates:

1. `DocTemplate` base (abstract participants and contracts)
2. `.specialize(...)` for product/rail/currency variants
3. `.instantiate(...)` for final participant bindings

Base templates remain unchanged after specialization.

## PayNote builder facade

`PayNotes` + `PayNoteBuilderVNext` provide high-level methods for:

- reserve/capture/refund/release/cancel
- child issuance on event
- card lock/unlock workflows
- once/barrier helpers
- lifecycle hooks such as `onFundsCaptured(...)`

## vNext examples included

- iPhone shipment escrow
- shipment template chain (template → EUR200/CHF + DHL → final instance)
- subscription paynote
- marketplace split
- agent budget
- milestone contractor
- reverse voucher
- recruitment classifier (structured JS + typed recruitment events)

Legacy examples remain for compatibility; vNext examples demonstrate the recommended style.
