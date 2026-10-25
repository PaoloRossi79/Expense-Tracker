# Bank worksheet (copy per bank → `docs/screenshot-import/worksheets/<bank>.md`)

Fill this in by looking at **your own screenshots** of the bank app. Layouts change between app versions, so do not guess; if unsure, leave the default and let the fixture tests tell you.

Bank: ____________   App version / date of screenshots: ____________

## A. How the app shows things

| # | Question | Your answer | Maps to profile field |
|---|---|---|---|
| 1 | Are rows grouped under **date headers** (e.g. "Today", "Yesterday", "Thursday 22 October")? Or does **each row** carry its own date in a small line below the vendor? | headers / per row | `dateSource` = `SECTION_HEADER` / `PER_ROW` |
| 2 | Copy 3–5 exact date strings as shown (header or row): | e.g. `Today`, `Yesterday`, `22 Oct`, `Thu 22 Oct` | check `DateResolver` handles them (T04); if not, note in section C |
| 3 | How is **spending** shown? (`-£12.50`, `£12.50`, …) | | `incomeRule` |
| 4 | How is **money in** shown? (`+£100.00`, `£100.00`, …) | | `incomeRule` = `PLUS_MARKS_INCOME` if money in has `+`; `UNSIGNED_IS_INCOME` if spending has `-` and money in has no sign |
| 5 | Where is the amount? Roughly what fraction of the width does the **left edge** of the widest amount reach? (e.g. 0.70) | | `minAmountLeftFraction` (use a little lower than your measurement, e.g. 0.55) |
| 6 | Is there a round **avatar/icon** with 1–2 letters on the left of each row? | yes / no | `avatarColumnFraction` (e.g. 0.15) or 0.0 |
| 7 | Does a row show text before the vendor like "Card payment to …"? Copy examples: | | `vendorNoisePrefixes` |
| 8 | How are **pending** items marked? Copy the exact word: | e.g. `Pending` | `pendingRegex` |
| 9 | Which rows are **not real spending** and must be ignored? (balance widgets, "Money in/out" totals, pot/savings transfers, round-ups, cashback…) Copy their text: | | `ignoreVendorRegexes` |
| 10 | Are **foreign-currency** amounts shown (€, $) under or next to the £ amount? | yes / no | nothing (non-£ amounts are skipped by design) |
| 11 | Is a **running balance** shown on each row? | yes / no | add `^balance` style regex if it appears as its own text |
| 12 | Do vendors ever span **two lines**? | yes / no | note in C |

## B. Fixtures ready

| Fixture file name | Variant (1–4 from `02-…` §3.1) | Expected file written? |
|---|---|---|
| `<bank>-basic.ocr.json` | 1 | ☐ |
| `<bank>-midday.ocr.json` | 2 | ☐ |
| `<bank>-income-pending.ocr.json` | 3 | ☐ |
| `<bank>-dark.ocr.json` | 4 | ☐ |

## C. Quirks / notes for the implementer

(free text: anything odd, e.g. "pot transfers show as `To Holiday pot`", "daily total shown on header line", "two-line vendors")
