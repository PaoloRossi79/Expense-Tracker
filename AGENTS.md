# AGENTS.md

Instructions for AI coding agents working in this Android repository. Read this file fully before making changes. When these rules conflict with a quick fix, follow the rules and explain the trade-off in your summary.

---

## 1. Project Overview

- **Platform:** Native Android, Kotlin only (no Java in new code).
- **Min SDK / Target SDK:** defined in `gradle/libs.versions.toml`. Do not lower minSdk without approval.
- **UI:** Jetpack Compose with Material 3. XML layouts are legacy; do not add new ones.
- **Architecture:** Clean Architecture with feature modules, unidirectional data flow (UDF), MVI-style ViewModels.
- **DI:** Hilt (or Koin if the project already uses it; do not mix).
- **Async:** Kotlin Coroutines and Flow. No RxJava in new code. No `GlobalScope`.
- **Persistence:** Room for structured data, DataStore for preferences. No SharedPreferences in new code.
- **Networking:** Retrofit + OkHttp + kotlinx.serialization (or Moshi, per the project's existing choice).

If something here contradicts the actual codebase, ask before "fixing" existing code to match these rules. Apply these rules to all new code.

---

## 2. Build, Run, and Verify Commands

Run these before declaring any task complete:

```bash
./gradlew clean build                      # compile all modules, run lint
./gradlew test                             # unit tests (JVM)
./gradlew connectedAndroidTest             # instrumented + UI tests (needs emulator/device)
./gradlew detekt ktlintCheck               # static analysis and formatting
./gradlew jacocoTestCoverageVerification   # enforce coverage thresholds
./gradlew lintRelease                      # Android Lint on release variant
```

- Never mark a task done if any of these fail for code you touched.
- Do not disable lint rules, detekt rules, or tests to make a build pass. If a rule is a false positive, add a narrowly scoped suppression with a comment explaining why.
- Do not edit `gradle/libs.versions.toml` or add dependencies without stating the reason in your summary.

---

## 3. Module Structure and Dependency Rules

```
app/                      -> composition root, navigation host, DI wiring
core/
  common/                 -> Result types, dispatchers, extensions (no Android deps where possible)
  designsystem/           -> theme, reusable Compose components
  network/                -> HTTP client setup, interceptors, error mapping
  database/               -> Room database, DAOs, migrations
  testing/                -> shared test fixtures, rules, fakes
feature/<name>/
  data/                   -> repository impls, data sources, DTOs, mappers
  domain/                 -> entities, use cases, repository interfaces (pure Kotlin)
  presentation/           -> ViewModels, UI state, UI events, Compose screens
```

**Dependency rules (enforced; violations fail review):**

1. `domain` depends on nothing Android-specific. Pure Kotlin/JVM only.
2. `data` depends on `domain`. It never depends on `presentation`.
3. `presentation` depends on `domain`. It never touches `data` classes directly.
4. Features do not depend on other features. Share through `core/*` or through navigation contracts.
5. Only `app` knows every module.
6. Use a dependency-rule check (for example, a Gradle task or a Konsist test) to enforce these boundaries in CI.

---

## 4. Clean Code Standards

### Naming and structure
- Classes and functions read as intent: `GetOrdersForUserUseCase`, not `Helper` or `Manager`.
- One public top-level class per file. File name matches the class.
- Functions do one thing. Aim for under 20 lines; split when a function needs a comment to explain a block.
- Maximum 3 levels of nesting. Use early returns (`?: return`, `if (x == null) return`).
- Prefer `val` over `var`. Prefer immutable collections exposed from public APIs.
- No magic numbers or strings. Use named constants or resources.
- No commented-out code. Delete it; git keeps history.
- No `!!` in production code. Use safe calls, `requireNotNull` with a message, or model the absence in the type system.
- No `TODO` without a ticket reference, for example `// TODO(PROJ-123): ...`.

### Kotlin idioms
- Use `sealed interface` / `sealed class` for state and results, not nullable flags.
- Use `data class` for models, `value class` for type-safe IDs and units.
- Use `when` exhaustively on sealed types. Do not add `else` branches that hide new cases.
- Use extension functions for mapping (`toDomain()`, `toEntity()`), kept in the mapper file for that layer.
- Prefer `Result`-style wrappers from `core/common` over throwing across layer boundaries.

### Formatting and static analysis
- Formatting: ktlint with the official Kotlin code style. Run `ktlintFormat` before committing.
- Static analysis: detekt with the project's `detekt.yml`. Zero new issues allowed.
- Lint: Android Lint must pass with no new warnings in changed files.

---

## 5. Architecture Patterns

### Presentation layer (MVI-style)
```kotlin
data class OrdersUiState(
    val isLoading: Boolean = false,
    val orders: List<OrderUi> = emptyList(),
    val error: UiText? = null,
)

sealed interface OrdersEvent {
    data object Refresh : OrdersEvent
    data class OrderClicked(val id: OrderId) : OrdersEvent
}

sealed interface OrdersEffect {  // one-shot side effects (navigation, snackbars)
    data class NavigateToDetail(val id: OrderId) : OrdersEffect
}
```

- ViewModels expose a single `StateFlow<UiState>` and a `Channel`/`SharedFlow` for effects.
- ViewModels take `UseCase`s, never repositories or data sources.
- ViewModels contain no `Context`, `View`, or Android framework references. Use `UiText` for localized strings.
- Compose screens are stateless: they take `state` and `onEvent` parameters. Hoist state.
- Collect flows with `collectAsStateWithLifecycle()`, never `collectAsState()`.

### Domain layer
- Use cases are single-purpose: one `operator fun invoke`.
- Repository interfaces live in `domain`. Implementations live in `data`.
- Domain models are not annotated with serialization or Room annotations.

### Data layer
- Repositories map DTOs and entities to domain models at the boundary.
- Single source of truth: Room is the cache; the UI observes the database, and network refreshes write into it.
- Expose `Flow` for reactive data. Use `suspend` for one-shot operations.

### Coroutines
- Inject `CoroutineDispatcher`s. Never hardcode `Dispatchers.IO` in classes under test.
- Use `viewModelScope` in ViewModels, `repeatOnLifecycle` in UI.
- Never swallow `CancellationException`. Rethrow it in every `catch`.
- Structured concurrency only. No fire-and-forget scopes.

### Navigation
- Type-safe navigation with the Navigation Compose APIs. Routes are defined in the feature's `presentation` or a shared contract module.
- Features expose entry points, not internal screens.

---

## 6. Testing Requirements

Coverage is a floor, not a goal. Tests must assert behaviour, not implementation details.

### Coverage targets (enforced by Jacoco in CI)
| Layer | Line coverage | Branch coverage |
|---|---|---|
| `domain` (use cases, models) | >= 90% | >= 85% |
| `data` (repositories, mappers) | >= 85% | >= 80% |
| `presentation` (ViewModels) | >= 85% | >= 80% |
| `core/*` | >= 80% | >= 75% |
| Overall | >= 80% | >= 75% |

Do not write tests solely to hit numbers. Coverage shows what ran, not what was verified.

### Unit tests (`src/test`, JVM)
- Frameworks: JUnit 5, MockK, Turbine (for Flow), kotlinx-coroutines-test.
- Name tests as sentences: `fun 'returns empty list when repository fails'()` (backtick names are allowed in tests).
- Structure: Arrange / Act / Assert, with blank lines between sections.
- Use `StandardTestDispatcher` / `runTest`. Never `Thread.sleep`.
- Test ViewModels by asserting the sequence of emitted `UiState`s with Turbine.
- Prefer fakes over mocks for repositories. Use MockK for collaborators where a fake is impractical.
- One logical assertion per test where practical.
- Cover: happy path, error path, empty state, boundary values, and cancellation.

### Integration tests (`src/androidTest` or `src/test` with Robolectric)
- Room DAOs: test against an in-memory database. Cover every query and every migration (use `MigrationTestHelper`).
- Repositories: wire real data sources against a fake HTTP server (MockWebServer) and an in-memory Room database. Verify the full path from network response to domain model.
- Retrofit services: contract tests against recorded JSON fixtures in `src/test/resources`.
- DataStore: test read/write round trips with a temporary file.

### UI tests (`src/androidTest`)
- Compose UI tests with `createComposeRule()`. Use `testTag` for stable selectors; do not match on display text alone when it can change.
- Test the screen with a fake ViewModel or a fake state, not the real network.
- End-to-end flows (login, purchase, navigation across screens) use a `HiltTestApplication` with test modules replacing network and database bindings.
- Use Espresso only for legacy View-based code; do not introduce new Espresso tests for Compose.
- Run UI tests on at least one API level at the project's minSdk and one at targetSdk.
- Consider Maestro or similar for black-box flow tests in CI, if the project has the infrastructure.

### Test doubles and fixtures
- Shared fixtures live in `core/testing`. Build fixtures with builder functions, for example `anOrder(id = ..., status = ...)`.
- Do not share mutable state between tests. Reset rules run in `@Before`/`@After`.
- Keep test data realistic. Use the same value shapes as production.

### Definition of done for a feature
- [ ] Unit tests for every use case and ViewModel branch
- [ ] Integration test for every repository and DAO touched
- [ ] UI test for each new screen's primary interaction
- [ ] No drop in coverage for touched modules
- [ ] `./gradlew test connectedAndroidTest detekt ktlintCheck` passes

---

## 7. Security Standards

Follow OWASP MASVS (Level 1 minimum, Level 2 for apps handling payments, health, or identity data) and the Android security guidance.

### Data at rest
- Never store secrets, tokens, or PII in plaintext SharedPreferences, files, or logs.
- Use Android Keystore-backed encryption for sensitive values (for example, `EncryptedDataStore` or a Keystore-wrapped key).
- Room databases holding sensitive data must be encrypted (SQLCipher or equivalent) with the key held in the Keystore.
- Do not write sensitive data to external storage or the clipboard without user action and an expiry.

### Data in transit
- HTTPS only. Set `android:usesCleartextTraffic="false"` and use a `network_security_config.xml`.
- Enable certificate pinning for the production API domain, with a documented backup pin and rotation plan.
- Never disable hostname verification or trust all certificates, including in debug builds.

### Authentication and secrets
- Tokens: store in the Keystore-backed store. Refresh tokens must not be stored alongside access tokens in plaintext.
- No API keys, client secrets, or signing keys in source control. Use `local.properties` (git-ignored) or CI secrets injected at build time.
- Use `BuildConfig` only for non-secret configuration.
- Use the platform biometric and credential APIs (`BiometricPrompt`, Credential Manager) for sign-in flows.

### Application hardening
- Release builds: R8 enabled, `isMinifyEnabled = true`, `isShrinkResources = true`. Keep rules minimal and reviewed.
- `android:debuggable="false"` in release. `android:allowBackup="false"` unless backups are designed and reviewed.
- Export only the components that must be exported. Default to `exported="false"`.
- Validate every intent extra and deep-link parameter. Treat all external input as untrusted.
- Use `FileProvider` for file sharing; never expose `file://` URIs.
- WebViews: disable JavaScript unless required; never use `addJavascriptInterface` with untrusted content.
- Enable Play Integrity checks for flows where client trust matters.

### Logging and analytics
- No PII, tokens, or full request/response bodies in logs. Use Timber or a wrapper that strips log output in release.
- Analytics events must not include PII. Document any new event in the analytics catalogue.

### Dependencies
- Every new dependency needs a justification in the PR description: why it is needed, its maintenance status, and its license.
- Enable Dependabot or Renovate. Keep dependencies within one minor version of latest where practical.
- Run dependency vulnerability scanning (for example, OWASP Dependency-Check or GitHub Dependabot alerts) in CI.

### Code-level rules
- Validate all user input at the domain boundary.
- Use parameterised queries in Room (`:param`). Never concatenate strings into SQL.
- Do not use `Random` for security-sensitive values; use `SecureRandom`.
- Do not use `MD5` or `SHA-1` for security purposes.
- Set timeouts on every network call.

---

## 8. Performance and Quality

- No work on the main thread. Use StrictMode in debug builds to catch violations.
- Avoid recomposition: use `remember`, stable types, and `derivedStateOf` where profiling shows need. Do not optimise without measuring.
- Lazy lists use stable keys (`items(key = ...)`).
- Images load through a single image library (Coil), with sizing and caching configured in `core/network` or `core/designsystem`.
- Avoid memory leaks: no static references to `Context` or `Activity`.
- Use Baseline Profiles for startup-critical paths.
- Large changes to hot paths must include a benchmark or a trace comparison.

---

## 9. Accessibility and Localization

- Every interactive element has a content description or a visible label.
- Touch targets are at least 48dp.
- Text scales with system font size; do not use fixed `sp` values that break at 200%.
- Colour is never the only signal for meaning.
- All user-facing strings live in `strings.xml` (or the Compose resource equivalent). No hardcoded text in Kotlin.
- Use plurals and format arguments correctly; never concatenate strings for translation.
- Include TalkBack-navigable semantics. Test with the Accessibility Scanner before merging UI changes.

---

## 10. Git and Pull Request Conventions

- Branch names: `feature/<ticket>-short-description`, `fix/<ticket>-short-description`.
- Commits: Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`). One logical change per commit.
- PRs must include:
  - Summary of what changed and why
  - Screenshots or screen recordings for UI changes
  - Test evidence (what was added, what the coverage delta is)
  - Security impact, if any
  - Any new dependency and its justification
- Keep PRs under about 400 changed lines where possible. Split larger work.
- Do not force-push to shared branches. Do not commit generated files, build outputs, or IDE configs.

---

## 11. Working Agreement for AI Agents

1. **Read before writing.** Inspect the surrounding module, its tests, and existing patterns before adding code. Match the conventions you find, unless they violate this file.
2. **Smallest correct change.** Do not refactor unrelated code in the same change. If you find a problem outside the task, report it rather than fixing it silently.
3. **Tests come with code.** A change without tests is incomplete. A bug fix must include a test that fails before the fix and passes after.
4. **Ask when unsure about architecture.** Module boundaries, new libraries, navigation changes, and data model changes need human sign-off.
5. **Never weaken security or tests to make something pass.** If a check fails, fix the cause.
6. **Never invent APIs.** Verify library APIs against the version in `libs.versions.toml` before using them.
7. **Report honestly.** In your summary, state what you ran, what passed, what you could not run (for example, no emulator available), and any known gaps.
8. **Keep secrets out of output.** Do not print tokens, keys, or personal data in logs, commits, or chat.

---

## 12. Definition of Done (Summary)

A task is complete only when:

- [ ] Code follows the module and dependency rules in section 3
- [ ] Clean code and Kotlin idioms in section 4 are followed
- [ ] Unit, integration, and UI tests exist for the change, and pass
- [ ] Coverage thresholds in section 6 are met for touched modules
- [ ] `detekt`, `ktlintCheck`, and Android Lint pass
- [ ] Security rules in section 7 are respected, with no new secrets, cleartext traffic, or PII in logs
- [ ] Accessibility and localization rules in section 9 are respected for UI changes
- [ ] PR follows section 10 and includes test and security notes
- [ ] Summary to the user states what was verified and what was not
