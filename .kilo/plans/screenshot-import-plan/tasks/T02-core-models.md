# T02 — Core models

**Depends on:** T01 · **Size:** S

## Goal

Create the OCR and parsed-transaction data classes and the parser interface, exactly as in `00-ARCHITECTURE.md` §4.

## Read first

`00-ARCHITECTURE.md` §3 and §4, `docs/APP_CONTEXT.md` (package, source root).

## Create

| File | Content |
|---|---|
| `<SRC>/<PKG>/importer/model/OcrModels.kt` | `OcrBox`, `OcrWord`, `OcrResult` (copy from architecture §4) |
| `<SRC>/<PKG>/importer/model/ImageRef.kt` | `data class ImageRef(val uri: String)` (architecture §4) |
| `<SRC>/<PKG>/importer/model/ParsedModels.kt` | `Bank`, `TransactionKind`, `ParsedTransaction`, `ParseResult` |
| `<SRC>/<PKG>/importer/parsing/BankStatementParser.kt` | the `BankStatementParser` interface |
| `app/src/test/…/importer/model/ModelsTest.kt` | tests below |

Package declarations must match the folders. Copy the code from the architecture file **verbatim**, only replacing `<PKG>`.

## Tests (write first)

`ModelsTest`:

1. `OcrBox(10, 20, 110, 60)` → `width == 100`, `height == 40`, `centerX == 60.0`, `centerY == 40.0`.
2. `ParsedTransaction(LocalDate.of(2026,10,24), "Tesco", 1250, TransactionKind.EXPENSE, "raw")` → `pending == false` and `dateUncertain == false`.
3. `OcrWord("a", OcrBox(0,0,1,1)).angleDeg == 0f`.
4. `ImageRef("content://x").uri == "content://x"`.
5. `Bank.valueOf("REVOLUT").displayName == "Revolut"`; `Bank.values().size == 5`.

## Done when

```
./gradlew :app:testDebugUnitTest --tests "*ModelsTest"
```

passes (replace `:app` with the module name from APP_CONTEXT).

## Do NOT

- Add Android imports (`android.*`) to these files.
- Add extra fields or helper functions.

## Commit message

`feat(importer): add OCR and parsed transaction models`
