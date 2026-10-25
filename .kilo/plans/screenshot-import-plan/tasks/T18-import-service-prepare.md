# T18 — `ImportService.prepare` and the adapters

**Depends on:** T07, T17 · **Size:** M

## Goal

Turn the parser output into review items: normalise vendors with the app's existing normaliser, suggest categories with the app's existing "last category per vendor" logic, flag duplicates against saved transactions, set sensible default ticks. All dependencies are **ports** (interfaces) so the logic is testable with fakes; thin adapters connect the ports to the real app.

## Read first

`00-ARCHITECTURE.md` §2, §7; `docs/APP_CONTEXT.md` (Data section: repository, vendor normaliser, last-category lookup, current user); `ParserRegistry`, `DuplicateDetector`.

## Create

| File | Content |
|---|---|
| `…/importer/domain/ImportModels.kt` | models and ports (below) |
| `…/importer/domain/ImportService.kt` | `prepare` (T19 adds `commit`) |
| `…/importer/domain/adapters/*.kt` | adapters to the real repositories (below) |
| `app/src/test/…/importer/domain/ImportServicePrepareTest.kt` | tests with fakes |

## `ImportModels.kt`

```kotlin
data class CategoryRef(val id: String, val name: String, val kind: TransactionKind)

data class ReviewItem(
    val id: Int,                     // index in the prepared list, stable for the session
    val selected: Boolean,
    val date: LocalDate,
    val vendor: String,
    val amountMinor: Long,
    val kind: TransactionKind,
    val category: CategoryRef?,
    val duplicate: DuplicateStatus,
    val pending: Boolean,
    val dateUncertain: Boolean,
    val rawText: String,
)

data class PreparedImport(val items: List<ReviewItem>, val warnings: List<String>)

// ---- ports ----
interface ExistingTransactionsSource { suspend fun between(from: LocalDate, to: LocalDate): List<ExistingTx> }
interface VendorCategoryLookup { suspend fun categoryFor(vendor: String, kind: TransactionKind): CategoryRef? }
interface CategorySource { suspend fun list(kind: TransactionKind): List<CategoryRef> }
fun interface VendorNameNormalizer { fun normalise(raw: String): String }

// ---- used by T19 (declared here so the service constructor is final) ----
data class NewTransaction(
    val date: LocalDate, val vendor: String, val amountMinor: Long,
    val kind: TransactionKind, val categoryId: String,
)
fun interface TransactionWriter { suspend fun add(tx: NewTransaction) }
data class CommitResult(val saved: Int, val failed: Int, val skippedNoCategory: Int)
```

## `ImportService`

```kotlin
class ImportService(
    private val existing: ExistingTransactionsSource,
    private val categories: VendorCategoryLookup,
    private val normaliser: VendorNameNormalizer,
    private val writer: TransactionWriter,
    private val parserFor: (Bank) -> BankStatementParser = ParserRegistry::forBank,
) {
    suspend fun prepare(ocr: OcrResult, bank: Bank, today: LocalDate): PreparedImport
    // suspend fun commit(items: List<ReviewItem>): CommitResult   -> T19
}
```

### `prepare` steps

1. `parse = parserFor(bank).parse(ocr, today)`; warnings start as `parse.warnings`.
2. If `parse.transactions` is empty → return `PreparedImport(emptyList(), warnings)` **without** calling any port.
3. `from = minDate − 1 day`, `to = maxDate + 1 day` over the parsed transactions. Fetch `existing.between(from, to)` in a `try`; on any `Exception` (rethrow `CancellationException`) use an empty list and add the warning `"Could not check for duplicates"`.
4. `statuses = DuplicateDetector.detect(parse.transactions, existingList)`.
5. For each parsed transaction (keep the order): 
   - `vendor = normaliser.normalise(p.vendor)`; if the result is blank use `p.vendor`;
   - `category = categories.categoryFor(vendor, p.kind)`;
   - `selected = !p.pending && status != LIKELY`;
   - `id` = position in the list.
6. **Income is not removed here.** The UI hides income by default (T20/T21). Reason: switching the income switch must not discard the user's edits.

## Adapters (`…/importer/domain/adapters/`)

Each adapter ≤ 30 lines, no business logic. Use the real class names recorded in `docs/APP_CONTEXT.md`.

| Port | Adapter | Uses |
|---|---|---|
| `ExistingTransactionsSource` | `RepositoryExistingTransactionsSource` | repository query by date range; if only a per-month query exists, query each month in `[from, to]` and filter by date; convert amounts to `Long` pence (`BigDecimal.movePointRight(2)` with rounding `HALF_UP`, never `Double * 100` without rounding) |
| `VendorCategoryLookup` | `AppVendorCategoryLookup` | the existing "last used category per vendor" function; if `NOT FOUND`, always return `null` and say so in the final summary |
| `CategorySource` | `RepositoryCategorySource` | the existing category list, mapped to `CategoryRef` (kind from the app's expense/income flag) |
| `VendorNameNormalizer` | `AppVendorNameNormalizer` | the existing Pascal-Case normaliser; **only if NOT FOUND**, create a fallback: capitalise the first letter of each space-separated word and lower-case the rest, keep `&` and apostrophes |

Wiring (providing an `ImportService` instance to the ViewModel) happens in T20 according to the DI style in `APP_CONTEXT.md`.

## Tests (write first) — `ImportServicePrepareTest`

Use fakes for all ports and `parserFor = { fakeParser }` where `fakeParser` returns a canned `ParseResult` (no OCR geometry needed). `today = 2026-10-24`.

1. Maps fields; `id`s are 0,1,2…; order preserved.
2. Vendor goes through the normaliser; a normaliser returning blank falls back to the original vendor.
3. Category lookup receives the **normalised** vendor and the item's kind; a `null` result stays `null`.
4. Duplicate `LIKELY` → `selected == false`; `POSSIBLE` → `selected == true`; `NONE` → `true`.
5. `pending == true` → `selected == false`.
6. Income items are **kept** in the result (with `kind == INCOME`).
7. `existing.between` is called once with `(minDate − 1, maxDate + 1)`.
8. `existing` throws → warning `"Could not check for duplicates"` present, all duplicates `NONE`, no exception escapes.
9. Parser warnings are returned first, unchanged.
10. Empty parse → empty items, ports **not** called (use fakes that fail the test if called).

## Reference code

**Verified (JVM tests pass):** `reference/main/domain/ImportModels.kt.txt`, `reference/main/domain/ImportService.kt.txt`; tests in `reference/test/DomainTests.kt.txt`. Try it yourself first; after 2 failed attempts copy the reference (rename `.kt.txt` to `.kt`, replace package `com.example.expenses` with `<PKG>`).

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*ImportServicePrepareTest"
./gradlew :app:assembleDebug
```

(The second command proves the adapters compile against the real repositories.)

## Do NOT

- Put Firestore, Android or Compose types in `ImportService` or the models.
- Change the existing repositories, normaliser or category logic (adapters only call them).
- Implement `commit` (T19).

## Commit message

`feat(importer): add ImportService.prepare with ports and adapters`
