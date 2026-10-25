# T21 — `ImportScreen` (entry point, bank and image picker, progress, result)

**Depends on:** T20 · **Size:** M

## Goal

The user-facing entry: a button in the app that opens the import flow; the first step lets the user pick the bank, choose whether to include income, and pick a screenshot; progress and completion are shown. The review step is a placeholder here and is built in T22.

The examples assume **Jetpack Compose + Material 3**. If `docs/APP_CONTEXT.md` says the app uses XML views, stop and ask the user before continuing; this task must be rewritten for that toolkit.

## Read first

`docs/APP_CONTEXT.md` (navigation file, Home screen file, "where an extra button could go", theme), the Add Expense screen (as a style reference), `ImportViewModel`, `ImportUiState`.

## Create / change

| File | Change |
|---|---|
| `…/importer/ui/ImportScreen.kt` | new: `ImportScreen(viewModel, onClose)` |
| Navigation file | new route `"import"` showing `ImportScreen`; ViewModel created the way other screens do |
| Home screen file | one new action/button **"Import screenshot"** (position from `APP_CONTEXT.md`) navigating to `"import"` |
| `app/src/main/res/values/strings.xml` | new strings (all user-visible text goes here) |

## Behaviour

```kotlin
@Composable
fun ImportScreen(viewModel: ImportViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onImagePicked(ImageRef(uri.toString()))
    }
    when (state.phase) {
        Phase.PICK -> PickStep(state, onBank = viewModel::onBankSelected,
            onIncome = viewModel::onIncludeIncomeChanged,
            onChoose = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
        Phase.WORKING, Phase.SAVING -> ProgressStep(saving = state.phase == Phase.SAVING)
        Phase.REVIEW -> Text("Review (T22)")             // placeholder, replaced in T22
        Phase.DONE -> DoneStep(state.result, onAnother = viewModel::onStartOver, onDone = onClose)
    }
}
```

`PickStep` content, top to bottom:

1. Title **Import from screenshot**.
2. Reassurance line: *"The image is read on this phone only. It is never uploaded or saved."*
3. **Bank** dropdown (`ExposedDropdownMenuBox` with an `OutlinedTextField`; entries from `Bank.values()` using `displayName`).
4. Switch **Include money in (income)** bound to `includeIncome`, default off, with a small helper text *"Most imports are expenses only."*
5. Button **Choose screenshot**.
6. If `state.error != null`: error text, plus `viewModel.onErrorShown()` when it is dismissed or the user changes something.

`ProgressStep`: centred `CircularProgressIndicator` + text *"Reading the screenshot on this phone…"* (or *"Saving…"*).

`DoneStep`: *"Imported N transactions"*; if `failed > 0` or `skippedNoCategory > 0` add a second line with those counts; buttons **Import another** and **Done**. **Done** calls `onClose` (navigate back to where the user came from, ideally the Monthly Transactions list so they see the result).

Use `Modifier.semantics`/`contentDescription` on the dropdown and button for accessibility. Support dark theme by using the app's existing theme colours only.

## Tests

- No new unit tests (logic is in the ViewModel). A minimal Compose UI test comes in T26.
- Manual check by **you**: open Home → Import screenshot → pick bank → pick image → progress → (placeholder) review.

## Done when

```
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

passes and the screen opens on the phone.

## Do NOT

- Request any storage or camera permission: the photo picker needs none.
- Copy the picked image anywhere.
- Add the share-sheet handling (T23) or the review UI (T22).

## Commit message

`feat(importer): add import entry screen and navigation`
