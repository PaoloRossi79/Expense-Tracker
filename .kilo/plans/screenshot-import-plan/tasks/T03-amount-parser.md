# T03 — `AmountParser`

**Depends on:** T02 · **Size:** S

## Goal

Turn the text of an amount as shown by a bank app (`-£12.50`, `+£1,234.56`, `- £ 3.00`, …) into pence plus the sign that was displayed. Returns `null` for anything that is not a clear GBP-style amount.

## Create

- `<SRC>/<PKG>/importer/parsing/AmountParser.kt`
- `app/src/test/…/importer/parsing/AmountParserTest.kt`

## Contract

```kotlin
enum class AmountSign { MINUS, PLUS, NONE }
data class ParsedAmount(val minor: Long, val sign: AmountSign)

object AmountParser {
    fun parse(text: String): ParsedAmount?
}
```

Rules:

1. Unicode minus/dashes (`−` U+2212, `–` U+2013, `—` U+2014) count as `-`.
2. All whitespace is ignored (`- £ 12.50` = `-£12.50`).
3. Optional sign, optional currency symbol, optional sign (but not a sign on both sides): `-£1.00`, `£-1.00`, `+£1.00`, `£1.00`, `1.00`.
4. Currency symbol is `£`, or the capital letter `E` (a very common OCR misreading of `£`). Any other symbol (`€`, `$`, `¥`…) → `null`.
5. Exactly **two decimals** are required. Thousands separators are commas in groups of three: `1,234.56`.
6. The whole text must match; `Balance £12.50` → `null`.
7. Result: `minor` = whole×100 + pence (always ≥ 0), `sign` = the sign that appeared.

## Reference implementation (you may use it as is)

```kotlin
object AmountParser {
    private val PATTERN =
        Regex("""^([+-])?([£E])?([+-])?(\d{1,3}(?:,\d{3})+|\d+)\.(\d{2})$""")

    fun parse(text: String): ParsedAmount? {
        val cleaned = text
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .filter { !it.isWhitespace() }
        val m = PATTERN.matchEntire(cleaned) ?: return null
        val before = m.groupValues[1]
        val after = m.groupValues[3]
        if (before.isNotEmpty() && after.isNotEmpty()) return null
        val sign = when (before.ifEmpty { after }) {
            "-" -> AmountSign.MINUS
            "+" -> AmountSign.PLUS
            else -> AmountSign.NONE
        }
        val whole = m.groupValues[4].replace(",", "").toLongOrNull() ?: return null
        val pence = m.groupValues[5].toLong()
        return ParsedAmount(whole * 100 + pence, sign)
    }
}
```

## Tests (write first) — `AmountParserTest`

| Input | Expected |
|---|---|
| `£12.50` | `1250, NONE` |
| `-£12.50` | `1250, MINUS` |
| `−£12.50` (U+2212) | `1250, MINUS` |
| `– £ 12.50` (en dash, spaces) | `1250, MINUS` |
| `+£1,234.56` | `123456, PLUS` |
| `£-3.00` | `300, MINUS` |
| `-E7.99` | `799, MINUS` |
| `12.50` | `1250, NONE` |
| `£0.00` | `0, NONE` |
| `£0.99` | `99, NONE` |
| `£12` | `null` |
| `£12.5` | `null` |
| `£12.500` | `null` |
| `€12.50` | `null` |
| `$12.50` | `null` |
| `£1,23.45` | `null` |
| `2026` | `null` |
| `23 Oct` | `null` |
| `Balance £12.50` | `null` |
| `+-£1.00` | `null` |
| `` (empty) | `null` |
| `£99999999999999999999.00` | `null` (overflow) |

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*AmountParserTest"
```

## Do NOT

- Use `Double` or `Float` anywhere (money is `Long` pence).
- Accept amounts without two decimals "to be more helpful"; the engine relies on this strictness to ignore dates, times and other numbers.

## Commit message

`feat(importer): add AmountParser`
