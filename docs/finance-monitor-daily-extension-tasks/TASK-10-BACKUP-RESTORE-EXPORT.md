# TASK-10 — Reliable Backup, Restore & Export

## Objective
Ensure users can trust the app with long-term financial history.

## Requirements
- Full local backup in a versioned format.
- Restore with validation before modifying the current database.
- CSV transaction export.
- JSON structured export/backup.
- Export selected date ranges/accounts.
- Include categories, accounts, budgets, recurring rules and goals in full backup.
- Create database migration/version compatibility strategy.
- Show backup date and restore summary.
- Provide optional periodic backup reminders.

## Reliability
A failed restore must not leave the database partially modified. Use validation and transactional replacement/migration strategies.

## Security
Offer encrypted backup design for sensitive full backups. Never include secrets/API tokens in portable exports.
