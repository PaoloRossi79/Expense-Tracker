# TASK_02C (OPTIONAL) - Import vendors from existing expenses

**Why:** TASK_02A only records vendors from expenses saved AFTER it was installed. This task adds a button that scans old expenses once and fills the vendor list (including each vendor's last category). Skip it if you are happy to build the list over time.
**Mode:** Code. **Needs:** TASK_02A and TASK_02B finished. `PROJECT_MAP.md` sections 4, 5.

## Behaviour
- A button "Import vendors from past expenses" on the Manage Categories screen.
- It reads all existing expense transactions (not income), oldest to newest, and for each vendor key keeps the name and the category of the NEWEST expense.
- It writes only vendors that do not exist yet (never overwrites a vendor that already has data from the live app).
- It can be pressed twice safely (nothing duplicates).
- It does not modify any transaction.

## Step 1 - Pure grouping logic + tests
Create `VendorBackfill.kt`:

```kotlin
data class PastExpense(val vendor: String, val categoryId: String?, val dateMillis: Long)

object VendorBackfill {
    /** Returns one Vendor per distinct key, using the newest expense for name+category. Blank vendors are skipped. */
    fun build(expenses: List<PastExpense>): List<Vendor> =
        expenses
            .filter { it.vendor.isNotBlank() }
            .sortedBy { it.dateMillis }
            .associateBy({ VendorNameFormatter.toKey(it.vendor) }) { it }
            .map { (key, e) ->
                Vendor(
                    key = key,
                    name = VendorNameFormatter.toDisplayName(e.vendor),
                    lastCategoryId = e.categoryId,
                )
            }
}
```
(`associateBy` keeps the last element for a repeated key, and the list is sorted oldest to newest, so the newest wins.)
Unit tests: (a) `tesco`(old, cat A) and `TESCO `(new, cat B) -> one vendor `Tesco`, category B; (b) blank vendor skipped; (c) two different vendors -> two results; (d) empty list -> empty result.
Run unit tests. Append to PROGRESS.md. STOP.

## Step 2 - Repository function
Add to `VendorRepository` and its Firestore implementation:
`suspend fun importMissing(vendors: List<Vendor>): Int` which, for each vendor, writes the document `<vendors path>/<key>` ONLY if it does not exist (check with a `get()` per document, or `set` inside a transaction). Use batches of at most 400 writes. Return how many were created. Do not touch existing documents.
Build. STOP.

## Step 3 - Use case + button
1. In the Manage Categories ViewModel add `fun importVendorsFromPastExpenses()` that: loads all expense transactions using the existing transaction repository function (PROJECT_MAP section 5), maps them to `PastExpense`, calls `VendorBackfill.build`, then `importMissing`, and exposes the count as a one-time message (use the same snackbar/toast mechanism the screen already has).
2. Add the button on the Manage Categories screen, calling that function. Show the result message "Imported N vendors".
Build. Run unit tests. Append to PROGRESS.md (Status DONE). STOP.

## Acceptance test
1. Press the button: message "Imported N vendors" with N > 0 (if you had old expenses).
2. Press again: "Imported 0 vendors".
3. Open Add Expense and start typing an old vendor: it is suggested, with its most recent category.
