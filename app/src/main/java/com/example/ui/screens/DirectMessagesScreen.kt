package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.StoryAvatarRing
import com.example.util.TimeFormatter
import kotlinx.coroutines.flow.Flow

@Composable
fun DirectMessagesScreen(
    currentUserId: String,
    conversationsState: UiState<List<Conversation>>,
    creatorsState: UiState<List<UserProfile>>,
    selectedPeer: UserProfile?,
    onSelectPeer: (UserProfile?) -> Unit,
    buildConversationId: (String) -> String,
    observeMessages: (String) -> Flow<UiState<List<DirectMessage>>>,
    onSendMessage: (UserProfile, String) -> Unit,
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
                    activePeerId = selectedPeer?.userId,
                    onSelectPeer = { onSelectPeer(it) },
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
                        peer = selectedPeer,
                        messagesFlow = messagesFlow,
                        showBackButton = false,
                        onBack = { onSelectPeer(null) },
                        onOpenProfile = { onOpenProfile(selectedPeer.userId) },
                        onSend = { text -> onSendMessage(selectedPeer, text) },
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
                    peer = selectedPeer,
                    messagesFlow = messagesFlow,
                    showBackButton = true,
                    onBack = { onSelectPeer(null) },
                    onOpenProfile = { onOpenProfile(selectedPeer.userId) },
                    onSend = { text -> onSendMessage(selectedPeer, text) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ConversationInboxPane(
                    currentUserId = currentUserId,
                    conversationsState = conversationsState,
                    creatorsState = creatorsState,
                    activePeerId = null,
                    onSelectPeer = { onSelectPeer(it) },
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
    activePeerId: String?,
    onSelectPeer: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val otherCreators = (creatorsState as? UiState.Success)?.data
        .orEmpty()
        .filter { it.userId != currentUserId }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.dm_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
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
                if (conversationsState.data.isEmpty()) {
                    EmptyChatPlaceholder(modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(conversationsState.data, key = { it.id }) { conv ->
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
    peer: UserProfile,
    messagesFlow: Flow<UiState<List<DirectMessage>>>,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val messagesState by messagesFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    var messageDraft by remember(peer.userId) { mutableStateOf("") }
    val listState = rememberLazyListState()

    val messagesList = (messagesState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(messagesList.size) {
        if (messagesList.isNotEmpty()) {
            listState.animateScrollToItem(messagesList.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // Chat Header
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
                Text(
                    text = peer.username,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Messages Area
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
                    // When brand-new conversation has not been created yet, show friendly prompt
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
                            items(state.data, key = { it.id }) { msg ->
                                val isMine = msg.senderId == currentUserId
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp,
                                            bottomStart = if (isMine) 18.dp else 4.dp,
                                            bottomEnd = if (isMine) 4.dp else 18.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isMine) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isMine) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = TimeFormatter.formatRelative(msg.createdAt),
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

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Message Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            IconButton(
                onClick = {
                    val trimmed = messageDraft.trim()
                    if (trimmed.isNotEmpty()) {
                        onSend(trimmed)
                        messageDraft = ""
                    }
                },
                enabled = messageDraft.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (messageDraft.isNotBlank()) {
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
                    tint = if (messageDraft.isNotBlank()) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
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
