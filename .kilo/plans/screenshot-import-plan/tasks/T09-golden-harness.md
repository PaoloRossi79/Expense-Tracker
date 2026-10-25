# T09 — Golden-fixture harness and fixture safety test

**Depends on:** T07 · **Size:** M

## Goal

A test that automatically runs **every** OCR fixture through its bank's parser and compares the result with the hand-written expected file, with a diagnostic message that shows exactly what the parser saw. A second test refuses fixtures that contain private-looking data.

All later bank tasks only add fixture files; no new test code.

## Read first

`00-ARCHITECTURE.md` §6 (file formats) and §8; `ParserRegistry`; the test builder.

## Create

| File | Purpose |
|---|---|
| `app/src/test/…/importer/testutil/Fixtures.kt` | loads fixtures and expected files |
| `app/src/test/…/importer/testutil/BandDump.kt` | human-readable dump of bands/segments |
| `app/src/test/…/importer/GoldenFixtureTest.kt` | parameterised golden test |
| `app/src/test/…/importer/FixtureSafetyTest.kt` | privacy guard |
| `app/src/test/resources/fixtures/generic/sample-synthetic.ocr.json` | synthetic fixture (content below) |
| `app/src/test/resources/fixtures/generic/sample-synthetic.expected.txt` | its expected file (content below) |

## `Fixtures.kt` (reference)

```kotlin
data class ExpectedRow(
    val date: LocalDate, val vendor: String, val amountMinor: Long,
    val kind: TransactionKind, val pending: Boolean, val dateUncertain: Boolean,
)
data class ExpectedFile(val bank: Bank, val today: LocalDate, val rows: List<ExpectedRow>)

object Fixtures {
    private val gson = Gson()
    private data class WordJson(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int, val angleDeg: Float?)
    private data class OcrJson(val imageWidth: Int, val imageHeight: Int, val words: List<WordJson>)

    private fun root(): File {
        val url = requireNotNull(Fixtures::class.java.classLoader?.getResource("fixtures")) { "fixtures folder missing" }
        return File(url.toURI())
    }

    /** e.g. "generic/sample-synthetic" */
    fun allNames(): List<String> = root().walkTopDown()
        .filter { it.isFile && it.name.endsWith(".ocr.json") }
        .map { it.relativeTo(root()).path.removeSuffix(".ocr.json").replace(File.separatorChar, '/') }
        .sorted().toList()

    fun loadOcr(name: String): OcrResult {
        val json = File(root(), "$name.ocr.json").readText()
        val parsed = gson.fromJson(json, OcrJson::class.java)
        return OcrResult(parsed.imageWidth, parsed.imageHeight,
            parsed.words.map { OcrWord(it.text, OcrBox(it.left, it.top, it.right, it.bottom), it.angleDeg ?: 0f) })
    }

    fun hasExpected(name: String) = File(root(), "$name.expected.txt").exists()

    fun loadExpected(name: String): ExpectedFile {
        // parse '# today: yyyy-MM-dd' and '# bank: NAME' headers; other '#' lines are comments;
        // each other non-blank line: date | vendor | amount | KIND [| FLAGS]
        // amount: BigDecimal(text).movePointRight(2).longValueExact()
        // FLAGS: comma separated, PENDING and/or DATE_UNCERTAIN
        // vendors are stored already normalised with normaliseVendor()
        TODO()   // PLACEHOLDER ONLY: the full working parseExpected is in reference/test/testutil/Fixtures.kt.txt
    }

    fun normaliseVendor(s: String) = s.trim().lowercase().replace(Regex("\\s+"), " ")
    fun wordTexts(name: String): List<String> = loadOcr(name).words.map { it.text }
}
```

Write a proper implementation of `loadExpected` (the `TODO()` above is a placeholder in this plan, not allowed in the committed file). Fail with a clear message if `today` or `bank` is missing.

## `BandDump.kt`

```kotlin
object BandDump {
    /** One line per band: index, y, then each segment as [left%|text]. */
    fun format(ocr: OcrResult): String
}
```

Example output line: `#03 y=400  [15%|Tesco]  [80%|-£12.50]` (left% = `box.left * 100 / imageWidth`, integer).

## `GoldenFixtureTest.kt`

```kotlin
@RunWith(Parameterized::class)
class GoldenFixtureTest(private val name: String) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun names(): List<String> = Fixtures.allNames()
    }

    @Test fun parserMatchesExpectedFile() {
        val ocr = Fixtures.loadOcr(name)
        val expected = Fixtures.loadExpected(name)
        val result = ParserRegistry.forBank(expected.bank).parse(ocr, expected.today)
        val actual = result.transactions.map {
            ExpectedRow(it.date, Fixtures.normaliseVendor(it.vendor), it.amountMinor, it.kind, it.pending, it.dateUncertain)
        }
        assertEquals(
            buildString {
                appendLine("Fixture: $name  bank=${expected.bank}  today=${expected.today}")
                appendLine("Warnings: ${result.warnings}")
                appendLine("--- bands seen by the parser ---")
                appendLine(BandDump.format(ocr))
            },
            expected.rows, actual,
        )
    }
}
```

(JUnit's `assertEquals(message, expected, actual)` prints both lists on failure, so the message plus the diff is everything needed to tune a profile.)

## `FixtureSafetyTest.kt`

Parameterised over the same names. For each fixture:

1. `Fixtures.hasExpected(name)` must be true (every OCR fixture needs an expected file).
2. Join all word texts with a single space and assert that **none** of these patterns matches (the failure message names the pattern but must **not** print the matching text):

| Pattern | Looks like |
|---|---|
| `\b\d{2}-\d{2}-\d{2}\b` | sort code |
| `\b\d{8}\b` | account number |
| `\b[A-Z]{2}\d{2}[A-Z0-9]{10,30}\b` | IBAN |
| `[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}` | email address |
| `\b(?:\+44\s?|0)7\d{3}\s?\d{6}\b` | UK mobile number |
| `\b0\d{3,4}\s?\d{6,7}\b` | UK landline |
| `\b(?:\d[ -]?){13,19}\b` | card number |

## Synthetic fixture (copy exactly)

`fixtures/generic/sample-synthetic.ocr.json`:

```json
{"imageWidth":1080,"imageHeight":2400,"words":[
{"text":"Today","left":40,"top":280,"right":160,"bottom":320,"angleDeg":0.0},
{"text":"Tesco","left":160,"top":380,"right":270,"bottom":420,"angleDeg":0.0},
{"text":"-£12.50","left":860,"top":380,"right":1020,"bottom":420,"angleDeg":0.0},
{"text":"Groceries","left":160,"top":440,"right":330,"bottom":475,"angleDeg":0.0},
{"text":"Yesterday","left":40,"top":560,"right":220,"bottom":600,"angleDeg":0.0},
{"text":"Salary","left":160,"top":660,"right":280,"bottom":700,"angleDeg":0.0},
{"text":"+£2,500.00","left":800,"top":660,"right":1020,"bottom":700,"angleDeg":0.0},
{"text":"Costa","left":160,"top":760,"right":260,"bottom":800,"angleDeg":0.0},
{"text":"-£3.40","left":880,"top":760,"right":1020,"bottom":800,"angleDeg":0.0}
]}
```

`fixtures/generic/sample-synthetic.expected.txt`:

```
# today: 2026-10-24
# bank: GENERIC
2026-10-24 | Tesco | 12.50 | EXPENSE
2026-10-23 | Salary | 2500.00 | INCOME
2026-10-23 | Costa | 3.40 | EXPENSE
```

## Tests for the helpers themselves

Add `FixturesTest`: `loadExpected("generic/sample-synthetic")` returns 3 rows, `today == 2026-10-24`, `bank == GENERIC`; an expected line with flags `| PENDING,DATE_UNCERTAIN` sets both flags (use a small temp resource or a private function that parses text, so the parsing function is testable without a file: split `loadExpected` into `parseExpected(text: String)` and the file reader).

## Reference code

**Verified (JVM tests pass):** `reference/test/testutil/Fixtures.kt.txt`, `reference/test/testutil/BandDump.kt.txt`, `reference/test/FixtureTests.kt.txt`. Try it yourself first; after 2 failed attempts copy the reference (rename `.kt.txt` to `.kt`, replace package `com.example.expenses` with `<PKG>`).

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*GoldenFixtureTest" --tests "*FixtureSafetyTest" --tests "*FixturesTest"
```

## Do NOT

- Commit any real fixture in this task.
- Print fixture word texts in assertion messages of `FixtureSafetyTest`.

## Commit message

`test(importer): golden fixture harness and fixture safety test`
