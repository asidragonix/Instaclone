package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Comment
import com.example.data.model.Post
import com.example.data.model.UiState
import com.example.ui.theme.HeartLikeRed
import com.example.ui.theme.StoryRingGradient
import com.example.util.ImageEncoder
import com.example.util.TimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@Composable
fun StoryAvatarRing(
    avatarData: String,
    username: String,
    size: Dp = 44.dp,
    showGradientRing: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clickableMod = if (onClick != null) {
        modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    } else {
        modifier
    }

    Box(
        modifier = clickableMod
            .size(size)
            .then(
                if (showGradientRing) {
                    Modifier
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(StoryRingGradient + StoryRingGradient.first()),
                            shape = CircleShape
                        )
                        .padding(3.dp)
                } else {
                    Modifier
                }
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (avatarData.isNotBlank()) {
            SocialMediaVisual(
                imageData = avatarData,
                contentDescription = stringResource(R.string.cd_user_avatar),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val initial = username.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            Text(
                text = initial,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SocialMediaVisual(
    imageData: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    when {
        imageData.startsWith("data:image") -> {
            val bitmapState by produceState<Bitmap?>(initialValue = null, key1 = imageData) {
                value = ImageEncoder.decodeDataUrlToBitmap(imageData)
            }
            val bmp = bitmapState
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = modifier
                )
            } else {
                Box(
                    modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }

        imageData.startsWith("preset://") -> {
            val drawableRes = ImageEncoder.presetDrawableRes(imageData)
            if (drawableRes != null) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = modifier
                )
            } else {
                StudioGradientScene(presetUri = imageData, modifier = modifier)
            }
        }

        else -> {
            AsyncImage(
                model = imageData,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun StudioGradientScene(
    presetUri: String,
    modifier: Modifier = Modifier
) {
    val palette = when (presetUri) {
        "preset://sunset_horizon" -> listOf(Color(0xFF1F1135), Color(0xFFC13584), Color(0xFFFF7A45), Color(0xFFFFD166))
        "preset://cyber_tokyo" -> listOf(Color(0xFF07071A), Color(0xFF2B1055), Color(0xFF833AB4), Color(0xFF00F5D4))
        "preset://coastal_breeze" -> listOf(Color(0xFF082032), Color(0xFF11698E), Color(0xFF16C79A), Color(0xFFF8F1F1))
        else -> listOf(Color(0xFF23190F), Color(0xFF8C4A27), Color(0xFFD9822B), Color(0xFFF6D58E))
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.linearGradient(
                colors = palette,
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.42f), Color.Transparent),
                center = Offset(w * 0.72f, h * 0.28f),
                radius = w * 0.36f
            ),
            radius = w * 0.36f,
            center = Offset(w * 0.72f, h * 0.28f)
        )
        drawCircle(
            color = palette.first().copy(alpha = 0.45f),
            radius = w * 0.55f,
            center = Offset(w * 0.22f, h * 0.92f)
        )
    }
}

@Composable
fun PostCard(
    post: Post,
    isLiked: Boolean,
    isBookmarked: Boolean,
    isOwnPost: Boolean,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onDirectMessageAuthor: (String, String, String) -> Unit,
    onDeletePost: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showHeartBurst by remember { mutableStateOf(false) }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(700L)
            showHeartBurst = false
        }
    }

    val likeIconScale by animateFloatAsState(
        targetValue = if (isLiked) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "likeScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Post Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StoryAvatarRing(
                    avatarData = post.authorAvatarData,
                    username = post.authorUsername,
                    size = 42.dp,
                    onClick = { onOpenProfile(post.authorId) }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenProfile(post.authorId) }
                ) {
                    Text(
                        text = post.authorUsername,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (post.location.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = post.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (isOwnPost && onDeletePost != null) {
                    IconButton(
                        onClick = onDeletePost,
                        modifier = Modifier.testTag("delete_post_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete_post),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Double-Tap Square Post Visual
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(post.id, isLiked) {
                        detectTapGestures(
                            onDoubleTap = {
                                showHeartBurst = true
                                if (!isLiked) {
                                    onToggleLike()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                SocialMediaVisual(
                    imageData = post.imageData,
                    contentDescription = stringResource(R.string.cd_post_image),
                    modifier = Modifier.fillMaxSize()
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = showHeartBurst,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(96.dp)
                    )
                }
            }

            // Engagement Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier.testTag("like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.cd_like_post),
                        tint = if (isLiked) HeartLikeRed else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.scale(likeIconScale)
                    )
                }

                IconButton(
                    onClick = onOpenComments,
                    modifier = Modifier.testTag("comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = stringResource(R.string.cd_comment_post),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!isOwnPost) {
                    IconButton(
                        onClick = {
                            onDirectMessageAuthor(
                                post.authorId,
                                post.authorUsername,
                                post.authorAvatarData
                            )
                        },
                        modifier = Modifier.testTag("dm_author_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_message_author),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.testTag("bookmark_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(R.string.cd_bookmark_post),
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Likes, Caption, Comments count, Timestamp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (post.likesCount == 1L) "1 like" else "${post.likesCount} likes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (post.caption.isNotBlank()) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(post.authorUsername)
                                append("  ")
                            }
                            append(post.caption)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (post.commentsCount > 0L) {
                    Text(
                        text = if (post.commentsCount == 1L) {
                            "View 1 comment"
                        } else {
                            "View all ${post.commentsCount} comments"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable(onClick = onOpenComments)
                            .padding(vertical = 2.dp)
                    )
                }

                Text(
                    text = TimeFormatter.formatRelative(post.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: Post,
    commentsFlow: Flow<UiState<List<Comment>>>,
    onAddComment: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val commentsState by commentsFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    var commentDraft by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.comments_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 360.dp)
                    .padding(vertical = 12.dp)
            ) {
                when (val state = commentsState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is UiState.Error -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_comments_yet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(state.data, key = { it.id }) { comment ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        StoryAvatarRing(
                                            avatarData = comment.authorAvatarData,
                                            username = comment.authorUsername,
                                            size = 36.dp,
                                            showGradientRing = false,
                                            onClick = {
                                                onDismiss()
                                                onOpenProfile(comment.authorId)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = comment.authorUsername,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = TimeFormatter.formatRelative(comment.createdAt),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = comment.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { commentDraft = it.take(500) },
                    placeholder = { Text(stringResource(R.string.add_comment_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field")
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        val trimmed = commentDraft.trim()
                        if (trimmed.isNotEmpty()) {
                            onAddComment(trimmed)
                            commentDraft = ""
                        }
                    },
                    enabled = commentDraft.isNotBlank(),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("submit_comment_button")
                ) {
                    Text(
                        text = stringResource(R.string.post_comment),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
