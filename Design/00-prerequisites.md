# Expense Tracker App — Pre-requisites (manual steps)

Everything **you** have to do by hand before (and alongside) the coding
agent building the app. Do it in order. Console screens change over time,
so button labels may differ slightly from what is written here; the
intent of each step stays the same.

**Cost:** everything below uses free tiers (Firebase **Spark** plan, Firebase
App Distribution, GitHub Free). No credit card should be needed. Check
current Spark quotas at firebase.google.com/pricing; household-scale usage
is far below them. GitHub Free gives a limited number of Actions minutes per
month for **private** repos (Android emulator UI tests use the most); public
repos are unlimited, but don't make this repo public because it will hold
family-related configuration.

**Estimated time:** 2-3 hours, mostly installs.

## 0. Values to write down as you go

Keep this table in a password manager or private note (never in the repo).

| Item | Where it comes from | Your value |
|---|---|---|
| App package name | You choose, §4 | e.g. `com.yourname.expensetracker` |
| Firebase project ID | §3 | |
| Firebase Android App ID | §4 (looks like `1:123...:android:abc...`) | |
| Web client ID | §3.3 | |
| Household document ID | §7 | |
| Keystore file + passwords + alias | §5 | |

## 1. Accounts

1. **Google accounts:** every family member who will use the app needs a
   Google (Gmail) account **signed in on their Android phone**. The app
   identifies people through that account. Note each person's exact email
   address.
2. **GitHub account:** create one at github.com if you don't have it. Turn
   on two-factor authentication (Settings -> Password and authentication).
3. **Firebase / Google Cloud:** uses your Google account. Use your own
   personal Google account as the project owner.

## 2. Install tools on your computer

1. **Git:** git-scm.com/downloads. Then in a terminal:
   `git config --global user.name "Your Name"` and
   `git config --global user.email "you@example.com"`.
2. **JDK 17:** install Temurin 17 (adoptium.net). Android Studio bundles its
   own JDK, but the Firebase emulator and command-line Gradle runs need one
   on your PATH. Check with `java -version`.
3. **Android Studio** (developer.android.com/studio): install, then open
   **More Actions -> SDK Manager** and install the latest stable **Android SDK
   Platform**, **Android SDK Build-Tools**, **Platform-Tools** and
   **Android Emulator**.
4. **Node.js LTS** (nodejs.org), then install the Firebase CLI:
   `npm install -g firebase-tools`. Check with `firebase --version`.
5. **VS Code**, plus (for the Ollama route) **Ollama** and the **Cline**
   extension; model and settings are covered in the earlier Ollama/Cline
   guidance (pull a coding model such as `qwen3-coder:30b` if your hardware
   allows, otherwise a smaller one; in Cline set provider **Ollama**, base
   URL `http://localhost:11434`).

## 3. Firebase project

### 3.1 Create the project
1. Go to console.firebase.google.com and sign in.
2. **Create a project** (or "Add project"). Name it e.g. `family-expense-tracker`.
3. Google Analytics: **turn off** (not needed).
4. Create. Confirm the project is on the **Spark (no-cost)** plan: bottom
   left of the console shows the plan. **Do not upgrade.**
5. Write down the **Project ID** (Project settings, gear icon).

### 3.2 Create the Firestore database
1. **Build -> Firestore Database -> Create database**.
2. Edition: **Standard** if asked. Mode: **Production mode** (the coding
   agent deploys proper security rules in Phase 1).
3. **Location:** pick the closest region for UK users, e.g. `europe-west2`
   (London). **This cannot be changed later.**
4. Enable.

### 3.3 Enable Google sign-in
1. **Build -> Authentication -> Get started**.
2. **Sign-in method** tab -> **Google** -> Enable.
3. Set the public-facing project name and your support email. Save.
4. Open the Google provider again, expand **Web SDK configuration**, and
   copy the **Web client ID**. The app needs it for Credential Manager
   sign-in. Write it down (it is an identifier, not a secret).

## 4. Register the Android app in Firebase

1. Decide your **package name** now, e.g. `com.yourname.expensetracker`. It
   must match the Android Studio project exactly and can't be changed later
   without re-registering.
2. In Firebase: **Project settings (gear) -> General -> Your apps -> Add app
   -> Android**. Enter the package name and a nickname. Leave the SHA field
   for now (or fill in the debug SHA-1 from step 3 below), then register.
3. Get your **debug SHA-1** (needed for Google sign-in to work in debug
   builds). In a terminal:
   - macOS/Linux: `keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android`
   - Windows (PowerShell): same command with `$env:USERPROFILE\.android\debug.keystore`
   - Once the project exists, `./gradlew signingReport` also prints it.
   Copy the **SHA-1** (and SHA-256) lines.
4. Firebase: **Project settings -> Your apps -> your Android app -> Add
   fingerprint**, paste SHA-1, then SHA-256.
5. **Download `google-services.json`** (after adding the fingerprints, so
   it includes the OAuth client entries). Put it in the Android project's
   `app/` folder. **Never commit it** (the `.gitignore` must contain it).
6. Write down the **App ID** shown on the same page.

## 5. Release signing key (for installable APKs)

APKs sent to family phones must be signed with a key you keep. **If you
lose this key you cannot update installed apps** (family would have to
uninstall and reinstall, losing local state), so back it up.

1. Generate it (run once, keep the file outside the repo):
   `keytool -genkeypair -v -keystore expense-tracker-release.jks -alias expensetracker -keyalg RSA -keysize 2048 -validity 10000`
   You'll be asked for a keystore password, key password and your details.
2. Store the `.jks` file and both passwords in a password manager, plus a
   second backup copy somewhere safe (e.g. an encrypted drive).
3. Get its fingerprint:
   `keytool -list -v -keystore expense-tracker-release.jks -alias expensetracker`
4. Add that **SHA-1 and SHA-256** to the Android app in Firebase (same place
   as §4 step 4), then **re-download `google-services.json`** and replace
   the old one.

## 6. Firebase CLI and emulators (local testing)

1. `firebase login` (opens a browser; use your Google account).
2. In the repository folder (after §8), run `firebase init emulators` and
   select **Firestore**; accept default ports; choose to download emulators
   when asked. (The coding agent can also do this in Phase 0/1, but your
   login has to be done by you.)
3. Check: `firebase emulators:start --only firestore` starts and shows the
   emulator ports. Stop it with Ctrl+C.

## 7. Create the first household (bootstrap)

The app's Add Member screen needs an existing member, so the very first
household is created by hand.

1. Firebase console -> **Firestore Database -> Data -> Start collection**.
2. Collection ID: `households`. Document ID: **Auto-ID** (write it down).
3. Add fields:
   - `name` (string): e.g. `Home`
   - `createdAt` (timestamp): now
   - `memberEmails` (array of strings): your Google email as the first
     item, **in lower case**, exactly as it appears on your phone's Google
     account. Add other family emails here too if you like (or add them
     later via the Add Member screen).
4. Save. Note the document ID in the §0 table.

## 8. GitHub repository

1. github.com -> **New repository**. Name e.g. `family-expense-tracker`.
   Visibility: **Private**. Don't add a README/license (the project adds
   its own); do add a Kotlin/Android `.gitignore` if offered.
2. Clone it: `git clone https://github.com/<you>/family-expense-tracker.git`
3. Copy the five docs (`00` to `04`) into a `docs/` folder and commit.
4. Make sure `.gitignore` includes at least: `google-services.json`,
   `*.jks`, `*.keystore`, `local.properties`, `.gradle/`, `build/`.
5. Optional but recommended: Settings -> **Branches** -> add a rule for
   `main` requiring the build workflow to pass before merging.

## 9. Distribution: Firebase App Distribution + GitHub secrets

### 9.1 Set up App Distribution
1. Firebase console -> **Release & Monitor -> App Distribution -> Get started**.
2. **Testers & Groups** tab -> **Add group**, name it `family` (note the
   group **alias**, usually the same in lower case).
3. Add every family member's email to the group.

### 9.2 Create a service account for GitHub Actions
1. Firebase console -> **Project settings -> Service accounts -> Manage
   service account permissions** (opens Google Cloud Console).
2. **Create service account**. Name it `github-actions-distribution`.
3. When asked to grant access, choose the role **Firebase App Distribution
   Admin**. Done.
4. Open the new service account -> **Keys -> Add key -> Create new key ->
   JSON**. A JSON file downloads. Treat it like a password.

### 9.3 Add GitHub Actions secrets
Repository -> **Settings -> Secrets and variables -> Actions -> New
repository secret**. Create each of these:

| Secret name | Value |
|---|---|
| `GOOGLE_SERVICES_JSON_BASE64` | base64 of `google-services.json` |
| `KEYSTORE_BASE64` | base64 of `expense-tracker-release.jks` |
| `KEYSTORE_PASSWORD` | keystore password from §5 |
| `KEY_ALIAS` | `expensetracker` (or the alias you chose) |
| `KEY_PASSWORD` | key password from §5 |
| `FIREBASE_APP_ID` | App ID from §4 |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | full contents of the JSON key from §9.2 |
| `FIREBASE_TESTER_GROUP` | `family` (group alias from §9.1) |

How to get base64 of a file:
- macOS: `base64 -i file | pbcopy`
- Linux: `base64 -w0 file`
- Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("file"))`

After saving the secrets, delete the downloaded service-account JSON from
your Downloads folder (keep it only in your password manager if you need a
copy).

## 10. Family phones and your test device

**Each family phone:**
1. Android **8.0 or newer** (the app's minimum is API 26).
2. Signed in with the Google account whose email is on the household's
   member list.
3. A screen lock set; add a fingerprint or face unlock if available (the
   app uses it to unlock; otherwise it falls back to the device PIN).
4. When the first build email arrives, tap the secure link, accept the
   invite, and install via the **Firebase App Tester** app or the browser.
   If the browser asks, allow **Install unknown apps** for it.
5. When the app asks, **allow notifications**: recurring-payment reminders
   depend on it.
6. For reliable reminders: Settings -> Apps -> Expense Tracker -> Battery ->
   set to **Unrestricted** (wording varies by phone brand).

**Your development device:** either use a physical phone (Developer options
-> enable **USB debugging**), or create an emulator in Android Studio
(**Device Manager -> Create device**) using a system image labelled
**Google Play**, then sign in to a Google account in the emulator. A
Google Play image is required for Google sign-in to work.

## 11. Verification checklist

Tick everything before starting Phase 1:

- [ ] Firebase project exists on the **Spark** plan
- [ ] Firestore created in the right region, production mode
- [ ] Google sign-in enabled; Web client ID recorded
- [ ] Android app registered; debug **and** release SHA-1/SHA-256 added;
      latest `google-services.json` downloaded (not committed)
- [ ] Release keystore created and backed up in two places
- [ ] `firebase --version` works and `firebase login` is done
- [ ] First household document created with your email in `memberEmails`
- [ ] Private GitHub repo created, cloned, `.gitignore` protects secrets
- [ ] App Distribution enabled, `family` group created with all emails
- [ ] Service account created with *Firebase App Distribution Admin*, JSON
      key stored as a GitHub secret, local copy deleted
- [ ] All eight GitHub secrets from §9.3 added
- [ ] Emulator (Google Play image) or physical phone ready
- [ ] Every family member has a Google account on an Android 8+ phone

## 12. Hand these to the coding agent

Tell the agent (in the prompt, not in committed files): the **package
name**, **Firebase project ID**, **Web client ID**, and **household
document ID**. Everything else comes from `google-services.json` and the
docs in `docs/`.

## 13. If something goes wrong

- **Google sign-in fails with "developer error" / code 10:** the SHA-1 of
  the build you're running isn't registered in Firebase (§4/§5), or you
  didn't re-download `google-services.json` after adding it.
- **A family member gets "access blocked" when signing in:** in Google
  Cloud Console -> APIs & Services -> OAuth consent screen (Google Auth
  Platform), either add their email as a **test user** or move the app's
  publishing status to **In production** (basic profile/email scopes don't
  require review).
- **Signed in but "not in a household":** their email isn't in
  `memberEmails` (check spelling and lower case), or they signed in with a
  different Google account than expected.
- **Reminders arrive late or not at all:** notification permission denied,
  or battery optimisation is restricting the app (§10 step 6). The Home
  "Pending recurring" card still shows what's due.
