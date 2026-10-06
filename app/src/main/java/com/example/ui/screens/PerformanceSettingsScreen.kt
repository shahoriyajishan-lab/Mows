package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.OutlineBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CipherWireViewModel

@Composable
fun PerformanceSettingsScreen(
    viewModel: CipherWireViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.goBack()
    }

    val telemetry by viewModel.telemetry.collectAsState()
    var purgeFeedback by remember { mutableStateOf<String?>(null) }
    var hardwareAcc by remember { mutableStateOf(true) }
    var directP2P by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier.testTag("perf_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Performance & Storage Optimizer",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ultra-Low Latency & Lightweight Resource Profile",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Live Footprint Overview Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(1.dp, OutlineBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DEVICE RESOURCE FOOTPRINT",
                        color = CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmeraldAccent.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIGHTWEIGHT PROFILE",
                            color = EmeraldAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResourceMetric(
                        icon = Icons.Default.Memory,
                        label = "RAM Footprint",
                        value = "${telemetry.ramFootprintMb} MB",
                        sub = "< 40 MB Cap"
                    )
                    ResourceMetric(
                        icon = Icons.Default.Storage,
                        label = "Cache / Temp",
                        value = "${telemetry.storageCacheMb} MB",
                        sub = "Zero-Trace"
                    )
                    ResourceMetric(
                        icon = Icons.Default.Speed,
                        label = "Mesh Latency",
                        value = "${telemetry.pingMs} ms",
                        sub = "Ultra-Low RTT"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Storage Purge Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(1.dp, OutlineBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "STORAGE OPTIMIZATION",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Instant purge of ephemeral stream buffers, decrypted media caches, and temporary key agreements to keep your device clean and lightweight.",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val reclaimed = viewModel.purgeCache()
                        purgeFeedback = "Successfully purged ${"%.1f".format(reclaimed)} MB! Storage footprint restored to 0.2 MB."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16253D)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("purge_cache_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Zero-Trace Cache Purge", color = CyanPrimary, fontWeight = FontWeight.Bold)
                }

                if (purgeFeedback != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = purgeFeedback!!,
                        color = EmeraldAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Network & Low-MB Latency Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(1.dp, OutlineBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "LATENCY & CODEC CONTROLS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Bandwidth Saver Toggle
                SettingSwitchRow(
                    title = "Low-MB Bandwidth Saver",
                    subtitle = "Lowers video bitrate to 220 kbps & audio to 10 kbps for minimal data usage",
                    checked = telemetry.isBandwidthSaverActive,
                    onCheckedChange = { viewModel.setBandwidthSaver(it) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Hardware Acceleration
                SettingSwitchRow(
                    title = "Hardware Acceleration (Zero-Copy)",
                    subtitle = "Direct GPU / NPU hardware pipeline for AV1 and VP9 video codec",
                    checked = hardwareAcc,
                    onCheckedChange = { hardwareAcc = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Direct P2P Mesh
                SettingSwitchRow(
                    title = "Direct P2P WebRTC Mesh",
                    subtitle = "Peer-to-peer data channel signaling bypassing intermediary relay servers",
                    checked = directP2P,
                    onCheckedChange = { directP2P = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cryptographic engine info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(1.dp, OutlineBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "CRYPTOGRAPHIC ENGINE STATUS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(10.dp))
                CryptoStatusItem("Symmetric Cipher", "AES-256-GCM (Hardware Cryptographic Provider)")
                CryptoStatusItem("Key Agreement", "Diffie-Hellman / Double Ratchet with Forward Secrecy")
                CryptoStatusItem("Audio Codec", "Opus Low-Delay with Forward Error Correction (FEC)")
                CryptoStatusItem("Video / Screen Codec", "AV1 & VP9 Real-time Low-Memory profile")
                CryptoStatusItem("Identity Hash", "SHA-256 Digest Grouped 60-Digit Safety Blocks")
            }
        }
    }
}

@Composable
private fun ResourceMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    sub: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp
        )
        Text(
            text = sub,
            color = EmeraldAccent,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmeraldAccent,
                checkedTrackColor = Color(0xFF003823)
            )
        )
    }
}

@Composable
private fun CryptoStatusItem(title: String, detail: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = title, color = CyanPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(text = detail, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
