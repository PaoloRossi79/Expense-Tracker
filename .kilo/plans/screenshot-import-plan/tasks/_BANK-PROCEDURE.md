# Bank parser procedure (shared by T12–T15 and T16)

Attach this file together with the bank's task file, `00-ARCHITECTURE.md`, `docs/APP_CONTEXT.md` and the bank's worksheet (`docs/screenshot-import/worksheets/<bank>.md`).

## Preconditions (you did these, see `02-PREREQUISITES-MANUAL-STEPS.md` §3)

- [ ] 2–4 **anonymised** fixtures `<bank>-*.ocr.json` and matching `<bank>-*.expected.txt` exist in a temporary place (e.g. `fixtures-private/ready/<bank>/`) — they will be copied into `app/src/test/resources/fixtures/<bank>/`.
- [ ] The worksheet for the bank is filled in.
- [ ] `./gradlew testDebugUnitTest` is green before you start.

## Steps

1. **Create** `<SRC>/<PKG>/importer/parsing/banks/<Bank>Parser.kt`:
   ```kotlin
   val <BANK>_PROFILE = ParserProfile(
       bank = Bank.<BANK>,
       dateSource = …,                 // worksheet Q1
       incomeRule = …,                 // worksheet Q3/Q4
       minAmountLeftFraction = …,      // worksheet Q5
       avatarColumnFraction = …,       // worksheet Q6
       vendorNoisePrefixes = listOf(…),// worksheet Q7
       pendingRegex = …,               // worksheet Q8
       ignoreVendorRegexes = DEFAULT_IGNORE_VENDOR_REGEXES + listOf(…), // worksheet Q9, Q11
   )
   object <Bank>Parser : BankStatementParser by ProfileBasedParser(<BANK>_PROFILE)
   ```
   Only set fields that differ from the defaults. If the bank needs code beyond the profile, use the `postProcess` lambda (see T16 for examples).
2. **Register** it in `ParserRegistry.forBank` (replace the `GenericParser` branch of that bank; keep the `when` exhaustive).
3. **Copy** the fixtures into `app/src/test/resources/fixtures/<bank>/`. The expected files must start with `# bank: <BANK>` (the enum name) and `# today: …`.
4. **Run** `./gradlew :app:testDebugUnitTest --tests "*GoldenFixtureTest" --tests "*FixtureSafetyTest"`.
5. **Tune** using the failure message. It lists the expected rows, the actual rows, the warnings and a **band dump** (what the parser saw). Use the table below. Change **one thing at a time**, re-run.
6. **Add a registry test**: `ParserRegistry.forBank(Bank.<BANK>).bank == Bank.<BANK>`.
7. **Add 2–3 regression tests** with the synthetic `ocr { }` builder for the bank's quirks (for example: a pending row, an ignored pot transfer, a money-in row). They document the intent independently of real data.
8. **Update the worksheet** section C with anything you learned. Commit worksheet + code + fixtures together.

## Tuning table

| What you see in the failure output | Likely cause | Change |
|---|---|---|
| Rows missing; band dump shows amounts at a left% **below** `minAmountLeftFraction × 100` | amount zone starts too far right | lower `minAmountLeftFraction` to (smallest amount left% − 5) / 100 |
| Vendor starts with an extra short word (`T Tesco`, `M Boots`) | avatar letters read as text | set `avatarColumnFraction` ≈ (avatar right edge % + 2) / 100 |
| Rows exist but every date is today and `DATE_UNCERTAIN` | wrong `dateSource`, or lookahead too small | switch to `PER_ROW`, or raise `subtitleLookahead` to 3 |
| Rows under a header get the **previous** day's date | header text not recognised | check the exact header text in the band dump; if its format is not supported, add a failing test to `DateResolverTest`, extend `DateResolver` (separate commit) |
| Income appears as expense (or the reverse) | wrong `incomeRule` | swap between `PLUS_MARKS_INCOME` and `UNSIGNED_IS_INCOME` |
| Extra rows like `Balance`, `Money in`, `Pot transfer`, `Round-up` | not real spending | add a regex to `ignoreVendorRegexes` |
| Vendor has a prefix such as `Card payment to` | bank wording | add to `vendorNoisePrefixes` |
| Vendor and subtitle merged into one band | rows too close | lower `bandTolerance` to 0.45 |
| Vendor and its amount ended up in **different** bands | amount sits slightly lower/higher | raise `bandTolerance` to 0.8 |
| Vendor and amount in the **same segment** (`Tesco -£12.50` as one piece) | gap too small for the split | lower `segmentGapFactor` to 1.0 |
| An amount is visible in the band dump but misread (`£12.5O`, `£l2.50`) | genuine OCR error | not a profile problem; correct the **expected** file only if the fixture is meant to show real behaviour, otherwise pick a cleaner screenshot; the review screen is the safety net for these |
| A vendor spans two lines | second line becomes its own band | accept it (the user can edit) or use a `postProcess` hook |

## Done criteria

- All fixtures of the bank pass, the whole suite passes (`./gradlew :app:testDebugUnitTest`).
- No change to `AmountParser`, `LayoutAnalyzer` or the engine unless a failing test for it was added first (and then in its own commit).
- `FixtureSafetyTest` passes.

## If you are stuck after ~3 sessions

Stop. The usual causes are wrong worksheet answers or a typo in the expected file. Bring **only** the anonymised `.ocr.json`, the `.expected.txt`, the profile and the failing output to a stronger model (or to Claude), never the original screenshot.
