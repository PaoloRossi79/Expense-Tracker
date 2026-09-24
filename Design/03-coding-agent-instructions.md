# Coding Agent Instructions — Expense Tracker App

You are implementing an Android family expense tracker. Read
`01-requirements-and-specifications.md` and `02-implementation-plan.md` in
this same folder first — they are the source of truth for scope and data
model. This file gives you build conventions and task framing.

## Tech stack (fixed — do not substitute)
- Kotlin, Jetpack Compose, Material 3
- Firebase Authentication, Cloud Firestore
- Firestore offline persistence enabled
- Min SDK 26 (for `BiometricPrompt`)
- MVVM: Compose UI → ViewModel (StateFlow) → Repository → Firestore

## Project structure
```
app/
  data/
    model/          // Transaction, Category, Vendor, RecurringRule, Household, Member
    repository/      // one repository per collection, wraps Firestore calls
    auth/            // AuthRepository (Firebase Auth + biometric gate)
  ui/
    home/            // transaction list + quick summary
    addtransaction/  // add expense / add income screens
    categories/
    vendors/
    recurring/
    summary/
    common/          // shared composables (amount field, date picker, etc.)
  navigation/
```

## Firestore rules — implement exactly this access pattern
A user may read/write anything under `households/{householdId}/**` **only
if** their uid is present in `households/{householdId}.memberIds`. Household
membership changes (invite/remove) must go through a path that lets an Admin
update `memberIds` for their own household only. Write the rules file and a
short set of manual test cases (as comments) showing: same-household read
allowed, other-household read denied, non-member write denied.

## Work in phases, in order
Follow `02-implementation-plan.md` phase by phase. **Do not start a phase
until the previous one builds and runs.** After each phase:
1. Confirm it compiles and the relevant screen is navigable
2. Summarise what was built and any deviations from spec
3. Flag anything from the "Open Questions" list (§7 in the requirements doc)
   that you had to guess at, and state the assumption you made

## Conventions
- All amounts stored as minor units (pence) as integers in Firestore, to
  avoid floating-point rounding; format to £ in the UI layer only.
- Dates stored as Firestore `Timestamp`, always UTC; display in device local
  time.
- Every write includes `addedByUid` and `updatedAt` (server timestamp).
- No hardcoded category/vendor lists in code — categories and vendors are
  household data, seeded (if at all) via a one-time setup script, not
  baked into the app.
- Vendor matching: normalise (uppercase, strip trailing store numbers/
  punctuation) before exact match; fall back to "contains" substring match
  against `vendor.matchAliases`. Keep this logic in one isolated,
  unit-testable function — do not scatter matching logic across UI code.
- **Automated tests are a required deliverable per phase, not optional
  polish.** Minimum bar for every phase:
  - Unit tests for all non-trivial logic added in that phase (vendor
    matching, recurring-rule due-date calculation, summary/aggregation math,
    amount formatting/rounding, and any new business logic as it's added)
  - Integration tests against the **Firestore emulator** for that phase's
    repository/data-access code, including the security-rules test cases
    described above (same-household allowed, cross-household denied,
    non-member write denied, creator-only edit/delete enforced)

  Add as the app grows (don't block early phases on these, but plan for
  them):
  - Compose UI tests for core flows (add expense/income, edit own vs.
    attempt-to-edit others' transaction)
  - Repository contract tests, so the data layer can be refactored safely
  - Instrumented end-to-end test: sign in → add transaction → confirm it
    syncs to a second simulated client
  - Wire up CI (e.g. GitHub Actions) to run unit + integration tests on
    every push once there's a shared repo

  Report test coverage added (not just code) in the per-phase summary back
  to the user.

## Definition of done, per phase
- Code compiles, screen(s) for that phase are reachable from navigation
- Unit tests written for that phase's business logic, and passing
- Integration tests (Firestore emulator) written for that phase's data
  access and security rules, and passing
- Firestore reads/writes verified against the emulator or a real test
  household (not just UI stubs)
- No secrets or `google-services.json` committed if this becomes a shared
  repo — confirm `.gitignore` covers it
- A short note back to the user (Paolo) listing what was built, what was
  tested, and any assumptions made on open/undecided requirements
