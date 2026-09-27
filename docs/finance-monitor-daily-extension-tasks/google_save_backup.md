# TASK — Google Account Login + Cloud Sync & Backup for Fin Pulse

## Objective

Implement Google Account authentication and secure cloud synchronization for the Fin Pulse Android application.

Users must be able to sign in with their Google account and have their Fin Pulse data associated with that account.

The following data must be preserved across devices/reinstallations:

- transactions
- income
- expenses
- transfers
- accounts/wallets
- categories
- custom categories
- budgets
- recurring transactions
- financial preferences
- application settings
- relevant user-created Fin Pulse data

The application must remain fully usable offline.

Signing into the same Google account on another Android device should restore/synchronize the user's Fin Pulse data.

---

# 1. Inspect Existing Architecture First

Before implementing anything, inspect the complete Fin Pulse project.

Identify:

- UI architecture
- Jetpack Compose structure
- ViewModels
- repositories
- Room/database implementation
- entities
- DAOs
- transaction model
- account/wallet model
- categories
- settings storage
- DataStore / SharedPreferences usage
- dependency injection
- navigation
- existing backend/cloud dependencies
- Gradle configuration
- minSdk / targetSdk
- existing Google/Firebase configuration

Do NOT create a parallel architecture if appropriate infrastructure already exists.

Reuse the current architecture and naming conventions.

Document the discovered architecture briefly before implementing.

---

# 2. Google Account Authentication

Implement modern Google authentication for Android using currently supported Google APIs.

IMPORTANT:

Do not introduce deprecated Google Sign-In APIs if the project can use the current recommended Android authentication stack.

Prefer:

- Credential Manager
- Sign in with Google / Google ID credentials

Implement:

Sign in with Google

and account management/logout functionality.

The login screen should follow the existing Fin Pulse Material design.

Example:

Fin Pulse

Keep your finances synchronized
across your devices.

[ Continue with Google ]

Users should also be able to use Fin Pulse locally if cloud synchronization is optional in the existing product design.

---

# 3. User Identity

Each cloud user must have a stable unique identifier.

Never use:

- email address
- display name

as the database ownership identifier.

Use the stable UID supplied by the authentication/backend system.

Example conceptual structure:

users/{uid}

All cloud financial data must be isolated under the authenticated user's UID.

A user MUST NEVER be able to read or modify another user's financial data.

---

# 4. Cloud Architecture

Inspect whether Fin Pulse already has a backend.

If it does, integrate Google authentication with the existing backend.

If no backend exists, implement an appropriate secure cloud architecture.

A suitable implementation may use:

Google authentication
        ↓
Firebase Authentication
        ↓
Firestore / cloud persistence
        ↓
Repository
        ↓
Room local database
        ↓
ViewModel / UI

Do not blindly introduce Firebase if the project already has a backend capable of supporting authenticated synchronization.

Choose the solution that best fits the existing architecture.

Document the decision.

---

# 5. Offline-First Architecture

Fin Pulse MUST remain offline-first.

Room/local storage remains the immediate source used by the application UI.

The app should NOT require network connectivity every time transactions are displayed.

Expected flow:

UI
 ↓
ViewModel
 ↓
Repository
 ↓
Local database
 ↕
Sync Engine
 ↕
Cloud

When a transaction is created:

1. Save locally immediately.
2. UI updates immediately.
3. Mark the record as requiring synchronization.
4. Synchronize with the cloud when network connectivity is available.

The user must be able to:

- create transactions offline
- edit transactions offline
- delete transactions offline
- browse transaction history offline

Changes synchronize later.

---

# 6. Synchronize Financial Data

Cloud synchronization must cover all relevant financial entities discovered in the project.

At minimum inspect and support:

Transaction

- id
- type
- amount
- currency
- account
- category
- note
- transaction date
- created timestamp
- updated timestamp

Accounts / wallets

Categories

Budgets

Recurring transactions

Any other persisted financial entities already implemented in Fin Pulse.

DO NOT silently omit existing user data from synchronization.

---

# 7. Stable IDs

All synchronized entities must have stable unique IDs.

Do not depend on local Room auto-increment IDs as globally unique cloud identifiers.

If necessary, migrate synchronized entities toward UUID-style identifiers while preserving compatibility with existing local data.

Relationships between:

- transactions
- categories
- accounts
- budgets

must remain valid after synchronization and restoration.

---

# 8. Sync Metadata

Introduce appropriate synchronization metadata where required.

For example:

syncId
updatedAt
createdAt
syncStatus
deletedAt

Possible sync state:

SYNCED
PENDING_CREATE
PENDING_UPDATE
PENDING_DELETE
SYNC_ERROR

Adapt this to the existing architecture rather than copying these fields blindly.

---

# 9. Deletions

Deletion synchronization must be handled correctly.

Do NOT allow this situation:

Device A deletes transaction X
→ cloud still contains X
→ Device B synchronizes
→ transaction X unexpectedly returns

Implement an appropriate deletion strategy such as tombstones / deletedAt metadata.

Eventually remove obsolete cloud records according to a documented cleanup strategy.

---

# 10. Conflict Resolution

Implement deterministic conflict handling.

Example:

Device A edits Transaction X offline.
Device B edits Transaction X.
Both later synchronize.

The application must not corrupt the database or create random duplicate transactions.

Use an appropriate conflict strategy.

A simple initial implementation can use:

updatedAt + last-write-wins

provided timestamps and server synchronization are handled safely.

Document the chosen conflict strategy.

---

# 11. Initial Sign-In Migration

This is extremely important.

Existing Fin Pulse users may already have transactions locally BEFORE Google login is introduced.

Signing into Google MUST NOT erase those transactions.

On first login:

1. Detect existing local data.
2. Detect existing cloud data.
3. Determine whether migration/merge is necessary.
4. Upload/merge local records safely.
5. Avoid duplicate transactions.
6. Preserve IDs and relationships.
7. Mark successfully migrated records as synchronized.

Never automatically replace an existing local database with an empty cloud database.

---

# 12. New Device Restoration

Test this scenario:

Device A

Google Account A
↓
100 Fin Pulse transactions
↓
Cloud synchronized

Then:

Device B
↓
Install Fin Pulse
↓
Login with Google Account A

Fin Pulse should automatically retrieve and restore:

- accounts
- categories
- transactions
- budgets
- recurring transactions
- synchronized settings

Relationships between records must remain intact.

---

# 13. Settings Synchronization

Inspect all existing application settings.

Separate them into:

## Device-specific settings

Examples:

- transient UI state
- device permissions
- device-specific notification configuration where appropriate

These should generally remain local.

## Account settings

Examples:

- preferred currency
- custom financial preferences
- relevant dashboard configuration
- other portable Fin Pulse preferences

These can synchronize between devices.

Do not blindly upload the entire DataStore/preferences file.

Create an explicit cloud settings model.

---

# 14. Account Screen

Add or extend:

Settings → Account

Logged out:

Google Account
Not connected

[ Sign in with Google ]

Logged in:

Google Account

Avatar
User name
email@example.com

Sync
Last synced: ...

[ Sync now ]

[ Sign out ]

Follow the existing Fin Pulse visual language.

---

# 15. Sync Status

Provide subtle synchronization state where useful.

Examples:

Synced

Syncing...

Waiting for internet

Sync error

Last synced:
27 Sep 2026, 21:32

Do not make normal application usage dependent on the sync indicator.

---

# 16. Manual Sync

Provide:

Sync now

This should trigger synchronization without creating duplicate records.

Multiple repeated sync operations must be idempotent.

---

# 17. Background Synchronization

Use an Android-appropriate background mechanism such as WorkManager.

Sync should occur when appropriate after:

- transaction creation
- transaction modification
- transaction deletion
- connectivity restoration
- application startup
- login

Avoid excessive network requests and battery usage.

Batch synchronization where appropriate.

---

# 18. Logout Behavior

Logout must be handled carefully.

Financial data must never accidentally appear under another Google account.

Scenario:

User A logs in
→ Fin Pulse downloads User A data
→ User A logs out
→ User B logs in

User B MUST NOT see User A transactions.

Design explicit account-scoped local storage or safe local database clearing/switching behavior.

Do not simply leave User A's Room data visible after switching accounts.

If logout would remove unsynchronized local changes, warn the user or synchronize them first.

---

# 19. Security

Financial history is sensitive data.

Implement security carefully.

Requirements:

- HTTPS only
- secure authentication tokens
- no passwords stored by Fin Pulse
- no auth tokens logged
- no financial data written to debug logs
- cloud database access restricted by authenticated UID
- server/database security rules
- validation of ownership
- least-privilege access

Never trust a UID supplied arbitrarily by the client when backend authorization can derive identity from the authenticated token.

---

# 20. Firestore Security Rules

If Firestore is selected, create proper production security rules.

Conceptually:

users/{uid}/...

should only be readable/writable when:

request.auth != null
AND
request.auth.uid == uid

Do NOT leave development rules such as unrestricted:

allow read, write: if true

Document and include the required security rules in the project.

---

# 21. Google/Firebase Configuration

Configure all required Android dependencies and Gradle plugins.

Document any external console configuration that the developer must perform manually, including where applicable:

- Firebase project
- Android package registration
- SHA-1
- SHA-256
- Google authentication provider
- OAuth configuration
- google-services.json
- Firestore
- Firestore security rules

Do NOT commit private credentials or secrets into Git.

Update `.gitignore` where necessary.

---

# 22. Error Handling

Handle gracefully:

- user cancels Google login
- no internet
- expired authentication
- cloud unavailable
- timeout
- failed upload
- failed download
- malformed cloud record
- duplicate record
- partial synchronization
- account switching

A temporary cloud failure must NOT prevent the user from accessing locally stored financial history.

---

# 23. Data Integrity

Financial data accuracy is more important than synchronization speed.

Synchronization must NEVER silently:

- change amounts
- change transaction types
- change currencies
- duplicate transactions
- lose transactions
- reconnect a transaction to the wrong account/category

Use transactions/atomic operations where required.

---

# 24. Tests

Add tests for the synchronization layer.

At minimum cover:

Google login state handling

Local → Cloud:
Create transaction
Edit transaction
Delete transaction

Cloud → Local:
Download new transaction
Update transaction
Delete transaction

Conflicts:
Local and remote modification

Account isolation:
User A data must not appear for User B

Migration:
Existing anonymous/local user → Google account

Offline:
Create transaction offline → reconnect → synchronization

Idempotency:
Running synchronization repeatedly does not create duplicates.

---

# 25. Verification

After implementation:

1. Build the complete Android project.
2. Run existing tests.
3. Run new synchronization tests.
4. Fix compilation failures.
5. Fix lint issues introduced by this task.
6. Verify Google authentication on a real/emulated Android device.
7. Test offline transaction creation.
8. Restore connectivity and verify synchronization.
9. Reinstall the application.
10. Login with the same Google account.
11. Confirm financial history is restored correctly.
12. Login with a different account.
13. Confirm complete account isolation.

---

# Deliverables

Provide:

- Google authentication implementation
- authentication repository
- account/session management
- cloud persistence layer
- offline-first synchronization engine
- Room integration
- WorkManager background synchronization
- settings synchronization
- account/settings UI
- logout/account switching
- migration of existing local users
- cloud security rules/configuration
- automated tests
- setup documentation

Also create:

docs/GOOGLE_AUTH_SETUP.md
docs/CLOUD_SYNC_ARCHITECTURE.md

`GOOGLE_AUTH_SETUP.md` must contain exact manual steps I need to perform in Google/Firebase consoles.

Do not leave the project in a partially implemented state.

The final result should allow a user to install Fin Pulse, sign into their Google account, use the app offline or online, and later sign into Fin Pulse on another device with the same account and recover their synchronized financial history and portable settings.