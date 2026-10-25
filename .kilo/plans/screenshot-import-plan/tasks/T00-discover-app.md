# T00 — Discover the existing app

**Depends on:** nothing · **Size:** S · **Mode:** Code, **read-only for source code**

## Goal

Write `docs/APP_CONTEXT.md`: a factual map of the existing app, so every later task uses real class names, packages and patterns instead of guessing.

## Read first

`settings.gradle(.kts)`, `app/build.gradle(.kts)`, `gradle/libs.versions.toml` (if it exists), `app/src/main/AndroidManifest.xml`, `MainActivity`, the navigation file, the Home / Add Expense / Monthly Transactions screens, the Transaction and Category models, the repository classes, the vendor feature files, existing tests, `.github/workflows/*`.

## Do

1. Explore the repository (list files, open the files above). **Do not modify any existing file.**
2. Create `docs/APP_CONTEXT.md` using the template below. For every item write the **real value and the real file path**. If something does not exist write `NOT FOUND`. Never leave `?` or `TODO`.
3. For each "file" entry open the file and double-check that the class/function name you wrote exists.

## Template for `docs/APP_CONTEXT.md`

```markdown
# APP_CONTEXT (generated in T00)

## Build
- Gradle module of the app:
- applicationId / base package (this is <PKG>):
- Main source root (<SRC>), e.g. app/src/main/java or app/src/main/kotlin:
- Debug source set exists? (app/src/debug):
- minSdk / targetSdk / compileSdk:
- Kotlin version / AGP version:
- Build files are .kts or .gradle; version catalog (libs.versions.toml) yes/no:
- Core library desugaring enabled (needed for java.time when minSdk < 26): yes/no
- JSON library already used (Gson / Moshi / kotlinx.serialization / none):

## UI
- Toolkit: Jetpack Compose or XML views:
- Material version (M2 / M3):
- Navigation library + file where routes are declared:
- Activity count (single activity?) and file of the main activity:
- Home screen file:
- Add Expense screen file + the composable/class used for: date picker, category picker, vendor autocomplete:
- Monthly Transactions screen file:
- Where an extra button could go on Home (top bar action, FAB menu, overflow menu):
- Biometric / sign-in gate: where it is, and how the rest of the app waits for it:

## Architecture
- DI: Hilt / Koin / manual (and the file where objects are created):
- ViewModel pattern + one example file:
- Coroutines and Flow used: yes/no:
- Settings storage used (DataStore / SharedPreferences / none) + example file:

## Data
- Transaction model (file + every field with its type; especially the amount type and the date type):
- Category model (file + fields; how expense-only / income-only is represented):
- Repository interface(s) (file + signatures) for: add transaction, query transactions by date range or by month, list categories, list vendors:
- Existing vendor Pascal-Case normaliser (file + function) or NOT FOUND:
- Existing "last used category per vendor" (file + function) or NOT FOUND:
- Where vendor usage and the last-used category per vendor are RECORDED when an expense is saved (repository or ViewModel; file + function):
- Does the repository `add` wait for the server (await) or return after the local write? Which function does Add Expense call to save?
- Current user / creator provider (file + function):
- Firestore offline persistence enabled/disabled:

## Testing and CI
- Unit test libraries in use (JUnit4/5, MockK/Mockito, Turbine, coroutines-test):
- Existing MainDispatcherRule or similar: file or NOT FOUND:
- Compose UI test dependencies present: yes/no
- One existing unit test file to copy the style from:
- CI workflow file(s):
- Exact command that runs all unit tests locally:
```

## Done when

- `docs/APP_CONTEXT.md` exists, contains no `?` or `TODO`, and every path in it exists (`ls` them).
- `git status` shows **only** `docs/APP_CONTEXT.md` as new.

## Do NOT

- Change, format or fix any source file.
- Invent anything. `NOT FOUND` is a valid answer.

## Commit message

`docs: add APP_CONTEXT for screenshot import`
