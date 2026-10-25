# T20 — `ImportViewModel`

**Depends on:** T19, T10 · **Size:** M

## Goal

One ViewModel that owns the whole import flow: choose bank → read image → prepare → review/edit → commit. Pure logic, tested with fakes; no Compose in this task.

## Read first

`docs/APP_CONTEXT.md` (ViewModel pattern and example file, DI, settings storage, MainDispatcherRule), `ImportService`, `OcrEngine`, an existing ViewModel + its test for style.

## Create

| File | Content |
|---|---|
| `…/importer/ui/ImportUiState.kt` | `Phase`, `ImportUiState` |
| `…/importer/ui/ImportViewModel.kt` | the ViewModel |
| `…/importer/domain/LastBankStore.kt` | port `interface LastBankStore { fun get(): Bank?; fun set(bank: Bank) }` |
| `…/importer/domain/adapters/PrefsLastBankStore.kt` | adapter using the app's existing settings storage (≤ 20 lines) |
| DI wiring | create `ImportService`, adapters and `OcrEngine` the way `APP_CONTEXT.md` says (Hilt module / Koin module / factory) |
| `app/src/test/…/importer/ui/ImportViewModelTest.kt` | tests |

## State

```kotlin
enum class Phase { PICK, WORKING, REVIEW, SAVING, DONE }

data class ImportUiState(
    val phase: Phase = Phase.PICK,
    val bank: Bank = Bank.GENERIC,
    val includeIncome: Boolean = false,
    val items: List<ReviewItem> = emptyList(),          // ALL items, including income
    val warnings: List<String> = emptyList(),
    val expenseCategories: List<CategoryRef> = emptyList(),
    val incomeCategories: List<CategoryRef> = emptyList(),
    val error: String? = null,
    val result: CommitResult? = null,
) {
    val visibleItems: List<ReviewItem> get() = items.filter { includeIncome || it.kind == TransactionKind.EXPENSE }
    val hiddenIncomeCount: Int get() = if (includeIncome) 0 else items.count { it.kind == TransactionKind.INCOME }
    val importable: List<ReviewItem> get() = visibleItems.filter { it.selected }
    val missingCategoryCount: Int get() = importable.count { it.category == null }
    val canImport: Boolean get() = importable.isNotEmpty() && missingCategoryCount == 0
}
```

## ViewModel

```kotlin
class ImportViewModel(
    private val ocr: OcrEngine,
    private val service: ImportService,
    private val categorySource: CategorySource,
    private val lastBank: LastBankStore,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    val state: StateFlow<ImportUiState>

    fun onBankSelected(bank: Bank)            // stores it; if an OCR result is cached and phase == REVIEW, re-prepare (edits are lost; the UI asks first)
    fun onIncludeIncomeChanged(value: Boolean) // pure UI filter, never re-parses, never loses edits
    fun onImagePicked(image: ImageRef)               // PICK -> WORKING -> REVIEW, or back to PICK with error
    fun onSharedImage(image: ImageRef)               // same as onImagePicked (used by T23)
    fun onItemChanged(id: Int, transform: (ReviewItem) -> ReviewItem)
    fun onToggleSelected(id: Int)
    fun onSelectAllVisible(selected: Boolean)
    fun onApplyCategoryToMissing(category: CategoryRef)   // only visible, selected items WITHOUT a category and with category.kind == item.kind
    fun onConfirmImport()                     // REVIEW -> SAVING -> DONE (or back to REVIEW with error)
    fun onStartOver()                         // back to PICK, clears items/result/cached OCR, keeps bank and includeIncome
    fun onErrorShown()                        // clears error
}
```

Behaviour details:

- Initial `bank` = `lastBank.get() ?: Bank.GENERIC`.
- `onImagePicked`: set `phase = WORKING`, `error = null`; `ocr.recognise(image)` → cache the `OcrResult` in a private field; `service.prepare(ocr, bank, LocalDate.now(clock))`; load both category lists once via `categorySource.list(...)`; `phase = REVIEW`. If **no items** result: stay in `REVIEW`; the screen shows the warnings (so the user sees "No transactions found" and can change the bank).
- Any exception (except cancellation) while reading/parsing: `phase = PICK`, `error = "Could not read this image. Try another screenshot or a different bank."` **Never put exception text, OCR text or amounts in `error` or anywhere else.**
- `onConfirmImport`: if `!canImport` → `error = "Choose a category for N item(s)"` (N = `missingCategoryCount`) or `"Nothing selected"`; else `SAVING` → `service.commit(importable)` → `DONE` with `result`. `service.commit` never throws for a single failed write (it counts it). So: if `result.saved == 0 && result.failed > 0` (or an unexpected exception) go back to `REVIEW` with `error = "Saving failed. Nothing was saved; try again."`; otherwise `DONE`.
- `onStartOver`: drop cached OCR result.

## Tests (write first) — `ImportViewModelTest`

Fakes for `OcrEngine`, `ImportService` (construct the real service with fake ports, or extract a small interface — real service with fakes is preferred), `CategorySource`, `LastBankStore`. `viewModelScope` needs `Dispatchers.setMain(...)`, so `kotlinx-coroutines-test` must be a test dependency. Use it (`runTest`, `StandardTestDispatcher`, `Dispatchers.setMain`) or the project's existing rule. Fixed `Clock`.

1. Initial state: `PICK`, bank from the store (or `GENERIC`), `includeIncome == false`.
2. `onBankSelected` persists to the store.
3. `onImagePicked` success → `WORKING` then `REVIEW`; items loaded; category lists loaded.
4. OCR throws → `PICK`, generic error message, no exception text.
5. `visibleItems` hides income by default; `onIncludeIncomeChanged(true)` shows it **and keeps edits** made earlier (edit an expense first, toggle, assert the edit survived).
6. `hiddenIncomeCount` correct.
7. `onToggleSelected` / `onSelectAllVisible(false)` only touch visible items (hidden income stays as it was).
8. `onApplyCategoryToMissing`: sets the category only on selected visible items with no category and the same kind.
9. `canImport` false while a selected visible item has no category; `onConfirmImport` then sets `error` and does not call the writer.
10. `onConfirmImport` happy path → `SAVING` → `DONE`; `result.saved` equals the number of importable items; hidden income items were **not** written.
11. Every write fails (fake writer throws) → back to `REVIEW` with the "Saving failed" message; items intact.
12. `onStartOver` → `PICK`, items empty, cached OCR dropped (a following bank change does not re-prepare).
13. `onBankSelected` in `REVIEW` re-prepares from the cached OCR **without** calling `ocr.recognise` again.
14. Empty prepare result → `REVIEW` with warnings (not an error).

## Reference code

**Verified (JVM tests pass):** `reference/main/ui/ImportUiState.kt.txt`, `reference/main/ui/ImportViewModel.kt.txt`; tests `ImportViewModelTest` in `reference/test/DomainTests.kt.txt`. Try it yourself first; after 2 failed attempts copy the reference (rename `.kt.txt` to `.kt`, replace package `com.example.expenses` with `<PKG>`).

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*ImportViewModelTest"
./gradlew :app:assembleDebug
```

## Do NOT

- Import Compose, navigation, `Context` or `android.net.Uri` into the ViewModel (it works with `ImageRef`; `Uri` cannot be created in JVM unit tests).
- Keep the image `Uri` or bitmap in the state; keep only the `OcrResult` (needed for re-parsing).
- Log or print anything.

## Commit message

`feat(importer): add ImportViewModel and wiring`
