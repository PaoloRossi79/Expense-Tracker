# TASK_01 - Fix: Add Expense -> Add Income -> Home shows Add Expense

**The bug:** user taps Add Expense, then Add Income, then Home. The app does NOT show Home; it stays on (or returns to) the Add Expense screen.
**Expected:** tapping Home always shows Home, whatever the user visited before.
**Mode:** Part A = Debug or Ask mode (only add temporary log lines and write the report). Part B = Code mode.
**Needs:** `PROJECT_MAP.md` section 3 (Navigation) and section 4 (Screens).

Do NOT guess the cause. Part A collects evidence first. Part B fixes it.

---
## PART A - Investigate

### Step A1 - Read the navigation code
Open the files listed in PROJECT_MAP.md section 3 (NavHost file and bottom bar file). Create `docs/agent-tasks/bug-nav-report.md` and write:
1. How the screens are connected: is Add Expense / Add Income a bottom-bar item (a "tab"), or a screen opened by a button from Home (a "child" of Home)?
2. The exact bottom-bar click code (copy it).
3. What happens when a user saves or cancels on Add Expense / Add Income (copy the navigate/popBackStack code).

### Step A2 - Check these causes one by one
In the report, for each cause write `YES (file:line, code)` or `NO`. Check all of them.

- **C1 - Restored state brings Add Expense back.** The bottom-bar click uses `saveState = true` and/or `restoreState = true`. If Add Expense was opened as a child of Home, leaving Home saves the stack `[Home, AddExpense]`. Tapping Home later RESTORES that stack and shows Add Expense. This is the most likely cause.
- **C2 - Stack just keeps growing.** The click calls `navigate(route)` with no `popUpTo` and no `launchSingleTop`.
- **C3 - "Selected tab" kept in a separate variable.** A variable (`remember`, ViewModel field, `mutableStateOf`) stores the selected item, and the click is ignored when `selected == target`. The variable gets out of sync with the real screen.
- **C4 - Replayed navigation event.** A ViewModel exposes a "saved"/"navigate" event (SharedFlow, LiveData, Channel, state flag) that is not consumed after use. When a screen comes back, the old event fires again and navigates to Add Expense.
- **C5 - Wrong route string.** The Home click uses a route string/ID that is different from the Home destination (typo, different constant).
- **C6 - Fragment/Activity based app.** Screens are shown with `add()` instead of `replace()`, or `hide/show` is used, so the old screen stays on top.

### Step A3 - Add temporary debug logging
Add this ONCE (Compose Navigation) where `navController` is created, next to the NavHost. Mark it with the comment `// NAVDBG-TEMP` so it can be removed later:

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
(If the app uses Fragments, log `findNavController().currentDestination?.label` in each fragment's `onResume` with the tag `NAVDBG`.)
Build the app.

### Step A4 - STOP and ask the user
Write in the report under "How to reproduce": run the app, tap Add Expense, Add Income, Home. In Logcat filter by `NAVDBG` and copy all lines.
Tell the user: "Please run the steps and paste the NAVDBG lines here." **STOP and wait.** Do not fix anything yet.

### Step A5 - Conclude (after the user pastes the log)
Read the log. In the report write "ROOT CAUSE: C? because ..." using the log lines as evidence. If the evidence does not match any of C1-C6, write the new cause you see. Append to PROGRESS.md. STOP.

---
## PART B - Fix

### Step B1 - Apply the fix that matches the root cause
Change only the navigation code. Use the matching recipe.

**Recipe for C1 / C2 / C5 (Navigation Compose).** Make the Home item reset to Home instead of restoring old state. Replace the Home click with:

```kotlin
// Home item: always land on Home, drop everything above it
if (!navController.popBackStack(HOME_ROUTE, inclusive = false)) {
    navController.navigate(HOME_ROUTE) {
        popUpTo(navController.graph.findStartDestination().id)
        launchSingleTop = true
    }
}
```
For the other bottom-bar items use:

```kotlin
navController.navigate(route) {
    popUpTo(navController.graph.findStartDestination().id)
    launchSingleTop = true
}
```
Rules: remove `saveState = true` and `restoreState = true` from items that open Add screens (a half-filled form must not be restored). Use the real route constants from PROJECT_MAP.md instead of `HOME_ROUTE`.

**Recipe for C3.** Delete the separate "selected" variable. Compute the selected item from the real destination:
`val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route` and highlight the item where `item.route == currentRoute`. Do not ignore a click just because the item looks selected.

**Recipe for C4.** After the screen has reacted to the event, clear it (set the flag back to false / call a `consumeEvent()` function), or use a `Channel` with `receiveAsFlow()` instead of a state flag.

**Recipe for C6 (Fragments).** Use the Navigation Component with `navigate(R.id.homeFragment)` plus `setPopUpTo(R.id.homeFragment, false)` and `setLaunchSingleTop(true)`; replace `add()` with `replace()`.

Build the app.

### Step B2 - Remove debug code
Search for `NAVDBG-TEMP` and delete those blocks. Build again. Confirm the search finds nothing.

### Step B3 - Add a regression test
Add a UI test following the example test in PROJECT_MAP.md section 7. It must:
1. Start the app at Home.
2. Click Add Expense item, then Add Income item, then Home item.
3. Assert the Home screen is displayed (use a test tag or unique text that exists only on Home; add `Modifier.testTag("home_screen")` to the Home screen root if needed).
Also test the reverse order (Add Income, Add Expense, Home) and Home -> Transactions -> Home.
If the project has no UI test setup at all, write `NOT FOUND: UI test setup` in PROGRESS.md and skip this step (the manual test below is then the check).

### Step B4 - Finish
Run unit tests. Append to PROGRESS.md. STOP.

---
## Acceptance test (you, on the phone or emulator)
1. Add Expense -> Add Income -> Home: Home is shown.
2. Add Income -> Add Expense -> Home: Home is shown.
3. Home -> Add Expense -> Home -> Add Expense: the form is empty each time you re-enter.
4. Transactions -> Manage Categories -> Home: Home is shown.
5. Press the phone Back button from Home: the app closes (no unexpected screens behind Home).
