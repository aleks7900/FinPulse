# TASK-04 — Bank Statement CSV Import

## Objective
Let users populate and maintain the app without manually entering every transaction.

## Requirements
- Import CSV files through Android's document picker.
- Build a mapping wizard for Date, Amount, Debit, Credit, Description, Merchant, Currency and optional Balance columns.
- Support common date/decimal formats.
- Preview parsed rows before import.
- Select destination account.
- Detect probable duplicate transactions.
- Show invalid rows with reasons.
- Allow users to exclude rows before confirmation.
- Save reusable import profiles for recurring bank formats.
- Perform large imports off the main thread.
- Present a post-import summary.

## Duplicate Detection
Use a deterministic fingerprint/candidate strategy based on relevant fields. Never silently discard ambiguous duplicates; surface them for review.

## Security
Process locally by default. Do not upload statements to external services.

## Testing
Cover malformed CSV, separators, quoted text, encodings, decimal comma/dot, duplicate files, reversed debit signs and large datasets.
