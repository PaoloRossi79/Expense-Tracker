# TASK_02B - Vendor autocomplete + category auto-fill (Add Expense screen)

**Goal:** on Add Expense, typing in the Vendor field shows matching existing vendors. Picking one fills the Vendor and auto-selects its last used category. Typing a new vendor that is not in the list is still allowed.
**Mode:** Code.
**Needs:** TASK_02A finished. `PROJECT_MAP.md` sections 1, 4, 5, 7.
**Only the Add Expense screen changes. Do NOT touch Add Money In.**

## Behaviour (do not change it)
1. Suggestions appear while typing; matching is case-insensitive.
2. Matches that START with the typed text come first, then matches that CONTAIN it. Maximum 8 suggestions.
3. If the field is empty and focused, show the 8 most recently used vendors.
4. Tapping a suggestion: Vendor text = the vendor's `name`, and the Category field = the vendor's `lastCategoryId` **only if** that id exists in the current list of expense categories. Otherwise leave Category unchanged.
5. After auto-fill the user can still change the category.
6. Typing something not in the list is fine. It is saved as a new vendor by TASK_02A when the expense is saved. No extra "add" button.
7. Category is auto-filled only on tapping a suggestion, never while just typing.

---
## Step 1 - Pure suggestion logic + unit tests
Create `VendorSuggestions.kt` next to `VendorNameFormatter.kt`:

```kotlin
import java.util.Locale

object VendorSuggestions {
    fun filter(vendors: List<Vendor>, query: String, limit: Int = 8): List<Vendor> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return vendors.take(limit) // list is already newest first
        val starts = vendors.filter { it.name.lowercase(Locale.ROOT).startsWith(q) }
        val contains = vendors.filter {
            val n = it.name.lowercase(Locale.ROOT)
            !n.startsWith(q) && n.contains(q)
        }
        return (starts + contains).take(limit)
    }

    /** Category to auto-select for this vendor, or null to leave the field alone. */
    fun categoryFor(vendor: Vendor, expenseCategoryIds: Set<String>): String? =
        vendor.lastCategoryId?.takeIf { it in expenseCategoryIds }
}
```
Create `VendorSuggestionsTest.kt` testing with vendors `Tesco`, `Tesco Extra`, `Costa Coffee`, `Boots`:
- query `te` -> `Tesco`, `Tesco Extra` (both start with it); `Costa Coffee` is NOT returned.
- query `ee` -> `Costa Coffee` (contains) only.
- query `TESCO` (upper-case) -> `Tesco`, `Tesco Extra`.
- query `` -> the first 8 of the list in the same order.
- query `zzz` -> empty list.
- with 12 vendors and query `` -> exactly 8.
- `categoryFor` returns the id when present in the set, returns null when `lastCategoryId` is null, and null when the id is not in the set.
Run unit tests. Append to PROGRESS.md. STOP.

## Step 2 - ViewModel state
Open the Add Expense ViewModel (PROJECT_MAP section 4).
1. Inject `VendorRepository` if not injected yet (done in 02A, step 5).
2. Expose the vendor list as state, using the same state type the ViewModel already uses (e.g. `StateFlow<List<Vendor>>` collecting `observeVendors()`).
3. Add `fun onVendorSelected(vendor: Vendor)` that sets the vendor text to `vendor.name` and sets the selected category to `VendorSuggestions.categoryFor(vendor, <ids of the expense categories already loaded in this ViewModel>)` when it is not null.
4. Do not change how the expense is saved.
Build. STOP.

## Step 3 - ViewModel test
Add a test with fake repositories (same approach as TASK_02A step 6):
1. Vendors `[Tesco(lastCategoryId="groceries")]`, expense categories `[groceries, transport]`. Call `onVendorSelected(Tesco)`. Expect vendor text `Tesco` and selected category `groceries`.
2. Same vendor but `lastCategoryId="deleted-cat"` (not in the list), with category `transport` already selected. After `onVendorSelected`, the category is still `transport`.
Run unit tests. STOP.

## Step 4 - The UI field
Check PROJECT_MAP section 1 for the UI toolkit and follow ONLY the matching part.

**If Jetpack Compose:** replace the plain Vendor text field with an editable dropdown (Material 3 `ExposedDropdownMenuBox` containing an `OutlinedTextField` (editable, not read-only) and an `ExposedDropdownMenu`). Rules:
- `value` = the ViewModel vendor text; `onValueChange` updates the ViewModel vendor text and sets `expanded = suggestions.isNotEmpty()`.
- `suggestions = VendorSuggestions.filter(vendors, vendorText)`.
- Each `DropdownMenuItem` shows `vendor.name`; its `onClick` calls `viewModel.onVendorSelected(vendor)` and sets `expanded = false`.
- Keep the same label, keyboard options and `Modifier.testTag` the old field had (the existing tests use them).
- Use the `menuAnchor()` modifier on the text field as the Material 3 version in the project requires (check how other dropdowns in the app do it, e.g. the Category dropdown, and copy that pattern exactly).

**If XML views:** replace the vendor `EditText`/`TextInputEditText` with `MaterialAutoCompleteTextView` (inside the same `TextInputLayout`; use style `ExposedDropdownMenu` only if the Category field already uses it). Set `threshold = 0` so suggestions show on focus. Update the adapter with `VendorSuggestions.filter(...)` names whenever the vendor list or text changes. In `setOnItemClickListener`, find the vendor by name and call `viewModel.onVendorSelected(vendor)`.

Do not change any other part of the screen. Build. STOP.

## Step 5 - UI test (only if the project already has UI tests)
Following the example in PROJECT_MAP section 7: type `Te` in the vendor field, assert the suggestion `Tesco` is shown, click it, assert the category field shows the category of Tesco. If there is no UI-test setup, write `NOT FOUND: UI test setup` in PROGRESS.md and skip.

## Step 6 - Finish
Run all unit tests. Append to PROGRESS.md (Status DONE). STOP.

---
## Acceptance test (you, with the app)
Prerequisite: you did the TASK_02A acceptance test, so `Tesco` (last category Household) and `Costa Coffee` exist.
1. Add Expense -> tap Vendor: you see recent vendors.
2. Type `tes`: `Tesco` appears. Tap it: Vendor = `Tesco`, Category = Household.
3. Change the category to Groceries and save. Add another expense, pick `Tesco` again: Category = Groceries (the latest one).
4. Type `Marks and Spencer` (new), choose a category, save. Next time `mar` suggests `Marks And Spencer`.
5. Type `TESCO` in capitals and save: no duplicate appears in the list.
6. Delete the category Household in Manage Categories, then pick a vendor whose last category was Household: the Category stays as it was (no crash).
