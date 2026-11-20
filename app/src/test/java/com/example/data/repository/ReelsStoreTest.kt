package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReelsStoreTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("instaclone_user_reels_v1", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `initializes user community reels without any automated bot accounts`() {
        val store = ReelsStore(context)
        val reels = store.uiState.value.reels

        assertTrue(reels.isNotEmpty())
        assertTrue(reels.none { it.username.endsWith("_ai") })
    }

    @Test
    fun `like comment follow and publish edited reel persist cleanly`() {
        val store = ReelsStore(context)
        val targetReel = store.uiState.value.reels.first()

        val initialLikes = targetReel.likesCount
        store.toggleLikeReel(targetReel.id)
        val likedReel = store.uiState.value.reels.first { it.id == targetReel.id }
        assertEquals(!targetReel.isLiked, likedReel.isLiked)
        val expectedLikes = if (likedReel.isLiked) initialLikes + 1L else initialLikes - 1L
        assertEquals(expectedLikes, likedReel.likesCount)

        store.addComment(targetReel.id, "tester", "Clean cinematography!")
        val commentedReel = store.uiState.value.reels.first { it.id == targetReel.id }
        assertTrue(commentedReel.comments.any { it.text == "Clean cinematography!" && it.author == "tester" })

        val beforeFollow = targetReel.isFollowing
        store.toggleFollowCreator(targetReel.username)
        assertEquals(
            !beforeFollow,
            store.uiState.value.reels.first { it.username == targetReel.username }.isFollowing
        )

        val published = store.publishEditedReel(
            authorId = "uid_123",
            username = "creator_user",
            displayName = "Creator User",
            avatarUrl = "",
            videoUrl = ReelsStore.STUDIO_MEDIA_CLIPS.first().videoUrl,
            posterUrl = ReelsStore.STUDIO_MEDIA_CLIPS.first().posterUrl,
            caption = "Edited in studio #test",
            audioTrack = "Studio Track",
            filterPreset = "Cinema",
            playbackSpeed = 1.25f,
            trimStartPct = 5f,
            trimEndPct = 95f,
            overlayText = "HELLO"
        )
        assertEquals(published.id, store.uiState.value.reels.first().id)

        val reloaded = ReelsStore(context)
        assertTrue(reloaded.uiState.value.reels.any { it.id == published.id && it.overlayText == "HELLO" })
    }
}
