# PROGRESS LOG

The agent appends one entry per finished step, newest at the bottom. Do not rewrite old entries.

## Entry format

```
### <YYYY-MM-DD> TASK_xx Step n - <short title>
- Files changed: <list, or "none">
- Build: PASS / FAIL (<error if fail>)
- Tests: PASS / FAIL / NOT RUN (<which>)
- Notes / open questions: <anything the next session must know>
- Status: DONE / BLOCKED
```

## Log

### 2026-10-19 TASK_00 Step 7 - Review complete, PROJECT_MAP.md created
- Files changed: docs/agent-tasks/PROJECT_MAP.md (new)
- Build: NOT RUN (gradle not available in this environment)
- Tests: NOT RUN (gradle not available in this environment)
- Notes / open questions: AGENTS.md describes Hilt/Room/Retrofit/feature modules, but the repo is a single :app module with manual ServiceLocator, Firestore, Google Sign-In via Credential Manager. PROJECT_MAP.md records what the code actually contains.
- Status: DONE

### 2026-10-20 TASK_01 Steps A1-A3 - Navigation bug investigation (Part A)
- Files changed: docs/agent-tasks/bug-nav-report.md (new); app/src/main/java/com/example/expensetracker/navigation/AppNavigation.kt (added NAVDBG-TEMP DisposableEffect + imports for Log, NavController, DisposableEffect)
- Build: PASS (./gradlew assembleDebug produced app-debug.apk)
- Tests: NOT RUN
- Notes / open questions: Step A4 (device reproduction / NAVDBG log capture) could not be performed — no emulator or connected device available. Static analysis points to C1 (saveState=true/restoreState=true in navigateTo at AppNavigation.kt:76-77) as the likely root cause. A secondary finding: Routes.ADD_EXPENSE="expense-entry" and Routes.ADD_INCOME="add-income" do not match NavHost composable routes "add/expense"/"add/income" (Routes.kt:14-15 vs AppNavigation.kt:151,186). Step B not started per instructions.
- Status: DONE (Part A complete, awaiting Step A4 device log before Step B)

### 2026-10-20 TASK_01 Step B - Fix navigation bug (Part B)
- Files changed:
  - app/src/main/java/com/example/expensetracker/navigation/AppNavigation.kt — Home tab now pops back to Home (`popBackStack(HOME, inclusive=false)` with navigate fallback); other tabs use `popUpTo(findStartDestination()) + launchSingleTop`; removed `saveState=true` / `restoreState=true`; removed NAVDBG-TEMP debug block and its imports; added `viewModelFactory` parameter (default `AppViewModelFactory`) as a test seam; bottom bar now shown on the Add screens and `selectedRoute` derives from the real destination.
  - app/src/main/java/com/example/expensetracker/navigation/Routes.kt — `ADD_EXPENSE="add/expense"`, `ADD_INCOME="add/income"` (match the NavHost composables); removed unused `ADD` pattern.
  - app/src/main/java/com/example/expensetracker/ui/home/HomeScreen.kt — added `Modifier.testTag("home_screen")`.
  - app/src/androidTest/java/com/example/expensetracker/navigation/AppNavigationTest.kt (new) — renders the real `AppNavigation` with fake ViewModels and covers Expense→Income→Home, Income→Expense→Home, and Transactions→Home.
- Build: PASS (`./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest`; `./gradlew ktlintCheck`).
- Tests: PASS unit (`testDebugUnitTest`). Instrumented `AppNavigationTest` compiles but was NOT executed — no emulator/device available in this environment; run `./gradlew connectedDebugAndroidTest`.
- Notes / open questions: Root cause fixed = C1 (saveState/restoreState) plus C5 route mismatch. Pre-existing, out-of-scope navigation bug still present: `Routes.edit()` returns `"add/<type>&transactionId=..."` and `Routes.confirmRecurring()` returns `"add/expense&ruleId=..."` (uses `&` instead of `?`), so editing a transaction / confirming a recurring occurrence will not resolve to the `add/expense` / `add/income` composables. Recommend a follow-up task. Acceptance test 1–5 still needs a manual/emulator run.
- Status: DONE (Step B complete; instrumented test not run)

### 2026-10-20 TASK_01 follow-up - Fix edit / confirmRecurring route builders
- Files changed:
  - app/src/main/java/com/example/expensetracker/navigation/Routes.kt — added `ADD_EXPENSE_ROUTE` / `ADD_INCOME_ROUTE` NavHost patterns with optional query arguments; `edit()` and `confirmRecurring()` now use `?` instead of `&`.
  - app/src/main/java/com/example/expensetracker/navigation/AppNavigation.kt — add screens registered with the new patterns; `currentRoute` normalized with `substringBefore('?')` so tab selection still works.
  - app/src/test/java/com/example/expensetracker/navigation/RoutesTest.kt (new) — regression tests for the built route strings + base/pattern agreement.
- Build: PASS (`./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest ktlintCheck`).
- Tests: PASS unit (`RoutesTest` 4/4, full `testDebugUnitTest`). Instrumented tests NOT run — no emulator/device.
- Notes / open questions: Add Expense/Income are now reachable both from the bottom bar (`add/expense` base route, defaults applied) and from edit/confirm-recurring (`add/expense?transactionId=...`, `add/expense?ruleId=...&dueDate=...`). Acceptance test 1–5 still needs a manual/emulator run.
- Status: DONE

### 2026-10-20 TASK_01 verification - Instrumented tests run on emulator
- Files changed: docs/agent-tasks/bug-nav-report.md (correction note); docs/agent-tasks/PROGRESS.md (this entry). No app code changed.
- Build: PASS.
- Tests: PASS — `AppNavigationTest` 3/3 on `Pixel_API_36` (API 36): Expense→Income→Home, Income→Expense→Home, Transactions→Home. Unit tests PASS (`RoutesTest` 4/4, full `testDebugUnitTest`). Ktlint PASS.
- Notes / open questions: Earlier "no emulator available" claims were wrong — `emulator`/`adb` are on PATH and `Pixel_API_36` exists. The AVD was PIN-locked (user 0 `RUNNING_LOCKED`), which crashed `ExpenseTrackerApp` init on CE storage. With user approval, the PIN reset was attempted; deleting the lock files corrupted system_server (`LockSettingsService.migrateUserToSpWithBoundKeysLocked`), so `Pixel_API_36` was wiped with `-wipe-data` and cold-booted; it is now `RUNNING_UNLOCKED`, `CredentialType: NONE`. Full `connectedDebugAndroidTest` was not run because the Firestore integration tests require the local Firestore emulator on host port 8080; only `AppNavigationTest` was targeted.
- Status: DONE

### 2026-10-20 TASK_02A Steps 1-6 - Vendor data layer (format, model, repository, record-on-save, tests)
- Files changed:
  - app/src/main/java/com/example/expensetracker/domain/VendorNameFormatter.kt (new) — `toDisplayName` (Pascal Case, spaces kept) and `toKey` (lower-case doc id, `/` → `_`).
  - app/src/test/java/com/example/expensetracker/domain/VendorNameFormatterTest.kt (new) — all table cases from the task plus `toKey("TESCO") == toKey("tesco ")`.
  - app/src/main/java/com/example/expensetracker/data/model/Vendor.kt — extended the existing Vendor class: `id` renamed to `key` (document id), added `lastCategoryId`, `updatedAt: Instant?`, `updatedBy`, default `name = ""`; existing `defaultCategoryId`/`matchAliases` kept.
  - app/src/main/java/com/example/expensetracker/data/repository/VendorRepository.kt — existing interface extended with `recordUsage(rawName, categoryId)`; Firestore impl writes `<households/{id}/vendors/{key}>` with `set(..., SetOptions.merge())`, `name`, `lastCategoryId`, server `updatedAt`, current `updatedBy`; `observeVendors` now orders by `updatedAt` DESC and maps `key = document.id`; constructor now takes `householdId` + `currentUserId` provider.
  - app/src/main/java/com/example/expensetracker/data/repository/FirestoreMappers.kt — Vendor map/parse includes the new fields; `updatedAt` uses `FieldValue.serverTimestamp()`.
  - app/src/main/java/com/example/expensetracker/data/ServiceLocator.kt — constructs `FirestoreVendorRepository` with household id and `authRepository.currentUser?.uid`.
  - app/src/main/java/com/example/expensetracker/ui/addtransaction/AddTransactionViewModel.kt — injectable `currentUserProvider` (defaults to ServiceLocator, matching TransactionsViewModel); vendor text formatted with `VendorNameFormatter.toDisplayName` before writing any transaction; after a successful NEW expense save calls `vendorRepository.recordUsage(rawVendorText, categoryId)`, logs failure with `Log.w` and does not fail the save.
  - app/src/test/java/com/example/expensetracker/ui/addtransaction/AddTransactionViewModelTest.kt (new) — 3 tests with hand-written fakes: formatted name + one `recordUsage` call, blank vendor does not crash, `recordUsage` throwing still saves the expense.
  - app/src/main/java/com/example/expensetracker/ui/vendors/VendorsViewModel.kt, ui/vendors/VendorsScreen.kt, ui/recurring/RecurringRulesScreen.kt, app/src/test/java/com/example/expensetracker/domain/VendorMatcherTest.kt — `vendor.id` → `vendor.key` ripple from the model rename.
  - app/src/androidTest/java/com/example/expensetracker/navigation/AppNavigationTest.kt — fake `VendorRepository` implements `recordUsage`.
  - firestore.rules — no change needed: `match /vendors/{vendorId}` already exists and is semantically identical to the `categories` block (read + create/update/delete for household members).
- Build: PASS (`assembleDebug`, `assembleDebugAndroidTest` compile, `ktlintCheck`, `lintDebug`).
- Tests: PASS unit (`testDebugUnitTest`, includes VendorNameFormatterTest 8/8 and AddTransactionViewModelTest 3/3). Instrumented/device tests NOT RUN (no UI change in this task); Firestore rules not executed against the emulator.
- Notes / open questions:
  - `Vendor.updatedAt` uses `java.time.Instant?` like the other models (Transaction/RecurringRule) instead of the task snippet's `com.google.firebase.Timestamp?`, keeping Firebase types out of models; mapping lives in FirestoreMappers.
  - The existing `createVendor` (manual Vendors screen) still uses auto-generated document ids, so a manually created vendor can duplicate one created by `recordUsage`. Aligning that entry point with the vendor key is left for TASK_02B / a follow-up.
  - Vendor docs written before this change have no `updatedAt`; Firestore's `orderBy("updatedAt")` excludes them, so legacy docs need a backfill (TASK_02C is the optional backfill task).
  - `recordUsage` runs for new expenses only (not edits, and not recurring confirmations — those create transactions through `RecurringRepository`). Extend if recurring usage should be tracked.
- Status: DONE

### 2026-10-20 TASK_02B Steps 1-6 - Vendor autocomplete + category auto-fill (Add Expense)
- Files changed:
  - app/src/main/java/com/example/expensetracker/domain/VendorSuggestions.kt (new) — pure `filter(vendors, query, limit = 8)` (case-insensitive, starts-with before contains, empty query returns newest-first list) and `categoryFor(vendor, expenseCategoryIds)`.
  - app/src/test/java/com/example/expensetracker/domain/VendorSuggestionsTest.kt (new) — all task cases (start/contains/case/empty/no match/limit) plus the three `categoryFor` cases.
  - app/src/main/java/com/example/expensetracker/ui/addtransaction/AddTransactionViewModel.kt — added `onVendorSelected(vendor)` (sets name, fills `lastCategoryId` only when it is one of the currently available categories); vendor list is now collected continuously from `observeVendors()` into the existing `AddTransactionUiState.vendors`; `onVendorChange` no longer auto-fills the category for EXPENSE (only a tapped suggestion does), while the INCOME/"Source" behavior is unchanged.
  - app/src/main/java/com/example/expensetracker/ui/addtransaction/AddTransactionScreen.kt — Add Expense vendor field is now an editable `ExposedDropdownMenuBox` + `OutlinedTextField` + `ExposedDropdownMenu` showing `VendorSuggestions.filter(...)`; tapping an item calls `onVendorSelected`; empty + focused opens the recent list; `testTag("vendor_field")`. Add Money In keeps the original plain field.
  - app/src/test/java/com/example/expensetracker/ui/addtransaction/AddTransactionViewModelTest.kt — fakes now accept category/vendor lists; added tests for selecting a vendor (fills name + last category), missing last category (keeps selection) and typing not auto-filling in an expense.
  - app/src/androidTest/java/com/example/expensetracker/ui/addtransaction/AddTransactionScreenTest.kt (new) — Compose screen test: type `Te`, tap `Tesco`, assert `Groceries` is selected.
- Build: PASS (`assembleDebug`, `assembleDebugAndroidTest`, `ktlintCheck`, `lintDebug`). `detekt` is not configured in this repo.
- Tests: PASS unit (`testDebugUnitTest`: `VendorSuggestionsTest` 9/9, `AddTransactionViewModelTest` 6/6). PASS instrumented on `Pixel_API_36` (API 36, headless): `AddTransactionScreenTest` 1/1 (`connectedDebugAndroidTest` with a class filter).
- Notes / open questions:
  - Used `menuAnchor(MenuAnchorType.PrimaryEditable)` instead of the plain `menuAnchor()` used by `CategoryPicker`, because that overload is deprecated in this Material 3 version; avoid a new deprecation warning. Same ExposedDropdownMenu pattern otherwise.
  - `vendor_field` test tag is new (the old field had none); it exists only to drive the Compose test.
  - Typing in the Add Expense vendor field no longer auto-selects via `VendorMatcher` (required by behaviour rule 7). Add Money In still uses the old matching path.
  - Manual acceptance test 1-6 (seeded `Tesco`/`Costa Coffee`, editing/deleting a category) was not run; it needs the Firestore emulator/data and a signed-in account. The automated screen test covers typing `Te` -> tap `Tesco` -> `Groceries`.
- Status: DONE

