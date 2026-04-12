# Changelog

## [1.1.0] - 2026-04-12

### Added
- **PDF text extraction**: Automatically extracts IBANs and structured communications from PDF attachments using PDFBox. Detected values are shown in a bottom sheet for confirmation.
- **Manual IBAN/communication override**: Tap the warning banner ("No IBAN" or "No structured communication") to open a bottom sheet where you can pick from detected values or enter manually. Overrides are persisted locally for 4 months via Room DB.
- **Warning banners become tappable**: "No IBAN — tap to set" / "No structured communication — tap to set". Once set, the banner disappears and the Pay button becomes available.

### Fixed
- **Swipe between invoices**: Changed HorizontalPager from `fillMaxSize()` to `weight(1f)` so it takes remaining space after the header, fixing layout constraints that prevented swipe gestures.
- **Odoo payment reconciliation**: Added `payment_difference_handling: reconcile` and `dont_redirect_to_payments: true` to payment wizard context. Added post-payment verification logging. Note: "in payment" is Odoo's correct state for a payment awaiting bank statement reconciliation — it will become "paid" when the bank statement is imported and matched.

## [1.0.0] - 2026-04-12

### Added
- **BEP payment deep links**: Discovered that Belgian banking apps register the `bepgenapp://dotx` URI scheme (Belgian Electronic Payment interoperability standard, same mechanism used by Doccle). The app now uses this as the primary payment method — tapping Pay triggers the system app chooser showing all installed Belgian banking apps with the payment pre-filled (IBAN, amount, communication, creditor name). No more clipboard workaround needed for banks that support BEP.

### Changed
- **Payment strategy**: Now three-tier — (1) BEP interop deep link, (2) bank-specific deep link, (3) clipboard + app launch fallback.
- **Wordmark**: "Pay" in white, "Unpaids" in accent orange.
- **Slogan**: "One app, zero friction" (was "one tap").
- **App icon**: Dark background, white document with orange accents, green checkmark.

### Fixed
- **Hello Bank detection**: Added `com.bnpp.hellobank` as primary package name (confirmed via `adb`).

### Added
- **Translations**: FR, DE, NL, ES, IT, PT for easter egg and settings strings.

## [0.9.0] - 2026-04-11

### Changed
- **Look and feel mirrors SnapAndMail**: Adopted the same color palette (accent orange `#E8793A` on near-black backgrounds), typography (DM Sans / DM Mono with tight letter-spacing), and top-bar wordmark style.
- **App wordmark at top**: The top bar now shows `PayUnpaids` with `Pay` and `paids` in accent orange and `Un` in white — matching SnapAndMail's `SnapAndMail` branding.
- **Invoice info moved**: The invoice counter / amount / date and supplier name moved from the top bar title to a secondary header row right below the wordmark.
- **Settings top bar**: Also uses `headlineMedium` + black background for consistency.

### Added
- **Triple-tap easter egg**: Tapping the `PayUnpaids` wordmark three times reveals a dialog with "Crafted by Patrick Alaerts / Assisted by Claude Code" and the tagline "Fetch \u2192 Review \u2192 Pay — One tap, zero friction."

## [0.8.1] - 2026-04-11

### Fixed
- **Built-in PDF viewer for scanned invoices**: `PdfRenderer` was running out of memory on large scanned PDFs (which are typically full-page images at high DPI), causing the viewer to fail and potentially triggering system fallback behavior. The renderer now:
  - Sizes bitmaps based on screen width (600\u20132000 px, clamped)
  - Caps total pixels to ~4M to avoid OOM
  - Falls back from ARGB_8888 to RGB_565 on OOM, then half-size as last resort
  - Closes pages properly in a `finally` block
  - Shows per-page error message instead of failing silently

## [0.8.0] - 2026-04-10

### Added
- **Swipe between invoices**: Horizontal swipe on the invoice content navigates to previous/next invoice. Synced bidirectionally with Prev/Next buttons.
- **Multi-page PDF navigation**: PDFs with multiple pages now use a vertical pager — swipe up/down to see additional pages. Page indicator (e.g. "1 / 2 \u2195") shown in the top-right.
- **All banks listed in settings**: All Belgian banks now appear in the bank picker, not just installed ones. Each shows a green check if installed, or "Not installed" subtitle. User can pre-select a bank before installing the app.

### Changed
- **Settings layout**: Test Connection button moved to right after the Odoo credentials (URL/username/password). Banking app section moved further down.

### Notes on ING / banking apps
- **No Belgian banking app supports payment deep links** with pre-filled IBAN/amount/communication. The app uses the clipboard fallback: payment details are copied to clipboard and the banking app is opened. You then need to start a new payment in the banking app and paste the details.

## [0.7.0] - 2026-04-09

### Changed
- **Bank selection moved to Settings**: User picks their preferred banking app in Settings, from a list of detected installed apps only. The Pay button on the invoice screen now launches that bank directly — no more bottom sheet picker.
- **Title/subtitle swapped**: Top bar now shows invoice info (sequence, amount, date) as the main title and supplier name as the subtitle. Subtitle is brighter (80% white instead of gray).

### Removed
- Payment bottom sheet (bank picker) — replaced by settings-based selection.

## [0.6.0] - 2026-04-09

### Added
- **Bank account selection**: When marking as paid and multiple bank journals exist in Odoo, a dialog lets you choose which account (company vs personal) the payment was made from. The selected journal is passed to Odoo's payment wizard.
- **Hello Bank support**: Added `be.bnpparibasfortis.easybanking` as alternate package name for BNP Paribas Fortis (Hello Bank variant). Each bank now carries a list of package names to try.

### Fixed
- **Payment state**: Added `group_payment: false` to payment wizard to improve reconciliation. Note: "in payment" is Odoo's standard state for registered payments awaiting bank statement reconciliation.
- **Prev/Next buttons**: Replaced `TextButton` (which overrides text colors) with custom `ActionButton` using `Box` + `clickable`. Buttons now display with proper white text, never grayed out.

### Changed
- `Bank` enum field `packageName` renamed to `packageNames` (list), supporting multiple package name variants per bank.

## [0.5.0] - 2026-04-09

### Changed
- **Undo paid**: Invoices are no longer removed from the list when marked as paid. They remain navigable with a green "PAID" overlay and the "Paid" button turns green. Tapping it again reverts the local paid state. Odoo payment registration happens immediately on mark-paid but undo only affects local state.
- **Pay button**: Hidden for already-paid invoices (reappears on undo).
- **Top bar subtitle**: Now shows paid count (e.g. "3/19 (5 paid)").
- **All Done**: Only shown when every invoice in the list is marked paid.

## [0.4.2] - 2026-04-09

### Fixed
- **Banking app launch**: Deep link schemes (`ing-homebank://`, etc.) are not supported by most Belgian banking apps. Now tries deep link first, then falls back to copying payment details to clipboard and opening the banking app by package name. Shows "Payment details copied — paste in [bank]" toast.

### Changed
- `Bank` enum now includes `packageName` for each bank (used for fallback launch).
- AndroidManifest `<queries>` now declares both URI schemes and package names.

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
