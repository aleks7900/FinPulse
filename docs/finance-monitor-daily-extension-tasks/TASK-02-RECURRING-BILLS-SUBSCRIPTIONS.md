# TASK-02 — Recurring Bills, Income & Subscription Center

## Objective
Make predictable financial events automatic and visible.

## Requirements
- Create recurring rules for expenses, income and transfers.
- Frequencies: daily, weekly, biweekly, monthly, quarterly, yearly and custom where practical.
- Track next occurrence and payment status.
- Support fixed and variable expected amounts.
- Create an Upcoming section for the next 7/30 days.
- Allow Skip, Paid, Edit occurrence, Pause and Cancel.
- Support salary, rent, utilities, loans, subscriptions and transfers to savings.
- Show monthly and annualized subscription totals.
- Send configurable reminders before due dates.
- Avoid duplicate generation after restart or WorkManager retry.
- Preserve historical generated transactions when a recurring rule changes.

## Important Behavior
Do not automatically mark a bill as paid merely because its due date passed. Distinguish expected, generated, paid, skipped and overdue states.

## Testing
Test month-end dates, February/leap years, timezone/date boundaries, retries, paused rules and edited individual occurrences.
