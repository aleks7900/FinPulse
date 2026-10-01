# Privacy Policy for Fin Pulse

**Last Updated:** September 27, 2026  
**Effective Date:** September 27, 2026

Fin Pulse ("we", "our", or "the App") is committed to protecting your privacy and personal financial information. This Privacy Policy explains how Fin Pulse handles, processes, stores, and protects your information when you use our Android application.

Please read this Privacy Policy carefully. By downloading, installing, or using Fin Pulse, you acknowledge and agree to the practices described in this policy.

---

## 1. Core Principles & Privacy Philosophy

Fin Pulse is designed with a **privacy-first and offline-first philosophy**:
- **Offline-First:** All financial calculations, transaction records, accounts, and budgets are stored locally on your device by default. You can use Fin Pulse completely offline without creating an account or providing any personal information.
- **Zero Advertising or Data Selling:** We do not display third-party advertisements, we do not track you across apps or websites, and we never sell, rent, or monetize your personal or financial data to third parties.
- **User Control:** Cloud synchronization is completely optional. If you choose to enable Google Account synchronization, your data is isolated strictly to your authenticated account.

---

## 2. Information We Collect and Process

### A. Information Stored Locally on Your Device
When you use Fin Pulse, the following data is created and saved strictly within your device's private application sandbox (using Android Room SQLite):
- **Financial Transactions:** Transaction amounts, timestamps, transaction types (income, expense, transfer), descriptions, notes, and tags.
- **Accounts & Wallets:** Account names, account types (checking, savings, credit, cash, investment), currency codes, and balances.
- **Categories & Budgets:** Custom and preset categories, budget limits, budget periods, and budget progress.
- **Recurring Transactions & Goals:** Recurring bill reminders, frequencies, savings goal targets, and deadlines.
- **Application Preferences:** Theme preferences (dark/light mode), default currency, privacy mode toggle (masking balances), and lock timeout settings.

### B. Information Processed via Optional Google Sign-In & Cloud Sync
If you explicitly choose to sign in with your Google Account to back up or synchronize your data across devices, we process:
- **Google Account Identifier (UID):** A unique, pseudonymous identifier provided by Firebase Authentication.
- **Profile Information:** Your Google display name, email address, and profile photo URL (used exclusively to display your active account profile in the app's settings).
- **Synchronized Financial Records:** An encrypted copy of your accounts, transactions, categories, budgets, and settings is transmitted securely via HTTPS/TLS to Google Cloud Firestore under your isolated user path (`/users/{user_id}/`).

### C. Biometric Data & Device Security
Fin Pulse supports biometric authentication (Fingerprint, Face Unlock) and device credentials (PIN/Pattern/Password) to prevent unauthorized access to your records on your phone.
- **Important:** Fin Pulse does **not** collect, store, or have access to your biometric samples (such as fingerprints or facial geometry). Biometric verification is handled entirely by the Android operating system's native `BiometricPrompt` framework and hardware-backed Android Keystore. Fin Pulse only receives a cryptographic success/failure callback from Android OS.

---

## 3. How We Use Your Information

We use the information collected solely to provide, operate, and maintain the functionality of Fin Pulse:
- To calculate your net worth, account balances, and cash flow analytics.
- To display category spending charts, budget progress, and upcoming bill reminders.
- To synchronize your financial records between multiple Android devices when signed into the same Google Account.
- To resolve data conflicts deterministically (using last-write-wins timestamps) during cloud synchronization.
- To protect your financial records through app lock and privacy masking.

We **do not**:
- Use your financial data for advertising, profiling, or behavioral analytics.
- Share your financial data with credit bureaus, financial institutions, or marketing agencies.
- Train machine learning models on your private financial records.

---

## 4. Third-Party Services & Infrastructure

Fin Pulse uses trusted industry-standard infrastructure provided by Google LLC:
- **Firebase Authentication & Android Credential Manager:** Securely authenticates your Google Account without exposing your Google account password to Fin Pulse.
- **Cloud Firestore (Google Cloud Platform):** Secure, encrypted cloud NoSQL database used to host your optional synchronized financial records.
- **Google Play Services:** Used for app updates, in-app reviews, and system library integration.

All communications between the App and Google Cloud infrastructure are encrypted in transit using industry-standard Transport Layer Security (TLS 1.3 / HTTPS).

---

## 5. Data Security & Storage

We implement rigorous technical safeguards to secure your data:
- **Local Sandbox Storage:** Your database is kept in Android's protected internal app storage (`/data/data/md.alexlab.finpulse/`), inaccessible to other applications installed on your device without root access.
- **Android Keystore:** Cryptographic keys and sensitive tokens are stored using the hardware-backed Android Keystore system.
- **Cloud Security Rules:** Firestore database rules enforce that data located at `/users/{userId}/` can **only** be read or written by the authenticated user whose `request.auth.uid == userId`. No other user or unauthorized third party can read your data.
- **Screenshot Protection:** When enabled, the App activates Android's `FLAG_SECURE` window attribute, preventing screenshots and hiding app contents in the Android recent apps task switcher.

---

## 6. Data Retention and Deletion Rights

You have complete control over your data at all times:
- **Local Data Deletion:** You can reset or delete all local data at any time by clearing application data in Android System Settings (`Settings > Apps > Fin Pulse > Storage > Clear Data`) or by uninstalling the application.
- **Cloud Data Deletion:** If you have enabled Google Cloud Sync and wish to delete your synchronized cloud data or account records, you can do so directly from within the app settings by tapping "Delete Cloud Data & Account" or by contacting us at the support address below. Upon request, all Firestore documents under your user ID will be permanently purged within 30 days.

---

## 7. Children's Privacy

Fin Pulse is not directed at children under the age of 13 (or under 16 in the European Economic Area). We do not knowingly collect personal identifiable information or financial records from children. If you become aware that a child has provided us with personal information, please contact us immediately, and we will take steps to delete such data.

---

## 8. International Data Transfers (GDPR & CCPA Notice)

- **European Economic Area (EEA) & UK Users:** Under the General Data Protection Regulation (GDPR), you have the right to access, rectify, export, or erase your personal data, as well as the right to restrict processing. If cloud sync is enabled, your data is processed by Google Cloud in accordance with standard contractual clauses (SCCs) and GDPR compliance frameworks.
- **California Residents (CCPA/CPRA):** We do not sell your personal information. You have the right to request disclosure of categories of personal information collected, request deletion, and not face discrimination for exercising your privacy rights.

---

## 9. Changes to This Privacy Policy

We may update our Privacy Policy periodically to reflect changes in our practices or applicable laws. When changes are made, the "Last Updated" date at the top of this document will be updated. We encourage you to review this Privacy Policy periodically.

---

## 10. Contact Us

If you have questions, feedback, or data deletion requests regarding this Privacy Policy, please contact us:

- **Email:** privacy@finpulse.app
- **Developer:** Fin Pulse Team
- **Website:** https://finpulse.app
