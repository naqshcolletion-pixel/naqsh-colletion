const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "nasir_ali_owner_1";
const OTHER_UID = "unauthorized_user_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user cannot read or write owner profile or snapshots", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(OWNER_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(OWNER_UID).set({
      userId: OWNER_UID,
      ownerName: "NASIR ALI",
      businessName: "NAQSH COLLECTION",
      ownerMobile: "+91 9471477887",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(OWNER_UID).collection("cloud_snapshots").get()
  );
});

test("Authenticated owner can create, update, and read own profile and snapshots", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    ownerDb.collection("users").doc(OWNER_UID).set({
      userId: OWNER_UID,
      ownerName: "NASIR ALI",
      businessName: "NAQSH COLLECTION",
      ownerMobile: "+91 9471477887",
      gstin: "",
      upiId: "nasir@ybl",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    ownerDb.collection("users").doc(OWNER_UID).update({
      upiId: "naqshcollection@ibl",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    ownerDb.collection("users").doc(OWNER_UID).collection("cloud_snapshots").doc("snap_1").set({
      userId: OWNER_UID,
      label: "Daily Boutique Backup",
      productCount: 30,
      customerCount: 12,
      invoiceCount: 8,
      totalRevenue: 45200.0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    ownerDb.collection("users").doc(OWNER_UID).collection("cloud_snapshots").get()
  );
});

test("Cross-user access and ghost/immutable field mutations are rejected", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await ownerDb.collection("users").doc(OWNER_UID).set({
    userId: OWNER_UID,
    ownerName: "NASIR ALI",
    businessName: "NAQSH COLLECTION",
    ownerMobile: "+91 9471477887",
    createdAt: created,
    updatedAt: created,
  });

  // Cross-user read & write blocked
  await assertFails(otherDb.collection("users").doc(OWNER_UID).get());
  await assertFails(otherDb.collection("users").doc(OWNER_UID).collection("cloud_snapshots").get());

  // Ghost field blocked
  await assertFails(
    ownerDb.collection("users").doc(OWNER_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt change blocked
  await assertFails(
    ownerDb.collection("users").doc(OWNER_UID).update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Future timestamp blocked
  await assertFails(
    ownerDb.collection("users").doc(OWNER_UID).collection("cloud_snapshots").doc("snap_future").set({
      userId: OWNER_UID,
      label: "Future",
      productCount: 1,
      customerCount: 1,
      invoiceCount: 1,
      totalRevenue: 100,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
