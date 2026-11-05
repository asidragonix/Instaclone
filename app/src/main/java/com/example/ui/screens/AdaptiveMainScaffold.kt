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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.viewmodel.InstaCloneViewModel

enum class InstaTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home_tab"),
    EXPLORE(R.string.nav_explore, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_explore_tab"),
    CREATE(R.string.nav_create, Icons.Filled.AddBox, Icons.Outlined.AddBox, "nav_create_tab"),
    MESSAGES(R.string.nav_messages, Icons.AutoMirrored.Filled.Send, Icons.AutoMirrored.Outlined.Send, "nav_messages_tab"),
    PROFILE(R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile_tab")
}

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

    val feedPostsState by viewModel.feedPostsState.collectAsStateWithLifecycle()
    val creatorsState by viewModel.publicProfilesState.collectAsStateWithLifecycle()
    val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val bookmarkedPostIds by viewModel.bookmarkedPostIds.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val isPublishing by viewModel.isPublishing.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(InstaTab.HOME) }
    var inspectedUserId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeChatPeer by remember { mutableStateOf<UserProfile?>(null) }
    var commentSheetPost by remember { mutableStateOf<Post?>(null) }

    val conversationsCount = (conversationsState as? UiState.Success)?.data?.size ?: 0
    val allPublicPosts = (feedPostsState as? UiState.Success)?.data.orEmpty()

    val openUserProfile: (String) -> Unit = { targetUid ->
        if (targetUid == viewModel.currentUserId) {
            inspectedUserId = null
            currentTab = InstaTab.PROFILE
        } else {
            inspectedUserId = targetUid
        }
    }

    val openDirectChat: (UserProfile) -> Unit = { peer ->
        inspectedUserId = null
        activeChatPeer = peer
        currentTab = InstaTab.MESSAGES
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
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        InstaTab.entries.forEach { tab ->
                            val selected = currentTab == tab && inspectedUserId == null
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    inspectedUserId = null
                                    currentTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
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
                            val selected = currentTab == tab && inspectedUserId == null
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    inspectedUserId = null
                                    currentTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
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
                    if (overrideUid != null) {
                        val profileFlow = remember(overrideUid) { viewModel.observeUserProfile(overrideUid) }
                        val postsFlow = remember(overrideUid) { viewModel.observeAuthorPosts(overrideUid) }
                        ProfileScreen(
                            targetUserId = overrideUid,
                            currentUserId = viewModel.currentUserId,
                            profileFlow = profileFlow,
                            authorPostsFlow = postsFlow,
                            allPublicPosts = allPublicPosts,
                            likedPostIds = likedPostIds,
                            bookmarkedPostIds = bookmarkedPostIds,
                            followingIds = followingIds,
                            showBackButton = true,
                            onBack = { inspectedUserId = null },
                            onToggleFollow = { uid, following -> viewModel.toggleFollow(uid, following) },
                            onMessageUser = openDirectChat,
                            onUpdateProfile = { _, _, _, _ -> },
                            onSignOut = onSignOut,
                            onToggleLike = { post -> viewModel.toggleLike(post.id, post.id in likedPostIds) },
                            onOpenComments = { post -> commentSheetPost = post },
                            onToggleBookmark = { post -> viewModel.toggleBookmark(post.id, post.id in bookmarkedPostIds) },
                            onOpenProfile = openUserProfile,
                            onDeletePost = { postId -> viewModel.deletePost(postId) }
                        )
                    } else {
                        when (currentTab) {
                            InstaTab.HOME -> {
                                HomeFeedScreen(
                                    currentUserId = viewModel.currentUserId,
                                    feedPostsState = feedPostsState,
                                    creatorsState = creatorsState,
                                    likedPostIds = likedPostIds,
                                    bookmarkedPostIds = bookmarkedPostIds,
                                    conversationsCount = conversationsCount,
                                    isDarkTheme = isDarkTheme,
                                    onToggleTheme = onToggleTheme,
                                    onOpenMessagesInbox = { currentTab = InstaTab.MESSAGES },
                                    onOpenCreatePost = { currentTab = InstaTab.CREATE },
                                    onToggleLike = { post -> viewModel.toggleLike(post.id, post.id in likedPostIds) },
                                    onOpenComments = { post -> commentSheetPost = post },
                                    onToggleBookmark = { post -> viewModel.toggleBookmark(post.id, post.id in bookmarkedPostIds) },
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
                                    onToggleFollow = { uid, following -> viewModel.toggleFollow(uid, following) },
                                    onToggleLike = { post -> viewModel.toggleLike(post.id, post.id in likedPostIds) },
                                    onOpenComments = { post -> commentSheetPost = post },
                                    onToggleBookmark = { post -> viewModel.toggleBookmark(post.id, post.id in bookmarkedPostIds) },
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

                            InstaTab.CREATE -> {
                                CreatePostScreen(
                                    isPublishing = isPublishing,
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
                                    selectedPeer = activeChatPeer,
                                    onSelectPeer = { activeChatPeer = it },
                                    buildConversationId = { peerUid -> viewModel.buildConversationId(peerUid) },
                                    observeMessages = { convId -> viewModel.observeMessages(convId) },
                                    onSendMessage = { peer, text -> viewModel.sendMessage(peer, text) },
                                    onOpenProfile = openUserProfile
                                )
                            }

                            InstaTab.PROFILE -> {
                                val ownPostsFlow = remember(viewModel.currentUserId) {
                                    viewModel.observeAuthorPosts(viewModel.currentUserId)
                                }
                                ProfileScreen(
                                    targetUserId = viewModel.currentUserId,
                                    currentUserId = viewModel.currentUserId,
                                    profileFlow = viewModel.myProfileState,
                                    authorPostsFlow = ownPostsFlow,
                                    allPublicPosts = allPublicPosts,
                                    likedPostIds = likedPostIds,
                                    bookmarkedPostIds = bookmarkedPostIds,
                                    followingIds = followingIds,
                                    showBackButton = false,
                                    onBack = {},
                                    onToggleFollow = { uid, following -> viewModel.toggleFollow(uid, following) },
                                    onMessageUser = openDirectChat,
                                    onUpdateProfile = { uname, dname, bio, avatar ->
                                        viewModel.updateProfile(uname, dname, bio, avatar) {}
                                    },
                                    onSignOut = onSignOut,
                                    onToggleLike = { post -> viewModel.toggleLike(post.id, post.id in likedPostIds) },
                                    onOpenComments = { post -> commentSheetPost = post },
                                    onToggleBookmark = { post -> viewModel.toggleBookmark(post.id, post.id in bookmarkedPostIds) },
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
