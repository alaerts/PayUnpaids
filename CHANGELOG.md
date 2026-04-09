# Changelog

## [0.4.1] - 2026-04-09

### Fixed
- **Mark-as-paid context serialization**: The `context` dict (containing `active_model` and `active_ids`) was serialized as a plain string instead of an XML-RPC struct, causing Odoo's `with_context()` to fail with `ValueError: dictionary update sequence element #0 has length 1`. Added `Map` handling to `xmlTypedValue` and refactored `xmlKwargs` to use it.

## [0.4.0] - 2026-04-09

### Fixed
- **Deep links**: Added `<queries>` declarations in AndroidManifest for banking app URI schemes (required on Android 11+). Added logging for deep link debugging.
- **Prev/Next buttons**: Removed `enabled` flag that was causing Material3 to gray out the buttons. Buttons now always look active; boundary checks are handled in click handlers.
- **Settings labels**: Changed label color from gray (`TextMuted`) to readable white (`TextPrimary`).

### Removed
- **Database field**: Removed the derived database display from the Settings screen — it's extracted automatically and was unnecessary UI clutter.

## [0.3.0] - 2026-04-09

### Fixed
- **Mark-as-paid now works in Odoo**: Previously used a no-op `write` call. Now properly creates an `account.payment.register` wizard and calls `action_create_payments` to register the payment in Odoo's accounting.

### Added
- **Pinch-to-zoom on PDFs**: PDF viewer now supports pinch-to-zoom (1x–5x) with pan gestures. Render resolution increased to 3x for clarity when zoomed.
- **Invoice date in top bar**: Subtitle now shows "Invoice X of Y . Amount . Date".
- **Play Store listing**: `PLAY_STORE_LISTING.md` with title, descriptions, tags, and asset guidelines. Includes "not affiliated with Odoo SA" disclaimer.

## [0.2.0] - 2026-04-09

### Fixed
- **PDF/invoice mismatch**: PDF viewer was caching to a single file (`current_invoice.pdf`), causing stale PDFs to display when navigating between invoices. Now uses unique file per invoice ID.
- **XML-RPC response parsing**: Replaced fragile regex-based parser with proper DOM parser (`DocumentBuilderFactory`). The regex approach could not handle Odoo 19's multiline XML with nested `<value>` tags, causing all invoices to silently fail to parse.
- **`search_read` duplicate domain**: The `domain` parameter was passed both as a positional argument and as a keyword argument, causing an Odoo 19 `TypeError`.
- **Fault message parsing**: XML-RPC fault strings were not extracted correctly from multiline responses.
- **Version parsing**: `server_version` was not parsed from multiline XML responses.

### Changed
- **Dark theme**: Entire UI updated to match the mockup's dark color scheme with amber (#F59E0B) accent.
- **Invoice screen**: Top bar now shows "Invoice X of Y . Amount", matching mockup layout.
- **Bottom action bar**: Restyled with rounded buttons matching mockup (Prev, Paid, Pay, Next).
- **Warning banners**: Amber-tinted background for missing structured communication, matching mockup.
- **Payment bottom sheet**: Full-width bank-colored buttons with brand badges and arrow indicators. Shows payment amount in header. Keytrade shows "Copies details to clipboard" subtitle.
- **All Done screen**: Dark card with stats rows, total count, "All caught up" text, matching mockup.
- **Settings screen**: Added derived database name field (auto-extracted, read-only). Full-width amber "Save & continue" button. Dark-themed input fields.
- **Bank logos**: Replaced generic Material icon with colored brand badges (ING orange, BNP green, KBC blue, Belfius red, Keytrade dark).

### Added
- **Help dialogs**: Each settings field has a `?` button with contextual help. Password field explains both local password setup and API key generation.
- **JSON-RPC authentication**: Falls back from JSON-RPC (`/web/session/authenticate`) to XML-RPC for Odoo Online compatibility.
- **Database listing**: Attempts to list available databases during connection test for diagnostics.
- **App logo**: Custom vector drawable — document with invoice lines and green checkmark.
- **Credential redaction**: API keys and passwords are redacted in Logcat output.

## [0.1.0] - 2026-04-08

### Added
- Initial MVP implementation.
- Odoo XML-RPC client (authenticate, search_read, write, read).
- Invoice fetching with PDF attachments via `ir.attachment`.
- Snap-and-mail auto-pay detection (`snapandmail_YYYY-MM-DD_HHMMSS.pdf` pattern).
- Deep link payment for ING, BNP Paribas Fortis, KBC, Belfius.
- Clipboard fallback for Keytrade.
- PDF viewer using `PdfRenderer` with multi-page paging.
- Invoice fallback card when no PDF is attached.
- Invoice navigation (prev/next) with counter.
- Warning banners for missing IBAN or structured communication.
- Payment bottom sheet with bank picker.
- Auto-mark-as-paid on return from banking app (`onResume`).
- All Done screen with session statistics.
- Settings screen with connection test.
- Room database for local invoice cache.
- EncryptedSharedPreferences for credential storage (AES-256).
- Hilt dependency injection.
- Jetpack Compose UI with Navigation.
- 25 unit tests (SnapAndMailFilter, PaymentUriBuilder, InvoiceViewModel).
