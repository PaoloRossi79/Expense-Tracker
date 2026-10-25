# T24 — Deskew for photographed screens (optional)

**Depends on:** T07, T10, T20 · **Size:** M · **Do it only if** a bank app blocks screenshots and you photograph the screen with a second device.

## Goal

Photos of a screen are slightly rotated. `Deskewer.deskew(ocr)` rotates the recognised words back so the row grouping (T06) works. Rotation only: no perspective correction. Photos stay less accurate than screenshots.

## Read first

`00-ARCHITECTURE.md` §2, `OcrModels.kt`, `ImportViewModel.kt`.
**Reference (verified, JVM tests pass):** `reference/main/ocr/Deskewer.kt.txt` and `reference/test/DeskewerTest.kt.txt`. Try the algorithm yourself first; after 2 failed attempts copy the reference (replace `com.example.expenses` with `<PKG>`).

## Create / change

| File | Content |
|---|---|
| `…/importer/ocr/Deskewer.kt` | `object Deskewer { fun deskew(ocr: OcrResult): OcrResult }` |
| `app/src/test/…/importer/ocr/DeskewerTest.kt` | tests below |
| `ImportViewModel.kt` | in `onImagePicked`, call `Deskewer.deskew(...)` on the result of `ocr.recognise(...)` **before** caching it |

## Algorithm

1. No words → return the same object.
2. `median` = median of `word.angleDeg` (angle of the word's baseline, 0 = upright, positive = clockwise).
3. If `abs(median) < 0.5` or `> 15` → return the same object (nothing to fix, or too tilted to trust).
4. Otherwise rotate every word's **centre** about the image centre by `-median` degrees (screen coordinates, y down: `nx = cx + dx·cos t − dy·sin t`, `ny = cy + dx·sin t + dy·cos t`, with `t = −median` in radians). Keep each word's width and height, round to `Int`, set `angleDeg = 0`.

## Tests (write first) — `DeskewerTest`

Helper `tilt(ocr, deg)` rotates word centres by `+deg` and sets `angleDeg = deg` (simulates a tilted photo; see the reference test).

1. Median 0.2° → returns the **same instance**.
2. Median 30° → same instance.
3. Empty OCR → same instance.
4. Upright OCR with 4 transaction rows (spread over the whole image height) tilted by `3.0` and by `-2.0`, then deskewed → `ProfileBasedParser(GENERIC_PROFILE)` returns the same (vendor, amount) pairs as the upright OCR.

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*DeskewerTest"
./gradlew :app:testDebugUnitTest
```

(The ViewModel tests must still pass: their fake OCR words have `angleDeg = 0`, so the deskewer is a no-op.)

## Do NOT

- Use `android.graphics`, OpenCV or any new dependency.
- Try to correct perspective (trapezoid) distortion.

## Commit message

`feat(importer): deskew slightly tilted photos`
