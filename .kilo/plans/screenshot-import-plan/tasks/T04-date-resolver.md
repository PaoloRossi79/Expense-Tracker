# T04 — `DateResolver`

**Depends on:** T02 · **Size:** M

## Goal

Recognise the date strings bank apps use for headers and per-row dates and convert them to `LocalDate`, resolving "Today", "Yesterday" and dates without a year.

## Create

- `<SRC>/<PKG>/importer/parsing/DateResolver.kt`
- `app/src/test/…/importer/parsing/DateResolverTest.kt`

## Contract

```kotlin
object DateResolver {
    /** The whole text must be a date. Used for header rows. */
    fun parseExact(text: String, today: LocalDate): LocalDate?

    /** Finds the first date anywhere inside the text (e.g. "22 Oct · Card payment"). Used for per-row dates. */
    fun findIn(text: String, today: LocalDate): LocalDate?
}
```

## Accepted formats

All case-insensitive. After normalising (lower-case; replace `,` `·` `•` with a space; strip one trailing `.`; collapse whitespace; trim):

1. `today`, `yesterday` (exact). In `findIn` they count as words anywhere in the text.
2. **Day–Month:** optional weekday, day, optional ordinal suffix (`st nd rd th`), month, optional 4-digit year.
   `22 oct`, `22 october`, `thu 22 oct`, `thursday 22 october 2026`, `22nd October`, `1 sept`
3. **Month–Day:** optional weekday, month, day, optional ordinal, optional year: `october 22`, `oct 22nd 2026`.
4. **Numeric (exact only, never in `findIn`):** `dd/MM`, `dd/MM/yy`, `dd/MM/yyyy`. Two-digit year → 2000 + yy. Only `/` is accepted as separator (dots would collide with amounts such as `1.05`).

Month tokens must be **exactly** one of: `january … december`, `jan feb mar apr may jun jul aug sep sept oct nov dec`.
Weekday tokens must be exactly one of: `monday … sunday`, `mon tue tues wed thu thur thurs fri sat sun`.
(Exact tokens matter: `22 Market` must **not** parse as 22 March.)

## Year inference

- Year given → use it.
- No year → `today.year`; if the result is after `today.plusDays(1)`, use the previous year.
- Invalid calendar dates (30 Feb) → `null`. (No retry in other years.)
- The weekday word is ignored for validation (OCR may garble it).

## Suggested structure

```kotlin
private val MONTHS = mapOf("january" to 1, /* … */ "jan" to 1, /* … */ "sept" to 9, /* … */)
private val WEEKDAYS = listOf("monday", /* … */ "thurs", "sun")
private val monthAlt = MONTHS.keys.sortedByDescending { it.length }.joinToString("|")
private val dayAlt = WEEKDAYS.sortedByDescending { it.length }.joinToString("|")
private const val ORD = "(?:st|nd|rd|th)?"
private val DM = "(?:(?:$dayAlt)\\s+)?(\\d{1,2})$ORD\\s+($monthAlt)(?:\\s+(\\d{4}))?"
private val MD = "(?:(?:$dayAlt)\\s+)?($monthAlt)\\s+(\\d{1,2})$ORD(?:\\s+(\\d{4}))?"
private val NUM = "(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}|\\d{2}))?"
// exact: Regex("^$DM$"), Regex("^$MD$"), Regex("^$NUM$")
// find : Regex("\\b$DM\\b"), Regex("\\b$MD\\b")      (use find(), not matchEntire)
```

Write one private `resolve(day, month, yearOrNull, today): LocalDate?` that applies the year rule and returns `null` on `DateTimeException`.

## Tests (write first) — `DateResolverTest`, `today = 2026-10-24` (a Saturday)

`parseExact`:

| Input | Expected |
|---|---|
| `Today` | 2026-10-24 |
| `YESTERDAY` | 2026-10-23 |
| `22 Oct` | 2026-10-22 |
| `Thu, 22 Oct` | 2026-10-22 |
| `Thursday 22 October` | 2026-10-22 |
| `22nd October 2025` | 2025-10-22 |
| `1 Sept` | 2026-09-01 |
| `October 22` | 2026-10-22 |
| `Oct 22nd, 2026` | 2026-10-22 |
| `28 Dec` | 2025-12-28 (would be in the future → previous year) |
| `25 Oct` | 2026-10-25 (tomorrow is allowed) |
| `26 Oct` | 2025-10-26 |
| `22/10/2026` | 2026-10-22 |
| `22/10/26` | 2026-10-22 |
| `22/10` | 2026-10-22 |
| `30 Feb` | null |
| `31/04/2026` | null |
| `22 Market` | null |
| `Tesco` | null |
| `22 Oct Tesco` | null (extra text; use `findIn`) |
| `1.05` | null |
| `` | null |

`findIn`:

| Input | Expected |
|---|---|
| `22 Oct · Card payment` | 2026-10-22 |
| `Card payment` | null |
| `Today at 14:32` | 2026-10-24 |
| `Paid yesterday` | 2026-10-23 |
| `Pending · 23 October` | 2026-10-23 |
| `Order 22/10/2026` | null (numeric not searched) |
| `Sat Nav Ltd` | null |

## Reference code

**Verified (JVM tests pass):** `reference/main/parsing/DateResolver.kt.txt`. Try it yourself first; after 2 failed attempts copy the reference (rename `.kt.txt` to `.kt`, replace package `com.example.expenses` with `<PKG>`).

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*DateResolverTest"
```

## Do NOT

- Read the system clock; `today` is always a parameter.
- Add locale-dependent parsing (`DateTimeFormatter` with `Locale`); the explicit tables above are deliberate.

## Commit message

`feat(importer): add DateResolver`
