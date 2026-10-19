# TASK_00 - Map the codebase (read-only)

**Goal:** create `docs/agent-tasks/PROJECT_MAP.md`, a short factual map of this Android app. Later tasks depend on it.
**Mode:** Ask or Code, but you may ONLY create/edit `docs/agent-tasks/PROJECT_MAP.md` and `docs/agent-tasks/PROGRESS.md`. Do NOT change any app code.
**Rule for every step:** write what you found into the matching section of PROJECT_MAP.md straight away, then move on. Write real file paths and real names copied from the code. If you cannot find something, write `NOT FOUND`. Never guess.

## Step 0 - Create the empty map
Create `docs/agent-tasks/PROJECT_MAP.md` with exactly this content:

```
# PROJECT MAP

## 1. Stack
- Language (Kotlin/Java):
- UI toolkit (Jetpack Compose / XML views):
- Build system and app module name:
- Dependency injection (Hilt/Koin/manual/none):

## 2. Commands
- Build command:
- Unit test command:
- UI/instrumented test command:

## 3. Navigation
- Navigation library (Navigation Compose / Navigation Component with Fragments / other):
- File that defines the NavHost / nav graph:
- File with the bottom navigation bar:
- Route names (Home, Add Expense, Add Money In, Transactions, Manage Categories, others):
- Start destination:
- How a bottom-bar item click navigates (paste the exact code, max 15 lines):

## 4. Screens
| Screen | UI file | ViewModel file |
|---|---|---|

## 5. Data (Firestore)
- Firestore collection paths and how they are scoped (per family/household id? top-level?):
- Transaction model: file, class name, all fields:
- Category model: file, class name, all fields (including expense/income type):
- Member/user model: file, class name, all fields:
- Vendor model or vendor-to-category mapping (exists already? where?):
- Repository classes (name, file, main functions):
- Where a new expense is saved (function name and file):
- Firestore rules file in repo (path or NOT FOUND):

## 6. Current user
- How the app knows the logged-in user id:
- How the app gets the user display name (Google account, members collection, other):
- Field name on a transaction that stores the creator:

## 7. Tests
- Unit test folder and framework:
- UI test folder and framework:
- One existing test file as an example (path):
```

## Step 1 - Stack and commands (fills sections 1 and 2)
1. List the repo root and the app module folder (max 3 levels deep).
2. Open `build.gradle(.kts)` of the app module and `settings.gradle(.kts)`. Record the language, whether `compose` is enabled, and whether Hilt/Koin plugins are present.
3. Record the build command. If unsure use `./gradlew assembleDebug`. Unit tests: `./gradlew testDebugUnitTest`. UI tests: `./gradlew connectedDebugAndroidTest`.
4. Check the commands work by running the build command once. Write PASS or the error.

## Step 2 - Navigation (fills section 3)
Search the whole project for each of these words and open the files found:
`NavHost`, `composable(`, `NavController`, `NavigationBar`, `BottomNavigation`, `BottomNavigationView`, `nav_graph`, `navigate(`.
Record the items listed in section 3. For "How a bottom-bar item click navigates", copy the real code; do not summarise it.

## Step 3 - Screens (fills section 4)
Find the screens: Home, Add Expense, Add Money In, Monthly Transactions, Manage Categories, Add Member (if present). For each, record the UI file and the ViewModel file. One table row per screen.

## Step 4 - Data (fills section 5)
Search for: `FirebaseFirestore`, `.collection(`, `.document(`, `data class`, `Repository`, `firestore.rules`.
Record every Firestore collection path you find and the model classes with all their fields (copy field names and types exactly). Find the function that saves a new expense and record its name and file.

## Step 5 - Current user (fills section 6)
Search for: `FirebaseAuth`, `currentUser`, `uid`, `displayName`, `createdBy`, `userId`, `GoogleSignIn`, `BiometricPrompt`.
Record how the user id is obtained, where a display name could come from, and the exact field name used for the creator on a transaction.

## Step 6 - Tests (fills section 7)
List the test folders (`src/test`, `src/androidTest`). Record frameworks used (JUnit, MockK, Mockito, Compose test rule, Espresso) and one example test file path.

## Step 7 - Review
1. Re-read PROJECT_MAP.md. Every line must be filled or say `NOT FOUND`.
2. Append an entry to PROGRESS.md (Status DONE).
3. STOP. Tell the user which items are `NOT FOUND` so they can help.
