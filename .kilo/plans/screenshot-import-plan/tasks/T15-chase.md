# T15 — Chase parser

**Depends on:** T12 · **Size:** M · **Who:** You (fixtures) + Kilo (code)

## Goal

`ChaseParser` returns correct transactions for your anonymised Chase fixtures.

## Read first

`tasks/_BANK-PROCEDURE.md` (**follow it**), `docs/screenshot-import/worksheets/chase.md`, `RevolutParser.kt` (as the pattern), `ParserRegistry.kt`.

## Your part (before Kilo starts)

Same as T12 (`02-PREREQUISITES-MANUAL-STEPS.md` §3). Fixtures go to `app/src/test/resources/fixtures/chase/` as `chase-basic`, `chase-midday`, `chase-income-pending`, `chase-dark`; expected files start with `# bank: CHASE`.

## Watch-outs to check on **your** screenshots (verify, do not assume)

- The feed can include non-spending items: round-ups or savings movements, cashback credits, interest. Decide per worksheet Q9 what to ignore, and what should count as income.
- Date presentation (group headers vs per row) and its exact text: worksheet Q1/Q2.
- Check whether amounts are signed on both money in and money out, and write `incomeRule` from your own screenshots (worksheet Q3/Q4).
- Avatars or merchant logos on the left (worksheet Q6).
- Pending/settled labels (worksheet Q8).

## Kilo's part

`_BANK-PROCEDURE.md` steps 1–8 with `ChaseParser.kt` (`CHASE_PROFILE`, `ChaseParser`), registry branch `Bank.CHASE -> ChaseParser`, fixtures `fixtures/chase/`, and `ChaseParserTest` with 2–3 synthetic regression tests.

## Done when

```
./gradlew :app:testDebugUnitTest
```

is green; only the parser, registry, tests, fixtures and worksheet changed.

## Do NOT

- Change the engine or other banks' parsers.
- Commit private data.

## Commit message

`feat(importer): add Chase parser with golden fixtures`
