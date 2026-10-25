# Screenshot Import — Implementation Plan

Feature for the family expense tracker (Kotlin, native Android): pick a screenshot of a bank-app transaction list → recognise the text **on the phone** → turn it into transactions → review → save.

No image is ever uploaded. OCR is Google ML Kit's **bundled** Latin text model (compiled into the APK, no model download), and all parsing is plain Kotlin.

## Decisions already taken

| Topic | Decision |
|---|---|
| Approach | **A**: on-device OCR + rule-based parsing |
| OCR | ML Kit `com.google.mlkit:text-recognition:16.0.1` (bundled model; confirmed on Google's ML Kit docs page, Oct 2026) |
| Banks | Revolut, Monzo, Barclays, Chase + a generic fallback. One parser per bank, the user picks the bank in the UI |
| Income | Parsers detect it, but it is **excluded by default** (switch on the import screen) |
| Safety net | Mandatory review screen before anything is saved + duplicate detection |
| Currency | GBP only. Non-£ amounts are skipped with a warning |
| Input | Gallery picker (v1), share sheet (T23) |
| Creator | Every imported record is stamped with the current user, like any other record |

## Files in this folder

| File | What it is |
|---|---|
| `README.md` | This overview and the task list |
| `00-ARCHITECTURE.md` | Design, package layout, exact data models, parsing algorithm. **Every task refers to it** |
| `01-KILO-USAGE-GUIDE.md` | How to run these tasks with Kilo + Ollama / DeepSeek, prompt template, rules for the small model |
| `02-PREREQUISITES-MANUAL-STEPS.md` | What **you** must do by hand (capture and anonymise screenshots, test on the phone) |
| `CHECKPOINT.md` | Status, decisions and remaining work (read first when resuming) |
| `reference/` | Verified Kotlin reference code and tests (`.kt.txt`) used by the tasks |
| `tasks/T00…T27` | One self-contained file per task, in dependency order |
| `tasks/_BANK-WORKSHEET.md` | Worksheet you fill in per bank before the bank task starts |

Copy the whole folder into your repo as `docs/screenshot-import/`.

## Milestones

| Milestone | After task | What you can do |
|---|---|---|
| M1 – Foundations | T09 | The parsing engine passes all tests on synthetic data |
| M2 – OCR works | T11 | Debug app produces OCR dumps from your real screenshots (used to build fixtures) |
| M3 – First bank | T12 | First real bank parsed correctly in tests (start with the bank you use most) |
| M4 – End to end | T22 | Pick screenshot → review → save works on the phone |
| M5 – Release ready | T27 | Privacy checks, UI tests and CI all green |

## Task list

Size: S ≈ 1 small file, M ≈ 2–4 files, L ≈ the largest unit (still one session). "You" means manual work is needed, see `02-PREREQUISITES-MANUAL-STEPS.md`.

| ID | Task | Depends on | Size | Who |
|---|---|---|---|---|
| **Phase 0 – Setup** | | | | |
| T00 | Discover the existing app, write `docs/APP_CONTEXT.md` | – | S | Kilo (read-only) |
| T01 | Add dependencies and `.gitignore` entries | T00 | S | Kilo |
| **Phase 1 – Parsing core (pure Kotlin, JVM tests)** | | | | |
| T02 | Core models (OCR + parsed transaction) | T01 | S | Kilo |
| T03 | `AmountParser` | T02 | S | Kilo |
| T04 | `DateResolver` | T02 | M | Kilo |
| T05 | `VendorCleaner` | T02 | S | Kilo |
| T06 | `LayoutAnalyzer` + synthetic OCR test builder | T02 | M | Kilo |
| T07 | `ParserProfile`, engine (section-header dates), `GenericParser`, registry | T03–T06 | L | Kilo |
| T08 | Engine extensions: per-row dates, pending flag, avatar filter | T07 | M | Kilo |
| T09 | Golden-fixture test harness + fixture safety test | T07 | M | Kilo |
| **Phase 2 – OCR** | | | | |
| T10 | `MlKitOcrEngine` | T02 | S | Kilo |
| T11 | Debug-only OCR dump tool (builds your fixtures) | T10 | M | Kilo + You |
| **Phase 3 – Bank parsers** | | | | |
| T12 | Revolut | T08, T09, T11 | M | You + Kilo |
| T13 | Monzo | T12 | M | You + Kilo |
| T14 | Barclays | T12 | M | You + Kilo |
| T15 | Chase | T12 | M | You + Kilo |
| T16 | Template for "other banks" | T12 | S | Kilo |
| **Phase 4 – App integration** | | | | |
| T17 | `DuplicateDetector` | T02 | M | Kilo |
| T18 | `ImportService.prepare` (normalise, categorise, flag duplicates) | T07, T17 | M | Kilo |
| T19 | `ImportService.commit` (save selected items) | T18 | S | Kilo |
| T20 | `ImportViewModel` | T19, T10 | M | Kilo |
| T21 | `ImportScreen` (entry point, bank + image picker) | T20 | M | Kilo |
| T22 | `ReviewScreen` | T21 | L | Kilo |
| T23 | Share-sheet intake | T21 | S | Kilo |
| **Phase 5 – Hardening** | | | | |
| T24 | Deskew for photographed screens (optional) | T07, T10 | M | Kilo |
| T25 | Privacy hardening and airplane-mode verification | T22 | S | Kilo + You |
| T26 | UI tests and manual QA checklist | T22 | M | Kilo + You |
| T27 | CI and final documentation | all | S | Kilo |

## Suggested order

1. T00 → T11 in order (≈ one evening per 3–4 tasks).
2. While tasks run, do the manual capture work for your most-used bank (`02-PREREQUISITES-MANUAL-STEPS.md`, section 3).
3. T12 (first bank), then T17 → T22 for the app integration. Banks T13–T15 can be done any time after T12, one per session.
4. T23–T27 last.

## Definition of done for the whole feature

- `./gradlew testDebugUnitTest` green, including one golden fixture set per bank.
- Airplane-mode run works up to the review screen (T25).
- A release build does **not** contain the debug OCR dump activity (T25).
- Nothing in the importer package logs OCR text, vendors or amounts (enforced by a test, T25).
- Duplicates from an overlapping second screenshot are flagged (T17, T26).
