# TASK — Localize Fin Pulse Android App into 11 Languages

## Objective

Implement complete, production-ready internationalization and localization for the existing **Fin Pulse Android application**.

Fin Pulse is a financial monitoring application, so translations must use **accurate and natural financial terminology**, not literal machine-style translations.

Keep **English as the default/fallback language** and add the following 11 languages:

1. Russian — `ru`
2. Spanish — `es`
3. Portuguese (Brazil) — `pt-rBR`
4. German — `de`
5. French — `fr`
6. Italian — `it`
7. Polish — `pl`
8. Turkish — `tr`
9. Japanese — `ja`
10. Korean — `ko`
11. Simplified Chinese — `zh-rCN`

Final support: **English + 11 localized languages = 12 languages total.**

---

## 1. Audit the Existing Project

First inspect the entire Fin Pulse Android project.

Find every user-facing hardcoded string in:

- Kotlin
- Java
- Jetpack Compose
- XML layouts
- Navigation
- Dialogs
- Notifications
- Widgets
- Settings
- Error handling
- Validation messages
- Toasts
- Snackbars

Pay particular attention to financial UI such as:

- Dashboard
- Accounts
- Balance
- Transactions
- Income
- Expenses
- Budgets
- Categories
- Analytics
- Statistics
- Reports
- Cash flow
- Net worth
- Assets
- Liabilities
- Savings
- Investments
- Goals
- Recurring payments
- Subscriptions
- Notifications
- Financial insights
- Search and filters
- Date ranges
- Currency settings
- Security/settings screens
- Onboarding
- Empty states
- Error states

Move all appropriate hardcoded strings into Android string resources.

Do NOT translate technical identifiers such as API parameters, database values, analytics event IDs, URLs, enum persistence values, or backend contracts.

---

## 2. Android Localization Structure

Maintain:

```text
res/values/strings.xml
```

Create:

```text
res/values-ru/strings.xml
res/values-es/strings.xml
res/values-pt-rBR/strings.xml
res/values-de/strings.xml
res/values-fr/strings.xml
res/values-it/strings.xml
res/values-pl/strings.xml
res/values-tr/strings.xml
res/values-ja/strings.xml
res/values-ko/strings.xml
res/values-zh-rCN/strings.xml
```

English must remain the fallback language.

Use `translatable="false"` for strings that intentionally must never be translated.

---

## 3. Financial Translation Quality

Translation quality is especially important for Fin Pulse.

Do NOT blindly translate financial terminology word-for-word.

Use terminology normally found in native banking, personal finance, budgeting, and investment applications in each target market.

Correctly distinguish concepts such as:

- Income
- Expense
- Transfer
- Transaction
- Balance
- Available balance
- Current balance
- Budget
- Spending
- Savings
- Cash flow
- Net worth
- Asset
- Liability
- Investment
- Profit
- Loss
- Return
- Interest
- Recurring payment
- Subscription
- Financial goal
- Account
- Category
- Merchant
- Statement
- Forecast
- Monthly spending
- Spending limit

Context matters. A translation that is linguistically correct but financially misleading is unacceptable.

---

## 4. Russian Localization

Russian must receive particular attention because financial terminology and pluralization can differ significantly from English.

Use natural Russian financial-app terminology.

Examples of the intended style:

```text
Dashboard → Обзор
Transactions → Транзакции / Операции depending on context
Income → Доходы
Expenses → Расходы
Budget → Бюджет
Savings → Сбережения
Investments → Инвестиции
Net worth → Чистый капитал
Cash flow → Денежный поток
Recurring payments → Регулярные платежи
Financial goals → Финансовые цели
```

Choose terminology based on the actual UI context rather than blindly following these examples.

Avoid awkward literal translations.

---

## 5. Currency and Number Localization

Do NOT hardcode financial number formatting.

Use locale-aware formatting where appropriate.

For example, currencies may appear differently:

```text
$1,234.56
1 234,56 €
1.234,56 €
```

Preserve the actual monetary value while formatting according to the user's locale and selected currency.

Do not assume that application language determines account currency.

**Locale and currency are separate concepts.**

For example, a Russian-language user may still have accounts in EUR, USD, MDL, GBP, etc.

Do not automatically change financial data or account currencies when the UI language changes.

---

## 6. Dates and Times

Use locale-aware date/time formatting.

Avoid hardcoded patterns such as:

```text
MM/dd/yyyy
```

when the date is intended for user display.

Ensure dates, month names, weekdays, and relative date labels display naturally for each locale.

Examples:

```text
Today
Yesterday
This week
This month
Last 30 days
January
February
```

Translate and/or format these appropriately.

---

## 7. Pluralization

Audit quantity-dependent strings and use Android `<plurals>` resources where appropriate.

Examples:

```text
1 transaction
5 transactions

1 account
3 accounts

1 day
30 days
```

Russian, Polish and other supported languages have more complex plural rules than English.

Implement proper Android plural resources rather than concatenating numbers with translated nouns.

---

## 8. Formatting Placeholders

Preserve all placeholders correctly:

```text
%s
%d
%1$s
%2$s
%1$d
```

Example:

```xml
<string name="monthly_spending">You spent %1$s this month</string>
```

Translations must preserve the value placeholder while allowing grammatically appropriate sentence ordering.

Never change the placeholder type accidentally.

---

## 9. Jetpack Compose

If Fin Pulse uses Jetpack Compose, replace hardcoded UI strings with resources:

```kotlin
stringResource(R.string.some_string)
```

For quantities use:

```kotlin
pluralStringResource(...)
```

Do not introduce unnecessary architectural changes.

---

## 10. Language Selector

Implement or extend:

**Settings → Language**

Available options:

```text
System Default
English
Русский
Español
Português (Brasil)
Deutsch
Français
Italiano
Polski
Türkçe
日本語
한국어
简体中文
```

Use Android's recommended per-app language APIs where compatible with the project's architecture and SDK configuration.

The selected language must persist across:

- App restart
- Activity recreation
- Process recreation

Changing the language must NOT modify:

- Currency
- Accounts
- Transactions
- Financial data
- User preferences unrelated to language

---

## 11. UI Layout Validation

Translations can be substantially longer than English.

Pay particular attention to:

- Russian
- German
- French
- Polish

Check:

- Buttons
- Bottom navigation
- Tabs
- Cards
- Charts
- Dialogs
- Settings rows
- Filters
- Financial summaries
- Small dashboard widgets

Avoid text clipping and overlapping.

Prefer flexible layouts, wrapping, or appropriate ellipsis rather than globally reducing font sizes.

---

## 12. Charts and Analytics

Localize user-visible chart content:

- Titles
- Legends
- Axis labels
- Tooltips
- Period selectors
- Empty states
- Category labels when they are predefined by the application

Examples:

```text
Income vs Expenses
Monthly Spending
Spending by Category
Cash Flow
Balance History
Net Worth
Last 7 Days
Last 30 Days
This Year
```

Do NOT translate user-created category/account names.

---

## 13. User-Generated Content

Never automatically translate data created by the user.

For example:

```text
"My Salary"
"Vacation Fund"
"Apartment"
"Netflix"
"Main Account"
```

must remain exactly as entered.

Only application-provided/default categories should be localized.

---

## 14. Notifications

Audit and localize all notification content.

Examples:

- Budget warnings
- Spending alerts
- Recurring payment reminders
- Goal progress
- Daily/weekly summaries
- Financial insights

Ensure notifications use the currently selected application locale where technically appropriate.

---

## 15. Accessibility

Localize accessibility-related strings including:

- `contentDescription`
- Semantic labels
- Screen-reader descriptions
- Action descriptions

Do not leave English accessibility text when the rest of the application is localized.

---

## 16. Translation Completeness Validation

Compare every locale against the default:

```text
values/strings.xml
```

Every translatable key must exist in every supported locale.

Detect:

- Missing translations
- Duplicate keys
- Accidentally untranslated English strings
- Invalid placeholders
- Invalid XML
- Incorrect apostrophe/quote escaping
- Missing plural forms
- Hardcoded UI strings

---

## 17. Build and Test

After implementation:

1. Build the Android project.
2. Run existing unit tests.
3. Run existing Android/instrumentation tests where available.
4. Validate Android resources.
5. Check all locale resource files.
6. Check formatting placeholders.
7. Check pluralization.
8. Check locale switching.
9. Check currency formatting.
10. Check date formatting.
11. Search again for remaining hardcoded user-facing strings.

Fix all localization-related problems introduced by this task.

---

## Constraints

Do NOT:

- Change financial calculations.
- Modify account balances.
- Modify database schemas unless absolutely necessary for locale support.
- Change backend/API contracts.
- Translate user-generated data.
- Couple language selection to currency selection.
- Change analytics identifiers.
- Rename API parameters.
- Redesign the application unnecessarily.
- Introduce unrelated functionality.

Preserve existing architecture and behavior.

---

## Final Deliverables

When finished, provide a report containing:

- Languages implemented.
- Localization files created/modified.
- Number of translated strings.
- Number of hardcoded strings migrated.
- Plural resources added.
- Currency/date formatting changes.
- Language selector implementation details.
- Any strings intentionally marked non-translatable.
- Remaining localization concerns.
- Build result.
- Test result.

The final application must be fully buildable and support:

**English + Russian + Spanish + Portuguese (Brazil) + German + French + Italian + Polish + Turkish + Japanese + Korean + Simplified Chinese.**

The localization must be suitable for a **production financial Android application**, with special attention to financial terminology, currency formatting, dates, pluralization, and data integrity.