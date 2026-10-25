# T17 — `DuplicateDetector`

**Depends on:** T02 · **Size:** M

## Goal

Pure function that decides, for each parsed transaction, whether it probably already exists in the app. This protects against importing the same screenshot twice, or two overlapping screenshots.

## Read first

`00-ARCHITECTURE.md` §7 (duplicate rules).

## Create

- `…/importer/dedupe/DuplicateDetector.kt`
- `app/src/test/…/importer/dedupe/DuplicateDetectorTest.kt`

## Contract

```kotlin
enum class DuplicateStatus { NONE, POSSIBLE, LIKELY }

data class ExistingTx(
    val date: LocalDate,
    val amountMinor: Long,
    val kind: TransactionKind,
    val vendor: String,
)

object DuplicateDetector {
    /** One status per parsed item, same size and order as [parsed]. */
    fun detect(parsed: List<ParsedTransaction>, existing: List<ExistingTx>): List<DuplicateStatus>
}
```

## Algorithm

```
statuses = MutableList(parsed.size) { NONE }
available = existing.toMutableList()        // each saved transaction can be used once

Pass 1 (LIKELY): for i in parsed.indices (in order)
    j = first index in available where
          date == parsed[i].date  &&  amountMinor == parsed[i].amountMinor  &&  kind == parsed[i].kind
          && fuzzyEqual(vendor, parsed[i].vendor)
    if found: statuses[i] = LIKELY; available.removeAt(j)

Pass 2 (POSSIBLE): for i in parsed.indices where statuses[i] == NONE
    j = first index in available where
          abs(daysBetween(date, parsed[i].date)) <= 1 && amountMinor == … && kind == …
    if found: statuses[i] = POSSIBLE; available.removeAt(j)
```

`fuzzyEqual(a, b)`: let `na`, `nb` = lower-case, only letters and digits. True when `na == nb`, or when `min(na.length, nb.length) >= 3` and one contains the other.

Why two passes: if one saved "Tesco" £4.00 matches two parsed rows (Tesco and Costa, both £4.00 that day), the Tesco row must get it, not whichever comes first.

## Tests (write first) — `DuplicateDetectorTest`

Helper `p(date, vendor, minor, kind = EXPENSE)` and `e(...)`. Dates around 2026-10-20.

1. Empty `existing` → all `NONE`; result size equals `parsed` size; empty `parsed` → empty list.
2. Same date, amount, kind, vendor → `LIKELY`.
3. Same date/amount/kind, completely different vendor → `POSSIBLE`.
4. Same vendor/amount, date +1 day → `POSSIBLE`; date −1 day → `POSSIBLE`; date +2 days → `NONE`.
5. Different amount → `NONE`. Different kind → `NONE`.
6. Multiset: 2 identical parsed, 1 existing → `[LIKELY, NONE]`; 1 parsed, 2 identical existing → `[LIKELY]`.
7. Fuzzy: parsed `Tesco Stores 3456` vs existing `Tesco` → `LIKELY`; `K F C` vs `KFC` → `LIKELY`; `BP` vs `Bpost` (shorter than 3 chars) → `POSSIBLE` (not LIKELY).
8. Pass priority: parsed `[Costa 400, Tesco 400]` (same day), existing `[Tesco 400]` → `[NONE, LIKELY]`.
9. Different year, same day/month → `NONE`.

## Reference code

**Verified (JVM tests pass):** `reference/main/dedupe/DuplicateDetector.kt.txt`. Try it yourself first; after 2 failed attempts copy the reference (rename `.kt.txt` to `.kt`, replace package `com.example.expenses` with `<PKG>`).

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*DuplicateDetectorTest"
```

## Do NOT

- Access repositories or Android classes here; the function receives plain lists.
- Treat two identical rows **within the same screenshot** as duplicates of each other (they may be two real purchases).

## Commit message

`feat(importer): add DuplicateDetector`
