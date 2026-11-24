const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    profileName: "Living Room 4K",
    activeProfile: true,
    nuvioUsername: "alice_cine",
    nuvioEmail: "alice@example.com",
    nuvioPasswordHint: "StrongPass!2026",
    nuvioAccountCreated: false,
    realDebridApiKey: "",
    realDebridAccountReady: false,
    realDebridVerified: false,
    realDebridUserLabel: "",
    realDebridPremiumDays: 0,
    tmdbApiKey: "",
    tmdbVerified: false,
    mdblistApiKey: "",
    rpdbApiKey: "",
    aioStreamsInstanceUrl: "https://aiostreams.elfhosted.com",
    aioStreamsPassword: "",
    enabledAddons: ["torrentio", "mediafusion", "comet"],
    customStreamSources: [],
    preferredResolutions: ["2160p", "1080p"],
    cachedOnly: true,
    excludeCam: true,
    preferDolbyVision: true,
    maxFileSizeGb: 35,
    formatterPreset: "PRISM",
    aioStreamsManifestUrl: "",
    aioStreamsVerified: false,
    aioMetadataInstanceUrl: "https://aiometadata.elfhosted.com",
    movieProvider: "TMDB",
    seriesProvider: "TMDB",
    animeProvider: "Kitsu",
    rpdbPostersEnabled: false,
    animeCatalogsEnabled: true,
    aioMetadataManifestUrl: "",
    aioMetadataVerified: false,
    nuvioMetaInstalled: false,
    nuvioStreamsInstalled: false,
    nuvioOrderVerified: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validCatalogPayload(uid, profileId, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    profileId,
    title: "Trending Movies",
    mediaType: "Movies",
    itemKind: "CATALOG",
    sourceProvider: "TMDB",
    sourceReference: "trending/movie/week",
    description: "Weekly trending theatrical and streaming hits.",
    enabled: true,
    sortOrder: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
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

test("Unauthenticated users cannot read or write profiles or catalogs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/profiles`).get());
  await assertFails(
    unauthDb
      .doc(`users/${ALICE_UID}/profiles/profile_main`)
      .set(validProfilePayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/catalogs`).get());
});

test("Owner can create, list, update (action-based), and delete their profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.doc(`users/${ALICE_UID}/profiles/profile_main`);

  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/profiles`).get());

  // Action: Update Nuvio Account Credentials
  await assertSucceeds(
    profileRef.update({
      nuvioUsername: "alice_nuvio_pro",
      nuvioAccountCreated: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Action: Update Real-Debrid & API Keys Vault
  await assertSucceeds(
    profileRef.update({
      realDebridApiKey: "RD_TEST_TOKEN_12345",
      realDebridAccountReady: true,
      realDebridVerified: true,
      realDebridUserLabel: "alice_rd (premium)",
      realDebridPremiumDays: 180,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Action: Update AIOStreams single-config hub + custom stream sources
  await assertSucceeds(
    profileRef.update({
      enabledAddons: ["torrentio", "mediafusion", "comet", "jackettio"],
      customStreamSources: ["https://custom-addon.example/manifest.json"],
      formatterPreset: "MINIMALISTIC",
      maxFileSizeGb: 50,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Action: Update AIOMetadata settings
  await assertSucceeds(
    profileRef.update({
      movieProvider: "IMDb",
      seriesProvider: "TVDB",
      rpdbPostersEnabled: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(profileRef.delete());
});

test("Profile rejects shadow fields, @DocumentId id field, immutable mutations, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.doc(`users/${ALICE_UID}/profiles/profile_main`);

  // Reject extra `id` key
  await assertFails(
    profileRef.set(validProfilePayload(ALICE_UID, { id: "profile_main" }))
  );

  // Reject shadow field
  await assertFails(
    profileRef.set(validProfilePayload(ALICE_UID, { isAdmin: true }))
  );

  // Reject future timestamp
  await assertFails(
    profileRef.set(
      validProfilePayload(ALICE_UID, {
        createdAt: new Date(Date.now() + 60000),
      })
    )
  );

  // Create valid doc first
  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID)));

  // Reject mutating createdAt
  await assertFails(
    profileRef.update({
      profileName: "Changed",
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject invalid enum value
  await assertFails(
    profileRef.update({
      formatterPreset: "INVALID_FORMAT",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's profiles and catalogs", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db
      .doc(`users/${ALICE_UID}/profiles/profile_main`)
      .set(validProfilePayload(ALICE_UID));
    await db
      .doc(`users/${ALICE_UID}/catalogs/cat_1`)
      .set(validCatalogPayload(ALICE_UID, "profile_main"));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/profiles/profile_main`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/profiles`).get());
  await assertFails(
    bobDb
      .doc(`users/${ALICE_UID}/profiles/profile_main`)
      .update({ profileName: "Hijacked", updatedAt: new Date(Date.now() - 500) })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/catalogs/cat_1`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/catalogs`).get());
});

test("Catalogs enforce parent profile existence, valid mutations, and reject orphaned writes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Orphaned catalog without parent profile must fail
  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/catalogs/cat_orphan`)
      .set(validCatalogPayload(ALICE_UID, "nonexistent_profile"))
  );

  // Create parent profile first
  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/profiles/profile_main`)
      .set(validProfilePayload(ALICE_UID))
  );

  const catRef = aliceDb.doc(`users/${ALICE_UID}/catalogs/cat_1`);
  await assertSucceeds(
    catRef.set(validCatalogPayload(ALICE_UID, "profile_main"))
  );

  // List catalogs succeeds for owner
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/catalogs`).get());

  // Reorder update succeeds
  await assertSucceeds(
    catRef.update({
      sortOrder: 2,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Toggle enabled succeeds
  await assertSucceeds(
    catRef.update({
      enabled: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject mutating profileId
  await assertFails(
    catRef.update({
      profileId: "other_profile",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(catRef.delete());
});
