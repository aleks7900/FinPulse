# TASK-12 — Daily & Weekly Financial Digest

## Objective
Give users a concise habit-forming summary instead of requiring them to inspect every analytics screen.

## Requirements
Create optional digest cards/notifications summarizing:
- spending today/this week
- comparison with previous period
- largest expenses
- budget status
- upcoming bills
- uncategorized/review items
- savings progress
- unusual deterministic changes

## Behavior
- User chooses daily, weekly or disabled.
- Tapping a digest opens the relevant details.
- Avoid spam: group insights and prioritize meaningful changes.
- Use local deterministic analytics initially.
- Make notification contents privacy-configurable.

## Architecture
Design an insight model that can later support server or AI-generated explanations without coupling them to notifications.
