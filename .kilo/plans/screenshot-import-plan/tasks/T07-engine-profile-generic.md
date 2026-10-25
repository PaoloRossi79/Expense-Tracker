# T07 — `ParserProfile`, the engine, `GenericParser`, registry

**Depends on:** T03, T04, T05, T06 · **Size:** L (the heart of the feature; take your time)

## Goal

Implement the generic parsing engine described in `00-ARCHITECTURE.md` §5.2, driven by a `ParserProfile`, for **section-header dates** (rows grouped under "Today", "22 October" …). A bank parser will later be "this engine + a profile". Also add `GenericParser` and `ParserRegistry`.

PER_ROW dates, pending flags and the avatar filter come in **T08**; leave them as described below.

## Read first

`00-ARCHITECTURE.md` §5 (whole), the test builder from T06, and the signatures of `AmountParser`, `DateResolver`, `VendorCleaner`, `LayoutAnalyzer`.

## Create

| File | Content |
|---|---|
| `…/importer/parsing/ParserProfile.kt` | `DateSource`, `IncomeRule`, `DEFAULT_IGNORE_VENDOR_REGEXES`, `ParserProfile`, `GENERIC_PROFILE` |
| `…/importer/parsing/ProfileBasedParser.kt` | the engine |
| `…/importer/parsing/banks/GenericParser.kt` | `object GenericParser : BankStatementParser by ProfileBasedParser(GENERIC_PROFILE)` |
| `…/importer/parsing/ParserRegistry.kt` | `ParserRegistry.forBank(bank)` |
| `…/test/…/ProfileBasedParserTest.kt`, `ParserRegistryTest.kt` | tests below |

## `ParserProfile.kt`

```kotlin
enum class DateSource { SECTION_HEADER, PER_ROW }
enum class IncomeRule { PLUS_MARKS_INCOME, UNSIGNED_IS_INCOME }

val DEFAULT_IGNORE_VENDOR_REGEXES: List<Regex> = listOf(
    Regex("^(balance|money in|money out|available balance|left to spend)\\b", RegexOption.IGNORE_CASE),
    Regex("^total (in|out|spent|spend)\\b", RegexOption.IGNORE_CASE),
)

data class ParserProfile(
    val bank: Bank,
    val dateSource: DateSource = DateSource.SECTION_HEADER,
    val incomeRule: IncomeRule = IncomeRule.PLUS_MARKS_INCOME,
    val minAmountLeftFraction: Double = 0.55,
    val bandTolerance: Double = 0.6,
    val segmentGapFactor: Double = 1.5,
    val vendorNoisePrefixes: List<String> = emptyList(),
    val ignoreVendorRegexes: List<Regex> = DEFAULT_IGNORE_VENDOR_REGEXES,
    val subtitleLookahead: Int = 2,        // used from T08
    val pendingRegex: Regex? = null,       // used from T08
    val avatarColumnFraction: Double = 0.0, // used from T08
)

val GENERIC_PROFILE = ParserProfile(bank = Bank.GENERIC)
```

## `ProfileBasedParser.kt` — reference implementation

Follow §5.2 of the architecture. This is a near-complete reference; adapt it if something does not compile, but keep the **behaviour and the warning texts** identical.

```kotlin
class ProfileBasedParser(
    private val profile: ParserProfile,
    private val postProcess: (List<ParsedTransaction>) -> List<ParsedTransaction> = { it },
) : BankStatementParser {

    override val bank: Bank get() = profile.bank

    override fun parse(ocr: OcrResult, today: LocalDate): ParseResult {
        val bands = LayoutAnalyzer.buildBands(ocr, profile.bandTolerance, profile.segmentGapFactor)
        val amountZoneLeft = profile.minAmountLeftFraction * ocr.imageWidth

        var currentDate: LocalDate? = null
        var skippedNoVendor = 0
        var skippedForeign = 0
        var skippedZero = 0
        val results = mutableListOf<ParsedTransaction>()

        for (band in bands) {
            val last = band.segments.last()
            val inAmountZone = last.box.left >= amountZoneLeft
            val amount = if (inAmountZone) AmountParser.parse(last.text) else null

            if (inAmountZone && amount == null &&
                FOREIGN_AMOUNT.matches(last.text.filter { !it.isWhitespace() })
            ) {
                skippedForeign++
                continue
            }
            if (amount == null) {
                if (profile.dateSource == DateSource.SECTION_HEADER) {
                    DateResolver.parseExact(band.text, today)?.let { currentDate = it }
                }
                continue
            }

            val vendorRaw = band.segments.dropLast(1).joinToString(" ") { it.text }
            if (profile.dateSource == DateSource.SECTION_HEADER) {
                val headerDate = DateResolver.parseExact(vendorRaw, today)
                if (headerDate != null) {      // header line that also shows a daily total
                    currentDate = headerDate
                    continue
                }
            }

            val vendor = VendorCleaner.clean(vendorRaw, profile.vendorNoisePrefixes)
            if (vendor.isBlank()) { skippedNoVendor++; continue }
            if (profile.ignoreVendorRegexes.any { it.containsMatchIn(vendor) }) continue
            if (amount.minor == 0L) { skippedZero++; continue }

            val date: LocalDate
            val uncertain: Boolean
            when (profile.dateSource) {
                DateSource.SECTION_HEADER -> {
                    date = currentDate ?: today
                    uncertain = currentDate == null
                }
                DateSource.PER_ROW -> TODO("PER_ROW dates are implemented in T08")
            }

            results += ParsedTransaction(
                date = date,
                vendor = vendor,
                amountMinor = amount.minor,
                kind = kindOf(amount.sign),
                rawText = band.text,
                dateUncertain = uncertain,
            )
        }

        val finalResults = postProcess(results)
        val warnings = buildList {
            if (finalResults.isEmpty()) add("No transactions found")
            if (finalResults.any { it.dateUncertain }) add("Dates above the first date header are unknown; today's date was used")
            if (skippedNoVendor > 0) add("Skipped $skippedNoVendor amount(s) with no vendor name")
            if (skippedForeign > 0) add("Skipped $skippedForeign non-GBP amount(s)")
            if (skippedZero > 0) add("Skipped $skippedZero zero amount(s)")
        }
        return ParseResult(finalResults, warnings)
    }

    private fun kindOf(sign: AmountSign): TransactionKind = when (profile.incomeRule) {
        IncomeRule.PLUS_MARKS_INCOME ->
            if (sign == AmountSign.PLUS) TransactionKind.INCOME else TransactionKind.EXPENSE
        IncomeRule.UNSIGNED_IS_INCOME ->
            if (sign == AmountSign.MINUS) TransactionKind.EXPENSE else TransactionKind.INCOME
    }

    private companion object {
        val FOREIGN_AMOUNT =
            Regex("^[+\\-\\u2212\\u2013\\u2014]?[€$¥][+\\-\\u2212\\u2013\\u2014]?\\d[\\d,]*(\\.\\d+)?$")
    }
}
```

## `ParserRegistry.kt`

```kotlin
object ParserRegistry {
    fun forBank(bank: Bank): BankStatementParser = when (bank) {
        Bank.GENERIC -> GenericParser
        // Replaced one by one in T12–T15:
        Bank.REVOLUT, Bank.MONZO, Bank.BARCLAYS, Bank.CHASE -> GenericParser
    }
}
```

## Tests (write first) — `ProfileBasedParserTest`

Use `today = LocalDate.of(2026, 10, 24)` and the `ocr { }` builder (T06). Default profile = `GENERIC_PROFILE` unless stated. "Amounts" use `right("…")`; image width 1080 ⇒ amount zone starts at x = 594.

1. **Basic grouping** (the same data as the T09 fixture):
   ```kotlin
   ocr {
     row(300) { word("Today", 40) }
     row(400) { word("Tesco", 160); right("-£12.50") }
     row(457) { word("Groceries", 160) }
     row(580) { word("Yesterday", 40) }
     row(680) { word("Salary", 160); right("+£2,500.00") }
     row(780) { word("Costa", 160); right("-£3.40") }
   }
   ```
   → 3 transactions, in this order: `2026-10-24 Tesco 1250 EXPENSE`, `2026-10-23 Salary 250000 INCOME`, `2026-10-23 Costa 340 EXPENSE`; `warnings` is empty.
2. **`UNSIGNED_IS_INCOME`**: profile `GENERIC_PROFILE.copy(incomeRule = UNSIGNED_IS_INCOME)`: `-£12.50` → EXPENSE, `£5.00` → INCOME, `+£100.00` → INCOME.
3. **`PLUS_MARKS_INCOME`**: `£5.00` (no sign) → EXPENSE.
4. **No header above the first row**: rows without any header → `date == today`, `dateUncertain == true`, warning `"Dates above the first date header are unknown; today's date was used"`. A header after them, then a row → that row has `dateUncertain == false`.
5. **Foreign currency**: row `Cafe` + `right("€12.00")` → no transactions; warnings contain `"Skipped 1 non-GBP amount(s)"` and `"No transactions found"`.
6. **Balance row ignored**: `Balance` + `£1,234.56` → no transaction and **no** "no vendor" warning.
7. **Zero amount**: `Refund` + `£0.00` → skipped; warning `"Skipped 1 zero amount(s)"`.
8. **Header with daily total**: row `Today` at x=40 plus `right("£45.00")`, then `Tesco -£3.00` → one transaction (Tesco, today, certain); no transaction for the header line.
9. **Amount in the left half is not an anchor**: `word("Save", 100)`, `word("£5.00", 300)` in one row → no transaction.
10. **Vendor prefix**: `GENERIC_PROFILE.copy(vendorNoisePrefixes = listOf("Card payment to"))`, vendor text `words("Card payment to Tesco", 160)` → vendor `Tesco`.
11. **Multi-word vendor**: `words("Pret A Manger", 160)` → `Pret A Manger`.
12. **No vendor**: row with only `right("-£5.00")` → skipped; warning `"Skipped 1 amount(s) with no vendor name"`.
13. **Custom ignore regex**: profile with `ignoreVendorRegexes = DEFAULT_IGNORE_VENDOR_REGEXES + Regex("^To .* pot$")`; row `To Holiday pot -£50.00` → ignored.
14. **Unreadable amount**: `-£12.5` (one decimal) → not a transaction, no crash.
15. **Empty OCR** (`ocr { }`) → empty list, warning `"No transactions found"`.
16. **`postProcess` hook**: `ProfileBasedParser(GENERIC_PROFILE) { list -> list.filter { it.vendor != "X" } }` removes vendor `X`.
17. **Multiple rows under one header** keep the same date and top-to-bottom order.

`ParserRegistryTest`: for every `Bank.values()` the registry returns a non-null parser (`forBank(it)` does not throw).

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*ProfileBasedParserTest" --tests "*ParserRegistryTest"
```

and the earlier tests still pass (`./gradlew :app:testDebugUnitTest`).

## Do NOT

- Add PER_ROW, pending, avatar logic (T08).
- Read the clock, log anything, or add Android imports.
- Change the warning texts (later tests and the UI compare against them).

## Commit message

`feat(importer): add parser profile, engine, generic parser and registry`
