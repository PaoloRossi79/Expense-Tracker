# T08 — Engine extensions: per-row dates, pending flag, avatar filter

**Depends on:** T07 · **Size:** M

## Goal

Complete the three profile options that T07 left unused, so the engine covers apps that print the date inside each row, mark pending items, and show a round avatar with letters on the left.

## Read first

`00-ARCHITECTURE.md` §5.2 steps 0, j, l and §5.4; `ProfileBasedParser.kt`; `ProfileBasedParserTest.kt` (reuse its helpers).

## Changes (only `ProfileBasedParser.kt` and the test file)

### 1. Avatar filter (step 0)

Before building bands: if `profile.avatarColumnFraction > 0.0`, drop every word that satisfies **all** of:

- `text.length <= 2`
- the text contains at least one **letter**
- `box.right <= avatarColumnFraction × ocr.imageWidth`

(Digits-only words such as the `5` of `5 Oct` must survive.) Build a new `OcrResult` with the remaining words and use it for the rest of the parse.

### 2. Per-row dates (step j)

For `DateSource.PER_ROW`, replace the `TODO(...)`:

- Look at the bands `index+1 … index+profile.subtitleLookahead` (as long as they exist).
- **Stop** scanning as soon as one of those bands is itself an amount band (its last segment is in the amount zone and `AmountParser.parse(...) != null`): that band belongs to the next transaction.
- In each scanned band call `DateResolver.findIn(band.text, today)`; the first hit is the date, `dateUncertain = false`.
- No hit → `date = today`, `dateUncertain = true`.

In `PER_ROW` mode the header logic (`currentDate`, header rows with daily totals) stays **disabled** (it already is, because it checks `dateSource == SECTION_HEADER`).

Extract the "is this an amount band?" test into a private function `isAmountBand(band, amountZoneLeft)` and reuse it in the main loop to avoid duplicated logic. Change the loop to `for ((index, band) in bands.withIndex())`.

### 3. Pending flag (step l)

If `profile.pendingRegex != null`: scan the same bands as in 2 (`index+1 … index+lookahead`, stopping at the next amount band); `pending = true` if any band's text matches the regex (`containsMatchIn`). Works for both `SECTION_HEADER` and `PER_ROW`. Add `pending` to the `ParsedTransaction`.

### 4. Warning text

Unchanged. (`"Dates above the first date header are unknown; today's date was used"` also covers the per-row case; the message is intentionally generic.)

## Tests (write first) — add to `ProfileBasedParserTest`

Per-row profile: `GENERIC_PROFILE.copy(dateSource = DateSource.PER_ROW)`; `today = 2026-10-24`.

1. Row `Tesco … -£12.50` followed (y+57) by a band `words("22 Oct · Card payment", 160)` → `2026-10-22`, `dateUncertain == false`.
2. Row with a date two bands below (lookahead 2) → found. Date three bands below → **not** found (`today`, uncertain).
3. Two consecutive transaction rows where only the second has a date line: the **first** row must **not** take the second row's date (scan stops at the next amount band) → `today`, uncertain; the second gets its own date.
4. Header-like band `Today` with no amount is ignored in PER_ROW mode (no crash, no transactions).

Pending (`pendingRegex = Regex("(?i)\\bpending\\b")`):

5. Subtitle `Pending` under a row → `pending == true`; a row without it → `false`.
6. `Pending` under the **second** row does not mark the first row.
7. Works in `SECTION_HEADER` mode too.

Avatar (`avatarColumnFraction = 0.15`; 0.15 × 1080 = 162):

8. Row with `word("T", 40)`, `word("Tesco", 160)`, `right("-£12.50")` → vendor `Tesco`. With fraction `0.0` the vendor is `T Tesco` (documents the default behaviour).
9. A header row `words("5 Oct", 40)` still parses as a header with the filter on (the `5` is digits-only).
10. A two-letter vendor starting right of the avatar zone (`word("BP", 170)`) is **not** dropped.

## Reference code

**Verified (JVM tests pass):** `reference/main/parsing/ProfileBasedParser.kt.txt` (the FINAL engine including T08); tests: `ProfileBasedParserTest` in `reference/test/parsing/BasicParsingTests.kt.txt`. Try it yourself first; after 2 failed attempts copy the reference (rename `.kt.txt` to `.kt`, replace package `com.example.expenses` with `<PKG>`).

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*ProfileBasedParserTest"
```

and then the whole unit-test suite is green.

## Do NOT

- Change the default values of any profile field.
- Add `TODO()` anywhere; after this task none may remain.
- Handle dates printed **inside** the vendor/amount row (middle segment): that is a bank-specific quirk for a `postProcess` hook (see T16).

## Commit message

`feat(importer): per-row dates, pending flag and avatar filter in engine`
