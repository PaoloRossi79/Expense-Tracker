# T01 — Add dependencies and ignore rules

**Depends on:** T00 · **Size:** S

## Goal

Add exactly the libraries the feature needs and make sure private fixtures can never be committed.

## Read first

`docs/APP_CONTEXT.md` (Build section), the app's `build.gradle(.kts)`, `gradle/libs.versions.toml` if present, `.gitignore`.

## Changes

Follow the style the project already uses (version catalog **or** direct strings, `.kts` **or** Groovy). Only **add**; do not upgrade or reorder anything.

1. **ML Kit text recognition (bundled model, works fully offline from install):**
   ```kotlin
   implementation("com.google.mlkit:text-recognition:16.0.1")
   ```
   16.0.1 is the version shown on Google's ML Kit documentation. If a newer stable version exists and the build is fine with it, you may use it, but note it in the commit message.
   Use the **bundled** artifact, not `play-services-mlkit-text-recognition` (that one downloads its model through Google Play services at first use).

2. **Test-only JSON reader for fixtures (unit tests):**
   ```kotlin
   testImplementation("com.google.code.gson:gson:2.11.0")
   ```
   Skip this line if `APP_CONTEXT.md` says Gson is already available to unit tests. If the app already uses Moshi or kotlinx.serialization, still use Gson here: it needs no compiler plugin, and it is only for tests and the debug tool.

3. **Debug tool JSON writer (T11):**
   ```kotlin
   debugImplementation("com.google.code.gson:gson:2.11.0")
   ```

4. **Only if `APP_CONTEXT.md` says desugaring is needed (minSdk < 26)**, enable core library desugaring so `java.time.LocalDate` works:
   ```kotlin
   android { compileOptions { isCoreLibraryDesugaringEnabled = true } }
   dependencies { coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5") }
   ```
   (Use the latest 2.x version if 2.1.5 is not available.)

5. **Coroutines test and JUnit** are expected to exist already (see APP_CONTEXT). If `kotlinx-coroutines-test` is missing, add it as `testImplementation` with the same version as the project's coroutines version.

6. **`.gitignore`**: append these lines (keep existing content):
   ```
   # screenshot import: private bank data must never be committed
   fixtures-private/
   *.ocr.private.json
   ```

## Done when

```
./gradlew assembleDebug testDebugUnitTest
```

passes, and `git diff --stat` shows only the build file(s), the version catalog (if used) and `.gitignore`.

## Do NOT

- Add any networking, analytics, Firebase ML or "Google Play services" ML artifact.
- Change `minSdk`, Kotlin, AGP or Gradle versions.

## Commit message

`build: add ML Kit text recognition and test deps for screenshot import`
