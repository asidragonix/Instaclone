package com.example.data.bot

data class AiBotAccount(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val bio: String,
    val niche: String,
    val followersCount: Long,
    val isFollowing: Boolean = false
)

data class ReelComment(
    val id: String,
    val author: String,
    val text: String,
    val createdAtMillis: Long
)

data class BotReel(
    val id: String,
    val botId: String?,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val videoUrl: String,
    val posterUrl: String,
    val caption: String,
    val audioTrack: String,
    val niche: String,
    val isAiBot: Boolean,
    val cycleNumber: Int = 1,
    val likesCount: Long,
    val isLiked: Boolean = false,
    val isFollowing: Boolean = false,
    val createdAtMillis: Long,
    val filterPreset: String = "Original",
    val playbackSpeed: Float = 1.0f,
    val trimStartPct: Float = 0f,
    val trimEndPct: Float = 100f,
    val overlayText: String = "",
    val comments: List<ReelComment> = emptyList()
)

data class BotActivityLog(
    val timeLabel: String,
    val message: String
)

data class AiBotReelsUiState(
    val bots: List<AiBotAccount> = emptyList(),
    val reels: List<BotReel> = emptyList(),
    val botReelsCount: Int = 0,
    val logs: List<BotActivityLog> = emptyList(),
    val isWatchdogActive: Boolean = true,
    val lastBanner: String = "100 AI Bots active • 1 video/day per bot • 24h auto-replace enabled"
)
