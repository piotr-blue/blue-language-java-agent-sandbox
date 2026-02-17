# Blue Language Spec Alignment Status

This branch implements the active alignment plan for Blue Language Specification 1.0, with the required divergence:

- **BlueId semantics are computed as** `minimize(resolve(node))`.

## Completed alignment areas

### 1) Semantic BlueId unification
- Added provider-aware semantic BlueId paths for identity-critical comparisons.
- Updated subtype and inherited-list identity checks to use semantic IDs under provider context.
- Preserved canonical hash utilities for low-level canonical operations.

### 2) Schema compatibility + constraints completeness
- Added `schema` alias support while preserving legacy `constraints`.
- Added support for:
  - `minFields`
  - `maxFields`
  - `enum`
- Extended propagation and verification logic with regression coverage.

### 3) List semantics
- Added first-class `mergePolicy` model support.
- Enforced:
  - default `positional`
  - explicit `append-only`
  - rejection of unknown merge policies
- Implemented list control forms:
  - `$previous` anchor validation
  - `$pos` positional overlay validation
  - `$empty` treated as content
- Added list regression tests for malformed/out-of-range/duplicate controls and valid overlays.

### 4) BlueId canonicalization exactness
- Enforced recursive cleanup in canonicalization/hash paths:
  - remove `null`
  - remove empty maps
  - preserve empty lists
- Normalized non-content list controls out of hash payloads:
  - `$previous: true`
  - `$pos`

### 5) Processor model conformance refinements
- Registry lookup now supports assignable subtype matching with most-specific selection.
- Added must-understand enforcement for non-reserved marker contracts.
- Preserved reserved marker key behavior and compatibility checks.

## Verification

- Targeted module suites were run after each logical change.
- Full repository suite (`./gradlew test`) passes on this branch.
