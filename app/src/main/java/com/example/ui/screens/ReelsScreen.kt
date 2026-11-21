package com.example.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Reel
import com.example.data.model.ReelsUiState
import com.example.data.repository.ReelsStore
import com.example.ui.components.StoryAvatarRing
import com.example.ui.theme.HeartLikeRed
import com.example.ui.theme.InstaCoral
import kotlinx.coroutines.delay

enum class ReelsFeedFilter(val label: String) {
    ALL("All"),
    FOLLOWING("Following")
}

private val speedOptions = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    uiState: ReelsUiState,
    currentUserId: String,
    currentUsername: String,
    initialFocusedReelId: String?,
    onClearFocusedReel: () -> Unit,
    onToggleLike: (String) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onDeleteComment: (String, String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onShareReel: (Reel) -> Unit,
    onOpenReelsEditor: (Reel?) -> Unit,
    onEditReelCaption: (Reel) -> Unit,
    onDeleteReel: (String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    var activeFilter by remember { mutableStateOf(ReelsFeedFilter.ALL) }
    var isMuted by remember { mutableStateOf(true) }
    var commentTargetReelId by remember { mutableStateOf<String?>(null) }

    val filteredReels = remember(uiState.reels, activeFilter) {
        when (activeFilter) {
            ReelsFeedFilter.ALL -> uiState.reels
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
            currentUsername = currentUsername,
            onAddComment = { text -> onAddComment(commentTargetReel.id, text) },
            onDeleteComment = { commentId -> onDeleteComment(commentTargetReel.id, commentId) },
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
                    imageVector = Icons.Filled.Movie,
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
                    isOwnReel = reel.authorId == currentUserId || reel.username == currentUsername,
                    isActivePage = isCurrentlyVisible,
                    isMuted = isMuted,
                    onToggleMute = { isMuted = !isMuted },
                    onDoubleTapLike = {
                        if (!reel.isLiked) onToggleLike(reel.id)
                    },
                    onToggleLike = { onToggleLike(reel.id) },
                    onToggleBookmark = { onToggleBookmark(reel.id) },
                    onOpenComments = { commentTargetReelId = reel.id },
                    onToggleFollow = { onToggleFollow(reel.username) },
                    onShare = { onShareReel(reel) },
                    onRemixReel = { onOpenReelsEditor(reel) },
                    onEditCaption = { onEditReelCaption(reel) },
                    onDeleteReel = { onDeleteReel(reel.id) },
                    onOpenProfile = {
                        if (reel.authorId.isNotBlank()) onOpenProfile(reel.authorId)
                    }
                )
            }
        }

        // Top Overlay Header with Filter Chips and Reels Edit Studio
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
    reel: Reel,
    isOwnReel: Boolean,
    isActivePage: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenComments: () -> Unit,
    onToggleFollow: () -> Unit,
    onShare: () -> Unit,
    onRemixReel: () -> Unit,
    onEditCaption: () -> Unit,
    onDeleteReel: () -> Unit,
    onOpenProfile: () -> Unit
) {
    var showHeartBurst by remember { mutableStateOf(false) }
    var isVideoPrepared by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var playbackSpeed by remember(reel.id) { mutableFloatStateOf(reel.playbackSpeed.coerceIn(0.5f, 2.0f)) }
    var showSpeedHud by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
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

    LaunchedEffect(playbackSpeed, mediaPlayerRef, isVideoPrepared) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val mp = mediaPlayerRef ?: return@runCatching
                mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
            }
        }
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
                    onLongPress = {
                        showSpeedHud = !showSpeedHud
                    },
                    onTap = {
                        if (showSpeedHud) {
                            showSpeedHud = false
                        } else {
                            isPaused = !isPaused
                        }
                    }
                )
            }
    ) {
        AsyncImage(
            model = reel.posterUrl,
            contentDescription = reel.caption,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setOnPreparedListener { mp ->
                        isVideoPrepared = true
                        mediaPlayerRef = mp
                        mp.isLooping = true
                        val vol = if (isMuted) 0f else 1f
                        mp.setVolume(vol, vol)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            runCatching {
                                mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                            }
                        }
                        if (isActivePage && !isPaused) {
                            start()
                        }
                    }
                    setOnErrorListener { _, _, _ ->
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
                    if (isActivePage && !isPaused) {
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

        // Play/Pause Indicator when paused
        if (isPaused) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Paused - tap to resume",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(44.dp)
                )
            }
        }

        // Long-Press Speed Selector HUD
        AnimatedVisibility(
            visible = showSpeedHud,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                    .testTag("reel_speed_hud_${reel.id}")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Speed, contentDescription = null, tint = InstaCoral, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Playback Speed",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        speedOptions.forEach { spd ->
                            val selected = playbackSpeed == spd
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) InstaCoral else Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .clickable {
                                        playbackSpeed = spd
                                        showSpeedHud = false
                                    }
                                    .testTag("speed_option_${spd}x")
                            ) {
                                Text(
                                    text = if (spd == 1.0f) "1x" else "${spd}x",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Hold video anytime to change speed",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
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

        // Right-side Engagement Action Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 22.dp)
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
                    text = ReelsStore.formatCount(reel.likesCount),
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
                onClick = onToggleBookmark,
                modifier = Modifier.testTag("reel_bookmark_${reel.id}")
            ) {
                Icon(
                    imageVector = if (reel.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save reel",
                    tint = if (reel.isBookmarked) InstaCoral else Color.White,
                    modifier = Modifier.size(26.dp)
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

            // Speed Pill Button (also triggered via long-press)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (playbackSpeed != 1.0f) InstaCoral else Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .clickable { showSpeedHud = !showSpeedHud }
                    .testTag("reel_speed_pill_${reel.id}")
            ) {
                Text(
                    text = if (playbackSpeed == 1.0f) "1x" else "${playbackSpeed}x",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // Manage / Remix Menu
            Box {
                IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier.testTag("reel_more_${reel.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Reel options",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Remix in Reels Studio") },
                        onClick = {
                            showMoreMenu = false
                            onRemixReel()
                        }
                    )
                    if (isOwnReel) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_caption)) },
                            onClick = {
                                showMoreMenu = false
                                onEditCaption()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Reel", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMoreMenu = false
                                onDeleteReel()
                            }
                        )
                    }
                }
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

        // Bottom-left Creator Info, Caption & Scrolling Audio
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 84.dp, bottom = 22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onOpenProfile)
            ) {
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
                    text = "${reel.audioTrack} • ${reel.niche} • Tap pause • Hold speed",
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
    reel: Reel,
    currentUsername: String,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
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
                            if (comment.author == currentUsername) {
                                IconButton(onClick = { onDeleteComment(comment.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete comment",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
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
