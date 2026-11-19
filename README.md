# Sales App

Native Android app for a small shop sales workflow. The interface is Kotlin + Jetpack Compose, with large touch targets and short steps. Google Apps Script is the planned low-cost backend for Shop data and invoice PDFs.

## First cut

- Home screen for **Create a bill**, **Find a product**, and **Add a product**.
- Product lookup by name/SKU and QR scanning.
- Bill draft with phone entry, Android contact picker, product quantities, and a visible total.
- Product form with selling price, purchase cost, and starting quantity.
- Invoice number generation, Sheet updates, and PDF output are handled by the backend, not the phone.
- Until the Apps Script endpoint is configured, the app clearly labels local sample data as Preview Mode.

## Open and build

Open this directory in Android Studio and let Gradle sync. Build the debug APK with the Gradle task `app > Tasks > build > assembleDebug`.

After deploying the Apps Script web app, set its `/exec` URL and the Web/server OAuth client ID in the ignored `local.properties` file:

```properties
shop.api.baseUrl=https://script.google.com/macros/s/DEPLOYMENT_ID/exec
google.server.clientId=WEB_OAUTH_CLIENT_ID.apps.googleusercontent.com
```

Rebuild the Android app to enable live sign-in and data. Do not put service-account keys or other private credentials in Android.

## Backend

The active backend source and setup steps are in [`apps-script/`](apps-script/README.md). It reads and updates the Shop workbook, creates private invoice PDFs, and checks a Google ID token against an explicit staff allowlist. The Apps Script web app runs as its owner; sales users should not be given edit access to the script project.

The older [`backend/`](backend/README.md) folder is the unused Cloud Run prototype. It is retained as reference source only. No Cloud Run service or billing account is needed for the Apps Script approach.
