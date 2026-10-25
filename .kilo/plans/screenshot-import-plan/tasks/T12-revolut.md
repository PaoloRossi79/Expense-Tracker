# T12 — Revolut parser

**Depends on:** T08, T09, T11 · **Size:** M · **Who:** You (fixtures) + Kilo (code)

This is the **first bank**: do the bank you use most first, whichever it is, and use this file as the model for the others (T13–T15 are the same procedure). The task is written for Revolut; if your first bank is another one, swap the names and use that bank's file from T13–T15.

## Goal

`RevolutParser` returns correct transactions for your anonymised Revolut fixtures.

## Read first

`tasks/_BANK-PROCEDURE.md` (**follow it**), `docs/screenshot-import/worksheets/revolut.md`, `ProfileBasedParser`, `ParserRegistry`.

## Your part (before Kilo starts, ~30–45 min)

1. Capture screenshots, dump OCR, anonymise, write expected files, fill the worksheet: `02-PREREQUISITES-MANUAL-STEPS.md` §3 and `tasks/_BANK-WORKSHEET.md`.
2. Put the finished fixtures into `app/src/test/resources/fixtures/revolut/` named `revolut-basic`, `revolut-midday`, `revolut-income-pending`, `revolut-dark` (`.ocr.json` + `.expected.txt`). Start every expected file with `# bank: REVOLUT`.

## Watch-outs to check on **your** screenshots (do not assume; verify)

- Revolut accounts are often multi-currency: a row may show a foreign amount (€, $) next to or under the £ amount. Non-£ amounts are skipped by design; make sure the £ amount is still read.
- Rows such as currency exchanges between pockets ("Exchanged to …") and top-ups may appear in the list. Decide, per worksheet Q9, whether they are real spending/income for the family tracker; if not, add an `ignoreVendorRegexes` entry for them.
- Subtitle lines under the vendor (category, time, "Pending", …) must not become vendors.
- Check how each date is printed (worksheet Q1/Q2) before choosing `dateSource`.

## Kilo's part

Follow `_BANK-PROCEDURE.md` steps 1–8 with:

- file `…/importer/parsing/banks/RevolutParser.kt` (`REVOLUT_PROFILE`, `RevolutParser`),
- registry branch `Bank.REVOLUT -> RevolutParser`,
- fixtures folder `fixtures/revolut/`.

## Tests

- Golden tests: automatic for every fixture.
- `ParserRegistryTest`: `forBank(Bank.REVOLUT).bank == Bank.REVOLUT`.
- 2–3 synthetic regression tests in `RevolutParserTest` for the quirks you listed in the worksheet (e.g. foreign-currency row, ignored exchange row, money-in row).

## Done when

```
./gradlew :app:testDebugUnitTest
```

is green and `git status` shows only: the parser file, `ParserRegistry.kt`, the new tests, the fixtures and the worksheet.

## Do NOT

- Commit anything from `fixtures-private/` or any file that fails `FixtureSafetyTest`.
- Edit the engine or the other parsers.

## Commit message

`feat(importer): add Revolut parser with golden fixtures`
