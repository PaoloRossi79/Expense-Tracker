# T10 — `OcrEngine` and the ML Kit implementation

**Depends on:** T02 (and T01 for the dependency) · **Size:** S

## Goal

Wrap ML Kit's on-device text recognition behind an interface, producing the project's own `OcrResult` (word-level boxes). Nothing else in the app may import ML Kit classes.

## Read first

`00-ARCHITECTURE.md` §4; `docs/APP_CONTEXT.md` (coroutines usage).

## Create

| File | Content |
|---|---|
| `…/importer/ocr/OcrEngine.kt` | interface |
| `…/importer/ocr/MlKitOcrEngine.kt` | ML Kit implementation |
| `…/importer/ocr/Geometry.kt` | pure helper `angleDegrees` (unit-testable) |
| `app/src/test/…/importer/ocr/GeometryTest.kt` | JVM test |
| `app/src/androidTest/…/importer/ocr/MlKitOcrEngineTest.kt` | instrumented test (needs a device/emulator) |

## `OcrEngine.kt`

```kotlin
interface OcrEngine {
    /** Reads the image at [uri] and returns every recognised word with its box. Never uploads anything. */
    suspend fun recognise(image: ImageRef): OcrResult
}
```

## `Geometry.kt`

```kotlin
/** Angle in degrees of the vector (x0,y0)->(x1,y1). 0 = pointing right; positive = clockwise on screen. */
fun angleDegrees(x0: Int, y0: Int, x1: Int, y1: Int): Float =
    if (x0 == x1 && y0 == y1) 0f
    else Math.toDegrees(Math.atan2((y1 - y0).toDouble(), (x1 - x0).toDouble())).toFloat()
```

`GeometryTest`: `(0,0,100,0)` → `0f`; `(0,0,100,100)` → `45f` (delta 0.01); `(0,0,100,-100)` → `-45f`; `(5,5,5,5)` → `0f`.

## `MlKitOcrEngine.kt` (reference)

```kotlin
class MlKitOcrEngine(private val context: Context) : OcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)   // Latin script, bundled model
    }

    override suspend fun recognise(imageRef: ImageRef): OcrResult = withContext(Dispatchers.Default) {
        val image = InputImage.fromFilePath(context, Uri.parse(imageRef.uri))       // reads the uri directly; applies EXIF rotation
        val text = recognizer.process(image).awaitResult()
        val rotated = image.rotationDegrees % 180 != 0
        OcrResult(
            imageWidth = if (rotated) image.height else image.width,
            imageHeight = if (rotated) image.width else image.height,
            words = text.textBlocks.flatMap { it.lines }.flatMap { it.elements }.mapNotNull { e ->
                val r = e.boundingBox ?: return@mapNotNull null
                val p = e.cornerPoints
                val angle = if (p != null && p.size >= 2) angleDegrees(p[0].x, p[0].y, p[1].x, p[1].y) else 0f
                OcrWord(e.text, OcrBox(r.left, r.top, r.right, r.bottom), angle)
            },
        )
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
```

Imports: `com.google.mlkit.vision.text.TextRecognition`, `com.google.mlkit.vision.text.latin.TextRecognizerOptions`, `com.google.mlkit.vision.common.InputImage`, `com.google.android.gms.tasks.Task`, `kotlinx.coroutines.*`, `kotlin.coroutines.resume`, `kotlin.coroutines.resumeWithException`.

ML Kit's `Text.Element` gives `text`, `boundingBox` and `cornerPoints`; its documentation page shows the Text → TextBlock → Line → Element hierarchy and the bundled-model dependency. Word-level (Element) granularity is deliberate: the layout analyser needs it to split vendor from amount even if ML Kit puts both in one line.

## Instrumented test (written, run by you on a device/emulator)

`MlKitOcrEngineTest`:

1. Create a 1080×600 white `Bitmap`, draw `Tesco` and `-£12.50` in black with `Canvas`/`Paint` (text size 48), save as PNG into `context.cacheDir`.
2. `runBlocking { MlKitOcrEngine(context).recognise(ImageRef(Uri.fromFile(file).toString())) }`.
3. Assert `imageWidth == 1080` and that some word `contains("Tesco", ignoreCase = true)`. Do **not** assert on the amount (the `£` glyph is the part OCR most often misreads).
4. Delete the temp file in a `finally`.

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*GeometryTest"
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
```

Both pass (the second proves the instrumented test compiles). **You**: run the instrumented test once on your phone (`./gradlew :app:connectedDebugAndroidTest`, or from Android Studio) and confirm it passes.

Provide one `MlKitOcrEngine` instance for the whole app (singleton in DI); the recognizer is created lazily.

## Do NOT

- Add any ML Kit class to a public signature (only `OcrResult` crosses the boundary).
- Save, copy or cache the image; do not log recognised text.
- Use the Google Play services (unbundled) artifact.

## Commit message

`feat(importer): add OcrEngine with ML Kit implementation`
