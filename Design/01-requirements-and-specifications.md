# Expense Tracker App — Requirements & Specifications

## 1. Overview

A simple, private Android app for a household to track money in and money out,
with categorised summaries, recurring transactions, and automatic
categorisation of known vendors. Data lives in the cloud so all family members
see a near-real-time shared view.

- **Platform:** Native Android — Kotlin + Jetpack Compose
- **Backend:** Firebase — Firebase Authentication + Cloud Firestore
  (Firestore's offline cache gives "add an expense with no signal, it syncs
  later" for free)
- **Users:** A small, fixed household (not open sign-up) — each member has
  their own login, all linked to one shared household
- **Currency:** GBP only (single currency, no FX) — *confirm*
- **Scale:** Single household, a handful of users, years of transaction
  history — this is a design scale of "dozens of users, tens of thousands of
  rows," not a multi-tenant SaaS product

## 2. Users & Roles

| Role | Can do |
|---|---|
| **Member** (default) | Add transactions; edit/delete **only transactions they created**; view all household transactions & summaries; add vendors |
| **Admin** (household creator, maybe more than one) | Everything a Member can, plus: manage categories, manage vendor→category rules, invite/remove household members, edit recurring rules. **Admins do not get override rights on other members' individual transactions** — editing/deleting a transaction is creator-only, full stop. |

**Decided:** only the creator of a transaction can edit or delete it — not
even an Admin can edit someone else's entry. Enforce this server-side in
Firestore Security Rules (`request.auth.uid == resource.data.addedByUid` on
update/delete of a transaction document), not just in the UI.

### 2.1 Household model

- A household is created by the first user (becomes Admin).
- Admin invites other members by **email** — Firebase Authentication email
  link. Invited email is added to a pending-invite list on the household;
  when that email signs in via Firebase Auth, they're automatically attached
  to the household as a Member.
- All members of a household see the **same shared data** — this is a joint
  family ledger, not separate personal ledgers that roll up.

## 3. Authentication

- **Sign-in:** Firebase Authentication, **email link (passwordless)** —
  this pairs naturally with the email-based invite flow (§2.1): the invite
  email doubles as the sign-in method, no separate password or Google
  account requirement.
- **App-level lock:** Android `BiometricPrompt` (fingerprint/face) or device
  PIN as a *second, local* gate after Firebase sign-in — so the app locks
  itself on the phone even though the Firebase session stays alive. This
  matches your "biometric or device-based authentication" ask.
- Each family member signs in with their **own identity** — required for
  "who added this transaction" and for the individual-login model you chose.

## 4. Functional Requirements

### 4.1 Add Expense ("Money Out")
Fields:
- Amount (required, GBP, positive number)
- Date (required, defaults to today)
- Where / Vendor (required — free text with autocomplete against known
  vendors; see §4.4)
- Category (auto-filled if vendor is known, editable; required)
- Notes (optional free text)
- Payment method (optional — e.g. card/cash/direct debit) — *nice-to-have,
  confirm if wanted*
- Added by (auto-set to the signed-in user)

### 4.2 Add Money In ("Income")
Fields:
- Amount (required)
- Date (required)
- Category / Reason (required — e.g. Salary, Gift, Refund, Side income)
- Type: **One-off** or **Recurring**
- If Recurring: frequency (weekly/monthly/annually), start date, optional end
  date
- Notes (optional)
- Added by (auto-set)

### 4.3 Recurring Transactions (expenses AND income)
Examples: mortgage, council tax, salary, subscriptions.
- A recurring rule stores: amount, category, vendor/reason, frequency
  (weekly/monthly/annually/custom day-of-month), start date, optional end
  date, active/paused flag.
- **Decided behaviour:** the **first occurrence** of a new recurring rule
  requires the user to confirm it before it posts as a transaction (catches
  typos/wrong amounts early). **Every occurrence after that auto-posts** on
  its due date without a confirmation step, and shows up in history/summaries
  automatically. Editing the rule's amount/category later should re-arm a
  one-time confirmation on its next occurrence, so a changed mortgage payment
  doesn't silently auto-post the old amount.
- Recurring rules are editable and can be paused/stopped without deleting
  history.

### 4.4 Categories & Vendors
- **Categories:** CRUD (create/edit/delete/reorder), each with a name, an
  icon/colour, and a type scope (expense category, income category, or
  both). Deleting a category in use should either be blocked or require
  reassigning existing transactions.
- **Vendors:** CRUD, each vendor optionally mapped to a default category
  (e.g. "Tesco" → Groceries, "Vodafone" → Communications, "PureGym" →
  Sport).
- **Auto-allocation:** when the user types a vendor name in "Where," the app
  matches it against known vendors (exact match first, then fuzzy/contains
  match — e.g. "TESCO STORES 2903" should still match "Tesco") and
  pre-fills the category. The user can always override before saving. A
  new/unrecognised vendor prompts "Save this as a new vendor and map it to a
  category?" so the vendor list grows over time.

### 4.5 Summaries & Reports
- Total money in vs money out for a selected period (this month, last month,
  custom range, year).
- Breakdown by category (bar/pie-style list with amounts and %).
- Breakdown by vendor (top spend vendors).
- Trend over time (monthly totals, last 6–12 months).
- Filter by household member ("who spent what") — *confirm if wanted, given
  it's a shared ledger.*
- Recurring vs one-off breakdown (e.g. "fixed costs" vs "discretionary").

## 5. Non-Functional Requirements

- **Sync:** near-real-time — a transaction added on one phone should appear
  on another within seconds when both are online (Firestore snapshot
  listeners give this natively).
- **Offline support:** must be able to add/view transactions with no
  connection; syncs automatically when back online (Firestore offline
  persistence handles this largely for free).
- **Data isolation:** one household's data must never be visible to another
  household — enforced server-side via Firestore Security Rules, not just
  app logic.
- **Data ownership/export:** ability to export transactions (e.g. CSV) —
  *nice-to-have, confirm if wanted for v1 or later.*
- **Simplicity first:** this is a family tool, not a commercial product —
  favour a small, clear feature set over configurability.
- **Automated testing:** required from the start, not an afterthought.
  Minimum bar:
  - **Unit tests** for all non-trivial business logic (vendor matching,
    recurring-rule due-date calculation, summary/aggregation math, amount
    formatting/rounding)
  - **Integration tests** against the Firestore emulator, covering both data
    access (repositories read/write what's expected) and **security rules**
    (same-household access allowed, cross-household access denied,
    non-member writes denied, creator-only edit/delete enforced)

  Recommended, to add on top as the app matures:
  - **Compose UI tests** for the core flows (add expense, add income, edit
    own transaction, attempt-to-edit someone else's transaction is blocked)
  - **Repository contract tests** so the data layer can be refactored (e.g.
    swapped or mocked) without breaking callers
  - **End-to-end / instrumented tests** on a real or emulated device for the
    full sign-in → add → sync → see-on-second-device path
  - **CI pipeline** (e.g. GitHub Actions) running unit + integration tests
    on every push, so regressions are caught before they reach a phone

## 6. Data Model (Firestore, high level)

```
households/{householdId}
  name, createdAt, memberIds: [uid, uid, ...]

households/{householdId}/members/{uid}
  displayName, role: "admin" | "member", joinedAt

households/{householdId}/categories/{categoryId}
  name, type: "expense" | "income" | "both", icon, color

households/{householdId}/vendors/{vendorId}
  name, defaultCategoryId, matchAliases: [string, ...]

households/{householdId}/transactions/{transactionId}
  type: "expense" | "income"
  amount, date, categoryId, vendorId (nullable for income),
  reason (for income), notes, paymentMethod (optional),
  isRecurringInstance: bool, recurringRuleId (nullable),
  addedByUid, createdAt, updatedAt

households/{householdId}/recurringRules/{ruleId}
  type: "expense" | "income"
  amount, categoryId, vendorId/reason, frequency, startDate, endDate,
  active: bool
```

Security rule shape: a user can only read/write documents under
`households/{householdId}/**` where their `uid` is in that household's
`memberIds`.

## 7. Open Questions (need your input before/while building)

~~Editing others' transactions~~ — **decided:** creator-only, see §2.
~~Recurring behaviour~~ — **decided:** confirm-first-then-auto-post, see §4.3.
~~Invite mechanism~~ — **decided:** email-based (Firebase email link), see §2.1 and §3.

1. **Per-member breakdown:** do you want "who spent what" in the summaries,
   or should it stay a fully pooled household view?
2. **Export:** is CSV/PDF export needed for v1, or can it wait?
3. **Budgets:** any interest in setting a monthly budget per category with
   an over/under indicator? (Not in your original list — flagging in case
   it's implicitly wanted.)
4. **Distribution:** is this just for your own family's phones (sideload /
   Firebase App Distribution / internal Play testing track), or do you want
   it published on the Play Store (even privately)?
