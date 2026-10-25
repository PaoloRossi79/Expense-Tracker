# T25 — Privacy hardening and airplane-mode verification

**Depends on:** T22 · **Size:** S · **Who:** Kilo (test) + You (manual checks)

## Goal

Turn the privacy rules of `00-ARCHITECTURE.md` §8 into checks that fail loudly: no logging or networking code in the importer, no debug tool in release builds, works offline.

## Read first

`00-ARCHITECTURE.md` §8, `docs/APP_CONTEXT.md` (source root, package, module name).

## 1. Kilo: `NoLoggingInImporterTest`

File: `app/src/test/…/importer/NoLoggingInImporterTest.kt`.

The test walks the folder `src/main/<java|kotlin>/<PKG path>/importer` (unit tests run with the module folder as working directory; use the real source root from APP_CONTEXT) and fails if any `.kt` file contains one of these substrings:

`Log.` · `println(` · `Timber` · `printStackTrace` · `System.out` · `System.err` · `java.net.` · `okhttp` · `retrofit` · `HttpURLConnection` · `Crashlytics` · `FirebaseAnalytics` · `logEvent`

Implementation hints:

```kotlin
class NoLoggingInImporterTest {
    private val forbidden = listOf("Log.", "println(", "Timber", "printStackTrace", "System.out", "System.err",
        "java.net.", "okhttp", "retrofit", "HttpURLConnection", "Crashlytics", "FirebaseAnalytics", "logEvent")

    @Test fun importerSourcesContainNoLoggingOrNetworking() {
        val root = File("src/main/java/<PKG path>/importer")       // adjust to the real source root
        assertTrue("importer folder not found: ${root.absolutePath}", root.isDirectory)
        val hits = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.flatMap { f ->
            val text = f.readText()
            forbidden.filter { text.contains(it) }.map { "${f.name}: $it" }
        }.toList()
        assertTrue("Forbidden tokens found: $hits", hits.isEmpty())
    }
}
```

A comment that happens to contain a token also fails the test: reword the comment. Fix real findings in the code (do not weaken the list).

Also add `OcrDumpNotInMainTest`: assert that no file under `src/main` contains `OcrDump`.

## 2. You: manual checks (about 15 minutes)

1. **Release build has no debug tool.** Build the release APK the way your release pipeline does (`./gradlew :app:assembleRelease`; if signing is not set up locally use the CI artifact). Then:
   ```
   $ANDROID_HOME/build-tools/<latest>/aapt2 dump xmltree --file AndroidManifest.xml app/build/outputs/apk/release/app-release.apk | grep -c OcrDump
   ```
   must print `0`. (Alternative: `apkanalyzer manifest print <apk> | grep -c OcrDump`.)
2. **Airplane mode.** Switch on airplane mode (Wi-Fi off too). Open the debug or release app, sign in, Import screenshot, pick a screenshot: the review screen must appear with rows. (Saving will queue offline; do not save real data during this test unless you want it.)
3. **No copy of the image.** After an import, check the app's cache folder size did not grow by an image (Settings → Apps → your app → Storage: cache should stay small).
4. **Release smoke test.** Install the **release** build and import one screenshot: R8/ProGuard can break libraries; if ML Kit fails only in release, add the keep rules from ML Kit's documentation and re-test.
5. **Crash reports.** If Crashlytics is used, confirm the importer never builds exception messages from OCR text (the ViewModel uses fixed messages by design).

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*NoLoggingInImporterTest" --tests "*OcrDumpNotInMainTest"
```

passes and you have ticked the five manual checks.

## Do NOT

- Add `FLAG_SECURE`, analytics or any new library.
- Remove strings from the forbidden list to make the test pass.

## Commit message

`test(importer): enforce no logging/networking in importer, verify release build`
