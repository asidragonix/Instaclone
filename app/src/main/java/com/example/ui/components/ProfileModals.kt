package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Post
import com.example.data.model.Story
import com.example.data.model.UserProfile
import com.example.util.ImageEncoder
import kotlinx.coroutines.launch

@Composable
fun EditProfileDialog(
    initialProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (
        username: String,
        displayName: String,
        bio: String,
        avatarData: String,
        category: String,
        pronouns: String,
        website: String,
        visibility: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf(initialProfile.username) }
    var displayName by remember { mutableStateOf(initialProfile.displayName) }
    var bio by remember { mutableStateOf(initialProfile.bio) }
    var avatarData by remember { mutableStateOf(initialProfile.avatarData) }
    var category by remember { mutableStateOf(initialProfile.category) }
    var pronouns by remember { mutableStateOf(initialProfile.pronouns) }
    var website by remember { mutableStateOf(initialProfile.website) }
    var visibility by remember { mutableStateOf(initialProfile.visibility) }

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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
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
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
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
                    value = category,
                    onValueChange = { category = it.take(50) },
                    label = { Text("Category (e.g. Photographer)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pronouns,
                    onValueChange = { pronouns = it.take(30) },
                    label = { Text("Pronouns (optional)") },
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
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it.take(200) },
                    label = { Text("Website URL (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = visibility == "public",
                        onClick = { visibility = "public" },
                        leadingIcon = { Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Public") }
                    )
                    FilterChip(
                        selected = visibility == "private",
                        onClick = { visibility = "private" },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Private") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(username, displayName, bio, avatarData, category, pronouns, website, visibility)
                },
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

@Composable
fun FollowConnectionsDialog(
    initialTab: Int,
    profileUsername: String,
    followers: List<UserProfile>,
    following: List<UserProfile>,
    currentUserId: String,
    followingIds: Set<String>,
    onToggleFollow: (String, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    var tab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var search by remember { mutableStateOf("") }

    val activeList = if (tab == 0) followers else following
    val filtered = remember(activeList, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) activeList
        else activeList.filter {
            it.username.lowercase().contains(q) || it.displayName.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("@$profileUsername") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Followers (${followers.size})") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Following (${following.size})") })
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search users…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tab == 0) "No followers yet" else "Not following anyone yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.userId }) { user ->
                            val isSelf = user.userId == currentUserId
                            val isFollow = user.userId in followingIds
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onOpenProfile(user.userId)
                                    }
                            ) {
                                StoryAvatarRing(
                                    avatarData = user.avatarData,
                                    username = user.username,
                                    size = 40.dp,
                                    showGradientRing = false
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.username,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = user.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                if (!isSelf) {
                                    if (isFollow) {
                                        OutlinedButton(onClick = { onToggleFollow(user.userId, true) }) {
                                            Text("Following")
                                        }
                                    } else {
                                        Button(onClick = { onToggleFollow(user.userId, false) }) {
                                            Text("Follow")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CreateHighlightDialog(
    ownStories: List<Story>,
    ownPosts: List<Post>,
    onDismiss: () -> Unit,
    onSave: (title: String, coverData: String, storyIds: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(ownStories.take(1).map { it.id }.toSet()) }

    val itemsToPick: List<Pair<String, String>> = remember(ownStories, ownPosts) {
        val storyItems = ownStories.map {
            it.id to (it.mediaData.ifBlank { ImageEncoder.studioPresets.first().uriValue })
        }
        val postItems = ownPosts.map { it.id to it.imageData }
        (storyItems + postItems).distinctBy { it.first }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Story Highlight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(32) },
                    label = { Text("Highlight Name (e.g. Travel, Work)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Select cover & moments (${selectedIds.size} selected)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (itemsToPick.isEmpty()) {
                    Text(
                        text = "Share a story or post first to add it into a highlight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                    ) {
                        items(itemsToPick, key = { it.first }) { (id, media) ->
                            val isSelected = id in selectedIds
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedIds = if (isSelected) selectedIds - id else selectedIds + id
                                    }
                            ) {
                                SocialMediaVisual(
                                    imageData = media,
                                    contentDescription = "Highlight item",
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cover = itemsToPick.firstOrNull { it.first in selectedIds }?.second
                        ?: itemsToPick.firstOrNull()?.second
                        ?: ImageEncoder.studioPresets.first().uriValue
                    onSave(title.ifBlank { "Highlights" }, cover, selectedIds.toList())
                },
                enabled = title.isNotBlank() || selectedIds.isNotEmpty()
            ) {
                Text("Save Highlight")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun EditCaptionDialog(
    initialCaption: String,
    initialLocation: String = "",
    showLocation: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (caption: String, location: String) -> Unit
) {
    var caption by remember { mutableStateOf(initialCaption) }
    var location by remember { mutableStateOf(initialLocation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_caption)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(2200) },
                    label = { Text("Caption") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showLocation) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it.take(80) },
                        label = { Text("Location") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(caption, location) }) {
                Text(stringResource(R.string.save_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
