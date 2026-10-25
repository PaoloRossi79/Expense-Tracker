# T06 — `LayoutAnalyzer` and the synthetic OCR test builder

**Depends on:** T02 · **Size:** M

## Goal

Group OCR words into horizontal **bands** (visual rows) and split each band into **segments** (text separated by a large horizontal gap). Also create the test helper `ocr { }` that every later parser test uses.

## Read first

`00-ARCHITECTURE.md` §5.1.

## Create

- `<SRC>/<PKG>/importer/parsing/LayoutAnalyzer.kt`
- `app/src/test/…/importer/testutil/OcrTestBuilder.kt`
- `app/src/test/…/importer/parsing/LayoutAnalyzerTest.kt`

## Contract

```kotlin
data class Segment(val text: String, val box: OcrBox)

data class Band(val segments: List<Segment>) {
    val text: String get() = segments.joinToString(" ") { it.text }
    val centerY: Double get() = segments.map { it.box.centerY }.average()
}

object LayoutAnalyzer {
    fun buildBands(
        ocr: OcrResult,
        bandTolerance: Double = 0.6,
        segmentGapFactor: Double = 1.5,
    ): List<Band>
}
```

Algorithm = `00-ARCHITECTURE.md` §5.1, restated:

1. No words → empty list.
2. `medianHeight` = median of word heights greater than 0 (if there are none: `1.0`).
3. Sort words by `box.centerY`. Walk them; keep the running mean `centerY` of the current band. A word joins the band if `abs(word.centerY − bandMean) <= bandTolerance × medianHeight`, else a new band starts.
4. Sort the band's words by `box.left`. Start a new segment when `word.box.left − previous.box.right > segmentGapFactor × medianHeight`.
5. Segment text = words joined with a single space. Segment box = union (min left/top, max right/bottom).
6. Return bands ordered top to bottom.

## Test builder (exact behaviour)

```kotlin
fun ocr(width: Int = 1080, height: Int = 2400, block: OcrBuilder.() -> Unit): OcrResult

class OcrBuilder {
    fun row(y: Int, block: RowBuilder.() -> Unit)   // y = vertical centre of the row in px
}
class RowBuilder {
    /** One word starting at x. Width = 22 px per character, height = 40 px. */
    fun word(text: String, x: Int)
    /** Several words separated by single spaces, starting at x, with a 20 px gap between words. */
    fun words(text: String, x: Int)
    /** Right-aligned: the word ends at (image width − margin). Default margin 60. */
    fun right(text: String, margin: Int = 60)
}
```

Word box: `left = x`, `right = x + 22 × text.length`, `top = y − 20`, `bottom = y + 20`. `right(...)` computes `x = imageWidth − margin − 22 × text.length`. The builder keeps `imageWidth` for that purpose. `words("Card payment", x)` adds `Card` at `x`, then `payment` at `x + 22×4 + 20`.

With these numbers `medianHeight = 40`, so the default band tolerance is 24 px and the segment split threshold is 60 px.

## Tests (write first) — `LayoutAnalyzerTest`

1. Empty OCR → empty list.
2. One row: `word("Tesco", 100)` and `right("-£12.50")` → **1 band, 2 segments**, texts `Tesco` and `-£12.50`.
3. Two rows at y=400 and y=460 → **2 bands**, first is the upper one.
4. Words of the same row given in reverse order → still sorted left to right.
5. Rows given bottom-first → bands still top to bottom.
6. `words("Card payment", 100)` → **1 segment** with text `Card payment`.
7. `word("-", 860)` then `word("£12.50", 860 + 22 + 20)` (20 px gap) → **1 segment** `- £12.50`.
8. Two words 400 px apart in the same row → **2 segments**.
9. A word at y=410 and another at y=400 (10 px off) → same band; at y=440 and y=400 → different bands (40 px > 24 px).
10. Segment box is the union: for `words("Card payment", 100)` the box `left == 100`, `right == 100 + 22×4 + 20 + 22×7`.

## Reference code

**Verified (JVM tests pass):** `reference/main/parsing/LayoutAnalyzer.kt.txt`, `reference/test/testutil/OcrTestBuilder.kt.txt`. Try it yourself first; after 2 failed attempts copy the reference (rename `.kt.txt` to `.kt`, replace package `com.example.expenses` with `<PKG>`).

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*LayoutAnalyzerTest"
```

## Do NOT

- Put any amount/date/vendor logic here; this class only knows geometry.
- Use `android.graphics.Rect`/`Point`.

## Commit message

`feat(importer): add LayoutAnalyzer and OCR test builder`
