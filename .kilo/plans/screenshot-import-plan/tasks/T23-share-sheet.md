# T23 — Share-sheet intake ("Share → Expense Tracker")

**Depends on:** T21 · **Size:** S

## Goal

From the gallery (or the screenshot preview), choose **Share → <app name>** and land directly on the import flow with that image — but only **after** the app's sign-in/biometric step.

## Read first

`docs/APP_CONTEXT.md` (main activity file, biometric/sign-in gate, navigation file), `MainActivity`, `ImportScreen.kt`.

## Create / change

| File | Change |
|---|---|
| `…/importer/share/OneShotHolder.kt` | generic holder (pure Kotlin, tested) |
| `…/importer/share/PendingSharedImage.kt` | `object PendingSharedImage { val holder = OneShotHolder<ImageRef>() }` |
| `AndroidManifest.xml` | intent filter on the main activity |
| `MainActivity` | read `ACTION_SEND` image intents (in `onCreate` and `onNewIntent`) and store the Uri |
| Navigation / sign-in code | after authentication succeeds, if a pending image exists → navigate to `"import"` |
| `ImportScreen.kt` | on first composition, `PendingSharedImage.holder.take()?.let(viewModel::onSharedImage)` |
| `app/src/test/…/importer/share/OneShotHolderTest.kt` | tests |

## `OneShotHolder`

```kotlin
class OneShotHolder<T : Any> {
    private val value = AtomicReference<T?>(null)
    fun set(item: T) { value.set(item) }
    fun take(): T? = value.getAndSet(null)
    fun hasValue(): Boolean = value.get() != null
}
```

Tests: `take()` on empty → `null`; `set` then `take` → the value, then `null`; second `set` replaces the first; `hasValue` reflects state.

## Manifest

Inside the main activity (keep its existing filters):

```xml
<intent-filter>
    <action android:name="android.intent.action.SEND" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="image/*" />
</intent-filter>
```

## Activity code

```kotlin
private fun handleShareIntent(intent: Intent?) {
    if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
        IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?.let { PendingSharedImage.holder.set(ImageRef(it.toString())) }
    }
}
```

Call it from `onCreate` (after `setContent` setup is fine) and from `onNewIntent`.

## Important rules

- **Do not process the image before the user has signed in.** Only store the `Uri`; navigation to `"import"` happens after the gate passes (use the same mechanism the app already uses to decide that the user is authenticated, see `APP_CONTEXT.md`).
- The shared `Uri` carries a temporary read permission that lives as long as the receiving task; the import starts immediately after sign-in and reads the image once. Do **not** persist the `Uri` (no saved state, no DataStore).
- If the user shares several images (`ACTION_SEND_MULTIPLE`), ignore it (not declared in the manifest).

## Reference code

**Verified (JVM tests pass):** `reference/main/share/OneShotHolder.kt.txt`. Try it yourself first; after 2 failed attempts copy the reference (rename `.kt.txt` to `.kt`, replace package `com.example.expenses` with `<PKG>`).

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*OneShotHolderTest"
./gradlew :app:assembleDebug
```

**You**: share a screenshot from the gallery → app opens → sign-in → import flow starts with that image; share while the app is already open → it jumps to the import flow.

## Do NOT

- Add `ACTION_VIEW` or other filters.
- Copy the shared image to app storage.

## Commit message

`feat(importer): accept screenshots from the Android share sheet`
