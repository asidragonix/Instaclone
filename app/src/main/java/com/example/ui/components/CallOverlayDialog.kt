package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CallSession
import com.example.ui.theme.HeartLikeRed
import com.example.ui.theme.InstaCoral
import com.example.ui.theme.InstaViolet
import kotlinx.coroutines.delay

@Composable
fun CallOverlayDialog(
    callSession: CallSession,
    currentUserId: String,
    onAcceptCall: () -> Unit,
    onDeclineCall: () -> Unit,
    onMarkConnected: () -> Unit,
    onEndCall: (durationSeconds: Int) -> Unit
) {
    val context = LocalContext.current
    val isOutgoing = callSession.callerId == currentUserId
    val peerName = if (isOutgoing) callSession.calleeUsername else callSession.callerUsername
    val peerAvatar = if (isOutgoing) callSession.calleeAvatarData else callSession.callerAvatarData
    val isVideoCall = callSession.callType == "video"

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isMicMuted by remember { mutableStateOf(false) }
    var isCameraEnabled by remember { mutableStateOf(isVideoCall) }
    var isSpeakerOn by remember { mutableStateOf(isVideoCall) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var permissionWarning by remember { mutableStateOf<String?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val requiredPermissions = remember(isVideoCall) {
        if (isVideoCall) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micGranted = results[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val camGranted = !isVideoCall || results[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        hasAudioPermission = micGranted
        hasCameraPermission = camGranted

        if (!micGranted || !camGranted) {
            permissionWarning = if (isVideoCall && !camGranted && !micGranted) {
                "Mic & Camera permissions denied — tap Retry to enable live media."
            } else if (isVideoCall && !camGranted) {
                "Camera permission denied — continuing in audio mode."
            } else {
                "Microphone permission denied — microphone muted."
            }
            if (!micGranted) isMicMuted = true
            if (!camGranted) isCameraEnabled = false
        } else {
            permissionWarning = null
            isMicMuted = false
            if (isVideoCall) isCameraEnabled = true
        }
    }

    LaunchedEffect(callSession.id) {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    // Real AudioManager communication mode, speakerphone, and mic mute routing
    val audioManager = remember(context) {
        runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull()
    }
    DisposableEffect(callSession.id) {
        val prevMode = runCatching { audioManager?.mode }.getOrNull()
        val prevSpeaker = runCatching { audioManager?.isSpeakerphoneOn }.getOrNull()
        val prevMute = runCatching { audioManager?.isMicrophoneMute }.getOrNull()
        runCatching {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        }
        onDispose {
            runCatching {
                if (prevSpeaker != null) audioManager?.isSpeakerphoneOn = prevSpeaker
                if (prevMute != null) audioManager?.isMicrophoneMute = prevMute
                if (prevMode != null) audioManager?.mode = prevMode
            }
        }
    }

    LaunchedEffect(isSpeakerOn, isMicMuted) {
        runCatching {
            audioManager?.isSpeakerphoneOn = isSpeakerOn
            audioManager?.isMicrophoneMute = isMicMuted
        }
    }

    // Subtle Ringing / Ringback Tone while status == "ringing"
    LaunchedEffect(callSession.status, isOutgoing) {
        if (callSession.status == "ringing") {
            val toneGen = runCatching {
                ToneGenerator(AudioManager.STREAM_VOICE_CALL, 45)
            }.getOrNull()
            try {
                while (true) {
                    val tone = if (isOutgoing) ToneGenerator.TONE_SUP_RINGTONE else ToneGenerator.TONE_PROP_BEEP2
                    runCatching { toneGen?.startTone(tone, 650) }
                    delay(2200L)
                }
            } finally {
                runCatching { toneGen?.stopTone() }
                runCatching { toneGen?.release() }
            }
        }
    }

    // Active timer while connected
    LaunchedEffect(callSession.status) {
        if (callSession.status == "active") {
            while (true) {
                delay(1000L)
                elapsedSeconds += 1
            }
        }
    }

    // Pulsing halo animation for ringing state
    val infiniteTransition = rememberInfiniteTransition(label = "callRingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    BackHandler {
        if (!isOutgoing && callSession.status == "ringing") {
            onDeclineCall()
        } else {
            onEndCall(elapsedSeconds)
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF140D26), Color(0xFF1F1638), Color(0xFF0A0912))
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            // Live CameraX background preview during ringing if video & camera enabled, OR PiP during active
            if (isVideoCall && isCameraEnabled && hasCameraPermission && callSession.status == "ringing") {
                LiveCameraPreviewSurface(
                    isFrontCamera = isFrontCamera,
                    onCameraError = { cameraError = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .alpha(0.38f)
                )
            }

            // Top Call Type Badge, Timer Pill, and Permission Retry Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = if (isVideoCall) "InstaClone Video Call" else "InstaClone Voice Call",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    if (callSession.status == "active") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1DB954).copy(alpha = 0.25f)
                        ) {
                            val m = elapsedSeconds / 60
                            val s = elapsedSeconds % 60
                            Text(
                                text = String.format("%02d:%02d", m, s),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9DF0B8),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (permissionWarning != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = InstaCoral.copy(alpha = 0.28f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = permissionWarning.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = InstaCoral,
                                modifier = Modifier.clickable {
                                    permissionLauncher.launch(requiredPermissions)
                                }
                            ) {
                                Text(
                                    text = "Retry",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Picture-in-Picture Live CameraX Preview card when active video call
            if (isVideoCall && isCameraEnabled && hasCameraPermission && callSession.status == "active") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 56.dp, end = 4.dp)
                        .size(width = 112.dp, height = 164.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .background(Color.Black)
                        .testTag("call_local_camera_pip")
                ) {
                    LiveCameraPreviewSurface(
                        isFrontCamera = isFrontCamera,
                        onCameraError = { cameraError = it },
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = if (isFrontCamera) "Front" else "Rear",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Center Peer Identity & Call State
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (callSession.status == "ringing") {
                        Box(
                            modifier = Modifier
                                .size(124.dp)
                                .scale(pulseScale)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(InstaCoral)
                        )
                    }
                    StoryAvatarRing(
                        avatarData = peerAvatar,
                        username = peerName,
                        size = 112.dp,
                        showGradientRing = true
                    )
                }

                Text(
                    text = "@$peerName",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                val statusLabel = when (callSession.status) {
                    "active" -> {
                        val m = elapsedSeconds / 60
                        val s = elapsedSeconds % 60
                        "Connected • ${String.format("%02d:%02d", m, s)}"
                    }
                    "ringing" -> if (isOutgoing) "Ringing…" else "Incoming ${callSession.callType} call…"
                    else -> callSession.status.replaceFirstChar { it.uppercase() }
                }

                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )

                if (cameraError != null && isVideoCall && isCameraEnabled) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = if (isFrontCamera) "Front Camera Preview Mode" else "Rear Camera Preview Mode",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Single-device testing helper when waiting on peer answer
                if (isOutgoing && callSession.status == "ringing") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onMarkConnected,
                        colors = ButtonDefaults.buttonColors(containerColor = InstaViolet),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("simulate_answer_call_button")
                    ) {
                        Text("Connect Audio/Video Preview")
                    }
                }
            }

            // Bottom Call Controls
            if (!isOutgoing && callSession.status == "ringing") {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onDeclineCall,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(HeartLikeRed)
                                .testTag("decline_call_button")
                        ) {
                            Icon(Icons.Filled.CallEnd, contentDescription = "Decline call", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Decline", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onAcceptCall,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1DB954))
                                .testTag("accept_call_button")
                        ) {
                            Icon(
                                imageVector = if (isVideoCall) Icons.Filled.Videocam else Icons.Filled.Call,
                                contentDescription = "Accept call",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Accept", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp)
                    ) {
                        IconButton(
                            onClick = { isMicMuted = !isMicMuted },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isMicMuted) Color.White else Color.White.copy(alpha = 0.16f))
                                .testTag("call_mute_button")
                        ) {
                            Icon(
                                imageVector = if (isMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = if (isMicMuted) "Unmute microphone" else "Mute microphone",
                                tint = if (isMicMuted) Color.Black else Color.White
                            )
                        }

                        if (isVideoCall) {
                            IconButton(
                                onClick = { isCameraEnabled = !isCameraEnabled },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(if (!isCameraEnabled) Color.White else Color.White.copy(alpha = 0.16f))
                                    .testTag("call_camera_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isCameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                                    contentDescription = "Toggle camera",
                                    tint = if (!isCameraEnabled) Color.Black else Color.White
                                )
                            }

                            IconButton(
                                onClick = { isFrontCamera = !isFrontCamera },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .testTag("call_flip_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Cameraswitch,
                                    contentDescription = "Flip camera",
                                    tint = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = { isSpeakerOn = !isSpeakerOn },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.16f))
                                .testTag("call_speaker_button")
                        ) {
                            Icon(
                                imageVector = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                                contentDescription = "Toggle speaker",
                                tint = if (isSpeakerOn) Color.Black else Color.White
                            )
                        }

                        IconButton(
                            onClick = { onEndCall(elapsedSeconds) },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(HeartLikeRed)
                                .testTag("call_end_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CallEnd,
                                contentDescription = "End call",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCameraPreviewSurface(
    isFrontCamera: Boolean,
    onCameraError: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(isFrontCamera, lifecycleOwner) {
        runCatching {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener(
                {
                    runCatching {
                        val cameraProvider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val preferred = if (isFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        val selector = if (cameraProvider.hasCamera(preferred)) {
                            preferred
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                        onCameraError(null)
                    }.onFailure { err ->
                        onCameraError(err.localizedMessage ?: "Camera unavailable")
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }.onFailure { err ->
            onCameraError(err.localizedMessage ?: "Camera unavailable")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
