# Coding Agent Instructions — Expense Tracker App (v4)

You are implementing an Android family expense tracker. Read
`01-requirements-and-specifications.md` and `02-implementation-plan.md` in
this same folder first — they are the source of truth for scope, data
model, and screen behaviour. Read `04-cicd-and-release-pipeline.md` before
Phase 8/9 work, and `00-prerequisites.md` for the manual setup the user does. This file gives you build conventions and task framing.

## Tech stack (fixed — do not substitute)
- Kotlin, Jetpack Compose, Material 3
- Firebase Authentication (**Google Sign-In provider only** — no email/
  password, no email-link), Android **Credential Manager** for account
  selection
- `androidx.biometric.BiometricPrompt` for the local re-entry gate
- Cloud Firestore, offline persistence enabled
- **AndroidX WorkManager** + local notifications (`NotificationCompat`) for
  recurring-transaction reminders. **No Cloud Functions, no Blaze plan** —
  everything runs on Firebase's free Spark tier
- Min SDK 26
- MVVM: Compose UI → ViewModel (StateFlow) → Repository → Firestore

## Project structure
```
app/
  data/
    model/          // Transaction, Category, Vendor, RecurringRule, Household
    repository/      // one repository per collection, wraps Firestore calls
    auth/            // AuthRepository (Google Sign-In + Firebase Auth + biometric gate)
  ui/
    home/            // monthly totals + bottom nav
    addtransaction/  // add expense / add income screens (shared form, vendor auto-match, different category source)
    transactions/    // monthly list, filters, sort, edit/delete
    categories/       // CRUD screen
    vendors/          // CRUD screen, default-category mapping
    recurring/         // rules CRUD screen, pending-confirmation card, notification workers/actions
    members/           // Add Member screen + roster view
    common/          // shared composables (amount field, date picker, category picker w/ inline create)
  navigation/
```

## Firestore rules — implement exactly this access pattern
- A user may read/write under `households/{householdId}/**` only if their
  authenticated email is in `households/{householdId}.memberEmails`.
- A user may **update or delete** a transaction document only if
  `request.auth.uid == resource.data.createdByUid`. This applies even to
  other household members who can otherwise read the document — read access
  and write/delete access are different rules.
- Category, vendor, and recurring-rule writes: open to any household member
  unless open question #2 in the requirements doc resolves to a stricter
  role — check before building Phase 2.
- `memberEmails`: **append-only** for any existing member — reject any
  update that removes an existing entry (there's no "remove member" feature
  yet, so the rule should actively prevent it, not just rely on no UI
  existing for it).
- Write the rules file and integration tests (against the emulator) in the
  same commit — same-household read/write allowed, cross-household denied,
  non-member denied, creator-only edit/delete enforced, category-type
  mismatch rejected if that constraint is mirrored in rules, `memberEmails`
  append-only enforced.

## Auth flow detail
1. App launch → check for an existing Firebase Auth session.
2. No session → launch Credential Manager's `GetCredentialRequest` for
   Google ID tokens → exchange with Firebase Auth (`GoogleAuthProvider`).
3. On success → look up the signed-in email against the household's
   `memberEmails`. Not a member → show a clear "not in a household" state,
   do not proceed to Home.
4. Existing session on relaunch → **do not** re-run Credential Manager;
   instead show `BiometricPrompt` (with `DEVICE_CREDENTIAL` fallback if no
   biometrics enrolled) to re-confirm the user before showing Home.

## Vendor matching (Phase 3)
- Normalise the typed vendor string (uppercase, strip trailing store
  numbers/punctuation) before exact match; fall back to a "contains"
  substring match against each vendor's `matchAliases`.
- Keep this in **one isolated, unit-testable function** — do not scatter
  matching logic across the Add Expense / Add Money In composables.
- On no match, surface the "save as new vendor?" prompt rather than
  silently leaving the category blank.

## Recurring transactions (Phase 5) — notification-driven, client-side
- Rules are defined by the user on the Recurring Rules screen (reason,
  amount, day of month, category, optional vendor, start/end, active).
- Next-due-date calculation is a **pure, unit-testable function**: monthly
  by day-of-month, clamped to the last day of shorter months (day 31 in
  February -> 28/29), honouring start date, end date and paused state.
- An occurrence is **pending** when its due date has passed/arrived and
  `recurringRules/{ruleId}/occurrences/{yyyyMMdd}` does not exist.
- A daily `PeriodicWorkRequest` (WorkManager) runs on each device, finds
  pending occurrences, and posts a local notification at the user-configured
  time (default 09:00). Re-notify daily until resolved. Treat the timing as
  approximate (Android may delay background work) — the Home "Pending
  recurring" card is the reliable fallback, so never depend on the
  notification alone.
- Notification actions: **Confirm**, **Edit & confirm** (opens the prefilled
  Add form), **Skip**.
- **Confirm** writes the transaction with the deterministic ID
  `rec_{ruleId}_{yyyyMMdd}` and the `occurrences` doc in one batched write.
  Rules make both **create-only**, so a concurrent confirm from another
  device fails harmlessly: treat "already exists" as success and just
  dismiss the notification. Never generate random IDs for recurring posts.
- The transaction is attributed to the **confirming user**
  (`createdByUid`), dated on the due date, with `isRecurringInstance: true`.
- Request `POST_NOTIFICATIONS` (Android 13+) with a short rationale; if
  denied, the app must still work via the pending card.
- Do not build any server-side posting job.

## Conventions
- All amounts stored as minor units (pence) as integers in Firestore, to
  avoid floating-point rounding; format to £ in the UI layer only.
- Dates stored as Firestore `Timestamp`, UTC; display in device local time.
- Every transaction write includes `createdByUid`, `createdByName`, and a
  server `updatedAt` timestamp.
- Category `type` (`expense`/`income`) is set once at creation; do not build
  a "change category type" path — if a type change is ever needed, that's a
  delete-and-recreate, not an edit, given existing transactions reference it.
- Category, vendor, and recurring-rule data is plain Firestore data — no
  hardcoded lists in code.
- Keep filter/sort logic, totals-calculation logic, vendor-matching, and
  recurring due-date calculation in isolated, unit-testable functions — do
  not scatter this logic across UI composables.

## Testing — mandatory, per phase, not optional polish
Every phase in the implementation plan must ship with:
- **Unit tests** for any non-trivial logic added that phase (totals calc,
  filter/sort, amount formatting, category-type validation, vendor
  matching, recurring due-date calculation, pending detection)
- **Integration tests** against the **Firestore emulator** for that
  phase's repository/data-access code, including the relevant security-rules cases
- **UI (Compose) tests** for that phase's user-facing flows, including the
  negative case where relevant (e.g. edit/delete controls not available on
  another user's transaction)

Recommended, layer in once the mandatory three are solid:
- Repository contract tests
- Instrumented end-to-end test: sign in → add → confirm sync to a second
  simulated client
- Screenshot/regression tests on the Home totals and transaction list
- ktlint/detekt wired into CI (see Phase 8)

## Definition of done, per phase
- Code compiles, screen(s) for that phase are reachable from navigation
- Unit tests written for that phase's logic, and passing
- Integration tests (Firestore emulator) written for that phase's
  data access and relevant security rules, and passing
- UI tests written for that phase's user flows, and passing
- Firestore reads/writes verified against the emulator or a real test
  household (not just UI stubs)
- No secrets (`google-services.json`, signing keys, SMTP/API credentials)
  committed — confirm `.gitignore` and GitHub Actions secrets are used
  correctly
- A short note back to the user (Paolo) listing what was built, what was
  tested, and any assumptions made on open/undecided requirements
