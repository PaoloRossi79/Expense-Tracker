# Expense Tracker App — CI/CD & Release Pipeline

Repository hosted on GitHub. Two GitHub Actions workflows: a **build/test
pipeline** on every push/PR, and a **release pipeline** that gets an APK to
the family without publishing to the Play Store.

## 1. Build pipeline (`.github/workflows/build.yml`)

**Trigger:** every push and pull request.

**Steps:**
1. Checkout code
2. Set up JDK (17) and the Android SDK
3. Restore Gradle cache
4. Start the **Firebase Emulator Suite** (Firestore) as a background
   service — needed for integration tests
5. Run **lint/static analysis** (ktlint/detekt) — fail fast on style/bug
   patterns before burning time on tests
6. Run **unit tests** (`./gradlew test`)
7. Run **integration tests** against the running emulator
   (`./gradlew connectedCheck` or a dedicated integration source set,
   pointed at `localhost` emulator ports)
8. Run **UI tests** — either:
   - on a GitHub Actions-hosted Android emulator (`reactivecircus/
     android-emulator-runner` action is the common choice), or
   - via **Firebase Test Lab**, if you want tests on real device
     configurations rather than a CI-hosted emulator
9. Fail the whole workflow if any step fails
10. Upload test reports as workflow artifacts for inspection

**Secrets needed in the GitHub repo:** `GOOGLE_SERVICES_JSON_BASE64`
(contents of `google-services.json`, written to a file at build time) —
never commit this file directly. Full secret list and how to create each
one: `00-prerequisites.md` §9.

## 2. Release pipeline (`.github/workflows/release.yml`)

**Trigger:** your choice from requirements doc open question #5 —
recommended default: **on pushing a version tag** (e.g. `v1.0.0`), since it
gives you an explicit, deliberate release moment rather than releasing on
every merge.

**Steps:**
1. Checkout code
2. Set up JDK/Android SDK, restore cache
3. Run the full test suite again (don't skip this on a release build —
   a tag can be pushed from any commit)
4. Build a **signed release APK** (signing key stored as a GitHub Actions
   secret — `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
   `KEY_PASSWORD`)
5. Distribute the APK — pick one:

### Option A — Firebase App Distribution (recommended)
- Use the `wzieba/Firebase-Distribution-Github-Action` step with
  `appId`, `serviceCredentialsFileContent` (a service account JSON with
  the **Firebase App Distribution Admin** role), `file` (the APK path) and
  `groups` (the tester group alias). The older `token`/`firebase login:ci`
  option is deprecated, so don't use it.
- Configure a **tester group** in Firebase App Distribution containing the
  family's email addresses
- On release, the Action uploads the APK and Firebase **automatically
  emails each tester a secure, expiring download link** — this is exactly
  "send an email with a secure link to download the APK," built in, with no
  custom email-sending code to write or maintain
- Testers install the **Firebase App Tester** app once (or get a direct
  install link) to receive and install future releases

### Option B — Signed URL + custom email step (if you'd rather not depend on Firebase App Distribution)
1. Upload the APK to a storage bucket (Firebase Storage or an S3-compatible
   bucket) as part of the workflow
2. Generate a **time-limited signed URL** for that upload (short expiry —
   e.g. 7 days)
3. Send an email containing that link via an email-API step (e.g.
   SendGrid, Resend, or raw SMTP) — recipient list from a GitHub Actions
   secret or repo variable (requirements doc open question #6)
4. **Do not** attach the raw APK to the email — large attachments get
   stripped or blocked by many mail providers, and a link can be revoked/
   expired; an attachment can't be

**Recommendation:** start with Option A. It solves exactly the problem
described (secure link via email) with an existing, maintained tool rather
than a bespoke email-sending step to build and keep working.

## 3. What's explicitly NOT in scope
- No Play Store listing, no Play Console release track, no public
  distribution — this is confirmed as intentionally out for now.
- No auto-update mechanism beyond "testers get a new email each release" —
  Firebase App Distribution's tester app does handle update notifications
  if Option A is used; Option B does not provide this and users would need
  to notice a new email each time.
