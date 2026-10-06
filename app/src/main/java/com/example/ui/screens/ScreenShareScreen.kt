package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LatencyHud
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.OutlineBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSecurity
import com.example.viewmodel.CipherWireViewModel

@Composable
fun ScreenShareScreen(
    peerName: String,
    peerHandle: String,
    viewModel: CipherWireViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.endCall()
    }

    val callState by viewModel.activeCall.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()

    var isPaused by remember { mutableStateOf(false) }
    var selectedFps by remember { mutableStateOf(60) }
    var isLaserActive by remember { mutableStateOf(true) }
    var laserPos by remember { mutableStateOf(Offset(320f, 420f)) }

    val infiniteTransition = rememberInfiniteTransition(label = "laser_glow")
    val laserGlow by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserGlow"
    )

    val minutes = callState.durationSeconds / 60
    val seconds = callState.durationSeconds % 60
    val timeFormatted = "%02d:%02d".format(minutes, seconds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040711))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Broadcast Status Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberSurface)
                        .border(1.dp, OutlineBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isPaused) Color(0xFFFFB800) else EmeraldAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isPaused) "SCREEN SHARE PAUSED" else "LIVE SCREEN SHARE ACTIVE",
                                color = if (isPaused) Color(0xFFFFB800) else EmeraldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Sharing with $peerName • $timeFormatted",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF142239))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${telemetry.pingMs}ms | ${selectedFps} FPS",
                                color = CyanPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                LatencyHud(telemetry = telemetry)
            }

            // Shared Screen Mock Presentation Canvas with Interactive Laser Pointer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .border(1.5.dp, CyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF091122)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                laserPos = change.position
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                laserPos = offset
                            }
                        }
                        .testTag("screen_share_canvas")
                ) {
                    // Shared Workspace Presentation Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CIPHERWIRE // ARCHITECTURE SPECIFICATION",
                                color = CyanPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "CONFIDENTIAL E2EE",
                                color = CyberCrimson,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Diagram blocks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresentationDiagramNode(
                                title = "ALICE (Client A)",
                                desc = "AES-256-GCM\nOpus low-delay\n14ms latency",
                                accent = CyanPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            PresentationDiagramNode(
                                title = "P2P MESH",
                                desc = "DTLS-SRTP P2P\nDirect Signaling\nZero Server State",
                                accent = EmeraldAccent,
                                modifier = Modifier.weight(1f)
                            )
                            PresentationDiagramNode(
                                title = "BOB (Client B)",
                                desc = "Hardware Decrypt\nMemory < 30MB\nForward Secrecy",
                                accent = VioletSecurity,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Performance Metrics inside stream
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF050A14))
                                .border(1.dp, OutlineBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "LIVE PROTOCOL TELEMETRY STREAM:",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Codec: AV1 Hardware Real-time profile (Zero-Copy)\n• Bitrate: ${telemetry.videoBitrateKbps} kbps (Adaptive Jitter)\n• Device Storage Footprint: Lightweight (< 5 MB Cache)\n• Interactive Annotation: Laser Pointer Tracking Active",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "💡 Tap or drag on the canvas to point with the laser highlight",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Laser Pointer Canvas Overlay
                    if (isLaserActive) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = CyberCrimson.copy(alpha = 0.35f),
                                radius = laserGlow,
                                center = laserPos
                            )
                            drawCircle(
                                color = CyberCrimson,
                                radius = 7f,
                                center = laserPos
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3f,
                                center = laserPos
                            )
                        }
                    }
                }
            }

            // Bottom Presentation Control Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CyberSurface)
                    .border(1.dp, OutlineBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Laser Pointer Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLaserActive) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.dp, if (isLaserActive) CyanPrimary else OutlineBorder, RoundedCornerShape(8.dp))
                            .clickable { isLaserActive = !isLaserActive }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("toggle_laser_pointer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Highlight,
                            contentDescription = "Laser Pointer",
                            tint = if (isLaserActive) CyanPrimary else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Laser Pointer",
                            color = if (isLaserActive) CyanPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // FPS Selector
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "FPS:", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        FpsBadge(fps = 30, selected = selectedFps == 30, onSelect = { selectedFps = 30 })
                        Spacer(modifier = Modifier.width(4.dp))
                        FpsBadge(fps = 60, selected = selectedFps == 60, onSelect = { selectedFps = 60 })
                    }

                    // Pause / Resume Stream
                    IconButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16253D))
                            .testTag("pause_screen_share_button")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause / Resume",
                            tint = if (isPaused) EmeraldAccent else CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action buttons (Switch to video, Stop sharing, End call)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.switchToVideoFromScreenShare() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16253D)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("switch_to_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Return to Video", color = TextPrimary, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.endCall() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCrimson),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("stop_sharing_end_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopScreenShare,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Stop Sharing", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PresentationDiagramNode(
    title: String,
    desc: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C1628))
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = title,
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun FpsBadge(fps: Int, selected: Boolean, onSelect: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) CyanPrimary else Color(0xFF16233B))
            .clickable { onSelect() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "${fps}p",
            color = if (selected) CyberBackground else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
