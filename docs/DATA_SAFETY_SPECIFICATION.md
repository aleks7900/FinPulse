# Google Play Data Safety Specification for Fin Pulse

This document provides exact, verified answers for completing the **Data safety** questionnaire in the **Google Play Console** for **Fin Pulse** (`md.alexlab.finpulse`).

---

## 1. Data Collection and Security Overview

| Play Console Question | Answer | Details / Justification |
| :--- | :--- | :--- |
| **Does your app collect or share any of the required user data types?** | **Yes** | App collects user profile data upon Google Sign-In and user-entered financial logs for cloud sync. |
| **Is all of the user data collected by your app encrypted in transit?** | **Yes** | All network traffic to Firebase Authentication and Google Cloud Firestore uses TLS 1.3 / HTTPS. |
| **Do you provide a way for users to request that their data be deleted?** | **Yes** | Users can delete local data in app settings or request permanent cloud data deletion via in-app button or support email. |
| **Does your app allow users to create an account?** | **Yes** | Users can sign in using their existing Google Account via Android Credential Manager / Firebase Auth. |
| **Can users request account deletion from within the app and via a web URL?** | **Yes** | Dedicated "Delete Account & Cloud Data" feature in settings, plus a web deletion request form at `https://finpulse.app/delete-account`. |

---

## 2. Data Types Collected and Handled

### Category: Personal Info

#### 1. Name
- **Collected?** Yes (Only when user explicitly signs in with Google)
- **Shared?** No (Never shared with third parties)
- **Processed ephemerally?** No (Stored in Firestore user profile to display in account screen)
- **Is collection optional?** Yes (Users can use Fin Pulse completely anonymously offline)
- **Data purposes:**
  - App functionality (Displays user's Google display name on the profile header)
  - Account management

#### 2. Email Address
- **Collected?** Yes (Only when user explicitly signs in with Google)
- **Shared?** No
- **Processed ephemerally?** No
- **Is collection optional?** Yes
- **Data purposes:**
  - App functionality
  - Account management (Unique user identifier for multi-device sync)

#### 3. User IDs (Firebase UID)
- **Collected?** Yes
- **Shared?** No
- **Processed ephemerally?** No
- **Is collection optional?** Yes
- **Data purposes:**
  - Account management (Used as root document key `/users/{userId}` for cloud security isolation)

---

### Category: Financial Info

#### 1. Other Financial Info (Transaction Records, Balances, Budgets)
- **Collected?** Yes (User-entered financial tracking data)
- **Shared?** No (Never shared with advertisers, credit bureaus, or third parties)
- **Processed ephemerally?** No (Stored locally in Room DB and synchronized to user's private Firestore account if signed in)
- **Is collection optional?** No (Core app functionality requires logging expenses/income, though entering dummy data or using the app offline is fully supported)
- **Data purposes:**
  - App functionality (Tracking personal income, expenses, category budgets, net worth)
  - Account management (Synchronizing data across user's devices)

---

### Category: App info and performance

#### 1. Crash logs
- **Collected?** Yes (Via Firebase Crashlytics)
- **Shared?** No (Transmitted solely to developer's private Firebase Crashlytics project)
- **Processed ephemerally?** No (Stored in Firebase Crashlytics for 90 days per retention policy)
- **Is collection optional?** No (Automatically enabled for release builds to diagnose critical app failures)
- **Data purposes:**
  - Analytics (Crash rate metrics)
  - App functionality (Diagnosing and resolving unexpected crashes and ANRs)

#### 2. Diagnostics
- **Collected?** Yes (Non-fatal technical errors and diagnostic breadcrumbs)
- **Shared?** No
- **Processed ephemerally?** No
- **Is collection optional?** No
- **Data purposes:**
  - Analytics
  - App functionality (Diagnosing cloud synchronization and storage errors)

---

### Category: Device or Other IDs
- **Collected?** Yes (Firebase Installation ID / App Instance ID generated automatically by Firebase SDK for diagnostic routing; no hardware IMEI or Advertising ID is collected)
- **Shared?** No
- **Processed ephemerally?** No
- **Purposes:** Analytics, App functionality

---

### Category: Biometrics
- **Collected?** **No**
- **Explanation:** Although Fin Pulse offers Fingerprint and Face Unlock via Android's `BiometricPrompt`, the biometric authentication is handled entirely by the Android operating system and Android Keystore. Fin Pulse receives only a boolean cryptographic result (`onAuthenticationSucceeded`) and has zero access to raw biometric data. Google Play policy dictates this is **not** considered data collection.

---

## 3. Data Safety Summary Table for Play Console Entry

| Data Type | Collected? | Shared? | Encrypted in Transit? | Ephemeral? | Required or Optional? | Purposes |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **Personal Info > Name** | Yes | No | Yes | No | Optional | App functionality, Account management |
| **Personal Info > Email Address** | Yes | No | Yes | No | Optional | App functionality, Account management |
| **Personal Info > User IDs** | Yes | No | Yes | No | Optional | Account management |
| **Financial Info > Other Financial Info** | Yes | No | Yes | No | Optional (Sync) / Required (Local) | App functionality, Account management |
| **App info and performance > Crash logs** | Yes | No | Yes | No | Required | Analytics, App functionality |
| **App info and performance > Diagnostics** | Yes | No | Yes | No | Required | Analytics, App functionality |
| **Device or other IDs > Device or other IDs** | Yes | No | Yes | No | Required | Analytics, App functionality |

---

## 4. Privacy Policy & Account Deletion URLs for Play Console

- **Privacy Policy URL:** `https://finpulse.app/privacy` (Point to hosted `docs/PRIVACY_POLICY.md`)
- **Account & Data Deletion URL:** `https://finpulse.app/delete-account`
  - *Content requirement for Google Play:* The URL must clearly explain how users can request complete deletion of their account and all associated financial records.
