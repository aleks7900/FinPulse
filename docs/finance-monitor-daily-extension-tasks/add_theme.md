# TASK — Implement Light / Dark Theme System for Fin Pulse Android App

## Objective

Implement a complete, production-ready **Light / Dark / System Default theme system** for the existing **Fin Pulse Android application**.

Fin Pulse is a financial monitoring application. The theme system must preserve excellent readability of financial data, charts, balances, transactions, analytics, and positive/negative indicators.

The result should feel like a polished modern fintech application.

Support:

- System Default
- Light
- Dark

Do not redesign the application or modify financial/business logic.

---

# 1. Audit Existing Theme Architecture

Before making changes, inspect the entire project.

Determine:

- Jetpack Compose / XML Views / hybrid architecture
- Material 2 or Material 3
- Existing `Theme`
- Existing `ColorScheme`
- Typography
- Shapes
- Existing theme state
- Existing hardcoded colors
- DataStore/SharedPreferences/settings architecture
- Navigation architecture
- System bar handling
- Splash screen configuration

Search the project for hardcoded colors such as:

```kotlin
Color.White
Color.Black
Color.Gray
Color(...)
```

and XML colors such as:

```xml
android:textColor
android:background
android:tint
```

Determine which colors are application-theme colors and which carry financial semantic meaning.

Do NOT blindly replace every explicit color.

---

# 2. Theme Modes

Implement exactly three modes:

```text
System Default
Light
Dark
```

### System Default

Follow the current Android system theme.

### Light

Force Fin Pulse light mode regardless of system theme.

### Dark

Force Fin Pulse dark mode regardless of system theme.

Theme changes should take effect immediately.

---

# 3. Persist Theme Preference

Persist the user's selection using the project's existing settings architecture.

Prefer DataStore if it is already used or appropriate.

Example internal values:

```text
SYSTEM
LIGHT
DARK
```

Theme selection must survive:

- Activity recreation
- App restart
- Process recreation
- Device restart

Avoid a visible wrong-theme flash during startup.

---

# 4. Settings

Add an Appearance section to Settings if one does not already exist.

Example:

```text
Appearance

Theme
System Default
Light
Dark
```

Use a UI consistent with the existing Fin Pulse settings design.

A radio dialog, bottom sheet, or dedicated theme-selection screen is acceptable depending on existing architecture.

Do not introduce an unrelated design system.

---

# 5. Light Theme

Create a professional fintech-oriented light theme.

It should feel:

- Clean
- Premium
- Trustworthy
- Modern
- Data-focused
- Comfortable for prolonged use

Avoid making every surface pure white.

Use appropriate differentiation between:

- Main background
- Cards
- Elevated surfaces
- Navigation
- Dialogs
- Input fields
- Analytics panels

Financial information should remain the primary visual focus.

---

# 6. Dark Theme

Create or improve a polished dark theme.

Avoid simply using:

```text
#000000
```

for every background.

Use layered dark surfaces where appropriate so cards and sections remain distinguishable.

Ensure excellent readability for:

- Balances
- Transaction amounts
- Account names
- Categories
- Charts
- Secondary information
- Dates
- Labels

If Fin Pulse already has a polished dark palette, preserve and integrate it rather than replacing it with generic Material defaults.

---

# 7. Semantic Color System

Centralize reusable theme colors.

For Compose, prefer semantic colors such as:

```kotlin
MaterialTheme.colorScheme.background
MaterialTheme.colorScheme.surface
MaterialTheme.colorScheme.surfaceVariant
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.secondary
MaterialTheme.colorScheme.onBackground
MaterialTheme.colorScheme.onSurface
MaterialTheme.colorScheme.onSurfaceVariant
MaterialTheme.colorScheme.outline
MaterialTheme.colorScheme.error
```

Individual screens should not independently decide whether they need white or black based on dark mode.

The theme should provide the correct semantic colors.

---

# 8. Financial Semantic Colors

This is critical.

Some colors represent **financial meaning**, not application theme.

Examples:

```text
Positive / Income
Negative / Expense
Profit
Loss
Gain
Decrease
Warning
Budget exceeded
```

Do NOT blindly map these to generic theme colors.

Create semantic financial colors where appropriate, for example conceptually:

```text
financialPositive
financialNegative
financialWarning
financialNeutral
```

Provide suitable variants for both Light and Dark themes.

A positive amount and negative amount must remain immediately distinguishable in both themes.

Do not assume that green/red alone is sufficient for accessibility.

Where important, combine color with:

- `+` / `−`
- Icons
- Labels
- Direction indicators

Do not change the meaning of existing financial colors.

---

# 9. Dashboard

Carefully validate the main financial dashboard.

Check:

- Total balance
- Net worth
- Income
- Expenses
- Monthly spending
- Budget progress
- Savings
- Account cards
- Financial insights
- Recent transactions
- Quick actions

Cards must remain visually distinguishable from the page background in both themes.

Important numbers must maintain strong contrast.

---

# 10. Transactions

Audit transaction screens.

Ensure correct theming for:

- Transaction rows
- Merchant names
- Category names
- Dates
- Amounts
- Income
- Expenses
- Transfers
- Pending transactions
- Search
- Filters
- Dividers
- Selected states

Positive and negative transaction values must remain clear in both themes.

---

# 11. Charts and Analytics

Charts require special attention.

Audit:

- Line charts
- Bar charts
- Pie/donut charts
- Area charts
- Spending breakdowns
- Income vs Expenses
- Balance history
- Cash flow
- Net worth
- Budget charts

Theme chart components separately from normal UI surfaces where necessary.

Ensure:

- Axis labels remain readable.
- Grid lines are visible but subtle.
- Legends remain readable.
- Tooltips have sufficient contrast.
- Selected data points are visible.
- Chart colors remain distinguishable.
- Chart backgrounds integrate with the surrounding UI.

Do not simply invert chart colors.

---

# 12. Category Colors

If transaction categories have their own colors, preserve their semantic/category identity.

Examples:

```text
Food
Transport
Shopping
Entertainment
Housing
Health
Subscriptions
Salary
Investments
```

Do not automatically replace category colors with theme primary colors.

However, adjust rendering where necessary to ensure category colors remain visible against both light and dark surfaces.

---

# 13. Budget UI

Check:

- Budget progress
- Remaining amount
- Used amount
- Limits
- Warning thresholds
- Exceeded budgets

Ensure warnings remain visually clear in both themes.

Do not make an exceeded budget visually indistinguishable from a normal budget.

---

# 14. Accounts

Validate account cards and account details.

Examples:

- Bank accounts
- Cash
- Credit cards
- Savings
- Investments

Ensure:

- Balance visibility
- Account icons
- Card backgrounds
- Secondary information
- Currency labels

remain readable in both modes.

---

# 15. Components

Audit all reusable components:

- Top app bars
- Bottom navigation
- Navigation drawer
- Cards
- Buttons
- FABs
- Tabs
- Chips
- Filters
- Switches
- Sliders
- Radio buttons
- Text fields
- Search fields
- Dialogs
- Bottom sheets
- Snackbars
- Date pickers
- Loading states
- Empty states
- Error states
- Premium UI if present

Remove theme-specific hardcoded colors where appropriate.

---

# 16. System Bars

Correctly theme:

- Status bar
- Navigation bar
- Edge-to-edge areas
- Status bar icons
- Navigation bar icons

Light mode must not produce white icons on a light background.

Dark mode must not produce dark icons on a dark background.

Preserve existing edge-to-edge behavior.

---

# 17. Splash / Startup Theme

Make startup theme-aware.

Avoid:

```text
Dark theme
→ white splash
→ dark application
```

or:

```text
Light theme
→ black splash
→ light application
```

Use the persisted theme preference as early as safely possible.

Avoid theme flashing.

---

# 18. Theme Switching

Theme changes should apply immediately.

Do not restart the entire application unless technically necessary.

Preserve:

- Navigation state
- Current screen
- User input where possible
- Current financial data

Do not refetch financial information solely because the theme changed.

---

# 19. Accessibility

Check WCAG-style contrast principles for important financial information.

Pay special attention to:

- Small secondary text
- Chart labels
- Disabled states
- Transaction amounts
- Positive/negative values
- Budget warnings
- Text over colored cards

Do not rely solely on red vs green to communicate important financial states.

---

# 20. Localization

Fin Pulse supports multiple languages.

All new strings must use Android string resources.

Add translations for the theme UI to every currently supported locale, including the previously implemented:

- English
- Russian
- Spanish
- Portuguese (Brazil)
- German
- French
- Italian
- Polish
- Turkish
- Japanese
- Korean
- Simplified Chinese

Strings may include:

```text
Appearance
Theme
System Default
Light
Dark
```

Do not leave new theme settings untranslated.

---

# 21. Architecture

Maintain a single source of truth for the selected theme.

Conceptually:

```text
Persisted Theme Preference
        ↓
Settings Repository
        ↓
ViewModel / Observable State
        ↓
FinPulseTheme
        ↓
Entire Application UI
```

Adapt this to the project's existing architecture.

Do not introduce duplicate theme state in individual screens.

---

# 22. Dynamic Color

Do NOT automatically enable Material You dynamic colors.

First determine whether Fin Pulse has a defined brand palette.

If dynamic color would replace important Fin Pulse brand or financial semantic colors, keep the application's own color system.

If dynamic colors already exist, preserve their intended behavior unless there is a clear reason to change it.

---

# 23. Testing Matrix

Test:

```text
Android Light + System Default
Android Dark + System Default

Android Light + App Light
Android Dark + App Light

Android Light + App Dark
Android Dark + App Dark
```

Expected behavior:

```text
System Default → follows Android
Light → always light
Dark → always dark
```

Also verify persistence after process/application restart.

---

# 24. Visual QA

Inspect at minimum:

- Dashboard
- Accounts
- Account details
- Transactions
- Transaction details
- Add/Edit transaction
- Budgets
- Analytics
- Charts
- Categories
- Search
- Filters
- Financial goals
- Settings
- Notifications
- Dialogs
- Bottom sheets
- Date selectors
- Empty states
- Error states

Check every screen in both Light and Dark mode.

---

# 25. Build Validation

After implementation:

1. Build the project.
2. Run existing unit tests.
3. Run instrumentation/UI tests where available.
4. Run lint if configured.
5. Search again for inappropriate hardcoded theme colors.
6. Validate theme persistence.
7. Validate system-theme synchronization.
8. Validate system bars.
9. Validate startup behavior.
10. Validate charts in both themes.
11. Validate positive/negative financial states.
12. Validate all supported localization resources.

Fix any problems introduced by this task.

---

# Constraints

Do NOT:

- Change financial calculations.
- Change balances or transaction values.
- Modify backend APIs.
- Change database behavior unnecessarily.
- Redesign unrelated screens.
- Replace category colors unnecessarily.
- Couple theme selection with language or currency.
- Introduce unnecessary dependencies.
- Modify chart data.
- Change financial semantics.
- Translate user-generated data.
- Enable dynamic colors if they undermine Fin Pulse's visual identity.

Preserve existing application architecture and functionality.

---

# Definition of Done

The implementation is complete when:

- Light theme works across the entire app.
- Dark theme works across the entire app.
- System Default correctly follows Android.
- Theme selection persists.
- Switching themes works correctly.
- No obvious incorrect hardcoded theme colors remain.
- Financial semantic colors work in both themes.
- Positive/negative values remain clear.
- Charts are readable in both themes.
- Dashboard cards are readable in both themes.
- System bars are correctly themed.
- Startup does not produce obvious theme flashing.
- New strings are translated into all supported languages.
- Existing financial functionality remains unchanged.
- Project builds successfully.

# Final Report

When complete, provide:

1. Files created.
2. Files modified.
3. Theme architecture used.
4. Light and dark palettes introduced.
5. Financial semantic colors introduced/updated.
6. Hardcoded colors migrated.
7. Charts updated.
8. Settings/theme selector implementation.
9. Persistence mechanism.
10. Localization resources updated.
11. Build/test commands executed.
12. Build/test results.
13. Remaining areas requiring manual device visual QA.