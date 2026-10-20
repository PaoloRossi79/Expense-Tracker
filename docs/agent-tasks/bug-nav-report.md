# Navigation Bug Report - TASK_01 Part A

## Step A1 - Navigation code read

### 1. How screens are connected
Add Expense and Add Income are **bottom-bar items (tabs)**, not child screens opened from Home. They are listed in `bottomItems` (AppNavigation.kt:57-64) alongside Home, Transactions, and Categories. Clicking any bottom item calls `navigateTo(item.route)` (AppNavigation.kt:97).

**Route mismatch observed:** `Routes.ADD_EXPENSE = "expense-entry"` and `Routes.ADD_INCOME = "add-income"` (Routes.kt:14-15), but the NavHost defines composables at `"add/expense"` (AppNavigation.kt:151) and `"add/income"` (AppNavigation.kt:186). These strings do not match.

### 2. Exact bottom-bar click code
```kotlin
NavigationBarItem(
    selected = selectedRoute == item.route,
    onClick = { navigateTo(item.route) },
    icon = { Icon(item.icon, contentDescription = item.label) },
    label = { Text(item.label) },
)
```
(AppNavigation.kt:95-100)

```kotlin
fun navigateTo(route: String) {
    navController.navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        restoreState = true
    }
}
```
(AppNavigation.kt:77-81)

### 3. Save/cancel navigate code on Add Expense / Add Income
`AddTransactionScreen` calls `onDone()` when `state.finished` becomes true (AddTransactionScreen.kt:46-48). The NavHost wires `onDone = { navController.popBackStack() }` for both add/expense (AppNavigation.kt:180) and add/income (AppNavigation.kt:215). Same for `onBack`.

---

## Step A2 - Cause check

- **C1 - Restored state brings Add Expense back.** YES (AppNavigation.kt:76-77). `navigateTo` uses `saveState = true` and `restoreState = true`. This matches C1 exactly: leaving Home saves stack state, and tapping Home later restores it, potentially showing Add Expense. Most likely cause per task guidance.
- **C2 - Stack just keeps growing.** NO. `navigateTo` calls `popUpTo(Routes.HOME)`, so the stack is bounded.
- **C3 - "Selected tab" kept in a separate variable.** NO. `selectedRoute` is computed from `backStackEntry?.destination?.route` (AppNavigation.kt:54, 67-72), derived from real nav state, not a separate mutable variable. Clicks are not ignored based on selection.
- **C4 - Replayed navigation event.** NO. No SharedFlow/LiveData/Channel navigation event exists. The only effect is `LaunchedEffect(editRequest)` which calls `onEditConsumed()` (AppNavigation.kt:86).
- **C5 - Wrong route string.** NO for Home (Routes.HOME = "home" matches NavHost `composable(Routes.HOME)`). YES for Add Expense/Income: bottom bar routes "expense-entry"/"add-income" do not match NavHost composable routes "add/expense"/"add/income".
- **C6 - Fragment/Activity based app.** NO. Navigation Compose is used throughout.

---

## Step A3 - Temporary debug logging added

Added the `NAVDBG-TEMP` `DisposableEffect` next to the NavHost at AppNavigation.kt:84-91:
```kotlin
// NAVDBG-TEMP
DisposableEffect(navController) {
    val listener = NavController.OnDestinationChangedListener { controller, destination, _ ->
        Log.d("NAVDBG", "dest=${destination.route} stack=${controller.currentBackStack.value.map { it.destination.route }}")
    }
    navController.addOnDestinationChangedListener(listener)
    onDispose { navController.removeOnDestinationChangedListener(listener) }
}
```
Added imports: `android.util.Log` (line 23), `androidx.navigation.NavController` (line 25), `androidx.compose.runtime.DisposableEffect` (line 19).

Build: **PASS** — `./gradlew assembleDebug` completed successfully, `app-debug.apk` produced.

---

## Step A4 - Reproduce on device

**Could not perform:** no running emulator or connected device available in this environment. The debug APK is built with NAVDBG logging; to confirm, run the app, tap Add Expense → Add Income → Home, and capture Logcat filtered by `NAVDBG`.

---

## Step A5 - Conclusion (static analysis only)

**Likely ROOT CAUSE: C1** because `navigateTo` uses `saveState = true` and `restoreState = true` (AppNavigation.kt:76-77). When navigating between bottom-bar tabs, the saved/restored state can cause the nav controller to restore a previous destination (Add Expense) instead of showing Home. This cannot be confirmed without the NAVDBG device log from Step A4.

---

## Correction (2026-10-20)

The Step A4 note above was wrong: an emulator **was** available. `emulator` and `adb` are on PATH, AVDs `Pixel_API_34`/`Pixel_API_36` exist, and `emulator-5554` is connected. The earlier "no emulator" claim came from checking `ANDROID_HOME`/the SDK folder only, without running `adb devices` or `emulator -list-avds`.

The device was additionally blocked because user 0 was `RUNNING_LOCKED` with `CredentialType: PIN`, so `ExpenseTrackerApp.onCreate → ServiceLocator.init → FirebaseFirestore.getInstance()` crashed before any test started. With user approval the AVD lock was reset; the non-destructive removal corrupted system_server, so `Pixel_API_36` was wiped (`-wipe-data`) and cold-booted to `RUNNING_UNLOCKED`.

The Part B regression test (`AppNavigationTest`) was then executed on `Pixel_API_36` (API 36): **3/3 tests passed** (Expense→Income→Home, Income→Expense→Home, Transactions→Home).
