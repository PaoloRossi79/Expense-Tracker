# 02 — Prerequisites and manual steps (you do these)

Estimated total: ~30–45 min of setup, then ~30–45 min per bank for fixtures.

## 1. One-time setup

1. **Branch**: `git checkout -b feature/screenshot-import`.
2. **Copy the plan** into the repo as `docs/screenshot-import/` and commit it.
3. **Confirm the build works from a terminal** (Kilo will use the same commands):
   ```
   ./gradlew assembleDebug
   ./gradlew testDebugUnitTest
   ```
   Both must pass before T00. Fix any existing failure first.
4. **Ollama** model with a 16k context, and the Kilo rules file: see `01-KILO-USAGE-GUIDE.md`, section 1.
5. **Create a private folder** `fixtures-private/` in the repo root. T01 adds it to `.gitignore`. Raw OCR dumps and original screenshots live only here. Never commit it, never paste its content into an AI chat.
6. **Test phone**: your own Android phone with USB debugging or at least USB file transfer. The debug build will install a second launcher icon called **OCR Dump** (T11).

## 2. Accounts and keys

None needed. ML Kit's bundled model works without a Google account, API key or Firebase setup, and the feature adds no new cloud service.

## 3. Per-bank fixtures (do this for each bank before its task T12–T15)

Goal: 2–4 screenshots per bank that cover its layouts, converted into **anonymised OCR fixtures** plus a **hand-written expected file**.

### 3.1 Capture

Take screenshots of the bank app's transaction list. For each bank cover:

| # | Variant | Why |
|---|---|---|
| 1 | Normal list: 8–12 rows, light theme | Baseline |
| 2 | List that starts in the **middle** of a day (no header on top) and ends in the middle of another | Tests date handling without a header |
| 3 | A list containing **money in**, a **refund**, a **pending** item, and (if your bank has them) a pot/round-up/transfer | Tests income, pending and ignore rules |
| 4 | Same as 1 in **dark theme** | OCR accuracy differs |

Tips: screenshot the list at normal font size; keep balance/total widgets in the shot if they are normally visible, since the parser must ignore them. If the bank app blocks screenshots, see T24 (photograph the screen with a second device).

### 3.2 Dump the OCR (after T11 is done)

1. Install the debug build; open the **OCR Dump** app icon.
2. Pick a screenshot → it shows the recognised text and saves `…ocr.json` in the app's files folder.
3. Copy the files to your PC over USB (`Android/data/<your applicationId>/files/ocr-dumps/`), or use **Copy JSON** and paste into a file. Put them in `fixtures-private/<bank>/`. Do **not** email them or send them through a cloud service; if you must move them wirelessly, use a local method (for example a local network transfer you control).
4. Keep the original screenshots in `fixtures-private/` too, so you can check them against the expected file.

### 3.3 Anonymise (mandatory before anything is committed)

Open each `.ocr.json` in VS Code and replace, using *Find and Replace* on the `"text"` values only:

- every vendor / person name that identifies you or your family → a generic one (`Vendor A`, `Vendor B`, `Shop One`…). Keeping real big-brand names (Tesco) is fine; remove names of people, small local shops, employers;
- every reference/ID/card/account/sort code/phone/email → remove or replace;
- optionally round the amounts (e.g. 12.50 stays, 1,234.56 becomes 1,200.00). Edit both the OCR file and the expected file in the same way.

Never change the numeric `left/top/right/bottom` values; the layout must stay intact.

Then save the anonymised file as `app/src/test/resources/fixtures/<bank>/<name>.ocr.json` (T09 explains naming). The `FixtureSafetyTest` (T09) will fail if it finds things that look like sort codes, account numbers, IBANs, card numbers, emails or phone numbers.

Final check before commit: search the fixture for your own name, your family's names, your employer and your street.

### 3.4 Write the expected file

Open the original screenshot next to `<name>.expected.txt` and write one line per **transaction you want imported**, top to bottom, in the format from `00-ARCHITECTURE.md` §6. Decide the `today:` header as the date the screenshot was taken (it is used to resolve "Today", "Yesterday" and dates without a year).

Include income lines too (the parser detects them; the import switch hides them later).

### 3.5 Fill the worksheet

Fill `tasks/_BANK-WORKSHEET.md` (one copy per bank, save it as `docs/screenshot-import/worksheets/<bank>.md`). The bank task gives it to Kilo.

## 4. After the app tasks

- T25: run the airplane-mode check and the release-build check by hand.
- T26: walk through the manual QA checklist on a real phone with real (private) screenshots, **without** committing anything.
- Importing for real: first import goes through the review screen; compare each row against the bank app, at least for the first 2–3 imports per bank.

## 5. Things that need your decision during the work

| When | Decision |
|---|---|
| T18 | Where pending transactions should land by default (plan: unticked) |
| T21 | Where the "Import screenshot" button lives (Home top bar, Add Expense, or overflow menu); T00 lists the options |
| T22 | Whether income rows show in the review list at all when the switch is off (plan: hidden, with a count) |
| T24 | Whether you need photo-of-screen support at all (only if a bank blocks screenshots) |
