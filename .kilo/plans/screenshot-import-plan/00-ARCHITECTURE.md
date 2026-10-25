# 00 — Architecture

Read this before any task. Names here are binding: tasks use them exactly.

`<PKG>` = base package from `docs/APP_CONTEXT.md` (created in T00). `<SRC>` = main source root recorded there. New code lives in the sub-package `<PKG>.importer`. (`importer`, not `import`, because `import` is a Kotlin keyword.)

## 1. Goals and hard constraints

1. Input: one screenshot (or photo) of a bank-app transaction list. Output: a list of transactions (date, vendor, amount, expense/income) ready to review and save.
2. **Everything runs on the phone.** No network call may be made with image data or OCR text. No cloud OCR, no cloud LLM.
3. Pure-Kotlin parsing core with **no Android imports**, so every rule is unit-testable on the JVM in seconds.
4. One parser per bank (a configuration profile plus optional code). The user chooses the bank.
5. The user always reviews before saving. The parser may be wrong; the review screen is the safety net.
6. Reuse what the app already has: vendor Pascal-Case normaliser, last-used category per vendor, creator stamping, repositories.

## 2. Pipeline

```
 image (gallery picker / share sheet)
   │
   ▼
 OcrEngine.recognise(ImageRef)                 ML Kit, on device          → OcrResult (words + boxes)
   │
   ▼  (optional, T24) Deskewer            fixes tilted photos         → OcrResult
   │
   ▼
 ParserRegistry.forBank(bank).parse(ocr, today)                       → ParseResult (ParsedTransaction[], warnings)
   │
   ▼
 ImportService.prepare(...)               normalise vendor, suggest category,
   │                                      drop income (default), flag duplicates → PreparedImport (ReviewItem[])
   ▼
 ReviewScreen (user edits / unticks)
   │
   ▼
 ImportService.commit(items)              writes via the app's repository, creator = current user
```

## 3. Package and file layout

```
<SRC>/<PKG>/importer/
  model/
    OcrModels.kt            OcrBox, OcrWord, OcrResult
    ImageRef.kt             ImageRef
    ParsedModels.kt         Bank, TransactionKind, ParsedTransaction, ParseResult
  parsing/
    AmountParser.kt         ParsedAmount, AmountSign, AmountParser
    DateResolver.kt
    VendorCleaner.kt
    LayoutAnalyzer.kt       Segment, Band, LayoutAnalyzer
    ParserProfile.kt        DateSource, IncomeRule, ParserProfile
    ProfileBasedParser.kt   the engine
    BankStatementParser.kt  interface
    ParserRegistry.kt
    banks/
      GenericParser.kt  RevolutParser.kt  MonzoParser.kt  BarclaysParser.kt  ChaseParser.kt
  ocr/
    OcrEngine.kt            interface
    MlKitOcrEngine.kt       ML Kit implementation
    Deskewer.kt             (T24, optional)
  dedupe/
    DuplicateDetector.kt    DuplicateStatus, ExistingTx, DuplicateDetector
  domain/
    ImportModels.kt         ReviewItem, PreparedImport, CommitResult, ports (interfaces)
    ImportService.kt
    adapters/               thin classes that connect the ports to the app's real repositories
  ui/
    ImportViewModel.kt  ImportScreen.kt  ReviewScreen.kt

<SRC-debug>/…/OcrDumpActivity.kt          debug build only (T11)

app/src/test/…/importer/…                 unit tests, same package names as the code
app/src/test/resources/fixtures/<bank>/   <name>.ocr.json + <name>.expected.txt
```

## 4. Data model (copy exactly)

```kotlin
package <PKG>.importer.model

import java.time.LocalDate

/** Pixel box in the coordinate space of the recognised image. */
data class OcrBox(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Double get() = (left + right) / 2.0
    val centerY: Double get() = (top + bottom) / 2.0
}

/** One recognised word (ML Kit "Element"). angleDeg is 0 for an upright word. */
data class OcrWord(val text: String, val box: OcrBox, val angleDeg: Float = 0f)

data class OcrResult(val imageWidth: Int, val imageHeight: Int, val words: List<OcrWord>)

/** Pointer to the picked image (text of a content:// URI). Keeps the importer free of android.net.Uri, which cannot be created in JVM unit tests. */
data class ImageRef(val uri: String)

enum class Bank(val displayName: String) {
    REVOLUT("Revolut"),
    MONZO("Monzo"),
    BARCLAYS("Barclays"),
    CHASE("Chase"),
    GENERIC("Other / generic"),
}

enum class TransactionKind { EXPENSE, INCOME }

data class ParsedTransaction(
    val date: LocalDate,
    val vendor: String,          // cleaned by VendorCleaner, NOT yet Pascal-Cased
    val amountMinor: Long,       // always positive, in pence (12.50 -> 1250)
    val kind: TransactionKind,
    val rawText: String,         // text of the source row, for debugging / review
    val pending: Boolean = false,
    val dateUncertain: Boolean = false,   // true when no date header/row date was found
)

data class ParseResult(val transactions: List<ParsedTransaction>, val warnings: List<String>)
```

```kotlin
package <PKG>.importer.parsing

interface BankStatementParser {
    val bank: Bank
    fun parse(ocr: OcrResult, today: LocalDate): ParseResult
}
```

Money is **always `Long` pence** inside the importer. Conversion to the app's own amount type happens only in `ImportService.commit` (T19).

## 5. The parsing engine (used by every bank)

The engine is generic; a **bank = a `ParserProfile`** (settings) plus an optional `postProcess` hook.

### 5.1 Step A — bands and segments (`LayoutAnalyzer`, T06)

1. `medianHeight` = median of `word.box.height` (ignore height ≤ 0; if none, use 1).
2. Sort words by `centerY`. A word joins the current **band** if `abs(word.centerY − band.meanCenterY) <= bandTolerance × medianHeight`, otherwise it starts a new band.
3. Inside a band sort by `left`. Start a new **segment** when `word.left − previousWord.right > segmentGapFactor × medianHeight`. A segment's text is its words joined by one space; its box is the union of the word boxes.
4. Result: bands top → bottom, each with segments left → right.

So a typical row `Tesco ............ -£12.50` is one band with two segments; the big gap splits vendor from amount.

### 5.2 Step B — walk the bands (`ProfileBasedParser`, T07/T08)

```
parse(ocr, today):
 0. (T08) if profile.avatarColumnFraction > 0: drop words with text.length <= 2 and box.right <= fraction * imageWidth
 1. bands = LayoutAnalyzer.buildBands(...)
 2. currentDate = null; results = []; counters for skipped rows
 3. for each band (index i):
    a. last = band.segments.last()
    b. inAmountZone = last.box.left >= profile.minAmountLeftFraction * ocr.imageWidth
    c. amount = if (inAmountZone) AmountParser.parse(last.text) else null
    d. if inAmountZone && amount == null && last.text looks like a non-GBP amount (€ $ ¥ + digits): skippedForeign++ ; continue
    e. if amount == null:                       // not a transaction row
         in SECTION_HEADER mode: DateResolver.parseExact(band.text, today)?.let { currentDate = it }
         continue
    f. vendorRaw = text of all segments except the last
    g. in SECTION_HEADER mode: if DateResolver.parseExact(vendorRaw, today) != null → it is a date header that also shows a
         daily total: currentDate = that date; continue
    h. vendor = VendorCleaner.clean(vendorRaw, profile.vendorNoisePrefixes)
    i. if vendor is blank → skippedNoVendor++ ; continue
       if any profile.ignoreVendorRegexes.containsMatchIn(vendor) → continue
       if amount.minor == 0 → skippedZero++ ; continue
    j. date:
         SECTION_HEADER: currentDate ?: today (dateUncertain = true)
         PER_ROW (T08):  first DateResolver.findIn(...) on bands i+1 .. i+subtitleLookahead, stopping at the next amount band;
                         if none → today (dateUncertain = true)
    k. kind from profile.incomeRule + amount.sign (section 5.3)
    l. (T08) pending = any of bands i+1 .. i+subtitleLookahead (stop at next amount band) matches profile.pendingRegex
    m. add ParsedTransaction(date, vendor, amount.minor, kind, rawText = band.text, pending, dateUncertain)
 4. warnings (exact texts, tests rely on them):
      "No transactions found"                         if results is empty
      "Dates above the first date header are unknown; today's date was used"   if any dateUncertain row
      "Skipped N amount(s) with no vendor name"       if skippedNoVendor > 0
      "Skipped N non-GBP amount(s)"                   if skippedForeign > 0
      "Skipped N zero amount(s)"                      if skippedZero > 0
 5. results = profile.postProcess(results)   (default: identity)
```

### 5.3 Income rule

| `IncomeRule` | Meaning | Expense | Income |
|---|---|---|---|
| `PLUS_MARKS_INCOME` (default) | app shows `+£` on money in | `-£x`, or no sign | `+£x` |
| `UNSIGNED_IS_INCOME` | app shows `-£` on spending and no sign on money in | `-£x` | `£x` or `+£x` |

Colour (green/red) cannot be read from OCR, so the sign is the only signal. The review screen lets the user fix anything wrong.

### 5.4 `ParserProfile` fields

| Field | Default | Meaning |
|---|---|---|
| `bank` | – | which `Bank` |
| `dateSource` | `SECTION_HEADER` | `SECTION_HEADER`: dates are group headers ("Today", "22 October"). `PER_ROW`: each row has its own date in a subtitle |
| `incomeRule` | `PLUS_MARKS_INCOME` | see 5.3 |
| `minAmountLeftFraction` | 0.55 | amount text must start at or right of this fraction of the image width |
| `bandTolerance` | 0.6 | vertical tolerance for "same row", as a fraction of median word height |
| `segmentGapFactor` | 1.5 | horizontal gap (× median word height) that splits segments |
| `vendorNoisePrefixes` | empty | e.g. `"Card payment to"`; removed from the start of vendor text |
| `ignoreVendorRegexes` | built-in list | rows whose vendor matches are dropped (balances, totals) |
| `subtitleLookahead` | 2 | how many bands below a row are searched for date / pending |
| `pendingRegex` | null | e.g. `(?i)\bpending\b` |
| `avatarColumnFraction` | 0.0 (off) | drop 1–2 character words in the left avatar column |

Built-in `DEFAULT_IGNORE_VENDOR_REGEXES` (all case-insensitive):
`^(balance|money in|money out|available balance|left to spend)\b`, `^total (in|out|spent|spend)\b`.

## 6. File formats used by tests

**OCR fixture** `fixtures/<bank>/<name>.ocr.json`:

```json
{"imageWidth":1080,"imageHeight":2400,
 "words":[{"text":"Tesco","left":160,"top":380,"right":270,"bottom":420,"angleDeg":0.0}]}
```

**Expected file** `fixtures/<bank>/<name>.expected.txt` (hand-written by you from the real screenshot):

```
# today: 2026-10-24
# bank: MONZO
2026-10-24 | Tesco | 12.50 | EXPENSE
2026-10-23 | Salary | 2500.00 | INCOME
2026-10-22 | Costa | 3.40 | EXPENSE | PENDING,DATE_UNCERTAIN
```

Columns: date | vendor | amount (decimal, positive) | EXPENSE or INCOME | optional flags (`PENDING`, `DATE_UNCERTAIN`). A missing flags column means neither flag is set. Lines starting with `#` are headers/comments. Vendor is compared case-insensitively after collapsing spaces. Order = top to bottom of the screenshot.

## 7. Duplicate rules (T17)

Compared with transactions already saved (date range = parsed min − 1 day … max + 1 day), as a **multiset**: each saved transaction can explain **at most one** parsed row (two identical coffees on one day are legitimate).

1. Pass 1: same date + same amount + same kind + vendors "fuzzy equal" → `LIKELY`.
2. Pass 2 (on what is left): date within ±1 day + same amount + same kind → `POSSIBLE`.
3. Everything else → `NONE`.

Fuzzy equal: normalise (lower-case, keep letters and digits only); equal, or one contains the other (min length 3).
Review defaults: `LIKELY` → unticked; `POSSIBLE` → ticked with a warning badge.

## 8. Privacy rules (hard rules, tested in T25)

- Never copy, cache or persist the image. Read it straight from the picked URI (carried as `ImageRef`).
- Never log OCR text, vendors or amounts anywhere in `importer/` (no `Log.`, `println`, `Timber`). A unit test enforces this.
- Firestore receives only the same fields as a manually entered transaction.
- The OCR dump tool exists only in the `debug` source set and must not appear in a release build.
- Test fixtures committed to git must be anonymised (T09 has a safety test; `02-…` has the checklist).
- Raw (non-anonymised) dumps are never pasted into an AI chat, local or cloud.

## 9. Testing strategy

| Layer | Tool | Where |
|---|---|---|
| Amount/date/vendor/layout/engine rules | JUnit4, synthetic `ocr { }` builder (T06) | `src/test` |
| Per-bank accuracy | Golden fixtures: real OCR JSON (anonymised) + your hand-written expected file (T09) | `src/test/resources/fixtures` |
| Dedupe, service, ViewModel | JUnit4 with fakes for every port | `src/test` |
| ML Kit | One small instrumented test that OCRs a generated bitmap (T10) | `src/androidTest` |
| Review screen | Compose UI test with fake state (T26) | `src/androidTest` |
| Real phone | Manual QA checklist (T26) | by you |

## 10. Conventions for generated code

- Kotlin, 4-space indent, no wildcard imports, explicit types on public functions.
- Pure logic = top-level `object` or small `class` with constructor-injected ports; no singletons holding state.
- No `!!`. No `TODO()` left in committed code (except the explicit `PER_ROW` stub in T07, removed in T08).
- Every public function in `parsing/` has at least one unit test.
- Time is passed in (`today: LocalDate`), never read inside the parsers.

## 11. Known limits (accepted for v1)

- GBP only; amounts need two decimals.
- Colour is invisible to OCR, so income/expense relies on signs.
- One image per import. Overlapping screenshots are handled by duplicate detection.
- A bank's app update can change its layout: add a new fixture, adjust the profile (see T16).
- Strongly tilted photos of a screen are only handled after T24, and screenshots are much more accurate than photos. Some banking apps block screenshots; if yours does, see T24.
