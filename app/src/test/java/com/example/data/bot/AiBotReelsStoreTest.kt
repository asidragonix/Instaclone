package com.example.data.bot

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiBotReelsStoreTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("instaclone_ai_bot_reels_v1", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `initializes 100 bots with strictly 1 active reel per bot`() {
        val store = AiBotReelsStore(context)
        val state = store.uiState.value

        assertEquals(100, state.bots.size)
        assertEquals(100, state.botReelsCount)

        val botReels = state.reels.filter { it.isAiBot }
        assertEquals(100, botReels.size)
        assertEquals(100, botReels.mapNotNull { it.botId }.toSet().size)
        assertTrue(state.isWatchdogActive)
    }

    @Test
    fun `simulateNextDay24h deletes old reels and uploads 100 fresh replacement reels`() {
        val store = AiBotReelsStore(context)
        val beforeIds = store.uiState.value.reels
            .filter { it.isAiBot }
            .associateBy({ it.botId!! }, { it.id })

        val replacedCount = store.simulateNextDay24h()
        assertEquals(100, replacedCount)

        val afterState = store.uiState.value
        assertEquals(100, afterState.botReelsCount)
        val afterReels = afterState.reels.filter { it.isAiBot }
        assertEquals(100, afterReels.size)

        afterReels.forEach { reel ->
            val oldId = beforeIds[reel.botId!!]
            assertNotEquals(oldId, reel.id)
            assertEquals(2, reel.cycleNumber)
        }
    }

    @Test
    fun `single bot replace like comment follow and studio publish work and persist`() {
        val store = AiBotReelsStore(context)
        val firstBot = store.uiState.value.bots.first()
        val oldBotReel = store.uiState.value.reels.first { it.botId == firstBot.id }

        store.replaceSingleBotReel(firstBot.id)
        val replacedBotReel = store.uiState.value.reels.first { it.botId == firstBot.id }
        assertNotEquals(oldBotReel.id, replacedBotReel.id)
        assertEquals(100, store.uiState.value.botReelsCount)

        val initialLikes = replacedBotReel.likesCount
        store.toggleLikeReel(replacedBotReel.id)
        val likedReel = store.uiState.value.reels.first { it.id == replacedBotReel.id }
        assertTrue(likedReel.isLiked)
        assertEquals(initialLikes + 1L, likedReel.likesCount)

        store.addComment(replacedBotReel.id, "tester", "Awesome 24h bot drop!")
        val commentedReel = store.uiState.value.reels.first { it.id == replacedBotReel.id }
        assertTrue(commentedReel.comments.any { it.text == "Awesome 24h bot drop!" && it.author == "tester" })

        val beforeFollow = firstBot.isFollowing
        store.toggleFollowBot(firstBot.id, firstBot.username)
        assertEquals(!beforeFollow, store.uiState.value.bots.first { it.id == firstBot.id }.isFollowing)

        val published = store.publishEditedReel(
            username = "creator_user",
            displayName = "Creator User",
            avatarUrl = "",
            videoUrl = AiBotReelsStore.MEDIA_CATALOG.first().videoUrl,
            posterUrl = AiBotReelsStore.MEDIA_CATALOG.first().posterUrl,
            caption = "Edited in studio #test",
            audioTrack = "Studio Track",
            filterPreset = "Cinema",
            playbackSpeed = 1.25f,
            trimStartPct = 5f,
            trimEndPct = 95f,
            overlayText = "HELLO"
        )
        assertEquals(published.id, store.uiState.value.reels.first().id)

        // Reload new store instance to verify SharedPreferences persistence
        val reloaded = AiBotReelsStore(context)
        assertEquals(100, reloaded.uiState.value.botReelsCount)
        assertTrue(reloaded.uiState.value.reels.any { it.id == published.id && it.overlayText == "HELLO" })
    }
}
