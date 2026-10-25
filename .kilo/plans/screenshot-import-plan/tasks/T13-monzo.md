# T13 — Monzo parser

**Depends on:** T12 · **Size:** M · **Who:** You (fixtures) + Kilo (code)

## Goal

`MonzoParser` returns correct transactions for your anonymised Monzo fixtures.

## Read first

`tasks/_BANK-PROCEDURE.md` (**follow it**), `docs/screenshot-import/worksheets/monzo.md`, `RevolutParser.kt` (as the pattern), `ParserRegistry.kt`.

## Your part (before Kilo starts)

Same as T12: capture → dump → anonymise → expected files → worksheet (`02-PREREQUISITES-MANUAL-STEPS.md` §3). Fixtures go to `app/src/test/resources/fixtures/monzo/` as `monzo-basic`, `monzo-midday`, `monzo-income-pending`, `monzo-dark`; expected files start with `# bank: MONZO`.

## Watch-outs to check on **your** screenshots (verify, do not assume)

- A feed can mix real spending with internal movements (for example money moved to or from a savings pot, if your account has pots). Decide per worksheet Q9 whether those rows are ignored, and add the matching `ignoreVendorRegexes` entry.
- The feed can show a per-day or per-period summary next to the date header (daily total). The engine treats "date header + amount" as a header and skips it; confirm this on your fixture.
- Rows may have an emoji or coloured avatar on the left. If the band dump shows stray 1–2 character words, set `avatarColumnFraction`.
- Pending items (card authorisations) may be marked differently from settled ones; set `pendingRegex` from worksheet Q8.
- Check the sign convention on money in vs money out (worksheet Q3/Q4) before choosing `incomeRule`.

## Kilo's part

`_BANK-PROCEDURE.md` steps 1–8 with `MonzoParser.kt` (`MONZO_PROFILE`, `MonzoParser`), registry branch `Bank.MONZO -> MonzoParser`, fixtures `fixtures/monzo/`, and `MonzoParserTest` with 2–3 synthetic regression tests for the worksheet quirks.

## Done when

```
./gradlew :app:testDebugUnitTest
```

is green; only the parser, registry, tests, fixtures and worksheet changed.

## Do NOT

- Change the engine, the Revolut parser or the shared test helpers.
- Commit private data.

## Commit message

`feat(importer): add Monzo parser with golden fixtures`
