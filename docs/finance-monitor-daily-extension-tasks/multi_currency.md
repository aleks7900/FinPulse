# TASK — Implement Multi-Currency Support for FinPulse

## Objective

Extend the FinPulse Android application so users can:

1. Change the application's default currency in **Settings**.
2. Select a separate currency for each **Account**.
3. Select a separate currency for each **Budget**.
4. See the correct currency symbol/code everywhere amounts are displayed.

Inspect the existing architecture, database, models, repositories, ViewModels, Compose/XML UI, calculations, and settings before implementing the feature. Integrate with the current architecture rather than creating a parallel system.

---

## 1. Application Default Currency

Add a setting:

**Settings → Currency → Default currency**

Allow the user to select their preferred default currency.

Examples:

- USD — US Dollar ($)
- EUR — Euro (€)
- GBP — British Pound (£)
- MDL — Moldovan Leu (L)
- RON — Romanian Leu (lei)
- UAH — Ukrainian Hryvnia (₴)
- PLN — Polish Zloty (zł)
- CHF — Swiss Franc
- JPY — Japanese Yen (¥)
- CNY — Chinese Yuan (¥)
- CAD
- AUD
- and other commonly used ISO 4217 currencies.

Store the **ISO 4217 currency code**, e.g.:

```text
USD
EUR
MDL
GBP
```

Do not store only the symbol because symbols such as `$` are ambiguous.

Persist the selected default currency using the application's existing settings/preferences architecture.

The selection must survive application restart.

---

## 2. Currency Selector UI

Create a reusable currency picker that can be used by:

- Settings
- Account creation/editing
- Budget creation/editing

The picker should display:

```text
🇲🇩 MDL — Moldovan Leu
🇪🇺 EUR — Euro
🇺🇸 USD — US Dollar
🇬🇧 GBP — British Pound
```

Where appropriate, also show the currency symbol.

Provide:

- Search
- Currency code
- Currency name
- Symbol
- Current selection indicator

Search should support queries such as:

```text
USD
Dollar
EUR
Euro
MDL
Leu
```

Follow the application's existing Material Design / Material 3 UI.

---

## 3. Account-Level Currency

Every financial account must have its own currency.

Examples:

```text
MAIB Card
12,450.00 MDL

Revolut EUR
€1,250.00

USD Savings
$5,400.00
```

Add a `currencyCode` field to the Account model/entity if one does not already exist.

Example:

```kotlin
currencyCode: String
```

When creating a new account:

```text
Currency = application's current default currency
```

The user can override it using the currency picker.

When editing an account, allow changing its currency.

### Important

Changing an account's currency must NOT silently reinterpret historical monetary values.

For example:

```text
Old account:
10,000 MDL

User changes currency to EUR
```

Do NOT silently turn this into:

```text
10,000 EUR
```

Inspect how FinPulse stores transactions and balances and implement a safe strategy.

If changing the currency of an account containing transactions would make historical data ambiguous, show an appropriate warning or prevent the unsafe change.

Do not automatically perform currency conversion unless an exchange-rate system actually exists.

---

## 4. Budget-Level Currency

Every Budget must also support its own currency independently from accounts.

Add:

```kotlin
currencyCode: String
```

to the appropriate Budget entity/model if necessary.

When creating a budget:

```text
Currency = application's current default currency
```

Allow the user to override it.

Example:

```text
Monthly Living
15,000 MDL

Travel Europe
€2,000

US Trip
$3,000
```

Budget screens must display amounts using the budget's own currency.

---

## 5. Currency Resolution Rules

Implement clear currency ownership.

### Application/global values

Use:

```text
Default Currency
```

### Account-related amounts

Use:

```text
Account.currencyCode
```

### Budget-related amounts

Use:

```text
Budget.currencyCode
```

Do NOT make existing accounts and budgets dynamically inherit future changes to the default currency.

Example:

```text
Default currency = MDL
Create Account A → MDL

Change default currency → EUR

Account A remains MDL.
New Account B defaults to EUR.
```

The default currency should therefore act as the **initial value for newly created objects**, not as a live override of existing objects.

---

## 6. Transactions

Inspect the existing transaction architecture carefully.

Transactions associated with an account should normally use the account's currency unless FinPulse already has an explicit transaction currency model.

Ensure transaction screens display the correct currency.

Examples:

```text
Coffee
-45.00 MDL

Salary
+2,500.00 EUR
```

Do not assume all transactions use the global currency.

If transaction records need their own persisted `currencyCode` to preserve historical correctness, implement the appropriate migration.

---

## 7. Transfers Between Different Currencies

Inspect existing account-to-account transfer functionality.

If transfers can occur between accounts with different currencies, do NOT assume:

```text
100 USD = 100 EUR
```

At minimum, detect cross-currency transfers.

If FinPulse does not currently have exchange-rate/conversion functionality, prevent incorrect automatic conversion and clearly indicate that the accounts use different currencies.

Structure the implementation so exchange-rate support can be added later.

---

## 8. Money Formatting

Create/reuse a centralized money formatter instead of manually concatenating currency symbols.

Avoid:

```kotlin
"$" + amount
amount + " €"
```

Use a centralized mechanism based on:

```text
amount
currencyCode
locale
```

For example:

```kotlin
formatMoney(
    amount = amount,
    currencyCode = account.currencyCode
)
```

Formatting should correctly handle:

- Currency symbol
- Decimal digits
- Decimal separator
- Thousands separator
- Currency placement
- Locale

Examples depending on locale:

```text
$1,250.50
1.250,50 €
12 450,00 MDL
¥5,000
```

Avoid hardcoded `$`, `€`, `MDL`, etc. throughout the UI.

---

## 9. Dashboard and Aggregated Values

This is critical.

Inspect:

- Dashboard
- Total balance
- Net worth
- Income totals
- Expense totals
- Charts
- Statistics
- Reports
- Budget summaries
- Account summaries

Never mathematically add monetary amounts from different currencies without conversion.

Incorrect:

```text
Account A = 1,000 USD
Account B = 1,000 EUR

Total = 2,000 USD
```

If FinPulse currently has no exchange-rate engine, do not fabricate a converted total.

Use a safe presentation, such as grouped balances:

```text
USD 1,000
EUR 1,000
MDL 12,000
```

or another UX consistent with the existing application.

Keep the architecture ready for a future currency-conversion/exchange-rate feature.

---

## 10. Database Migration

Inspect the existing Room/database schema.

Add currency fields where necessary without destroying existing user data.

Existing records that have no currency should be migrated using the application's current/default legacy currency.

Create proper Room migrations.

Do NOT use destructive migration.

Verify:

- Existing accounts survive upgrade.
- Existing budgets survive upgrade.
- Existing transactions survive upgrade.
- Existing amounts remain unchanged.
- Existing relationships remain intact.

---

## 11. Localization

All new UI text must use Android string resources.

Add translations for every language currently supported by FinPulse.

This includes strings such as:

```text
Default currency
Currency
Select currency
Search currencies
Account currency
Budget currency
Different currencies
Change currency
```

Do not introduce new hardcoded user-visible strings.

---

## 12. Cloud Sync / Serialization Compatibility

Inspect whether Account, Budget, Transaction, settings, or related entities are serialized, backed up, exported, or synchronized.

If FinPulse already has or is implementing Google/cloud synchronization, ensure the new currency fields are included in the relevant DTOs and serialization/sync models.

Maintain backward compatibility with records that do not yet contain `currencyCode`.

---

## 13. Tests

Add/update tests covering at least:

- Default currency persistence
- Changing default currency
- New account inherits current default
- Existing account does not change when default changes
- Account-specific currency
- Budget-specific currency
- New budget inherits current default
- Existing budget does not change when default changes
- Correct money formatting
- Database migration
- Existing data preservation
- Different currencies displayed correctly
- Mixed-currency totals are not incorrectly summed
- Cross-currency transfer behavior
- App restart preserves settings

Run the relevant Gradle build, unit tests, lint, and existing test suite.

Fix regressions introduced by the implementation.

---

## Acceptance Criteria

The implementation is complete when:

- The user can change the default currency from Settings.
- The user can choose a currency when creating/editing an account.
- The user can choose a currency when creating/editing a budget.
- Each account permanently stores its currency.
- Each budget permanently stores its currency.
- Changing the global default does not mutate existing accounts/budgets.
- Currency formatting is centralized.
- Hardcoded currency symbols are removed from user-facing UI.
- Existing financial data is safely migrated.
- Mixed currencies are never incorrectly added together.
- The feature works after application restart.
- Cloud/export/backup models remain compatible where applicable.
- All new text is localized.
- The project builds and tests successfully.

## Final Report

After implementation, report:

1. Database/schema changes.
2. Models/entities modified.
3. New currency components/services introduced.
4. Screens modified.
5. Migration strategy.
6. How mixed-currency totals are handled.
7. How account/budget currency changes are handled safely.
8. Tests added/updated.
9. Build/lint/test results.

Implement the feature directly. Do not stop after producing an analysis or implementation plan.