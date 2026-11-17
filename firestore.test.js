const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_workspace_1";
const BOB_UID = "bob_workspace_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

function validPimsAccountPayload(created) {
  return {
    personalId: "PIMS-2026-0001",
    passwordHash: "a1b2c3d4e5f60718293a4b5c6d7e8f90",
    passwordSalt: "somesalt12345678",
    role: "SUPER_ADMIN",
    active: true,
    mustChangePassword: false,
    failedLoginAttempts: 0,
    lockedUntilEpochMs: 0,
    canEditSelfProfile: true,
    assignedRecordIds: [],
    fullNameBn: "মোঃ আল আমিন মিয়া",
    fullNameEn: "Md. Al Amin Mia",
    fatherName: "মোঃ করিম মিয়া",
    motherName: "মিসেস রহিমা বেগম",
    dateOfBirth: "1995-05-12",
    gender: "পুরুষ",
    bloodGroup: "O+",
    mobileNumber: "01711000000",
    emailAddress: "alamin@example.com",
    photoBase64: "",
    institutionId: "inst_1",
    institutionName: "ঢাকা কলেজ",
    institutionType: "COLLEGE",
    currentClassOrYear: "স্নাতক",
    educationalQualification: "বিএসসি",
    departmentOrSubject: "কম্পিউটার বিজ্ঞান",
    academicInfo: "প্রথম শ্রেণি",
    careerGoal: "সফটওয়্যার আর্কিটেক্ট",
    personalIntro: "প্রযুক্তি ও তথ্য নিরাপত্তা নিয়ে কাজ করতে আগ্রহী।",
    skills: "Kotlin, Compose",
    interests: "পাঠ, গবেষণা",
    orgLevel: "কেন্দ্রীয়",
    orgDesignation: "সুপার অ্যাডমিন",
    orgJoiningDate: "2024-01-01",
    orgStatus: "সক্রিয়",
    orgAdditionalInfo: "",
    divisionId: "div_dhaka",
    divisionName: "ঢাকা",
    districtId: "dist_dhaka",
    districtName: "ঢাকা",
    upazilaId: "upz_dhanmondi",
    upazilaName: "ধানমন্ডি",
    unionId: "",
    unionName: "",
    wardId: "",
    wardName: "",
    villageName: "জিগাতলা",
    createdAt: created,
    updatedAt: created,
  };
}

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

test("Unauthenticated access is strictly denied across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("pims_accounts").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("locations").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("vault_entries").get());
});

test("Workspace root: owner can create and update; cross-user and ghost fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      workspaceInitialized: true,
      nextIdSequence: 2,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      nextIdSequence: 3,
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user read/write must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      nextIdSequence: 99,
      updatedAt: pastDate(500),
    })
  );

  // Ghost field must fail
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      workspaceInitialized: true,
      nextIdSequence: 2,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("PIMS accounts: admin create/delete, owner own-profile edit without role escalation, immutable & validation guards", async () => {
  const adminDb = testEnv
    .authenticatedContext("admin_uid_1", {
      email: "mdalaminmiabd2880@gmail.com",
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true })
    .firestore();
  const created = pastDate(5000);

  const payload = validPimsAccountPayload(created);

  // Standard user CANNOT create arbitrary PIMS accounts or self-assign SUPER_ADMIN
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pims_accounts")
      .doc("acc_unauth")
      .set(payload)
  );

  // Admin CAN create PIMS account under Alice's workspace
  await assertSucceeds(
    adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pims_accounts")
      .doc("acc_1")
      .set(payload)
  );

  // Standard owner CAN update their own non-privileged profile field when canEditSelfProfile == true
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pims_accounts")
      .doc("acc_1")
      .update({
        bloodGroup: "A+",
        updatedAt: pastDate(1000),
      })
  );

  // Standard owner CANNOT escalate role, active, canEditSelfProfile, or assignedRecordIds
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pims_accounts")
      .doc("acc_1")
      .update({
        role: "ADMIN",
        updatedAt: pastDate(900),
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pims_accounts")
      .doc("acc_1")
      .update({
        active: false,
        updatedAt: pastDate(900),
      })
  );

  // Standard owner CANNOT delete accounts (admin only)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pims_accounts")
      .doc("acc_1")
      .delete()
  );

  // Reject modifying immutable personalId or createdAt
  await assertFails(
    adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pims_accounts")
      .doc("acc_1")
      .update({
        personalId: "PIMS-HACKED",
        updatedAt: pastDate(500),
      })
  );
  await assertFails(
    adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pims_accounts")
      .doc("acc_1")
      .update({
        createdAt: pastDate(500),
        updatedAt: pastDate(500),
      })
  );

  // Reject @DocumentId 'id' inside doc body, invalid role, and future timestamp even as admin
  await assertFails(
    adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pims_accounts")
      .doc("acc_2")
      .set({ ...payload, id: "acc_2" })
  );
  await assertFails(
    adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pims_accounts")
      .doc("acc_3")
      .set({ ...payload, role: "ROOT_GOD" })
  );
  await assertFails(
    adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pims_accounts")
      .doc("acc_4")
      .set({ ...payload, createdAt: new Date(Date.now() + 60000) })
  );

  // Cross-user isolation
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("pims_accounts").get()
  );
});

test("Admin-only writes for Settings, Locations, Institutions, OrgPositions; standard user read-only & own Vault access", async () => {
  const adminDb = testEnv
    .authenticatedContext("admin_uid_2", {
      email: "mdalaminmiabd2880@gmail.com",
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const created = pastDate(5000);

  const settingsPayload = {
    appNameBn: "ব্যক্তিগত তথ্য",
    appNameEn: "Personal Info",
    primaryColorHex: "#0D4A3E",
    accentColorHex: "#C9962B",
    headerText: "স্বাগতম",
    footerText: "সর্বস্বত্ব সংরক্ষিত",
    welcomeMessageBn: "নিরাপদ তথ্য ব্যবস্থাপনা",
    welcomeMessageEn: "Secure PIMS",
    contactPhone: "01700000000",
    contactEmail: "admin@pims.bd",
    logoBase64: "",
    showStatsSection: true,
    showQuickActionsSection: true,
    showPrivacyNotice: true,
    mandatoryFields: ["fullNameBn", "mobileNumber"],
    createdAt: created,
    updatedAt: created,
  };
  const locationPayload = {
    level: "DIVISION",
    parentId: "",
    nameBn: "ঢাকা",
    nameEn: "Dhaka",
    code: "30",
    createdAt: created,
    updatedAt: created,
  };
  const institutionPayload = {
    nameBn: "ঢাকা কলেজ",
    nameEn: "Dhaka College",
    institutionType: "COLLEGE",
    eiinCode: "108123",
    divisionId: "div_dhaka",
    districtId: "dist_dhaka",
    upazilaId: "",
    unionId: "",
    wardId: "",
    locationLabel: "ঢাকা > ঢাকা",
    createdAt: created,
    updatedAt: created,
  };
  const orgPayload = {
    levelNameBn: "কেন্দ্রীয়",
    levelNameEn: "Central",
    designationBn: "পরিচালক",
    designationEn: "Director",
    rankOrder: 1,
    createdAt: created,
    updatedAt: created,
  };

  // Standard user CANNOT write settings, locations, institutions, or org_positions
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("website").set(settingsPayload)
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("locations").doc("div_dhaka").set(locationPayload)
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("institutions").doc("inst_1").set(institutionPayload)
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("org_positions").doc("pos_1").set(orgPayload)
  );

  // Admin CAN write settings, locations, institutions, and org_positions
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).collection("settings").doc("website").set(settingsPayload)
  );
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).collection("locations").doc("div_dhaka").set(locationPayload)
  );
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).collection("institutions").doc("inst_1").set(institutionPayload)
  );
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).collection("org_positions").doc("pos_1").set(orgPayload)
  );

  // Standard owner CAN read master datasets in their own workspace
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("locations").get());

  // Standard owner CAN read/write their own personal VaultEntry
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("vault_entries")
      .doc("v_1")
      .set({
        ownerPersonalId: "PIMS-2026-0001",
        category: "PRIVATE_NOTE",
        title: "গোপন নোট",
        ciphertextBase64: "ZW5jcnlwdGVk",
        ivBase64: "MTIzNDU2Nzg5MDEy",
        saltBase64: "c2FsdDEyMzQ1Njc4",
        createdAt: created,
        updatedAt: created,
      })
  );

  // ActivityLog (append-only, updates rejected, non-admin delete rejected)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("activity_logs")
      .doc("log_1")
      .set({
        actorPersonalId: "PIMS-2026-0001",
        actorRole: "SUPER_ADMIN",
        actionType: "LOGIN",
        targetPersonalId: "PIMS-2026-0001",
        summaryBn: "সফল লগইন",
        summaryEn: "Login succeeded",
        status: "SUCCESS",
        createdAt: created,
      })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("activity_logs").doc("log_1").delete()
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("activity_logs").doc("log_1").update({ status: "FAILED" })
  );
});

test("RBAC: only verified admins can write /admins, enumerate/delete /users, or delete audit logs; email spoofing and self-promotion rejected", async () => {
  const created = pastDate(5000);
  const superAdminDb = testEnv
    .authenticatedContext("super_admin_uid", {
      email: "mdalaminmiabd2880@gmail.com",
      email_verified: true,
    })
    .firestore();

  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "mdalaminmiabd2880@gmail.com",
      email_verified: false,
    })
    .firestore();

  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();

  const delegatedAdminUid = "delegated_admin_uid";
  const delegatedAdminDb = testEnv
    .authenticatedContext(delegatedAdminUid, {
      email: "delegated@example.com",
      email_verified: true,
    })
    .firestore();

  const disabledAdminUid = "disabled_admin_uid";
  const disabledAdminDb = testEnv
    .authenticatedContext(disabledAdminUid, {
      email: "disabled@example.com",
      email_verified: true,
    })
    .firestore();

  // 1. Non-admin CANNOT self-promote to /admins
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      role: "SUPER_ADMIN",
      active: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // 2. Spoofed admin (email matches but email_verified == false) CANNOT write /admins
  await assertFails(
    spoofedAdminDb.collection("admins").doc(delegatedAdminUid).set({
      userId: delegatedAdminUid,
      email: "delegated@example.com",
      role: "ADMIN",
      active: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // 3. Bootstrapped Super Admin (email_verified == true) CAN create active and disabled admin entries
  await assertSucceeds(
    superAdminDb.collection("admins").doc(delegatedAdminUid).set({
      userId: delegatedAdminUid,
      email: "delegated@example.com",
      role: "ADMIN",
      active: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    superAdminDb.collection("admins").doc(disabledAdminUid).set({
      userId: disabledAdminUid,
      email: "disabled@example.com",
      role: "ADMIN",
      active: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // 4. Alice creates her own workspace doc and audit log
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      workspaceInitialized: true,
      nextIdSequence: 1,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("activity_logs")
      .doc("log_alice")
      .set({
        actorPersonalId: "BD-00001",
        actorRole: "NORMAL_USER",
        actionType: "LOGIN",
        targetPersonalId: "BD-00001",
        summaryBn: "লগইন",
        summaryEn: "Login",
        status: "SUCCESS",
        createdAt: created,
      })
  );

  // 5. Alice CANNOT inject ghost role field into her own doc
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "SUPER_ADMIN",
      updatedAt: pastDate(1000),
    })
  );

  // 6. Alice CANNOT list all /users unfiltered, CAN query own doc, CANNOT delete own root doc
  await assertFails(aliceDb.collection("users").get());
  await assertSucceeds(
    aliceDb.collection("users").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).delete());

  // 7. Disabled admin CANNOT read/modify Alice's doc or delete logs
  await assertFails(disabledAdminDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    disabledAdminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("activity_logs")
      .doc("log_alice")
      .delete()
  );

  // 8. Active delegated admin CAN read Alice's doc & delete audit log, but CANNOT read Alice's private encrypted vault
  await assertSucceeds(delegatedAdminDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    delegatedAdminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("activity_logs")
      .doc("log_alice")
      .delete()
  );
  await assertFails(
    delegatedAdminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("vault_entries")
      .get()
  );

  // 9. Bootstrapped admin CAN enumerate /users and delete user doc
  await assertSucceeds(superAdminDb.collection("users").get());
  await assertSucceeds(superAdminDb.collection("users").doc(ALICE_UID).delete());
});

