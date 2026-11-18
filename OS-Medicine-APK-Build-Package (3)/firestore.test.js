const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PATIENT_UID = "patient_alice_01";
const PATIENT_BOB_UID = "patient_bob_02";
const PHARMACY_OWNER_UID = "pharmacy_owner_01";
const ADMIN_UID = "admin_01";
const ADMIN_EMAIL = "osamsuse21@gmail.com";

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

function adminCtx() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

test("Unauthenticated user cannot read or write any collection", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").get());
  await assertFails(db.collection("pharmacies").get());
  await assertFails(db.collection("prescriptionRequests").get());
  await assertFails(db.collection("payments").get());
  await assertFails(db.collection("notifications").get());
});

test("User cannot self-assign admin role on profile creation", async () => {
  const db = testEnv.authenticatedContext(PATIENT_UID).firestore();
  const now = new Date();
  await assertFails(
    db.collection("users").doc(PATIENT_UID).set({
      userId: PATIENT_UID,
      fullName: "Alice",
      phoneNumber: "0911000001",
      email: "alice@example.com",
      role: "admin",
      accountStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Patient can create and read own user profile, Bob cannot read Alice profile", async () => {
  const aliceDb = testEnv.authenticatedContext(PATIENT_UID).firestore();
  const bobDb = testEnv.authenticatedContext(PATIENT_BOB_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(PATIENT_UID).set({
      userId: PATIENT_UID,
      fullName: "Alice Tesfaye",
      phoneNumber: "0911000001",
      email: "alice@example.com",
      role: "patient",
      accountStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(PATIENT_UID).get());
  await assertFails(bobDb.collection("users").doc(PATIENT_UID).get());
});

test("Pharmacy registration forces PENDING status and blocks self-approval", async () => {
  const pharmDb = testEnv.authenticatedContext(PHARMACY_OWNER_UID).firestore();
  const now = new Date();

  // Attempt to register pre-approved fails
  await assertFails(
    pharmDb.collection("pharmacies").doc("ph_1").set({
      ownerId: PHARMACY_OWNER_UID,
      pharmacyName: "Bole Meds",
      pharmacistName: "Dawit",
      phoneNumber: "0911223344",
      email: "bole@meds.et",
      address: "Bole, Addis Ababa",
      latitude: 9.01,
      longitude: 38.76,
      businessLicenseNumber: "LIC-991",
      licenseVerificationStatus: "VERIFIED",
      approvalStatus: "APPROVED",
      availability: true,
      averageRating: 0,
      totalRatings: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid PENDING registration succeeds
  await assertSucceeds(
    pharmDb.collection("pharmacies").doc("ph_1").set({
      ownerId: PHARMACY_OWNER_UID,
      pharmacyName: "Bole Meds",
      pharmacistName: "Dawit",
      phoneNumber: "0911223344",
      email: "bole@meds.et",
      address: "Bole, Addis Ababa",
      latitude: 9.01,
      longitude: 38.76,
      businessLicenseNumber: "LIC-991",
      licenseVerificationStatus: "PENDING",
      approvalStatus: "PENDING",
      availability: true,
      averageRating: 0,
      totalRatings: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Pharmacy owner cannot self-approve
  await assertFails(
    pharmDb.collection("pharmacies").doc("ph_1").update({
      approvalStatus: "APPROVED",
      updatedAt: new Date(),
    })
  );

  // Spoofed unverified admin email cannot approve
  const spoofDb = testEnv.authenticatedContext("spoof", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofDb.collection("pharmacies").doc("ph_1").update({
      approvalStatus: "APPROVED",
      licenseVerificationStatus: "VERIFIED",
      updatedAt: new Date(),
    })
  );

  // Verified admin can approve
  await assertSucceeds(
    adminCtx().firestore().collection("pharmacies").doc("ph_1").update({
      approvalStatus: "APPROVED",
      licenseVerificationStatus: "VERIFIED",
      updatedAt: new Date(),
    })
  );
});

test("Prescription request and payment security: patient cannot self-unlock payment", async () => {
  const aliceDb = testEnv.authenticatedContext(PATIENT_UID).firestore();
  const bobDb = testEnv.authenticatedContext(PATIENT_BOB_UID).firestore();
  const pharmDb = testEnv.authenticatedContext(PHARMACY_OWNER_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("prescriptionRequests").doc("rx_1").set({
      patientId: PATIENT_UID,
      patientName: "Alice",
      patientPhone: "0911000001",
      prescriptionImageUrl: "https://storage.example/rx_1.jpg",
      deliveryMode: "pickup",
      latitude: 9.01,
      longitude: 38.76,
      requestStatus: "PENDING",
      paymentUnlocked: false,
      eligibleOwnerIds: [PHARMACY_OWNER_UID],
      createdAt: now,
    })
  );

  // Eligible pharmacy can read, unrelated patient Bob cannot
  await assertSucceeds(pharmDb.collection("prescriptionRequests").doc("rx_1").get());
  await assertFails(bobDb.collection("prescriptionRequests").doc("rx_1").get());

  // Patient submits payment in PENDING state
  await assertSucceeds(
    aliceDb.collection("payments").doc("pay_1").set({
      userId: PATIENT_UID,
      requestId: "rx_1",
      paymentProvider: "telebirr",
      paymentAmount: 50,
      recipientNumber: "+251925694548",
      paymentPhone: "0911000001",
      paymentReference: "TXN-TB-12345",
      paymentScreenshotUrl: "https://storage.example/pay_1.jpg",
      verificationStatus: "PENDING",
      createdAt: now,
    })
  );

  // Patient cannot approve own payment or unlock request
  await assertFails(
    aliceDb.collection("payments").doc("pay_1").update({
      verificationStatus: "APPROVED",
    })
  );
  await assertFails(
    aliceDb.collection("prescriptionRequests").doc("rx_1").update({
      paymentUnlocked: true,
      requestStatus: "UNLOCKED",
    })
  );

  // Admin can approve payment and unlock request
  await assertSucceeds(
    adminCtx().firestore().collection("payments").doc("pay_1").update({
      verificationStatus: "APPROVED",
      adminId: ADMIN_UID,
      verifiedAt: new Date(),
    })
  );
  await assertSucceeds(
    adminCtx().firestore().collection("prescriptionRequests").doc("rx_1").update({
      paymentUnlocked: true,
      requestStatus: "UNLOCKED",
    })
  );
});

test("Free Firestore inline data URI image fallback works within 200KB limit and rejects oversized strings", async () => {
  const aliceDb = testEnv.authenticatedContext(PATIENT_UID).firestore();
  const now = new Date();
  const inlineDataUri = "data:image/jpeg;base64," + "A".repeat(4000);

  await assertSucceeds(
    aliceDb.collection("prescriptionRequests").doc("rx_inline").set({
      patientId: PATIENT_UID,
      patientName: "Alice",
      patientPhone: "0911000001",
      prescriptionImageUrl: inlineDataUri,
      deliveryMode: "pickup",
      latitude: 9.01,
      longitude: 38.76,
      requestStatus: "PENDING",
      paymentUnlocked: false,
      eligibleOwnerIds: [],
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("payments").doc("pay_inline").set({
      userId: PATIENT_UID,
      requestId: "rx_inline",
      paymentProvider: "telebirr",
      paymentAmount: 50,
      recipientNumber: "+251925694548",
      paymentPhone: "0911000001",
      paymentReference: "TXN-TB-INLINE",
      paymentScreenshotUrl: inlineDataUri,
      verificationStatus: "PENDING",
      createdAt: now,
    })
  );

  const oversized = "data:image/jpeg;base64," + "A".repeat(200001);
  await assertFails(
    aliceDb.collection("prescriptionRequests").doc("rx_too_big").set({
      patientId: PATIENT_UID,
      patientName: "Alice",
      patientPhone: "0911000001",
      prescriptionImageUrl: oversized,
      deliveryMode: "pickup",
      latitude: 9.01,
      longitude: 38.76,
      requestStatus: "PENDING",
      paymentUnlocked: false,
      eligibleOwnerIds: [],
      createdAt: now,
    })
  );
});
