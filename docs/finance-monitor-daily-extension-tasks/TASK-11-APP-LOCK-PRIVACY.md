# TASK-11 — App Lock, Biometrics & Privacy Mode

## Objective
Protect sensitive financial information during everyday phone usage.

## Requirements
- Biometric unlock using supported Android APIs.
- Optional device-credential fallback.
- Configurable automatic lock timeout.
- Lock after app returns from background according to policy.
- Optional FLAG_SECURE/screenshot protection.
- Privacy mode to mask balances.
- Hide sensitive values in recent-app previews where practical.
- Store secrets using Android Keystore-backed mechanisms.
- Never log sensitive financial values or credentials.

## UX
Security should not make rapid daily transaction entry unnecessarily painful. Provide sensible configurable behavior.
