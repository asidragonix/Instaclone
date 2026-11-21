package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.HomeFeedMode
import com.example.data.model.Post
import com.example.data.model.Reel
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.ReelsStore
import com.example.ui.components.PostCard
import com.example.ui.components.SocialMediaVisual
import com.example.ui.components.StoriesTrayRow
import com.example.ui.components.StoryAvatarRing
import com.example.ui.theme.InstaCoral

@Composable
fun HomeFeedScreen(
    currentUserId: String,
    currentUserProfile: UserProfile?,
    feedPostsState: UiState<List<Post>>,
    storiesState: UiState<List<Story>>,
    reels: List<Reel>,
    viewedStoryIds: Set<String>,
    likedPostIds: Set<String>,
    bookmarkedPostIds: Set<String>,
    conversationsCount: Int,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenReelsWatch: (String?) -> Unit,
    onOpenMessagesInbox: () -> Unit,
    onOpenCreatePost: () -> Unit,
    onOpenCreateStory: () -> Unit,
    onOpenStoryGroup: (List<Story>) -> Unit,
    onToggleLike: (Post) -> Unit,
    onOpenComments: (Post) -> Unit,
    onToggleBookmark: (Post) -> Unit,
    onSharePost: (Post) -> Unit,
    onEditPost: (Post) -> Unit,
    onOpenProfile: (String) -> Unit,
    onDirectMessageAuthor: (String, String, String) -> Unit,
    onDeletePost: (String) -> Unit
) {
    var feedMode by remember { mutableStateOf(HomeFeedMode.MIXED) }
    var gridPage by remember { mutableIntStateOf(0) }
    var inspectedPost by remember { mutableStateOf<Post?>(null) }

    val activeStories = (storiesState as? UiState.Success)?.data.orEmpty()

    if (inspectedPost != null) {
        val livePost = (feedPostsState as? UiState.Success)?.data
            ?.find { it.id == inspectedPost!!.id } ?: inspectedPost!!
        BackHandler { inspectedPost = null }
        Dialog(
            onDismissRequest = { inspectedPost = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                PostCard(
                    post = livePost,
                    isLiked = livePost.id in likedPostIds,
                    isBookmarked = livePost.id in bookmarkedPostIds,
                    isOwnPost = livePost.authorId == currentUserId,
                    onToggleLike = { onToggleLike(livePost) },
                    onOpenComments = { onOpenComments(livePost) },
                    onToggleBookmark = { onToggleBookmark(livePost) },
                    onSharePost = { onSharePost(livePost) },
                    onEditPost = if (livePost.authorId == currentUserId) {
                        {
                            inspectedPost = null
                            onEditPost(livePost)
                        }
                    } else null,
                    onOpenProfile = { uid ->
                        inspectedPost = null
                        onOpenProfile(uid)
                    },
                    onDirectMessageAuthor = { uid, uname, avatar ->
                        inspectedPost = null
                        onDirectMessageAuthor(uid, uname, avatar)
                    },
                    onDeletePost = if (livePost.authorId == currentUserId) {
                        {
                            onDeletePost(livePost.id)
                            inspectedPost = null
                        }
                    } else null
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Brand Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { onOpenReelsWatch(null) },
                modifier = Modifier.testTag("home_watch_reels_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = stringResource(R.string.cd_watch_reels),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = stringResource(R.string.cd_toggle_theme),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = onOpenMessagesInbox,
                modifier = Modifier.testTag("open_dm_inbox_button")
            ) {
                BadgedBox(
                    badge = {
                        if (conversationsCount > 0) {
                            Badge { Text(conversationsCount.coerceAtMost(99).toString()) }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = stringResource(R.string.cd_open_messages),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .testTag("home_feed_list"),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Stories Tray (Your Story + Friends' Stories)
            item(key = "stories_tray") {
                StoriesTrayRow(
                    currentUserProfile = currentUserProfile,
                    currentUserId = currentUserId,
                    stories = activeStories,
                    viewedStoryIds = viewedStoryIds,
                    onCreateStory = onOpenCreateStory,
                    onOpenAuthorStories = onOpenStoryGroup
                )
            }

            // 2. Feed Mode Toggle: Mixed Feed vs Posts Only (6)
            item(key = "feed_mode_toggle") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    FilterChip(
                        selected = feedMode == HomeFeedMode.MIXED,
                        onClick = { feedMode = HomeFeedMode.MIXED },
                        leadingIcon = { Icon(Icons.Filled.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(HomeFeedMode.MIXED.label) },
                        modifier = Modifier.testTag("feed_mode_mixed_chip")
                    )
                    FilterChip(
                        selected = feedMode == HomeFeedMode.POSTS_GRID,
                        onClick = {
                            feedMode = HomeFeedMode.POSTS_GRID
                            gridPage = 0
                        },
                        leadingIcon = { Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(HomeFeedMode.POSTS_GRID.label) },
                        modifier = Modifier.testTag("feed_mode_grid_chip")
                    )
                }
            }

            when (feedPostsState) {
                is UiState.Loading -> {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UiState.Error -> {
                    item(key = "error") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = feedPostsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    val posts = feedPostsState.data
                    if (posts.isEmpty()) {
                        item(key = "empty_feed") {
                            EmptyFeedCard(onOpenCreatePost = onOpenCreatePost)
                        }
                    } else if (feedMode == HomeFeedMode.POSTS_GRID) {
                        val pageSize = 6
                        val totalPages = ((posts.size - 1) / pageSize) + 1
                        val safePage = gridPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                        val pagePosts = posts.drop(safePage * pageSize).take(pageSize)

                        item(key = "posts_grid_6") {
                            PostsSixGridCard(
                                posts = pagePosts,
                                currentPage = safePage,
                                totalPages = totalPages,
                                onPrevPage = { if (safePage > 0) gridPage = safePage - 1 },
                                onNextPage = { if (safePage < totalPages - 1) gridPage = safePage + 1 },
                                onInspectPost = { inspectedPost = it }
                            )
                        }
                    } else {
                        itemsIndexed(posts, key = { _, p -> p.id }) { index, post ->
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                PostCard(
                                    post = post,
                                    isLiked = post.id in likedPostIds,
                                    isBookmarked = post.id in bookmarkedPostIds,
                                    isOwnPost = post.authorId == currentUserId,
                                    onToggleLike = { onToggleLike(post) },
                                    onOpenComments = { onOpenComments(post) },
                                    onToggleBookmark = { onToggleBookmark(post) },
                                    onSharePost = { onSharePost(post) },
                                    onEditPost = if (post.authorId == currentUserId) {
                                        { onEditPost(post) }
                                    } else null,
                                    onOpenProfile = onOpenProfile,
                                    onDirectMessageAuthor = onDirectMessageAuthor,
                                    onDeletePost = if (post.authorId == currentUserId) {
                                        { onDeletePost(post.id) }
                                    } else null,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                // Interleave a Featured Reel Card every 2 posts in Mixed Feed
                                if (reels.isNotEmpty() && index % 2 == 0) {
                                    val reel = reels[(index / 2) % reels.size]
                                    FeaturedInlineReelCard(
                                        reel = reel,
                                        onWatchReel = { onOpenReelsWatch(reel.id) },
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedInlineReelCard(
    reel: Reel,
    onWatchReel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onWatchReel)
            .testTag("mixed_feed_reel_${reel.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            AsyncImage(
                model = reel.posterUrl,
                contentDescription = reel.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.25f),
                            1f to Color.Black.copy(alpha = 0.8f)
                        )
                    )
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = InstaCoral,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("REEL", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Watch reel",
                    tint = Color.White,
                    modifier = Modifier.padding(14.dp).size(34.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                StoryAvatarRing(
                    avatarData = reel.avatarUrl,
                    username = reel.username,
                    size = 34.dp,
                    showGradientRing = false
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${reel.username}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reel.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${ReelsStore.formatCount(reel.likesCount)} likes",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PostsSixGridCard(
    posts: List<Post>,
    currentPage: Int,
    totalPages: Int,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onInspectPost: (Post) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        val rows = posts.chunked(3)
        rows.forEach { rowPosts ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowPosts.forEach { post ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onInspectPost(post) }
                            .testTag("home_grid_post_${post.id}")
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
                                .padding(6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(post.likesCount.toString(), style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
                repeat(3 - rowPosts.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        if (totalPages > 1) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                OutlinedButton(onClick = onPrevPage, enabled = currentPage > 0) {
                    Text("Previous")
                }
                Text(
                    text = "Page ${currentPage + 1} of $totalPages",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onNextPage, enabled = currentPage < totalPages - 1) {
                    Text("Next")
                }
            }
        }
    }
}

@Composable
private fun EmptyFeedCard(
    onOpenCreatePost: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_feed),
                contentDescription = stringResource(R.string.empty_feed_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.empty_feed_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_feed_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onOpenCreatePost,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("empty_create_post_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.create_first_post))
            }
        }
    }
}
