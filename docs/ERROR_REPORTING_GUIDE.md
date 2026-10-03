# FinPulse - Production Error & Crash Reporting Guide

## 1. Overview & Reporting Platform

FinPulse uses **Firebase Crashlytics** for centralized, production-grade error and crash reporting. 
Since FinPulse already utilizes Google Firebase services (Firebase Authentication and Cloud Firestore), Crashlytics provides unified infrastructure with zero credential leakage, zero SMTP risks, and direct R8/ProGuard symbolication.

### Key Capabilities
- **Automatic Fatal Crash Capture**: Captures any unhandled exceptions in Release and Debug builds without altering standard Android uncaught exception handling or application lifecycle.
- **Symbolicated / Deobfuscated Stack Traces**: Integrated with Android Gradle Plugin R8/ProGuard mapping upload (`uploadCrashlyticsMappingFileRelease`) to ensure obfuscated production stack traces retain exact class, method, and line numbers.
- **Non-Fatal Error Diagnostics**: Important unexpected errors (cloud sync, backup export/restore, CSV import, background workers, storage failures) are safely captured via an abstraction interface with technical context.
- **Fail-Safe Resiliency**: Error reporting calls are wrapped in defensive try-catch handlers. If the device is offline or the Crashlytics backend encounters a transient issue, the app will never crash, and reports are safely queued locally until connectivity is restored.
- **Strict Data Sanitization & Zero PII Guarantee**: Centralized sanitizer (`DataSanitizer`) strips passwords, PINs, tokens, financial amounts, balances, account names, transaction descriptions, notes, and cleartext emails before any diagnostic data reaches the network or disk.

---

## 2. Architecture & Abstraction

To ensure high cohesion, low coupling, and testability, reporting is abstracted behind the `ErrorReporter` interface:

```
[ Domain / Presentation / Workers ]
               │
               ▼
      [ ErrorReporter ]  (Interface in core.reporting)
               │
   ┌───────────┴────────────┐
   ▼                         ▼
[ FirebaseCrashlytics-   [ NoOpErrorReporter ]
   ErrorReporter ]         (Unit Tests / Off)
         │
         ▼
  [ DataSanitizer ] ──(Filters forbidden keys, regex scrubs PII, hashes UIDs)
         │
         ▼
[ FirebaseCrashlytics SDK ]
```

### Components
1. **[ErrorReporter](file:///c:/Users/aleks/.gemini/antigravity-ide/scratch/FinPulse/app/src/main/java/md/alexlab/finpulse/core/reporting/ErrorReporter.kt)**: The public contract for recording exceptions, breadcrumb logs, custom diagnostic keys, and anonymous user contexts.
2. **[FirebaseCrashlyticsErrorReporter](file:///c:/Users/aleks/.gemini/antigravity-ide/scratch/FinPulse/app/src/main/java/md/alexlab/finpulse/core/reporting/FirebaseCrashlyticsErrorReporter.kt)**: Production implementation delegating to `FirebaseCrashlytics`.
3. **[DataSanitizer](file:///c:/Users/aleks/.gemini/antigravity-ide/scratch/FinPulse/app/src/main/java/md/alexlab/finpulse/core/reporting/DataSanitizer.kt)**: Centralized scrubber enforcing privacy policies and data redacting.
4. **[DiagnosticContext](file:///c:/Users/aleks/.gemini/antigravity-ide/scratch/FinPulse/app/src/main/java/md/alexlab/finpulse/core/reporting/DiagnosticContext.kt)**: Standardized technical identifier generator for FinPulse features.
5. **[AppContainer](file:///c:/Users/aleks/.gemini/antigravity-ide/scratch/FinPulse/app/src/main/java/md/alexlab/finpulse/di/AppContainer.kt)**: Dependency injection container providing a singleton `ErrorReporter` across the app.

---

## 3. Where to View Production Errors

All crash and non-fatal reports are visible remotely in the Firebase Console:

1. Navigate to: **[Firebase Console](https://console.firebase.google.com/)**
2. Select the project: **`finpulse-cloud`** (Project Number: `500924060314`)
3. In the left navigation menu, open **Release & Monitor** > **Crashlytics**
4. Filter by:
   - **Event Type**: `Crashes` (fatal uncaught errors) or `Non-fatals` (caught unexpected failures)
   - **Build Version**: e.g., `1.0.0 (1)`
   - **App Variant**: `md.alexlab.finpulse`
5. Click on an issue to inspect:
   - Full symbolicated stack trace with line numbers
   - Device model, manufacturer, Android OS version, battery percentage, orientation, free RAM, and storage
   - **Keys tab**: Attached sanitized technical metadata (`feature`, `operation`, `sync_mode`, `app_version`, `build_type`)
   - **Logs tab**: Diagnostic session breadcrumbs leading up to the error
   - **User tab**: Pseudonymous hashed user ID (`anon_<sha256>`)

---

## 4. How to Enable Developer Notifications & Alerts

Firebase Crashlytics includes automated real-time alerts. To configure them:

1. In the **Firebase Console**, click the **Gear Icon (Project Settings)** > **Integrations** or **Alerts**.
2. Alternatively, in the **Crashlytics dashboard**, click the **Bell icon / Alert settings** in the top right.
3. Enable the following alert types:
   - **Velocity Alerts**: Fires immediately when an issue impacts more than 1% of user sessions in a 1-hour window.
   - **New Fatal Issue Alert**: Fires an alert the very first time a new crash signature is observed in production.
   - **Regressed Issue Alert**: Fires if an issue marked as "Closed" in the dashboard reoccurs in a new version.
4. Set delivery channels:
   - **Email**: Receive alerts at your developer email address.
   - **Slack**: Connect a webhook to a `#dev-alerts` channel.
   - **Jira / PagerDuty**: (Optional) Connect enterprise incident tracking if desired.

> [!NOTE]
> Do NOT implement an SMTP client or send emails directly from the client Android device. Crashlytics aggregates, deduplicates, and sends intelligent alerts server-side.

---

## 5. What Errors Are Reported vs. Ignored

### Reported Errors (Unexpected Technical Failures)
- **Cloud Synchronization Failures**: Unhandled network timeouts, Firestore batch failures, tombstone synchronization conflicts in `CloudSyncEngine`.
- **Data Import Failures**: Malformed statement parsing failures, unexpected exceptions while inserting CSV statement records in `CsvImportViewModel`.
- **Data Export & Backup Failures**: Database serialization errors, encryption failures, or restore write failures in `ExportViewModel`.
- **Background Worker Failures**: Repeated failures in `SyncWorker`, `ExchangeRateSyncWorker`, `RecurringCheckWorker`, and `FinancialDigestWorker`.
- **Storage / Firestore Failures**: Security rule violations, connection drops, or batch commit failures in `FirestoreCloudStorageDataSource`.
- **Unexpected Authentication Errors**: Fatal exceptions during Google credential exchange.

### Ignored Errors (Normal / Expected User Conditions)
- **Form Validation**: Empty category names, missing transaction notes, zero amounts.
- **PIN & Biometric Rejections**: Normal incorrect PIN attempts or canceled biometric prompts.
- **Backup Password Prompt**: Missing passwords for encrypted backups (`PASSWORD_REQUIRED`) is treated as a normal user flow.
- **Offline Network Mode**: Expected lack of connectivity when device is offline without errors.

---

## 6. Diagnostic Context & Data Sanitization

### Attached Technical Metadata
When an error occurs, the following technical identifiers are attached:
- `feature`: "cloud_sync", "csv_import", "backup_export", "database", "auth", "exchange_rates", "recurring", "financial_digest"
- `operation`: "full_sync", "upload_changes", "download_changes", "create_backup", "restore_backup", "export_csv", "parse_csv", "execute_import"
- `sync_mode`: "cloud" or "local"
- `app_version`: "1.0.0"
- `build_type`: "release" or "debug"
- `status`: Technical error category or code

### Strictly Excluded / Redacted Data
FinPulse implements a strict denylist and regex scrubbing in `DataSanitizer`. The following are NEVER sent:
- **Financial Information**: Transaction amounts, balances, currency figures (scrubbed via `CURRENCY_AMOUNT_REGEX`).
- **Account Details**: Account names, bank names, credit card numbers, IBANs (scrubbed via `CARD_OR_IBAN_REGEX`).
- **Personal Descriptions**: Transaction titles, notes, payee/merchant names.
- **Credentials & Tokens**: Passwords, PINs, Google ID tokens, OAuth tokens, Bearer headers, Firebase API keys (scrubbed via `BEARER_TOKEN_REGEX`, `JWT_REGEX`, `GOOGLE_API_KEY_REGEX`).
- **Cleartext Emails**: All email addresses are redacted to `[REDACTED_EMAIL]`.
- **User IDs**: Raw Google UID is one-way hashed using SHA-256 (`anon_<hex>`). On sign-out or account deletion, the user context is immediately cleared (`setUserId("")`).

---

## 7. How to Test Crash Reporting

A safe, debug-only diagnostic menu is integrated into the app.

### Method 1: In-App Debug Diagnostics (Recommended)
1. Build and run the app in **Debug** mode (`.\gradlew.bat installDebug` or run from Android Studio).
2. Open the **More (Hub)** tab from the bottom navigation.
3. Scroll to the bottom to find the **Crash & Error Diagnostics (Debug Only)** card:
   - **Enable / Disable Collection**: Toggle whether events are transmitted to Firebase.
   - **Test Non-Fatal**: Records a test `IllegalStateException` with sanitized technical context and logs breadcrumbs. Verify the event appears in the Firebase Crashlytics dashboard under "Non-fatals".
   - **Trigger Test Fatal Crash**: Prompts for confirmation and throws an uncaught `RuntimeException`. The app will crash. Re-launch the app to allow Crashlytics to upload the crash report.
4. Verify in the Firebase Console:
   - Crashlytics dashboard displays the crash with source file and line numbers.

> [!IMPORTANT]
> The `DebugDiagnosticsSection` is conditionally compiled inside `if (BuildConfig.DEBUG)`. In release builds (`minifyEnabled = true`), R8 removes this UI and its triggers completely.

### Method 2: Command Line Test Run
Run unit tests to verify the sanitization engine and reporter behavior:
```powershell
.\gradlew.bat testDebugUnitTest --tests "md.alexlab.finpulse.core.reporting.*"
```

Verify release packaging and R8 mapping upload:
```powershell
.\gradlew.bat assembleRelease
.\gradlew.bat bundleRelease
```

---

## 8. Google Play Data Safety & Privacy Disclosures

When publishing updates to Google Play Console, update the **Data Safety** questionnaire with the following entries:

### App info and performance
1. **Crash logs**:
   - **Collected?** Yes
   - **Shared?** No
   - **Encrypted in transit?** Yes (TLS 1.3 / HTTPS)
   - **Ephemeral?** No (Stored in Firebase Crashlytics for 90 days)
   - **Purposes:** Analytics, App functionality
2. **Diagnostics**:
   - **Collected?** Yes (Non-fatal error contexts and breadcrumbs)
   - **Shared?** No
   - **Encrypted in transit?** Yes
   - **Purposes:** Analytics, App functionality

### Device or other IDs
- **Collected?** Yes (Firebase Installation ID - a pseudonymous installation-specific UUID generated by Firebase SDK)
- **Shared?** No
- **Encrypted in transit?** Yes
- **Purposes:** Analytics, App functionality
