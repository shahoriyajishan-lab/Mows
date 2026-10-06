package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.CryptoPayloadDialog
import com.example.ui.components.SecurityShieldBadge
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceHigh
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.OutlineBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CipherWireViewModel
import com.example.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatDetailScreen(
    conversationId: String,
    viewModel: CipherWireViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.goBack()
    }

    val conversation by viewModel.repository.getConversation(conversationId).collectAsState(initial = null)
    val messages by viewModel.repository.getMessages(conversationId).collectAsState(initial = emptyList())
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val voiceDurationSec by viewModel.voiceDurationSec.collectAsState()
    val selectedDestructDuration by viewModel.selectedDestructDuration.collectAsState()
    val inspectingMessage by viewModel.inspectingMessage.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showTimerMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (inspectingMessage != null) {
        CryptoPayloadDialog(
            message = inspectingMessage!!,
            onDismiss = { viewModel.inspectMessageCrypto(null) }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBackground,
        topBar = {
            conversation?.let { conv ->
                ChatTopBar(
                    conversation = conv,
                    onBack = { viewModel.goBack() },
                    onVerifySafety = { viewModel.navigateTo(Screen.SafetyVerification(conversationId)) },
                    onAudioCall = { viewModel.startAudioCall(conv.peerName, conv.peerHandle, conv.peerAvatarRes) },
                    onVideoCall = { viewModel.startVideoCall(conv.peerName, conv.peerHandle, conv.peerAvatarRes) },
                    onScreenShare = { viewModel.startScreenShare(conv.peerName, conv.peerHandle) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // E2EE Channel Banner & Ephemeral indicator
            ChannelSecurityBanner(
                destructDuration = selectedDestructDuration,
                onSelectTimer = { showTimerMenu = true },
                onViewFingerprint = { viewModel.navigateTo(Screen.SafetyVerification(conversationId)) }
            )

            // Disappearing timer dropdown
            DropdownMenu(
                expanded = showTimerMenu,
                onDismissRequest = { showTimerMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Disappearing: OFF") },
                    onClick = {
                        viewModel.setDestructDuration(0L)
                        showTimerMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("10 seconds (Burner)") },
                    onClick = {
                        viewModel.setDestructDuration(10L)
                        showTimerMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("1 minute") },
                    onClick = {
                        viewModel.setDestructDuration(60L)
                        showTimerMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("1 hour") },
                    onClick = {
                        viewModel.setDestructDuration(3600L)
                        showTimerMenu = false
                    }
                )
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onInspectCrypto = { viewModel.inspectMessageCrypto(message) }
                    )
                }
            }

            // Composer bar
            MessageComposer(
                inputText = inputText,
                onTextChanged = { inputText = it },
                onSend = {
                    viewModel.sendTextMessage(conversationId, inputText)
                    inputText = ""
                },
                isRecording = isRecordingVoice,
                recordDurationSec = voiceDurationSec,
                onStartRecording = { viewModel.startVoiceRecording() },
                onStopAndSendRecording = { viewModel.sendVoiceMessage(conversationId) },
                onCancelRecording = { viewModel.cancelVoiceRecording() },
                destructActive = selectedDestructDuration > 0,
                onToggleTimer = { showTimerMenu = true }
            )
        }
    }
}

@Composable
private fun ChatTopBar(
    conversation: ConversationEntity,
    onBack: () -> Unit,
    onVerifySafety: () -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit,
    onScreenShare: () -> Unit
) {
    val context = LocalContext.current
    val avatarDrawableId = remember(conversation.peerAvatarRes) {
        if (conversation.peerAvatarRes.isNotEmpty()) {
            context.resources.getIdentifier(
                conversation.peerAvatarRes,
                "drawable",
                context.packageName
            )
        } else 0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberSurface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Box(modifier = Modifier.size(40.dp)) {
                if (avatarDrawableId != 0) {
                    Image(
                        painter = painterResource(id = avatarDrawableId),
                        contentDescription = conversation.peerName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(1.dp, CyanPrimary, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(CyberSurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.peerName.take(1),
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.clickable { onVerifySafety() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.peerName,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Verify Safety",
                        tint = if (conversation.isVerified) EmeraldAccent else Color(0xFFFFB800),
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = "${conversation.latencyMs}ms latency • Ratchet Sync",
                    color = EmeraldAccent,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Call Action Icons
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAudioCall, modifier = Modifier.testTag("call_audio_action")) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Audio Call",
                    tint = CyanPrimary
                )
            }
            IconButton(onClick = onVideoCall, modifier = Modifier.testTag("call_video_action")) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video Call",
                    tint = EmeraldAccent
                )
            }
            IconButton(onClick = onScreenShare, modifier = Modifier.testTag("share_screen_action")) {
                Icon(
                    imageVector = Icons.Default.ScreenShare,
                    contentDescription = "Share Screen",
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ChannelSecurityBanner(
    destructDuration: Long,
    onSelectTimer: () -> Unit,
    onViewFingerprint: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF090E1A))
            .border(1.dp, OutlineBorder)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onViewFingerprint() }
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "AES-256-GCM Direct Channel",
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (destructDuration > 0) CyberCrimson.copy(alpha = 0.2f) else Color.Transparent)
                .clickable { onSelectTimer() }
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Ephemeral Timer",
                tint = if (destructDuration > 0) CyberCrimson else TextSecondary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (destructDuration > 0) "${destructDuration}s Disappear" else "Timer: Off",
                color = if (destructDuration > 0) CyberCrimson else TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    onInspectCrypto: () -> Unit
) {
    val isMe = message.isFromMe
    val align = if (isMe) Alignment.End else Alignment.Start
    val bg = if (isMe) Color(0xFF0D323E) else CyberSurfaceVariant
    val borderCol = if (isMe) CyanPrimary.copy(alpha = 0.5f) else OutlineBorder

    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isMe) 14.dp else 2.dp,
                        bottomEnd = if (isMe) 2.dp else 14.dp
                    )
                )
                .background(bg)
                .border(
                    1.dp,
                    borderCol,
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isMe) 14.dp else 2.dp,
                        bottomEnd = if (isMe) 2.dp else 14.dp
                    )
                )
                .clickable { onInspectCrypto() }
                .padding(10.dp)
                .testTag("message_bubble_${message.id}")
        ) {
            Column {
                if (message.isSelfDestruct) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = "Self-destruct",
                            tint = CyberCrimson,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "EPHEMERAL ENCRYPTED",
                            color = CyberCrimson,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (message.isVoiceNote) {
                    // Voice Note Player bubble
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(220.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play voice note",
                                tint = CyberBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            AudioWaveformVisualizer(
                                isSpeaking = false,
                                barCount = 16,
                                modifier = Modifier.height(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Opus Voice • ${message.voiceDurationSec}s",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = CyanPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = timeFormatted,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "✓✓",
                            color = EmeraldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageComposer(
    inputText: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    isRecording: Boolean,
    recordDurationSec: Int,
    onStartRecording: () -> Unit,
    onStopAndSendRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    destructActive: Boolean,
    onToggleTimer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberSurface)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        if (isRecording) {
            // Live voice recording state
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E1220))
                    .border(1.dp, CyberCrimson, RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = CyberCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RECORDING ENCRYPTED OPUS: ${recordDurationSec}s",
                        color = CyberCrimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCancelRecording) {
                        Text(text = "CANCEL", color = TextSecondary, fontSize = 11.sp)
                    }
                    IconButton(
                        onClick = onStopAndSendRecording,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary)
                            .testTag("send_voice_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Send Voice Note",
                            tint = CyberBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            // Standard typing bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleTimer, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Toggle Disappearing Timer",
                        tint = if (destructActive) CyberCrimson else TextSecondary
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = onTextChanged,
                    placeholder = {
                        Text(
                            text = "End-to-end encrypted message...",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CyberBackground,
                        unfocusedContainerColor = CyberBackground,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = OutlineBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3
                )

                if (inputText.isNotBlank()) {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = CyberBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onStartRecording,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CyberSurfaceHigh)
                            .testTag("start_voice_recording_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record Voice Note",
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
