package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Bookmark
import com.example.data.model.Comment
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.Follow
import com.example.data.model.Post
import com.example.data.model.PostLike
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class InstaCloneRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before modifying Firestore.")
    }

    fun sanitizeUsername(raw: String, fallbackUid: String): String {
        val cleaned = raw.lowercase()
            .replace(Regex("[^a-z0-9_.]"), "")
            .trim('.')
            .take(28)
        if (cleaned.length >= 2) return cleaned
        val uidSuffix = fallbackUid.lowercase().replace(Regex("[^a-z0-9]"), "").take(8)
        return "creator_${uidSuffix.ifBlank { "user" }}".take(28)
    }

    suspend fun ensureUserProfile(
        preferredDisplayName: String?,
        preferredUsernameSeed: String?,
        preferredAvatarUrl: String?
    ): Result<UserProfile> {
        val uid = requireUserId()
        val docRef = db.collection("profiles").document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) {
                    return Result.success(existing)
                }
            }

            val username = sanitizeUsername(
                preferredUsernameSeed ?: preferredDisplayName ?: "creator",
                uid
            )
            val displayName = (preferredDisplayName ?: username)
                .trim()
                .ifBlank { "Creator" }
                .take(50)
            val avatarData = (preferredAvatarUrl ?: "").trim().take(300000)

            val payload = mapOf(
                "userId" to uid,
                "username" to username,
                "displayName" to displayName,
                "bio" to "Sharing moments on InstaClone",
                "avatarData" to avatarData,
                "followersCount" to 0L,
                "followingCount" to 0L,
                "postsCount" to 0L,
                "visibility" to "public",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val created = docRef.get().await().toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: UserProfile(
                id = uid,
                userId = uid,
                username = username,
                displayName = displayName,
                bio = "Sharing moments on InstaClone",
                avatarData = avatarData,
                visibility = "public"
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarData: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("profiles").document(uid)
        val safeUsername = sanitizeUsername(username, uid)
        val safeDisplayName = displayName.trim().ifBlank { safeUsername }.take(50)
        val safeBio = bio.trim().take(200)
        val safeAvatar = avatarData.trim().take(340000)
        return try {
            docRef.update(
                mapOf(
                    "username" to safeUsername,
                    "displayName" to safeDisplayName,
                    "bio" to safeBio,
                    "avatarData" to safeAvatar,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "profiles/$userId"
        return db.collection("profiles").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observePublicProfiles(): Flow<List<UserProfile>> {
        val path = "profiles"
        return db.collection("profiles")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.followersCount }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observePublicFeedPosts(): Flow<List<Post>> {
        val path = "posts"
        return db.collection("posts")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Post::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observePostsByAuthor(authorId: String, currentUserId: String): Flow<List<Post>> {
        val path = "posts"
        val query = if (authorId == currentUserId) {
            db.collection("posts").whereEqualTo("authorId", authorId)
        } else {
            db.collection("posts")
                .whereEqualTo("authorId", authorId)
                .whereEqualTo("visibility", "public")
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Post::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun getPostById(postId: String): Result<Post> {
        val docRef = db.collection("posts").document(postId)
        return try {
            val snap = docRef.get().await()
            val post = snap.toObject(
                Post::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw NoSuchElementException("Post not found")
            Result.success(post)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createPost(
        authorUsername: String,
        authorAvatarData: String,
        imageData: String,
        caption: String,
        location: String,
        visibility: String = "public"
    ): Result<String> {
        val uid = requireUserId()
        val postId = "post_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val postRef = db.collection("posts").document(postId)
        val profileRef = db.collection("profiles").document(uid)

        return try {
            val profileSnap = profileRef.get().await()
            val currentPostsCount = profileSnap.getLong("postsCount") ?: 0L

            val batch = db.batch()
            batch.set(
                postRef,
                mapOf(
                    "authorId" to uid,
                    "authorUsername" to sanitizeUsername(authorUsername, uid),
                    "authorAvatarData" to authorAvatarData.take(340000),
                    "imageData" to imageData.take(440000),
                    "caption" to caption.trim().take(2200),
                    "location" to location.trim().take(80),
                    "likesCount" to 0L,
                    "commentsCount" to 0L,
                    "lastCommentId" to "",
                    "visibility" to if (visibility == "private") "private" else "public",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (profileSnap.exists()) {
                batch.update(
                    profileRef,
                    mapOf(
                        "postsCount" to (currentPostsCount + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(postId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, postRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        val profileRef = db.collection("profiles").document(uid)
        return try {
            val profileSnap = profileRef.get().await()
            val currentPostsCount = profileSnap.getLong("postsCount") ?: 0L
            val batch = db.batch()
            batch.delete(postRef)
            if (profileSnap.exists() && currentPostsCount > 0L) {
                batch.update(
                    profileRef,
                    mapOf(
                        "postsCount" to (currentPostsCount - 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, postRef.path)
            Result.failure(e)
        }
    }

    fun observeLikedPostIds(userId: String): Flow<Set<String>> {
        val path = "likes"
        return db.collection("likes")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PostLike::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).map { it.postId }.toSet()
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun toggleLike(postId: String, currentlyLiked: Boolean): Result<Unit> {
        val uid = requireUserId()
        val likeId = "${postId}_${uid}"
        val likeRef = db.collection("likes").document(likeId)
        val postRef = db.collection("posts").document(postId)
        return try {
            val postSnap = postRef.get().await()
            val currentLikes = postSnap.getLong("likesCount") ?: 0L
            val batch = db.batch()
            if (currentlyLiked) {
                val nextLikes = (currentLikes - 1L).coerceAtLeast(0L)
                batch.delete(likeRef)
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to nextLikes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "postId" to postId,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to (currentLikes + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, likeRef.path)
            Result.failure(e)
        }
    }

    fun observeComments(postId: String): Flow<List<Comment>> {
        val path = "comments"
        return db.collection("comments")
            .whereEqualTo("postId", postId)
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Comment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addComment(
        postId: String,
        authorUsername: String,
        authorAvatarData: String,
        text: String
    ): Result<String> {
        val uid = requireUserId()
        val trimmed = text.trim().take(500)
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Comment cannot be empty"))
        }
        val commentId = "cmt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val commentRef = db.collection("comments").document(commentId)
        val postRef = db.collection("posts").document(postId)
        return try {
            val postSnap = postRef.get().await()
            val currentComments = postSnap.getLong("commentsCount") ?: 0L
            val batch = db.batch()
            batch.set(
                commentRef,
                mapOf(
                    "postId" to postId,
                    "authorId" to uid,
                    "authorUsername" to sanitizeUsername(authorUsername, uid),
                    "authorAvatarData" to authorAvatarData.take(340000),
                    "text" to trimmed,
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to (currentComments + 1L),
                    "lastCommentId" to commentId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(commentId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            Result.failure(e)
        }
    }

    fun observeFollowingIds(userId: String): Flow<Set<String>> {
        val path = "follows"
        return db.collection("follows")
            .whereEqualTo("followerId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Follow::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).map { it.followingId }.toSet()
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun toggleFollow(targetUserId: String, currentlyFollowing: Boolean): Result<Unit> {
        val uid = requireUserId()
        if (uid == targetUserId) return Result.failure(IllegalArgumentException("Cannot follow yourself"))
        val followId = "${uid}_${targetUserId}"
        val followRef = db.collection("follows").document(followId)
        val myProfileRef = db.collection("profiles").document(uid)
        val targetProfileRef = db.collection("profiles").document(targetUserId)

        return try {
            val mySnap = myProfileRef.get().await()
            val targetSnap = targetProfileRef.get().await()
            val myFollowing = mySnap.getLong("followingCount") ?: 0L
            val targetFollowers = targetSnap.getLong("followersCount") ?: 0L

            val batch = db.batch()
            if (currentlyFollowing) {
                batch.delete(followRef)
                if (mySnap.exists()) {
                    batch.update(
                        myProfileRef,
                        mapOf(
                            "followingCount" to (myFollowing - 1L).coerceAtLeast(0L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                if (targetSnap.exists()) {
                    batch.update(
                        targetProfileRef,
                        mapOf(
                            "followersCount" to (targetFollowers - 1L).coerceAtLeast(0L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                batch.set(
                    followRef,
                    mapOf(
                        "followerId" to uid,
                        "followingId" to targetUserId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                if (mySnap.exists()) {
                    batch.update(
                        myProfileRef,
                        mapOf(
                            "followingCount" to (myFollowing + 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                if (targetSnap.exists()) {
                    batch.update(
                        targetProfileRef,
                        mapOf(
                            "followersCount" to (targetFollowers + 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, followRef.path)
            Result.failure(e)
        }
    }

    fun observeBookmarkedPostIds(userId: String): Flow<Set<String>> {
        val path = "users/$userId/bookmarks"
        return db.collection("users").document(userId).collection("bookmarks")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Bookmark::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).map { it.postId }.toSet()
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun toggleBookmark(postId: String, currentlyBookmarked: Boolean): Result<Unit> {
        val uid = requireUserId()
        val bookmarkRef = db.collection("users").document(uid).collection("bookmarks").document(postId)
        return try {
            if (currentlyBookmarked) {
                bookmarkRef.delete().await()
            } else {
                bookmarkRef.set(
                    mapOf(
                        "userId" to uid,
                        "postId" to postId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, bookmarkRef.path)
            Result.failure(e)
        }
    }

    fun buildConversationId(uidA: String, uidB: String): String {
        val sorted = listOf(uidA, uidB).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> {
        val path = "conversations"
        return db.collection("conversations")
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Conversation::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMessages(conversationId: String, currentUserId: String): Flow<List<DirectMessage>> {
        val path = "conversations/$conversationId/messages"
        return db.collection("conversations").document(conversationId).collection("messages")
            .whereArrayContains("participantIds", currentUserId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DirectMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun sendDirectMessage(
        myProfile: UserProfile,
        peerProfile: UserProfile,
        text: String
    ): Result<String> {
        val uid = requireUserId()
        val trimmed = text.trim().take(1000)
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty"))
        }
        val sortedProfiles = listOf(myProfile, peerProfile).sortedBy { it.userId }
        val userOne = sortedProfiles[0]
        val userTwo = sortedProfiles[1]
        val participantIds = listOf(userOne.userId, userTwo.userId)
        val conversationId = "${userOne.userId}_${userTwo.userId}"
        val convRef = db.collection("conversations").document(conversationId)
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val msgRef = convRef.collection("messages").document(msgId)

        return try {
            val convSnap = convRef.get().await()
            val batch = db.batch()
            if (convSnap.exists()) {
                batch.update(
                    convRef,
                    mapOf(
                        "lastMessage" to trimmed,
                        "lastSenderId" to uid,
                        "userOneUsername" to sanitizeUsername(userOne.username, userOne.userId),
                        "userOneAvatarData" to userOne.avatarData.take(340000),
                        "userTwoUsername" to sanitizeUsername(userTwo.username, userTwo.userId),
                        "userTwoAvatarData" to userTwo.avatarData.take(340000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    convRef,
                    mapOf(
                        "participantIds" to participantIds,
                        "userOneId" to userOne.userId,
                        "userOneUsername" to sanitizeUsername(userOne.username, userOne.userId),
                        "userOneAvatarData" to userOne.avatarData.take(340000),
                        "userTwoId" to userTwo.userId,
                        "userTwoUsername" to sanitizeUsername(userTwo.username, userTwo.userId),
                        "userTwoAvatarData" to userTwo.avatarData.take(340000),
                        "lastMessage" to trimmed,
                        "lastSenderId" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                msgRef,
                mapOf(
                    "conversationId" to conversationId,
                    "participantIds" to participantIds,
                    "senderId" to uid,
                    "senderUsername" to sanitizeUsername(myProfile.username, uid),
                    "text" to trimmed,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(conversationId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, msgRef.path)
            Result.failure(e)
        }
    }
}
