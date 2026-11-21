package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallSession
import com.example.data.model.Comment
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.PendingAttachment
import com.example.data.model.Post
import com.example.data.model.Reel
import com.example.data.model.ReelsUiState
import com.example.data.model.Story
import com.example.data.model.StoryComment
import com.example.data.model.StoryHighlight
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.InstaCloneRepository
import com.example.data.repository.ReelsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InstaCloneViewModel(
    private val repository: InstaCloneRepository,
    val currentUserId: String,
    initialDisplayName: String?,
    initialEmailPrefix: String?,
    initialPhotoUrl: String?,
    private val reelsStore: ReelsStore? = null
) : ViewModel() {

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 6)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _viewedStoryIds = MutableStateFlow<Set<String>>(emptySet())
    val viewedStoryIds: StateFlow<Set<String>> = _viewedStoryIds.asStateFlow()

    private val _localActiveCall = MutableStateFlow<CallSession?>(null)
    val localActiveCall: StateFlow<CallSession?> = _localActiveCall.asStateFlow()

    val reelsUiState: StateFlow<ReelsUiState> =
        reelsStore?.uiState ?: MutableStateFlow(ReelsUiState()).asStateFlow()

    val myProfileState: StateFlow<UiState<UserProfile?>> = repository
        .observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing current profile", err)
            emit(UiState.Error(err.localizedMessage ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val feedPostsState: StateFlow<UiState<List<Post>>> = repository
        .observePublicFeedPosts()
        .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing public feed", err)
            emit(UiState.Error(err.localizedMessage ?: "Failed to load feed"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val publicProfilesState: StateFlow<UiState<List<UserProfile>>> = repository
        .observePublicProfiles()
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing creators", err)
            emit(UiState.Error(err.localizedMessage ?: "Failed to load creators"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val storiesState: StateFlow<UiState<List<Story>>> = repository
        .observeActiveStories()
        .map<List<Story>, UiState<List<Story>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing stories", err)
            emit(UiState.Error(err.localizedMessage ?: "Failed to load stories"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val likedPostIds: StateFlow<Set<String>> = repository
        .observeLikedPostIds(currentUserId)
        .catch { err ->
            Log.w(TAG, "Error observing liked posts", err)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptySet()
        )

    val likedStoryIds: StateFlow<Set<String>> = repository
        .observeLikedStoryIds(currentUserId)
        .catch { err ->
            Log.w(TAG, "Error observing liked stories", err)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptySet()
        )

    val followingIds: StateFlow<Set<String>> = repository
        .observeFollowingIds(currentUserId)
        .catch { err ->
            Log.w(TAG, "Error observing following IDs", err)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptySet()
        )

    val followerIds: StateFlow<Set<String>> = repository
        .observeFollowerIds(currentUserId)
        .catch { err ->
            Log.w(TAG, "Error observing follower IDs", err)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptySet()
        )

    val bookmarkedPostIds: StateFlow<Set<String>> = repository
        .observeBookmarkedPostIds(currentUserId)
        .catch { err ->
            Log.w(TAG, "Error observing bookmarks", err)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptySet()
        )

    val conversationsState: StateFlow<UiState<List<Conversation>>> = repository
        .observeConversations(currentUserId)
        .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
        .catch { err ->
            Log.w(TAG, "Error observing conversations", err)
            emit(UiState.Error(err.localizedMessage ?: "Failed to load messages"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val callSessionsState: StateFlow<List<CallSession>> = repository
        .observeCallSessionsForUser(currentUserId)
        .catch { err ->
            Log.w(TAG, "Error observing call sessions", err)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList()
        )

    val activeCallSession: StateFlow<CallSession?> = combine(
        _localActiveCall,
        callSessionsState
    ) { local, sessions ->
        if (local != null) {
            val remote = sessions.find { it.id == local.id }
            if (remote != null) {
                when (remote.status) {
                    "ended", "declined", "missed" -> null
                    else -> remote
                }
            } else {
                local
            }
        } else {
            sessions.firstOrNull {
                it.calleeId == currentUserId && it.status == "ringing"
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                preferredDisplayName = initialDisplayName,
                preferredUsernameSeed = initialEmailPrefix ?: initialDisplayName,
                preferredAvatarUrl = initialPhotoUrl
            ).onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Could not initialize profile")
            }
        }
    }

    fun emitMessage(text: String) {
        _snackbarEvents.tryEmit(text)
    }

    fun currentMyProfileOrFallback(): UserProfile {
        val existing = (myProfileState.value as? UiState.Success)?.data
        if (existing != null) return existing
        val username = repository.sanitizeUsername("creator", currentUserId)
        return UserProfile(
            id = currentUserId,
            userId = currentUserId,
            username = username,
            displayName = "Creator"
        )
    }

    // --- POSTS ---

    fun createPost(
        imageData: String,
        caption: String,
        location: String,
        visibility: String,
        onCreated: () -> Unit
    ) {
        if (imageData.isBlank()) {
            _snackbarEvents.tryEmit("Please select a photo or studio preset first.")
            return
        }
        viewModelScope.launch {
            _isPublishing.value = true
            val myProfile = currentMyProfileOrFallback()
            val result = repository.createPost(
                authorUsername = myProfile.username,
                authorAvatarData = myProfile.avatarData,
                imageData = imageData,
                caption = caption,
                location = location,
                visibility = visibility
            )
            _isPublishing.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Post shared to InstaClone!")
                onCreated()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to publish post")
            }
        }
    }

    fun updatePostCaption(postId: String, caption: String, location: String) {
        viewModelScope.launch {
            repository.updatePostCaption(postId, caption, location).onSuccess {
                _snackbarEvents.tryEmit("Post updated")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to update post")
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId).onSuccess {
                _snackbarEvents.tryEmit("Post deleted")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to delete post")
            }
        }
    }

    fun toggleLike(postId: String, currentlyLiked: Boolean) {
        viewModelScope.launch {
            repository.toggleLike(postId, currentlyLiked).onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Could not update like")
            }
        }
    }

    fun toggleBookmark(postId: String, currentlyBookmarked: Boolean) {
        viewModelScope.launch {
            repository.toggleBookmark(postId, currentlyBookmarked).onSuccess {
                _snackbarEvents.tryEmit(if (currentlyBookmarked) "Removed from Saved" else "Saved to Profile")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Could not update bookmark")
            }
        }
    }

    fun toggleFollow(targetUserId: String, currentlyFollowing: Boolean) {
        viewModelScope.launch {
            repository.toggleFollow(targetUserId, currentlyFollowing).onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Could not update follow status")
            }
        }
    }

    fun updateProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarData: String,
        category: String = "",
        pronouns: String = "",
        website: String = "",
        visibility: String = "public",
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateProfile(
                username = username,
                displayName = displayName,
                bio = bio,
                avatarData = avatarData,
                category = category,
                pronouns = pronouns,
                website = website,
                visibility = visibility
            ).onSuccess {
                _snackbarEvents.tryEmit("Profile updated")
                onSaved()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    fun observeComments(postId: String): Flow<UiState<List<Comment>>> =
        repository.observeComments(postId)
            .map<List<Comment>, UiState<List<Comment>>> { UiState.Success(it) }
            .catch { err ->
                emit(UiState.Error(err.localizedMessage ?: "Failed to load comments"))
            }

    fun addComment(postId: String, text: String, onAdded: () -> Unit = {}) {
        viewModelScope.launch {
            val myProfile = currentMyProfileOrFallback()
            repository.addComment(
                postId = postId,
                authorUsername = myProfile.username,
                authorAvatarData = myProfile.avatarData,
                text = text
            ).onSuccess {
                onAdded()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to add comment")
            }
        }
    }

    // --- STORIES & HIGHLIGHTS ---

    fun createStory(
        mediaData: String,
        mediaType: String,
        bgGradient: String,
        overlayText: String,
        sticker: String,
        caption: String,
        onCreated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isPublishing.value = true
            val myProfile = currentMyProfileOrFallback()
            val result = repository.createStory(
                authorUsername = myProfile.username,
                authorAvatarData = myProfile.avatarData,
                mediaData = mediaData,
                mediaType = mediaType,
                bgGradient = bgGradient,
                overlayText = overlayText,
                sticker = sticker,
                caption = caption
            )
            _isPublishing.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Story published (24h)")
                onCreated()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to publish story")
            }
        }
    }

    fun deleteStory(storyId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteStory(storyId).onSuccess {
                _snackbarEvents.tryEmit("Story deleted")
                onDeleted()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to delete story")
            }
        }
    }

    fun toggleStoryLike(storyId: String, currentlyLiked: Boolean) {
        viewModelScope.launch {
            repository.toggleStoryLike(storyId, currentlyLiked).onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Could not like story")
            }
        }
    }

    fun observeStoryComments(storyId: String): Flow<UiState<List<StoryComment>>> =
        repository.observeStoryComments(storyId)
            .map<List<StoryComment>, UiState<List<StoryComment>>> { UiState.Success(it) }
            .catch { err ->
                emit(UiState.Error(err.localizedMessage ?: "Failed to load replies"))
            }

    fun addStoryComment(storyId: String, text: String, alsoSendDmToAuthor: UserProfile? = null) {
        viewModelScope.launch {
            val myProfile = currentMyProfileOrFallback()
            repository.addStoryComment(
                storyId = storyId,
                authorUsername = myProfile.username,
                authorAvatarData = myProfile.avatarData,
                text = text
            ).onSuccess {
                if (alsoSendDmToAuthor != null && alsoSendDmToAuthor.userId != currentUserId) {
                    repository.sendDirectMessage(
                        myProfile = myProfile,
                        peerProfile = alsoSendDmToAuthor,
                        text = "Replied to your story: ${text.trim()}"
                    )
                }
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to send reply")
            }
        }
    }

    fun markStoryViewed(story: Story) {
        if (story.id in _viewedStoryIds.value) return
        _viewedStoryIds.value = _viewedStoryIds.value + story.id
        if (story.authorId != currentUserId) {
            viewModelScope.launch {
                repository.incrementStoryView(story.id)
            }
        }
    }

    fun observeHighlights(ownerId: String): Flow<UiState<List<StoryHighlight>>> =
        repository.observeHighlights(ownerId)
            .map<List<StoryHighlight>, UiState<List<StoryHighlight>>> { UiState.Success(it) }
            .catch { err ->
                emit(UiState.Error(err.localizedMessage ?: "Failed to load highlights"))
            }

    fun createHighlight(title: String, coverData: String, storyIds: List<String>, onCreated: () -> Unit = {}) {
        viewModelScope.launch {
            repository.createHighlight(title, coverData, storyIds).onSuccess {
                _snackbarEvents.tryEmit("Story highlight saved!")
                onCreated()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to create highlight")
            }
        }
    }

    // --- PROFILE & CONNECTIONS ---

    fun observeUserProfile(userId: String): Flow<UiState<UserProfile?>> =
        repository.observeUserProfile(userId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { err ->
                emit(UiState.Error(err.localizedMessage ?: "Failed to load profile"))
            }

    fun observeAuthorPosts(authorId: String): Flow<UiState<List<Post>>> =
        repository.observePostsByAuthor(authorId, currentUserId)
            .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
            .catch { err ->
                emit(UiState.Error(err.localizedMessage ?: "Failed to load posts"))
            }

    // --- DIRECT MESSAGES & ATTACHMENTS ---

    fun buildConversationId(peerUserId: String): String =
        repository.buildConversationId(currentUserId, peerUserId)

    fun observeMessages(conversationId: String): Flow<UiState<List<DirectMessage>>> =
        repository.observeMessages(conversationId, currentUserId)
            .map<List<DirectMessage>, UiState<List<DirectMessage>>> { UiState.Success(it) }
            .catch { err ->
                emit(UiState.Error(err.localizedMessage ?: "Failed to load messages"))
            }

    fun sendMessage(
        peerProfile: UserProfile,
        text: String,
        attachment: PendingAttachment? = null,
        replyTo: DirectMessage? = null,
        onSent: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val myProfile = currentMyProfileOrFallback()
            repository.sendDirectMessage(
                myProfile = myProfile,
                peerProfile = peerProfile,
                text = text,
                attachment = attachment,
                replyTo = replyTo
            ).onSuccess { convId ->
                onSent(convId)
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to send message")
            }
        }
    }

    fun toggleMessageReaction(conversationId: String, message: DirectMessage, emoji: String) {
        viewModelScope.launch {
            repository.toggleMessageReaction(
                conversationId = conversationId,
                messageId = message.id,
                emoji = emoji,
                currentReactions = message.reactions
            ).onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to react")
            }
        }
    }

    fun deleteMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            repository.deleteDirectMessage(conversationId, messageId).onSuccess {
                _snackbarEvents.tryEmit("Message unsent")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to delete message")
            }
        }
    }

    // --- VOICE & VIDEO CALLS ---

    fun startCall(peerProfile: UserProfile, callType: String) {
        if (peerProfile.userId == currentUserId) return
        viewModelScope.launch {
            val myProfile = currentMyProfileOrFallback()
            repository.startCallSession(myProfile, peerProfile, callType).onSuccess { session ->
                _localActiveCall.value = session
            }.onFailure { err ->
                // Graceful fallback local call overlay if peer profile doc not yet present
                _localActiveCall.value = CallSession(
                    id = "local_${System.currentTimeMillis()}",
                    callerId = currentUserId,
                    callerUsername = myProfile.username,
                    callerAvatarData = myProfile.avatarData,
                    calleeId = peerProfile.userId,
                    calleeUsername = peerProfile.username,
                    calleeAvatarData = peerProfile.avatarData,
                    participantIds = listOf(currentUserId, peerProfile.userId),
                    callType = callType,
                    status = "ringing"
                )
                Log.w(TAG, "Call signaling fallback to local overlay", err)
            }
        }
    }

    fun acceptCall(callSession: CallSession) {
        _localActiveCall.value = callSession.copy(status = "active")
        if (!callSession.id.startsWith("local_")) {
            viewModelScope.launch {
                repository.updateCallStatus(callSession.id, "active")
            }
        }
    }

    fun declineCall(callSession: CallSession) {
        _localActiveCall.value = null
        if (!callSession.id.startsWith("local_")) {
            viewModelScope.launch {
                repository.updateCallStatus(callSession.id, "declined")
            }
        }
    }

    fun clearLocalActiveCallOnRemoteTermination(sessionId: String, remoteStatus: String) {
        if (_localActiveCall.value?.id == sessionId) {
            _localActiveCall.value = null
            val msg = when (remoteStatus) {
                "declined" -> "Call declined"
                "missed" -> "Call missed"
                else -> "Call ended"
            }
            _snackbarEvents.tryEmit(msg)
        }
    }

    fun markLocalCallConnected() {
        val current = _localActiveCall.value ?: return
        if (current.status == "ringing") {
            _localActiveCall.value = current.copy(status = "active")
            if (!current.id.startsWith("local_")) {
                viewModelScope.launch {
                    repository.updateCallStatus(current.id, "active")
                }
            }
        }
    }

    fun endCall(callSession: CallSession, durationSeconds: Int) {
        _localActiveCall.value = null
        val peerId = if (callSession.callerId == currentUserId) callSession.calleeId else callSession.callerId
        val peerName = if (callSession.callerId == currentUserId) callSession.calleeUsername else callSession.callerUsername
        val peerAvatar = if (callSession.callerId == currentUserId) callSession.calleeAvatarData else callSession.callerAvatarData
        viewModelScope.launch {
            if (!callSession.id.startsWith("local_")) {
                repository.updateCallStatus(callSession.id, "ended")
            }
            val label = if (callSession.callType == "video") "Video call" else "Voice call"
            val summary = if (durationSeconds > 0) {
                val mins = durationSeconds / 60
                val secs = durationSeconds % 60
                "$label ended • ${String.format("%02d:%02d", mins, secs)}"
            } else {
                "$label • Missed / Cancelled"
            }
            val myProfile = currentMyProfileOrFallback()
            repository.sendDirectMessage(
                myProfile = myProfile,
                peerProfile = UserProfile(
                    id = peerId,
                    userId = peerId,
                    username = peerName,
                    displayName = peerName,
                    avatarData = peerAvatar
                ),
                text = summary
            )
        }
    }

    // --- REELS ---

    fun toggleReelLike(reelId: String) {
        reelsStore?.toggleLikeReel(reelId)
    }

    fun toggleReelBookmark(reelId: String) {
        reelsStore?.toggleBookmarkReel(reelId)
    }

    fun addReelComment(reelId: String, text: String) {
        val myProfile = currentMyProfileOrFallback()
        reelsStore?.addComment(reelId, myProfile.username, text)
    }

    fun deleteReelComment(reelId: String, commentId: String) {
        reelsStore?.deleteComment(reelId, commentId)
    }

    fun updateReelCaption(reelId: String, newCaption: String) {
        reelsStore?.updateReelCaption(reelId, newCaption)
        _snackbarEvents.tryEmit("Reel caption updated")
    }

    fun deleteReel(reelId: String) {
        reelsStore?.deleteReel(reelId)
        _snackbarEvents.tryEmit("Reel deleted")
    }

    fun toggleFollowReelCreator(targetUsername: String) {
        reelsStore?.toggleFollowCreator(targetUsername)
    }

    fun publishEditedReel(
        videoUrl: String,
        posterUrl: String,
        caption: String,
        audioTrack: String,
        filterPreset: String,
        playbackSpeed: Float,
        trimStartPct: Float,
        trimEndPct: Float,
        overlayText: String,
        onPublished: (Reel) -> Unit
    ) {
        val store = reelsStore ?: return
        val myProfile = currentMyProfileOrFallback()
        val created = store.publishEditedReel(
            authorId = currentUserId,
            username = myProfile.username,
            displayName = myProfile.displayName,
            avatarUrl = myProfile.avatarData,
            videoUrl = videoUrl,
            posterUrl = posterUrl,
            caption = caption,
            audioTrack = audioTrack,
            filterPreset = filterPreset,
            playbackSpeed = playbackSpeed,
            trimStartPct = trimStartPct,
            trimEndPct = trimEndPct,
            overlayText = overlayText
        )
        _snackbarEvents.tryEmit("Published edited reel to Reels!")
        onPublished(created)
    }

    private companion object {
        const val TAG = "InstaCloneVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
