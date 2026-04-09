# PayUnpaids

Android app that fetches unpaid supplier invoices from [Odoo](https://www.odoo.com/) and lets you pay them one by one via Belgian banking apps.

> **Note:** This app is not affiliated with or endorsed by Odoo SA.

## Who is this for?

Small business owners and accountants who:
- Use **Odoo Accounting** (Online or on-premise) for supplier invoice management
- Bank with **ING, BNP Paribas Fortis, KBC, Belfius, or Keytrade** in Belgium
- Want to pay invoices quickly without manually copying IBANs and amounts

## How it works

1. Connect to your Odoo instance (URL + login + password/API key)
2. The app fetches all unpaid supplier invoices, oldest first
3. Each invoice is shown full-screen (PDF if attached, detail card otherwise)
4. Tap **Pay** to open your banking app with IBAN, amount, and structured communication pre-filled
5. When you return, the invoice is automatically marked as paid in Odoo
6. Invoices with `snapandmail_*.pdf` attachments are auto-marked as paid and skipped

## Supported banks

| Bank | Method |
|---|---|
| ING | Deep link (`ing-homebank://`) |
| BNP Paribas Fortis | Deep link (`bnpparibasfortis://`) |
| KBC | Deep link (`kbc-mobile://`) |
| Belfius | Deep link (`belfius://`) |
| Keytrade | Clipboard (copies IBAN + amount + ref, then opens app) |

## Tech stack

- **Kotlin** + **Jetpack Compose**
- **Hilt** for dependency injection
- **Room** for local invoice cache
- **OkHttp** for Odoo XML-RPC / JSON-RPC
- **EncryptedSharedPreferences** for credential storage (AES-256)
- **PdfRenderer** for inline PDF viewing

## Setup

1. Open the project in Android Studio
2. Sync Gradle
3. Run on a device or emulator (minSdk 25)

### Odoo connection

For **Odoo Online** (`*.odoo.com`), you need to set a local password:

1. Log into Odoo as admin
2. Settings > Users & Companies > Users > [your user]
3. Action > Change Password
4. Set a password — use it in the app

Alternatively, generate an **API key** under My Profile > Account Security > New API Key.

## License

Private — all rights reserved.
