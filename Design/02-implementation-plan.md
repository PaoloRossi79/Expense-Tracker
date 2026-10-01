# Expense Tracker App — Implementation Plan (v4)

A phased plan you can work through yourself, or hand phase-by-phase to a
coding agent using `03-coding-agent-instructions.md`. Each phase ends with
tests passing and a commit before the next one starts.

## Phase 0 — Setup (you, ~1–2 hours)
- [ ] Create a Firebase project (console.firebase.google.com)
- [ ] Enable **Authentication** → **Google** sign-in provider
- [ ] Enable **Cloud Firestore** (production mode, not test mode)
- [ ] Create the GitHub repository
- [ ] Create an Android Studio project (Kotlin, Jetpack Compose, min SDK 26+
      for `BiometricPrompt` support)
- [ ] Connect the Android project to Firebase (`google-services.json` —
      **do not commit this file**; confirm `.gitignore` covers it)
- [ ] Install the **Firebase Local Emulator Suite** (Firestore emulator) for
      local integration testing
- [ ] Do every manual step in `00-prerequisites.md` first, then settle the
      open questions in §10 of the requirements doc that affect the data
      model/permissions (at least #2) before Phase 1

## Phase 1 — Foundations
- [ ] Firestore security rules: household-scoped access by `memberEmails`
      (append-only), creator-only edit/delete on transactions, category-type
      integrity
- [ ] Security rules integration tests against the emulator, written
      alongside the rules (same-household allowed, cross-household denied,
      creator-only edit/delete enforced, `memberEmails` append-only enforced)
- [ ] Auth flow: Credential Manager → Google Sign-In → Firebase Auth →
      household-membership check → biometric gate on subsequent app opens
- [ ] Data models (Kotlin data classes) matching the Firestore schema
- [ ] Bottom-bar navigation shell (Home, Add Expense, Add Money In,
      Transactions, Categories) + a "More" menu stub for Recurring Rules,
      Vendors, and Add Member

## Phase 2 — Categories
- [ ] Manage Categories screen: full CRUD, flat list, type (`expense` |
      `income`) fixed at creation, deletion blocked while in use
- [ ] Unit tests: category-type validation logic
- [ ] Integration tests: category CRUD against the emulator, including
      security-rule enforcement
- [ ] UI tests: create/edit/delete category, attempt to delete a
      category in use is blocked with a clear message

## Phase 3 — Vendors & auto-allocation
- [ ] Vendors screen: CRUD, each vendor optionally mapped to a default
      category
- [ ] Vendor-matching function (exact match → normalised/fuzzy match),
      isolated and unit-testable, not scattered in UI code
- [ ] Unit tests: vendor matching (exact, normalised, no-match cases)
- [ ] Integration tests: vendor CRUD against the emulator
- [ ] UI tests: create/edit/delete vendor

## Phase 4 — Core transactions
- [ ] Add Expense screen, including inline "+ New category" quick action
      and vendor auto-match/auto-fill-category behaviour
- [ ] Add Money In screen, including inline "+ New category" quick action
- [ ] Transactions list: monthly view, reverse chronological, category
      indicator per row, month selector
- [ ] Edit (prefilled form) and Delete (with confirmation dialog), both
      restricted to the record's creator — enforced in UI **and** rules
- [ ] Filters (type, category) and sorting (date, amount) on the list
- [ ] Unit tests: amount formatting/rounding, filter/sort logic
- [ ] Integration tests: transaction CRUD against the emulator, including
      creator-only enforcement at the rules level
- [ ] UI tests: add expense (incl. inline category creation and vendor
      auto-match), add income, edit own transaction, delete with
      confirmation, edit/delete controls hidden or disabled on another
      user's transaction, filter and sort the list

## Phase 5 — Recurring transactions (notification-driven)
- [ ] Recurring Rules screen: create/edit/pause/resume/delete (type, reason,
      amount, day of month, category, optional vendor, start/end date)
- [ ] Next-due-date function (monthly, last-day clamping for days 29-31,
      respects start/end date and paused state) — pure and unit-testable
- [ ] `occurrences` handling: pending = due date reached with no
      `occurrences/{yyyyMMdd}` doc; confirmed/skipped writes one
- [ ] Daily background check (WorkManager) on each device that posts local
      notifications for pending occurrences, at the user-configured time
      (default 09:00); re-notify daily until resolved
- [ ] `POST_NOTIFICATIONS` runtime permission request (Android 13+) with a
      short explanation; graceful behaviour if denied (Home pending card
      still works)
- [ ] Notification actions: **Confirm**, **Edit & confirm**, **Skip**
- [ ] Confirm posts a transaction with deterministic ID
      `rec_{ruleId}_{yyyyMMdd}` (create-only, idempotent across devices);
      other devices dismiss their notification when the occurrence syncs
- [ ] Home "Pending recurring" card + confirmation flow
- [ ] Unit tests: next-due-date (each edge case), pending/overdue detection,
      notification scheduling logic
- [ ] Integration tests (Firestore emulator): rule CRUD, create-only
      enforcement for `rec_...` transactions and `occurrences`, two
      simultaneous confirms result in exactly one transaction
- [ ] UI tests: create a rule, Confirm / Edit & confirm / Skip flows, paused
      rule produces no pending item, Home pending card

## Phase 6 — Add Member
- [ ] Add Member screen: email input, validation, append to
      `memberEmails`, roster view of current members
- [ ] Integration tests: append succeeds for an existing member, duplicate/
      malformed email rejected, append-only enforced at the rules level
- [ ] UI tests: add a member, attempt to add a duplicate/invalid email

## Phase 7 — Home screen
- [ ] Monthly running totals (Money In, Money Out — and Net if confirmed
      in open question #3), including recurring-generated transactions
- [ ] Unit tests: totals calculation logic
- [ ] UI test: totals render correctly against a seeded set of transactions

## Phase 8 — CI pipeline (GitHub Actions)
- [ ] Workflow triggered on push/PR: build the app, run unit tests,
      integration tests (against the Firestore emulator,
      started as a CI service), and UI tests (instrumented, via Android
      emulator or Firebase Test Lab)
- [ ] Lint/static analysis step (ktlint/detekt)
- [ ] Pipeline fails the build on any test or lint failure
- [ ] See `04-cicd-and-release-pipeline.md` for full workflow detail

## Phase 9 — Release pipeline
- [ ] Triggered per the decision in open question #4 (tag push / manual
      dispatch / merge to main)
- [ ] Build a signed release APK
- [ ] Distribute via the chosen mechanism (Firebase App Distribution
      recommended, or signed-URL + email as the alternative) — **not**
      published to the Play Store
- [ ] Verify the distributed build installs and runs correctly on a real
      test device before calling this phase done

## Phase 10 — Polish
- [ ] Offline testing (airplane mode add → reconnect → syncs)
- [ ] Empty states, loading states, basic error handling
- [ ] App icon, simple theming
- [ ] Final pass on all three mandatory test categories for coverage gaps

## Suggested order of tackling open questions (requirements doc §10)
Settle before Phase 1, since it shapes the data model/rules:
1. Category/vendor/recurring management permissions (#2)

Settle before Phase 5:
2. Recurring defaults (#6), other cadences (#7), who gets notified (#8)

Settle before Phase 9, since it shapes the pipeline:
3. Release pipeline trigger (#4)
4. Email recipient list storage (#5)

Can be decided anytime without rework:
5. Remove member (#1)
6. Net figure on Home screen (#3)
