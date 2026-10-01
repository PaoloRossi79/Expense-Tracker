# Expense Tracker App — Requirements & Specifications (v4)

> **v4 note:** recurring transactions are now **defined on an in-app screen
> and confirmed by the user via a notification on the due day** — posting
> is triggered from the app, not by a server job. This removes the need for
> Cloud Functions and the paid Firebase Blaze plan; the whole stack stays
> on the free Spark tier. See §4.7. Manual setup steps live in
> `00-prerequisites.md`.

## 1. Overview

A private Android app for a household to track money in and money out, with
monthly running totals, a filterable transaction list, and simple category
management. Data lives in the cloud so all family members see a near-real-time
shared view. No public distribution for now — the app is sideloaded/installed
directly on family members' phones.

- **Platform:** Android only — Kotlin + Jetpack Compose
- **Backend:** Firebase — Cloud Firestore (free/Spark tier)
- **Identity:** Android/Google account on the device, no separate federation
  system to build — plus a biometric local gate
- **Distribution:** not published to the Play Store; APKs distributed via
  email (see §8 / `04-cicd-and-release-pipeline.md`)
- **Recurring transactions and vendor auto-allocation are in scope** (see
  §4.7 and §4.8). Recurring items are confirmed by the user from a
  notification; no server-side code is needed.

## 2. Authentication & Identity

- **First launch:** the app identifies the user via their **Android/Google
  account already on the device** — using Android's **Credential Manager**
  to offer the account(s) configured on the phone, authenticated through
  **Firebase Authentication's Google Sign-In provider**. This means no
  passwords, no email-link flow, and no custom federation to build —
  Firebase + Credential Manager does the identity work using an identity the
  device already has.
- **Household membership check:** the signed-in Google account's email is
  checked against the household's member list in Firestore. If it's not a
  member, the app shows a clear "not part of a household yet" state (see
  §10 open question on how members get added).
- **Subsequent launches:** rather than re-running the full Google
  sign-in flow every time, the app re-authenticates locally with
  **`BiometricPrompt`** (fingerprint/face/device PIN as fallback) to confirm
  it's the same person, while the underlying Firebase session stays signed
  in. If biometrics aren't enrolled on the device, fall back to device PIN
  unlock (`BiometricPrompt` supports this natively via
  `BIOMETRIC_WEAK`/`DEVICE_CREDENTIAL`).
- **Identity on every record:** every expense, income entry, and category
  carries the Firebase `uid` (and a denormalised display name) of the user
  who created it.

## 3. Users & Household Model

- A household is a small, fixed group of family members — not open sign-up.
- All members of a household see the **same shared data** (pooled ledger,
  not separate personal ledgers).
- **Record ownership:** every transaction (expense or income) is tagged with
  its creator. **Only the creator can edit or delete a record** — this must
  be enforced server-side in Firestore Security Rules, not just hidden in
  the UI.
- Categories are **shared household data** — any member can manage them (no
  separate "admin" role is specified in this version; see §10).
- **Adding members:** any existing household member can add a new member
  via the Add Member screen (§4.9) — there's no separate admin/creator role
  gating this in v3. **Removing a member is not covered yet** — flagged in
  §10 as a natural next addition, not built in this round.

## 4. Functional Requirements — Screens

### 4.1 Identification screen
- Shown on first launch (and if the signed-in account isn't a household
  member, or biometric check fails).
- Triggers Android Credential Manager → Google account chooser → Firebase
  Google Sign-In.
- On success + valid household membership → proceeds to Home.

### 4.2 Home / Initial screen
- Shows **this month's running totals**: total Money In, total Money Out
  (and a net figure is a natural addition — confirm if wanted).
- A **"Pending recurring"** card appears when one or more recurring items
  are due and not yet confirmed or skipped (§4.7), so a missed notification
  never means a missed payment. Tapping it opens the confirmation flow.
- Bottom bar with icons for the four core, frequent actions: **Add
  Expense**, **Add Money In**, **Transactions**, **Categories**. The less
  frequent screens — **Recurring Rules** (§4.7), **Vendors** (§4.8), and
  **Add Member** (§4.9) — live behind a **"More" / overflow menu** rather
  than crowding the bottom bar; revisit this grouping once the app is in
  use if it doesn't feel right.

### 4.3 Add Expense
Form fields:
- Amount (required)
- Vendor (free text)
- Date (required, defaults to today)
- Category (required — dropdown populated from the **expense** category
  list)
- **Quick action:** "+ New category" inline from the dropdown — opens a
  small create-category prompt (name only, type defaults to Expense); on
  save, the new category is immediately selected as this expense's category.
- Record saved with: amount, vendor, date, categoryId, createdByUid,
  createdAt.

### 4.4 Add Money In
Same structure as Add Expense, but:
- Category dropdown is populated from the **income** category list (a
  separate list from expense categories — see §5).
- Same inline "+ New category" quick action, creating an income-type
  category.

### 4.5 Transactions list (monthly)
- Shows **all transactions (expenses and income) for the selected month**,
  in **reverse chronological order**.
- Each row shows a **category indicator** (icon/color/label/tag — pick one
  consistent treatment) so expense vs income and category are visually
  scannable without opening the item.
- **Contextual menu per item** (long-press or overflow `⋮`):
  - **Edit** — opens the same form as Add (Expense or Money In, matching
    the record's type), prefilled with the existing values.
  - **Delete** — requires a confirmation dialog before removing.
  - Both actions are **only available to the record's creator** — hidden or
    disabled for other household members' records (and blocked server-side
    regardless of what the UI shows).
- **Filters:** at minimum by type (Expense/Income/Both) and by category.
- **Sorting:** at minimum by date (asc/desc) and by amount (asc/desc).
- Month selector to move between months (defaults to current month).

### 4.6 Manage Categories
- Full CRUD on category entities: Create, Read, Update, Delete.
- **No hierarchy** — a flat list.
- Each category has a **type**: `expense` or `income` — **never both**. A
  category created for expenses cannot be reused for income entries, and
  vice versa.
- Fields: name (required), type (required, fixed at creation — changing
  type after transactions reference it is a data-integrity risk; recommend
  blocking type changes once a category is in use), optional icon/color.
- Deleting a category that's in use by existing transactions should either
  be blocked, or require reassigning those transactions first — pick one
  and apply it consistently (recommend: block deletion with a clear message
  showing how many transactions use it).

### 4.7 Recurring Transactions
Examples: mortgage, council tax, salary, subscriptions. Works for both
expenses and income.

**Defining rules (Recurring Rules screen, behind the "More" menu, §4.2):**
- Create / edit / pause / resume / delete a rule. Fields: type (expense or
  income), **reason/description**, **amount**, **day of the month** (1-31),
  category (from the matching expense or income list), vendor (optional,
  with the same auto-allocation as §4.8), start date, optional end date,
  active/paused flag.
- **Short months:** a rule set for day 29, 30 or 31 falls due on the **last
  day** of months that are shorter (e.g. day 31 → 28 Feb). Assumed default;
  see §10.
- Monthly is the required cadence. Weekly and annual cadences are optional
  extras (§10).

**Confirming occurrences (notification-driven, triggered from the app):**
- On the due day, every household member's device shows a **local
  notification** at a configurable time (default 09:00): e.g. "Mortgage
  £1,250.00 is due today. Confirm?".
- Notification actions: **Confirm** (posts the transaction as specified),
  **Edit & confirm** (opens the Add form prefilled, for bills whose amount
  varies), and **Skip** (marks this occurrence as skipped, no transaction).
- Confirming creates a normal transaction (`isRecurringInstance: true`,
  linked to the rule) dated on the due date, attributed to **whoever
  confirmed it**. It then appears in the transactions list and Home totals.
- **No double-posting:** the transaction ID is deterministic
  (`rec_{ruleId}_{yyyyMMdd}`), so if two members confirm at the same time
  the second attempt is a no-op. Once an occurrence is confirmed or skipped
  on one device, the other devices' notifications for it are dismissed
  when they sync.
- **Missed occurrences:** an unconfirmed occurrence stays **pending**. It
  shows in the Home "Pending recurring" card and the user is re-notified
  daily until it is confirmed or skipped.
- **Implementation approach:** each device runs a daily background check
  (WorkManager) against the household's rules and posts local
  notifications. This needs **no server code**, so no Cloud Functions and
  no Blaze plan. Trade-off: if nobody opens the app or Android's battery
  optimisation suppresses background work, a notification can be late; the
  pending card and re-notification cover that.
- **Permissions:** Android 13+ requires the user to grant the
  `POST_NOTIFICATIONS` permission; ask on first launch and explain why.
- Pausing a rule stops future notifications and doesn't affect history.

### 4.8 Vendor Auto-Allocation
- **Vendors** are a managed list, each optionally mapped to a **default
  category** (e.g. "Tesco" → Groceries, "Vodafone" → Communications,
  "PureGym" → Sport). Manageable from a Vendors screen (behind the "More"
  menu, §4.2) — add/edit/delete a vendor and its default category.
- In **Add Expense** (and Add Money In, where "vendor" means income
  source), as the user types into the Vendor field, the app matches
  against known vendors: **exact match** first, then a **normalised/
  fuzzy match** (uppercase, strip trailing store numbers/punctuation,
  "contains" match) — e.g. "TESCO STORES 2903" should still match "Tesco".
- On a match, the **category pre-fills automatically**; the user can
  always override it before saving.
- On an **unrecognised vendor**, prompt: "Save this as a new vendor and
  map it to a category?" — if accepted, it's added to the vendor list for
  future auto-matching.

### 4.9 Add Member
- A simple form: enter the new member's email address, submit.
- On submit, the email is added to the household's `memberEmails` (§5).
  The new member gets access the moment they sign in with a matching
  Google account on their phone — there's no separate accept-an-invite
  step, since sign-in is Google-account-based rather than email-link-based.
- Show the current member list on this screen too, so it doubles as a
  simple roster view (even though removal isn't built yet — §10).
- Basic validation: well-formed email, not already a member.
- **Bootstrap:** the very first household and its first member are created
  **manually in the Firestore console** (steps in `00-prerequisites.md`),
  since Add Member requires an existing member. Clients can't create
  household documents.

## 5. Data Model (Firestore, high level)

```
households/{householdId}
  name, createdAt, memberEmails: [email, email, ...]

households/{householdId}/categories/{categoryId}
  name, type: "expense" | "income", icon (optional), color (optional)

households/{householdId}/transactions/{transactionId}
  type: "expense" | "income"
  amount            // store as integer minor units (pence), not float
  date              // Firestore Timestamp, stored UTC
  vendor            // free text, expense or income source
  vendorId          // nullable — set when the vendor matched a known vendor
  categoryId        // must reference a category of matching type
  isRecurringInstance: bool
  recurringRuleId   // nullable
  // id for recurring instances: rec_{ruleId}_{yyyyMMdd} (idempotent)
  createdByUid
  createdByName     // denormalised for display without extra reads
  createdAt
  updatedAt

households/{householdId}/vendors/{vendorId}
  name, defaultCategoryId, matchAliases: [string, ...]

households/{householdId}/recurringRules/{ruleId}
  type: "expense" | "income"
  reason, amount, categoryId, vendorId (nullable)
  dayOfMonth        // 1-31, clamped to the month's last day when needed
  startDate, endDate (nullable), active: bool
  createdByUid, createdByName, createdAt, updatedAt

households/{householdId}/recurringRules/{ruleId}/occurrences/{yyyyMMdd}
  status: "confirmed" | "skipped"
  resolvedByUid, resolvedAt
  transactionId (nullable; set when confirmed)
```

Security rule shape:
- A user may read/write documents under `households/{householdId}/**` only
  if their authenticated email is in that household's `memberEmails`.
- A user may **update or delete** a `transactions/{transactionId}` document
  only if `request.auth.uid == resource.data.createdByUid`.
- Category, vendor, and recurring-rule writes: any household member (no
  stricter role defined here — see §10).
- Recurring occurrences: any household member may create a transaction
  with a `rec_...` ID or an `occurrences` document **only if it doesn't
  already exist** (create-only, no overwrite), which is what makes
  confirmation idempotent across devices.
- `memberEmails` writes: any existing household member may **append** a new
  email (the Add Member screen); removing an email isn't built yet — enforce
  "append-only, no removal" at the rules level for now so the UI can't be
  the only thing preventing it.

## 6. Non-Functional Requirements

- **Sync:** near-real-time — a transaction added on one phone should appear
  on another within seconds when both are online (Firestore snapshot
  listeners).
- **Offline support:** add/view transactions with no connection; syncs
  automatically when back online (Firestore offline persistence).
- **Data isolation:** one household's data is never visible to another —
  enforced in Firestore Security Rules, not just app logic.
- **Simplicity first:** this is a family tool — favour a small, clear
  feature set over configurability.

## 7. Testing Requirements (mandatory)

- **Unit tests** — mandatory. Cover business logic: amount formatting/
  rounding, date handling, filter/sort logic, category-type validation
  (expense category can't be used for income and vice versa), totals
  calculation for the Home screen, **vendor matching** (exact + normalised/
  fuzzy match), and **recurring-rule next-due-date calculation** (including short-month
  clamping, end dates, paused rules, and pending/overdue detection).
- **Integration tests** — mandatory. Against the **Firestore emulator**:
  repository read/write behaviour, and **security rules** (same-household
  read/write allowed, cross-household denied, creator-only edit/delete
  enforced, category-type constraints enforced server-side if mirrored
  there, `memberEmails` append-only enforced, **recurring confirmation is
  idempotent** — a second create of the same `rec_...` transaction is
  rejected).
- **UI tests** — mandatory. Compose UI tests for the core flows: sign-in →
  Home totals render, Add Expense (including inline category creation and
  vendor auto-match), Add Money In, edit a transaction, delete with
  confirmation, filter/sort the transactions list, category CRUD, vendor
  CRUD, recurring rule creation, notification Confirm/Edit/Skip actions and the
  Home "Pending recurring" card, Add Member,
  and verifying edit/delete controls are hidden/disabled on another user's
  transaction.

**Suggested, on top of the mandatory three:**
- **Repository contract tests**, so the data layer can be refactored safely
- **Instrumented end-to-end test**: sign in → add transaction → confirm
  sync to a second simulated client
- **Screenshot/regression tests** for the Home totals and transaction list,
  since miscalculated totals in a finance app are a high-cost bug class
- **Static analysis / lint** (ktlint or detekt) wired into CI so style and
  common bug patterns are caught automatically, not just correctness bugs

## 8. CI/CD & Release (summary — full detail in `04-cicd-and-release-pipeline.md`)

- Repository hosted on **GitHub**.
- **Build pipeline** (GitHub Actions): on every push/PR, build the app and
  run unit + integration + UI tests; fail the pipeline on any failure.
- **Release pipeline**: triggered once a build completes successfully
  (recommend: on a version tag, or manual dispatch) — builds a release APK
  and gets it to the family **without publishing to the Play Store**:
  - **Recommended:** Firebase App Distribution — it already emails testers
    a secure download link out of the box, no custom email-sending code
    needed.
  - **Alternative:** build the APK as a CI artifact, upload to a storage
    bucket with a time-limited signed URL, and send that link by email via
    an SMTP/email-API step in the pipeline.
  - A plain email with the raw APK attached is **not recommended** (size
    limits, no revocation, looks like a phishing attempt to most mail
    clients) — a secure link is the better of the two options you raised.

## 9. Summary of What Changed Across Drafts

**v1 → v2:**
- Auth moved from Firebase email-link invites to **Google Sign-In via
  Android Credential Manager**, with biometrics as the local re-entry gate.
- Recurring transactions and vendor auto-allocation were dropped.
- UI tests added as a mandatory testing category, alongside unit +
  integration.
- CI/CD and a release pipeline came into scope, including non-Play-Store
  distribution via email/secure link.

**v2 → v3:**
- **Recurring transactions reinstated** — see §4.7. Confirm-first-then-
  auto-post behaviour carried over from the earlier decision.
- **Vendor auto-allocation reinstated** — see §4.8, including a Vendors
  management screen.
- **Add Member screen added** — see §4.9. Any member can add a new member;
  removal isn't built yet (§10).
- Data model extended with `vendors` and `recurringRules` collections, and
  a `vendorId`/`recurringRuleId` link on transactions.

**v3 → v4 (this version):**
- **Recurring items redesigned:** defined on an in-app screen (day of
  month, amount, reason, etc.) and **confirmed per occurrence from a
  notification** on the due day. This replaces "confirm first, then
  auto-post".
- **Cloud Functions and the Blaze plan dropped** — posting is triggered from
  the app, so everything stays on Firebase's free Spark tier.
- **`00-prerequisites.md` added** — manual setup steps with detailed
  instructions.

## 10. Open Questions

1. **Remove member:** not built in this round — worth flagging whether it's
   needed soon, since right now membership only grows.
2. **Category management permissions:** should *any* household member be
   able to create/edit/delete shared categories (and now vendors and
   recurring rules), or should that be restricted to an admin/creator role?
3. **Net figure on Home screen:** show Money In − Money Out as a third
   number, or keep it to just the two totals as specified?
4. **Release pipeline trigger:** release on every merge to `main`, on a
   version tag you push manually, or via manual workflow dispatch?
5. **Email recipient list:** static list of family email addresses stored
   as a GitHub Actions secret, or should this be configurable without a
   code change (e.g. a repo variable)?
6. **Recurring defaults to confirm:** (a) days 29-31 clamp to the last day
   of shorter months; (b) notification time defaults to 09:00 and is
   user-configurable; (c) unconfirmed items re-notify daily. Say if you want
   any of these different.
7. **Other cadences:** monthly is built first. Do you also want weekly
   and/or annual recurring items in this round, or later?
8. **Who gets notified:** currently every household member's device gets the
   notification and the first to confirm wins. Alternative: only the rule's
   creator is notified. Which do you prefer?
