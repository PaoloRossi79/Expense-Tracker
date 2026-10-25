# T05 — `VendorCleaner`

**Depends on:** T02 · **Size:** S

## Goal

Clean the raw text of a vendor as read by OCR: collapse spaces, remove stray icons/arrows at the edges, remove bank-specific prefixes such as "Card payment to". (Pascal-Casing is done later by the app's existing normaliser, not here.)

## Create

- `<SRC>/<PKG>/importer/parsing/VendorCleaner.kt`
- `app/src/test/…/importer/parsing/VendorCleanerTest.kt`

## Contract

```kotlin
object VendorCleaner {
    fun clean(raw: String, noisePrefixes: List<String> = emptyList()): String
}
```

Steps, in this order:

1. Replace every run of whitespace by one space; trim.
2. Remove leading characters until the first letter or digit; remove trailing characters after the last letter or digit. (`•`, `›`, `>`, `|`, `-`, `.`, quotes… at the edges go away; `&`, `'`, `.` **inside** the text stay.)
3. If the text starts with one of `noisePrefixes` (case-insensitive) followed by a space, `:` or the end of the text, remove that prefix and the separator. Try the longest prefix first; remove at most one prefix.
4. Step 2 and step 1 again on the result (so `"Card payment to  - Tesco"` ends clean).
5. The case of the remaining text is **not** changed.

## Tests (write first) — `VendorCleanerTest`

| Input (prefixes) | Expected |
|---|---|
| `  Tesco   Stores  ` | `Tesco Stores` |
| `• Tesco` | `Tesco` |
| `Tesco ›` | `Tesco` |
| `"Pret A Manger"` | `Pret A Manger` |
| `M&S Simply Food` | `M&S Simply Food` |
| `McDonald's` | `McDonald's` |
| `Card payment to Tesco` (`["Card payment to"]`) | `Tesco` |
| `card payment to tesco` (`["Card payment to"]`) | `tesco` |
| `Card payment to: Tesco` (`["Card payment to"]`) | `Tesco` |
| `Payment to Mr Smith` (`["Payment", "Payment to"]`) | `Mr Smith` (longest prefix wins) |
| `Card payment to` (`["Card payment to"]`) | `` |
| `Card payments Ltd` (`["Card payment"]`) | `Card payments Ltd` (prefix must be followed by space, `:` or end of text) |
| `---` | `` |
| `` | `` |

## Reference code

**Verified (JVM tests pass):** `reference/main/parsing/VendorCleaner.kt.txt`. Try it yourself first; after 2 failed attempts copy the reference (rename `.kt.txt` to `.kt`, replace package `com.example.expenses` with `<PKG>`).

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*VendorCleanerTest"
```

## Do NOT

- Change letter case, strip digits inside names, or remove words other than the configured prefixes.

## Commit message

`feat(importer): add VendorCleaner`
