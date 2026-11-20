package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.Reel
import com.example.data.model.ReelComment
import com.example.data.model.ReelsUiState
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ReelsStore(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private var userReels: MutableList<Reel> = mutableListOf()
    private var friendReels: MutableList<Reel> = mutableListOf()

    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    init {
        initialize()
    }

    @Synchronized
    fun initialize() {
        loadFromPrefs()
        if (friendReels.isEmpty()) {
            friendReels = createDefaultCommunityReels().toMutableList()
        }
        persistAndEmit()
    }

    @Synchronized
    fun toggleLikeReel(reelId: String) {
        var updated = false
        userReels = userReels.map { reel ->
            if (reel.id == reelId) {
                updated = true
                val nextLiked = !reel.isLiked
                val nextCount = if (nextLiked) reel.likesCount + 1L else (reel.likesCount - 1L).coerceAtLeast(0L)
                reel.copy(isLiked = nextLiked, likesCount = nextCount)
            } else reel
        }.toMutableList()

        if (!updated) {
            friendReels = friendReels.map { reel ->
                if (reel.id == reelId) {
                    val nextLiked = !reel.isLiked
                    val nextCount = if (nextLiked) reel.likesCount + 1L else (reel.likesCount - 1L).coerceAtLeast(0L)
                    reel.copy(isLiked = nextLiked, likesCount = nextCount)
                } else reel
            }.toMutableList()
        }
        persistAndEmit()
    }

    @Synchronized
    fun addComment(reelId: String, author: String, text: String) {
        val trimmed = text.trim().take(400)
        if (trimmed.isEmpty()) return
        val newComment = ReelComment(
            id = "rc_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            author = author.ifBlank { "you" },
            text = trimmed,
            createdAtMillis = System.currentTimeMillis()
        )
        userReels = userReels.map { r ->
            if (r.id == reelId) r.copy(comments = r.comments + newComment) else r
        }.toMutableList()
        friendReels = friendReels.map { r ->
            if (r.id == reelId) r.copy(comments = r.comments + newComment) else r
        }.toMutableList()
        persistAndEmit()
    }

    @Synchronized
    fun toggleFollowCreator(targetUsername: String) {
        val currentFollowing = (userReels + friendReels)
            .firstOrNull { it.username == targetUsername }
            ?.isFollowing ?: false
        val nextFollowing = !currentFollowing

        userReels = userReels.map { r ->
            if (r.username == targetUsername) r.copy(isFollowing = nextFollowing) else r
        }.toMutableList()
        friendReels = friendReels.map { r ->
            if (r.username == targetUsername) r.copy(isFollowing = nextFollowing) else r
        }.toMutableList()
        persistAndEmit()
    }

    @Synchronized
    fun publishEditedReel(
        authorId: String,
        username: String,
        displayName: String,
        avatarUrl: String,
        videoUrl: String,
        posterUrl: String,
        caption: String,
        audioTrack: String,
        filterPreset: String,
        playbackSpeed: Float,
        trimStartPct: Float,
        trimEndPct: Float,
        overlayText: String
    ): Reel {
        val created = Reel(
            id = "reel_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
            authorId = authorId,
            username = username.ifBlank { "creator" },
            displayName = displayName.ifBlank { "Creator" },
            avatarUrl = avatarUrl,
            videoUrl = videoUrl.ifBlank { STUDIO_MEDIA_CLIPS.first().videoUrl },
            posterUrl = posterUrl.ifBlank { STUDIO_MEDIA_CLIPS.first().posterUrl },
            caption = caption.trim().ifBlank { "Created in InstaClone Reels Studio #reels" },
            audioTrack = audioTrack.ifBlank { "Original Studio Audio" },
            niche = "Creator Studio",
            likesCount = 1L,
            isLiked = false,
            isFollowing = true,
            createdAtMillis = System.currentTimeMillis(),
            filterPreset = filterPreset,
            playbackSpeed = playbackSpeed,
            trimStartPct = trimStartPct,
            trimEndPct = trimEndPct,
            overlayText = overlayText.trim(),
            comments = emptyList()
        )
        userReels = (listOf(created) + userReels).toMutableList()
        persistAndEmit()
        return created
    }

    private fun persistAndEmit() {
        saveToPrefs()
        val combined = buildCombinedReels()
        _uiState.value = ReelsUiState(reels = combined)
    }

    private fun buildCombinedReels(): List<Reel> {
        val merged = userReels + friendReels
        val seen = HashSet<String>(merged.size)
        return merged.filter { seen.add(it.id) }
    }

    private fun saveToPrefs() {
        val userArr = JSONArray()
        userReels.forEach { r -> userArr.put(reelToJson(r)) }

        val friendArr = JSONArray()
        friendReels.forEach { r -> friendArr.put(reelToJson(r)) }

        prefs.edit()
            .putString(KEY_USER_REELS, userArr.toString())
            .putString(KEY_FRIEND_REELS, friendArr.toString())
            .apply()
    }

    private fun loadFromPrefs() {
        val userJson = prefs.getString(KEY_USER_REELS, null)
        userReels = if (!userJson.isNullOrBlank()) {
            runCatching { parseReelsArray(JSONArray(userJson)).toMutableList() }
                .getOrElse { mutableListOf() }
        } else {
            mutableListOf()
        }

        val friendJson = prefs.getString(KEY_FRIEND_REELS, null)
        friendReels = if (!friendJson.isNullOrBlank()) {
            runCatching { parseReelsArray(JSONArray(friendJson)).toMutableList() }
                .getOrElse { createDefaultCommunityReels().toMutableList() }
        } else {
            createDefaultCommunityReels().toMutableList()
        }
    }

    private fun reelToJson(r: Reel): JSONObject {
        val commentsArr = JSONArray()
        r.comments.forEach { c ->
            commentsArr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("author", c.author)
                    put("text", c.text)
                    put("createdAtMillis", c.createdAtMillis)
                }
            )
        }
        return JSONObject().apply {
            put("id", r.id)
            put("authorId", r.authorId)
            put("username", r.username)
            put("displayName", r.displayName)
            put("avatarUrl", r.avatarUrl)
            put("videoUrl", r.videoUrl)
            put("posterUrl", r.posterUrl)
            put("caption", r.caption)
            put("audioTrack", r.audioTrack)
            put("niche", r.niche)
            put("likesCount", r.likesCount)
            put("isLiked", r.isLiked)
            put("isFollowing", r.isFollowing)
            put("createdAtMillis", r.createdAtMillis)
            put("filterPreset", r.filterPreset)
            put("playbackSpeed", r.playbackSpeed.toDouble())
            put("trimStartPct", r.trimStartPct.toDouble())
            put("trimEndPct", r.trimEndPct.toDouble())
            put("overlayText", r.overlayText)
            put("comments", commentsArr)
        }
    }

    private fun parseReelsArray(arr: JSONArray): List<Reel> {
        val parsed = ArrayList<Reel>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val commentsJson = o.optJSONArray("comments") ?: JSONArray()
            val parsedComments = ArrayList<ReelComment>(commentsJson.length())
            for (j in 0 until commentsJson.length()) {
                val c = commentsJson.getJSONObject(j)
                parsedComments.add(
                    ReelComment(
                        id = c.optString("id", "rc_$j"),
                        author = c.optString("author", "user"),
                        text = c.optString("text", ""),
                        createdAtMillis = c.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
            parsed.add(
                Reel(
                    id = o.getString("id"),
                    authorId = o.optString("authorId", o.getString("username")),
                    username = o.getString("username"),
                    displayName = o.optString("displayName", o.getString("username")),
                    avatarUrl = o.optString("avatarUrl", ""),
                    videoUrl = o.getString("videoUrl"),
                    posterUrl = o.optString("posterUrl", ""),
                    caption = o.optString("caption", ""),
                    audioTrack = o.optString("audioTrack", "Original Audio"),
                    niche = o.optString("niche", "Reels"),
                    likesCount = o.optLong("likesCount", 0L),
                    isLiked = o.optBoolean("isLiked", false),
                    isFollowing = o.optBoolean("isFollowing", false),
                    createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                    filterPreset = o.optString("filterPreset", "Original"),
                    playbackSpeed = o.optDouble("playbackSpeed", 1.0).toFloat(),
                    trimStartPct = o.optDouble("trimStartPct", 0.0).toFloat(),
                    trimEndPct = o.optDouble("trimEndPct", 100.0).toFloat(),
                    overlayText = o.optString("overlayText", ""),
                    comments = parsedComments
                )
            )
        }
        return parsed
    }

    data class StudioClipItem(
        val label: String,
        val videoUrl: String,
        val posterUrl: String
    )

    companion object {
        private const val PREFS_NAME = "instaclone_user_reels_v1"
        private const val KEY_USER_REELS = "user_edited_reels_json"
        private const val KEY_FRIEND_REELS = "community_reels_json"

        val STUDIO_MEDIA_CLIPS: List<StudioClipItem> = listOf(
            StudioClipItem(
                label = "Coastal Surf",
                videoUrl = "https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-576p.mp4",
                posterUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800"
            ),
            StudioClipItem(
                label = "Deep Ocean",
                videoUrl = "https://vjs.zencdn.net/v/oceans.mp4",
                posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800"
            ),
            StudioClipItem(
                label = "Spring Bloom",
                videoUrl = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                posterUrl = "https://images.unsplash.com/photo-1490750967868-88aa4486c946?w=800"
            )
        )

        private fun createDefaultCommunityReels(): List<Reel> {
            val now = System.currentTimeMillis()
            return listOf(
                Reel(
                    id = "friend-reel-1",
                    authorId = "sara_vlogs",
                    username = "sara_vlogs",
                    displayName = "Sara Ali",
                    avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
                    videoUrl = "https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-576p.mp4",
                    posterUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800",
                    caption = "Chasing waves with friends #ocean #surf #weekendvibes",
                    audioTrack = "Original Soundtrack • Ocean Chill Remix",
                    niche = "Travel",
                    likesCount = 1840L,
                    isLiked = false,
                    isFollowing = false,
                    createdAtMillis = now - 2L * 3600_000L,
                    comments = listOf(
                        ReelComment("fc_1", "mohit_v", "Epic wave shot!", now - 3600_000L)
                    )
                ),
                Reel(
                    id = "friend-reel-2",
                    authorId = "mohit_v",
                    username = "mohit_v",
                    displayName = "Mohit Verma",
                    avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    videoUrl = "https://vjs.zencdn.net/v/oceans.mp4",
                    posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800",
                    caption = "Crystal blue waters and marine life #scubadiving #travel",
                    audioTrack = "Underwater Symphony • Lo-Fi Mix",
                    niche = "Adventure",
                    likesCount = 3420L,
                    isLiked = true,
                    isFollowing = true,
                    createdAtMillis = now - 5L * 3600_000L,
                    comments = listOf(
                        ReelComment("fc_2", "aarav_travels", "Need to go diving here next month!", now - 1800_000L)
                    )
                )
            )
        }

        fun formatCount(num: Long): String =
            if (num >= 1000L) {
                val k = num / 1000.0
                val text = String.format(Locale.US, "%.1f", k).removeSuffix(".0")
                "${text}K"
            } else {
                num.toString()
            }
    }
}
