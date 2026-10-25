# APP_CONTEXT (generated in T00)

## Build
- Gradle module of the app: `:app` (single module; `settings.gradle.kts` includes only `include(":app")`)
- applicationId / base package (this is <PKG>): `com.paolorossi.expensetracker` (`app/build.gradle.kts` namespace + applicationId)
- Main source root (<SRC>), e.g. app/src/main/java or app/src/main/kotlin: `app/src/main/java` (note: files sit in directories named `com/example/expensetracker/`, but every Kotlin file declares `package com.paolorossi.expensetracker…`)
- Debug source set exists (app/src/debug): No — `app/src` contains only `main`, `test`, `androidTest`
- minSdk / targetSdk / compileSdk: 26 / 35 / 35 (`app/build.gradle.kts` defaultConfig and android block)
- Kotlin version / AGP version: Kotlin `2.0.21` / AGP `8.7.3` (`gradle/libs.versions.toml`)
- Build files are .kts or .gradle; version catalog (libs.versions.toml) yes/no: `.kts` (`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`); version catalog yes (`gradle/libs.versions.toml`)
- Core library desugaring enabled (needed for java.time when minSdk < 26): No — not configured, and not needed because `minSdk = 26` (`java.time` available natively)
- JSON library already used (Gson / Moshi / kotlinx.serialization / none): none declared; the Android platform `org.json` API is used directly in `app/src/main/java/com/example/expensetracker/data/diagnostics/DiagnosticsReporter.kt`

## UI
- Toolkit: Jetpack Compose (`buildFeatures.compose = true` in `app/build.gradle.kts`)
- Material version (M2 / M3): Material 3 (`androidx.compose.material3:material3` via `androidx-compose-bom` 2024.12.01 in `gradle/libs.versions.toml`)
- Navigation library + file where routes are declared: `androidx.navigation:navigation-compose` 2.8.5; routes in `app/src/main/java/com/example/expensetracker/navigation/Routes.kt`, NavHost in `app/src/main/java/com/example/expensetracker/navigation/AppNavigation.kt`
- Activity count (single activity or not) and file of the main activity: 1 activity (single-activity app) — `MainActivity` extends `FragmentActivity`, in `app/src/main/java/com/example/expensetracker/MainActivity.kt`
- Home screen file: `app/src/main/java/com/example/expensetracker/ui/home/HomeScreen.kt` (+ `HomeViewModel.kt`)
- Add Expense screen file + the composable/class used for: date picker, category picker, vendor autocomplete: `app/src/main/java/com/example/expensetracker/ui/addtransaction/AddTransactionScreen.kt`; date picker = `DateField` in `app/src/main/java/com/example/expensetracker/ui/common/Components.kt` (Material3 `DatePickerDialog` at line 219); category picker = `CategoryPicker` in the same `ui/common/Components.kt` (line 115); vendor autocomplete = private `VendorAutocompleteField` in `AddTransactionScreen.kt` (line 122, `ExposedDropdownMenuBox` + `VendorSuggestions.filter`)
- Monthly Transactions screen file: `app/src/main/java/com/example/expensetracker/ui/transactions/TransactionsScreen.kt` (+ `TransactionsViewModel.kt`)
- Where an extra button could go on Home (top bar action, FAB menu, overflow menu): overflow menu — `HomeScreen` passes `actions = { IconButton(Icons.Filled.MoreVert) … DropdownMenu }` to `ScreenScaffold` (`ui/home/HomeScreen.kt` lines 58–94); an entry would be a new `DropdownMenuItem` there (bottom navigation items live in `navigation/AppNavigation.kt`)
- Biometric / sign-in gate: where it is, and how the rest of the app waits for it: `data/auth/BiometricGate.kt` (BiometricPrompt, BIOMETRIC_WEAK + DEVICE_CREDENTIAL) surfaced by `RootGate`/`LockedScreen` in `ui/auth/IdentificationScreen.kt`; the gate state machine is `RootViewModel` in `ui/auth/RootViewModel.kt` (`RootState.Loading → SignedOut → Locked → Ready`). `MainActivity.onCreate` renders `RootGate` for every state except `RootState.Ready`, and renders `AppNavigation` only when `Ready`

## Architecture
- DI: Hilt / Koin / manual (and the file where objects are created): manual service locator — `app/src/main/java/com/example/expensetracker/data/ServiceLocator.kt` (no DI framework). ViewModels are created by `app/src/main/java/com/example/expensetracker/ui/AppViewModelFactory.kt`
- ViewModel pattern + one example file: AndroidX `ViewModel` exposing an immutable UiState over `MutableStateFlow`/`StateFlow`; example `app/src/main/java/com/example/expensetracker/ui/addtransaction/AddTransactionViewModel.kt`
- Coroutines and Flow used: yes — `kotlinx-coroutines-android` 1.9.0, `callbackFlow` repositories, `StateFlow` UiState, `collectAsStateWithLifecycle()`
- Settings storage used (DataStore / SharedPreferences / none) + example file: none for app settings; `SharedPreferences` is used only for the diagnostics install id in `app/src/main/java/com/example/expensetracker/data/diagnostics/DiagnosticsCollector.kt` (line 46). No DataStore dependency

## Data
- Transaction model (file + every field with its type; especially the amount type and the date type): `app/src/main/java/com/example/expensetracker/data/model/Transaction.kt` — `Transaction(id: String = "", type: TransactionType, amountPence: Long, date: LocalDate, vendor: String, vendorId: String? = null, categoryId: String, isRecurringInstance: Boolean = false, recurringRuleId: String? = null, createdByUid: String, createdByName: String, createdAt: Instant? = null, updatedAt: Instant? = null)`. Amount = `Long` pence; date = `java.time.LocalDate`
- Category model (file + fields; how expense-only / income-only is represented): `app/src/main/java/com/example/expensetracker/data/model/Category.kt` — `Category(id: String = "", name: String, type: CategoryType, icon: String? = null, color: String? = null)`, with `enum class CategoryType { EXPENSE, INCOME }`. A category is always exactly one type; `CategoryRules` enforces the match
- Repository interface(s) (file + signatures) for: add transaction, query transactions by date range or by month, list categories, list vendors:
  - add transaction: `TransactionRepository.addTransaction(householdId: String, transaction: Transaction): String` in `app/src/main/java/com/example/expensetracker/data/repository/TransactionRepository.kt`
  - query by date range or by month: NOT FOUND in any repository. `TransactionRepository` only exposes `observeTransactions(householdId): Flow<List<Transaction>>` (all transactions ordered by `date` DESC). Month filtering is done in memory (`TransactionsViewModel`, `MonthlyTotalsCalculator`); range filtering for dedupe will have to be done the same way
  - list categories: `CategoryRepository.observeCategories(householdId: String): Flow<List<Category>>` in `app/src/main/java/com/example/expensetracker/data/repository/CategoryRepository.kt`
  - list vendors: `VendorRepository.observeVendors(householdId: String): Flow<List<Vendor>>` in `app/src/main/java/com/example/expensetracker/data/repository/VendorRepository.kt`
- Existing vendor Pascal-Case normaliser (file + function) or NOT FOUND: `app/src/main/java/com/example/expensetracker/domain/VendorNameFormatter.kt` — `VendorNameFormatter.toDisplayName(raw: String): String` (also `toKey(raw: String): String`)
- Existing "last used category per vendor" (file + function) or NOT FOUND: `app/src/main/java/com/example/expensetracker/domain/VendorSuggestions.kt` — `VendorSuggestions.categoryFor(vendor: Vendor, expenseCategoryIds: Set<String>): String?` reading `Vendor.lastCategoryId` (`data/model/Vendor.kt`); vendor matching is `VendorMatcher.bestMatch` in `domain/VendorMatcher.kt`
- Where vendor usage and the last-used category per vendor are RECORDED when an expense is saved (repository or ViewModel; file + function): repository — `VendorRepository.recordUsage(rawName: String, categoryId: String?)` in `app/src/main/java/com/example/expensetracker/data/repository/VendorRepository.kt` (Firestore merge writes `name`, `lastCategoryId`, `updatedAt`, `updatedBy`); called from `AddTransactionViewModel.save()` in `ui/addtransaction/AddTransactionViewModel.kt` (line 265) only for new expenses
- Repository `add` behaviour (waits for the server vs returns after the local write) and the function Add Expense calls to save: it awaits the server write — `FirestoreTransactionRepository.addTransaction` calls `collection(...).add(...).await()` (round trip, not fire-and-forget). Add Expense calls `transactionRepository.addTransaction(...)` from `AddTransactionViewModel.save()` (line 248)
- Current user / creator provider (file + function): `AuthRepository.currentUser: AuthUser?` in `app/src/main/java/com/example/expensetracker/data/auth/AuthRepository.kt` (`FirebaseGoogleAuthRepository`, backed by `FirebaseAuth.currentUser`); consumed as `ServiceLocator.authRepository.currentUser` by `AddTransactionViewModel` (`currentUserProvider`, line 58) and by the `FirestoreVendorRepository` constructor lambda in `data/ServiceLocator.kt`. `Transaction.createdByUid` / `createdByName` are stamped in `save()`
- Firestore offline persistence enabled/disabled: enabled — `ServiceLocator.init` sets `FirebaseFirestoreSettings.Builder().setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())` (`data/ServiceLocator.kt` lines 72–79)

## Testing and CI
- Unit test libraries in use (JUnit4/5, MockK/Mockito, Turbine, coroutines-test): JUnit 4 (`junit:junit:4.13.2`), MockK (`io.mockk:mockk:1.13.13`), `kotlinx-coroutines-test:1.9.0`; no Turbine, no Truth/AssertK, no JUnit 5
- Existing MainDispatcherRule or similar: file or NOT FOUND: NOT FOUND — `AddTransactionViewModelTest` calls `Dispatchers.setMain(StandardTestDispatcher())` / `resetMain()` in `@Before`/`@After` (`app/src/test/java/com/example/expensetracker/ui/addtransaction/AddTransactionViewModelTest.kt`)
- Compose UI test dependencies present: yes — `androidx.compose.ui:ui-test-junit4` + `ui-test-manifest` (`app/build.gradle.kts`), with tests in `app/src/androidTest/java/com/example/expensetracker/ui/`
- One existing unit test file to copy the style from: `app/src/test/java/com/example/expensetracker/domain/VendorNameFormatterTest.kt`
- CI workflow file(s): `.github/workflows/build.yml` (ktlintCheck, testDebugUnitTest, assembleDebug, Firestore emulator rules job) and `.github/workflows/release.yml`
- Exact command that runs all unit tests locally: `./gradlew testDebugUnitTest`
