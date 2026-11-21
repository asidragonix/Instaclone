package com.example.data.model

data class ReelComment(
    val id: String,
    val author: String,
    val text: String,
    val createdAtMillis: Long
)

data class Reel(
    val id: String,
    val authorId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val videoUrl: String,
    val posterUrl: String,
    val caption: String,
    val audioTrack: String,
    val niche: String = "Reels",
    val likesCount: Long,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val isFollowing: Boolean = false,
    val viewsCount: Long = 1240L,
    val createdAtMillis: Long,
    val filterPreset: String = "Original",
    val playbackSpeed: Float = 1.0f,
    val trimStartPct: Float = 0f,
    val trimEndPct: Float = 100f,
    val overlayText: String = "",
    val comments: List<ReelComment> = emptyList()
)

data class ReelsUiState(
    val reels: List<Reel> = emptyList()
)
