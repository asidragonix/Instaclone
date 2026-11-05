package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class InstaCloneRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createPostLikeCommentAndDm_authenticatedUsers_succeedsAtomically() = runBlocking {
        val runTag = UUID.randomUUID().toString().take(6)
        val aliceUid = signInTestUser("alice_$runTag@test.com")
        val repo = InstaCloneRepository(firestore, auth)

        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile("Alice Rivera", "alice_$runTag", "").getOrThrow()
        }
        assertEquals(aliceUid, aliceProfile.userId)

        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createPost(
                authorUsername = aliceProfile.username,
                authorAvatarData = "",
                imageData = "https://images.unsplash.com/photo-1506744038136-46273834b3fb",
                caption = "Sunset ridge",
                location = "Yosemite",
                visibility = "public"
            ).getOrThrow()
        }
        assertTrue(postId.isNotEmpty())

        // Switch to Bob and engage (like + comment + DM)
        val bobUid = signInTestUser("bob_$runTag@test.com")
        val bobProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile("Bob Chen", "bob_$runTag", "").getOrThrow()
        }
        assertEquals(bobUid, bobProfile.userId)

        val likeRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.toggleLike(postId, currentlyLiked = false)
        }
        assertTrue(likeRes.isSuccess)

        val commentRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addComment(postId, bobProfile.username, "", "Amazing shot!")
        }
        assertTrue(commentRes.isSuccess)

        val dmRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.sendDirectMessage(bobProfile, aliceProfile, "Hey Alice, loved the Yosemite photo!")
        }
        assertTrue(dmRes.isSuccess)

        val feedPosts = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePublicFeedPosts().first { list -> list.any { it.id == postId && it.likesCount == 1L } }
        }
        assertTrue(feedPosts.any { it.id == postId && it.commentsCount == 1L })
    }

    @Test
    fun crossUserPrivatePostAndDmIsolation_failsWithPermissionDenied() = runBlocking {
        val runTag = UUID.randomUUID().toString().take(6)
        signInTestUser("alice_priv_$runTag@test.com")
        val repo = InstaCloneRepository(firestore, auth)
        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile("Alice", "alicep_$runTag", "").getOrThrow()
        }

        val privatePostId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createPost(
                authorUsername = aliceProfile.username,
                authorAvatarData = "",
                imageData = "https://images.unsplash.com/photo-1506744038136-46273834b3fb",
                caption = "Private draft",
                location = "",
                visibility = "private"
            ).getOrThrow()
        }

        signInTestUser("bob_priv_$runTag@test.com")
        val getResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getPostById(privatePostId)
        }
        assertTrue(getResult.isFailure)
        val firestoreEx = getResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun unauthenticatedCaller_observePublicFeedPosts_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = InstaCloneRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observePublicFeedPosts().first()
            }
            fail("Expected exception for unauthenticated caller")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
