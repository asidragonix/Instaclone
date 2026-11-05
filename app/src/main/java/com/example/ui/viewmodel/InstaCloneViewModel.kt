package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.InstaCloneRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InstaCloneViewModel(
    private val repository: InstaCloneRepository,
    val currentUserId: String,
    initialDisplayName: String?,
    initialEmailPrefix: String?,
    initialPhotoUrl: String?
) : ViewModel() {

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

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
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateProfile(username, displayName, bio, avatarData).onSuccess {
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

    fun buildConversationId(peerUserId: String): String =
        repository.buildConversationId(currentUserId, peerUserId)

    fun observeMessages(conversationId: String): Flow<UiState<List<DirectMessage>>> =
        repository.observeMessages(conversationId, currentUserId)
            .map<List<DirectMessage>, UiState<List<DirectMessage>>> { UiState.Success(it) }
            .catch { err ->
                emit(UiState.Error(err.localizedMessage ?: "Failed to load messages"))
            }

    fun sendMessage(peerProfile: UserProfile, text: String, onSent: (String) -> Unit = {}) {
        viewModelScope.launch {
            val myProfile = currentMyProfileOrFallback()
            repository.sendDirectMessage(myProfile, peerProfile, text).onSuccess { convId ->
                onSent(convId)
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to send message")
            }
        }
    }

    private companion object {
        const val TAG = "InstaCloneVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
