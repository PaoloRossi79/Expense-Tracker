# PROJECT MAP

## 1. Stack
- Language (Kotlin/Java): Kotlin (app/build.gradle.kts: kotlin-android plugin, compileOptions JavaVersion 17)
- UI toolkit (Jetpack Compose / XML views): Jetpack Compose + Material 3 (`buildFeatures { compose = true }`, app/build.gradle.kts:96-99); theme XML only (res/values/themes.xml), no Compose XML layouts
- Build system and app module name: Gradle Kotlin DSL, module `:app` (settings.gradle.kts:23)
- Dependency injection (Hilt/Koin/manual/none): none — manual locator `ServiceLocator` (app/src/main/java/com/example/expensetracker/data/ServiceLocator.kt); AGENTS.md says Hilt but the repo does not use it

## 2. Commands
- Build command: `./gradlew assembleDebug`
- Unit test command: `./gradlew testDebugUnitTest`
- UI/instrumented test command: `./gradlew connectedDebugAndroidTest`

## 3. Navigation
- Navigation library (Navigation Compose / Navigation Component with Fragments / other): Navigation Compose (androidx-navigation-compose 2.8.5, gradle/libs.versions.toml:41)
- File that defines the NavHost / nav graph: app/src/main/java/com/example/expensetracker/navigation/AppNavigation.kt (NavHost at line 106)
- File with the bottom navigation bar: app/src/main/java/com/example/expensetracker/navigation/AppNavigation.kt (Material3 NavigationBar at lines 93-103)
- Route names: Home = "home"; Add Expense bottom item = "expense-entry"; Add Money In bottom item = "add-income"; Transactions = "transactions"; Categories = "categories"; Vendors = "vendors"; Recurring = "recurring"; Members = "members"; add expense route = "add/expense"; add income route = "add/income" (app/src/main/java/com/example/expensetracker/navigation/Routes.kt)
- Start destination: Routes.HOME = "home" (AppNavigation.kt:108)
- How a bottom-bar item click navigates (exact code):
```
                        NavigationBarItem(
                            selected = selectedRoute == item.route,
                            onClick = { navigateTo(item.route) },
```
with
```
    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            restoreState = true
        }
    }
```
(AppNavigation.kt:74-79, 95-98)

## 4. Screens
| Screen | UI file | ViewModel file |
|---|---|---|
| Home | app/src/main/java/com/example/expensetracker/ui/home/HomeScreen.kt | app/src/main/java/com/example/expensetracker/ui/home/HomeViewModel.kt |
| Transactions | app/src/main/java/com/example/expensetracker/ui/transactions/TransactionsScreen.kt | app/src/main/java/com/example/expensetracker/ui/transactions/TransactionsViewModel.kt |
| Categories | app/src/main/java/com/example/expensetracker/ui/categories/CategoriesScreen.kt | app/src/main/java/com/example/expensetracker/ui/categories/CategoriesViewModel.kt |
| Vendors | app/src/main/java/com/example/expensetracker/ui/vendors/VendorsScreen.kt | app/src/main/java/com/example/expensetracker/ui/vendors/VendorsViewModel.kt |
| Recurring Rules | app/src/main/java/com/example/expensetracker/ui/recurring/RecurringRulesScreen.kt | app/src/main/java/com/example/expensetracker/ui/recurring/RecurringRulesViewModel.kt |
| Add Member | app/src/main/java/com/example/expensetracker/ui/members/AddMemberScreen.kt | app/src/main/java/com/example/expensetracker/ui/members/AddMemberViewModel.kt |
| Add Expense / Add Money In | app/src/main/java/com/example/expensetracker/ui/addtransaction/AddTransactionScreen.kt | app/src/main/java/com/example/expensetracker/ui/addtransaction/AddTransactionViewModel.kt |
| Sign-in gate | app/src/main/java/com/example/expensetracker/ui/auth/IdentificationScreen.kt | app/src/main/java/com/example/expensetracker/ui/auth/RootViewModel.kt |

## 5. Data (Firestore)
- Firestore collection paths and how they are scoped (per family/household id? top-level?): Scoped per household under `households/{householdId}/`: `households/{id}/transactions`, `households/{id}/categories`, `households/{id}/vendors`, `households/{id}/recurringRules/{ruleId}/occurrences/{occurrenceId}`. Top-level: `diagnostics/{docId}`. Household id fixed at bootstrap via BuildConfig.HOUSEHOLD_ID (ServiceLocator.kt:60). Sources: TransactionRepository.kt:43, CategoryRepository.kt:33, VendorRepository.kt:33, RecurringRepository.kt:79/81-84, firestore.rules lines 40-84.
- Transaction model: file app/src/main/java/com/example/expensetracker/data/model/Transaction.kt; class `data class Transaction`; fields: `id: String = ""`, `type: TransactionType`, `amountPence: Long`, `date: LocalDate`, `vendor: String`, `vendorId: String? = null`, `categoryId: String`, `isRecurringInstance: Boolean = false`, `recurringRuleId: String? = null`, `createdByUid: String`, `createdByName: String`, `createdAt: Instant? = null`, `updatedAt: Instant? = null`. Enum `TransactionType { EXPENSE, INCOME }` in same file.
- Category model: file app/src/main/java/com/example/expensetracker/data/model/Category.kt; class `data class Category`; fields (including expense/income type): `id: String = ""`, `name: String`, `type: CategoryType` (enum `CategoryType { EXPENSE, INCOME }`), `icon: String? = null`, `color: String? = null`.
- Member/user model: members live on `Household.memberEmails: List<String>` (app/src/main/java/com/example/expensetracker/data/model/Household.kt: id, name, createdAt?, memberEmails); current user model `AuthUser(uid, email, displayName)` in app/src/main/java/com/example/expensetracker/data/auth/AuthRepository.kt:17-21. No separate Member class.
- Vendor model or vendor-to-category mapping (exists already? where?): `data class Vendor` in app/src/main/java/com/example/expensetracker/data/model/Vendor.kt: fields `id: String = ""`, `name: String`, `defaultCategoryId: String? = null`, `matchAliases: List<String> = emptyList()`. Vendor-to-category mapping = `Vendor.defaultCategoryId`; repository app/src/main/java/com/example/expensetracker/data/repository/VendorRepository.kt.
- Repository classes (name, file, main functions): `TransactionRepository` / `FirestoreTransactionRepository` — data/repository/TransactionRepository.kt (observeTransactions, getTransaction, addTransaction, updateTransaction, deleteTransaction, countForCategory); `CategoryRepository` / `FirestoreCategoryRepository` — data/repository/CategoryRepository.kt (observeCategories, createCategory, updateCategory, deleteCategory); `HouseholdRepository` / `FirestoreHouseholdRepository` — data/repository/HouseholdRepository.kt (observeHousehold, getHousehold, addMember); `VendorRepository` / `FirestoreVendorRepository` — data/repository/VendorRepository.kt (observeVendors, createVendor, updateVendor, deleteVendor); `RecurringRepository` / `FirestoreRecurringRepository` — data/repository/RecurringRepository.kt (observeRules, observePendingOccurrences, createRule, updateRule, deleteRule, setActive, confirmOccurrence, skipOccurrence).
- Where a new expense is saved (function name and file): UI entry `AddTransactionViewModel.save()` → `transactionRepository.addTransaction(ServiceLocator.householdId, transaction)` at app/src/main/java/com/example/expensetracker/ui/addtransaction/AddTransactionViewModel.kt:222-233; persisted by `FirestoreTransactionRepository.addTransaction` at app/src/main/java/com/example/expensetracker/data/repository/TransactionRepository.kt:65-68 (`collection(householdId).add(transaction.toMap()).await().id`).
- Firestore rules file in repo (path or NOT FOUND): `firestore.rules` at repo root.

## 6. Current user
- How the app knows the logged-in user id: `FirebaseAuth.getInstance().currentUser.uid`, exposed via `FirebaseGoogleAuthRepository.currentUser` and `observeAuthState()` (app/src/main/java/com/example/expensetracker/data/auth/AuthRepository.kt:45-52). Sign-in: Google ID token via Credential Manager (`GetGoogleIdOption` with serverClientId WEB_CLIENT_ID) + `GoogleAuthProvider.getCredential` + `FirebaseAuth.signInWithCredential` (AuthRepository.kt:54-82).
- How the app gets the user display name (Google account, members collection, other): Google account — `FirebaseUser.displayName ?: email ?: "Unknown"` (AuthRepository.kt:90-94). No members-collection lookup for display name.
- Field name on a transaction that stores the creator: `createdByUid` (Transaction.kt:25; also `RecurringRule.createdByUid` at RecurringRule.kt:21).

## 7. Tests
- Unit test folder and framework: `app/src/test/java`; JUnit 4 (`org.junit.Test`), MockK 1.13.13, kotlinx-coroutines-test (gradle/libs.versions.toml:17,50,60; app/build.gradle.kts:155-157)
- UI test folder and framework: `app/src/androidTest/java`; AndroidX Test runner `AndroidJUnit4` + Jetpack Compose `createComposeRule()` + assertions `onNodeWithText`/`onAllNodesWithContentDescription` (app/build.gradle.kts:159-166; ui/transactions/TransactionsScreenTest.kt)
- One existing test file as an example (path): app/src/test/java/com/example/expensetracker/domain/CategoryRulesTest.kt
