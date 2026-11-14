package com.example.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.data.bot.AiBotReelsStore
import com.example.data.bot.AiBotReelsUiState
import com.example.data.bot.BotReel
import com.example.ui.components.StoryAvatarRing
import com.example.ui.theme.HeartLikeRed
import com.example.ui.theme.InstaCoral
import com.example.ui.theme.InstaViolet
import kotlinx.coroutines.delay

enum class ReelsFeedFilter(val label: String) {
    ALL("All"),
    AI_BOTS("AI Bots (100)"),
    FOLLOWING("Following")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    uiState: AiBotReelsUiState,
    initialFocusedReelId: String?,
    onClearFocusedReel: () -> Unit,
    onToggleLike: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onToggleFollow: (String?, String) -> Unit,
    onOpenAiBotsHub: () -> Unit,
    onOpenReelsEditor: (BotReel?) -> Unit
) {
    val context = LocalContext.current
    var activeFilter by remember { mutableStateOf(ReelsFeedFilter.ALL) }
    var isMuted by remember { mutableStateOf(true) }
    var commentTargetReelId by remember { mutableStateOf<String?>(null) }

    val filteredReels = remember(uiState.reels, activeFilter) {
        when (activeFilter) {
            ReelsFeedFilter.ALL -> uiState.reels
            ReelsFeedFilter.AI_BOTS -> uiState.reels.filter { it.isAiBot }
            ReelsFeedFilter.FOLLOWING -> uiState.reels.filter { it.isFollowing }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { filteredReels.size }
    )

    LaunchedEffect(initialFocusedReelId, filteredReels) {
        if (initialFocusedReelId != null) {
            val idx = filteredReels.indexOfFirst { it.id == initialFocusedReelId }
            if (idx >= 0) {
                pagerState.scrollToPage(idx)
            } else if (activeFilter != ReelsFeedFilter.ALL) {
                activeFilter = ReelsFeedFilter.ALL
            }
            onClearFocusedReel()
        }
    }

    val commentTargetReel = commentTargetReelId?.let { id -> uiState.reels.find { it.id == id } }
    if (commentTargetReel != null) {
        ReelCommentsSheet(
            reel = commentTargetReel,
            onAddComment = { text -> onAddComment(commentTargetReel.id, text) },
            onDismiss = { commentTargetReelId = null }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (filteredReels.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No reels in this filter yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { activeFilter = ReelsFeedFilter.ALL }) {
                    Text("Show all ${uiState.reels.size} reels")
                }
            }
        } else {
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                key = { page -> filteredReels[page].id },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reels_vertical_pager")
            ) { page ->
                val reel = filteredReels[page]
                val isCurrentlyVisible = pagerState.settledPage == page
                ReelPagerItem(
                    reel = reel,
                    isActivePage = isCurrentlyVisible,
                    isMuted = isMuted,
                    onToggleMute = { isMuted = !isMuted },
                    onDoubleTapLike = {
                        if (!reel.isLiked) onToggleLike(reel.id)
                    },
                    onToggleLike = { onToggleLike(reel.id) },
                    onOpenComments = { commentTargetReelId = reel.id },
                    onToggleFollow = { onToggleFollow(reel.botId, reel.username) },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Watch @${reel.username}'s reel on InstaClone: ${reel.caption}\n${reel.videoUrl}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Reel"))
                    },
                    onEditReel = { onOpenReelsEditor(reel) }
                )
            }
        }

        // Top Overlay Header with Filter Chips, 100 AI Bots Hub, and Reels Edit Studio
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Reels",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = InstaViolet.copy(alpha = 0.9f),
                    modifier = Modifier
                        .clickable(onClick = onOpenAiBotsHub)
                        .testTag("open_ai_bots_hub_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SmartToy,
                            contentDescription = stringResource(R.string.cd_ai_bots_hub),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${uiState.botReelsCount} AI Bots",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = InstaCoral.copy(alpha = 0.92f),
                    modifier = Modifier
                        .clickable { onOpenReelsEditor(null) }
                        .testTag("open_reels_studio_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCut,
                            contentDescription = stringResource(R.string.reels_studio_title),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Edit Studio",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(ReelsFeedFilter.entries) { filter ->
                    val selected = activeFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { activeFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Black.copy(alpha = 0.45f),
                            labelColor = Color.White,
                            selectedContainerColor = Color.White,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ReelPagerItem(
    reel: BotReel,
    isActivePage: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onToggleFollow: () -> Unit,
    onShare: () -> Unit,
    onEditReel: () -> Unit
) {
    var showHeartBurst by remember { mutableStateOf(false) }
    var isVideoPrepared by remember { mutableStateOf(false) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(700L)
            showHeartBurst = false
        }
    }

    LaunchedEffect(isMuted, mediaPlayerRef) {
        val vol = if (isMuted) 0f else 1f
        runCatching { mediaPlayerRef?.setVolume(vol, vol) }
    }

    val likeScale by animateFloatAsState(
        targetValue = if (reel.isLiked) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "reelLikeScale"
    )

    val filterTint: Color = when (reel.filterPreset) {
        "Cinema" -> Color(0x280D1B2A)
        "Warm Sunset" -> Color(0x30FF7A45)
        "Neon Cyber" -> Color(0x30833AB4)
        "B&W" -> Color(0x45181818)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(reel.id, reel.isLiked) {
                detectTapGestures(
                    onDoubleTap = {
                        showHeartBurst = true
                        onDoubleTapLike()
                    },
                    onTap = { onToggleMute() }
                )
            }
    ) {
        // Poster thumbnail underneath for instant visual continuity while streaming
        AsyncImage(
            model = reel.posterUrl,
            contentDescription = reel.caption,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Native VideoView active only when nearby/visible
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setOnPreparedListener { mp ->
                        isVideoPrepared = true
                        mediaPlayerRef = mp
                        mp.isLooping = true
                        val vol = if (isMuted) 0f else 1f
                        mp.setVolume(vol, vol)
                        if (isActivePage) {
                            start()
                        }
                    }
                    setOnErrorListener { _, _, _ ->
                        // Gracefully keep poster visual without modal popup if offline
                        isVideoPrepared = false
                        true
                    }
                    setVideoURI(Uri.parse(reel.videoUrl))
                }
            },
            update = { videoView ->
                runCatching {
                    val vol = if (isMuted) 0f else 1f
                    mediaPlayerRef?.setVolume(vol, vol)
                    if (isActivePage) {
                        if (!videoView.isPlaying && isVideoPrepared) {
                            videoView.start()
                        }
                    } else {
                        if (videoView.isPlaying) {
                            videoView.pause()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(reel.id) {
            onDispose {
                mediaPlayerRef = null
            }
        }

        if (filterTint != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(filterTint)
            )
        }

        // Bottom gradient scrim for crisp text legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )

        // Custom overlay sticker text if added in Reels Edit Studio
        if (reel.overlayText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = reel.overlayText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Double-Tap Heart Animation
        AnimatedVisibility(
            visible = showHeartBurst,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(104.dp)
            )
        }

        // Right-side Instagram Engagement Action Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier.testTag("reel_like_${reel.id}")
                ) {
                    Icon(
                        imageVector = if (reel.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like reel",
                        tint = if (reel.isLiked) HeartLikeRed else Color.White,
                        modifier = Modifier
                            .size(30.dp)
                            .scale(likeScale)
                    )
                }
                Text(
                    text = AiBotReelsStore.formatFollowerCount(reel.likesCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onOpenComments,
                    modifier = Modifier.testTag("reel_comment_${reel.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment on reel",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = reel.comments.size.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier.testTag("reel_share_${reel.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share reel",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(
                onClick = onEditReel,
                modifier = Modifier.testTag("reel_edit_${reel.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCut,
                    contentDescription = "Remix or edit reel",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .testTag("reel_mute_${reel.id}")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute reel" else "Mute reel",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Bottom-left Creator Info, 24h Expiry Badge, Caption & Scrolling Audio
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 80.dp, bottom = 22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (reel.isAiBot) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = InstaViolet.copy(alpha = 0.88f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AI BOT • 1/1 DAILY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.border(
                            1.dp,
                            Color(0xFFFFC837).copy(alpha = 0.65f),
                            RoundedCornerShape(10.dp)
                        )
                    ) {
                        Text(
                            text = AiBotReelsStore.formatExpiresInBadge(reel.createdAtMillis),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD166),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StoryAvatarRing(
                    avatarData = reel.avatarUrl,
                    username = reel.username,
                    size = 38.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "@${reel.username}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (reel.isFollowing) Color.White.copy(alpha = 0.22f) else Color.Transparent,
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleFollow)
                        .testTag("reel_follow_${reel.id}")
                ) {
                    Text(
                        text = if (reel.isFollowing) "Following" else "Follow",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = reel.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${reel.audioTrack} • ${reel.niche} • Tap for sound",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReelCommentsSheet(
    reel: BotReel,
    onAddComment: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf("") }

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
                text = "Comments (${reel.comments.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 10.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 320.dp)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (reel.comments.isEmpty()) {
                    item {
                        Text(
                            text = "No comments yet. Say something nice!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(reel.comments, key = { it.id }) { comment ->
                        Column {
                            Text(
                                text = "@${comment.author}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = comment.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
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
                    value = draft,
                    onValueChange = { draft = it.take(400) },
                    placeholder = { Text(stringResource(R.string.add_comment_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        val trimmed = draft.trim()
                        if (trimmed.isNotEmpty()) {
                            onAddComment(trimmed)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(stringResource(R.string.post_comment))
                }
            }
        }
    }
}
