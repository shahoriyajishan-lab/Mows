package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AudioCallScreen
import com.example.ui.screens.CallsHistoryScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.PerformanceSettingsScreen
import com.example.ui.screens.SafetyVerificationScreen
import com.example.ui.screens.ScreenShareScreen
import com.example.ui.screens.VideoCallScreen
import com.example.ui.theme.CipherBackground
import com.example.ui.theme.CipherWireTheme
import com.example.viewmodel.CipherWireViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CipherWireTheme {
                CipherWireApp()
            }
        }
    }
}

@Composable
fun CipherWireApp(viewModel: CipherWireViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CipherBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                is Screen.ChatList -> {
                    ChatListScreen(viewModel = viewModel)
                }
                is Screen.ChatDetail -> {
                    ChatDetailScreen(
                        conversationId = screen.conversationId,
                        viewModel = viewModel
                    )
                }
                is Screen.AudioCall -> {
                    AudioCallScreen(
                        peerName = screen.peerName,
                        peerHandle = screen.peerHandle,
                        avatarRes = screen.avatarRes,
                        viewModel = viewModel
                    )
                }
                is Screen.VideoCall -> {
                    VideoCallScreen(
                        peerName = screen.peerName,
                        peerHandle = screen.peerHandle,
                        avatarRes = screen.avatarRes,
                        viewModel = viewModel
                    )
                }
                is Screen.ScreenShare -> {
                    ScreenShareScreen(
                        peerName = screen.peerName,
                        peerHandle = screen.peerHandle,
                        viewModel = viewModel
                    )
                }
                is Screen.SafetyVerification -> {
                    SafetyVerificationScreen(
                        conversationId = screen.conversationId,
                        viewModel = viewModel
                    )
                }
                is Screen.CallLogs -> {
                    CallsHistoryScreen(viewModel = viewModel)
                }
                is Screen.PerformanceSettings -> {
                    PerformanceSettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
