# T19 — `ImportService.commit`

**Depends on:** T18 · **Size:** S

## Goal

Save the selected review items through the app's normal repository, so imported records look exactly like manually entered ones (including the creator).

## Read first

`ImportService.kt`, `ImportModels.kt` (`NewTransaction`, `TransactionWriter`, `CommitResult` already exist), `docs/APP_CONTEXT.md` (repository `add`, current user, amount and date types).

## Changes

### 1. `ImportService.commit`

```kotlin
suspend fun commit(items: List<ReviewItem>): CommitResult
```

- Take `items.filter { it.selected }`, sorted by `date` ascending (stable).
- For each: if `category == null` → `skippedNoCategory++`, **not written**. Otherwise call `writer.add(NewTransaction(date, vendor, amountMinor, kind, category.id))`; success → `saved++`; any `Exception` except `CancellationException` → `failed++` (keep going with the next one).
- Return `CommitResult(saved, failed, skippedNoCategory)`.

### 2. Adapter `AppTransactionWriter` (`…/importer/domain/adapters/`)

Maps `NewTransaction` to the app's real transaction model and calls the existing repository `add` function:

- amount: `amountMinor` → the app's amount type (`BigDecimal(amountMinor).movePointLeft(2)` or `amountMinor / 100.0` only if the app's model really uses `Double`);
- date → the app's date type;
- creator: whatever the app already does for manual entries (the repository or the current-user provider found in `APP_CONTEXT.md`); do **not** invent a second mechanism;
- category by id; vendor as given.

After a successful add, also call the same vendor-usage recording that Add Expense uses (APP_CONTEXT, "where vendor usage is recorded"), so autocomplete and the last-used category learn from imports; if it lives in a ViewModel, extract it to a shared function in a separate commit first.

**Offline writes:** with Firestore offline persistence an awaited `add` never completes while offline (it is only queued). Wrap the write in `withTimeoutOrNull(5_000)`; a timeout means "queued" and counts as saved when persistence is enabled, otherwise as failed. Otherwise the SAVING screen would hang.

If the repository's `add` is not `suspend`, wrap it with `suspendCancellableCoroutine`/`await()` the way the existing code does.

## Tests (write first) — `ImportServiceCommitTest` (fake `TransactionWriter` recording calls)

1. Only `selected` items are written.
2. Items with `category == null` are not written and counted in `skippedNoCategory`.
3. Writer throws for the 2nd of 3 → `saved == 2`, `failed == 1`; the 3rd is still attempted.
4. Written in ascending date order.
5. Empty list → `CommitResult(0, 0, 0)`; the writer is not called.
6. A `CancellationException` thrown by the writer propagates (not counted as failure).
7. `NewTransaction` carries the item's vendor (already normalised in `prepare`), minor amount, kind and `category.id`.

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*ImportServiceCommitTest"
./gradlew :app:assembleDebug
```

## Do NOT

- Batch or parallelise writes in this task (order and error counting must stay simple). Firestore queues offline writes by itself.
- Change repository code.
- Log anything about the items.

## Commit message

`feat(importer): add ImportService.commit and transaction writer adapter`
