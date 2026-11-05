package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.PostCard
import com.example.ui.components.SocialMediaVisual
import com.example.ui.components.StoryAvatarRing
import com.example.util.ImageEncoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    targetUserId: String,
    currentUserId: String,
    profileFlow: Flow<UiState<UserProfile?>>,
    authorPostsFlow: Flow<UiState<List<Post>>>,
    allPublicPosts: List<Post>,
    likedPostIds: Set<String>,
    bookmarkedPostIds: Set<String>,
    followingIds: Set<String>,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onToggleFollow: (String, Boolean) -> Unit,
    onMessageUser: (UserProfile) -> Unit,
    onUpdateProfile: (username: String, displayName: String, bio: String, avatarData: String) -> Unit,
    onSignOut: () -> Unit,
    onToggleLike: (Post) -> Unit,
    onOpenComments: (Post) -> Unit,
    onToggleBookmark: (Post) -> Unit,
    onOpenProfile: (String) -> Unit,
    onDeletePost: (String) -> Unit
) {
    if (showBackButton) {
        BackHandler(onBack = onBack)
    }

    val isOwnProfile = targetUserId == currentUserId
    val profileState by profileFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    val authorPostsState by authorPostsFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)

    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditModal by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }

    val profile = (profileState as? UiState.Success)?.data
    val authorPosts = (authorPostsState as? UiState.Success)?.data.orEmpty()
    val savedPosts = remember(allPublicPosts, bookmarkedPostIds, authorPosts) {
        (allPublicPosts + authorPosts)
            .distinctBy { it.id }
            .filter { it.id in bookmarkedPostIds }
    }

    val activeGridPosts = if (isOwnProfile && selectedTab == 1) savedPosts else authorPosts

    val modalPost = (authorPosts + savedPosts).find { it.id == selectedPostId }
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
                    } else {
                        null
                    }
                )
            }
        }
    }

    if (showEditModal && profile != null) {
        EditProfileDialog(
            initialProfile = profile,
            onDismiss = { showEditModal = false },
            onSave = { uname, dname, bio, avatar ->
                onUpdateProfile(uname, dname, bio, avatar)
                showEditModal = false
            }
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
            Text(
                text = profile?.let { "@${it.username}" } ?: stringResource(R.string.nav_profile),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )
            if (isOwnProfile) {
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
            columns = GridCells.Adaptive(minSize = 115.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "profile_header") {
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
                        isOwnProfile = isOwnProfile,
                        isFollowing = profile.userId in followingIds,
                        onEditProfile = { showEditModal = true },
                        onToggleFollow = { onToggleFollow(profile.userId, profile.userId in followingIds) },
                        onMessage = { onMessageUser(profile) }
                    )
                }
            }

            if (isOwnProfile) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "profile_tabs") {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Filled.GridOn, contentDescription = null) },
                            text = { Text(stringResource(R.string.tab_posts)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Filled.Bookmark, contentDescription = null) },
                            text = { Text(stringResource(R.string.tab_saved)) }
                        )
                    }
                }
            }

            if (activeGridPosts.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "empty_grid") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isOwnProfile && selectedTab == 1) {
                                "No saved posts yet."
                            } else {
                                "No posts shared yet."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(activeGridPosts, key = { it.id }) { post ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedPostId = post.id }
                            .testTag("profile_post_tile_${post.id}")
                    ) {
                        SocialMediaVisual(
                            imageData = post.imageData,
                            contentDescription = post.caption.ifBlank { post.authorUsername },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderSection(
    profile: UserProfile,
    postsCountOverride: Long,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onEditProfile: () -> Unit,
    onToggleFollow: () -> Unit,
    onMessage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StoryAvatarRing(
                avatarData = profile.avatarData,
                username = profile.username,
                size = 84.dp
            )
            Spacer(modifier = Modifier.width(20.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatColumn(count = postsCountOverride, label = stringResource(R.string.stat_posts))
                ProfileStatColumn(count = profile.followersCount, label = stringResource(R.string.stat_followers))
                ProfileStatColumn(count = profile.followingCount, label = stringResource(R.string.stat_following))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = profile.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (profile.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isOwnProfile) {
            OutlinedButton(
                onClick = onEditProfile,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("edit_profile_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.edit_profile))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isFollowing) {
                    OutlinedButton(
                        onClick = onToggleFollow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("follow_user_button")
                    ) {
                        Text(text = stringResource(R.string.following))
                    }
                } else {
                    Button(
                        onClick = onToggleFollow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("follow_user_button")
                    ) {
                        Text(text = stringResource(R.string.follow))
                    }
                }

                OutlinedButton(
                    onClick = onMessage,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("message_user_button")
                ) {
                    Text(text = stringResource(R.string.message))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ProfileStatColumn(count: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

@Composable
private fun EditProfileDialog(
    initialProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (username: String, displayName: String, bio: String, avatarData: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf(initialProfile.username) }
    var displayName by remember { mutableStateOf(initialProfile.displayName) }
    var bio by remember { mutableStateOf(initialProfile.bio) }
    var avatarData by remember { mutableStateOf(initialProfile.avatarData) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                ImageEncoder.encodeUriToDataUrl(
                    context = context,
                    uri = uri,
                    maxDimension = 256,
                    jpegQuality = 75
                ).onSuccess { encoded ->
                    avatarData = encoded
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_profile)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StoryAvatarRing(
                    avatarData = avatarData,
                    username = username,
                    size = 76.dp
                )
                TextButton(
                    onClick = {
                        avatarPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.change_avatar))
                }
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(50) },
                    label = { Text(stringResource(R.string.edit_display_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.lowercase().take(28) },
                    label = { Text(stringResource(R.string.edit_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(200) },
                    label = { Text(stringResource(R.string.edit_bio)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(username, displayName, bio, avatarData) },
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text(stringResource(R.string.save_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
