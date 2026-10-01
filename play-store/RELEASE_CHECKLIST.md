# Google Play Production Release Checklist for FinPulse

**Application ID:** `com.finpulse.app` | **Target SDK:** 36 | **Min SDK:** 26  
**Release Version:** `1.0.0` (`versionCode = 1`)  

---

## 1. Repository & Technical Build Readiness

| Status | Verification Item | Details / Verification Command |
| :---: | :--- | :--- |
| [x] | **Unit Tests Passing** | All unit tests pass cleanly: `./gradlew testDebugUnitTest` (`BUILD SUCCESSFUL`). |
| [x] | **Android Lint Clean** | All 14 prior lint errors resolved (`LocalDate` API 26 compatibility, string format escapes): `./gradlew lint` (`BUILD SUCCESSFUL`). |
| [x] | **Release AAB Generation** | Production release bundle compiles cleanly with R8 minification and resource shrinking enabled: `./gradlew bundleRelease` (`BUILD SUCCESSFUL`). |
| [x] | **Debuggable Disabled** | `isDebuggable = false` (default for release build type). `applicationIdSuffix = ".debug"` is excluded from release. |
| [x] | **Target SDK Compliant** | `targetSdk = 36` (Android 16), surpassing Google Play minimum target SDK requirement (API 34/35). |
| [x] | **Min SDK Configured** | `minSdk = 26` (Android 8.0 Oreo), providing modern Android coverage across ~98% of active devices. |
| [x] | **R8 / ProGuard Rules** | Optimized rules configured in `proguard-rules.pro` covering Kotlin Serialization, Room, Firebase, Credential Manager, and Biometric libraries. |
| [x] | **Network Security Config** | `network_security_config.xml` enforces `cleartextTrafficPermitted="false"`. |
| [x] | **Backup & Data Extraction Rules** | Configured `data_extraction_rules.xml` (API 31+) and `backup_rules.xml` (API 26-30). |
| [x] | **Git Hygiene & Secret Exclusion** | `.gitignore` verified to exclude `key.properties`, `*.jks`, `*.keystore`, `*.aab`, `*.apk`, `secrets.properties`, and local database files. |
| [ ] | **Production Signing Credentials** | **MANUAL ACTION:** Provide production keystore credentials in `key.properties` or CI/CD environment variables (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). |
| [ ] | **Production Firebase Config** | **MANUAL ACTION:** Replace template `app/google-services.json` with the production Firebase project configuration before launching cloud sync. |

---

## 2. Google Play Store Release Assets

| Status | Verification Item | Specification / File Location |
| :---: | :--- | :--- |
| [x] | **App Icon (512x512)** | `play-store/graphics/icon_512x512.png` (PNG, 32-bit, alpha channel, no pre-rounded corners). |
| [x] | **Adaptive Launcher Icons** | Configured in `app/src/main/res/mipmap-*` and `mipmap-anydpi-v26` (foreground, background, monochrome). |
| [x] | **Feature Graphic (1024x500)** | `play-store/graphics/feature_graphic_1024x500.png` (Exact 1024x500 px, high-resolution branding). |
| [x] | **Phone Screenshots (1080x1920)** | 5 high-resolution mockups in `play-store/screenshots/phone/`: <br>1. Dashboard & Cashflow<br>2. Transactions & Analytics<br>3. Google Cloud Sync<br>4. Offline & Biometric Lock<br>5. Budgets & Financial Goals |
| [x] | **Store Listing Copy (English)** | Prepared in `play-store/listing/`: `title.txt` (28 chars), `short_description.txt` (76 chars), `full_description.txt` (3,800 chars), `release_notes_v1.0.0.txt`. |
| [x] | **Categorization & Keywords** | Primary Category: Finance. Tags: Personal Finance, Expense Tracker, Budget Planner, Money Manager, Bill Reminder. |

---

## 3. Legal & Privacy Compliance

| Status | Verification Item | Details / Action Required |
| :---: | :--- | :--- |
| [x] | **Draft Privacy Policy** | Created in `play-store/PRIVACY_POLICY.md` strictly matching repository code and data flows. |
| [x] | **Draft Terms of Use** | Created in `TERMS_OF_USE.md` with standard financial disclaimer and limitation of liability. |
| [x] | **Data Safety Specification** | Documented in `play-store/DATA_SAFETY.md` covering all collected data types, purposes, encryption, and deletion. |
| [ ] | **Legal Review of Policies** | **MANUAL ACTION:** Publisher / legal counsel must review `PRIVACY_POLICY.md` and `TERMS_OF_USE.md` and fill in corporate/entity placeholders. |
| [ ] | **Host Public Privacy Policy** | **MANUAL ACTION:** Host the approved Privacy Policy at a publicly accessible HTTPS URL (e.g., `https://finpulse.app/privacy`). |
| [ ] | **Host Web Account Deletion Page** | **MANUAL ACTION:** Deploy a web deletion request page (e.g., `https://finpulse.app/delete-account`) satisfying Google Play policy. |

---

## 4. Google Play Console Setup & Declarations

| Status | Verification Item | Action in Play Console |
| :---: | :--- | :--- |
| [ ] | **Create Application Entry** | Console > Create app > FinPulse > Default language: English (United States) > Free. |
| [ ] | **App Category & Contact Info** | Category: Finance > Support Email: `[support@finpulse.app]` > Website: `[https://finpulse.app]`. |
| [ ] | **Set Privacy Policy URL** | Enter public URL in **Policy > App Content > Privacy Policy**. |
| [ ] | **Ads Declaration** | Select **"No, my app does not contain ads"**. |
| [ ] | **App Access** | Select **"All functionality is available without restrictions"** (or provide test Google credentials for sync). |
| [ ] | **Target Audience** | Select **18 and older** (Adults). |
| [ ] | **Content Rating Questionnaire** | Complete IARC questionnaire (select Finance/Utility; all violent/explicit content questions answered "No"). Expected: PEGI 3 / ESRB Everyone. |
| [ ] | **Financial Features Declaration** | Declare as personal finance/budgeting tool (non-banking, non-custodial). |
| [ ] | **Data Safety Form** | Fill out questionnaire following `play-store/DATA_SAFETY.md`. |
| [ ] | **Account Deletion URLs** | Input in-app instructions and web deletion URL (`https://finpulse.app/delete-account`). |
| [ ] | **Upload Store Assets** | Upload 512x512 icon, 1024x500 feature graphic, and phone screenshots from `play-store/`. |
| [ ] | **Upload Listing Text** | Copy Title, Short Description, Full Description from `play-store/listing/`. |

---

## 5. Release Tracks & Deployment Strategy

| Stage | Track | Recommended Procedure |
| :---: | :--- | :--- |
| **Stage 1** | **Internal Testing** | 1. Upload generated `app-release.aab`.<br>2. Invite team members (1-5 testers).<br>3. Verify installation, onboarding, Room database operations, biometric lock, and CSV import on real physical hardware. |
| **Stage 2** | **Closed Testing (20 Testers)** | 1. Google Play requires personal developer accounts to run closed testing with >= 20 opt-in testers for 14 continuous days.<br>2. Monitor Google Play Pre-Launch Report for crashes, ANRs, or accessibility warnings across diverse OEM test devices. |
| **Stage 3** | **Open Testing (Optional)** | Recommended if broader feedback on localized translations (Spanish, French, German, Portuguese, etc.) is desired. |
| **Stage 4** | **Production Staged Rollout** | 1. Release to **10%** of users on Day 1.<br>2. Monitor Android Vitals (Crash Rate < 1.09%, ANR Rate < 0.47%).<br>3. Increase rollout: **25%** (Day 3), **50%** (Day 5), **100%** (Day 7) if vitals remain stable. |
| **Stage 5** | **Post-Launch Monitoring** | Monitor Google Play Console > Quality > Android Vitals for crash spikes, ANR spikes, or unexpected background battery drain. |
