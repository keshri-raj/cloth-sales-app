# Sales App Apps Script backend

This is the lower-cost backend option for the native Android app. It reads and updates the existing Shop spreadsheet, creates invoice PDFs in a private Drive folder, and serializes writes with a script lock. It does not require Cloud Run or a paid Cloud Run billing project.

## Setup

### Local website preview

Run `node apps-script/local-dev.mjs` from the repository root, then open `http://localhost:5173/`. The preview injects the public Web OAuth client ID and proxies `/api` calls to the deployed Apps Script endpoint, so the page can use the same backend without `google.script.run`. Google sign-in requires `http://localhost:5173` to be listed as an authorized JavaScript origin on the Web OAuth client. Do not deploy this local proxy to a public server.

### Google Cloud configuration already created

- Google Cloud project: `Sales App` (`sales-app-515120`, project number `866971493231`).
- OAuth consent screen: External, Testing mode.
- OAuth test users: `keshrirajcp@gmail.com`, `munnisbv@gmail.com`, `simransbv@gmail.com`.
- Android OAuth client: package `com.shop.salesapp`, debug signing certificate registered.
- Web/server OAuth client ID: `866971493231-c6k2bc9kvutjsdasqbt45iqvvvd7v0b1.apps.googleusercontent.com`.
- A standalone Apps Script project is deployed at `https://script.google.com/home/projects/1s_jnVs5E7f9GNY7hzk4-KNCQvOLyOSOQd6bu5tJEFJ7NvCpRrnnv02dd/edit`.

1. In the Apps Script project above, add the `Code.gs` and `appsscript.json` files from this folder. In **Project Settings**, enable **Show `appsscript.json` manifest file** and replace the generated manifest.
2. In **Project Settings → Script Properties**, add:
   - `SHOP_SPREADSHEET_ID`: the ID of the existing Shop spreadsheet.
   - `GOOGLE_OAUTH_CLIENT_ID`: `866971493231-c6k2bc9kvutjsdasqbt45iqvvvd7v0b1.apps.googleusercontent.com`.
   - `AUTHORIZED_EMAILS`: `keshrirajcp@gmail.com,munnisbv@gmail.com,simransbv@gmail.com`. Add each account to the Google OAuth test users while the app remains in Testing mode.
   - `ADMIN_EMAILS`: `keshrirajcp@gmail.com`. Every other approved account is a salesperson. Keep each administrator in both `AUTHORIZED_EMAILS` and `ADMIN_EMAILS`.
3. In **Services**, add the Advanced Google services **Drive API** and **Google Sheets API**. Enable the same APIs in the script's linked Google Cloud project if prompted.
4. Run `setupInvoiceFolder` once from the editor and authorize the requested scopes. It creates a private `Sales App Invoices` folder using the script owner's Drive and saves its ID as `INVOICE_FOLDER_ID`.
5. Deploy as **Web app**, executing as the script owner. The deployment access setting must allow the Android app's HTTPS request to reach `doPost`; the endpoint then verifies the Google ID token's audience, verified email, and staff allowlist on every call. Do not share edit access to the script project with sales users.
6. Copy the deployed `/exec` URL into `shop.api.baseUrl` in the ignored Android `local.properties`. The Web/server client ID is already set in `google.server.clientId`. Rebuild the Android app.

`doPost` accepts JSON actions including `session`, `requestDeletion`, `listDeletionRequests`, `reviewDeletionRequest`, `setProductAvailability`, and `addStockAdjustment`, along with product, customer, invoice, and payment actions. Stock additions are administrator-only and recorded with timestamp, cloth SKU, quantity, reason, and administrator in the `Stock Adjustments` tab. Current stock includes these additions while preserving the original opening stock and sales history. The script creates the adjustments tab when needed. `keshrirajcp@gmail.com` is the administrator by default; set `ADMIN_EMAILS` in Script Properties to change that assignment. Every other account in `AUTHORIZED_EMAILS` is a salesperson. Salespeople can request a payment reversal, bill cancellation, or product removal. Requests are stored in the `Deletion Requests` tab and appear to administrators in the matching Payment, Bills, or Cloth section. Approval marks payments and bills void, or marks a product unavailable; it retains the original sheet rows and request/reviewer audit record. Voided bill lines are excluded from invoice totals, customer totals, and inventory sold quantity. The API refuses new bills and orders for unavailable products even if a phone has stale cached data. Credit invoices record any amount received when the bill is created. Customer-level receipts allocate to the oldest unpaid bills first and record each allocation in `Payments` under one receipt number. Customer summaries derive totals from active invoice and payment records. The `invoicePdf` action checks that the requested invoice exists and its PDF belongs to the private invoice folder, then returns the PDF bytes to the signed-in app so Android can share the attachment through WhatsApp or another installed app. It returns JSON envelopes because Apps Script web apps do not let a script choose arbitrary HTTP status codes. The Android client follows Apps Script's content redirect.

## Authentication note

Google verifies ID-token signatures through Google's token verification endpoint, then the script checks the token audience and the configured staff email allowlist. The endpoint fails closed if verification is unavailable. Google documents this endpoint as a debugging-oriented validation route rather than its recommended production verifier, so it can be throttled or intermittently unavailable. At this shop's expected low request volume it is practical for a first rollout, but it may occasionally require a retry. If uninterrupted authentication becomes important, use a backend with Google's recommended ID-token verification library.

## Sheet behavior

- The tab names and headers match the existing Shop workbook.
- Product SKUs, customer IDs, and invoice numbers are generated server-side.
- Invoices validate stock and payment mode, write the invoice and line rows together in one Sheets API batch, and clean up the PDF if the sheet batch fails.
- Formula coverage is extended through the latest populated row, up to row 1,000. The Inventory QR formula is expanded through `A2:A1000`.
- PDFs remain private and are shared as viewer-only with the configured staff allowlist so the salesperson can open the receipt.
- The API serializes writes using `LockService` to prevent two sales from selecting the same blank row.

## Cost and limits

Apps Script has daily service quotas and execution limits. Review Google's current [Apps Script quotas](https://developers.google.com/apps-script/guides/services/quotas). This backend is sized for a handful of sales and lookups per day; it is not a high-volume public API.
