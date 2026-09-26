# TASK-01 — Ultra-Fast Transaction Entry

## Objective
Make recording everyday expenses and income fast enough that users will actually use the app daily.

## Requirements
- Add a persistent Quick Add action accessible from primary screens.
- Support Expense, Income and Transfer.
- Default to the last-used account when appropriate.
- Provide amount-first entry with a numeric keypad.
- Suggest recent/frequent categories and merchants.
- Allow one-tap selection of today/yesterday/custom date.
- Support optional notes and tags without making them mandatory.
- Remember safe user preferences such as last account/category patterns.
- Provide "Save & add another".
- Allow editing immediately after save via snackbar/action.
- Add home-screen shortcut/deep-link support for creating a transaction.
- Ensure balances and dashboard aggregates update immediately.

## Smart Defaults
Implement deterministic suggestions based on recent history. Do not silently assign uncertain categories; make suggestions easy to override.

## UX Target
A normal expense should be recordable in roughly 3–5 interactions after opening Quick Add.

## Engineering
Keep transaction creation business logic outside Compose. Validate monetary precision, transfers, account state and currency rules in the domain layer.

## Testing
Cover expense/income/transfer creation, defaults, invalid amounts, archived accounts, different currencies, rapid repeated saves and process recreation.
