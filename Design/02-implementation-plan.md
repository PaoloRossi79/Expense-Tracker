# Expense Tracker App — Implementation Plan

A phased plan you can work through yourself, or hand phase-by-phase to a
coding agent using the accompanying `03-coding-agent-instructions.md`.

## Phase 0 — Setup (you, ~1 hour)
- [ ] Create a Firebase project (console.firebase.google.com)
- [ ] Enable **Authentication** (Email Link sign-in)
- [ ] Enable **Cloud Firestore** (start in production mode, not test mode)
- [ ] Create an Android Studio project (Kotlin, Jetpack Compose, min SDK 26+
      recommended for BiometricPrompt support)
- [ ] Connect the Android project to Firebase (`google-services.json`)
- [ ] Install the **Firebase Local Emulator Suite** (Firestore emulator) for
      local integration testing
- [ ] Decide answers to the open questions in the requirements doc (§7) —
      at least #1, #2, #3, #6, since they affect the data model and screens

## Phase 1 — Foundations
- [ ] Firestore security rules implementing household-scoped access (see
      requirements doc §6), including creator-only edit/delete on
      transactions
- [ ] Security rules integration tests against the emulator (same-household
      allowed, cross-household denied, non-member write denied,
      creator-only edit/delete enforced) — write these alongside the rules,
      not after
- [ ] Auth flow: sign in → create-or-join household → biometric/device lock
      on app resume
- [ ] Data models (Kotlin data classes) matching the Firestore schema
- [ ] Basic navigation shell (bottom nav or nav drawer): Home/Summary, Add,
      Categories, Vendors, Settings

## Phase 2 — Core transactions
- [ ] Add Expense screen + save to Firestore
- [ ] Add Money In screen (one-off) + save to Firestore
- [ ] Transaction list (household-wide, real-time via Firestore listener)
- [ ] Edit/delete transaction — **creator-only**, enforced in Firestore
      Security Rules, not just hidden in the UI

## Phase 3 — Categories & vendor auto-allocation
- [ ] Category management screen (CRUD)
- [ ] Vendor management screen (CRUD, default category mapping)
- [ ] Vendor-matching logic in the Add Expense flow (exact + fuzzy match,
      auto-fill category, prompt to save new vendors)

## Phase 4 — Recurring transactions
- [ ] Recurring rule management screen (create/edit/pause)
- [ ] First occurrence of a new (or just-edited) recurring rule prompts the
      user to confirm before posting
- [ ] Subsequent occurrences auto-post on their due date via a scheduled
      Cloud Function — no confirmation needed
- [ ] Editing a rule's amount/category re-arms the one-time confirmation on
      its next occurrence

## Phase 5 — Summaries & reports
- [ ] Period selector (this month / last month / custom / year)
- [ ] Totals in vs out, by category, by vendor
- [ ] Trend chart (monthly totals)
- [ ] Optional: per-member filter (open question #3)

## Phase 6 — Polish & distribution
- [ ] Offline testing (airplane mode add → reconnect → syncs)
- [ ] Empty states, loading states, basic error handling
- [ ] App icon, simple theming
- [ ] Distribute per open question #7 (sideload APK to family phones is
      simplest to start; Play internal testing track if you want
      auto-updates without manual APK sharing)

## Suggested order of tackling open questions
✅ Editing permissions — decided: creator-only
✅ Recurring auto-post vs confirm — decided: confirm-first-then-auto-post
✅ Invite mechanism — decided: email-based (Firebase email link)

All data-model-shaping decisions are now made — Phase 1 is unblocked.

These can be deferred to Phase 5/6 without rework:
1. Per-member breakdown (§7.1 in requirements doc)
2. Export (§7.2)
3. Budgets (§7.3)
4. Distribution method (§7.4)
