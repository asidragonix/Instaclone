package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Post
import com.example.data.model.Reel
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.CallOverlayDialog
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.CreateStoryDialog
import com.example.ui.components.EditCaptionDialog
import com.example.ui.components.FullScreenStoryViewerDialog
import com.example.ui.components.ShareContentSheet
import com.example.ui.viewmodel.InstaCloneViewModel

enum class InstaTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home_tab"),
    EXPLORE(R.string.nav_explore, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_explore_tab"),
    REELS(R.string.nav_reels, Icons.Filled.Movie, Icons.Outlined.Movie, "nav_reels_tab"),
    CREATE(R.string.nav_create, Icons.Filled.AddBox, Icons.Outlined.AddBox, "nav_create_tab"),
    MESSAGES(R.string.nav_messages, Icons.AutoMirrored.Filled.Send, Icons.AutoMirrored.Outlined.Send, "nav_messages_tab"),
    PROFILE(R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile_tab")
}

private data class SharePayload(
    val title: String,
    val message: String
)

@Composable
fun AdaptiveMainScaffold(
    viewModel: InstaCloneViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val myProfileState by viewModel.myProfileState.collectAsStateWithLifecycle()
    val feedPostsState by viewModel.feedPostsState.collectAsStateWithLifecycle()
    val creatorsState by viewModel.publicProfilesState.collectAsStateWithLifecycle()
    val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
    val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val likedStoryIds by viewModel.likedStoryIds.collectAsStateWithLifecycle()
    val viewedStoryIds by viewModel.viewedStoryIds.collectAsStateWithLifecycle()
    val bookmarkedPostIds by viewModel.bookmarkedPostIds.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val followerIds by viewModel.followerIds.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val callSessions by viewModel.callSessionsState.collectAsStateWithLifecycle()
    val localActiveCall by viewModel.localActiveCall.collectAsStateWithLifecycle()
    val activeCall by viewModel.activeCallSession.collectAsStateWithLifecycle()
    val isPublishing by viewModel.isPublishing.collectAsStateWithLifecycle()
    val reelsUiState by viewModel.reelsUiState.collectAsStateWithLifecycle()

    LaunchedEffect(localActiveCall, callSessions) {
        val local = localActiveCall ?: return@LaunchedEffect
        val remote = callSessions.find { it.id == local.id } ?: return@LaunchedEffect
        if (remote.status == "ended" || remote.status == "declined" || remote.status == "missed") {
            viewModel.clearLocalActiveCallOnRemoteTermination(local.id, remote.status)
        }
    }

    var currentTab by rememberSaveable { mutableStateOf(InstaTab.HOME) }
    var inspectedUserId by rememberSaveable { mutableStateOf<String?>(null) }
    var showReelsEditor by rememberSaveable { mutableStateOf(false) }
    var reelsEditorSeedReel by remember { mutableStateOf<Reel?>(null) }
    var focusedReelId by rememberSaveable { mutableStateOf<String?>(null) }
    var exploreInitialSearch by rememberSaveable { mutableStateOf("") }
    var activeChatPeer by remember { mutableStateOf<UserProfile?>(null) }
    var commentSheetPost by remember { mutableStateOf<Post?>(null) }

    var showCreateStoryDialog by rememberSaveable { mutableStateOf(false) }
    var activeStoryGroup by remember { mutableStateOf<List<Story>?>(null) }
    var sharePayload by remember { mutableStateOf<SharePayload?>(null) }
    var editPostTarget by remember { mutableStateOf<Post?>(null) }
    var editReelTarget by remember { mutableStateOf<Reel?>(null) }

    val myProfile = (myProfileState as? UiState.Success)?.data
    val allCreators = (creatorsState as? UiState.Success)?.data.orEmpty()
    val allPublicPosts = (feedPostsState as? UiState.Success)?.data.orEmpty()
    val activeStories = (storiesState as? UiState.Success)?.data.orEmpty()
    val conversationsCount = (conversationsState as? UiState.Success)?.data?.size ?: 0

    val openUserProfile: (String) -> Unit = { targetUid ->
        showReelsEditor = false
        if (targetUid == viewModel.currentUserId) {
            inspectedUserId = null
            currentTab = InstaTab.PROFILE
        } else {
            inspectedUserId = targetUid
        }
    }

    val openDirectChat: (UserProfile) -> Unit = { peer ->
        showReelsEditor = false
        inspectedUserId = null
        activeChatPeer = peer
        currentTab = InstaTab.MESSAGES
    }

    val openExploreWithQuery: (String) -> Unit = { query ->
        inspectedUserId = null
        showReelsEditor = false
        exploreInitialSearch = query
        currentTab = InstaTab.EXPLORE
    }

    // Active Incoming or Outgoing Call Overlay
    val currentActiveCall = activeCall
    if (currentActiveCall != null) {
        CallOverlayDialog(
            callSession = currentActiveCall,
            currentUserId = viewModel.currentUserId,
            onAcceptCall = { viewModel.acceptCall(currentActiveCall) },
            onDeclineCall = { viewModel.declineCall(currentActiveCall) },
            onMarkConnected = { viewModel.markLocalCallConnected() },
            onEndCall = { secs -> viewModel.endCall(currentActiveCall, secs) }
        )
    }

    // Create Story Fullscreen Dialog
    if (showCreateStoryDialog) {
        CreateStoryDialog(
            isPublishing = isPublishing,
            onDismiss = { showCreateStoryDialog = false },
            onPublishStory = { mediaData, mediaType, bgGradient, overlayText, sticker, caption ->
                viewModel.createStory(
                    mediaData = mediaData,
                    mediaType = mediaType,
                    bgGradient = bgGradient,
                    overlayText = overlayText,
                    sticker = sticker,
                    caption = caption
                ) {
                    showCreateStoryDialog = false
                }
            }
        )
    }

    // Full-Screen Story Viewer
    val currentStories = activeStoryGroup
    if (!currentStories.isNullOrEmpty()) {
        // Keep stories synced with live Firestore counts if present in activeStories
        val syncedStories = currentStories.map { st ->
            activeStories.find { it.id == st.id } ?: st
        }
        FullScreenStoryViewerDialog(
            stories = syncedStories,
            currentUserId = viewModel.currentUserId,
            likedStoryIds = likedStoryIds,
            observeComments = { storyId -> viewModel.observeStoryComments(storyId) },
            onMarkViewed = { st -> viewModel.markStoryViewed(st) },
            onToggleLike = { st -> viewModel.toggleStoryLike(st.id, st.id in likedStoryIds) },
            onSendReply = { st, text, alsoDm ->
                val authorProfile = allCreators.find { it.userId == st.authorId }
                    ?: UserProfile(
                        id = st.authorId,
                        userId = st.authorId,
                        username = st.authorUsername,
                        displayName = st.authorUsername,
                        avatarData = st.authorAvatarData
                    )
                viewModel.addStoryComment(
                    storyId = st.id,
                    text = text,
                    alsoSendDmToAuthor = if (alsoDm) authorProfile else null
                )
            },
            onShareStory = { st ->
                sharePayload = SharePayload(
                    title = "Share @${st.authorUsername}'s Story",
                    message = "Check out @${st.authorUsername}'s story on InstaClone: ${st.caption.ifBlank { st.overlayText }}"
                )
            },
            onDeleteStory = { storyId ->
                viewModel.deleteStory(storyId) {
                    val remaining = syncedStories.filterNot { it.id == storyId }
                    activeStoryGroup = remaining.ifEmpty { null }
                }
            },
            onAddAnotherStory = {
                activeStoryGroup = null
                showCreateStoryDialog = true
            },
            onOpenProfile = openUserProfile,
            onDismiss = { activeStoryGroup = null }
        )
    }

    // Share Sheet (Posts, Reels, Stories -> DM Friend or System Apps)
    val currentShare = sharePayload
    if (currentShare != null) {
        ShareContentSheet(
            title = currentShare.title,
            shareText = currentShare.message,
            friends = allCreators,
            currentUserId = viewModel.currentUserId,
            onSendToFriend = { friend ->
                viewModel.sendMessage(peerProfile = friend, text = currentShare.message) {
                    viewModel.emitMessage("Shared to @${friend.username}")
                }
            },
            onDismiss = { sharePayload = null }
        )
    }

    // Edit Caption Dialog (Post or Reel)
    val targetEditPost = editPostTarget
    if (targetEditPost != null) {
        EditCaptionDialog(
            initialCaption = targetEditPost.caption,
            initialLocation = targetEditPost.location,
            showLocation = true,
            onDismiss = { editPostTarget = null },
            onSave = { newCap, newLoc ->
                viewModel.updatePostCaption(targetEditPost.id, newCap, newLoc)
                editPostTarget = null
            }
        )
    }

    val targetEditReel = editReelTarget
    if (targetEditReel != null) {
        EditCaptionDialog(
            initialCaption = targetEditReel.caption,
            showLocation = false,
            onDismiss = { editReelTarget = null },
            onSave = { newCap, _ ->
                viewModel.updateReelCaption(targetEditReel.id, newCap)
                editReelTarget = null
            }
        )
    }

    if (commentSheetPost != null) {
        val targetPost = commentSheetPost!!
        val commentsFlow = remember(targetPost.id) { viewModel.observeComments(targetPost.id) }
        CommentsBottomSheet(
            post = targetPost,
            commentsFlow = commentsFlow,
            onAddComment = { text -> viewModel.addComment(targetPost.id, text) },
            onOpenProfile = { uid ->
                commentSheetPost = null
                openUserProfile(uid)
            },
            onDismiss = { commentSheetPost = null }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        InstaTab.entries.forEach { tab ->
                            val selected = currentTab == tab && inspectedUserId == null && !showReelsEditor
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    showReelsEditor = false
                                    inspectedUserId = null
                                    currentTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        InstaTab.entries.forEach { tab ->
                            val selected = currentTab == tab && inspectedUserId == null && !showReelsEditor
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    showReelsEditor = false
                                    inspectedUserId = null
                                    currentTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val overrideUid = inspectedUserId
                    if (showReelsEditor) {
                        ReelsEditScreen(
                            initialReel = reelsEditorSeedReel,
                            onBack = {
                                showReelsEditor = false
                                reelsEditorSeedReel = null
                            },
                            onPublishReel = { videoUrl, posterUrl, caption, audioTrack, filterPreset, speed, trimStart, trimEnd, overlayText ->
                                viewModel.publishEditedReel(
                                    videoUrl = videoUrl,
                                    posterUrl = posterUrl,
                                    caption = caption,
                                    audioTrack = audioTrack,
                                    filterPreset = filterPreset,
                                    playbackSpeed = speed,
                                    trimStartPct = trimStart,
                                    trimEndPct = trimEnd,
                                    overlayText = overlayText
                                ) { created ->
                                    showReelsEditor = false
                                    reelsEditorSeedReel = null
                                    focusedReelId = created.id
                                    currentTab = InstaTab.REELS
                                }
                            }
                        )
                    } else if (overrideUid != null) {
                        val profileFlow = remember(overrideUid) { viewModel.observeUserProfile(overrideUid) }
                        val postsFlow = remember(overrideUid) { viewModel.observeAuthorPosts(overrideUid) }
                        val highlightsFlow = remember(overrideUid) { viewModel.observeHighlights(overrideUid) }
                        ProfileScreen(
                            targetUserId = overrideUid,
                            currentUserId = viewModel.currentUserId,
                            profileFlow = profileFlow,
                            authorPostsFlow = postsFlow,
                            highlightsFlow = highlightsFlow,
                            allPublicPosts = allPublicPosts,
                            allCreators = allCreators,
                            allReels = reelsUiState.reels,
                            activeStories = activeStories,
                            likedPostIds = likedPostIds,
                            bookmarkedPostIds = bookmarkedPostIds,
                            followingIds = followingIds,
                            followerIds = followerIds,
                            showBackButton = true,
                            onBack = { inspectedUserId = null },
                            onToggleFollow = { uid, following -> viewModel.toggleFollow(uid, following) },
                            onMessageUser = openDirectChat,
                            onStartCall = { peer, callType -> viewModel.startCall(peer, callType) },
                            onUpdateProfile = { _, _, _, _, _, _, _, _ -> },
                            onCreateHighlight = { _, _, _ -> },
                            onOpenStories = { stories -> activeStoryGroup = stories },
                            onOpenCreateStory = { showCreateStoryDialog = true },
                            onOpenCreatePost = { currentTab = InstaTab.CREATE },
                            onOpenCreateReel = {
                                reelsEditorSeedReel = null
                                showReelsEditor = true
                            },
                            onOpenReelById = { reelId ->
                                inspectedUserId = null
                                focusedReelId = reelId
                                currentTab = InstaTab.REELS
                            },
                            onSearchHashtag = openExploreWithQuery,
                            onSignOut = onSignOut,
                            onToggleLike = { post -> viewModel.toggleLike(post.id, post.id in likedPostIds) },
                            onOpenComments = { post -> commentSheetPost = post },
                            onToggleBookmark = { post -> viewModel.toggleBookmark(post.id, post.id in bookmarkedPostIds) },
                            onSharePost = { post ->
                                sharePayload = SharePayload(
                                    title = "Share @${post.authorUsername}'s Post",
                                    message = "Check out @${post.authorUsername}'s post on InstaClone: ${post.caption}"
                                )
                            },
                            onEditPost = { post -> editPostTarget = post },
                            onOpenProfile = openUserProfile,
                            onDeletePost = { postId -> viewModel.deletePost(postId) }
                        )
                    } else {
                        when (currentTab) {
                            InstaTab.HOME -> {
                                HomeFeedScreen(
                                    currentUserId = viewModel.currentUserId,
                                    currentUserProfile = myProfile,
                                    feedPostsState = feedPostsState,
                                    storiesState = storiesState,
                                    reels = reelsUiState.reels,
                                    viewedStoryIds = viewedStoryIds,
                                    likedPostIds = likedPostIds,
                                    bookmarkedPostIds = bookmarkedPostIds,
                                    conversationsCount = conversationsCount,
                                    isDarkTheme = isDarkTheme,
                                    onToggleTheme = onToggleTheme,
                                    onOpenReelsWatch = { reelId ->
                                        focusedReelId = reelId
                                        currentTab = InstaTab.REELS
                                    },
                                    onOpenMessagesInbox = { currentTab = InstaTab.MESSAGES },
                                    onOpenCreatePost = { currentTab = InstaTab.CREATE },
                                    onOpenCreateStory = { showCreateStoryDialog = true },
                                    onOpenStoryGroup = { group -> activeStoryGroup = group },
                                    onToggleLike = { post -> viewModel.toggleLike(post.id, post.id in likedPostIds) },
                                    onOpenComments = { post -> commentSheetPost = post },
                                    onToggleBookmark = { post -> viewModel.toggleBookmark(post.id, post.id in bookmarkedPostIds) },
                                    onSharePost = { post ->
                                        sharePayload = SharePayload(
                                            title = "Share @${post.authorUsername}'s Post",
                                            message = "Check out @${post.authorUsername}'s post on InstaClone: ${post.caption}"
                                        )
                                    },
                                    onEditPost = { post -> editPostTarget = post },
                                    onOpenProfile = openUserProfile,
                                    onDirectMessageAuthor = { uid, uname, avatar ->
                                        openDirectChat(
                                            UserProfile(
                                                id = uid,
                                                userId = uid,
                                                username = uname,
                                                displayName = uname,
                                                avatarData = avatar
                                            )
                                        )
                                    },
                                    onDeletePost = { postId -> viewModel.deletePost(postId) }
                                )
                            }

                            InstaTab.EXPLORE -> {
                                ExploreScreen(
                                    currentUserId = viewModel.currentUserId,
                                    postsState = feedPostsState,
                                    creatorsState = creatorsState,
                                    likedPostIds = likedPostIds,
                                    bookmarkedPostIds = bookmarkedPostIds,
                                    followingIds = followingIds,
                                    initialSearchQuery = exploreInitialSearch,
                                    onClearInitialSearch = { exploreInitialSearch = "" },
                                    onToggleFollow = { uid, following -> viewModel.toggleFollow(uid, following) },
                                    onToggleLike = { post -> viewModel.toggleLike(post.id, post.id in likedPostIds) },
                                    onOpenComments = { post -> commentSheetPost = post },
                                    onToggleBookmark = { post -> viewModel.toggleBookmark(post.id, post.id in bookmarkedPostIds) },
                                    onSharePost = { post ->
                                        sharePayload = SharePayload(
                                            title = "Share @${post.authorUsername}'s Post",
                                            message = "Check out @${post.authorUsername}'s post on InstaClone: ${post.caption}"
                                        )
                                    },
                                    onEditPost = { post -> editPostTarget = post },
                                    onOpenProfile = openUserProfile,
                                    onDirectMessageAuthor = { uid, uname, avatar ->
                                        openDirectChat(
                                            UserProfile(
                                                id = uid,
                                                userId = uid,
                                                username = uname,
                                                displayName = uname,
                                                avatarData = avatar
                                            )
                                        )
                                    },
                                    onDeletePost = { postId -> viewModel.deletePost(postId) }
                                )
                            }

                            InstaTab.REELS -> {
                                ReelsScreen(
                                    uiState = reelsUiState,
                                    currentUserId = viewModel.currentUserId,
                                    currentUsername = myProfile?.username ?: "creator",
                                    initialFocusedReelId = focusedReelId,
                                    onClearFocusedReel = { focusedReelId = null },
                                    onToggleLike = { reelId -> viewModel.toggleReelLike(reelId) },
                                    onToggleBookmark = { reelId -> viewModel.toggleReelBookmark(reelId) },
                                    onAddComment = { reelId, text -> viewModel.addReelComment(reelId, text) },
                                    onDeleteComment = { reelId, commentId -> viewModel.deleteReelComment(reelId, commentId) },
                                    onToggleFollow = { uname -> viewModel.toggleFollowReelCreator(uname) },
                                    onShareReel = { reel ->
                                        sharePayload = SharePayload(
                                            title = "Share @${reel.username}'s Reel",
                                            message = "Watch @${reel.username}'s reel on InstaClone: ${reel.caption}\n${reel.videoUrl}"
                                        )
                                    },
                                    onOpenReelsEditor = { seedReel ->
                                        reelsEditorSeedReel = seedReel
                                        showReelsEditor = true
                                    },
                                    onEditReelCaption = { reel -> editReelTarget = reel },
                                    onDeleteReel = { reelId -> viewModel.deleteReel(reelId) },
                                    onOpenProfile = openUserProfile
                                )
                            }

                            InstaTab.CREATE -> {
                                CreatePostScreen(
                                    isPublishing = isPublishing,
                                    onOpenCreateStory = { showCreateStoryDialog = true },
                                    onOpenReelsStudio = {
                                        reelsEditorSeedReel = null
                                        showReelsEditor = true
                                    },
                                    onPublishPost = { imageData, caption, location, visibility ->
                                        viewModel.createPost(
                                            imageData = imageData,
                                            caption = caption,
                                            location = location,
                                            visibility = visibility,
                                            onCreated = { currentTab = InstaTab.HOME }
                                        )
                                    }
                                )
                            }

                            InstaTab.MESSAGES -> {
                                DirectMessagesScreen(
                                    currentUserId = viewModel.currentUserId,
                                    conversationsState = conversationsState,
                                    creatorsState = creatorsState,
                                    callSessions = callSessions,
                                    selectedPeer = activeChatPeer,
                                    onSelectPeer = { activeChatPeer = it },
                                    buildConversationId = { peerUid -> viewModel.buildConversationId(peerUid) },
                                    observeMessages = { convId -> viewModel.observeMessages(convId) },
                                    onSendMessage = { peer, text, attachment, reply ->
                                        viewModel.sendMessage(
                                            peerProfile = peer,
                                            text = text,
                                            attachment = attachment,
                                            replyTo = reply
                                        )
                                    },
                                    onToggleReaction = { convId, msg, emoji ->
                                        viewModel.toggleMessageReaction(convId, msg, emoji)
                                    },
                                    onDeleteMessage = { convId, msgId ->
                                        viewModel.deleteMessage(convId, msgId)
                                    },
                                    onStartCall = { peer, callType -> viewModel.startCall(peer, callType) },
                                    onOpenProfile = openUserProfile
                                )
                            }

                            InstaTab.PROFILE -> {
                                val ownPostsFlow = remember(viewModel.currentUserId) {
                                    viewModel.observeAuthorPosts(viewModel.currentUserId)
                                }
                                val ownHighlightsFlow = remember(viewModel.currentUserId) {
                                    viewModel.observeHighlights(viewModel.currentUserId)
                                }
                                ProfileScreen(
                                    targetUserId = viewModel.currentUserId,
                                    currentUserId = viewModel.currentUserId,
                                    profileFlow = viewModel.myProfileState,
                                    authorPostsFlow = ownPostsFlow,
                                    highlightsFlow = ownHighlightsFlow,
                                    allPublicPosts = allPublicPosts,
                                    allCreators = allCreators,
                                    allReels = reelsUiState.reels,
                                    activeStories = activeStories,
                                    likedPostIds = likedPostIds,
                                    bookmarkedPostIds = bookmarkedPostIds,
                                    followingIds = followingIds,
                                    followerIds = followerIds,
                                    showBackButton = false,
                                    onBack = {},
                                    onToggleFollow = { uid, following -> viewModel.toggleFollow(uid, following) },
                                    onMessageUser = openDirectChat,
                                    onStartCall = { peer, callType -> viewModel.startCall(peer, callType) },
                                    onUpdateProfile = { uname, dname, bio, avatar, cat, pronouns, website, visibility ->
                                        viewModel.updateProfile(
                                            username = uname,
                                            displayName = dname,
                                            bio = bio,
                                            avatarData = avatar,
                                            category = cat,
                                            pronouns = pronouns,
                                            website = website,
                                            visibility = visibility
                                        )
                                    },
                                    onCreateHighlight = { title, cover, storyIds ->
                                        viewModel.createHighlight(title, cover, storyIds)
                                    },
                                    onOpenStories = { stories -> activeStoryGroup = stories },
                                    onOpenCreateStory = { showCreateStoryDialog = true },
                                    onOpenCreatePost = { currentTab = InstaTab.CREATE },
                                    onOpenCreateReel = {
                                        reelsEditorSeedReel = null
                                        showReelsEditor = true
                                    },
                                    onOpenReelById = { reelId ->
                                        focusedReelId = reelId
                                        currentTab = InstaTab.REELS
                                    },
                                    onSearchHashtag = openExploreWithQuery,
                                    onSignOut = onSignOut,
                                    onToggleLike = { post -> viewModel.toggleLike(post.id, post.id in likedPostIds) },
                                    onOpenComments = { post -> commentSheetPost = post },
                                    onToggleBookmark = { post -> viewModel.toggleBookmark(post.id, post.id in bookmarkedPostIds) },
                                    onSharePost = { post ->
                                        sharePayload = SharePayload(
                                            title = "Share @${post.authorUsername}'s Post",
                                            message = "Check out @${post.authorUsername}'s post on InstaClone: ${post.caption}"
                                        )
                                    },
                                    onEditPost = { post -> editPostTarget = post },
                                    onOpenProfile = openUserProfile,
                                    onDeletePost = { postId -> viewModel.deletePost(postId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
