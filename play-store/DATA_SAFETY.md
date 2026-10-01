# Google Play Data Safety Specification for FinPulse

**Application ID:** `com.finpulse.app`  
**Target SDK:** 36 (Android 16) | **Min SDK:** 26 (Android 8.0)  
**Document Purpose:** Exact data safety questionnaire declaration answers for Google Play Console submission.

---

## 1. High-Level Data Practice Declarations

In the Google Play Console **App Content > Data Safety** questionnaire, provide the following responses:

| Play Console Question | Answer | Rationale & Code Verification |
| :--- | :---: | :--- |
| **Does your app collect or share any of the required user data types?** | **Yes** | App collects user profile data upon optional Google Sign-In and user-entered financial logs for cloud sync. |
| **Is all of the user data collected by your app encrypted in transit?** | **Yes** | Verified: All network traffic to Firebase Auth, Google Cloud Firestore, and public exchange rate APIs uses TLS 1.3 / HTTPS. `network_security_config.xml` enforces `cleartextTrafficPermitted="false"`. |
| **Do you provide a way for users to request that their data be deleted?** | **Yes** | In-app "Delete Cloud Data & Account" feature in `SecurityScreen.kt` / `CloudSyncManager.kt` and web request path at `https://finpulse.app/delete-account`. |
| **Does your app allow users to create an account?** | **Yes** | Users can sign in using their Google Account via Android Credential Manager / Firebase Authentication. |
| **Can users request account deletion from within the app and via a web URL?** | **Yes** | In-app deletion button purges Firestore `/users/{uid}` path; publisher must host the web deletion form at the provided URL. |

---

## 2. Comprehensive Data Safety Declaration Table

The following table details every data type relevant to FinPulse and its Google/Firebase SDK dependencies:

| Data Type | Collected? | Shared? | Purpose | Required / Optional | Encrypted in Transit? | User Deletion? | Technical Source in Repository |
| :--- | :---: | :---: | :--- | :---: | :---: | :---: | :--- |
| **Personal Info > Name** | **Yes** | **No** | • App functionality<br>• Account management | **Optional** (only when user signs in with Google) | Yes (TLS 1.3) | Yes | Retrieved from `GoogleIdTokenCredential` / Firebase Auth and stored in `UserPreferencesDataStore` to render profile header. |
| **Personal Info > Email Address** | **Yes** | **No** | • App functionality<br>• Account management | **Optional** (only when user signs in with Google) | Yes (TLS 1.3) | Yes | Retrieved from `GoogleIdTokenCredential` / Firebase Auth; used as account identifier. |
| **Personal Info > User IDs** | **Yes** | **No** | • Account management | **Optional** (only when user signs in with Google) | Yes (TLS 1.3) | Yes | Firebase Auth pseudonymous User ID (`uid`); serves as Firestore root document key `/users/{uid}` for security rule isolation. |
| **Financial Info > Other Financial Info** *(Transactions, Balances, Budgets, Debt, Goals)* | **Yes** | **No** | • App functionality<br>• Account management | **Optional** (Cloud Sync) / **Required** (Local App Usage) | Yes (TLS 1.3) | Yes | Room Database (`finpulse.db`). Transmitted to Firestore `/users/{uid}` only if cloud sync is activated by user. |
| **App info and performance > App Preferences &amp; Settings** *(Theme, Currency, Language, UI &amp; Notification Preferences)* | **Yes** | **No** | • App functionality<br>• Account management | **Optional** (only when signed into Google) | Yes (TLS 1.3) | Yes | DataStore Preferences (`user_preferences`). Synchronized to Firestore under `/users/{uid}/settings/app` to restore settings across devices. |
| **Photos and Videos** | **No** | **No** | N/A | N/A | N/A | N/A | User profile photo URL from Google Auth is loaded directly into image painter in memory; no photo library scanning or uploads. |

| **Biometrics** | **No** | **No** | N/A | N/A | N/A | N/A | Fingerprint/Face authentication uses Android OS `BiometricPrompt`. App never accesses raw biometric data; Google Play policy states OS biometrics are **not** collected. |
| **Location (Precise or Coarse)** | **No** | **No** | N/A | N/A | N/A | N/A | Zero location permissions in manifest (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` absent). |
| **Contacts** | **No** | **No** | N/A | N/A | N/A | N/A | Zero contact permissions (`READ_CONTACTS` absent). |
| **Messages / SMS** | **No** | **No** | N/A | N/A | N/A | N/A | Zero SMS permissions (`READ_SMS` / `RECEIVE_SMS` absent). |
| **Device or Other IDs** *(Advertising ID, IMEI, MAC)* | **No** | **No** | N/A | N/A | N/A | N/A | App does not collect Google Advertising ID (AAID), hardware serial numbers, or MAC addresses. |
| **App Activity** *(Interactions, Search History)* | **No** | **No** | N/A | N/A | N/A | N/A | In-app search queries are executed purely in-memory / local Room DB. No analytics trackers installed. |
| **App Diagnostics** *(Crash Logs, Performance)* | **No** | **No** | N/A | N/A | N/A | N/A | App does not bundle Firebase Crashlytics or external performance monitoring SDKs. |

---

## 3. Proven Code/Config Findings vs External Verification Requirements

### Proven from Repository Code:
1. **Zero Advertising SDKs:** Neither Google AdMob, Unity Ads, AppLovin, nor ironSource exist in `libs.versions.toml` or `app/build.gradle.kts`.
2. **Zero Analytics SDKs:** Neither Firebase Analytics, Google Analytics, nor third-party trackers are integrated.
3. **Strict Firestore Rule Isolation:** `firestore.rules` enforces `request.auth.uid == userId` for all read, write, and delete operations under `/users/{userId}`.
4. **Local Data Processing:** CSV parsing, categorization rules matching, duplicate detection, and debt payoff calculations run entirely on the device CPU with zero external API calls.
5. **No Dangerous Runtime Permissions:** Manifest requests only `INTERNET`, `ACCESS_NETWORK_STATE`, `USE_BIOMETRIC`, and `POST_NOTIFICATIONS`.

### External Verification Required by Publisher:
1. **Firebase Console Rules Deployment:** Verify that `firestore.rules` from the repository has been deployed to the production Firebase project.
2. **Web Deletion URL Availability:** Deploy a web deletion page (e.g., `https://finpulse.app/delete-account`) to satisfy Google Play's mandatory account and data deletion requirement.
3. **Public Privacy Policy URL:** Host `play-store/PRIVACY_POLICY.md` at a publicly accessible, crawlable HTTPS URL (e.g., `https://finpulse.app/privacy`).
