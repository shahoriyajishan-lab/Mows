package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CipherRepository
import com.example.data.local.AppDatabase
import com.example.data.local.CallLogEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.telemetry.TelemetryEngine
import com.example.telemetry.TelemetryStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class Screen {
    data object ChatList : Screen()
    data class ChatDetail(val conversationId: String) : Screen()
    data class AudioCall(val peerName: String, val peerHandle: String, val avatarRes: String) : Screen()
    data class VideoCall(val peerName: String, val peerHandle: String, val avatarRes: String) : Screen()
    data class ScreenShare(val peerName: String, val peerHandle: String) : Screen()
    data class SafetyVerification(val conversationId: String) : Screen()
    data object CallLogs : Screen()
    data object PerformanceSettings : Screen()
}

data class ActiveCallState(
    val peerName: String = "",
    val peerHandle: String = "",
    val avatarRes: String = "",
    val callType: String = "AUDIO", // AUDIO, VIDEO, SCREEN_SHARE
    val isConnected: Boolean = true,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isVideoEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    val isScreenSharing: Boolean = false,
    val isLaserPointerActive: Boolean = false,
    val laserPosition: Offset = Offset.Zero,
    val isLowBandwidthMode: Boolean = false
)

class CipherWireViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = CipherRepository(
        database.conversationDao(),
        database.messageDao(),
        database.callLogDao()
    )

    private val telemetryEngine = TelemetryEngine(viewModelScope)
    val telemetry: StateFlow<TelemetryStats> = telemetryEngine.telemetry

    val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallLogEntity>> = repository.callLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow<Screen>(Screen.ChatList)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenBackStack = mutableListOf<Screen>(Screen.ChatList)

    private val _activeCall = MutableStateFlow(ActiveCallState())
    val activeCall: StateFlow<ActiveCallState> = _activeCall.asStateFlow()

    private var callTimerJob: Job? = null
    private var voiceRecordJob: Job? = null

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _voiceDurationSec = MutableStateFlow(0)
    val voiceDurationSec: StateFlow<Int> = _voiceDurationSec.asStateFlow()

    private val _selectedDestructDuration = MutableStateFlow(0L) // 0L = Off, 10L, 60L, 3600L
    val selectedDestructDuration: StateFlow<Long> = _selectedDestructDuration.asStateFlow()

    private val _inspectingMessage = MutableStateFlow<MessageEntity?>(null)
    val inspectingMessage: StateFlow<MessageEntity?> = _inspectingMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
        startPeriodicExpirationPurger()
    }

    fun navigateTo(screen: Screen) {
        screenBackStack.add(screen)
        _currentScreen.value = screen
    }

    fun goBack(): Boolean {
        if (screenBackStack.size > 1) {
            screenBackStack.removeAt(screenBackStack.lastIndex)
            _currentScreen.value = screenBackStack.last()
            return true
        }
        return false
    }

    fun startAudioCall(peerName: String, peerHandle: String, avatarRes: String) {
        _activeCall.value = ActiveCallState(
            peerName = peerName,
            peerHandle = peerHandle,
            avatarRes = avatarRes,
            callType = "AUDIO",
            isConnected = true,
            durationSeconds = 0,
            isMuted = false,
            isSpeakerOn = false
        )
        startCallTimer()
        navigateTo(Screen.AudioCall(peerName, peerHandle, avatarRes))
    }

    fun startVideoCall(peerName: String, peerHandle: String, avatarRes: String) {
        _activeCall.value = ActiveCallState(
            peerName = peerName,
            peerHandle = peerHandle,
            avatarRes = avatarRes,
            callType = "VIDEO",
            isConnected = true,
            durationSeconds = 0,
            isMuted = false,
            isSpeakerOn = true,
            isVideoEnabled = true,
            isFrontCamera = true
        )
        startCallTimer()
        navigateTo(Screen.VideoCall(peerName, peerHandle, avatarRes))
    }

    fun startScreenShare(peerName: String, peerHandle: String) {
        _activeCall.value = _activeCall.value.copy(
            peerName = peerName,
            peerHandle = peerHandle,
            callType = "SCREEN_SHARE",
            isConnected = true,
            isScreenSharing = true
        )
        if (callTimerJob == null) startCallTimer()
        navigateTo(Screen.ScreenShare(peerName, peerHandle))
    }

    fun switchToScreenShareFromCall() {
        val current = _activeCall.value
        _activeCall.value = current.copy(
            callType = "SCREEN_SHARE",
            isScreenSharing = true
        )
        navigateTo(Screen.ScreenShare(current.peerName, current.peerHandle))
    }

    fun switchToVideoFromScreenShare() {
        val current = _activeCall.value
        _activeCall.value = current.copy(
            callType = "VIDEO",
            isScreenSharing = false
        )
        navigateTo(Screen.VideoCall(current.peerName, current.peerHandle, current.avatarRes))
    }

    fun endCall() {
        val current = _activeCall.value
        callTimerJob?.cancel()
        callTimerJob = null

        viewModelScope.launch {
            repository.logCall(
                peerName = current.peerName.ifEmpty { "Secure Peer" },
                peerHandle = current.peerHandle.ifEmpty { "@secure" },
                callType = current.callType,
                direction = "OUTGOING",
                durationSec = current.durationSeconds,
                latencyMs = telemetry.value.pingMs
            )
        }

        _activeCall.value = ActiveCallState()

        // Pop all call screens until we reach chat or main list
        while (screenBackStack.isNotEmpty() &&
            (screenBackStack.last() is Screen.AudioCall ||
             screenBackStack.last() is Screen.VideoCall ||
             screenBackStack.last() is Screen.ScreenShare)
        ) {
            screenBackStack.removeAt(screenBackStack.lastIndex)
        }
        _currentScreen.value = screenBackStack.lastOrNull() ?: Screen.ChatList
    }

    fun toggleMute() {
        _activeCall.value = _activeCall.value.copy(isMuted = !_activeCall.value.isMuted)
    }

    fun toggleSpeaker() {
        _activeCall.value = _activeCall.value.copy(isSpeakerOn = !_activeCall.value.isSpeakerOn)
    }

    fun toggleVideo() {
        _activeCall.value = _activeCall.value.copy(isVideoEnabled = !_activeCall.value.isVideoEnabled)
    }

    fun flipCamera() {
        _activeCall.value = _activeCall.value.copy(isFrontCamera = !_activeCall.value.isFrontCamera)
    }

    fun toggleLaserPointer() {
        _activeCall.value = _activeCall.value.copy(isLaserPointerActive = !_activeCall.value.isLaserPointerActive)
    }

    fun updateLaserPosition(offset: Offset) {
        _activeCall.value = _activeCall.value.copy(laserPosition = offset)
    }

    fun setDestructDuration(seconds: Long) {
        _selectedDestructDuration.value = seconds
    }

    fun startVoiceRecording() {
        _isRecordingVoice.value = true
        _voiceDurationSec.value = 0
        voiceRecordJob?.cancel()
        voiceRecordJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _voiceDurationSec.value += 1
            }
        }
    }

    fun cancelVoiceRecording() {
        voiceRecordJob?.cancel()
        voiceRecordJob = null
        _isRecordingVoice.value = false
        _voiceDurationSec.value = 0
    }

    fun sendVoiceMessage(conversationId: String) {
        val duration = _voiceDurationSec.value.coerceAtLeast(1)
        voiceRecordJob?.cancel()
        voiceRecordJob = null
        _isRecordingVoice.value = false
        _voiceDurationSec.value = 0

        viewModelScope.launch {
            val destructDuration = _selectedDestructDuration.value
            repository.sendMessage(
                conversationId = conversationId,
                text = "🎙️ Encrypted Voice Transmission ($duration s)",
                isVoiceNote = true,
                voiceDurationSec = duration,
                isSelfDestruct = destructDuration > 0,
                destructDurationSec = destructDuration
            )
        }
    }

    fun sendTextMessage(conversationId: String, text: String) {
        if (text.isBlank()) return
        val destructDuration = _selectedDestructDuration.value
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = conversationId,
                text = text,
                isVoiceNote = false,
                isSelfDestruct = destructDuration > 0,
                destructDurationSec = destructDuration
            )

            // Trigger an authentic responsive peer reply if conversing with seeded peers
            delay(1500)
            val reply = when (conversationId) {
                "alex" -> "Decrypted payload with authentication tag OK. Latency verified at ${telemetry.value.pingMs}ms."
                "elena" -> "Received with forward secrecy intact. Audio/video mesh channel ready."
                "marcus" -> "Acknowledged over direct peer channel."
                else -> "Encrypted packet acknowledged by peer."
            }
            repository.receiveSimulatedReply(conversationId, conversationId, reply)
        }
    }

    fun toggleVerification(conversationId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.setVerification(conversationId, !currentStatus)
        }
    }

    fun inspectMessageCrypto(message: MessageEntity?) {
        _inspectingMessage.value = message
    }

    fun purgeCache(): Float {
        return telemetryEngine.purgeCache()
    }

    fun setBandwidthSaver(enabled: Boolean) {
        telemetryEngine.setBandwidthSaver(enabled)
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _activeCall.value = _activeCall.value.copy(
                    durationSeconds = _activeCall.value.durationSeconds + 1
                )
            }
        }
    }

    private fun startPeriodicExpirationPurger() {
        viewModelScope.launch {
            while (isActive) {
                delay(3000)
                repository.purgeExpiredMessages()
            }
        }
    }
}
