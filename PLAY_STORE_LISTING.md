# Google Play Store Listing

## App name
PayUnpaids

## Short description (80 chars max)
Pay your Odoo supplier invoices in seconds via Belgian banking apps.

## Full description (4000 chars max)

PayUnpaids fetches your unpaid supplier invoices from Odoo and lets you pay them one-by-one through your Belgian banking app — no manual copying of IBANs, amounts, or structured communications.

HOW IT WORKS

1. Connect to your Odoo instance (URL + login + password)
2. The app fetches all unpaid supplier invoices, sorted oldest first
3. Each invoice is displayed full-screen — as the attached PDF or a detail card
4. Tap Pay to open your banking app with all payment details pre-filled
5. When you return, the invoice is automatically marked as paid in Odoo
6. Move to the next invoice. Repeat until done.

SUPPORTED BANKS

- ING (deep link)
- BNP Paribas Fortis (deep link)
- KBC (deep link)
- Belfius (deep link)
- Keytrade (clipboard + app launch)

FEATURES

- Full-screen PDF invoice viewer with pinch-to-zoom
- Automatic detection and skipping of snap-and-mail invoices
- Warning banners when structured communication or IBAN is missing
- Session statistics: auto-paid, bank-paid, manually marked
- Secure credential storage (AES-256 encryption via Android Keystore)
- Offline invoice cache for fast navigation
- Dark theme optimized for quick scanning

WHO IS THIS FOR?

Small business owners, freelancers, and accountants in Belgium who:
- Use Odoo Accounting (Online or on-premise) for supplier invoices
- Want to speed up their payment workflow
- Are tired of manually copying payment details between Odoo and their banking app

ODOO COMPATIBILITY

- Odoo 17, 18, 19 (Online and on-premise)
- Connects via XML-RPC / JSON-RPC
- Requires a local password or API key (see in-app help for setup)

DISCLAIMER

This app is not affiliated with, endorsed by, or sponsored by Odoo SA. "Odoo" is a registered trademark of Odoo SA. This app is an independent third-party tool that connects to the Odoo platform via its public API.

## Category
Business

## Content rating
Everyone

## Tags
odoo, invoices, payment, banking, belgium, accounting, supplier, bookkeeping

## Contact email
(your support email)

## Privacy policy URL
(required for apps that access user accounts)

---

## Store assets needed

### App icon
512x512 PNG — use the existing ic_launcher_foreground.xml (invoice + green checkmark on white background)

### Feature graphic
1024x500 PNG — suggested design:
- Dark background (#0d1117)
- App icon on the left
- Text: "PayUnpaids" in white, "Pay Odoo invoices via your banking app" in amber (#F59E0B)
- Phone mockup on the right showing the invoice screen

### Screenshots (min 2, max 8)
Suggested screenshots:
1. Settings screen (first-time setup)
2. Invoice PDF viewer with top bar showing supplier name and amount
3. Invoice with "no structured communication" warning
4. Payment bottom sheet with bank selection
5. All Done screen with session statistics
