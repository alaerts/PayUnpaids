# PayUnpaids — Android App

Kotlin Android app that fetches unpaid invoices from Odoo and triggers payment via Belgian banking apps using deep links.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| API calls | Retrofit + OkHttp |
| Local cache | Room Database |
| Credential storage | EncryptedSharedPreferences |
| DI | Hilt |
| Async | Coroutines + Flow |
| Navigation | Navigation Compose |
| Testing | JUnit + MockK + Espresso |

---

## Odoo Connection

- Protocol: XML-RPC over HTTPS (`/xmlrpc/2/common`, `/xmlrpc/2/object`)
- Authentication: API key (not password)
- Database name: extracted from the URL subdomain — `https://mycompany.odoo.com` → `mycompany`
- Credentials stored in `EncryptedSharedPreferences` (AES-256, Android Keystore)

Fields to retrieve per invoice:
- `id`
- `name` (invoice reference)
- `partner_id` (supplier name)
- `amount_residual` (amount due)
- `invoice_date_due`
- `payment_reference` (structured communication `+++xxx/xxxx/xxxxx+++`)
- `partner_bank_id.acc_number` (IBAN)
- `invoice_date`
- `state` (filter: `posted`, `not in: paid`)
- Attachments: fetched separately via `ir.attachment`

---

## App Behaviour

### On launch
1. Check for stored credentials → if none, show Settings screen
2. Fetch all unpaid invoices from Odoo (oldest first)
3. For each invoice, check attached PDF filename:
    - Matches `snapandmail_YYYY-MM-DD_HHMMSS.pdf` → auto-mark as paid in Odoo, skip silently
    - Already marked paid → skip
4. Navigate directly to the first remaining unpaid invoice (no list screen)
5. Sync progress logged to Logcat only — no sync UI screen

### Invoice screen
- Shows the attached PDF full screen (via `PdfRenderer`)
- If no PDF attached: show invoice detail card (supplier, amount, due date, IBAN, structured ref)
- Top bar: supplier name · invoice counter (e.g. `3 / 19`) · amount · settings gear
- Bottom action bar: `← Prev` · `✓ Paid` · `Pay ▾` · `Next →`
- Sort order: oldest invoice first

### Warnings
- **No structured communication** (`+++ref+++` missing): amber warning banner below app bar. Pay button remains enabled — user must verify manually in banking app.
- **No IBAN**: Pay button hidden, warning shown.

### After tapping Pay
- Bottom sheet appears with bank picker
- On return from banking app (`onResume`): invoice automatically marked as paid in Odoo

### All done screen
- Shown when no unpaid invoices remain
- Displays session stats: auto-paid, paid via bank, marked manually
- Refresh button to re-sync

---

## Payment — Deep Links

| Bank | Mechanism | Deep link scheme |
|---|---|---|
| ING | Deep link | `ing-homebank://payment?iban=...&amount=...&currency=EUR&name=...&communication=...` |
| BNP Paribas Fortis | Deep link | `bnpparibasfortis://payment?...` |
| KBC | Deep link | `kbc-mobile://payment?...` |
| Belfius | Deep link | `belfius://payment?...` |
| Keytrade | Clipboard fallback | Copies IBAN + amount + ref to clipboard, then opens Keytrade |

Parameters passed to every banking app:
- Creditor IBAN
- Amount (EUR)
- Structured communication (`+++xxx/xxxx/xxxxx+++`)
- Creditor name

---

## Screens

See `mockup.html` for the interactive screen mockup.

| Screen | Trigger |
|---|---|
| Settings | First launch (no credentials) or via ⚙ icon |
| Invoice viewer (PDF) | Default invoice display |
| Invoice viewer (no comm. ref warning) | `payment_reference` field is empty |
| Pay sheet (bottom sheet) | Tap Pay ▾ |
| All done | No unpaid invoices remain |

---

## Settings Screen

Fields:
- Odoo URL (e.g. `https://mycompany.odoo.com`)
- Username (email)
- API Key (masked input)

- Database name is **not** asked — extracted automatically from the URL subdomain
- "Test connection" calls `/xmlrpc/2/common` → `authenticate` and shows result inline
- On save: credentials written to `EncryptedSharedPreferences`
- Accessible anytime via ⚙ icon in top app bar

---

## Development Plan

### Phase 1 — Odoo XML-RPC Layer
Tests first:
- `OdooAuthTest` — authenticate, assert UID returned
- `InvoiceFetchTest` — fetch unpaid invoices, assert fields present
- `InvoiceMarkPaidTest` — mark paid, re-fetch, assert status
- `AttachmentFetchTest` — retrieve PDF binary
- `SnapAndMailDetectionTest` — filename regex (unit, no network)

Implementation:
- `OdooXmlRpcClient` — raw XML-RPC via OkHttp
- `InvoiceRepository` — fetch, mark paid, fetch attachments
- `SnapAndMailFilter` — filename pattern matcher

### Phase 2 — Deep Link Payment Layer
Tests first:
- `IngDeepLinkTest` — assert correct URI built
- `BnpDeepLinkTest` — assert correct URI built
- `KbcDeepLinkTest` — assert correct URI built
- `BelfiusDeepLinkTest` — assert correct URI built
- `KeytradeClipboardTest` — assert clipboard payload correct
- `PaymentDispatcherTest` — correct handler called per bank
- `OnResumeMarkPaidTest` — return from banking app triggers mark-as-paid

Implementation:
- `PaymentUriBuilder` — builds deep link URIs per bank
- `PaymentDispatcher` — fires correct Intent or clipboard fallback
- `BankingAppMonitor` — `onResume` hook

### Phase 3 — Data Layer & Local Cache
Tests first:
- `RoomInvoiceDaoTest`
- `SyncRepositoryTest`
- `CredentialStorageTest`

Implementation:
- `InvoiceEntity` + `InvoiceDao` (Room)
- `SyncRepository`
- `CredentialStore` (EncryptedSharedPreferences wrapper)

### Phase 4 — Core UI
Tests first:
- `InvoiceViewModelTest` — ordering, auto-skip, SnapAndMail auto-pay
- `NavigationTest` — prev/next, counter
- `NoCommRefWarningTest` — warning shown, Pay still enabled
- `AllDoneStateTest` — empty state when no invoices remain

Implementation:
- `InvoiceViewModel`
- `InvoicePdfScreen` (PdfRenderer)
- `InvoiceFallbackScreen` (no PDF)
- `PaymentBottomSheet` (bank picker)
- `AllDoneScreen`

### Phase 5 — Settings Screen
Tests first:
- `FirstLaunchDetectionTest`
- `ConnectionTestTest`
- `SettingsSaveTest`

Implementation:
- `SettingsScreen`
- `SettingsViewModel`
- `AppStartupRouter`

### Phase 6 — Integration & Polish
- End-to-end flow with real Odoo sandbox
- Error handling (network, Odoo down, malformed data)
- Loading states
- Edge case hardening

### Dependency graph
```
Phase 1 (Odoo) ──┐
                  ├──→ Phase 3 (Cache) ──→ Phase 4 (UI) ──→ Phase 6
Phase 2 (Pay)  ──┘                               ↑
                                           Phase 5 (Settings)
```
Phases 1 and 2 are fully independent and can be developed in parallel.