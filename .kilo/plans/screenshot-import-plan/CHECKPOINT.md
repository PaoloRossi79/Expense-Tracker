# CHECKPOINT: state of the screenshot-import plan

Last updated: 2026-10-25. Read this first if you (or another session) continue the work.

## Decisions (final)
- Approach A: on-device ML Kit OCR (`com.google.mlkit:text-recognition:16.0.1`, bundled, verified on Google's docs, latest as of Jul 2026) + rule-based parsing.
- Banks: Revolut, Monzo, Barclays, Chase + GENERIC; user picks the bank in the UI. GBP only. Income parsed but hidden by default.
- Kotlin native; tasks written for Kilo with Ollama/DeepSeek.
- Privacy: no image or OCR text ever leaves the phone, and no real fixtures in git or in any AI chat.

## Done
| Item | Status |
|---|---|
| README, 00-ARCHITECTURE, 01-KILO-USAGE-GUIDE, 02-PREREQUISITES | written (earlier pass); need the small fixes below |
| tasks T00 to T23, `_BANK-PROCEDURE`, `_BANK-WORKSHEET` | written (earlier pass); need the small fixes below |
| `reference/`: verified Kotlin reference code and tests | DONE: 98 JVM tests pass (Kotlin 2.0.21, JUnit 4.13.2), mutation-checked. Files are `.kt.txt`; rename to `.kt`, replace package `com.example.expenses` with `<PKG>` |
| Not compiled/verified (Android-only) | MlKitOcrEngine, debug OcrDumpActivity, Compose screens, adapters, manifest, share intent |

Re-run the verification: `reference/verify-build.sh` expects the layout of the original sandbox (main/, test/, stubs/, res/); the stub for androidx ViewModel is `reference/ViewModelStub.kt.forverification`.

## Remaining work (in this order)
1. **Write missing task files** `tasks/T24..T27` (listed in README, files do not exist):
   - T24 Deskew (optional): `Deskewer.deskew(OcrResult)`: median word `angleDeg`; if |median| < 0.5 or > 15 return unchanged; else rotate word centres about the image centre by -median, keep each word's width/height. Applied in `ImportViewModel` right after `ocr.recognise`. Test: rotate a synthetic OCR by 3 degrees, parse, same rows.
   - T25 Privacy hardening: `NoLoggingInImporterTest` scanning `<SRC>/<PKG>/importer/**` for `Log.`, `println(`, `Timber`, `printStackTrace`, `System.out`, `java.net.`, `okhttp`, `retrofit`, `Crashlytics`, `FirebaseAnalytics`; release APK must not contain `OcrDumpActivity` (`aapt2 dump xmltree` or `apkanalyzer manifest print`); airplane-mode manual check; release-build smoke test (R8).
   - T26 Tests: copy `reference/test/DomainTests.kt.txt` `ImportFlowIntegrationTest` (import twice -> duplicates flagged); Compose UI test for ReviewScreen with `testTag`s (`review_row_<id>`, `review_checkbox_<id>`, `review_import_button`, `review_hidden_income_text`, `review_empty_state`); `docs/screenshot-import/QA-CHECKLIST.md` (per bank, dark theme, big font, back navigation, share sheet, offline, creator shown, release build).
   - T27 CI and docs: confirm workflow runs `testDebugUnitTest`; step failing if `git ls-files` contains `fixtures-private/` or `*.ocr.private.json`; release-APK check for `OcrDumpActivity`; `docs/screenshot-import/USER-GUIDE.md` for the family; tick README.
2. **Apply the design fixes found during verification** (the reference code already has them):
   - **`ImageRef`**: add `model/ImageRef.kt` (`data class ImageRef(val uri: String)`). `OcrEngine.recognise(image: ImageRef)`, `ImportViewModel.onImagePicked/onSharedImage(image: ImageRef)`, T23 holder is `OneShotHolder<ImageRef>`. Reason: `android.net.Uri` cannot be created in plain JVM unit tests. `MlKitOcrEngine` converts with `Uri.parse(image.uri)`; Compose/Activity code uses `ImageRef(uri.toString())`. Update T02 (models + test), T10, T20, T21, T23 and architecture section 4.
   - **T05**: the noise prefix must also match when it is the whole text (followed by space, `:` or end of text).
   - **T20**: after `commit`, if `saved == 0 && failed > 0` return to REVIEW with "Saving failed. Nothing was saved; try again." (the service never throws for per-item failures, so the old test 11 was impossible). Add a note that the ViewModel needs `kotlinx-coroutines-test` (`Dispatchers.setMain`) in real tests.
   - **T00**: add questions: where is vendor usage / last-used category recorded when an expense is saved (repository or ViewModel)? does the repository `add` wait for the Firestore server ack?
   - **T19**: the writer adapter must also call the same vendor-usage recording as Add Expense; wrap the write with a timeout (about 5 s); when Firestore offline persistence is on, a timeout means "queued", so count it as saved (an offline `await()` never completes and would hang the SAVING screen).
   - **T09**: replace the `TODO()` in the `Fixtures.loadExpected` snippet with a pointer to `reference/test/testutil/Fixtures.kt.txt` (`parseExpected` is implemented there).
   - **T04, T06, T07, T08, T16, T17, T18, T22, T23**: add a line "Reference (verified): `reference/...`. Try yourself first; after 2 failed attempts copy it". T07's inline engine is the staged version (no PER_ROW, pending, avatar); the final engine is in `reference/main/parsing/ProfileBasedParser.kt.txt`.
   - **01-KILO-USAGE-GUIDE**: Kilo's own docs recommend `num_ctx` of at least 32768 (use 16384 only as the minimum); models they suggest: `qwen3-coder:30b` (main) or `devstral:24b`; tool-call glitches: rephrase or use "Enhance Prompt"; disable MCP tools and keep sessions short; rules: `.kilocode/rules/` still works (backward compatible), the newer Kilo uses `kilo.jsonc` `instructions` with `.kilo/rules/*.md`.
3. **Optionally split the grouped test files** in `reference/test/` (BasicParsingTests, FixtureTests, DomainTests) into one file per class, matching the task names.
4. **Package and deliver**: zip `screenshot-import-plan/`, send to the user, write README and ARCHITECTURE to the claude.ai Project.

## Verified facts used
- ML Kit text-recognition 16.0.1 bundled (about 4 MB per script per architecture), minSdk 23 per Google docs page.
- Kilo + Ollama: Kilo docs say at least 32k context; `.kilocode/rules/` included for backward compatibility.
- Maven Central is not reachable from the authoring sandbox; only pure-Kotlin parts were compiled and run (Gradle's bundled Kotlin 2.0.21 compiler).

## Progress log
- T24 Deskewer: reference + test verified (102 tests green): reference/main/ocr/Deskewer.kt.txt
- T24 to T27 task files written. Reference pointers added to T04,05,06,08,09,16,17,18,20,22,23. Fixes applied: T05 end-of-text, T09 TODO note, T20 commit-failure rule, T00 vendor-usage and add-wait questions, T19 vendor usage + offline timeout.
- ImageRef changes + Kilo guide + README updated (done). STILL TO DO: final consistency grep, zip, deliver, project docs.
