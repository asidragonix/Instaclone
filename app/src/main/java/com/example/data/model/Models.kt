package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarData: String = "",
    val category: String = "",
    val pronouns: String = "",
    val website: String = "",
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarData: String = "",
    val imageData: String = "",
    val caption: String = "",
    val location: String = "",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val lastCommentId: String = "",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostLike(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarData: String = "",
    val text: String = "",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Follow(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val followingId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Bookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val userOneId: String = "",
    val userOneUsername: String = "",
    val userOneAvatarData: String = "",
    val userTwoId: String = "",
    val userTwoUsername: String = "",
    val userTwoAvatarData: String = "",
    val lastMessage: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun peerUsername(currentUserId: String): String =
        if (userOneId == currentUserId) userTwoUsername else userOneUsername

    fun peerAvatarData(currentUserId: String): String =
        if (userOneId == currentUserId) userTwoAvatarData else userOneAvatarData

    fun peerUserId(currentUserId: String): String =
        if (userOneId == currentUserId) userTwoId else userOneId
}

@Keep
@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val senderId: String = "",
    val senderUsername: String = "",
    val text: String = "",
    val attachmentType: String = "",
    val attachmentName: String = "",
    val attachmentSize: Long = 0L,
    val attachmentData: String = "",
    val replyToId: String = "",
    val replyToSender: String = "",
    val replyToText: String = "",
    val reactions: Map<String, Long> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarData: String = "",
    val mediaData: String = "",
    val mediaType: String = "image", // "image" | "video" | "text"
    val bgGradient: String = "sunset",
    val overlayText: String = "",
    val sticker: String = "",
    val caption: String = "",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val viewsCount: Long = 0L,
    val lastCommentId: String = "",
    val expiresAtMillis: Long = 0L,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoryLike(
    @DocumentId val id: String = "",
    val storyId: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoryComment(
    @DocumentId val id: String = "",
    val storyId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatarData: String = "",
    val text: String = "",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoryHighlight(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val coverData: String = "",
    val storyIds: List<String> = emptyList(),
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerUsername: String = "",
    val callerAvatarData: String = "",
    val calleeId: String = "",
    val calleeUsername: String = "",
    val calleeAvatarData: String = "",
    val participantIds: List<String> = emptyList(),
    val callType: String = "voice", // "voice" | "video"
    val status: String = "ringing", // "ringing" | "active" | "ended" | "declined" | "missed"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class PendingAttachment(
    val type: String, // "image" | "video" | "file"
    val name: String,
    val sizeBytes: Long,
    val data: String
)

enum class HomeFeedMode(val label: String) {
    MIXED("Mixed Feed"),
    POSTS_GRID("Posts Only (6)")
}

enum class ProfileContentTab(val label: String) {
    POSTS("Posts"),
    REELS("Reels"),
    SAVED("Saved"),
    TAGGED("Tagged")
}

enum class SavedContentFilter(val label: String) {
    ALL("All"),
    POSTS("Posts"),
    REELS("Reels")
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
