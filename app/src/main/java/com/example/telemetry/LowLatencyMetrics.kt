package com.example.telemetry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class TelemetryStats(
    val pingMs: Int = 16,
    val jitterMs: Float = 0.3f,
    val packetLossPercent: Float = 0.0f,
    val audioBitrateKbps: Int = 18,
    val videoBitrateKbps: Int = 420,
    val audioCodec: String = "Opus Low-Delay FEC",
    val videoCodec: String = "AV1 Zero-Copy HW",
    val currentFps: Int = 60,
    val meshProtocol: String = "Direct P2P WebRTC / DTLS-SRTP",
    val ramFootprintMb: Float = 28.5f,
    val storageCacheMb: Float = 1.8f,
    val isHardwareAccelerated: Boolean = true,
    val isBandwidthSaverActive: Boolean = false,
    val isDirectP2P: Boolean = true
)

class TelemetryEngine(private val scope: CoroutineScope) {
    private val _telemetry = MutableStateFlow(TelemetryStats())
    val telemetry: StateFlow<TelemetryStats> = _telemetry.asStateFlow()

    private var bandwidthSaver = false
    private var simulatedPingBase = 15

    init {
        startTelemetryLoop()
    }

    fun setBandwidthSaver(enabled: Boolean) {
        bandwidthSaver = enabled
        _telemetry.value = _telemetry.value.copy(
            isBandwidthSaverActive = enabled,
            audioBitrateKbps = if (enabled) 10 else 18,
            videoBitrateKbps = if (enabled) 220 else 420,
            currentFps = if (enabled) 30 else 60
        )
    }

    fun purgeCache(): Float {
        val previousCache = _telemetry.value.storageCacheMb
        _telemetry.value = _telemetry.value.copy(
            storageCacheMb = 0.2f
        )
        return previousCache
    }

    private fun startTelemetryLoop() {
        scope.launch {
            while (isActive) {
                delay(1200)
                val current = _telemetry.value
                val pingDelta = Random.nextInt(-2, 3)
                val newPing = (simulatedPingBase + pingDelta).coerceIn(11, 28)
                val newJitter = (0.2f + Random.nextFloat() * 0.3f)
                val ramVariation = (27.5f + Random.nextFloat() * 2.0f)
                val audioRate = if (bandwidthSaver) 10 + Random.nextInt(0, 3) else 18 + Random.nextInt(-2, 3)
                val videoRate = if (bandwidthSaver) 220 + Random.nextInt(-10, 15) else 420 + Random.nextInt(-20, 30)

                _telemetry.value = current.copy(
                    pingMs = newPing,
                    jitterMs = "%.2f".format(newJitter).toFloat(),
                    packetLossPercent = if (Random.nextInt(0, 15) == 0) 0.05f else 0.0f,
                    audioBitrateKbps = audioRate,
                    videoBitrateKbps = videoRate,
                    ramFootprintMb = "%.1f".format(ramVariation).toFloat()
                )
            }
        }
    }
}
