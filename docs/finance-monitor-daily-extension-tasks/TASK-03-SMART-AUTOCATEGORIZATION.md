# TASK-03 — Smart Transaction Categorization & Rules

## Objective
Reduce repetitive manual categorization during daily use.

## Requirements
- Suggest categories from merchant/payee and historical transactions.
- Add user-defined rules such as:
  - merchant contains X -> category Y
  - description contains X -> category Y
  - account + merchant -> category
- Provide rule priority and conflict handling.
- Allow applying a rule to existing matching transactions with explicit confirmation.
- Add a review queue for uncategorized/low-confidence transactions.
- Let users correct suggestions quickly.
- Use corrections as local deterministic history signals.
- Never overwrite a manually chosen category without explicit user action.

## Architecture
Create a categorization engine independent from UI and data-import providers so ML/AI classification can be added later.

## Testing
Cover normalization, case differences, merchant aliases, conflicting rules, manual overrides and bulk application.
