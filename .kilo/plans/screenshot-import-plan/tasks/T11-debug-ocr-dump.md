# T11 — Debug-only OCR dump tool

**Depends on:** T10 · **Size:** M

## Goal

A tiny debug-build-only app screen that turns one of your screenshots into the **OCR JSON** used for fixtures, and shows the bands the parser will see. This is how real bank layouts get into the test suite without ever sending an image anywhere.

It must exist **only in the `debug` source set** so that no release APK (the one emailed to the family) contains it.

## Read first

`docs/APP_CONTEXT.md` (debug source set, package), `MlKitOcrEngine`, `LayoutAnalyzer`, `00-ARCHITECTURE.md` §6.

## Create

| File | Content |
|---|---|
| `app/src/debug/AndroidManifest.xml` | declares the activity with its own launcher entry |
| `app/src/debug/java/<PKG path>/debug/OcrDumpActivity.kt` | the screen (plain Android views, no Compose, no app theme dependency) |
| `app/src/debug/java/<PKG path>/debug/OcrDumpJson.kt` | `toJson(OcrResult): String`, `fromJson(String): OcrResult`, Gson-based |
| `app/src/testDebug/java/<PKG path>/debug/OcrDumpJsonTest.kt` | round-trip unit test (a `testDebug` source set, because the class only exists in debug) |

(If the project uses `src/debug/kotlin` instead of `java`, follow the project.)

## Manifest (debug only)

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <activity
            android:name="<PKG>.debug.OcrDumpActivity"
            android:exported="true"
            android:label="OCR Dump"
            android:theme="@android:style/Theme.Material.Light.DarkActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

This adds a **second launcher icon** named "OCR Dump" in debug builds only, independent of the app's navigation and sign-in.

## `OcrDumpActivity` behaviour

Build the layout in code: vertical `LinearLayout` with padding 24dp containing:

1. Button **Pick screenshot** → `registerForActivityResult(ActivityResultContracts.PickVisualMedia())` with `PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)`. (The system photo picker needs no storage permission.)
2. Button **Copy JSON** → copies the last JSON to the clipboard (`ClipboardManager`).
3. Button **Delete all dumps** → deletes every file in the dumps folder, then shows "Deleted N files".
4. A scrollable `TextView` (monospace) for output.

When an image is picked:

1. Run `MlKitOcrEngine(applicationContext).recognise(ImageRef(uri.toString()))` inside `lifecycleScope.launch`.
2. Convert to JSON with `OcrDumpJson.toJson`, write to `getExternalFilesDir("ocr-dumps")/dump-<yyyyMMdd-HHmmss>.ocr.json`.
3. Show: the saved file path, the word count, then the band listing — for each band from `LayoutAnalyzer.buildBands(ocr)`: `#NN y=<centerY int> [<left%>|<text>] [<left%>|<text>] …` (left% = `box.left × 100 / imageWidth`). This is the same format as `BandDump` in the tests.
4. On exceptions show the exception class name and message in the `TextView`; never crash.

The JSON must match the fixture format in `00-ARCHITECTURE.md` §6 exactly (`imageWidth`, `imageHeight`, `words[{text,left,top,right,bottom,angleDeg}]`).

## Unit test — `OcrDumpJsonTest`

Round trip: build an `OcrResult` with 3 words (one with `angleDeg = 1.5f`), `fromJson(toJson(x)) == x`. Also assert that `toJson` output contains the key `"words"`.

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*OcrDumpJsonTest"
./gradlew :app:assembleDebug
```

**You** (manual, 5 minutes): install the debug build, open **OCR Dump**, pick one screenshot, check that

- the band listing shows each transaction as one line with the vendor on the left (low %) and the amount on the right (high %),
- a JSON file appears in `Android/data/<applicationId>/files/ocr-dumps/` when the phone is connected to your PC by USB file transfer.

## Do NOT

- Put any of this in `src/main`.
- Upload, share-intent or email the JSON from inside the tool (copy to clipboard and USB only).
- Commit generated dumps; they belong in `fixtures-private/` (see `02-PREREQUISITES-MANUAL-STEPS.md` §3).

## Commit message

`feat(debug): add debug-only OCR dump tool for building fixtures`
