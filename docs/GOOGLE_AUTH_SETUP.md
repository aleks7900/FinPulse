# Google Account & Cloud Sync Setup Guide for Fin Pulse

This guide covers the manual configuration steps in the **Firebase Console** and **Google Cloud Console** required to enable Google Account Authentication (via Android Credential Manager) and Firestore Cloud Synchronization in Fin Pulse.

---

## 1. Firebase Project Setup

1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** (or select an existing project).
3. Name your project (e.g. `FinPulse-Production` or `FinPulse-Dev`).
4. (Optional) Enable Google Analytics for the project, then click **Create project**.

---

## 2. Register Android Application

1. In project overview, click the **Android** icon (or go to **Project settings** > **General** > **Your apps** > **Add app**).
2. Enter the Package Name:
   - **Android package name**: `com.finpulse.app`
   - **App nickname**: `Fin Pulse`
3. Retrieve your Android debug signing certificate fingerprints (SHA-1 and SHA-256):
   - Run the Gradle signing report in the terminal:
     ```bash
     ./gradlew signingReport
     ```
   - Look under `Variant: debug` -> `Config: debug` -> `Store: ~/.android/debug.keystore`.
   - Copy the `SHA1` (e.g. `AA:BB:CC:...`) and `SHA-256` fingerprints.
4. Paste the **SHA-1** and **SHA-256** into the Firebase app registration dialog.
   *(Note: Android Credential Manager and Google ID require the correct SHA-1/SHA-256 fingerprints to authorize the client token).*
5. Click **Register app**.

---

## 3. Download and Place `google-services.json`

1. Download the generated `google-services.json` from the Firebase Console.
2. Place the file in the `app/` directory of the Fin Pulse project:
   ```
   FinPulse/
   ├── app/
   │   ├── google-services.json   <-- Place file here
   │   ├── build.gradle.kts
   │   └── src/
   ```
3. A dummy placeholder `google-services.json` is included in the project for compilation. Replace it with your actual downloaded file.
4. *Important: Never commit production API keys or service account credentials with administrative privileges. Ensure `google-services.json` is tracked according to your team's security policy.*

---

## 4. Enable Google Sign-In in Firebase Authentication

1. In the Firebase Console left menu, navigate to **Build** > **Authentication**.
2. Click **Get started** if not already enabled.
3. Under the **Sign-in method** tab, click **Google**.
4. Toggle **Enable**.
5. Set the **Project support email** (select your developer/admin email).
6. Click **Save**.
7. Note the **Web SDK configuration**:
   - Firebase automatically creates a **Web client ID** (format: `1234567890-abcdefg.apps.googleusercontent.com`).
   - If using a custom web client ID in your build config, configure `finpulse_web_client_id` in your string resources or `local.properties`.

---

## 5. Enable Cloud Firestore Database

1. In the Firebase Console left menu, navigate to **Build** > **Firestore Database**.
2. Click **Create database**.
3. Choose a database location close to your primary user base (e.g. `us-central1` or `europe-west1`).
4. Choose **Start in production mode** (this ensures all paths are protected by default).
5. Click **Create**.

---

## 6. Deploy Firestore Security Rules

1. In the Firebase Console, go to **Firestore Database** > **Rules** tab.
2. Copy the contents of [`firestore.rules`](../firestore.rules) located at the root of the Fin Pulse repository.
3. Paste the contents into the editor.
4. Verify that:
   - Root collections default to `allow read, write: if false;`.
   - Paths matching `/users/{userId}/**` enforce `request.auth.uid == userId`.
5. Click **Publish**.

Alternatively, you can deploy using the Firebase CLI:
```bash
npm install -g firebase-tools
firebase login
firebase init firestore
firebase deploy --only firestore:rules
```

---

## 7. Verifying Authentication on Emulator / Real Device

1. **Google Play Services**: Ensure the target Android device or emulator has **Google Play Services** installed and an active Google Account added in **Device Settings** > **Passwords & Accounts**.
2. **Build and Run**:
   ```bash
   ./gradlew installDebug
   ```
3. Open Fin Pulse -> Navigate to **More** (`...`) -> **Google Account & Cloud Sync**.
4. Tap **Continue with Google**.
5. Select your Google account in the Credential Manager bottom sheet.
6. Verify:
   - The user profile loads with avatar, display name, and email.
   - Status transitions from *Syncing...* to *Synced*.
   - In Firebase Console > **Authentication** > **Users**, your authenticated UID appears.
   - In Firebase Console > **Firestore Database**, a new document under `/users/{uid}/...` is created with your financial data.

---

## 8. Troubleshooting Common Issues

| Issue | Cause | Resolution |
|-------|-------|------------|
| `DeveloperException` / `10: Developer error` | Mismatched SHA-1 fingerprint or Package Name | Double-check that your debug/release SHA-1 fingerprint in Firebase matches the keystore signing your APK. |
| `No credential available` | No Google account logged into device | Add a Google account in the Android system settings. |
| `PERMISSION_DENIED` on Firestore write | Security rules mismatch or user unauthenticated | Confirm `request.auth.uid` matches the `{userId}` path parameter and that rules are published. |
| Offline writes not syncing | Waiting for network constraint | Reconnect to Wi-Fi/cellular; WorkManager or manual "Sync Now" will immediately flush the local queue. |
