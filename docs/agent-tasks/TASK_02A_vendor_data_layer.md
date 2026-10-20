# TASK_02A - Vendors: storage, Pascal Case, no duplicates, last category

**Goal:** the app remembers every vendor ever used on an expense, stored once per vendor in a nice "Pascal Case" form, together with the category used the last time that vendor was used. This task has NO screen changes (UI comes in TASK_02B).
**Mode:** Code.
**Needs:** `PROJECT_MAP.md` sections 2, 5, 6, 7.
**If PROJECT_MAP says the project is Java, write Java instead of the Kotlin below with the same logic.**

## The design (do not change it)
- One Firestore collection `vendors`, placed in the same scope as the `categories` collection (same parent path as categories; see PROJECT_MAP section 5).
- **Document ID = the vendor "key"** (lower-case, trimmed, single spaces). Same key = same vendor = no duplicates, enforced by Firestore itself.
- Document fields: `name` (display name, Pascal Case), `lastCategoryId` (string or null), `updatedAt` (timestamp), `updatedBy` (user id).
- Pascal Case here means: first letter of each word upper-case, the rest lower-case, spaces kept. `"tesco  extra "` -> `"Tesco Extra"`.
- A vendor is saved/updated each time a NEW expense is saved. Editing an old expense does not update vendors.
- Blank vendors are ignored.

---
## Step 1 - Name formatter + unit tests
Check if a vendor/name formatter already exists (search `Vendor`). If not, create a new file `VendorNameFormatter.kt` in the same package as the other model/util classes:

```kotlin
import java.util.Locale

object VendorNameFormatter {
    /** "  tesco   EXTRA " -> "Tesco Extra" */
    fun toDisplayName(raw: String): String =
        raw.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.lowercase(Locale.ROOT)
                    .replaceFirstChar { it.titlecase(Locale.ROOT) }
            }

    /** Firestore document id: same vendor in any spelling gives the same key. */
    fun toKey(raw: String): String =
        toDisplayName(raw).lowercase(Locale.ROOT).replace("/", "_")
}
```
Create `VendorNameFormatterTest.kt` in the unit test folder with these cases (all must pass):

| Input | toDisplayName | toKey |
|---|---|---|
| `tesco` | `Tesco` | `tesco` |
| `  tesco   extra ` | `Tesco Extra` | `tesco extra` |
| `TESCO EXTRA` | `Tesco Extra` | `tesco extra` |
| `SAINSBURY'S` | `Sainsbury's` | `sainsbury's` |
| `m&s` | `M&s` | `m&s` |
| `` (empty) | `` | `` |
| `a/b` | `A/b` | `a_b` |

Also assert `toKey("TESCO") == toKey("tesco ")`.
Run the unit tests. Append to PROGRESS.md. STOP.

## Step 2 - Vendor model
Check PROJECT_MAP section 5 "Vendor model or vendor-to-category mapping". If a Vendor class already exists, **extend it** with the missing fields instead of creating a second class. Otherwise create next to the Category model:

```kotlin
data class Vendor(
    val key: String = "",            // document id
    val name: String = "",           // display name, Pascal Case
    val lastCategoryId: String? = null,
    val updatedAt: com.google.firebase.Timestamp? = null,
    val updatedBy: String? = null,
)
```
(All fields have default values so Firestore can build the object. Match the style the other model classes use for Firestore, e.g. `@DocumentId` on `key` if the other models do that.)
Build. STOP.

## Step 3 - Repository functions
Find the repository that handles categories (PROJECT_MAP section 5). Create a new `VendorRepository` interface and a Firestore implementation next to it, following the exact same pattern (constructor, scope path, DI):

```kotlin
interface VendorRepository {
    /** All vendors, newest first. Emits again when data changes. */
    fun observeVendors(): kotlinx.coroutines.flow.Flow<List<Vendor>>

    /** Call after a new expense is saved. Creates or updates the vendor. Ignores blank names. */
    suspend fun recordUsage(rawName: String, categoryId: String?)
}
```
Implementation of `recordUsage`:
1. `val name = VendorNameFormatter.toDisplayName(rawName)`; if `name.isEmpty()` return.
2. `val key = VendorNameFormatter.toKey(rawName)`.
3. Write one document at `<vendors path>/<key>` with `set(map, SetOptions.merge())` where map = `name`, `lastCategoryId` (the argument), `updatedAt` (server timestamp), `updatedBy` (current user id from PROJECT_MAP section 6).
4. Await the result like other repository functions do (`.await()`).

Implementation of `observeVendors`: a snapshot listener (or the same technique used by the category repository) on the `vendors` collection ordered by `updatedAt` descending; map documents to `Vendor` and set `key = document.id`.
Register the repository wherever the category repository is registered (DI module / manual factory).
Build. Append to PROGRESS.md. STOP.

## Step 4 - Firestore rules
1. If PROJECT_MAP section 5 gives a rules file in the repo: copy the rule block that covers `categories`, paste it directly under itself, replace `categories` with `vendors`. Do not change anything else.
2. If rules are NOT in the repo: create `docs/agent-tasks/MANUAL_STEPS.md` containing the text "In the Firebase console -> Firestore -> Rules, add a `vendors` rule identical to the existing `categories` rule (same path pattern, same conditions)" and show the current categories rule if known. Tell the user this is a manual step.
Append to PROGRESS.md. STOP.

## Step 5 - Record usage when an expense is saved
Open the ViewModel of Add Expense (PROJECT_MAP section 4) and the function that saves a new expense (section 5).
1. Inject `VendorRepository` the same way the other repositories are injected.
2. Before saving, replace the vendor text with `VendorNameFormatter.toDisplayName(vendorText)` so the transaction itself also stores the formatted name.
3. **After** the expense was saved successfully (not before, not on failure), call `vendorRepository.recordUsage(vendorText, selectedCategoryId)`. If this call throws, log it and do NOT fail the expense save.
4. Do not touch Add Money In.
Build. STOP.

## Step 6 - ViewModel test
In the existing unit test folder add a test for the Add Expense ViewModel using a fake `VendorRepository` (a small class in the test that records the arguments it received; do not use the Firestore emulator). Test:
1. Saving an expense with vendor `"  tesco "` and category `"cat-groceries"` calls `recordUsage` once with those values, and the saved expense has vendor `"Tesco"`.
2. Saving with a blank vendor does not crash (and `recordUsage` either is not called or ignores it).
3. If the fake throws from `recordUsage`, the expense is still saved.
Follow how existing ViewModel tests (PROJECT_MAP section 7) create the ViewModel and fake repositories. Run unit tests. Append to PROGRESS.md (Status DONE). STOP.

---
## Acceptance test (you, with the app)
1. Add an expense with vendor `tesco` and category Groceries. In the Firebase console you see a vendor document with id `tesco`, name `Tesco`, `lastCategoryId` = Groceries.
2. Add another with vendor `TESCO ` and category Household. Still ONE document; `lastCategoryId` is now Household.
3. Add one with vendor `Costa Coffee`. A second document `costa coffee` appears.
4. The expense list shows vendors as `Tesco`, `Tesco`, `Costa Coffee`.
