# Play Console Data safety and listing notes

**Prepared from source inspection and the shop's confirmed retention and audience details. Review every answer in Play Console against the final production build before submission.**

## Data types to review for declaration

| Play data category | App data | Purpose | Current source evidence |
| --- | --- | --- | --- |
| Personal info | Customer name, phone number, optional email and address | Customer records, bills, payments, order requests | Customer API and customer form fields |
| Personal info | Staff Google account email | Authentication and access control | Google ID token email is verified and checked against the staff allowlist |
| Financial info | Purchase history, invoice totals, payment amount/method, balance due | Billing and payment records | Invoices and payment actions in the backend |
| Photos and videos | Product images | Product catalog and product identification | Product image picker and product image records |

Review whether the Play form categorizes prices/transaction records as purchase history or other financial information. The app does not collect bank account or card numbers in the inspected source.

## Preliminary answers to verify

- **Collected:** Yes. The app sends shop/customer/product/transaction records to the shop backend. Google account email is used to authenticate staff.
- **Shared:** No sale of data is apparent in the inspected code. Shop records are processed/stored by Google Sheets, Drive, and Apps Script. Staff can explicitly share invoice PDFs with another installed app. Confirm the Play Console definition and the shop's contractual setup before answering.
- **Ephemeral processing:** No. Customer, catalog, and transaction records are persisted in the shop's sheets/Drive; product photos are associated with catalog records. Local drafts/cache may also persist on-device.
- **Encryption in transit:** Yes for backend API calls, which use HTTPS. Confirm the deployed endpoint and any separately configured URLs are HTTPS before submitting.
- **Deletion:** There is no self-service account deletion flow. Shop records remain in Sheets/Drive until the administrator removes them; some removal/cancellation requests require administrator review and preserve audit history. State these retention and deletion limits in the Play form.
- **Ads:** No ads SDK or ad permission is present in the inspected Android build. Recheck the final release dependencies before declaring.
- **Target audience:** Adult shop owners and staff; not directed to children. Select the matching adult audience in Play Console.

## Required before public release

1. Publish `PRIVACY_POLICY_DRAFT.md` as a readable page at `https://ekanima.in/privacy-policy.html` and verify it is publicly accessible.
2. Confirm who is legally responsible for customer data (shop/business operator versus app publisher).
3. Confirm whether optional product-image upload and invoice sharing work exactly as described in the production build.
4. Complete Play Console Data safety, target audience, app access instructions, content rating, and store listing using the confirmed practices.
5. Reconcile OAuth Android credentials with the final application ID and register the Play app-signing SHA-1 after Play App Signing enrollment.
