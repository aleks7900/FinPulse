# Google Play Console Declarations Guide for FinPulse

**Application ID:** `com.finpulse.app`  
**Target SDK:** 36 (Android 16) | **Min SDK:** 26 (Android 8.0)  
**Document Purpose:** Exact instructions for completing all policy declarations and questionnaires in Google Play Console under **Policy > App Content**.

---

## 1. Declarations Summary Matrix

| Declaration / Questionnaire | Selection in Play Console | Verification / Technical Evidence in Code |
| :--- | :--- | :--- |
| **Ads Declaration** | **"No, my app does not contain ads"** | Zero advertising SDKs in `libs.versions.toml` and `app/build.gradle.kts`. |
| **Advertising ID (AAID)** | **"No"** | App does not collect or request `com.google.android.gms.permission.AD_ID`. |
| **App Access / Credentials** | **"All functionality is available without restrictions"** OR provide demo account | App is 100% usable offline without login. If testing cloud sync, provide a test Google account. |
| **Target Audience & Content** | **18 and older** (Adults) | App is a financial management tool for adults. Not directed to children. |
| **Financial Features Declaration** | **Personal Finance / Budgeting (Non-Banking)** | App does not execute wire transfers, make loans, issue credit, or custody fiat currency. |
| **Account Creation & Deletion** | **"Yes, users can create an account"** & provide in-app + web deletion URL | In-app deletion button in `SecurityScreen.kt` + web URL `https://finpulse.app/delete-account`. |
| **Government Apps** | **"No"** | App is not affiliated with or representing any government entity. |
| **Health & Medical Features** | **"No health features"** | App does not track health, medical symptoms, fitness, or telehealth. |
| **COVID-19 Contact Tracing / Status** | **"No"** | Not applicable. |
| **Data Safety** | **Complete as per `play-store/DATA_SAFETY.md`** | Detailed field-by-field answers provided. |
| **Content Rating (IARC)** | **Everyone / All Ages (PEGI 3 / ESRB Everyone)** | Questionnaire answers: No violence, no offensive language, no gambling, no controlled substances. |
| **News Apps** | **"No"** | App is not a news publication. |
| **User Generated Content (UGC)** | **"No public UGC"** | Notes/transactions are private to the user; no public feed, comments, or messaging. |

---

## 2. Permissions & Sensitive API Policy Declarations

Google Play enforces strict justification declarations for specific Android permissions. Below is the exact status for FinPulse:

### 1. Location (Fine, Coarse, Background)
- **Status:** **NOT APPLICABLE**
- **Evidence:** `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, and `ACCESS_BACKGROUND_LOCATION` are absent from `AndroidManifest.xml` and all merged dependency manifests.

### 2. Camera & Microphone
- **Status:** **NOT APPLICABLE**
- **Evidence:** `android.permission.CAMERA` and `android.permission.RECORD_AUDIO` are absent.

### 3. Photo & Media Access (Storage)
- **Status:** **NOT APPLICABLE**
- **Evidence:** Neither `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_EXTERNAL_STORAGE`, nor `MANAGE_EXTERNAL_STORAGE` are declared. All file exports (CSV, JSON) and imports use standard Android `FileProvider` (`androidx.core.content.FileProvider`) and system Storage Access Framework (SAF) pickers (`ActivityResultContracts.OpenDocument` / `CreateDocument`).

### 4. Accessibility API (`AccessibilityService`)
- **Status:** **NOT APPLICABLE**
- **Evidence:** FinPulse does not bind or declare an `AccessibilityService`.

### 5. Foreground Services (`FOREGROUND_SERVICE`)
- **Status:** **WORKMANAGER MERGED ONLY**
- **Evidence:** FinPulse does not declare custom long-running foreground services or notification channels requiring `FOREGROUND_SERVICE_*` types in its main manifest. Standard short periodic tasks use WorkManager workers (`RecurringCheckWorker`, `FinancialDigestWorker`, `ExchangeRateSyncWorker`, `SyncWorker`).

### 6. Exact Alarms (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`)
- **Status:** **NOT APPLICABLE**
- **Evidence:** Neither exact alarm permission is declared. Reminders and recurring tasks run via standard battery-efficient WorkManager scheduling.

### 7. Full-Screen Intents (`USE_FULL_SCREEN_INTENT`)
- **Status:** **NOT APPLICABLE**
- **Evidence:** Not requested. Notifications are posted as standard notifications via `NotificationManagerCompat`.

### 8. SMS & Call Log Permissions
- **Status:** **NOT APPLICABLE**
- **Evidence:** No SMS or call log permissions are declared.

### 9. Package Visibility (`QUERY_ALL_PACKAGES`)
- **Status:** **NOT APPLICABLE**
- **Evidence:** `QUERY_ALL_PACKAGES` is absent. Deep links are scoped strictly to `finpulse://`.

### 10. Subscriptions & In-App Billing
- **Status:** **NOT APPLICABLE**
- **Evidence:** Google Play Billing Library (`com.android.billingclient:billing`) is not bundled. The app is completely free with zero in-app purchases.

---

## 3. Account Deletion Requirement Action Items

Google Play requires that apps enabling account creation must offer:
1. **In-App Account & Data Deletion:**
   - **Implemented:** In FinPulse, navigating to `More > Cloud Synchronization` provides the button **"Delete Cloud Data & Account"**. When tapped, it deletes all documents in Firestore under `/users/{userId}` and terminates the session.
2. **Web-Based Account & Data Deletion Resource:**
   - **Manual Requirement:** The publisher must deploy a web page at the URL specified in Play Console (e.g., `https://finpulse.app/delete-account`).
   - The web page must specify:
     - The app name ("FinPulse").
     - The steps to request account and data deletion.
     - The types of data deleted (all synchronized financial ledgers, account balances, budgets, user profile metadata).
     - The retention period (deleted immediately from production Firestore; backups purged within 30 days).
