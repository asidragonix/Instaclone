package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.FilterChip
import com.example.R
import com.example.data.model.CallSession
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.PendingAttachment
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.ChatMessageBubbleItem
import com.example.ui.components.MediaLightboxDialog
import com.example.ui.components.MessageActionsDialog
import com.example.ui.components.PendingAttachmentPreviewBar
import com.example.ui.components.StoryAvatarRing
import com.example.ui.components.VideoAttachmentDialog
import com.example.ui.components.openAttachmentFileIntent
import com.example.util.MediaAttachmentHelper
import com.example.util.TimeFormatter
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DirectMessagesScreen(
    currentUserId: String,
    conversationsState: UiState<List<Conversation>>,
    creatorsState: UiState<List<UserProfile>>,
    callSessions: List<CallSession>,
    selectedPeer: UserProfile?,
    onSelectPeer: (UserProfile?) -> Unit,
    buildConversationId: (String) -> String,
    observeMessages: (String) -> Flow<UiState<List<DirectMessage>>>,
    onSendMessage: (UserProfile, String, PendingAttachment?, DirectMessage?) -> Unit,
    onToggleReaction: (String, DirectMessage, String) -> Unit,
    onDeleteMessage: (String, String) -> Unit,
    onStartCall: (UserProfile, String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isExpandedPane = maxWidth >= 700.dp

        if (isExpandedPane) {
            Row(modifier = Modifier.fillMaxSize()) {
                ConversationInboxPane(
                    currentUserId = currentUserId,
                    conversationsState = conversationsState,
                    creatorsState = creatorsState,
                    callSessions = callSessions,
                    activePeerId = selectedPeer?.userId,
                    onSelectPeer = { onSelectPeer(it) },
                    onStartCall = onStartCall,
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (selectedPeer != null) {
                    val convId = remember(selectedPeer.userId) {
                        buildConversationId(selectedPeer.userId)
                    }
                    val messagesFlow = remember(convId) { observeMessages(convId) }
                    ChatThreadPane(
                        currentUserId = currentUserId,
                        conversationId = convId,
                        peer = selectedPeer,
                        messagesFlow = messagesFlow,
                        showBackButton = false,
                        onBack = { onSelectPeer(null) },
                        onOpenProfile = { onOpenProfile(selectedPeer.userId) },
                        onStartCall = { callType -> onStartCall(selectedPeer, callType) },
                        onSend = { text, att, reply -> onSendMessage(selectedPeer, text, att, reply) },
                        onToggleReaction = { msg, emoji -> onToggleReaction(convId, msg, emoji) },
                        onDeleteMessage = { msgId -> onDeleteMessage(convId, msgId) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                } else {
                    EmptyChatPlaceholder(modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        } else {
            if (selectedPeer != null) {
                BackHandler { onSelectPeer(null) }
                val convId = remember(selectedPeer.userId) {
                    buildConversationId(selectedPeer.userId)
                }
                val messagesFlow = remember(convId) { observeMessages(convId) }
                ChatThreadPane(
                    currentUserId = currentUserId,
                    conversationId = convId,
                    peer = selectedPeer,
                    messagesFlow = messagesFlow,
                    showBackButton = true,
                    onBack = { onSelectPeer(null) },
                    onOpenProfile = { onOpenProfile(selectedPeer.userId) },
                    onStartCall = { callType -> onStartCall(selectedPeer, callType) },
                    onSend = { text, att, reply -> onSendMessage(selectedPeer, text, att, reply) },
                    onToggleReaction = { msg, emoji -> onToggleReaction(convId, msg, emoji) },
                    onDeleteMessage = { msgId -> onDeleteMessage(convId, msgId) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ConversationInboxPane(
                    currentUserId = currentUserId,
                    conversationsState = conversationsState,
                    creatorsState = creatorsState,
                    callSessions = callSessions,
                    activePeerId = null,
                    onSelectPeer = { onSelectPeer(it) },
                    onStartCall = onStartCall,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ConversationInboxPane(
    currentUserId: String,
    conversationsState: UiState<List<Conversation>>,
    creatorsState: UiState<List<UserProfile>>,
    callSessions: List<CallSession>,
    activePeerId: String?,
    onSelectPeer: (UserProfile) -> Unit,
    onStartCall: (UserProfile, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCallsTab by remember { mutableStateOf(false) }
    val q = searchQuery.trim().lowercase()

    val otherCreators = (creatorsState as? UiState.Success)?.data
        .orEmpty()
        .filter { it.userId != currentUserId }
        .filter {
            q.isEmpty() || it.username.lowercase().contains(q) || it.displayName.lowercase().contains(q)
        }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.dm_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showCallsTab,
                    onClick = { showCallsTab = false },
                    label = { Text("Chats") }
                )
                FilterChip(
                    selected = showCallsTab,
                    onClick = { showCallsTab = true },
                    label = { Text("Calls (${callSessions.size})") },
                    modifier = Modifier.testTag("inbox_calls_tab")
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search messages or friends…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.cancel))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp)
                .testTag("dm_search_input")
        )

        if (otherCreators.isNotEmpty()) {
            Text(
                text = stringResource(R.string.dm_start_chat),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(otherCreators, key = { it.userId }) { user ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(66.dp)
                            .clickable { onSelectPeer(user) }
                            .testTag("start_dm_user_${user.userId}")
                    ) {
                        StoryAvatarRing(
                            avatarData = user.avatarData,
                            username = user.username,
                            size = 54.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user.username,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        if (showCallsTab) {
            val filteredCalls = remember(callSessions, q) {
                if (q.isEmpty()) callSessions
                else callSessions.filter {
                    val peerName = if (it.callerId == currentUserId) it.calleeUsername else it.callerUsername
                    peerName.lowercase().contains(q)
                }
            }
            if (filteredCalls.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No recent calls yet. Tap a friend above or open any chat to start a Voice or Video call.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredCalls, key = { it.id }) { call ->
                        val isOutgoing = call.callerId == currentUserId
                        val peerId = if (isOutgoing) call.calleeId else call.callerId
                        val peerName = if (isOutgoing) call.calleeUsername else call.callerUsername
                        val peerAvatar = if (isOutgoing) call.calleeAvatarData else call.callerAvatarData
                        val peerProfile = UserProfile(
                            id = peerId,
                            userId = peerId,
                            username = peerName,
                            displayName = peerName,
                            avatarData = peerAvatar
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPeer(peerProfile) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            StoryAvatarRing(
                                avatarData = peerAvatar,
                                username = peerName,
                                size = 46.dp,
                                showGradientRing = false
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "@$peerName",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${if (isOutgoing) "Outgoing" else "Incoming"} ${call.callType} • ${call.status} • ${TimeFormatter.formatRelative(call.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onStartCall(peerProfile, "voice") }) {
                                Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.cd_voice_call))
                            }
                            IconButton(onClick = { onStartCall(peerProfile, "video") }) {
                                Icon(Icons.Filled.Videocam, contentDescription = stringResource(R.string.cd_video_call))
                            }
                        }
                    }
                }
            }
            return@Column
        }

        when (conversationsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = conversationsState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            is UiState.Success -> {
                val filteredConversations = remember(conversationsState.data, q) {
                    if (q.isEmpty()) conversationsState.data
                    else conversationsState.data.filter {
                        it.peerUsername(currentUserId).lowercase().contains(q) ||
                            it.lastMessage.lowercase().contains(q)
                    }
                }

                if (filteredConversations.isEmpty()) {
                    EmptyChatPlaceholder(modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filteredConversations, key = { it.id }) { conv ->
                            val peerId = conv.peerUserId(currentUserId)
                            val peerName = conv.peerUsername(currentUserId)
                            val peerAvatar = conv.peerAvatarData(currentUserId)
                            val isSelected = activePeerId == peerId

                            Surface(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.background
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectPeer(
                                            UserProfile(
                                                id = peerId,
                                                userId = peerId,
                                                username = peerName,
                                                displayName = peerName,
                                                avatarData = peerAvatar
                                            )
                                        )
                                    }
                                    .testTag("conversation_row_${conv.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StoryAvatarRing(
                                        avatarData = peerAvatar,
                                        username = peerName,
                                        size = 48.dp,
                                        showGradientRing = false
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = peerName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = conv.lastMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = TimeFormatter.formatRelative(conv.updatedAt ?: conv.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun ChatThreadPane(
    currentUserId: String,
    conversationId: String,
    peer: UserProfile,
    messagesFlow: Flow<UiState<List<DirectMessage>>>,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onStartCall: (callType: String) -> Unit,
    onSend: (String, PendingAttachment?, DirectMessage?) -> Unit,
    onToggleReaction: (DirectMessage, String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messagesState by messagesFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)

    var messageDraft by remember(peer.userId) { mutableStateOf("") }
    var pendingAttachment by remember(peer.userId) { mutableStateOf<PendingAttachment?>(null) }
    var replyTarget by remember(peer.userId) { mutableStateOf<DirectMessage?>(null) }
    var isProcessingAttachment by remember { mutableStateOf(false) }
    var attachmentError by remember { mutableStateOf<String?>(null) }
    var showAttachMenu by remember { mutableStateOf(false) }

    var lightboxImageData by remember { mutableStateOf<String?>(null) }
    var videoPlayerUri by remember { mutableStateOf<String?>(null) }
    var actionTargetMessage by remember { mutableStateOf<DirectMessage?>(null) }

    val listState = rememberLazyListState()
    val messagesList = (messagesState as? UiState.Success)?.data.orEmpty()

    LaunchedEffect(messagesList.size) {
        if (messagesList.isNotEmpty()) {
            listState.animateScrollToItem(messagesList.lastIndex)
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingAttachment = true
            attachmentError = null
            scope.launch {
                MediaAttachmentHelper.readAttachmentFromUri(context, uri)
                    .onSuccess { pendingAttachment = it }
                    .onFailure { attachmentError = it.localizedMessage ?: "Failed to attach media" }
                isProcessingAttachment = false
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isProcessingAttachment = true
            attachmentError = null
            scope.launch {
                MediaAttachmentHelper.readAttachmentFromUri(context, uri, forcedType = "file")
                    .onSuccess { pendingAttachment = it }
                    .onFailure { attachmentError = it.localizedMessage ?: "Failed to attach file" }
                isProcessingAttachment = false
            }
        }
    }

    if (lightboxImageData != null) {
        MediaLightboxDialog(
            imageData = lightboxImageData!!,
            onDismiss = { lightboxImageData = null }
        )
    }

    if (videoPlayerUri != null) {
        VideoAttachmentDialog(
            videoUriString = videoPlayerUri!!,
            onDismiss = { videoPlayerUri = null }
        )
    }

    val actMsg = actionTargetMessage
    if (actMsg != null) {
        MessageActionsDialog(
            message = actMsg,
            isMine = actMsg.senderId == currentUserId,
            onReact = { emoji -> onToggleReaction(actMsg, emoji) },
            onReply = { replyTarget = actMsg },
            onDelete = { onDeleteMessage(actMsg.id) },
            onDismiss = { actionTargetMessage = null }
        )
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // Chat Header with Profile Link + Voice & Video Calling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenProfile)
            ) {
                StoryAvatarRing(
                    avatarData = peer.avatarData,
                    username = peer.username,
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = peer.username,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tap profile • End-to-end synced",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = { onStartCall("voice") },
                modifier = Modifier.testTag("chat_voice_call_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = stringResource(R.string.cd_voice_call),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = { onStartCall("video") },
                modifier = Modifier.testTag("chat_video_call_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = stringResource(R.string.cd_video_call),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Messages List Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (val state = messagesState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Error -> {
                    Text(
                        text = "Say hello to @${peer.username}!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Text(
                            text = "Say hello to @${peer.username}!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(state.data, key = { _, m -> m.id }) { idx, msg ->
                                val prevDayKey = state.data.getOrNull(idx - 1)?.createdAt?.let(::formatDateDivider)
                                val curDayKey = formatDateDivider(msg.createdAt)
                                if (curDayKey != null && curDayKey != prevDayKey) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                        ) {
                                            Text(
                                                text = curDayKey,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                val isMine = msg.senderId == currentUserId
                                ChatMessageBubbleItem(
                                    message = msg,
                                    isMine = isMine,
                                    onImageClick = { lightboxImageData = it },
                                    onVideoClick = { videoPlayerUri = it },
                                    onOpenFile = { openAttachmentFileIntent(context, it) },
                                    onCallBack = { callType -> onStartCall(callType) },
                                    onReply = { replyTarget = msg },
                                    onReact = { emoji -> onToggleReaction(msg, emoji) },
                                    onLongPressActions = { actionTargetMessage = msg }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reply Banner
        val activeReply = replyTarget
        if (activeReply != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Replying to @${activeReply.senderUsername}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeReply.text.ifBlank { activeReply.attachmentName.ifBlank { "Attachment" } },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { replyTarget = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel reply")
                    }
                }
            }
        }

        if (isProcessingAttachment) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preparing attachment…", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (attachmentError != null) {
            Text(
                text = attachmentError.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        val currentPending = pendingAttachment
        if (currentPending != null) {
            PendingAttachmentPreviewBar(
                attachment = currentPending,
                onClear = { pendingAttachment = null }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Message Composer with Attachment Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(
                    onClick = { showAttachMenu = true },
                    modifier = Modifier.testTag("chat_attach_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = stringResource(R.string.cd_attach_media),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showAttachMenu,
                    onDismissRequest = { showAttachMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Photo or Video") },
                        onClick = {
                            showAttachMenu = false
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Document or File") },
                        onClick = {
                            showAttachMenu = false
                            documentPickerLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }
            }

            OutlinedTextField(
                value = messageDraft,
                onValueChange = { messageDraft = it.take(1000) },
                placeholder = { Text(stringResource(R.string.dm_input_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )
            Spacer(modifier = Modifier.width(8.dp))

            val canSend = (messageDraft.isNotBlank() || pendingAttachment != null) && !isProcessingAttachment
            IconButton(
                onClick = {
                    if (canSend) {
                        onSend(messageDraft.trim(), pendingAttachment, replyTarget)
                        messageDraft = ""
                        pendingAttachment = null
                        replyTarget = null
                    }
                },
                enabled = canSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .testTag("send_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.cd_send_message),
                    tint = if (canSend) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

private fun formatDateDivider(ts: Timestamp?): String? {
    val date = ts?.toDate() ?: return null
    val calMsg = Calendar.getInstance().apply { time = date }
    val calNow = Calendar.getInstance()
    return when {
        calMsg.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
            calMsg.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR) -> "Today"
        calMsg.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
            calMsg.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR) - 1 -> "Yesterday"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
    }
}

@Composable
private fun EmptyChatPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Forum,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.dm_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.dm_empty_inbox),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
