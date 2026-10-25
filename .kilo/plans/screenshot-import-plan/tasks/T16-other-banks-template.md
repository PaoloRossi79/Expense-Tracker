# T16 — Template for "other banks" and `postProcess` examples

**Depends on:** T12 · **Size:** S

## Goal

Make adding the 5th, 6th… bank a 30-minute, mostly mechanical job, and give small models concrete examples of the `postProcess` hook for quirks that profiles cannot express.

## Read first

`ParserProfile.kt`, `ProfileBasedParser.kt`, `ParserRegistry.kt`, `RevolutParser.kt`, `_BANK-PROCEDURE.md`.

## Create

1. `docs/screenshot-import/ADDING_A_BANK.md` — a checklist (≤ 40 lines) with these steps:
   1. Add the enum entry to `Bank` (`T02` file), e.g. `NATIONWIDE("Nationwide")`.
   2. Run the build: the exhaustive `when` in `ParserRegistry` will fail to compile; add the branch.
   3. Capture/dump/anonymise fixtures and fill a worksheet copy (`02-…` §3, `_BANK-WORKSHEET.md`).
   4. Create `…/parsing/banks/<Name>Parser.kt` with a profile (`_BANK-PROCEDURE.md`).
   5. Run the golden tests, tune, add regression tests.
   6. The bank appears in the UI automatically (the bank dropdown uses `Bank.values()`).
2. `…/importer/parsing/PostProcessors.kt` with reusable, tested helpers:

```kotlin
object PostProcessors {
    /** Marks rows whose vendor matches [regex] as INCOME (e.g. refunds that carry no '+' sign). */
    fun markAsIncome(regex: Regex): (List<ParsedTransaction>) -> List<ParsedTransaction> = { list ->
        list.map { if (regex.containsMatchIn(it.vendor)) it.copy(kind = TransactionKind.INCOME) else it }
    }

    /** Removes rows whose vendor matches [regex]. */
    fun dropVendors(regex: Regex): (List<ParsedTransaction>) -> List<ParsedTransaction> = { list ->
        list.filterNot { regex.containsMatchIn(it.vendor) }
    }

    /** Removes [regex] matches from the vendor text (e.g. trailing store numbers or location codes). */
    fun stripFromVendor(regex: Regex): (List<ParsedTransaction>) -> List<ParsedTransaction> = { list ->
        list.map { it.copy(vendor = it.vendor.replace(regex, "").trim().replace(Regex("\\s+"), " ")) }
            .filter { it.vendor.isNotBlank() }
    }

    /** Applies several post-processors in order. */
    fun chain(vararg steps: (List<ParsedTransaction>) -> List<ParsedTransaction>):
        (List<ParsedTransaction>) -> List<ParsedTransaction> = { list -> steps.fold(list) { acc, step -> step(acc) } }
}
```

3. `PostProcessorsTest` (write first):
   - `markAsIncome(Regex("(?i)refund"))`: vendor `Refund Boots` becomes INCOME, `Tesco` unchanged.
   - `dropVendors(Regex("^Pot "))`: removes `Pot Holiday`, keeps `Potters`.
   - `stripFromVendor(Regex("\\s+\\d{3,}$"))`: `Tesco Stores 3456` → `Tesco Stores`; a vendor that becomes empty is removed.
   - `chain(...)`: applies in the given order (use two steps whose order matters).
   - Usage example in a test: `ProfileBasedParser(GENERIC_PROFILE, PostProcessors.dropVendors(Regex("^X$")))`.

## Reference code

**Verified (JVM tests pass):** `reference/main/parsing/PostProcessors.kt.txt`. Try it yourself first; after 2 failed attempts copy the reference (rename `.kt.txt` to `.kt`, replace package `com.example.expenses` with `<PKG>`).

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*PostProcessorsTest"
```

## Do NOT

- Change existing parsers to use the helpers unless a fixture needs it.
- Add helpers beyond the four above.

## Commit message

`feat(importer): add PostProcessors and a guide for adding banks`
