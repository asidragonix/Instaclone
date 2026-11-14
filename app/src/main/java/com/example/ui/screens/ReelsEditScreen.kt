package com.example.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.data.bot.AiBotReelsStore
import com.example.data.bot.BotReel
import com.example.ui.theme.InstaCoral

@Composable
fun ReelsEditScreen(
    initialReel: BotReel?,
    onBack: () -> Unit,
    onPublishReel: (
        videoUrl: String,
        posterUrl: String,
        caption: String,
        audioTrack: String,
        filterPreset: String,
        playbackSpeed: Float,
        trimStartPct: Float,
        trimEndPct: Float,
        overlayText: String
    ) -> Unit
) {
    BackHandler(onBack = onBack)

    var videoUrl by remember(initialReel) {
        mutableStateOf(initialReel?.videoUrl ?: AiBotReelsStore.MEDIA_CATALOG.first().videoUrl)
    }
    var posterUrl by remember(initialReel) {
        mutableStateOf(initialReel?.posterUrl ?: AiBotReelsStore.MEDIA_CATALOG.first().posterUrl)
    }
    var caption by remember(initialReel) {
        mutableStateOf(
            initialReel?.caption ?: "Edited in InstaClone Reels Studio #reels #creator"
        )
    }
    var audioTrack by remember(initialReel) {
        mutableStateOf(initialReel?.audioTrack ?: "Coastal Horizon • Sunset Beats Mix")
    }
    var filterPreset by remember(initialReel) {
        mutableStateOf(initialReel?.filterPreset ?: "Original")
    }
    var playbackSpeed by remember(initialReel) {
        mutableFloatStateOf(initialReel?.playbackSpeed ?: 1.0f)
    }
    var trimStart by remember(initialReel) {
        mutableFloatStateOf(initialReel?.trimStartPct ?: 0f)
    }
    var trimEnd by remember(initialReel) {
        mutableFloatStateOf(initialReel?.trimEndPct ?: 100f)
    }
    var overlayText by remember(initialReel) {
        mutableStateOf(initialReel?.overlayText ?: "")
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            videoUrl = uri.toString()
        }
    }

    val filterPresets = listOf("Original", "Cinema", "Warm Sunset", "Neon Cyber", "B&W")
    val speedOptions = listOf(0.5f, 1.0f, 1.5f, 2.0f)
    val audioPresets = listOf(
        "Coastal Horizon • Sunset Beats Mix",
        "Deep Blue Drift • Ambient Synth",
        "Sunlit Garden • Acoustic Lo-Fi",
        "Original Studio Audio"
    )

    val previewTint = when (filterPreset) {
        "Cinema" -> Color(0x280D1B2A)
        "Warm Sunset" -> Color(0x30FF7A45)
        "Neon Cyber" -> Color(0x30833AB4)
        "B&W" -> Color(0x45181818)
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("reels_studio_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
            Icon(
                imageVector = Icons.Filled.ContentCut,
                contentDescription = null,
                tint = InstaCoral,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.reels_studio_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 9:16 Live Preview Frame
            Box(
                modifier = Modifier
                    .width(210.dp)
                    .aspectRatio(9f / 16f)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            ) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = "Reel preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                mp.setVolume(0f, 0f)
                                start()
                            }
                            setOnErrorListener { _, _, _ -> true }
                            setVideoURI(Uri.parse(videoUrl))
                        }
                    },
                    update = { view ->
                        runCatching {
                            if (view.tag != videoUrl) {
                                view.tag = videoUrl
                                view.setVideoURI(Uri.parse(videoUrl))
                                view.start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (previewTint != Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(previewTint)
                    )
                }
                if (overlayText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.68f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = overlayText,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "$filterPreset • ${playbackSpeed}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    videoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pick Video from Device")
            }

            Text(
                text = "Or Choose Studio Clip",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(AiBotReelsStore.MEDIA_CATALOG, key = { it.niche }) { item ->
                    val selected = videoUrl == item.videoUrl
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                videoUrl = item.videoUrl
                                posterUrl = item.posterUrl
                            }
                    ) {
                        Text(
                            text = item.niche,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Text(
                text = "Visual Filter",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filterPresets) { preset ->
                    FilterChip(
                        selected = filterPreset == preset,
                        onClick = { filterPreset = preset },
                        label = { Text(preset) }
                    )
                }
            }

            Text(
                text = "Playback Speed (${playbackSpeed}x) & Trim (${trimStart.toInt()}% - ${trimEnd.toInt()}%)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                speedOptions.forEach { spd ->
                    FilterChip(
                        selected = playbackSpeed == spd,
                        onClick = { playbackSpeed = spd },
                        label = { Text("${spd}x") }
                    )
                }
            }
            RangeSlider(
                value = trimStart..trimEnd,
                onValueChange = { range ->
                    if (range.endInclusive - range.start >= 10f) {
                        trimStart = range.start
                        trimEnd = range.endInclusive
                    }
                },
                valueRange = 0f..100f
            )

            OutlinedTextField(
                value = overlayText,
                onValueChange = { overlayText = it.take(60) },
                label = { Text("On-Screen Text Overlay (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Audio Soundtrack",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(audioPresets) { track ->
                    FilterChip(
                        selected = audioTrack == track,
                        onClick = { audioTrack = track },
                        leadingIcon = {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(track) }
                    )
                }
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it.take(1000) },
                label = { Text("Reel Caption & Hashtags") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onPublishReel(
                        videoUrl,
                        posterUrl,
                        caption,
                        audioTrack,
                        filterPreset,
                        playbackSpeed,
                        trimStart,
                        trimEnd,
                        overlayText
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = InstaCoral),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_edited_reel_button")
            ) {
                Text(
                    text = stringResource(R.string.publish_reel),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
