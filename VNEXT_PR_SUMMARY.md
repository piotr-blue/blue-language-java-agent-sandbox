# Blue Java SDK vNext — Change Summary

Branch: `cursor/blue-java-sdk-vnext-5cee`

## Implemented

### Typed events and hierarchy
- Added typed domain events:
  - `ShippingEvents`
  - `PayNoteDemoEvents`
  - `RecruitmentEvents`
- Added typed PayNote card lock/unlock events in `PayNoteTypes`.
- Added `Core/Channel` alias and expanded `TypeAliases` mappings.
- Added hierarchy compatibility tests for:
  - `Core/Channel -> Conversation/Timeline Channel -> MyOS/MyOS Timeline Channel`

### Channel + binding ergonomics
- Added DSL helpers:
  - `timelineChannels(...)`
  - `myOsTimelineChannels(...)`
  - `compositeTimelineChannel(...)`
  - `channelSourceBinding(...)`
- Added `ChannelSourceBindingBuilder`.
- Added role-driven binding APIs in bootstrap and document builders.
- Added typed value wrappers:
  - `DocPath`
  - `ChannelKey`
  - `PayNoteRole`
  with builder overloads to reduce stringly-typed wiring.

### Template pipeline
- Added immutable template wrappers:
  - `DocTemplate`
  - `DocSpecializer`
  - `DocInstanceBindings`
  - `DocTemplates.template(...)`
- Upgraded shipment template chain to first-class template/specialize/instantiate flow.

### Structured JS ergonomics
- Added `JsCommon` helpers:
  - request extraction
  - coalesce
  - safe number parsing expression
  - typed JS event object helper
- Extended JS builder helpers and determinism assertions.

### PayNote SDK facade
- Added `PayNotes` + `PayNoteBuilderVNext` with:
  - reserve/capture/refund/release/cancel flows
  - card lock/unlock flows
  - child issuance
  - lifecycle hooks (`onFundsReserved`, `onCaptureRequested`, `onFundsCaptured`, `onReleased`)
  - once/barrier and allow-list direct change helpers
  - flattened `operation("...").channel(...).description(...).steps(...).done()` authoring API
  - fail-fast validation for incomplete operation builder configuration

### Required examples
- Added vNext examples:
  - iPhone shipment escrow
  - shipment template chain (template → EUR200/CHF+DHL → final instance)
  - subscription paynote
  - marketplace split paynote
  - agent budget paynote
  - milestone contractor paynote
  - reverse voucher paynote
  - recruitment classifier vNext
- Added shipment-focused class chain examples:
  - `ShipmentPayNote` (base template)
  - `DHLShipmentPayNote` (specialized overlay)
  - `AliceBobShipmentPayNote` (final instantiated document)
  - `ShipmentPayNoteNodeChaining` (ad-hoc Node extension path)

### Legacy alignment
- Migrated legacy shipment confirmation flows to typed shipment event emission.
- Marked legacy `PayNoteOverlay` as deprecated and added compatibility test.

### Documentation
- Added root `README.md` vNext authoring guide.
- Added `src/test/resources/samples/paynote/vnext-sdk-guide.md`.

## Validation

- Full repository tests pass: `./gradlew test`
- Paynote-focused suite passes: `./gradlew test --tests "blue.language.samples.paynote.*"`

## PR status

PR creation is blocked in this environment by token permissions (`Resource not accessible by integration`, HTTP 403).
Use the branch above to open the PR from a token/account with pull request write access.

Manual PR URL:

- https://github.com/piotr-blue/blue-language-java-agent-sandbox/pull/new/cursor/blue-java-sdk-vnext-5cee

Equivalent CLI command (from an account/token with PR write permissions):

- `gh pr create --base master --head cursor/blue-java-sdk-vnext-5cee --title "feat: blue java sdk vnext paynote dsl and examples"`
