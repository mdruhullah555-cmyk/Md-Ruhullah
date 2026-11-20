const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any SpendWise collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("budgets").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("recurring").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set({
        userId: ALICE_UID,
        title: "Coffee",
        amount: 4.5,
        type: "EXPENSE",
        category: "Food & Dining",
        dateMillis: Date.now(),
        monthKey: "2026-11",
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("User profile create, partial currency update, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      currencyCode: "USD",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update of currencyCode succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      currencyCode: "BDT",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read & write fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ currencyCode: "EUR" })
  );

  // Ghost field & invalid currency fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      currencyCode: "XYZ",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Transactions CRUD mirrors Kotlin payloads and enforces schema/ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Create without optional note
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set({
        userId: ALICE_UID,
        title: "Salary",
        amount: 3500.0,
        type: "INCOME",
        category: "Salary",
        dateMillis: Date.now(),
        monthKey: "2026-11",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Create with optional note
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_2")
      .set({
        userId: ALICE_UID,
        title: "Groceries",
        amount: 64.25,
        type: "EXPENSE",
        category: "Food & Dining",
        note: "Weekly market run",
        dateMillis: Date.now(),
        monthKey: "2026-11",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_2")
      .update({
        title: "Organic Groceries",
        amount: 72.1,
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // List subcollection succeeds for owner, fails for Bob
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .orderBy("dateMillis", "desc")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );

  // Reject @DocumentId 'id' in payload
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_bad_id")
      .set({
        id: "tx_bad_id",
        userId: ALICE_UID,
        title: "Bad",
        amount: 10,
        type: "EXPENSE",
        category: "Other",
        dateMillis: Date.now(),
        monthKey: "2026-11",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject tampered future timestamp & negative amount & createdAt overwrite
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_future")
      .set({
        userId: ALICE_UID,
        title: "Future",
        amount: 10,
        type: "EXPENSE",
        category: "Other",
        dateMillis: Date.now(),
        monthKey: "2026-11",
        createdAt: new Date(Date.now() + 60000),
        updatedAt: created,
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_neg")
      .set({
        userId: ALICE_UID,
        title: "Negative",
        amount: -5,
        type: "EXPENSE",
        category: "Other",
        dateMillis: Date.now(),
        monthKey: "2026-11",
        createdAt: created,
        updatedAt: created,
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_2")
      .update({
        createdAt: new Date(Date.now() - 200),
        updatedAt: new Date(Date.now() - 200),
      })
  );
});

test("Budgets and Recurring payments CRUD and bounds validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("budgets")
      .doc("budget_2026-11_OVERALL")
      .set({
        userId: ALICE_UID,
        category: "OVERALL",
        limitAmount: 2000.0,
        monthKey: "2026-11",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("budgets")
      .doc("budget_2026-11_OVERALL")
      .update({
        limitAmount: 2250.0,
        updatedAt: new Date(Date.now() - 500),
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("recurring")
      .doc("rec_rent")
      .set({
        userId: ALICE_UID,
        title: "Apartment Rent",
        amount: 950.0,
        type: "EXPENSE",
        category: "Housing & Rent",
        frequency: "MONTHLY",
        dayOfMonth: 1,
        nextDueMillis: Date.now() + 86400000,
        reminderDaysBefore: 3,
        active: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("recurring")
      .doc("rec_rent")
      .update({
        active: false,
        updatedAt: new Date(Date.now() - 500),
      })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("budgets").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("recurring").get()
  );

  // Invalid dayOfMonth or reminderDaysBefore fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("recurring")
      .doc("rec_bad")
      .set({
        userId: ALICE_UID,
        title: "Invalid",
        amount: 20,
        type: "EXPENSE",
        category: "Other",
        frequency: "MONTHLY",
        dayOfMonth: 45,
        nextDueMillis: Date.now(),
        reminderDaysBefore: 2,
        active: true,
        createdAt: created,
        updatedAt: created,
      })
  );
});

test("Loans CRUD, partial repayment updates, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("loans")
      .doc("loan_1")
      .set({
        userId: ALICE_UID,
        kind: "BORROW",
        personName: "Rahim",
        amount: 1500,
        paidAmount: 0,
        dateMillis: Date.now(),
        note: "Emergency travel",
        settled: false,
        repayments: [],
        createdAt: created,
        updatedAt: created,
      })
  );

  // Partial repayment update succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("loans")
      .doc("loan_1")
      .update({
        paidAmount: 500,
        settled: false,
        repayments: [`${Date.now()}|500`],
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Cross-user read & write fail
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("loans").get()
  );

  // Overpaying above total amount fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("loans")
      .doc("loan_1")
      .update({
        paidAmount: 2000,
        settled: true,
        updatedAt: new Date(Date.now() - 300),
      })
  );
});
