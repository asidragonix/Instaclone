package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.PostCard
import com.example.ui.components.SocialMediaVisual
import com.example.ui.components.StoryAvatarRing

@Composable
fun ExploreScreen(
    currentUserId: String,
    postsState: UiState<List<Post>>,
    creatorsState: UiState<List<UserProfile>>,
    likedPostIds: Set<String>,
    bookmarkedPostIds: Set<String>,
    followingIds: Set<String>,
    onToggleFollow: (String, Boolean) -> Unit,
    onToggleLike: (Post) -> Unit,
    onOpenComments: (Post) -> Unit,
    onToggleBookmark: (Post) -> Unit,
    onOpenProfile: (String) -> Unit,
    onDirectMessageAuthor: (String, String, String) -> Unit,
    onDeletePost: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPostId by remember { mutableStateOf<String?>(null) }

    val allPosts = (postsState as? UiState.Success)?.data.orEmpty()
    val allCreators = (creatorsState as? UiState.Success)?.data.orEmpty()

    val filteredCreators by remember(searchQuery, allCreators, currentUserId) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            allCreators
                .filter { it.userId != currentUserId }
                .filter {
                    q.isEmpty() ||
                        it.username.lowercase().contains(q) ||
                        it.displayName.lowercase().contains(q) ||
                        it.bio.lowercase().contains(q)
                }
        }
    }

    val filteredPosts by remember(searchQuery, allPosts) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            val matching = if (q.isEmpty()) {
                allPosts
            } else {
                allPosts.filter {
                    it.caption.lowercase().contains(q) ||
                        it.authorUsername.lowercase().contains(q) ||
                        it.location.lowercase().contains(q)
                }
            }
            matching.sortedByDescending { it.likesCount * 2L + it.commentsCount }
        }
    }

    val selectedPost = allPosts.find { it.id == selectedPostId }
    if (selectedPost != null) {
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
                    post = selectedPost,
                    isLiked = selectedPost.id in likedPostIds,
                    isBookmarked = selectedPost.id in bookmarkedPostIds,
                    isOwnPost = selectedPost.authorId == currentUserId,
                    onToggleLike = { onToggleLike(selectedPost) },
                    onOpenComments = { onOpenComments(selectedPost) },
                    onToggleBookmark = { onToggleBookmark(selectedPost) },
                    onOpenProfile = { uid ->
                        selectedPostId = null
                        onOpenProfile(uid)
                    },
                    onDirectMessageAuthor = { uid, uname, avatar ->
                        selectedPostId = null
                        onDirectMessageAuthor(uid, uname, avatar)
                    },
                    onDeletePost = if (selectedPost.authorId == currentUserId) {
                        {
                            onDeletePost(selectedPost.id)
                            selectedPostId = null
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("explore_search_input")
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 115.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredCreators.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "creators_header") {
                    Column {
                        Text(
                            text = stringResource(R.string.creators_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            items(filteredCreators, key = { it.userId }) { user ->
                                val isFollowing = user.userId in followingIds
                                CreatorDiscoveryCard(
                                    profile = user,
                                    isFollowing = isFollowing,
                                    onToggleFollow = { onToggleFollow(user.userId, isFollowing) },
                                    onOpenProfile = { onOpenProfile(user.userId) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "moments_header") {
                Text(
                    text = stringResource(R.string.trending_posts_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
            }

            if (postsState is UiState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "grid_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredPosts.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "grid_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching posts found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedPostId = post.id }
                            .testTag("explore_tile_${post.id}")
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
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.58f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = post.likesCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorDiscoveryCard(
    profile: UserProfile,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(156.dp)
            .clickable(onClick = onOpenProfile)
            .testTag("creator_card_${profile.userId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StoryAvatarRing(
                avatarData = profile.avatarData,
                username = profile.username,
                size = 56.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.username,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (isFollowing) {
                OutlinedButton(
                    onClick = onToggleFollow,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.following),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else {
                Button(
                    onClick = onToggleFollow,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.follow),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
