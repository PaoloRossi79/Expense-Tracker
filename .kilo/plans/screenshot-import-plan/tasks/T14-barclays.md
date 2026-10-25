# T14 — Barclays parser

**Depends on:** T12 · **Size:** M · **Who:** You (fixtures) + Kilo (code)

## Goal

`BarclaysParser` returns correct transactions for your anonymised Barclays fixtures.

## Read first

`tasks/_BANK-PROCEDURE.md` (**follow it**), `docs/screenshot-import/worksheets/barclays.md`, `RevolutParser.kt` (as the pattern), `ParserRegistry.kt`.

## Your part (before Kilo starts)

Same as T12 (`02-PREREQUISITES-MANUAL-STEPS.md` §3). Fixtures go to `app/src/test/resources/fixtures/barclays/` as `barclays-basic`, `barclays-midday`, `barclays-income-pending`, `barclays-dark`; expected files start with `# bank: BARCLAYS`.

## Watch-outs to check on **your** screenshots (verify, do not assume)

- Some banks print the date **in each row** instead of as group headers; if so choose `dateSource = PER_ROW` and set `subtitleLookahead` to cover the line where the date sits (worksheet Q1/Q2).
- Some apps show a **running balance** on each row or in a separate column. If a balance is read as an amount, add a vendor-side ignore regex for the label that precedes it (worksheet Q11); if the balance appears in the same band as the real amount, note it in worksheet section C and ask for help: it needs a `postProcess` hook.
- Rows can carry a wording prefix (for example for card payments or direct debits). Put those in `vendorNoisePrefixes` if you want them removed (worksheet Q7).
- Pending items and the way they are labelled: worksheet Q8.
- Debits and credits may be shown differently from the apps you checked first; fill worksheet Q3/Q4 from this bank's screenshots only.

## Kilo's part

`_BANK-PROCEDURE.md` steps 1–8 with `BarclaysParser.kt` (`BARCLAYS_PROFILE`, `BarclaysParser`), registry branch `Bank.BARCLAYS -> BarclaysParser`, fixtures `fixtures/barclays/`, and `BarclaysParserTest` with 2–3 synthetic regression tests.

## Done when

```
./gradlew :app:testDebugUnitTest
```

is green; only the parser, registry, tests, fixtures and worksheet changed.

## Do NOT

- Change the engine or other banks' parsers.
- Commit private data.

## Commit message

`feat(importer): add Barclays parser with golden fixtures`
