# T27 — CI and final documentation

**Depends on:** all earlier tasks · **Size:** S

## Goal

Make CI protect the feature and leave the family and your future self with clear docs.

## Read first

`.github/workflows/*` (see APP_CONTEXT), `docs/screenshot-import/ADDING_A_BANK.md` (T16), `README.md` of this plan.

## Changes

1. **Unit tests in CI.** Confirm the existing build workflow runs `./gradlew testDebugUnitTest` (this includes the golden fixtures, `FixtureSafetyTest`, `NoLoggingInImporterTest`). If it only runs `assembleDebug`, add the test step before the build step. Do not add emulator tests to CI.
2. **Private-data guard step** (add to the same job):
   ```yaml
   - name: Block private fixtures
     run: |
       if git ls-files | grep -E '^fixtures-private/|\.ocr\.private\.json$'; then
         echo "Private fixture files are tracked by git"; exit 1
       fi
   ```
3. **Release APK check** (add after the step that builds the release APK; skip if the pipeline builds no APK in this job):
   ```yaml
   - name: Debug tool must not be in release
     run: |
       AAPT2=$(ls -d "$ANDROID_HOME"/build-tools/*/aapt2 | sort -V | tail -1)
       APK=$(find app/build/outputs/apk/release -name '*.apk' | head -1)
       test -n "$APK"
       if "$AAPT2" dump xmltree --file AndroidManifest.xml "$APK" | grep -q OcrDump; then
         echo "OcrDump is in the release manifest"; exit 1
       fi
   ```
4. **User guide** `docs/screenshot-import/USER-GUIDE.md` (≤ 60 lines, plain language, for the family): what the feature does; "the picture never leaves your phone"; steps (Home → Import screenshot → choose bank → choose screenshot → check every row → Import); what the badges mean; what to do when rows are wrong (edit or untick); which banks are supported; tip: screenshot 8–12 rows at a time and avoid cutting a row in half.
5. **Known limits** section at the end of the user guide (copy from `00-ARCHITECTURE.md` §11, in plain words).
6. **Tick everything** in `README.md` (this plan) and add the final test count to `docs/screenshot-import/README.md` if you keep one.

## Done when

- A push to the feature branch runs the workflow green on GitHub.
- Temporarily adding a file `fixtures-private/x.txt` to git makes the guard step fail (try it locally with the `git ls-files | grep` line, then remove the file).

## Do NOT

- Add secrets, new services or emulator-based jobs.

## Commit message

`ci: run importer tests, block private fixtures and debug tool in release; add user guide`
