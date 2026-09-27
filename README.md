# FinPulse — Modern Android Personal Finance Monitor

FinPulse is an institutional-grade, offline-first personal financial management platform built for Android. It is engineered with 100% Kotlin, Jetpack Compose, Material 3, Clean Architecture, and Coroutines/StateFlow.

---

## 🌟 Key Highlights & Engineering Standards

- **Safe Monetary Math (`Money`)**: Floating-point types (`Float`, `Double`) are prohibited for financial calculations. All values are encapsulated in a `Money` value object (`amountMinor: Long`, currency code: `String`, `BigDecimal` bridge with `RoundingMode.HALF_EVEN`).
- **Clean Architecture & MVI/MVVM**: Strict layer separation: `core`, `domain`, `data`, `presentation`. The domain layer has zero Android dependencies and executes pure business logic.
- **Offline-First Reactive State**: Room Database functions as the Single Source of Truth (`SSOT`), exposing reactive Kotlin `Flow<T>`. Any transaction or balance mutation instantly updates UI components across all screens.
- **Institutional Double-Entry Consistency**: Account transfers deduct from the source account and credit the destination account atomically. Transaction modifications or deletions automatically reverse previous amounts before applying updates.
- **Deterministic Financial Insights**: Built-in algorithmic engine detecting spending velocity anomalies, category inflation, budget overruns, and subscription burdens.
- **Comprehensive Debt Payoff Modeling**: Simulates debt freedom dates comparing **Debt Snowball** (lowest balance first) against **Debt Avalanche** (highest interest rate first).
- **Fintech Design System**: Premium dark/light themes, custom Canvas charts (Multi-segment Donut, Sparklines, Cash Flow Bar charts, segmented progress indicators), and privacy masking ("••••••").
- **Privacy & Security**: Native biometric prompt support, PIN protection with SHA-256 hashing, `FLAG_SECURE` window screenshot protection, and standard RFC-4180 CSV export.

---

## 📱 Feature Capabilities

### 1. Executive Dashboard
- **Total Balance & Available Funds**: Real-time aggregated net worth across accounts.
- **Cash Flow Pills**: Period income vs expenses.
- **Multi-Period Filtering**: Instant filtering for Week, Month, 3 Months, 6 Months, Year, and All-Time.
- **Portfolio Metrics Strip**: Savings, Investments, Outstanding Debt, and Savings Rate ($S / I \times 100\%$).
- **Smart Insights Banner**: Immediate alerts on budget danger, burn rate, or category surges.
- **Recent Transactions Ledger**: Instant transaction preview with quick status badges.

### 2. Transaction Management & Ledger
- **Types**: Income, Expense, Transfer, Refund.
- **Metadata**: Category, source account, destination account, merchant/payee, timestamp, tags, and notes.
- **Search & Filtering**: Real-time search across merchants, notes, descriptions, and tags. Multi-criteria bottom sheet for category, account, and sorting (date, amount).
- **Operations**: Create, edit, duplicate, and delete with automatic double-entry balance updates.

### 3. Accounts Portfolio
- **Account Types**: Cash, Bank Account, Credit Card, Savings Account, Investment Account, Digital Wallet, Loan, Other.
- **Aggregated View**: Total net worth across active accounts.
- **Fund Transfers**: Transfer funds between accounts with balance verification.
- **Account Archiving**: Archive dormant accounts without losing historical transaction records.

### 4. Granular Category Budgeting
- **Period Types**: Monthly and Weekly budgets.
- **Visual Thresholds**:
  - `< 70%`: Normal (Emerald)
  - `70% - 90%`: Informational / Elevated (Amber)
  - `90% - 100%`: Warning (Amber/Crimson)
  - `> 100%`: Exceeded (Crimson)
- **Spending Projection**: Linear regression forecasting month-end expenditure based on elapsed days:
  $$\text{Projected Spend} = \frac{\text{Spent MTD}}{\text{Days Elapsed}} \times \text{Days In Month}$$

### 5. Financial Analytics & Visualizations
- **Donut Chart**: Multi-segment Canvas donut chart of expenses by category with sweep animation.
- **Net Cash Flow Card**: Income vs Expenses variance.
- **Merchant Leaderboard**: Ranking top merchants by transaction frequency and total expenditure.
- **Key Statistics**: Average daily burn rate, largest single expense, and savings rate.

### 6. Subscriptions & Recurring Bills
- **Obligations Engine**: Frequency tracking (Daily, Weekly, Bi-Weekly, Monthly, Quarterly, Yearly).
- **Annualized Burden**: Automatic calculation of monthly and annualized subscription costs.
- **Next Due Dates**: Upcoming timeline view.
- **Active / Cancelled Switcher**: Pause subscriptions and gauge impact on monthly cash flow.

### 7. Financial Savings Goals
- **Milestone Tracking**: Target amount, current amount, and target deadline date.
- **Monthly Savings Calculator**:
  $$\text{Suggested Monthly Contribution} = \frac{\text{Target Amount} - \text{Current Saved}}{\text{Months Remaining}}$$
- **Fast Deposit Dialog**: Add funds directly to any goal.

### 8. Investments & Asset Portfolio
- **Asset Classes**: Stocks, ETFs, Cryptocurrencies, Bonds, Real Estate, Commodities, Cash equivalents.
- **Valuation & Metrics**: Quantity, purchase price, current market price, total invested, current value, total profit/loss, and percentage return ($P/L\%$).

### 9. Debt Payoff Planner
- **Debt Classes**: Credit Cards, Personal Loans, Mortgages, Student Loans, Auto Loans.
- **Strategy Comparison**:
  - **Snowball**: Lowest balance first (psychological momentum).
  - **Avalanche**: Highest interest rate first (interest minimization).
- **Amortization Modeling**: Estimated months to debt-free status and record payment dialog.

### 10. Security & Privacy
- **Biometric Authentication**: Fingerprint and face biometric unlock.
- **PIN Protection**: 4-6 digit passcode with SHA-256 secure hashing.
- **Screenshot Protection**: `WindowManager.LayoutParams.FLAG_SECURE` preventing OS recents screenshots.
- **Privacy Mode**: One-tap toggle to mask balances across the interface.

### 11. Data Portability & Export
- **CSV Export**: Standard RFC-4180 export compatible with Excel, Google Sheets, and accounting software.
- **JSON Backup & Restore**: Full local snapshot of all database entities.

### 12. Interactive Onboarding Flow
- **4-Step Wizard**:
  1. Welcome & Architecture intro
  2. Base Currency selection (USD, EUR, GBP, JPY, CAD, AUD, etc.)
  3. First Account setup (name & starting balance)
  4. Initial Category Budget setup
  5. One-tap skip option

---

## 🏗️ Technology Stack

| Layer | Technology |
|:---|:---|
| **Language** | Kotlin 2.0.21 |
| **UI Framework** | Jetpack Compose (BOM 2024.10.01) |
| **Design System** | Material 3 + Custom Fintech Theme & Canvas Charts |
| **Architecture** | Clean Architecture + MVI / MVVM with Unidirectional Data Flow |
| **State Management** | Kotlin Coroutines 1.9.0 + StateFlow / SharedFlow |
| **Local Database** | Room 2.6.1 + KSP 2.0.21-1.0.28 (Indexed SQLite) |
| **Preferences & Security** | AndroidX DataStore Preferences 1.1.1 + Android Keystore SHA-256 |
| **Navigation** | AndroidX Navigation Compose 2.8.5 |
| **Build System** | Gradle 8.13 + AGP 8.7.2 + Version Catalog (`libs.versions.toml`) |
| **Compatibility** | Min SDK 26 (Android 8.0), Target SDK 36 (Android 16), Java 17 |

---

## 📂 Project Structure

```
FinPulse/
 ├── gradle/
 │    └── libs.versions.toml                     // Dependency Version Catalog
 ├── app/
 │    ├── build.gradle.kts                       // App build configuration
 │    └── src/
 │         ├── main/
 │         │    ├── AndroidManifest.xml          // Permissions (Biometrics, Notifications)
 │         │    └── java/com/finpulse/app/
 │         │         ├── FinPulseApplication.kt  // App container & default seeder
 │         │         ├── MainActivity.kt         // Theme host & FLAG_SECURE controller
 │         │         │
 │         │         ├── core/
 │         │         │    ├── model/             // Money, CurrencyInfo, TimePeriod
 │         │         │    ├── designsystem/      // FinPulseTheme, Colors, Typography
 │         │         │    ├── ui/                // Canvas Charts, BalanceCard, TransactionItem
 │         │         │    ├── database/          // Room DB, 8 Entities, 8 DAOs
 │         │         │    └── datastore/         // UserPreferencesDataStore
 │         │         │
 │         │         ├── domain/
 │         │         │    ├── model/             // Pure domain entities (Account, Transaction...)
 │         │         │    ├── repository/        // Clean repository contracts
 │         │         │    ├── usecase/           // DashboardSummary, EvaluateBudgetStatus
 │         │         │    └── engine/            // InsightsEngine, DebtPayoffEngine
 │         │         │
 │         │         ├── data/
 │         │         │    ├── mapper/            // Entity <-> Domain bidirectional mappers
 │         │         │    └── repository/        // Repositories with double-entry balance sync
 │         │         │
 │         │         ├── di/
 │         │         │    └── AppContainer.kt    // Dependency injection container
 │         │         │
 │         │         └── presentation/
 │         │              ├── navigation/        // FinPulseNavHost, Routes, BottomNavBar
 │         │              ├── onboarding/        // Multi-step onboarding wizard
 │         │              ├── dashboard/         // Main DashboardScreen & ViewModel
 │         │              ├── accounts/          // AccountsScreen & ViewModel
 │         │              ├── transactions/      // TransactionsScreen & ViewModel
 │         │              ├── budgets/           // BudgetsScreen & ViewModel
 │         │              ├── analytics/         // AnalyticsScreen & ViewModel
 │         │              ├── recurring/         // RecurringScreen & ViewModel
 │         │              ├── goals/             // GoalsScreen & ViewModel
 │         │              ├── investments/       // InvestmentsScreen & ViewModel
 │         │              ├── debt/              // DebtScreen & ViewModel
 │         │              ├── categories/        // CategoriesScreen
 │         │              ├── insights/          // InsightsScreen feed
 │         │              ├── search/            // SearchScreen
 │         │              ├── security/          // SecurityScreen
 │         │              ├── export/            // ExportScreen
 │         │              └── more/              // MoreHubScreen
 │         │
 │         └── test/java/com/finpulse/app/
 │              ├── domain/
 │              │    ├── MoneyTest.kt            // Precision arithmetic unit tests
 │              │    ├── FinancialCalculationTest.kt // Debt amortization & goals math tests
 │              │    └── EnginesTest.kt          // Dashboard, Budget, & Payoff engine tests
```

---

## 🚀 Building & Testing

### Prerequisites
- JDK 17 (e.g. Amazon Corretto 17)
- Android SDK (API 34/36)

### Run Unit Tests
```bash
.\gradlew.bat testDebugUnitTest
```

### Build Debug APK
```bash
.\gradlew.bat assembleDebug
```
The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Build Production Release Android App Bundle (AAB)
```bash
.\gradlew.bat bundleRelease
```
The optimized, R8-minified AAB will be located at:
```
app/build/outputs/bundle/release/app-release.aab
```

### Install onto Device or Emulator
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 Google Play Publication & Store Assets

Complete publication materials, legal compliance documents, and graphical assets are prepared and ready for submission:

- **Publication Guide:** [docs/GOOGLE_PLAY_PUBLICATION_GUIDE.md](file:///c:/Users/aleks/.gemini/antigravity-ide/scratch/FinPulse/docs/GOOGLE_PLAY_PUBLICATION_GUIDE.md)
- **Data Safety Form Answers:** [docs/DATA_SAFETY_SPECIFICATION.md](file:///c:/Users/aleks/.gemini/antigravity-ide/scratch/FinPulse/docs/DATA_SAFETY_SPECIFICATION.md)
- **Privacy Policy:** [docs/PRIVACY_POLICY.md](file:///c:/Users/aleks/.gemini/antigravity-ide/scratch/FinPulse/docs/PRIVACY_POLICY.md)
- **Terms of Service:** [docs/TERMS_OF_SERVICE.md](file:///c:/Users/aleks/.gemini/antigravity-ide/scratch/FinPulse/docs/TERMS_OF_SERVICE.md)
- **Store Listing Copy:** [distribution/play_store/listing/](file:///c:/Users/aleks/.gemini/antigravity-ide/scratch/FinPulse/distribution/play_store/listing/) (Title, short description, full description, release notes)
- **Store Graphics:** [distribution/play_store/graphics/](file:///c:/Users/aleks/.gemini/antigravity-ide/scratch/FinPulse/distribution/play_store/graphics/) (512x512 App Icon, 1024x500 Feature Graphic)
- **Promotional Screenshots:** [distribution/play_store/screenshots/phone/](file:///c:/Users/aleks/.gemini/antigravity-ide/scratch/FinPulse/distribution/play_store/screenshots/phone/) (5 high-res 1080x1920 phone screenshots)

---

## 🔮 Future Integration Hooks

The repository interfaces (`AccountRepository`, `TransactionRepository`, `InvestmentRepository`) and engine abstractions are deliberately isolated so external financial APIs can be attached without altering business rules:
- **Open Banking / Plaid**: Attach remote sync to `AccountRepository` and `TransactionRepository` using WorkManager background workers.
- **Live Market Feeds**: Implement remote quote fetching (AlphaVantage, Yahoo Finance, Binance) inside `InvestmentRepository`.
- **AI Financial Advisory**: Replace or augment `InsightsEngine` with an LLM prompt pipeline that consumes `DashboardSummary` and generates personalized coaching.
