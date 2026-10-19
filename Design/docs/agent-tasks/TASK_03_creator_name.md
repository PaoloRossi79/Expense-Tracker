# TASK_03 - Show the creator's name on transactions

**Goal:** every transaction (expense or money in) shows who created it. New transactions store the creator's name; older ones look the name up from the members list; if that fails, show "Unknown".
**Mode:** Code.
**Needs:** `PROJECT_MAP.md` sections 4, 5, 6, 7. Section 6 must name the existing creator-id field (the app already records who created each record).

## Behaviour (do not change it)
- The existing creator id field and the "only the creator can edit/delete" rule stay exactly as they are.
- New field `createdByName` on a transaction, set once at creation, never changed by edits.
- Name shown = `createdByName` if not blank, else the display name of the member whose id equals the creator id, else `Unknown`.
- Where it is shown: each row of the Monthly Transactions list (small secondary text, e.g. `by Paolo`) and the edit/detail screen (read-only line `Created by Paolo`).

---
## Step 1 - Display-name resolver + unit tests
Create `CreatorLabel.kt` in the same package as the Transaction model (use the real class/field names from PROJECT_MAP; the names below are placeholders):

```kotlin
object CreatorLabel {
    fun resolve(createdByName: String?, createdById: String?, memberNamesById: Map<String, String>): String =
        createdByName?.takeIf { it.isNotBlank() }
            ?: createdById?.let { memberNamesById[it] }?.takeIf { it.isNotBlank() }
            ?: "Unknown"
}
```
Unit tests (`CreatorLabelTest`):
- name `Anna`, id `u1`, map empty -> `Anna`
- name null, id `u1`, map `{u1: "Paolo"}` -> `Paolo`
- name `  ` (blank), id `u1`, map `{u1: "Paolo"}` -> `Paolo`
- name null, id `u2`, map `{u1: "Paolo"}` -> `Unknown`
- name null, id null, empty map -> `Unknown`
Run unit tests. Append to PROGRESS.md. STOP.

## Step 2 - Add the field to the transaction model
Open the Transaction model (PROJECT_MAP section 5). Add `val createdByName: String? = null` (with a default value, so old Firestore documents without the field still load). Use the same annotations/style as the other fields. If expenses and income use different classes, add the field to both.
Build. STOP.

## Step 3 - Provide the current user's display name
Look in PROJECT_MAP section 6 for how the display name is obtained.
1. If the code already has a function that returns the current member/user (with a name), reuse it.
2. Otherwise create `fun currentUserDisplayName(): String` in the class that already provides the current user id. Order: the member's name from the members collection if available; else the Google account `displayName`; else the part of the email before `@`; else `Unknown`.
Build. STOP.

## Step 4 - Set it when a transaction is created
Find the functions that create a NEW expense and a NEW money-in record (PROJECT_MAP section 5). In each, set `createdByName = currentUserDisplayName()` when building the object. Do not set it in the edit/update functions (an edit must keep the old value: check that the update function copies the existing `createdByName`, or writes only the edited fields).
Do not change the creator-id logic.
Build. Add a ViewModel unit test (fake repositories, same style as TASK_02A step 6): creating an expense sets `createdByName` to the fake user's name; editing it does not change `createdByName`. Run unit tests. STOP.

## Step 5 - Make member names available to the list
Open the Monthly Transactions ViewModel (PROJECT_MAP section 4).
1. Load the members list with the existing member repository function.
2. Expose `memberNamesById: Map<String, String>` (member id -> display name) as state, same style as the other state in this ViewModel.
Build. STOP.

## Step 6 - Show it in the UI
Check PROJECT_MAP section 1 (Compose or XML) and follow the style of the neighbouring code.
1. **Monthly Transactions row:** add a small secondary text line under the existing texts: `"by " + CreatorLabel.resolve(tx.createdByName, tx.<creatorIdField>, memberNamesById)`. Use the smallest/secondary text style already used for the date or category line. Add a test tag/id `transaction_creator`.
2. **Edit/detail screen:** add a read-only line `Created by <name>` (no input field). Resolve with the same function. If this screen has no access to `memberNamesById`, pass the already-resolved name from the list through the same mechanism used to pass the transaction (do not add a new data load).
Do not change sorting, filtering or the contextual menu.
Build. STOP.

## Step 7 - Finish
Run all unit tests. If the project has UI tests, add one: the list shows `by <name>` for a transaction created by a known user. Append to PROGRESS.md (Status DONE). STOP.

---
## Acceptance test (you, with the app)
1. Create a new expense: the list row shows `by <your name>`.
2. Edit it (change the amount): the name is unchanged.
3. Open an OLD transaction (created before this feature): it shows the name of the member who created it, or `Unknown` if that member is not in the members list.
4. Sign in as another family member and create a transaction: it shows that person's name.
5. Only the creator can still edit/delete (unchanged behaviour).
