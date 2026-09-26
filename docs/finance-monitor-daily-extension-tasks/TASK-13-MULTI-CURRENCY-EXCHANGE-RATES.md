# TASK-13 — Production-Ready Multi-Currency

## Objective
Make balances and analytics reliable for users with accounts in multiple currencies.

## Requirements
- User-defined base currency.
- Preserve original transaction currency and amount.
- Store the exchange rate used for historical conversion where needed.
- Create an ExchangeRateProvider interface.
- Support manual rates/offline fallback.
- Show last rate update timestamp.
- Never silently combine raw values from different currencies.
- Correctly handle transfers between accounts with different currencies.
- Allow transfer-side amounts/rates to represent real bank conversion differences.
- Define rounding rules per currency/monetary model.

## Testing
Add extensive tests for conversions, historical analytics, rounding, rate changes and cross-currency transfers.
