# T26 — Integration test, UI tests and manual QA checklist

**Depends on:** T22 · **Size:** M · **Who:** Kilo + You

## Goal

Prove the whole flow works (JVM integration test), that the review screen behaves (Compose UI test), and give yourself a checklist for real-phone testing.

## Read first

`docs/APP_CONTEXT.md` (Compose UI test dependencies, test style), `ReviewScreen.kt`, `ImportUiState.kt`.
**Reference (verified):** `ImportFlowIntegrationTest` at the end of `reference/test/DomainTests.kt.txt`, and the fakes at the top of that file.

## Part 1 — JVM integration test

File: `app/src/test/…/importer/ImportFlowIntegrationTest.kt` (move the shared fakes into `…/importer/testutil/Fakes.kt` if they are not there yet).

Uses: the real `ParserRegistry`, the real `ImportService`, the real `ImportViewModel`, the fixture `generic/sample-synthetic`, a fake OCR engine returning that fixture, a fake category source, and an in-memory `Store` that is both `ExistingTransactionsSource` and `TransactionWriter`. Clock fixed at 2026-10-24.

Scenario:
1. Pick image → 3 items (`Tesco`, `Salary`, `Costa`); 2 visible (income hidden).
2. Category lookup knows only `Tesco`; `canImport == false`; `onApplyCategoryToMissing(expenseCategory)` → `canImport == true`.
3. `onConfirmImport` → `DONE`; the store holds `Costa`, `Tesco` (ascending date); the income was **not** written.
4. `onStartOver`, pick the same image again → `Tesco` and `Costa` are `LIKELY` duplicates, `Salary` is `NONE`; `importable` is empty and `canImport == false`.

## Part 2 — Compose UI test (instrumented, runs on a device/emulator)

1. First add `Modifier.testTag(...)` to `ReviewScreen` (**no behaviour change**): row `review_row_<id>`, checkbox `review_checkbox_<id>`, import button `review_import_button`, hidden-income text `review_hidden_income_text`, empty state `review_empty_state`.
2. File `app/src/androidTest/…/importer/ui/ReviewScreenTest.kt`, using `createComposeRule()` and fake `ImportUiState` / recording `ReviewActions`:
   - two rows are shown;
   - clicking a checkbox calls `onToggle(id)`;
   - an item without category → the import button is disabled and the "Choose a category" text is shown;
   - all categories set → button enabled and its text contains the selected count;
   - `hiddenIncomeCount = 2` → the hidden-income text is shown;
   - no visible items → empty state shown.
3. Add Compose test dependencies only if `APP_CONTEXT.md` says they are missing (`androidx.compose.ui:ui-test-junit4`, debug `ui-test-manifest`), using the project's Compose BOM.

## Part 3 — Manual QA checklist

Create `docs/screenshot-import/QA-CHECKLIST.md` with these items as checkboxes (copy the list as is):

- [ ] Per bank (Revolut, Monzo, Barclays, Chase): import a real screenshot; every row vendor/amount/date matches the bank app (note mismatches).
- [ ] Income hidden by default; "Show" switch reveals it and keeps edits.
- [ ] Untick a row, edit vendor, amount, date and category; the Import N count updates.
- [ ] A row with no category blocks Import until fixed; "Set category for all missing" works.
- [ ] Second import of the same screenshot: rows flagged "Already imported?" and unticked.
- [ ] Overlapping second screenshot: only new rows are ticked.
- [ ] Pending rows unticked.
- [ ] Imported transactions show the right creator name and appear in the Monthly Transactions list; only the creator can edit/delete them.
- [ ] Vendor autocomplete and the last-used category per vendor learn from imported rows.
- [ ] Dark theme, large font size, portrait rotation.
- [ ] Back button from Import returns to Home and does not leave the app stuck on another screen.
- [ ] Share sheet: share a screenshot from the gallery → sign-in → import flow starts.
- [ ] Airplane mode: import reaches the review screen.
- [ ] Release build works (T25).
- [ ] Cancel the picker: no crash, state unchanged.
- [ ] Unreadable image (blank photo): friendly error, no crash.

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*ImportFlowIntegrationTest"
./gradlew :app:assembleDebugAndroidTest
```

pass. **You**: run `./gradlew :app:connectedDebugAndroidTest` on a device once, and work through the checklist with your real screenshots (nothing committed).

## Do NOT

- Change `ReviewScreen` behaviour while adding test tags.
- Commit real screenshots or real OCR dumps.

## Commit message

`test(importer): integration flow test, review screen UI tests and QA checklist`
