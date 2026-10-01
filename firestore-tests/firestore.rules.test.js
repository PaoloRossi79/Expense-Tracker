// Firestore security-rules tests (Design/01 §7, Design/03).
// Run with: cd firestore-tests && npm install && npm test
// (npm test starts the emulator via `firebase emulators:exec`).
import { readFileSync } from 'node:fs';
import { after, before, describe, it } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import { deleteDoc, doc, getDoc, setDoc, updateDoc } from 'firebase/firestore';

const HOUSEHOLD = 'home-1';
const OTHER = 'home-2';
const REC_ID = 'rec_rule1_20260315';

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-expense-tracker',
    firestore: {
      rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'),
      host: '127.0.0.1',
      port: 8080,
    },
  });

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'households', HOUSEHOLD), {
      name: 'Home',
      memberEmails: ['paolo@example.com', 'anna@example.com'],
    });
    await setDoc(doc(db, 'households', OTHER), {
      name: 'Other',
      memberEmails: ['stranger@example.com'],
    });
    await setDoc(doc(db, 'households', HOUSEHOLD, 'transactions', 'tx-1'), {
      type: 'expense',
      amount: 100,
      categoryId: 'c1',
      createdByUid: 'uid-paolo',
      isRecurringInstance: false,
    });
  });
});

after(async () => {
  await testEnv?.cleanup();
});

const asPaolo = () =>
  testEnv.authenticatedContext('uid-paolo', { email: 'paolo@example.com' }).firestore();
const asAnna = () =>
  testEnv.authenticatedContext('uid-anna', { email: 'anna@example.com' }).firestore();
const asStranger = () =>
  testEnv.authenticatedContext('uid-stranger', { email: 'stranger@example.com' }).firestore();

describe('household scoping', () => {
  it('allows a member to read their own household', async () => {
    await assertSucceeds(getDoc(doc(asPaolo(), 'households', HOUSEHOLD)));
  });

  it('denies cross-household read', async () => {
    await assertFails(getDoc(doc(asPaolo(), 'households', OTHER)));
    await assertFails(
      getDoc(doc(asPaolo(), 'households', OTHER, 'transactions', 'anything')),
    );
  });

  it('denies a non-member any access', async () => {
    await assertFails(getDoc(doc(asStranger(), 'households', HOUSEHOLD)));
    await assertFails(
      setDoc(doc(asStranger(), 'households', HOUSEHOLD, 'transactions', 'evil'), {
        type: 'expense',
        amount: 1,
        categoryId: 'c',
        createdByUid: 'uid-stranger',
      }),
    );
  });

  it('forbids clients creating or deleting households', async () => {
    await assertFails(setDoc(doc(asPaolo(), 'households', 'brand-new'), { name: 'x' }));
    await assertFails(deleteDoc(doc(asPaolo(), 'households', HOUSEHOLD)));
  });
});

describe('transactions are creator-only for write', () => {
  it('allows another member to read', async () => {
    await assertSucceeds(
      getDoc(doc(asAnna(), 'households', HOUSEHOLD, 'transactions', 'tx-1')),
    );
  });

  it('denies another member editing or deleting', async () => {
    await assertFails(
      updateDoc(doc(asAnna(), 'households', HOUSEHOLD, 'transactions', 'tx-1'), {
        amount: 999,
      }),
    );
    await assertFails(
      deleteDoc(doc(asAnna(), 'households', HOUSEHOLD, 'transactions', 'tx-1')),
    );
  });

  it('allows the creator to edit their own transaction', async () => {
    await assertSucceeds(
      updateDoc(doc(asPaolo(), 'households', HOUSEHOLD, 'transactions', 'tx-1'), {
        amount: 200,
      }),
    );
  });
});

describe('memberEmails is append-only', () => {
  it('allows appending a new member', async () => {
    await assertSucceeds(
      updateDoc(doc(asPaolo(), 'households', HOUSEHOLD), {
        memberEmails: ['paolo@example.com', 'anna@example.com', 'new@example.com'],
      }),
    );
  });

  it('denies removing an existing member', async () => {
    await assertFails(
      updateDoc(doc(asPaolo(), 'households', HOUSEHOLD), {
        memberEmails: ['paolo@example.com'],
      }),
    );
  });
});

describe('recurring confirmation is idempotent (create-only)', () => {
  it('allows the first confirmation of an occurrence', async () => {
    await assertSucceeds(
      setDoc(doc(asPaolo(), 'households', HOUSEHOLD, 'transactions', REC_ID), {
        type: 'expense',
        amount: 125000,
        categoryId: 'bills',
        createdByUid: 'uid-paolo',
        isRecurringInstance: true,
        recurringRuleId: 'rule1',
      }),
    );
    await assertSucceeds(
      setDoc(
        doc(asPaolo(), 'households', HOUSEHOLD, 'recurringRules', 'rule1', 'occurrences', '20260315'),
        { status: 'confirmed', resolvedByUid: 'uid-paolo', transactionId: REC_ID },
      ),
    );
  });

  it('rejects a second confirmation of the same occurrence', async () => {
    await assertFails(
      setDoc(doc(asAnna(), 'households', HOUSEHOLD, 'transactions', REC_ID), {
        type: 'expense',
        amount: 125000,
        categoryId: 'bills',
        createdByUid: 'uid-anna',
        isRecurringInstance: true,
        recurringRuleId: 'rule1',
      }),
    );
    await assertFails(
      setDoc(
        doc(asAnna(), 'households', HOUSEHOLD, 'recurringRules', 'rule1', 'occurrences', '20260315'),
        { status: 'skipped', resolvedByUid: 'uid-anna' },
      ),
    );
  });
});
