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
const EVE_UID = "eve_789";

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

function serverTimestamp() {
  return new Date(Date.now() - 1000);
}

async function seedProfiles() {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(aliceDb.collection("profiles").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice.lens",
      displayName: "Alice Rivera",
      bio: "Street photographer",
      avatarData: "",
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      visibility: "public",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    bobDb.collection("profiles").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob.studio",
      displayName: "Bob Chen",
      bio: "Visual designer",
      avatarData: "",
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      visibility: "public",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
}

test("Unauthenticated user cannot read profiles or posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("profiles").where("visibility", "==", "public").get()
  );
  await assertFails(
    unauthDb.collection("posts").where("visibility", "==", "public").get()
  );
});

test("Shadow Update Test: profile creation with ghost field isVerified fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice.lens",
      displayName: "Alice",
      bio: "Bio",
      avatarData: "",
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      visibility: "public",
      isVerified: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Identity Spoofing Test: Bob cannot create or edit Alice profile", async () => {
  await seedProfiles();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("profiles").doc(ALICE_UID).update({
      bio: "Hacked bio",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Private post isolation and aligned public feed query", async () => {
  await seedProfiles();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_pub_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice.lens",
      authorAvatarData: "",
      imageData: "https://images.unsplash.com/photo-1",
      caption: "Golden hour",
      location: "Tokyo",
      likesCount: 0,
      commentsCount: 0,
      lastCommentId: "",
      visibility: "public",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_priv_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice.lens",
      authorAvatarData: "",
      imageData: "https://images.unsplash.com/photo-2",
      caption: "Private draft",
      location: "",
      likesCount: 0,
      commentsCount: 0,
      lastCommentId: "",
      visibility: "private",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Aligned public query succeeds
  await assertSucceeds(
    bobDb.collection("posts").where("visibility", "==", "public").get()
  );

  // Unfiltered query without visibility/authorId filter fails
  await assertFails(bobDb.collection("posts").get());

  // Bob cannot get Alice's private post
  await assertFails(bobDb.collection("posts").doc("post_priv_1").get());
});

test("Atomic Like and Comment WriteBatch succeeds; spoofed likesCount fails", async () => {
  await seedProfiles();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice.lens",
      authorAvatarData: "",
      imageData: "https://images.unsplash.com/photo-1",
      caption: "Sunset",
      location: "Kyoto",
      likesCount: 0,
      commentsCount: 0,
      lastCommentId: "",
      visibility: "public",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob cannot increment likesCount without creating the corresponding like doc
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      updatedAt: serverTimestamp(),
    })
  );

  // Atomic batch like succeeds
  const likeBatch = bobDb.batch();
  likeBatch.set(bobDb.collection("likes").doc(`post_1_${BOB_UID}`), {
    postId: "post_1",
    userId: BOB_UID,
    createdAt: serverTimestamp(),
  });
  likeBatch.update(bobDb.collection("posts").doc("post_1"), {
    likesCount: 1,
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(likeBatch.commit());

  // Cross-user likes list isolation
  await assertSucceeds(
    bobDb.collection("likes").where("userId", "==", BOB_UID).get()
  );
  await assertFails(
    aliceDb.collection("likes").where("userId", "==", BOB_UID).get()
  );

  // Atomic batch comment succeeds
  const commentBatch = bobDb.batch();
  commentBatch.set(bobDb.collection("comments").doc("comment_1"), {
    postId: "post_1",
    authorId: BOB_UID,
    authorUsername: "bob.studio",
    authorAvatarData: "",
    text: "Incredible lighting!",
    visibility: "public",
    createdAt: serverTimestamp(),
  });
  commentBatch.update(bobDb.collection("posts").doc("post_1"), {
    commentsCount: 1,
    lastCommentId: "comment_1",
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(commentBatch.commit());
});

test("Direct Messaging isolates conversations and messages strictly to participants", async () => {
  await seedProfiles();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  const convId = `${ALICE_UID}_${BOB_UID}`;
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("conversations").doc(convId), {
    participantIds: [ALICE_UID, BOB_UID],
    userOneId: ALICE_UID,
    userOneUsername: "alice.lens",
    userOneAvatarData: "",
    userTwoId: BOB_UID,
    userTwoUsername: "bob.studio",
    userTwoAvatarData: "",
    lastMessage: "Hey Bob!",
    lastSenderId: ALICE_UID,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  batch.set(
    aliceDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .doc("msg_1"),
    {
      conversationId: convId,
      participantIds: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      senderUsername: "alice.lens",
      text: "Hey Bob!",
      createdAt: serverTimestamp(),
    }
  );
  await assertSucceeds(batch.commit());

  // Eve (non-participant) cannot read conversation or messages
  await assertFails(eveDb.collection("conversations").doc(convId).get());
  await assertFails(
    eveDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .where("participantIds", "array-contains", EVE_UID)
      .get()
  );

  // Alice can query her conversations and messages
  await assertSucceeds(
    aliceDb
      .collection("conversations")
      .where("participantIds", "array-contains", ALICE_UID)
      .get()
  );
  await assertSucceeds(
    aliceDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .where("participantIds", "array-contains", ALICE_UID)
      .get()
  );

  // Attachment + reply message succeeds for sender; participant can react; only sender can delete
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const msg2Ref = aliceDb.collection("conversations").doc(convId).collection("messages").doc("msg_2");
  await assertSucceeds(
    msg2Ref.set({
      conversationId: convId,
      participantIds: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      senderUsername: "alice.lens",
      text: "Check out this photo!",
      attachmentType: "image",
      attachmentName: "sunset.jpg",
      attachmentSize: 2048,
      attachmentData: "data:image/jpeg;base64,abc",
      replyToId: "msg_1",
      replyToSender: "alice.lens",
      replyToText: "Hey Bob!",
      reactions: {},
      createdAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc(convId).collection("messages").doc("msg_2").update({
      reactions: { "❤️": 1 },
    })
  );
  await assertFails(
    bobDb.collection("conversations").doc(convId).collection("messages").doc("msg_2").delete()
  );
  await assertSucceeds(msg2Ref.delete());
});

test("Stories, atomic StoryLike/StoryComment, Highlights, and Calls enforce ownership and participant isolation", async () => {
  await seedProfiles();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  // Profile extended fields update succeeds for owner
  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).update({
      category: "Street Photographer",
      pronouns: "she/her",
      website: "https://alice.example.com",
      updatedAt: serverTimestamp(),
    })
  );

  // Create story succeeds for owner
  await assertSucceeds(
    aliceDb.collection("stories").doc("story_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice.lens",
      authorAvatarData: "",
      mediaData: "",
      mediaType: "text",
      bgGradient: "sunset",
      overlayText: "Golden Hour",
      sticker: "✨",
      caption: "Evening walk",
      likesCount: 0,
      commentsCount: 0,
      viewsCount: 0,
      lastCommentId: "",
      expiresAtMillis: Date.now() + 86400000,
      visibility: "public",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Atomic StoryLike succeeds
  const storyLikeBatch = bobDb.batch();
  storyLikeBatch.set(bobDb.collection("story_likes").doc(`story_1_${BOB_UID}`), {
    storyId: "story_1",
    userId: BOB_UID,
    createdAt: serverTimestamp(),
  });
  storyLikeBatch.update(bobDb.collection("stories").doc("story_1"), {
    likesCount: 1,
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(storyLikeBatch.commit());

  // Atomic StoryComment succeeds
  const storyCommentBatch = bobDb.batch();
  storyCommentBatch.set(bobDb.collection("story_comments").doc("sc_1"), {
    storyId: "story_1",
    authorId: BOB_UID,
    authorUsername: "bob.studio",
    authorAvatarData: "",
    text: "Love this mood!",
    visibility: "public",
    createdAt: serverTimestamp(),
  });
  storyCommentBatch.update(bobDb.collection("stories").doc("story_1"), {
    commentsCount: 1,
    lastCommentId: "sc_1",
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(storyCommentBatch.commit());

  // Bob cannot delete Alice's story
  await assertFails(bobDb.collection("stories").doc("story_1").delete());

  // Highlight creation succeeds for owner, fails for spoofing
  await assertSucceeds(
    aliceDb.collection("highlights").doc("hl_1").set({
      ownerId: ALICE_UID,
      title: "Travel",
      coverData: "preset://sunset_horizon",
      storyIds: ["story_1"],
      visibility: "public",
      createdAt: serverTimestamp(),
    })
  );
  await assertFails(bobDb.collection("highlights").doc("hl_1").delete());

  // Calls strictly isolated to participants
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").set({
      callerId: ALICE_UID,
      callerUsername: "alice.lens",
      callerAvatarData: "",
      calleeId: BOB_UID,
      calleeUsername: "bob.studio",
      calleeAvatarData: "",
      participantIds: [ALICE_UID, BOB_UID],
      callType: "video",
      status: "ringing",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(eveDb.collection("calls").doc("call_1").get());
  await assertSucceeds(
    bobDb.collection("calls").doc("call_1").update({
      status: "active",
      updatedAt: serverTimestamp(),
    })
  );
});

