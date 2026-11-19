# Legacy Cloud Run API prototype

This folder is retained for reference only. The current low-cost architecture uses the Apps Script backend in `../apps-script/`; do not deploy this prototype for the app unless the architecture changes back to Cloud Run.

Private Google Cloud Run service planned for the Android app and the future Manager app.

## Sheet contract inspected for this app

Spreadsheet: `Shop`

- `Inventory`: SKU, Product Name, Category, Supplier, Unit Cost, Selling Price, Opening Stock, Reorder Level, Purchased Qty, Sold Qty, Current Stock, Stock Value, Stock Status, QR Code
- `Customers`: Customer ID, Customer Name, Phone, Email, City / Address, Customer Type, Total Orders, Total Spent, Last Purchase Date
- `Sales Invoices`: Invoice #, Date, Customer ID, Customer Name, Items, Invoice Total, Gross Profit, Payment Mode, Invoice File, Customer Phone
- `Sales Items`: Invoice #, Date, SKU, Product Name, Category, Quantity, Unit Price, Discount, Line Total, COGS, Gross Profit

The sheet's calculated formulas currently reference rows 2–100. The backend must extend formula coverage when it appends records; adding values only would silently stop stock and invoice calculations beyond those rows.

## Planned API

- `GET /v1/products?q=` — match SKU or product name, return price and current stock.
- `POST /v1/products` — create product, generate SKU server-side, write Inventory fields.
- `GET /v1/customers?phone=` — normalize and match phone number.
- `POST /v1/customers` — create customer if no exact phone match exists.
- `POST /v1/invoices` — atomically append invoice header and sales items with a unique invoice number; create a PDF in the private invoice folder; save its URL in `Invoice File`.

The service will use a Cloud Run service identity with access only to the Shop spreadsheet and the invoice folder. Android users authenticate with Google Sign-In and an email allowlist. No service-account credentials or sheet secrets are stored in the APK.

For this first single-shop implementation, deploy one Cloud Run instance with request concurrency set to 1 so allocation of the next blank spreadsheet row is serialized. The invoice number itself is a server-generated unique identifier, not a count-based sequence.

The existing formula columns stop at row 100. Every write re-evaluates formula coverage for the current data rows with references extended through row 1,000. The QR Code sheet formula is extended through row 1,000 as well. Do not disable those formula updates or append only raw values.

## Configuration still required before live writes

- Cloud Run project and region
- Android OAuth client ID and authorized staff email list
- Service identity granted access to the spreadsheet and invoice folder
- Shop spreadsheet ID supplied through server-side configuration

Cloud Run environment variables are `SHOP_SPREADSHEET_ID`, `INVOICE_FOLDER_ID`, `GOOGLE_OAUTH_CLIENT_ID`, and `AUTHORIZED_EMAILS` (comma-separated). The Android build needs the Cloud Run HTTPS URL as `shop.api.baseUrl` and the same web/server OAuth client ID as `google.server.clientId` in the ignored `local.properties` file.

This repo contains no deployed endpoint yet. The app clearly labels its local preview data and does not write to the live sheet.
