package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Post
import com.example.data.model.ProfileContentTab
import com.example.data.model.Reel
import com.example.data.model.SavedContentFilter
import com.example.data.model.Story
import com.example.data.model.StoryHighlight
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.ReelsStore
import com.example.ui.components.CreateHighlightDialog
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.FollowConnectionsDialog
import com.example.ui.components.PostCard
import com.example.ui.components.SocialMediaVisual
import com.example.ui.components.StoryAvatarRing
import com.example.ui.theme.StoryRingGradient
import kotlinx.coroutines.flow.Flow

@Composable
fun ProfileScreen(
    targetUserId: String,
    currentUserId: String,
    profileFlow: Flow<UiState<UserProfile?>>,
    authorPostsFlow: Flow<UiState<List<Post>>>,
    highlightsFlow: Flow<UiState<List<StoryHighlight>>>,
    allPublicPosts: List<Post>,
    allCreators: List<UserProfile>,
    allReels: List<Reel>,
    activeStories: List<Story>,
    likedPostIds: Set<String>,
    bookmarkedPostIds: Set<String>,
    followingIds: Set<String>,
    followerIds: Set<String>,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onToggleFollow: (String, Boolean) -> Unit,
    onMessageUser: (UserProfile) -> Unit,
    onStartCall: (UserProfile, String) -> Unit,
    onUpdateProfile: (
        username: String,
        displayName: String,
        bio: String,
        avatarData: String,
        category: String,
        pronouns: String,
        website: String,
        visibility: String
    ) -> Unit,
    onCreateHighlight: (title: String, coverData: String, storyIds: List<String>) -> Unit,
    onOpenStories: (List<Story>) -> Unit,
    onOpenCreateStory: () -> Unit,
    onOpenCreatePost: () -> Unit,
    onOpenCreateReel: () -> Unit,
    onOpenReelById: (String) -> Unit,
    onSearchHashtag: (String) -> Unit,
    onSignOut: () -> Unit,
    onToggleLike: (Post) -> Unit,
    onOpenComments: (Post) -> Unit,
    onToggleBookmark: (Post) -> Unit,
    onSharePost: (Post) -> Unit,
    onEditPost: (Post) -> Unit,
    onOpenProfile: (String) -> Unit,
    onDeletePost: (String) -> Unit
) {
    if (showBackButton) {
        BackHandler(onBack = onBack)
    }

    val context = LocalContext.current
    val isOwnProfile = targetUserId == currentUserId
    val profileState by profileFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    val authorPostsState by authorPostsFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    val highlightsState by highlightsFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)

    var selectedTab by remember(targetUserId) { mutableStateOf(ProfileContentTab.POSTS) }
    var savedFilter by remember { mutableStateOf(SavedContentFilter.ALL) }
    var showEditModal by remember { mutableStateOf(false) }
    var showCreateHighlightModal by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var connectionsDialogInitialTab by remember { mutableStateOf<Int?>(null) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }

    val profile = (profileState as? UiState.Success)?.data
    val authorPosts = (authorPostsState as? UiState.Success)?.data.orEmpty()
    val highlights = (highlightsState as? UiState.Success)?.data.orEmpty()

    val userStories = remember(activeStories, targetUserId) {
        activeStories.filter { it.authorId == targetUserId }
    }
    val userReels = remember(allReels, targetUserId, profile) {
        allReels.filter {
            it.authorId == targetUserId || (profile != null && it.username.equals(profile.username, ignoreCase = true))
        }
    }
    val savedPosts = remember(allPublicPosts, bookmarkedPostIds, authorPosts) {
        (allPublicPosts + authorPosts).distinctBy { it.id }.filter { it.id in bookmarkedPostIds }
    }
    val savedReels = remember(allReels) {
        allReels.filter { it.isBookmarked }
    }
    val taggedPosts = remember(allPublicPosts, profile) {
        val uname = profile?.username?.trim().orEmpty()
        if (uname.isBlank()) emptyList()
        else allPublicPosts.filter {
            it.caption.contains("@$uname", ignoreCase = true) ||
                it.location.contains(uname, ignoreCase = true)
        }
    }

    val availableTabs = remember(isOwnProfile) {
        if (isOwnProfile) {
            listOf(ProfileContentTab.POSTS, ProfileContentTab.REELS, ProfileContentTab.SAVED, ProfileContentTab.TAGGED)
        } else {
            listOf(ProfileContentTab.POSTS, ProfileContentTab.REELS, ProfileContentTab.TAGGED)
        }
    }

    val modalPost = (allPublicPosts + authorPosts + savedPosts).find { it.id == selectedPostId }
    if (modalPost != null) {
        BackHandler { selectedPostId = null }
        Dialog(
            onDismissRequest = { selectedPostId = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                PostCard(
                    post = modalPost,
                    isLiked = modalPost.id in likedPostIds,
                    isBookmarked = modalPost.id in bookmarkedPostIds,
                    isOwnPost = modalPost.authorId == currentUserId,
                    onToggleLike = { onToggleLike(modalPost) },
                    onOpenComments = { onOpenComments(modalPost) },
                    onToggleBookmark = { onToggleBookmark(modalPost) },
                    onSharePost = { onSharePost(modalPost) },
                    onEditPost = if (modalPost.authorId == currentUserId) {
                        {
                            selectedPostId = null
                            onEditPost(modalPost)
                        }
                    } else null,
                    onOpenProfile = { uid ->
                        selectedPostId = null
                        onOpenProfile(uid)
                    },
                    onDirectMessageAuthor = { uid, uname, avatar ->
                        selectedPostId = null
                        onMessageUser(
                            UserProfile(
                                id = uid,
                                userId = uid,
                                username = uname,
                                displayName = uname,
                                avatarData = avatar
                            )
                        )
                    },
                    onDeletePost = if (modalPost.authorId == currentUserId) {
                        {
                            onDeletePost(modalPost.id)
                            selectedPostId = null
                        }
                    } else null
                )
            }
        }
    }

    if (showEditModal && profile != null) {
        EditProfileDialog(
            initialProfile = profile,
            onDismiss = { showEditModal = false },
            onSave = { uname, dname, bio, avatar, cat, pronouns, website, visibility ->
                onUpdateProfile(uname, dname, bio, avatar, cat, pronouns, website, visibility)
                showEditModal = false
            }
        )
    }

    if (showCreateHighlightModal) {
        CreateHighlightDialog(
            ownStories = userStories,
            ownPosts = authorPosts,
            onDismiss = { showCreateHighlightModal = false },
            onSave = { title, cover, storyIds ->
                onCreateHighlight(title, cover, storyIds)
                showCreateHighlightModal = false
            }
        )
    }

    val connTab = connectionsDialogInitialTab
    if (connTab != null && profile != null) {
        val followerProfiles = remember(isOwnProfile, allCreators, followerIds, targetUserId) {
            if (isOwnProfile) allCreators.filter { it.userId in followerIds }
            else allCreators.filter { it.userId != targetUserId }.take(profile.followersCount.toInt().coerceAtMost(20))
        }
        val followingProfiles = remember(isOwnProfile, allCreators, followingIds, targetUserId) {
            if (isOwnProfile) allCreators.filter { it.userId in followingIds }
            else allCreators.filter { it.userId != targetUserId }.take(profile.followingCount.toInt().coerceAtMost(20))
        }
        FollowConnectionsDialog(
            initialTab = connTab,
            profileUsername = profile.username,
            followers = followerProfiles,
            following = followingProfiles,
            currentUserId = currentUserId,
            followingIds = followingIds,
            onToggleFollow = onToggleFollow,
            onOpenProfile = onOpenProfile,
            onDismiss = { connectionsDialogInitialTab = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            }
            if (profile?.visibility == "private") {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Private account",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                )
            }
            Text(
                text = profile?.let { "@${it.username}" } ?: stringResource(R.string.nav_profile),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
            if (isOwnProfile) {
                Box {
                    IconButton(
                        onClick = { showCreateMenu = true },
                        modifier = Modifier.testTag("profile_create_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create content",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    DropdownMenu(
                        expanded = showCreateMenu,
                        onDismissRequest = { showCreateMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Post") },
                            onClick = {
                                showCreateMenu = false
                                onOpenCreatePost()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Story (24h)") },
                            onClick = {
                                showCreateMenu = false
                                onOpenCreateStory()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Reel") },
                            onClick = {
                                showCreateMenu = false
                                onOpenCreateReel()
                            }
                        )
                    }
                }
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = stringResource(R.string.sign_out),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(3) }, key = "profile_header") {
                if (profileState is UiState.Loading && profile == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (profile != null) {
                    ProfileHeaderSection(
                        profile = profile,
                        postsCountOverride = authorPosts.size.toLong().coerceAtLeast(profile.postsCount),
                        hasActiveStories = userStories.isNotEmpty(),
                        isOwnProfile = isOwnProfile,
                        isFollowing = profile.userId in followingIds,
                        onAvatarClick = {
                            if (userStories.isNotEmpty()) {
                                onOpenStories(userStories)
                            } else if (isOwnProfile) {
                                onOpenCreateStory()
                            }
                        },
                        onClickFollowers = { connectionsDialogInitialTab = 0 },
                        onClickFollowing = { connectionsDialogInitialTab = 1 },
                        onEditProfile = { showEditModal = true },
                        onShareProfile = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Follow @${profile.username} (${profile.displayName}) on InstaClone!"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Profile"))
                        },
                        onToggleFollow = { onToggleFollow(profile.userId, profile.userId in followingIds) },
                        onMessage = { onMessageUser(profile) },
                        onVoiceCall = { onStartCall(profile, "voice") },
                        onVideoCall = { onStartCall(profile, "video") },
                        onClickWebsite = { url ->
                            val normalized = if (url.startsWith("http")) url else "https://$url"
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
                            }
                        },
                        onClickHashtag = onSearchHashtag
                    )
                }
            }

            // Story Highlights Row
            if (isOwnProfile || highlights.isNotEmpty()) {
                item(span = { GridItemSpan(3) }, key = "story_highlights_row") {
                    StoryHighlightsRow(
                        highlights = highlights,
                        isOwnProfile = isOwnProfile,
                        onAddHighlight = { showCreateHighlightModal = true },
                        onOpenHighlight = { hl ->
                            val matchingStories = activeStories.filter { it.id in hl.storyIds }
                            if (matchingStories.isNotEmpty()) {
                                onOpenStories(matchingStories)
                            } else {
                                onOpenStories(
                                    listOf(
                                        Story(
                                            id = "hl_${hl.id}",
                                            authorId = targetUserId,
                                            authorUsername = profile?.username ?: "creator",
                                            authorAvatarData = profile?.avatarData.orEmpty(),
                                            mediaData = hl.coverData,
                                            mediaType = "image",
                                            overlayText = hl.title,
                                            caption = hl.title
                                        )
                                    )
                                )
                            }
                        }
                    )
                }
            }

            // Content Tabs (Posts, Reels, Saved, Tagged)
            item(span = { GridItemSpan(3) }, key = "profile_tabs") {
                val activeTabIdx = availableTabs.indexOf(selectedTab).coerceAtLeast(0)
                Column {
                    TabRow(
                        selectedTabIndex = activeTabIdx,
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        availableTabs.forEach { tab ->
                            val icon = when (tab) {
                                ProfileContentTab.POSTS -> Icons.Filled.GridOn
                                ProfileContentTab.REELS -> Icons.Filled.Movie
                                ProfileContentTab.SAVED -> Icons.Filled.Bookmark
                                ProfileContentTab.TAGGED -> Icons.Filled.PersonPin
                            }
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(icon, contentDescription = tab.label, modifier = Modifier.size(20.dp)) },
                                text = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("profile_tab_${tab.name.lowercase()}")
                            )
                        }
                    }

                    if (selectedTab == ProfileContentTab.SAVED && isOwnProfile) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.saved_privacy_notice),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SavedContentFilter.entries.forEach { filter ->
                                    FilterChip(
                                        selected = savedFilter == filter,
                                        onClick = { savedFilter = filter },
                                        label = { Text(filter.label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            when (selectedTab) {
                ProfileContentTab.POSTS -> {
                    if (authorPosts.isEmpty()) {
                        item(span = { GridItemSpan(3) }, key = "empty_posts") {
                            EmptyTabMessage("No posts shared yet.")
                        }
                    } else {
                        items(authorPosts, key = { it.id }) { post ->
                            ProfilePostGridTile(
                                post = post,
                                onClick = { selectedPostId = post.id }
                            )
                        }
                    }
                }

                ProfileContentTab.REELS -> {
                    if (userReels.isEmpty()) {
                        item(span = { GridItemSpan(3) }, key = "empty_reels") {
                            EmptyTabMessage("No reels published yet.")
                        }
                    } else {
                        items(userReels, key = { it.id }) { reel ->
                            ProfileReelGridTile(
                                reel = reel,
                                onClick = { onOpenReelById(reel.id) }
                            )
                        }
                    }
                }

                ProfileContentTab.SAVED -> {
                    val showPosts = savedFilter == SavedContentFilter.ALL || savedFilter == SavedContentFilter.POSTS
                    val showReels = savedFilter == SavedContentFilter.ALL || savedFilter == SavedContentFilter.REELS
                    val visiblePosts = if (showPosts) savedPosts else emptyList()
                    val visibleReels = if (showReels) savedReels else emptyList()

                    if (visiblePosts.isEmpty() && visibleReels.isEmpty()) {
                        item(span = { GridItemSpan(3) }, key = "empty_saved") {
                            EmptyTabMessage("Nothing saved in this filter yet.")
                        }
                    } else {
                        items(visiblePosts, key = { "saved_post_${it.id}" }) { post ->
                            ProfilePostGridTile(
                                post = post,
                                onClick = { selectedPostId = post.id }
                            )
                        }
                        items(visibleReels, key = { "saved_reel_${it.id}" }) { reel ->
                            ProfileReelGridTile(
                                reel = reel,
                                onClick = { onOpenReelById(reel.id) }
                            )
                        }
                    }
                }

                ProfileContentTab.TAGGED -> {
                    if (taggedPosts.isEmpty()) {
                        item(span = { GridItemSpan(3) }, key = "empty_tagged") {
                            EmptyTabMessage("No tagged posts mentioning @${profile?.username ?: "user"} yet.")
                        }
                    } else {
                        items(taggedPosts, key = { "tagged_${it.id}" }) { post ->
                            ProfilePostGridTile(
                                post = post,
                                onClick = { selectedPostId = post.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePostGridTile(
    post: Post,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("profile_post_tile_${post.id}")
    ) {
        SocialMediaVisual(
            imageData = post.imageData,
            contentDescription = post.caption.ifBlank { post.authorUsername },
            modifier = Modifier.fillMaxSize()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(post.likesCount.toString(), style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }
}

@Composable
private fun ProfileReelGridTile(
    reel: Reel,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .clickable(onClick = onClick)
            .testTag("profile_reel_tile_${reel.id}")
    ) {
        AsyncImage(
            model = reel.posterUrl,
            contentDescription = reel.caption,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = ReelsStore.formatCount(reel.viewsCount),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun StoryHighlightsRow(
    highlights: List<StoryHighlight>,
    isOwnProfile: Boolean,
    onAddHighlight: () -> Unit,
    onOpenHighlight: (StoryHighlight) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isOwnProfile) {
            item(key = "add_highlight") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(66.dp)
                        .clickable(onClick = onAddHighlight)
                        .testTag("new_highlight_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_highlight))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.new_highlight),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
        items(highlights, key = { it.id }) { hl ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(66.dp)
                    .clickable { onOpenHighlight(hl) }
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                ) {
                    SocialMediaVisual(
                        imageData = hl.coverData,
                        contentDescription = hl.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hl.title,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyTabMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileHeaderSection(
    profile: UserProfile,
    postsCountOverride: Long,
    hasActiveStories: Boolean,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onAvatarClick: () -> Unit,
    onClickFollowers: () -> Unit,
    onClickFollowing: () -> Unit,
    onEditProfile: () -> Unit,
    onShareProfile: () -> Unit,
    onToggleFollow: () -> Unit,
    onMessage: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit,
    onClickWebsite: (String) -> Unit,
    onClickHashtag: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .then(
                        if (hasActiveStories) {
                            Modifier
                                .border(
                                    2.5.dp,
                                    Brush.sweepGradient(StoryRingGradient + StoryRingGradient.first()),
                                    CircleShape
                                )
                                .padding(4.dp)
                        } else {
                            Modifier
                        }
                    )
                    .clickable(onClick = onAvatarClick)
                    .testTag("profile_avatar_story_trigger"),
                contentAlignment = Alignment.Center
            ) {
                StoryAvatarRing(
                    avatarData = profile.avatarData,
                    username = profile.username,
                    size = 78.dp,
                    showGradientRing = false
                )
            }
            Spacer(modifier = Modifier.width(18.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatColumn(count = postsCountOverride, label = stringResource(R.string.stat_posts))
                ProfileStatColumn(
                    count = profile.followersCount,
                    label = stringResource(R.string.stat_followers),
                    onClick = onClickFollowers
                )
                ProfileStatColumn(
                    count = profile.followingCount,
                    label = stringResource(R.string.stat_following),
                    onClick = onClickFollowing
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (profile.pronouns.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = profile.pronouns,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (profile.category.isNotBlank()) {
            Text(
                text = profile.category,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (profile.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            val bioAnnotated = buildAnnotatedString {
                profile.bio.split(" ").forEachIndexed { idx, word ->
                    if (idx > 0) append(" ")
                    if (word.startsWith("#") || word.startsWith("@")) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                            append(word)
                        }
                    } else {
                        append(word)
                    }
                }
            }
            val firstHashtag = profile.bio.split(" ").firstOrNull { it.startsWith("#") }
            Text(
                text = bioAnnotated,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (firstHashtag != null) {
                    Modifier.clickable { onClickHashtag(firstHashtag.trimStart('#')) }
                } else {
                    Modifier
                }
            )
        }

        if (profile.website.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onClickWebsite(profile.website) }
            ) {
                Icon(
                    imageVector = Icons.Filled.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = profile.website,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isOwnProfile) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onEditProfile,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("edit_profile_button")
                ) {
                    Text(stringResource(R.string.edit_profile), maxLines = 1)
                }
                OutlinedButton(
                    onClick = onShareProfile,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("share_profile_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.share_profile), maxLines = 1)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFollowing) {
                    OutlinedButton(
                        onClick = onToggleFollow,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("follow_user_button")
                    ) {
                        Text(text = stringResource(R.string.following))
                    }
                } else {
                    Button(
                        onClick = onToggleFollow,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("follow_user_button")
                    ) {
                        Text(text = stringResource(R.string.follow))
                    }
                }

                OutlinedButton(
                    onClick = onMessage,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("message_user_button")
                ) {
                    Text(text = stringResource(R.string.message))
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = onVoiceCall) {
                        Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.cd_voice_call))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = onVideoCall) {
                        Icon(Icons.Filled.Videocam, contentDescription = stringResource(R.string.cd_video_call))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun ProfileStatColumn(
    count: Long,
    label: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick).padding(6.dp) else Modifier.padding(6.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
