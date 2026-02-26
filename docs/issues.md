# Open mapping issues

## 1) `Common/Named Event` mapping not discoverable in current repo.blue packages

- Java bean: `blue.language.types.common.NamedEvent`
- Current annotation value in codebase was a placeholder-style string.
- Lookup method:
  - Checked package index on `repo.blue/packages`
  - Checked `Common` package type list on `repo.blue/packages/Common/types`
- Result:
  - Current `Common` package exposes only `Currency` and `Timestamp` in the selected public revision.
  - No `Named Event` type page is available to extract authoritative type Blue ID.
- Action taken:
  - Kept existing alias/BlueId for `NamedEvent` unchanged.
  - Deferred until package revision containing `Common/Named Event` is publicly available.

## 2) `Payments/* Requested` types package not discoverable in current repo.blue package index

- Java beans: nested request classes under `blue.language.types.payments.PaymentRequests`
- Current annotation values are placeholder-style IDs.
- Lookup method:
  - Enumerated public package list from `repo.blue/packages`
  - Attempted direct lookups for a `Payments` package/type pages.
- Result:
  - `Payments` package is not currently listed in the public package index.
  - No canonical type pages are available to retrieve authoritative Blue IDs and full field schemas.
- Action taken:
  - Kept `PaymentRequests` nested class Blue IDs unchanged for now.
  - Deferred mapping update until `Payments` package/type docs are accessible.

## 3) Non-44-char IDs that are still canonical in repo.blue

During audit, some canonical IDs are 43 characters in current repo metadata:

- `Core/Triggered Event Channel` → `C77W4kVGcxL7Mkx9WL9QESPEFFL2GzWAe647s1Efprt`
- `MyOS/MyOS Session Link` → `d1vQ8ZTPcQc5KeuU6tzWaVukWRVtKjQL4hbvbpC22rB`

These were not treated as placeholders and were left unchanged.
