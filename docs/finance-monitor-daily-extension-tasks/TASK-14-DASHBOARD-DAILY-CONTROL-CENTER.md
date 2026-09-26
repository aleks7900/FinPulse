# TASK-14 — Daily Financial Control Center Dashboard

## Objective
Redesign the dashboard around the questions users need answered every day.

## Primary Content
At a glance show:
- total balance
- this month's income
- this month's spending
- remaining budget
- safe-to-spend estimate
- upcoming bills
- recent transactions
- review inbox count
- savings progress

## Requirements
- Allow card reordering/hiding.
- Provide quick period switching.
- Make cards drill down into details.
- Add concise trend indicators with accessible explanations.
- Avoid decorative charts that do not provide actionable information.
- Provide empty/loading/error states.
- Make data reactive so edits/imports immediately update affected cards.
- Keep expensive aggregation queries optimized and test with large transaction histories.

## UX Goal
The dashboard should answer "What needs my attention today?" in a few seconds.
