# Terms of Use for FinPulse

**Document Version:** 1.0.0  
**Effective Date:** [INSERT EFFECTIVE DATE, e.g., October 1, 2026]  
**Publisher / Operator:** [INSERT LEGAL ENTITY / DEVELOPER NAME]  
**Contact:** [INSERT SUPPORT EMAIL, e.g., support@finpulse.app]  

---

> [!IMPORTANT]
> **LEGAL REVIEW REQUIRED:** This document is a technical and operational draft prepared based on the FinPulse application codebase (`md.alexlab.finpulse`). It must be reviewed, adapted, and approved by qualified legal counsel prior to commercial distribution or formal publication on the Google Play Store.

---

## 1. Acceptance of Terms

By downloading, installing, accessing, or using FinPulse ("the Application", "Service", "App"), you agree to be bound by these Terms of Use ("Terms"). If you do not agree to these Terms, you must not download, install, or use the Application.

These Terms constitute a legally binding agreement between you ("User", "you") and [INSERT LEGAL ENTITY / DEVELOPER NAME] ("Company", "we", "us", or "our").

---

## 2. Description of Service

FinPulse is an offline-first personal financial management application designed for Android devices. The Service offers:
- Multi-account portfolio tracking (cash, checking, savings, credit cards, investments).
- Transaction recording, categorization, and double-entry consistency balance management.
- Category-based and overall monthly budgeting with pacing calculations and progress metrics.
- Cash flow analytics, donut charts, merchant breakdowns, and financial insight evaluations.
- Recurring subscription and payment tracking with annualized projections.
- Financial goals tracking and debt payoff simulations (Debt Snowball and Debt Avalanche models).
- On-device CSV statement importing, rule-based categorization, and duplicate transaction detection.
- Biometric (fingerprint/face) authentication and PIN lock security with window privacy (`FLAG_SECURE`).
- Optional Google Account synchronization powered by Google Firebase Authentication and Google Cloud Firestore.

---

## 3. No Financial, Tax, Investment, or Legal Advice

**FINPULSE IS A FINANCIAL RECORD-KEEPING AND INFORMATIONAL UTILITY ONLY.**

1. **No Fiduciary Duty:** The Application and its developers do not act as financial advisors, brokers, certified public accountants (CPAs), or tax professionals.
2. **Deterministic Mathematical Estimates:** Projections, debt freedom schedules, budget pacing, safe-to-spend estimations, and charts are generated deterministically based strictly on user-entered values and simplified mathematical formulas.
3. **User Discretion:** You are solely responsible for verifying the accuracy of transaction entries, currency rates, account balances, and budget figures. You should consult a licensed financial advisor or accountant prior to making significant financial commitments or investment decisions.

---

## 4. Permitted Use and License

1. **Grant of License:** We grant you a personal, revocable, non-exclusive, non-transferable, non-sublicensable limited license to install and run the Application on Android devices that you own or control, solely for your personal, non-commercial financial tracking purposes.
2. **Restrictions:** You shall not:
   - Decompile, reverse engineer, disassemble, or attempt to derive the source code of the Application (except to the extent permitted by mandatory statutory law).
   - Modify, adapt, translate, or create derivative works based upon the Application.
   - Rent, lease, lend, sell, redistribute, or sublicense the Application.
   - Use the Application to transmit malicious code, automated bots, scrapers, or viruses.
   - Interfere with, overburden, or compromise the integrity or security of our cloud sync infrastructure or connected third-party networks.
   - Use the Application for any unlawful, deceptive, or fraudulent purpose.

---

## 5. User Responsibilities & Account Security

1. **Physical and Device Security:** Because FinPulse operates primarily offline and stores sensitive financial ledgers in local device storage, you are solely responsible for maintaining device security (screen locks, device encryption, PIN codes, and biometric safeguards).
2. **App PIN and Biometric Lock:** If you configure an application PIN or biometric lock in FinPulse, you are responsible for maintaining your passcode. The developer cannot recover or reset forgotten local PIN hashes stored on your device.
3. **Google Sign-In Credentials:** If you activate optional cloud sync via your Google Account, you are responsible for safeguarding your Google account credentials and two-factor authentication. Any synchronization occurring under your authenticated credentials is your responsibility.
4. **Data Accuracy:** You bear sole responsibility for the records, amounts, currencies, and categories you input into the Application.

---

## 6. Offline Data, Backup & Cloud Synchronization

1. **Offline-First Data Storage:** By default, your financial data resides exclusively within Android's protected application sandbox on your physical device. Uninstalling the Application or clearing its storage without an independent CSV export, JSON backup, or activated Google Cloud Sync will result in the permanent, irrecoverable loss of all local records.
2. **Optional Cloud Synchronization:** Cloud synchronization relies upon network connectivity and Google Cloud infrastructure (Firebase Authentication and Google Cloud Firestore). While we endeavor to maintain high reliability and utilize deterministic conflict resolution (last-write-wins), we cannot guarantee continuous, error-free, or uninterrupted synchronization across all network states and device conditions.
3. **User Backups:** We recommend periodically exporting critical financial data via the built-in CSV export functionality.

---

## 7. Third-Party Services and External Dependencies

The Application integrates with third-party software and cloud infrastructure:
- **Google Play Services & Google Identity:** Authentication and Android Credential Manager integration.
- **Firebase Authentication & Google Cloud Firestore:** Cloud identity and encrypted remote document storage.
- **Public Exchange Rate Providers:** Foreign currency rates are retrieved via public HTTPS endpoints (`open.er-api.com` / `api.frankfurter.app`). We do not control or guarantee the timeliness, uninterrupted availability, or accuracy of third-party exchange rates.

Your use of third-party services is governed by their respective terms of service and privacy policies.

---

## 8. Intellectual Property

All title, ownership rights, trademarks, UI designs, code, graphics, documentation, and intellectual property associated with FinPulse are and remain the exclusive property of [INSERT LEGAL ENTITY / DEVELOPER NAME] or its licensors. All rights not expressly granted to you under these Terms are reserved.

---

## 9. Availability and Modifications to the Service

1. **Right to Update:** We reserve the right to modify, update, enhance, or discontinue any feature, tool, or component of the Application at any time without prior notice.
2. **No Maintenance Guarantee:** While we strive to issue updates for modern Android versions, we are under no obligation to provide ongoing maintenance, enhancements, or bug fixes for obsolete operating system versions or unsupported hardware configurations.

---

## 10. Disclaimer of Warranties

TO THE MAXIMUM EXTENT PERMITTED UNDER APPLICABLE LAW:
1. FINPULSE IS PROVIDED ON AN **"AS IS"** AND **"AS AVAILABLE"** BASIS, WITH ALL FAULTS AND WITHOUT WARRANTY OF ANY KIND.
2. WE EXPRESSLY DISCLAIM ALL WARRANTIES, WHETHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, INCLUDING WITHOUT LIMITATION WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, SYSTEM INTEGRATION, AND NON-INFRINGEMENT.
3. WE DO NOT WARRANT THAT THE SERVICE WILL BE UNINTERRUPTED, TIMELY, SECURE, ACCURATE, OR ERROR-FREE, OR THAT ANY DATA TRANSMISSIONS WILL REMAIN COMPLETELY FREE FROM LOSS, CORRUPTION, OR INTERCEPTION.

---

## 11. Limitation of Liability

TO THE FULLEST EXTENT PERMITTED BY APPLICABLE LAW:
1. IN NO EVENT SHALL [INSERT LEGAL ENTITY / DEVELOPER NAME], ITS DIRECTORS, EMPLOYEES, AFFILIATES, OR LICENSORS BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, EXEMPLARY, OR PUNITIVE DAMAGES, INCLUDING BUT NOT LIMITED TO LOSS OF PROFITS, DATA, GOODWILL, BANK OVERDRAFT CHARGES, PENALTIES, OR FINANCIAL LOSSES ARISING OUT OF OR IN CONNECTION WITH YOUR USE OR INABILITY TO USE THE APPLICATION.
2. OUR AGGREGATE LIABILITY ARISING FROM OR RELATED TO THESE TERMS OR THE SERVICE SHALL NOT EXCEED THE TOTAL AMOUNT ACTUALLY PAID BY YOU (IF ANY) TO PURCHASE OR USE FINPULSE IN THE TWELVE (12) MONTHS PRECEDING THE CLAIM, OR [INSERT LIABILITY CAP AMOUNT, e.g., $50.00 USD], WHICHEVER IS LESS.

---

## 12. Account Deletion and Termination

1. **Termination by User:** You may terminate these Terms at any time by deleting your cloud data via the in-app "Delete Cloud Data & Account" option and permanently uninstalling the Application from all devices.
2. **Termination by Publisher:** We reserve the right to suspend or terminate your access to cloud synchronization services immediately, without notice, if you breach these Terms or if required by law or Google Play policy.
3. **Survival:** Provisions that by their nature should survive termination shall survive (including Sections 3, 4.2, 8, 10, 11, 13, and 14).

---

## 13. Governing Law and Dispute Resolution

1. **Governing Law:** These Terms shall be governed by and construed in accordance with the laws of [INSERT GOVERNING JURISDICTION / COUNTRY / STATE], without giving effect to any principles of conflicts of law.
2. **Jurisdiction:** Any legal suit, action, or proceeding arising out of or related to these Terms or the Application shall be instituted exclusively in the competent courts of [INSERT COURT LOCATION / JURISDICTION].

---

## 14. Changes to These Terms

We may revise these Terms from time to time to reflect modifications in application functionality, statutory requirements, or Google Play developer policies. Updated versions will be published in the application repository and store listing, with the revised "Effective Date" posted at the top. Your continued use of FinPulse following the posting of updated Terms constitutes your binding agreement to the modifications.

---

## 15. Contact Information

For inquiries, feedback, or legal notices concerning these Terms of Use, please contact:

- **Entity / Developer:** [INSERT LEGAL ENTITY / DEVELOPER NAME]
- **Email:** [INSERT SUPPORT EMAIL, e.g., support@finpulse.app]
- **Website:** [INSERT PUBLIC WEBSITE URL, e.g., https://finpulse.app]
- **Physical Address:** [INSERT REGISTERED BUSINESS ADDRESS WHERE REQUIRED]
