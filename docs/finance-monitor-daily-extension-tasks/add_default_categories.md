# TASK — Add Comprehensive Default Financial Categories to Fin Pulse

## Objective

Expand the existing **Fin Pulse Android app** with a comprehensive set of built-in financial categories and subcategories based on the provided reference screenshot.

The categories must be suitable for real daily personal-finance tracking and integrated with the existing:

- Transactions
- Budgets
- Analytics
- Charts
- Search/filtering
- Light/Dark themes
- Localization system
- User-created categories

Do not simply hardcode a long UI list.

Implement a maintainable **default category catalog** that can evolve in future versions without duplicating categories or destroying user customization.

---

# 1. Inspect Existing Architecture First

Before implementation, inspect:

- Existing Category entity/model
- Database schema
- Room entities/DAOs if used
- Category repository
- Transaction → Category relationships
- Existing default categories
- Custom/user-created categories
- Category icons
- Category colors
- Localization
- Existing database migrations
- Seed/prepopulation mechanism
- Budget/category relationships
- Analytics/category grouping

Reuse the existing architecture whenever possible.

Do NOT replace the current category system unless necessary.

---

# 2. Category Model

Every built-in category should have a stable internal identity independent of its translated display name.

Conceptually:

```kotlin
Category(
    id = ...,
    key = "groceries",
    nameResource = R.string.category_groceries,
    type = EXPENSE,
    icon = ...,
    color = ...,
    isDefault = true
)
```

The exact implementation must follow the existing Fin Pulse architecture.

### Critical Rule

Never use the translated category name as the database identity.

For example:

```text
key = "groceries"
```

may display as:

```text
English → Groceries
Russian → Продукты
German → Lebensmittel
French → Courses
```

but it must remain the same category internally.

---

# 3. Default EXPENSE Categories

Add a comprehensive built-in expense catalog.

## Home & Utilities

- Rent
- Mortgage
- Home
- Utilities
- Electricity
- Water
- Gas
- Heating
- Internet
- Mobile Phone
- TV
- Home Maintenance
- Home Repairs
- Furniture
- Household Supplies
- Cleaning
- Security
- Property Tax

## Food & Dining

- Groceries
- Restaurants
- Cafes
- Fast Food
- Food Delivery
- Lunch
- Snacks
- Drinks
- Bakery
- Other Food

## Transportation

- Public Transport
- Taxi
- Fuel
- Parking
- Car Maintenance
- Car Repair
- Car Insurance
- Car Wash
- Tolls
- Car Rental
- Bicycle
- Other Transportation

## Shopping

- Shopping
- Clothing
- Shoes
- Accessories
- Electronics
- Appliances
- Online Shopping
- AliExpress
- Amazon
- Google Play
- App Store
- Games
- Gifts
- Other Shopping

## Health

- Health
- Pharmacy
- Medicine
- Doctor
- Dentist
- Hospital
- Medical Tests
- Vitamins
- Health Insurance
- Fitness
- Gym
- Other Health

## Entertainment

- Entertainment
- Cinema
- Music
- Games
- Events
- Concerts
- Nightlife
- Hobbies
- Books
- Streaming Services
- Other Entertainment

## Subscriptions & Digital Services

- Subscriptions
- Netflix
- Spotify
- YouTube
- Cloud Storage
- Software
- Mobile Apps
- Digital Services
- Other Subscriptions

## Personal Care

- Personal Care
- Hairdresser / Barber
- Beauty
- Cosmetics
- Spa
- Massage
- Other Personal Care

## Family & Children

- Children
- Kindergarten
- School
- Education
- Courses
- Toys
- Childcare
- Pocket Money
- Family Support
- Other Family Expenses

## Pets

- Pets
- Pet Food
- Veterinary
- Pet Supplies
- Grooming
- Other Pet Expenses

## Travel

- Travel
- Flights
- Hotels
- Train
- Bus
- Taxi / Transfers
- Car Rental
- Travel Insurance
- Vacation
- Tours & Activities
- Other Travel

## Education

- Education
- School
- University
- Courses
- Books
- Online Courses
- Training
- Other Education

## Financial Expenses

- Bank Fees
- Interest
- Loan Payment
- Credit Card Payment
- Insurance
- Taxes
- Fines
- Currency Exchange Fees
- Financial Services
- Other Financial Expenses

## Loans & Debt

- Loan
- Mortgage Payment
- Consumer Loan
- Car Loan
- Credit Card
- Debt Repayment
- Other Debt

## Charity & Donations

- Charity
- Donations
- Gifts to Others
- Religious Donations
- Other Donations

## Other Expenses

- Other Expense
- Uncategorized Expense

---

# 4. Default INCOME Categories

Income must have its own category set.

Add:

## Employment

- Salary
- Bonus
- Overtime
- Commission
- Tips
- Reimbursement

## Business & Freelance

- Freelance
- Business Income
- Consulting
- Contract Work
- Side Job
- Other Business Income

## Investments

- Investment Income
- Dividends
- Interest Income
- Capital Gains
- Crypto Income
- Rental Income

## Government / Benefits

- Benefits
- Pension
- Scholarship
- Social Payments
- Tax Refund

## Other Income

- Gifts Received
- Cashback
- Refund
- Sale of Items
- Prize / Winnings
- Other Income
- Uncategorized Income

---

# 5. Transfers Must Be Separate

If Fin Pulse supports account-to-account transfers, do NOT model transfers as ordinary income or expense.

Use a dedicated transaction type such as:

```text
TRANSFER
```

where supported by the existing architecture.

Possible transfer categories/actions:

- Transfer
- Account Transfer
- Cash Withdrawal
- Cash Deposit

Avoid artificially increasing income/expense analytics when money simply moves between the user's own accounts.

---

# 6. Category Hierarchy

If the current architecture supports parent/child categories, organize categories hierarchically.

Example:

```text
Food & Dining
├── Groceries
├── Restaurants
├── Cafes
├── Fast Food
├── Food Delivery
└── Other Food
```

Another example:

```text
Transportation
├── Public Transport
├── Taxi
├── Fuel
├── Parking
├── Car Maintenance
└── Car Insurance
```

If hierarchical categories are NOT currently supported, do not perform a risky database redesign solely for this task.

Instead, preserve the flat model and introduce a safe grouping mechanism only if compatible with the existing architecture.

---

# 7. Icons

Every default category must have an appropriate icon.

Examples:

```text
Groceries → shopping cart/basket
Restaurant → restaurant
Cafe → coffee
Home → home
Electricity → bolt
Water → water/drop
Internet → network/wifi
Phone → phone
Fuel → fuel pump
Parking → parking
Taxi → taxi/car
Health → health
Pharmacy → medication
Gym → fitness
Travel → airplane
Hotel → hotel
Salary → wallet/money
Investment → trending/chart
Education → school
Pets → pets
Gift → gift
Entertainment → movie
```

Use the project's existing icon system.

Do not introduce a huge new icon dependency if suitable icons already exist.

Icons must render correctly in both Light and Dark themes.

---

# 8. Category Colors

Assign sensible colors to built-in categories.

Colors should help users visually identify categories in:

- Transaction lists
- Charts
- Analytics
- Budget screens
- Category selector

Do not assign random colors at runtime.

Default category colors must be deterministic.

Ensure sufficient visibility in both:

- Light theme
- Dark theme

Do not make financial meaning dependent only on color.

---

# 9. Localization

All built-in category names must use Android localization resources.

Fin Pulse currently supports:

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

Add every new category to every supported locale.

For example:

```xml
<string name="category_groceries">Groceries</string>
<string name="category_restaurants">Restaurants</string>
<string name="category_salary">Salary</string>
```

Russian examples:

```xml
<string name="category_groceries">Продукты</string>
<string name="category_restaurants">Рестораны</string>
<string name="category_salary">Зарплата</string>
```

Use natural financial terminology in every language.

Do not translate brand names such as:

- Netflix
- Spotify
- AliExpress
- Google Play

unless a locale convention clearly requires a different display form.

---

# 10. Custom Categories

Users must continue to be able to create their own categories.

Clearly distinguish internally:

```text
DEFAULT CATEGORY
CUSTOM CATEGORY
```

Custom categories must:

- Keep the user-entered name
- NOT be automatically translated
- Support icons/colors if currently supported
- Work in transactions
- Work in budgets
- Work in analytics
- Survive application updates

Never overwrite custom categories while updating the built-in catalog.

---

# 11. Default Category Seeding

Implement safe, idempotent default-category initialization.

This is critical.

Launching the app multiple times must NOT create:

```text
Groceries
Groceries
Groceries
```

Use stable IDs/keys for built-in categories.

Conceptually:

```text
expense.groceries
expense.restaurant
expense.transport.fuel
income.salary
income.freelance
```

Seed missing default categories only.

---

# 12. Existing Users / Migration

The feature must work for both:

### New installations

Create the complete default catalog automatically.

### Existing installations

Safely add categories introduced by the update without:

- Deleting existing categories
- Duplicating categories
- Breaking transaction relationships
- Reassigning existing transactions incorrectly
- Resetting category colors
- Removing custom categories

If a database migration is required, implement a proper migration.

Do NOT use destructive migration.

---

# 13. Legacy Category Matching

Inspect existing Fin Pulse categories before seeding.

If the application already has categories equivalent to:

```text
Food
Groceries
Transport
Salary
Shopping
```

do not blindly create duplicates.

Determine whether existing built-in categories can be assigned stable default keys safely.

Do not automatically merge two user-created categories merely because their names look similar.

---

# 14. Category Selector UX

Improve the category selector if necessary to handle the expanded catalog.

Users should not have to scroll through hundreds of unstructured rows.

Support appropriate UX such as:

- Expense / Income tabs
- Grouped categories
- Search
- Recently used
- Frequently used

Example:

```text
Select Category

[ Expense ] [ Income ]

Search categories...

Frequently Used
Groceries
Restaurants
Fuel
Shopping

Food & Dining
Groceries
Restaurants
Cafe
Food Delivery

Transportation
Fuel
Taxi
Public Transport
Parking
```

Reuse existing Fin Pulse UI patterns.

---

# 15. Category Search

Search should work against localized category display names.

For example, when Russian is active:

```text
"зарп"
```

should be able to find:

```text
Зарплата
```

Do not require users to search using the internal English category key.

---

# 16. Frequently Used Categories

If the architecture can support this cleanly, expose frequently used categories based on actual transaction usage.

Do not create fake/random usage data.

A simple ranking by transaction count is sufficient unless an existing analytics mechanism provides something better.

This should not block the core category implementation if it would require major unrelated architecture changes.

---

# 17. Analytics Integration

All new categories must work correctly with existing analytics.

Verify:

- Spending by category
- Income by category
- Monthly expenses
- Monthly income
- Budget analytics
- Pie/donut charts
- Trend charts
- Filters
- Reports

Category names displayed in analytics must use the current application locale.

Historical transactions must remain valid.

---

# 18. Budget Integration

Default expense categories must be selectable when creating budgets.

Examples:

```text
Groceries Budget
Restaurant Budget
Entertainment Budget
Transport Budget
Shopping Budget
```

Do not allow inappropriate income-only categories in an expense budget unless the existing product intentionally supports that behavior.

---

# 19. Transaction Integration

When adding/editing a transaction:

### Expense

Show expense categories.

### Income

Show income categories.

### Transfer

Use transfer handling rather than expense/income categories where supported.

Changing transaction type should update available categories appropriately.

Never silently change the amount or account when category selection changes.

---

# 20. Delete/Edit Behavior

Built-in categories should not be accidentally destroyed if doing so would break historical transaction references.

Follow the current application's category behavior.

Prefer safe behavior such as:

- Built-in category → hide/disable
- Custom category → editable/deletable

when compatible with the existing architecture.

Never delete historical transactions when deleting a category.

---

# 21. User Customization

If the existing app supports category customization, preserve it.

Users may customize:

- Icon
- Color
- Display/order
- Visibility

Do not reset user customization every time default categories are synchronized.

---

# 22. Ordering

Use a deterministic, user-friendly default order.

For expenses, prioritize commonly used categories such as:

```text
Food & Dining
Housing
Transportation
Shopping
Health
Subscriptions
Entertainment
Personal Care
Family
Travel
Education
Financial Expenses
Other
```

Income should similarly prioritize:

```text
Salary
Business/Freelance
Investments
Benefits
Other Income
```

If the app supports manual reordering, preserve the user's custom order.

---

# 23. Light / Dark Theme Compatibility

Fin Pulse supports Light and Dark themes.

Verify:

- Category icons
- Category colors
- Category selector
- Search
- Group headers
- Selected category
- Unselected category
- Charts
- Transaction rows

in both themes.

Do not introduce hardcoded white/black backgrounds for the new UI.

---

# 24. Performance

The larger category catalog must not cause noticeable UI lag.

Use:

- Stable IDs
- Efficient database queries
- Lazy lists in Compose where appropriate
- Efficient filtering/search

Do not repeatedly recreate or insert default categories during normal UI rendering.

---

# 25. Tests

Add/update tests covering:

### Seeding

- Defaults created on first initialization
- Initialization is idempotent
- Repeated startup does not duplicate defaults

### Migration

- Existing users retain transactions
- Custom categories remain intact
- New default categories are added
- Existing category references remain valid

### Transaction types

- Expense shows expense categories
- Income shows income categories
- Transfers behave correctly

### Localization

- Built-in category display name follows current locale
- Custom category names remain unchanged

### Repository/database

- Stable category keys remain unique

---

# 26. Build & Validation

After implementation:

1. Build the Android project.
2. Run unit tests.
3. Run database migration tests where available.
4. Run instrumentation tests where available.
5. Validate all localization resources.
6. Verify new installation behavior.
7. Verify upgrade behavior with an existing database.
8. Verify category search.
9. Verify expense/income filtering.
10. Verify transaction creation/editing.
11. Verify budget integration.
12. Verify analytics.
13. Verify Light theme.
14. Verify Dark theme.

Fix all regressions introduced by this task.

---

# Constraints

Do NOT:

- Delete existing user categories.
- Delete transactions.
- Reset financial data.
- Translate user-created category names.
- Use localized names as database identifiers.
- Duplicate categories every startup.
- Couple categories to a specific currency.
- Change transaction amounts.
- Break historical transaction references.
- Use destructive database migrations.
- Rewrite unrelated architecture.
- Hardcode the entire catalog directly inside UI composables.
- Change backend contracts unnecessarily.

---

# Definition of Done

The task is complete when:

- Fin Pulse contains a comprehensive built-in category catalog.
- Expense and income categories are properly separated.
- Transfers are handled separately where supported.
- Every category has a stable internal identity.
- Default category seeding is idempotent.
- Existing users can upgrade safely.
- User-created categories remain untouched.
- Categories are localized in all 12 supported UI languages.
- Category icons/colors work in Light and Dark themes.
- Category search works.
- Transactions use the correct categories.
- Budgets integrate with expense categories.
- Analytics correctly aggregate categories.
- Existing financial data remains intact.
- Project builds successfully.
- Tests pass.

# Final Report

After completion report:

1. Default expense categories added.
2. Default income categories added.
3. Stable category-key strategy.
4. Database/schema changes.
5. Migration strategy.
6. Seeding strategy.
7. Icons/colors added.
8. Localization files modified.
9. Category selector changes.
10. Transaction integration changes.
11. Budget integration changes.
12. Analytics integration changes.
13. Tests added/updated.
14. Build/test results.
15. Any remaining manual QA recommendations.