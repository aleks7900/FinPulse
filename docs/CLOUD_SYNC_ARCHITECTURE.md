# Cloud Synchronization & Offline-First Architecture

Fin Pulse implements a robust, privacy-first, and offline-first cloud synchronization architecture. This document outlines the architectural patterns, data flow, conflict resolution mechanisms, and security guarantees.

---

## 1. High-Level Architecture Diagram

```
       +-------------------------------------------------------------+
       |                     Jetpack Compose UI                      |
       +-------------------------------------------------------------+
                                      |
                                      v
       +-------------------------------------------------------------+
       |                        View Models                          |
       +-------------------------------------------------------------+
                                      |
                                      v
       +-------------------------------------------------------------+
       |                    Domain Repositories                      |
       |   (Transaction, Account, Category, Budget, Recurring, etc.) |
       +-------------------------------------------------------------+
                 |                                      ^
                 v                                      |
+----------------------------------+                    |
|        Room Local Database       |                    |
|  - Financial Entities            |                    |
|  - sync_records (Metadata/Queue) |                    |
+----------------------------------+                    |
                 |                                      |
                 +-------------------+                  |
                                     v                  |
                   +------------------------------------+
                   |          CloudSyncEngine           |
                   |  (Conflict Resolution & Merging)   |
                   +------------------------------------+
                                     ^
                                     |
                   +------------------------------------+
                   |       WorkManager / SyncWorker     |
                   |   (Background & One-Time Sync)     |
                   +------------------------------------+
                                     ^
                                     |
                   +------------------------------------+
                   |     CloudStorageDataSource         |
                   |     (Firestore / In-Memory Mock)   |
                   +------------------------------------+
                                     |
                                     v HTTPS
                   +------------------------------------+
                   |       Google Cloud Firestore       |
                   |  path: /users/{authenticated_uid}/ |
                   +------------------------------------+
```

---

## 2. Core Architectural Pillars

### 2.1. Offline-First Guarantee
* **Room as the Single Source of Truth**: The UI *never* queries Firestore directly. All reads and writes target the local Room database instantaneously.
* **Non-Blocking User Experience**: When a user logs an expense, creates a budget, or edits a rule:
  1. The record is inserted/updated in Room in milliseconds.
  2. A corresponding metadata entry is inserted or updated in `sync_records` with status `PENDING_UPSERT`.
  3. UI observers (`StateFlow` / Live Queries) emit immediately.
  4. A background sync job is enqueued with network constraints.

### 2.2. Stable Unique Identifiers
* All synchronized entities use **UUIDv4 string identifiers** (e.g. `UUID.randomUUID().toString()`) rather than auto-incrementing integers.
* Inter-entity relationships (e.g. `transaction.accountId`, `transaction.categoryId`, `budget.categoryIds`) retain global uniqueness and validity across devices without translation tables or remapping.

### 2.3. Synchronization Metadata (`sync_records`)
The database maintains a dedicated tracking table:
```sql
CREATE TABLE sync_records (
    entityType TEXT NOT NULL,
    entityId TEXT NOT NULL,
    syncStatus TEXT NOT NULL,       -- SYNCED, PENDING_UPSERT, PENDING_DELETE, SYNC_ERROR
    localUpdatedAt INTEGER NOT NULL,
    cloudUpdatedAt INTEGER,
    isDeleted INTEGER NOT NULL DEFAULT 0,
    deletedAt INTEGER,
    errorMessage TEXT,
    PRIMARY KEY(entityType, entityId)
);
```

---

## 3. Conflict Resolution Strategy: Last-Write-Wins (LWW)

When the same record is modified on multiple devices concurrently or offline:

1. Each entity carries a millisecond timestamp (`updatedAt` / `localUpdatedAt`).
2. When synchronizing, `CloudSyncEngine` compares `localUpdatedAt` against the remote document's `updatedAt`:
   - **Local Newer (`local.updatedAt > remote.updatedAt`)**: Local record is uploaded to Firestore. The remote document is overwritten.
   - **Remote Newer (`remote.updatedAt > local.updatedAt`)**: Remote record is downloaded and saved to Room. Local record is overwritten.
   - **Timestamps Equal**: If payloads match, marked `SYNCED`. If conflicting, remote takes precedence to ensure deterministic convergence across all peers.
3. This guarantees eventual consistency without manual intervention or data corruption.

---

## 4. Deletion Strategy: Tombstones

To prevent a deleted item on Device A from being re-downloaded when Device B synchronizes:

1. When a user deletes an item:
   - The entity is removed from the active Room table (or soft-deleted).
   - A tombstone record is saved in `sync_records` with `isDeleted = true`, `deletedAt = System.currentTimeMillis()`, and status `PENDING_DELETE`.
2. During synchronization:
   - The tombstone is sent to Firestore as `{ isDeleted: true, deletedAt: ..., updatedAt: ... }`.
   - On peer devices, encountering a remote tombstone with `updatedAt >= local.updatedAt` causes the peer to delete its local copy.
3. **Obsolete Tombstone Pruning**:
   - Tombstones older than 30 days are purged locally and remotely once all active devices have acknowledged the deletion.

---

## 5. Initial Sign-In & Existing Data Migration

Existing local users may have created financial history before signing into Google. Fin Pulse treats user data with extreme care:

```
[ User Signs In with Google ]
              |
              v
[ Step 1: Query Remote Firestore Document Count ]
              |
      +-------+-------+
      |               |
[ Cloud is Empty ]    [ Cloud has Data ]
      |               |
      v               v
[ Upload all local ]  [ Two-Way Merge ]
[ entities to cloud]  - Remote records downloaded to Room
                      - Local un-synced items uploaded
                      - Conflicts resolved via LWW
              |
              v
[ Mark all local records as SYNCED ]
[ Update lastSyncedTimestamp in DataStore ]
```

**Never Overwrite Guarantee**: Signing in will *never* perform a blanket wipe of local un-synced data.

---

## 6. Multi-Account Isolation & Logout Safety

To prevent accidental data leakage if multiple family members or users share an Android device:

1. **Active User Tracking**: The active user's UID is recorded in `UserPreferencesDataStore.currentUserId`.
2. **Account Switching Detection**:
   - If User A logs out and User B logs in:
   - The engine detects `previousUserId != currentUserId`.
   - The local Room database is safely wiped of User A's data before restoring User B's cloud data.
3. **Logout Protection**:
   - If un-synced local changes exist (`pendingCount > 0`), the UI warns the user with a prompt:
     `"You have N unsynced changes. Sync now before signing out?"`
   - The user can choose to sync first or sign out anyway.

---

## 7. Cloud Storage Model (Firestore Structure)

Data is strictly partitioned under the user's authenticated UID:

```
/users/{uid}
    │
    ├── /transactions/{transactionId}
    │     ├── id, accountId, categoryId, amount, type, date, note, ...
    │     └── updatedAt, isDeleted
    │
    ├── /accounts/{accountId}
    │     ├── id, name, type, balance, currency, ...
    │     └── updatedAt, isDeleted
    │
    ├── /categories/{categoryId}
    ├── /budgets/{budgetId}
    ├── /recurring/{recurringId}
    ├── /goals/{goalId}
    ├── /assets/{assetId}
    ├── /debts/{debtId}
    ├── /categorization_rules/{ruleId}
    ├── /saved_filters/{filterId}
    │
    └── /settings/account_settings
          ├── baseCurrency
          ├── weekStartDay
          ├── isBiometricEnabled
          ├── defaultIncomeAccountId
          ├── defaultExpenseAccountId
          └── updatedAt
```

### Security Rules Enforcement
Cloud Firestore rules strictly enforce:
```javascript
match /users/{userId}/{document=**} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```
No client can ever read, list, or tamper with another user's financial documents.

---

## 8. Background Synchronization (WorkManager)

* **Worker**: `md.alexlab.finpulse.core.work.SyncWorker`
* **Trigger Conditions**:
  - `NetworkType.CONNECTED` (Requires internet connectivity)
  - `BatteryNotLow` (Preserves battery on low battery states)
* **Execution Frequency**:
  - **Periodic**: Runs every 6 hours in the background to ensure parity across secondary devices.
  - **One-Time Immediate**: Enqueued whenever the user creates, updates, or deletes transactions or taps "Sync Now".
* **Exponential Backoff**: Transient network dropouts retry with exponential backoff up to 3 attempts.
