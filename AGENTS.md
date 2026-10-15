# AGENTS.md

Rules for AI coding agents working on this native Android (Kotlin) codebase. Read this file before changing anything. Where the existing code disagrees with this file, apply these rules to new and modified code, and report the mismatch rather than rewriting unrelated code.

---

## 0. Quick Reference

| Task | Command |
|---|---|
| Build everything | `./gradlew assembleDebug` |
| Unit tests (JVM) | `./gradlew testDebugUnitTest` |
| Instrumented + UI tests | `./gradlew connectedDebugAndroidTest` |
| Format | `./gradlew ktlintFormat` |
| Static analysis | `./gradlew detekt ktlintCheck lintDebug` |
| Coverage gate | `./gradlew koverVerify` (or `jacocoTestCoverageVerification`) |
| Dependency vulnerabilities | `./gradlew dependencyCheckAnalyze` |

Run format, static analysis, unit tests, and the coverage gate before reporting any task as done. Run instrumented tests whenever you touched Room, navigation, or Compose UI, and say clearly if no emulator was available.

### Boundaries

**Always**
- Read the module, its tests, and neighbouring code before writing.
- Add or update tests in the same change as the code.
- Keep changes small and scoped to the task.

**Ask first**
- Adding or upgrading a dependency, changing module boundaries, changing the database schema, changing navigation structure, changing minSdk/targetSdk, touching ProGuard/R8 rules, signing, or CI config.

**Never**
- Commit secrets, keystores, `google-services.json` with real credentials, or tokens.
- Disable or loosen lint, detekt, tests, coverage thresholds, certificate pinning, or network security config to make something pass.
- Use `!!`, `GlobalScope`, `runBlocking` in production code, or `Thread.sleep` in tests.
- Invent library APIs. Check the version in the version catalog and the real documentation.

---

## 1. Stack

- **Language:** Kotlin (no new Java). Kotlin explicit API mode for library modules.
- **UI:** Jetpack Compose + Material 3. No new XML layouts.
- **Architecture:** Clean Architecture, feature modules, unidirectional data flow.
- **DI:** Hilt. Do not mix DI frameworks.
- **Async:** Coroutines + Flow. No RxJava in new code.
- **Local data:** Room (structured), DataStore (preferences). No new SharedPreferences.
- **Network:** Retrofit + OkHttp + kotlinx.serialization.
- **Build:** Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`), convention plugins in `build-logic/`.
- **Static analysis:** ktlint, detekt, Android Lint, all warnings treated as errors for new code.

If the repo uses a different library for any of these, follow the repo and tell me.

---

## 2. Project Structure

```
app/                         composition root: Application, MainActivity, nav host, DI wiring
build-logic/                 convention plugins (shared Gradle config)
core/
  common/                    pure Kotlin utilities, Result/Error types, dispatcher provider
  designsystem/              theme, typography, reusable composables
  network/                   OkHttp/Retrofit setup, interceptors, error mapping
  database/                  Room database, DAOs, migrations
  security/                  Keystore, encryption, token storage
  testing/                   fakes, builders, rules, test dispatchers
feature/<name>/
  domain/                    models, repository interfaces, use cases (pure Kotlin)
  data/                      repository impls, remote/local sources, DTOs, mappers
  presentation/              ViewModel, UiState/Event/Effect, composables
```

### Dependency rules

1. `domain` has no Android or framework dependencies.
2. `data` depends on `domain`. `presentation` depends on `domain`. Neither depends on the other.
3. Features never depend on other features. Share code via `core/*`, or define a navigation contract.
4. `app` is the only module that sees everything.
5. Enforce this in CI with an architecture test (for example Konsist) or a Gradle dependency check. If it does not exist yet, propose adding it.

---

## 3. Clean Code Rules

### Design principles
- SOLID, with emphasis on single responsibility and dependency inversion.
- Prefer composition over inheritance. Prefer small, focused types.
- Make illegal states unrepresentable: model state with sealed types and value classes, not nullable flags and booleans.
- Depend on abstractions at layer boundaries; inject everything that has side effects (dispatchers, clocks, random, I/O).

### Code style
- Names express intent: `ObserveCartTotalUseCase`, not `CartHelper`.
- Functions do one thing, usually under 20 lines, at most 3 levels of nesting, early returns.
- Maximum 4 parameters; otherwise introduce a parameter object.
- No magic values; use named constants or resources.
- Comments explain why, not what. No commented-out code.
- `TODO` requires a ticket: `// TODO(PROJ-123): ...`.
- KDoc on all public APIs of `core/*` modules.

### Kotlin practices
- `val` over `var`; immutable collections in public APIs.
- `data class` for models, `value class` for IDs and units, `sealed interface` for closed hierarchies.
- Exhaustive `when` on sealed types, with no catch-all `else` that hides new cases.
- Extension functions for mappers (`toDomain()`, `toEntity()`), one mapper file per boundary.
- Never swallow `CancellationException`; rethrow it in any broad `catch`.
- Expose failures as typed results (`sealed interface Outcome<out T>`) across layers rather than throwing.

---

## 4. Architecture Patterns

### Presentation (MVI)
```kotlin
@Immutable
data class CartUiState(
    val isLoading: Boolean = false,
    val items: List<CartItemUi> = emptyList(),
    val error: UiText? = null,
)

sealed interface CartEvent {
    data object Refresh : CartEvent
    data class RemoveItem(val id: ItemId) : CartEvent
}

sealed interface CartEffect {
    data class ShowMessage(val text: UiText) : CartEffect
}
```

- One `StateFlow<UiState>` per screen. One-shot effects through a `Channel` exposed as `Flow`.
- ViewModels depend on use cases only. No `Context`, `Resources`, or views inside ViewModels; use `UiText`.
- Composables are stateless: `Screen(state, onEvent)`. Hoist state. Provide `@Preview`s for key states.
- Collect with `collectAsStateWithLifecycle()`.
- Use stable keys in lazy lists.

### Domain
- A use case does one thing, exposed as `operator fun invoke`.
- Business rules live here, not in ViewModels or repositories.
- No serialization or Room annotations on domain models.

### Data
- Single source of truth: Room. The UI observes the database; network results write to it.
- Repositories map DTO/entity to domain at the boundary. DTOs never leave `data`.
- Handle offline, timeout, and server errors explicitly and map them to domain errors.

### Concurrency
- Inject `DispatcherProvider`; never hardcode `Dispatchers.IO` in testable classes.
- Structured concurrency only. Scopes are owned by a lifecycle (`viewModelScope`, `lifecycleScope` with `repeatOnLifecycle`).
- Main-safe suspend functions: callers should not need to switch dispatchers.

### Navigation
- Type-safe Navigation Compose. Each feature exposes a navigation entry point; internal screens stay internal.

---

## 5. Testing Standards

Tests verify behaviour. Coverage numbers are a safety net, not the goal.

### Test pyramid and tools

| Level | Where | Tools |
|---|---|---|
| Unit | `src/test` | JUnit 5, MockK, Turbine, kotlinx-coroutines-test, Truth or AssertK |
| Integration (JVM) | `src/test` | Robolectric, MockWebServer, in-memory Room |
| Integration (device) | `src/androidTest` | AndroidX Test, Room `MigrationTestHelper`, Hilt testing |
| UI | `src/androidTest` (and Robolectric where fast enough) | Compose UI test (`createComposeRule`), Hilt test rules |

Note: instrumented tests use JUnit 4 runners. Do not assume JUnit 5 in `androidTest`.

### Coverage thresholds (enforced in CI)

| Scope | Line | Branch |
|---|---|---|
| `domain` | 90% | 85% |
| `data` | 85% | 80% |
| ViewModels | 85% | 80% |
| `core/*` | 80% | 75% |
| Project overall | 80% | 75% |

Coverage must not decrease in any module you touch. Excluded from coverage: generated code (Hilt, Room, serialization), `@Preview` functions, and pure DI modules.

### Unit tests
- Naming: behaviour as a sentence, for example `` `emits error state when refresh fails` ``.
- Arrange / Act / Assert, separated by blank lines.
- Use `runTest` with an injected `TestDispatcher`. Test ViewModels by asserting the sequence of `UiState`s with Turbine.
- Prefer hand-written fakes over mocks for repositories; use MockK for the rest.
- Cover happy path, error path, empty input, boundaries, and cancellation.
- Tests are deterministic: no real clocks, network, or randomness.
- Bug fixes begin with a failing test that reproduces the bug.

### Integration tests
- **DAOs:** real Room against an in-memory database; every query is exercised.
- **Migrations:** every schema migration tested with `MigrationTestHelper` using exported schemas.
- **Repositories:** real data sources over MockWebServer + in-memory Room, verifying network to database to domain end to end, including error and offline paths.
- **API contracts:** parse recorded JSON fixtures from `src/test/resources`, including unknown fields and missing optional fields.
- **DataStore / Keystore wrappers:** verify read, write, and corruption handling.

### UI tests
- Select nodes by `testTag` or semantics, not by changeable display text.
- Screen-level tests render a composable with fixed `UiState` and assert on output and emitted events.
- Flow-level tests (for example sign-in to home) run with `HiltAndroidTest` and replace network and database bindings with test modules.
- Verify accessibility semantics (content descriptions, roles, touch target size) in screen tests.
- Run on the minSdk API level and the targetSdk API level. Keep flaky tests out of the main suite: fix or quarantine with a ticket.
- Use screenshot tests (Paparazzi, Roborazzi, or the AGP screenshot testing plugin) for design-system components if the project has them set up.

### Definition of done for tests
- [ ] Unit tests for all new use cases, mappers, and ViewModel branches
- [ ] Integration tests for every touched repository, DAO, or API contract
- [ ] UI test for every new screen's main path and one error path
- [ ] All tests pass locally and in CI; no skipped tests without a ticket
- [ ] Coverage thresholds met

---

## 6. Security Standards

Baseline: OWASP MASVS Level 1; Level 2 and the OWASP MASTG test cases for anything handling payments, identity, health, or other regulated data. Follow GDPR/UK GDPR principles for personal data (minimisation, purpose limitation, retention).

### Storage
- No tokens, secrets, or personal data in plaintext (preferences, files, logs, cache, crash reports).
- Use Android Keystore-backed keys. For encrypting stored values use an AEAD approach (for example Tink with a Keystore master key). Do not add new uses of the deprecated Jetpack Security `EncryptedSharedPreferences`/`EncryptedFile`.
- Encrypt Room databases that hold sensitive data (for example SQLCipher), with the key protected by the Keystore.
- Keep sensitive data off external storage and out of backups: `allowBackup="false"` or explicit `dataExtractionRules`/`fullBackupContent` exclusions.
- Clear sensitive data on logout. Mark sensitive screens with `FLAG_SECURE` where appropriate.

### Network
- HTTPS only. `usesCleartextTraffic="false"` and a reviewed `network_security_config.xml`.
- Certificate pinning for first-party API hosts, with a backup pin and a documented rotation procedure.
- Never install a trust-all `TrustManager` or disable hostname verification, in any build type.
- Timeouts on every call. Retry only idempotent requests, with backoff.
- Do not log request or response bodies or headers containing credentials.

### Authentication and authorization
- OAuth 2.0 / OIDC with PKCE via a vetted library (for example AppAuth). No custom crypto or custom auth protocols.
- Use Credential Manager for sign-in and `BiometricPrompt` (with crypto-bound keys for sensitive actions).
- Short-lived access tokens; refresh tokens stored encrypted. Authorization decisions are enforced on the server; the client is never the sole control.

### Secrets and configuration
- No secrets in source, `BuildConfig`, resources, or the repo history. Inject via CI secret store. Treat anything shipped in the APK as public.
- Signing keys and keystores are never committed. Use Play App Signing.

### Component and input hardening
- Components default to `android:exported="false"`; export only when required, with permissions or signature checks.
- Use explicit intents, and immutable `PendingIntent`s (`FLAG_IMMUTABLE`).
- Validate and sanitise all external input: intent extras, deep links, App Links, clipboard, file content. Prefer verified App Links over custom URL schemes.
- Share files via `FileProvider`. Avoid `file://` URIs and broad storage permissions.
- WebView: JavaScript off by default, no `addJavascriptInterface` for untrusted content, restrict navigation to an allowlist, disable file access.
- Room and any SQL use parameterised queries only.
- Request the minimum permissions, at the moment they are needed.

### Release hardening
- R8 minification and resource shrinking on in release; `debuggable=false`.
- Consider Play Integrity API for flows where device or app integrity matters, verified server-side.
- Strip logging in release (Timber tree that does nothing, or R8 log removal).

### Supply chain
- New dependencies need justification: purpose, maintenance activity, license, and known vulnerabilities.
- Pin versions in the catalog. Enable Dependabot or Renovate and a vulnerability scan in CI.
- Prefer AndroidX, Google, and Kotlin-team libraries where they fit.

### Secure coding checks
- `SecureRandom` for security-sensitive values; no MD5/SHA-1 for security; no ECB mode; no hardcoded IVs or keys.
- Secrets in memory: avoid keeping them in `String` longer than needed where practical.
- Run static security checks (detekt security rules, Android Lint security checks) and fix findings; do not suppress without a comment and a reviewer.

---

## 7. Performance, Accessibility, Localization

**Performance**
- Nothing blocking on the main thread; StrictMode enabled in debug.
- Measure before optimising: Macrobenchmark for startup and scrolling, Perfetto/Layout Inspector for investigation.
- Compose: stable or `@Immutable` state, `remember`/`derivedStateOf` where profiling justifies it, lambda stability, no heavy work in composition.
- Ship Baseline Profiles for startup and critical journeys.
- Single image-loading library (Coil) with sensible caching and downsampling.
- No `Context` or `Activity` references retained in singletons or ViewModels.

**Accessibility**
- Content descriptions for meaningful icons and images; labels for all inputs.
- Touch targets at least 48dp; support font scale up to 200%.
- Meaning is never conveyed by colour alone; meet WCAG AA contrast.
- Verify with TalkBack and the Accessibility Scanner.

**Localization**
- All user-visible strings in resources; no hardcoded text. Use plurals and format arguments, never string concatenation.
- Support RTL layouts. Format dates, numbers, and currency with locale-aware APIs.

---

## 8. Git, Review, and CI

- Branches: `feature/<ticket>-short-name`, `fix/<ticket>-short-name`.
- Commits follow Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).
- Keep PRs small (about 400 changed lines or fewer) and single-purpose.
- A PR description contains: what and why, test evidence, screenshots or recordings for UI changes, security or privacy impact, and new dependencies with justification.
- CI must pass before merge: build, ktlint, detekt, lint, unit tests, instrumented tests, coverage gate, vulnerability scan.
- Never force-push shared branches. Do not commit build outputs or IDE files.

---

## 9. How the Agent Should Work

1. **Understand first.** Read the relevant module, existing tests, and similar features. Match established patterns that comply with this file.
2. **Plan briefly for non-trivial work.** State the approach and any assumption in a few lines before large edits.
3. **Make the smallest correct change.** Report unrelated problems you notice instead of fixing them silently.
4. **Write the test with the code.** For a bug, write the failing test first.
5. **Verify.** Run the commands in section 0 for what you changed.
6. **Report honestly.** Your final summary lists: what changed and why, which commands you ran and their results, what you could not run, known gaps, and any decisions that need human review.
7. **Protect sensitive data.** Never print or commit secrets, tokens, or personal data, including in test fixtures. Use obviously fake values.

---

## 10. Final Checklist

- [ ] Architecture and dependency rules respected (section 2)
- [ ] Clean code and Kotlin rules followed (section 3)
- [ ] MVI/UDF patterns and coroutine rules followed (section 4)
- [ ] Unit, integration, and UI tests added and passing; coverage thresholds met (section 5)
- [ ] No security rule violated; no new secrets, cleartext traffic, or PII in logs (section 6)
- [ ] Accessibility and localization checked for UI changes (section 7)
- [ ] ktlint, detekt, and Android Lint clean
- [ ] PR description complete (section 8)
- [ ] Summary states what was verified and what was not (section 9)
