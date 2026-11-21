package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Story
import com.example.data.model.StoryComment
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.theme.HeartLikeRed
import com.example.ui.theme.InstaCoral
import com.example.ui.theme.StoryRingGradient
import com.example.util.ImageEncoder
import com.example.util.TimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

data class StoryAuthorGroup(
    val authorId: String,
    val authorUsername: String,
    val authorAvatarData: String,
    val stories: List<Story>,
    val hasUnseen: Boolean
)

val storyGradientPresets: List<Pair<String, List<Color>>> = listOf(
    "sunset" to listOf(Color(0xFF2B1055), Color(0xFFC13584), Color(0xFFFF7A45)),
    "cyber" to listOf(Color(0xFF07071A), Color(0xFF3A0CA3), Color(0xFF4CC9F0)),
    "ocean" to listOf(Color(0xFF082032), Color(0xFF11698E), Color(0xFF16C79A)),
    "emerald" to listOf(Color(0xFF0B3D2E), Color(0xFF1B8A5A), Color(0xFF7AE582)),
    "coral" to listOf(Color(0xFF6B0F1A), Color(0xFFB91372), Color(0xFFFF6B6B)),
    "midnight" to listOf(Color(0xFF141E30), Color(0xFF243B55), Color(0xFF4B6CB7))
)

fun resolveStoryBrush(gradientKey: String): Brush {
    val colors = storyGradientPresets.firstOrNull { it.first == gradientKey }?.second
        ?: storyGradientPresets.first().second
    return Brush.linearGradient(colors)
}

@Composable
fun StoriesTrayRow(
    currentUserProfile: UserProfile?,
    currentUserId: String,
    stories: List<Story>,
    viewedStoryIds: Set<String>,
    onCreateStory: () -> Unit,
    onOpenAuthorStories: (List<Story>) -> Unit
) {
    val ownStories = remember(stories, currentUserId) {
        stories.filter { it.authorId == currentUserId }
    }
    val friendGroups = remember(stories, currentUserId, viewedStoryIds) {
        stories
            .filter { it.authorId != currentUserId }
            .groupBy { it.authorId }
            .map { (uid, list) ->
                val first = list.first()
                StoryAuthorGroup(
                    authorId = uid,
                    authorUsername = first.authorUsername,
                    authorAvatarData = first.authorAvatarData,
                    stories = list,
                    hasUnseen = list.any { it.id !in viewedStoryIds }
                )
            }
            .sortedByDescending { it.hasUnseen }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Always show Your Story first with + button
            item(key = "your_story_tray") {
                val ownHasUnseen = ownStories.any { it.id !in viewedStoryIds }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clickable {
                            if (ownStories.isNotEmpty()) {
                                onOpenAuthorStories(ownStories)
                            } else {
                                onCreateStory()
                            }
                        }
                        .testTag("your_story_tray_item")
                ) {
                    Box(modifier = Modifier.size(66.dp)) {
                        StoryAvatarRing(
                            avatarData = currentUserProfile?.avatarData.orEmpty(),
                            username = currentUserProfile?.username ?: "you",
                            size = 64.dp,
                            showGradientRing = ownStories.isNotEmpty() && ownHasUnseen
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(InstaCoral)
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                .clickable { onCreateStory() }
                                .testTag("add_story_plus_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.create_story),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = stringResource(R.string.your_story),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. Friends' Story Groups
            items(friendGroups, key = { it.authorId }) { group ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clickable { onOpenAuthorStories(group.stories) }
                        .testTag("story_tray_${group.authorId}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .then(
                                if (group.hasUnseen) {
                                    Modifier.border(
                                        2.5.dp,
                                        Brush.sweepGradient(StoryRingGradient + StoryRingGradient.first()),
                                        CircleShape
                                    )
                                } else {
                                    Modifier.border(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    )
                                }
                            )
                            .padding(3.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        StoryAvatarRing(
                            avatarData = group.authorAvatarData,
                            username = group.authorUsername,
                            size = 56.dp,
                            showGradientRing = false
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = group.authorUsername,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (group.hasUnseen) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun CreateStoryDialog(
    isPublishing: Boolean,
    onDismiss: () -> Unit,
    onPublishStory: (
        mediaData: String,
        mediaType: String,
        bgGradient: String,
        overlayText: String,
        sticker: String,
        caption: String
    ) -> Unit
) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf("image") } // "image" | "text"
    var mediaData by remember { mutableStateOf(ImageEncoder.studioPresets.first().uriValue) }
    var bgGradient by remember { mutableStateOf("sunset") }
    var overlayText by remember { mutableStateOf("") }
    var sticker by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var isEncoding by remember { mutableStateOf(false) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            if (mime.startsWith("video/")) {
                mode = "video"
                mediaData = uri.toString()
            } else {
                isEncoding = true
                scope.launch {
                    ImageEncoder.encodeUriToDataUrl(context, uri, maxDimension = 720, jpegQuality = 75)
                        .onSuccess { encoded ->
                            mode = "image"
                            mediaData = encoded
                        }
                    isEncoding = false
                }
            }
        }
    }

    val stickerOptions = listOf("", "✨", "🔥", "📍 Today", "🎵 Vibes", "❤️Mood")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                    }
                    Text(
                        text = stringResource(R.string.create_story),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            onPublishStory(
                                if (mode == "text") "" else mediaData,
                                mode,
                                bgGradient,
                                overlayText,
                                sticker,
                                caption
                            )
                        },
                        enabled = !isPublishing && !isEncoding && (mode != "text" || overlayText.isNotBlank() || caption.isNotBlank()),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("publish_story_button")
                    ) {
                        Text(if (isPublishing) "Sharing…" else "Share Story")
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = mode != "text",
                            onClick = { if (mode == "text") mode = "image" },
                            leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Photo / Video") }
                        )
                        FilterChip(
                            selected = mode == "text",
                            onClick = { mode = "text" },
                            leadingIcon = { Icon(Icons.Filled.TextFields, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Gradient Card") }
                        )
                    }

                    // 9:16 Live Story Preview
                    Box(
                        modifier = Modifier
                            .width(210.dp)
                            .aspectRatio(9f / 16f)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(22.dp))
                            .background(resolveStoryBrush(bgGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mode == "image" && mediaData.isNotBlank()) {
                            SocialMediaVisual(
                                imageData = mediaData,
                                contentDescription = "Story preview",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (mode == "video" && mediaData.isNotBlank()) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            mp.setVolume(0f, 0f)
                                            start()
                                        }
                                        setVideoURI(Uri.parse(mediaData))
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (isEncoding) {
                            CircularProgressIndicator(color = Color.White)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            if (sticker.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.9f)
                                ) {
                                    Text(
                                        text = sticker,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            if (overlayText.isNotBlank()) {
                                Text(
                                    text = overlayText,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "24h",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (mode != "text") {
                        OutlinedButton(
                            onClick = {
                                mediaPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose Photo or Video from Device")
                        }
                    } else {
                        Text(
                            text = "Background Gradient",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(storyGradientPresets, key = { it.first }) { (key, colors) ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(colors))
                                        .border(
                                            width = if (bgGradient == key) 3.dp else 1.dp,
                                            color = if (bgGradient == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                        .clickable { bgGradient = key }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = overlayText,
                        onValueChange = { overlayText = it.take(120) },
                        label = { Text(if (mode == "text") "Story Card Message" else "On-Screen Text Overlay") },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("story_overlay_input")
                    )

                    Text(
                        text = "Sticker Badge",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(stickerOptions) { st ->
                            FilterChip(
                                selected = sticker == st,
                                onClick = { sticker = st },
                                label = { Text(if (st.isBlank()) "None" else st) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it.take(300) },
                        label = { Text("Caption (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenStoryViewerDialog(
    stories: List<Story>,
    currentUserId: String,
    likedStoryIds: Set<String>,
    observeComments: (String) -> Flow<UiState<List<StoryComment>>>,
    onMarkViewed: (Story) -> Unit,
    onToggleLike: (Story) -> Unit,
    onSendReply: (Story, String, Boolean) -> Unit,
    onShareStory: (Story) -> Unit,
    onDeleteStory: (String) -> Unit,
    onAddAnotherStory: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (stories.isEmpty()) return
    BackHandler(onBack = onDismiss)

    var currentIndex by remember(stories.first().authorId) { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, stories.lastIndex)
    val story = stories[safeIndex]

    var isPaused by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var progress by remember(story.id) { mutableFloatStateOf(0f) }
    var quickReplyDraft by remember(story.id) { mutableStateOf("") }
    var showCommentsDrawer by remember { mutableStateOf(false) }

    LaunchedEffect(story.id) {
        onMarkViewed(story)
        progress = 0f
        while (progress < 1f) {
            delay(50L)
            if (!isPaused && !showCommentsDrawer) {
                progress += 0.01f // ~5 seconds per story
            }
        }
        if (safeIndex < stories.lastIndex) {
            currentIndex = safeIndex + 1
        } else {
            onDismiss()
        }
    }

    if (showCommentsDrawer) {
        val commentsFlow = remember(story.id) { observeComments(story.id) }
        val commentsState by commentsFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
        ModalBottomSheet(
            onDismissRequest = { showCommentsDrawer = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Story Replies (${story.commentsCount})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 300.dp)
                        .padding(vertical = 12.dp)
                ) {
                    when (val state = commentsState) {
                        is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                        is UiState.Success -> {
                            if (state.data.isEmpty()) {
                                Text(
                                    text = "No replies yet. Be the first!",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(state.data, key = { it.id }) { c ->
                                        Row(verticalAlignment = Alignment.Top) {
                                            StoryAvatarRing(
                                                avatarData = c.authorAvatarData,
                                                username = c.authorUsername,
                                                size = 32.dp,
                                                showGradientRing = false
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(c.authorUsername, style = MaterialTheme.typography.labelLarge)
                                                Text(c.text, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Story Background / Media Canvas + Tap / Hold Gestures
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(resolveStoryBrush(story.bgGradient))
                    .pointerInput(story.id, safeIndex, stories.size) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { offset ->
                                val width = size.width.toFloat()
                                if (offset.x < width * 0.35f) {
                                    if (safeIndex > 0) currentIndex = safeIndex - 1 else progress = 0f
                                } else {
                                    if (safeIndex < stories.lastIndex) currentIndex = safeIndex + 1 else onDismiss()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (story.mediaType == "video" && story.mediaData.isNotBlank()) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    val vol = if (isMuted) 0f else 1f
                                    mp.setVolume(vol, vol)
                                    start()
                                }
                                setVideoURI(Uri.parse(story.mediaData))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (story.mediaType == "image" && story.mediaData.isNotBlank()) {
                    SocialMediaVisual(
                        imageData = story.mediaData,
                        contentDescription = story.caption.ifBlank { story.authorUsername },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Scrim for top/bottom controls legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.55f),
                                0.25f to Color.Transparent,
                                0.72f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.78f)
                            )
                        )
                )

                // Sticker + Overlay Text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 28.dp)
                ) {
                    if (story.sticker.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.92f)
                        ) {
                            Text(
                                text = story.sticker,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (story.overlayText.isNotBlank()) {
                        Text(
                            text = story.overlayText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Top Progress Segments + Author Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    stories.forEachIndexed { idx, _ ->
                        val segProgress = when {
                            idx < safeIndex -> 1f
                            idx == safeIndex -> progress
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segProgress },
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onDismiss()
                                onOpenProfile(story.authorId)
                            }
                    ) {
                        StoryAvatarRing(
                            avatarData = story.authorAvatarData,
                            username = story.authorUsername,
                            size = 38.dp,
                            showGradientRing = false
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = story.authorUsername,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = TimeFormatter.formatRelative(story.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    if (story.mediaType == "video") {
                        IconButton(onClick = { isMuted = !isMuted }) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Toggle mute",
                                tint = Color.White
                            )
                        }
                    }

                    if (story.authorId == currentUserId) {
                        IconButton(onClick = onAddAnotherStory) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_story), tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                onDeleteStory(story.id)
                                if (stories.size <= 1) onDismiss()
                            }
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete story", tint = Color.White)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close story", tint = Color.White)
                    }
                }
            }

            // Bottom Caption + Interaction Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (story.caption.isNotBlank()) {
                    Text(
                        text = story.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }

                if (story.authorId == currentUserId) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${story.viewsCount} views • ${story.likesCount} likes",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = quickReplyDraft,
                        onValueChange = { quickReplyDraft = it.take(400) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.story_reply_hint),
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )

                    if (quickReplyDraft.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val text = quickReplyDraft.trim()
                                if (text.isNotEmpty()) {
                                    onSendReply(story, text, story.authorId != currentUserId)
                                    quickReplyDraft = ""
                                }
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send reply",
                                tint = InstaCoral
                            )
                        }
                    }

                    val isLiked = story.id in likedStoryIds
                    IconButton(onClick = { onToggleLike(story) }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like story",
                            tint = if (isLiked) HeartLikeRed else Color.White
                        )
                    }

                    IconButton(onClick = { showCommentsDrawer = true }) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "View story replies",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { onShareStory(story) }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = stringResource(R.string.cd_share_content),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
