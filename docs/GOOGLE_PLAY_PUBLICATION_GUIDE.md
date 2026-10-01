# Google Play Publication Guide for Fin Pulse

This guide provides a complete, step-by-step walkthrough for preparing, signing, building, and publishing **Fin Pulse** (`md.alexlab.finpulse`) to the **Google Play Store**.

---

## Table of Contents
1. [Prerequisites & Account Setup](#1-prerequisites--account-setup)
2. [Release Signing Setup](#2-release-signing-setup)
3. [Building the Android App Bundle (AAB)](#3-building-the-android-app-bundle-aab)
4. [Firebase & Google Sign-In Fingerprint Configuration](#4-firebase--google-sign-in-fingerprint-configuration)
5. [Play Console Store Listing Setup](#5-play-console-store-listing-setup)
6. [Policy Declarations & Data Safety](#6-policy-declarations--data-safety)
7. [The 20-Tester / 14-Day Closed Testing Requirement](#7-the-20-tester--14-day-closed-testing-requirement)
8. [Production Release & Staged Rollout](#8-production-release--staged-rollout)

---

## 1. Prerequisites & Account Setup

1. **Google Play Developer Account:**
   - Sign up at [play.google.com/console](https://play.google.com/console).
   - Pay the one-time $25 registration fee.
   - Complete identity verification (Government ID, proof of address).
2. **Development Environment:**
   - Android SDK 34 / 35 installed.
   - JDK 17 configured (`java -version`).
   - PowerShell 5.1+ (Windows).

---

## 2. Release Signing Setup

Google Play mandates that release builds are signed with a secure upload key. Google Play App Signing then re-signs your app with your production key before distribution.

### Step 2.1: Generate Your Release Keystore
Run the automated keystore generator script from the project root:
```powershell
powershell -ExecutionPolicy Bypass -File "scripts/generate_release_keystore.ps1"
```
This script:
1. Generates an RSA 2048-bit keystore at `keystore/finpulse-release.keystore` (valid for 30 years).
2. Creates `key.properties` with your passwords and key alias.
3. Automatically computes and outputs your **SHA-1** and **SHA-256** fingerprints.

> [!WARNING]
> **Backup your keystore immediately!** Store `keystore/finpulse-release.keystore` and `key.properties` in a secure password manager or encrypted cloud backup. If you lose this key, you will not be able to push updates to the Play Store.

### Step 2.2: Verify key.properties
Ensure `key.properties` is located at the project root and contains:
```properties
storeFile=../keystore/finpulse-release.keystore
storePassword=YOUR_SECURE_PASSWORD
keyAlias=finpulse
keyPassword=YOUR_SECURE_PASSWORD
```
*(Note: `key.properties` and `*.keystore` are already excluded from Git in `.gitignore`)*.

---

## 3. Building the Android App Bundle (AAB)

Google Play requires the **Android App Bundle (.aab)** format for all new applications.

### Step 3.1: Build Release Bundle
Run the Gradle bundle task:
```powershell
.\gradlew.bat clean bundleRelease
```

### Step 3.2: Locate Output Bundle
Upon successful compilation and R8 shrinking:
- **Bundle File:** `app\build\outputs\bundle\release\app-release.aab`
- **Mapping File (for de-obfuscation):** `app\build\outputs\mapping\release\mapping.txt`

> [!NOTE]
> The Gradle build automatically enables R8 code shrinking (`isMinifyEnabled = true`) and resource stripping (`isShrinkResources = true`). Our custom rules in `app/proguard-rules.pro` preserve Room DAOs/Entities, Kotlinx Serialization, and Firebase Auth/Credentials without runtime crashes.

---

## 4. Firebase & Google Sign-In Fingerprint Configuration

For Google Sign-In and Cloud Sync to function in your production release build, Firebase must recognize your release signing certificates:

1. Retrieve the SHA-1 and SHA-256 fingerprints of your keystore:
   ```powershell
   keytool -list -v -keystore "keystore/finpulse-release.keystore" -alias finpulse
   ```
2. Open the [Firebase Console](https://console.firebase.google.com/) -> Select your FinPulse project.
3. Navigate to **Project Settings > General > Your Apps > md.alexlab.finpulse**.
4. Click **Add fingerprint**:
   - Paste your **SHA-1** fingerprint.
   - Paste your **SHA-256** fingerprint.
5. **After uploading your first AAB to Google Play Console:**
   - In Google Play Console, go to **Release > Setup > App integrity > App Signing**.
   - Copy the **Google Play App Signing key SHA-1 and SHA-256** fingerprints.
   - Add these Google Play App Signing fingerprints to your Firebase Console as well! This ensures that app bundles downloaded by users from the Play Store can successfully authenticate with Google.
6. Re-download `google-services.json` from Firebase and place it into `app/google-services.json` if client IDs or certificates changed.

---

## 5. Play Console Store Listing Setup

Navigate to **Grow > Store presence > Main store listing** in the Play Console:

### Step 5.1: Listing Details
Copy the pre-written metadata from `distribution/play_store/listing/`:
- **App Name (Title):** `Fin Pulse: Expense & Budget` (from `title.txt`)
- **Short Description:** `Smart expense tracker, offline budget planner & seamless Google cloud sync.` (from `short_description.txt`)
- **Full Description:** Paste content from `full_description.txt`.

### Step 5.2: Graphics & Screenshots
Upload the pre-rendered high-resolution assets:
1. **App Icon:**
   - File: `distribution/play_store/graphics/icon_512x512.png` (512x512 PNG, 32-bit color).
2. **Feature Graphic:**
   - File: `distribution/play_store/graphics/feature_graphic_1024x500.png` (1024x500 PNG, no alpha).
3. **Phone Screenshots (Upload in order):**
   - 1: `distribution/play_store/screenshots/phone/01_dashboard_and_cashflow.png`
   - 2: `distribution/play_store/screenshots/phone/02_transactions_and_analytics.png`
   - 3: `distribution/play_store/screenshots/phone/03_google_cloud_sync.png`
   - 4: `distribution/play_store/screenshots/phone/04_offline_and_biometric_lock.png`
   - 5: `distribution/play_store/screenshots/phone/05_budgets_and_financial_goals.png`

---

## 6. Policy Declarations & Data Safety

Navigate to **Policy and programs > App content** in Play Console:

1. **Privacy Policy:**
   - Enter your public URL (e.g., `https://finpulse.app/privacy` or your GitHub Pages URL hosting `docs/PRIVACY_POLICY.md`).
2. **Data Safety Questionnaire:**
   - Follow the exact field-by-field instructions in [`docs/DATA_SAFETY_SPECIFICATION.md`](file:///c:/Users/aleks/.gemini/antigravity-ide/scratch/FinPulse/docs/DATA_SAFETY_SPECIFICATION.md).
3. **Target Audience and Content:**
   - Target age: 18 and older.
   - Not designed for children.
4. **Financial Features Declaration:**
   - Declare feature: *Personal financial management / Expense tracking*.
   - Does the app provide banking or lending services? No.
5. **Content Rating (IARC):**
   - Fill out the questionnaire: Select "Utility, Productivity, Communication, or other".
   - Fin Pulse contains no violence, sexual content, profanity, or gambling.
   - Result: Everyone (PEGI 3, ESRB Everyone).
6. **Advertising & IDs:**
   - Does your app contain ads? **No**.

---

## 7. The 20-Tester / 14-Day Closed Testing Requirement

> [!IMPORTANT]
> For all personal Google Play developer accounts created after November 13, 2023, Google requires a **closed test with at least 20 testers opted-in for at least 14 days continuously** before applying for production access.

### Checklist to Pass Closed Testing:
1. **Create Closed Testing Track:**
   - In Play Console, go to **Release > Testing > Closed testing**.
   - Create a track (e.g., "Closed Alpha / Beta").
2. **Create a Tester List:**
   - Create an email list or Google Group containing at least 20 individual tester email addresses (friends, colleagues, or testing community members).
3. **Upload the Release Bundle:**
   - Create a new release in the closed testing track.
   - Upload `app/build/outputs/bundle/release/app-release.aab`.
   - Add release notes from `distribution/play_store/listing/release_notes_v1.0.0.txt`.
   - Submit the release for review.
4. **Distribute Opt-in Link:**
   - Once approved by Google review, copy the "Join on the web" or "Join on Android" link.
   - Send the link to all 20 testers.
   - **Crucial:** Ensure all 20 testers click "Become a tester", install the app from Google Play, and keep it installed for 14 consecutive days.
5. **Apply for Production:**
   - After 14 days with 20 active opted-in testers, a button labeled **"Apply for production"** will unlock on your Play Console Dashboard.
   - Answer the short questionnaire describing your testing feedback and fixes, then submit for production review.

---

## 8. Production Release & Staged Rollout

Once production access is approved:
1. Go to **Release > Production > Create new release**.
2. Select your validated bundle from the App Bundle Explorer or upload a new incremented version.
3. Paste release notes.
4. **Choose Staged Rollout (Recommended):**
   - Day 1: Roll out to **10%** of users.
   - Day 2-3: Monitor Crash Rate and ANR (Application Not Responding) rate in **Quality > Android Vitals**.
   - Day 4: Increase rollout to **50%**.
   - Day 7: Increase rollout to **100%**.

---

## Quick Reference Commands

| Action | Command |
| :--- | :--- |
| **Generate Keystore** | `powershell -ExecutionPolicy Bypass -File "scripts/generate_release_keystore.ps1"` |
| **Generate Graphics** | `powershell -ExecutionPolicy Bypass -File "scripts/generate_play_store_assets.ps1"` |
| **Generate Screenshots** | `powershell -ExecutionPolicy Bypass -File "scripts/generate_screenshots.ps1"` |
| **Build Release AAB** | `.\gradlew.bat bundleRelease` |
| **Build Release APK** | `.\gradlew.bat assembleRelease` |
| **Extract Keystore SHA-1** | `keytool -list -v -keystore "keystore/finpulse-release.keystore" -alias finpulse` |
