const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

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

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("profiles").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profiles")
      .doc("p1")
      .set({
        userId: ALICE_UID,
        name: "Alice",
        avatarId: "crimson_fox",
        maturityLevel: "ALL",
        preferredGenres: ["Sci-Fi"],
        autoplayNext: true,
        createdAt: now,
        updatedAt: now,
      })
  );
});

test("UserAccount: owner CRUD succeeds, cross-user & ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Streamer",
      email: "alice@zenix.website",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update preserving createdAt succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Cinema",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/write fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hijack" })
  );

  // Ghost field / role escalation fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Unfiltered list on /users fails
  await assertFails(aliceDb.collection("users").get());
});

test("ViewerProfile: create, list, partial update, validation bounds & cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Empty list query succeeds for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("profiles").get()
  );

  // Create valid profile (no @DocumentId `id` field in payload)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profiles")
      .doc("prof_main")
      .set({
        userId: ALICE_UID,
        name: "Alice",
        avatarId: "crimson_fox",
        maturityLevel: "ALL",
        preferredGenres: ["Sci-Fi", "Cyberpunk"],
        autoplayNext: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profiles")
      .doc("prof_main")
      .update({
        name: "Alice 4K",
        maturityLevel: "KIDS",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject `@DocumentId` id field on create
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profiles")
      .doc("prof_bad_id")
      .set({
        id: "prof_bad_id",
        userId: ALICE_UID,
        name: "Bad",
        avatarId: "crimson_fox",
        maturityLevel: "ALL",
        preferredGenres: ["Sci-Fi"],
        autoplayNext: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject invalid maturityLevel enum & future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profiles")
      .doc("prof_bad_enum")
      .set({
        userId: ALICE_UID,
        name: "Invalid",
        avatarId: "crimson_fox",
        maturityLevel: "UNRATED",
        preferredGenres: [],
        autoplayNext: false,
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profiles")
      .doc("prof_future")
      .set({
        userId: ALICE_UID,
        name: "Future",
        avatarId: "crimson_fox",
        maturityLevel: "ALL",
        preferredGenres: [],
        autoplayNext: true,
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );

  // Cross-user access denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("profiles").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profiles")
      .doc("prof_main")
      .delete()
  );
});

test("Watchlist, WatchHistory, and Ratings: parent gate, queries, and mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 8000);

  // Parent profile must exist before adding watchlist/history/ratings
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profiles")
      .doc("missing_prof")
      .collection("watchlist")
      .doc("solaris_drift")
      .set({
        userId: ALICE_UID,
        profileId: "missing_prof",
        contentId: "solaris_drift",
        title: "Solaris Drift",
        genre: "Sci-Fi",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Create parent profile
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profiles")
      .doc("prof_1")
      .set({
        userId: ALICE_UID,
        name: "Main",
        avatarId: "cyber_visor",
        maturityLevel: "ALL",
        preferredGenres: ["Sci-Fi"],
        autoplayNext: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  const profRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("profiles")
    .doc("prof_1");

  // Watchlist create, list, delete
  await assertSucceeds(
    profRef.collection("watchlist").doc("solaris_drift").set({
      userId: ALICE_UID,
      profileId: "prof_1",
      contentId: "solaris_drift",
      title: "Solaris Drift",
      genre: "Sci-Fi",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(profRef.collection("watchlist").get());

  // WatchHistory create, update progress, list
  await assertSucceeds(
    profRef.collection("watchHistory").doc("solaris_drift").set({
      userId: ALICE_UID,
      profileId: "prof_1",
      contentId: "solaris_drift",
      title: "Solaris Drift",
      genre: "Sci-Fi",
      episodeId: "s1e1",
      positionMs: 12000,
      durationMs: 734000,
      progressPercent: 2,
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    profRef.collection("watchHistory").doc("solaris_drift").update({
      positionMs: 120000,
      progressPercent: 16,
      completed: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject out-of-range progressPercent (>100)
  await assertFails(
    profRef.collection("watchHistory").doc("solaris_drift").update({
      progressPercent: 150,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ratings create, update, list
  await assertSucceeds(
    profRef.collection("ratings").doc("solaris_drift").set({
      userId: ALICE_UID,
      profileId: "prof_1",
      contentId: "solaris_drift",
      sentiment: "LIKE",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    profRef.collection("ratings").doc("solaris_drift").update({
      sentiment: "DISLIKE",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(profRef.collection("ratings").get());

  // Cross-user isolation on subcollections
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("profiles")
      .doc("prof_1")
      .collection("watchHistory")
      .get()
  );
});
