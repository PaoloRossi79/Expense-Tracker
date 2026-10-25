# T22 — `ReviewScreen`

**Depends on:** T21 · **Size:** L

## Goal

The mandatory safety net: a list of the detected transactions where the user can untick, correct and categorise each one before anything is saved.

## Read first

`docs/APP_CONTEXT.md` (the existing date picker, category picker and vendor autocomplete composables used by Add Expense), `ImportUiState`, `ImportViewModel`, `ImportScreen.kt`.

## Create / change

| File | Change |
|---|---|
| `…/importer/ui/ReviewScreen.kt` | new |
| `…/importer/ui/ReviewActions.kt` | `data class ReviewActions(...)` of lambdas, so the screen is **stateless** and testable |
| `ImportScreen.kt` | replace the placeholder in `Phase.REVIEW` with `ReviewScreen(state, actions)` |
| `strings.xml` | new strings |

## Stateless API

```kotlin
data class ReviewActions(
    val onToggle: (Int) -> Unit,
    val onChange: (Int, (ReviewItem) -> ReviewItem) -> Unit,
    val onSelectAll: (Boolean) -> Unit,
    val onApplyCategoryToMissing: (CategoryRef) -> Unit,
    val onIncludeIncomeChanged: (Boolean) -> Unit,
    val onConfirm: () -> Unit,
    val onStartOver: () -> Unit,
)

@Composable
fun ReviewScreen(state: ImportUiState, actions: ReviewActions, modifier: Modifier = Modifier)
```

## Layout

**Header (sticky):**

- Summary: *"N found · M selected"*; if `hiddenIncomeCount > 0`: *"K money-in rows hidden"* with a **Show** switch (`onIncludeIncomeChanged`).
- Select all / Select none.
- Button **Set category for all missing** → opens the app's category picker (expense or income list depending on the items still missing; if both kinds are missing, ask for expenses first) → `onApplyCategoryToMissing`.
- If `warnings` is not empty: a card listing them (e.g. *"Dates above the first date header are unknown…"*).
- If `visibleItems` is empty: an empty-state text (*"No transactions found. Check the bank selection or try a clearer screenshot."*) and a **Start over** button.

**List** (`LazyColumn`, `key = item.id`) — one row per `visibleItems` entry:

- Checkbox (`selected`), **date**, **vendor**, **amount** formatted as `£12.50` (from `amountMinor`; income shown with `+`), **category** name or a red *"Choose category"* chip.
- Badges (small chips), shown only when true: **Already imported?** (`LIKELY`), **Possible duplicate** (`POSSIBLE`), **Pending**, **Check date** (`dateUncertain`), **Income**.
- Tapping the row expands an editor with: vendor (reuse the app's vendor **autocomplete** field), amount (decimal text field; parse to pence with `BigDecimal`, reject invalid input, keep the old value on error), date (reuse the app's date picker), category (reuse the app's category picker restricted to categories of the item's kind: `expenseCategories` or `incomeCategories`).
- `LIKELY` duplicates start **unticked** (already set by `prepare`); the badge explains why.

**Bottom bar:** button **Import N** (N = `importable.size`), enabled only when `state.canImport`; when disabled because of missing categories show *"Choose a category for X item(s)"* under it. While the bar is visible, `state.error` appears as a `Snackbar`.

Reuse rule: if an existing composable (date picker, category picker, vendor autocomplete) is private to the Add Expense file, first make a **separate commit** that extracts it to a shared file without changing behaviour, then use it. Do not copy-paste duplicates.

## Formatting helper (pure, testable)

`…/importer/ui/MoneyFormat.kt`:

```kotlin
fun formatMinor(amountMinor: Long, kind: TransactionKind): String   // 1250 EXPENSE -> "£12.50", 250000 INCOME -> "+£2,500.00"
fun parseMinor(text: String): Long?                                   // "12.5" -> 1250, "12,50" -> 1250, "abc" -> null, "-3" -> null
```

Unit test `MoneyFormatTest` (write first): `1250/EXPENSE → "£12.50"`, `5/EXPENSE → "£0.05"`, `250000/INCOME → "+£2,500.00"`, `0 → "£0.00"`; `parseMinor("12.5") = 1250`, `("12.50") = 1250`, `("12,50") = 1250`, `("1,234.50") = 123450`, `("") = null`, `("12.345") = null`, `("-3") = null`, `("0") = 0`. Use `Locale.UK` explicitly in the formatter.

## Reference code

**Verified (JVM tests pass):** `reference/main/ui/MoneyFormat.kt.txt`. Try it yourself first; after 2 failed attempts copy the reference (rename `.kt.txt` to `.kt`, replace package `com.example.expenses` with `<PKG>`).

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*MoneyFormatTest"
./gradlew :app:assembleDebug
```

**You**: on the phone, import one real screenshot: rows appear, you can untick, edit vendor/amount/date/category, set category for all missing, and the Import button count is right. Do not save anything yet if you are not comfortable (use a test account/Firebase project if you have one).

## Do NOT

- Put business logic in the composable (the ViewModel owns the state).
- Save from this screen except via `actions.onConfirm`.
- Show raw OCR text (`rawText`) anywhere except a small "Original text" line in the expanded editor.

## Commit message

`feat(importer): add review screen with editing and duplicate badges`
