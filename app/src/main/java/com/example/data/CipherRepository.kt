package com.example.data

import com.example.crypto.CryptoManager
import com.example.data.local.CallLogDao
import com.example.data.local.CallLogEntity
import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CipherRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val callLogDao: CallLogDao
) {
    val conversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    val callLogs: Flow<List<CallLogEntity>> = callLogDao.getAllCallLogs()

    fun getConversation(id: String): Flow<ConversationEntity?> = conversationDao.getConversationById(id)
    fun getMessages(conversationId: String): Flow<List<MessageEntity>> = messageDao.getMessagesForConversation(conversationId)

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        isVoiceNote: Boolean = false,
        voiceDurationSec: Int = 0,
        isSelfDestruct: Boolean = false,
        destructDurationSec: Long = 0L,
        mediaType: String? = null
    ) {
        val payload = CryptoManager.encryptMessage(conversationId, text)
        val now = System.currentTimeMillis()
        val expiresAt = if (isSelfDestruct && destructDurationSec > 0) now + (destructDurationSec * 1000) else 0L

        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = "me",
            text = text,
            ciphertextHex = payload.cipherHex,
            ivHex = payload.ivHex,
            authTagHex = payload.authTagHex,
            timestamp = now,
            isFromMe = true,
            isVoiceNote = isVoiceNote,
            voiceDurationSec = voiceDurationSec,
            isSelfDestruct = isSelfDestruct,
            expiresAt = expiresAt,
            status = "READ",
            mediaType = mediaType
        )
        messageDao.insertMessage(message)
        val preview = if (isVoiceNote) "🎙️ Encrypted voice note ($voiceDurationSec s)" else text
        conversationDao.updateLastMessage(conversationId, preview, now)
    }

    suspend fun receiveSimulatedReply(conversationId: String, peerName: String, replyText: String) {
        val payload = CryptoManager.encryptMessage(conversationId, replyText)
        val now = System.currentTimeMillis()
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = conversationId,
            text = replyText,
            ciphertextHex = payload.cipherHex,
            ivHex = payload.ivHex,
            authTagHex = payload.authTagHex,
            timestamp = now,
            isFromMe = false,
            isVoiceNote = false,
            voiceDurationSec = 0,
            isSelfDestruct = false,
            expiresAt = 0L,
            status = "READ"
        )
        messageDao.insertMessage(message)
        conversationDao.updateLastMessage(conversationId, replyText, now)
    }

    suspend fun logCall(
        peerName: String,
        peerHandle: String,
        callType: String,
        direction: String,
        durationSec: Int,
        latencyMs: Int = 16
    ) {
        val entity = CallLogEntity(
            id = UUID.randomUUID().toString(),
            peerName = peerName,
            peerHandle = peerHandle,
            callType = callType,
            direction = direction,
            timestamp = System.currentTimeMillis(),
            durationSec = durationSec,
            latencyAvgMs = latencyMs,
            bytesTransferredKb = (durationSec * 36).toLong() + 45,
            isE2EEVerified = true
        )
        callLogDao.insertCallLog(entity)
    }

    suspend fun setVerification(conversationId: String, isVerified: Boolean) {
        conversationDao.setVerificationStatus(conversationId, isVerified)
    }

    suspend fun purgeExpiredMessages() {
        messageDao.deleteExpiredMessages(System.currentTimeMillis())
    }

    suspend fun seedInitialDataIfEmpty() {
        val initialConversations = listOf(
            ConversationEntity(
                id = "alex",
                peerName = "Alex Mercer",
                peerHandle = "@alex.mercer",
                peerAvatarRes = "avatar_alex_1791289615343",
                lastMessage = "Double ratchet synchronized. Ultra-low latency mesh is running at 14ms.",
                lastTimestamp = System.currentTimeMillis() - 1000 * 60 * 4,
                unreadCount = 0,
                isVerified = true,
                safetyNumber = CryptoManager.generateSafetyNumber("alex"),
                isOnline = true,
                securityLevel = "AES-256-GCM / Ratchet Epoch #8",
                latencyMs = 14
            ),
            ConversationEntity(
                id = "elena",
                peerName = "Elena Vance",
                peerHandle = "@elena.vance",
                peerAvatarRes = "avatar_elena_1791289640425",
                lastMessage = "Ready for the encrypted video review and screen share.",
                lastTimestamp = System.currentTimeMillis() - 1000 * 60 * 25,
                unreadCount = 1,
                isVerified = true,
                safetyNumber = CryptoManager.generateSafetyNumber("elena"),
                isOnline = true,
                securityLevel = "AES-256-GCM / Ratchet Epoch #12",
                latencyMs = 17
            ),
            ConversationEntity(
                id = "marcus",
                peerName = "Marcus Chen",
                peerHandle = "@marcus.ops",
                peerAvatarRes = "",
                lastMessage = "P2P WebRTC data channels tested: 0.00% packet loss.",
                lastTimestamp = System.currentTimeMillis() - 1000 * 60 * 180,
                unreadCount = 0,
                isVerified = false,
                safetyNumber = CryptoManager.generateSafetyNumber("marcus"),
                isOnline = false,
                securityLevel = "AES-256-GCM / Unverified Peer",
                latencyMs = 22
            ),
            ConversationEntity(
                id = "sarah",
                peerName = "Sarah Lin",
                peerHandle = "@sarah.qkd",
                peerAvatarRes = "",
                lastMessage = "Opus audio frame size set to 20ms FEC for crystal clear calls.",
                lastTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 12,
                unreadCount = 0,
                isVerified = true,
                safetyNumber = CryptoManager.generateSafetyNumber("sarah"),
                isOnline = true,
                securityLevel = "AES-256-GCM / Ratchet Epoch #5",
                latencyMs = 12
            )
        )

        conversationDao.insertConversations(initialConversations)

        // Seed messages for Alex
        val now = System.currentTimeMillis()
        val alexMessages = listOf(
            createSeedMessage("alex", "alex", "Hey, starting the encrypted test session. Verifying zero-knowledge keys.", now - 1000 * 60 * 15, false),
            createSeedMessage("alex", "me", "Keys match on my side. Initialized AES-256-GCM with ChaCha fallback.", now - 1000 * 60 * 12, true),
            createSeedMessage("alex", "alex", "Let's check real-time audio and screen sharing. Latency is hovering around 14ms.", now - 1000 * 60 * 8, false),
            createSeedMessage("alex", "me", "Audio Opus bitrate is only 18 kbps, super lightweight on data.", now - 1000 * 60 * 5, true),
            createSeedMessage("alex", "alex", "Double ratchet synchronized. Ultra-low latency mesh is running at 14ms.", now - 1000 * 60 * 4, false)
        )
        messageDao.insertMessages(alexMessages)

        // Seed messages for Elena
        val elenaMessages = listOf(
            createSeedMessage("elena", "elena", "We updated the security protocol schema for video streams.", now - 1000 * 60 * 45, false),
            createSeedMessage("elena", "me", "Great, hardware acceleration is engaged. RAM usage is under 30MB.", now - 1000 * 60 * 30, true),
            createSeedMessage("elena", "elena", "Ready for the encrypted video review and screen share.", now - 1000 * 60 * 25, false)
        )
        messageDao.insertMessages(elenaMessages)

        // Seed call logs
        val seedCalls = listOf(
            CallLogEntity(
                id = "call-1",
                peerName = "Alex Mercer",
                peerHandle = "@alex.mercer",
                callType = "VIDEO",
                direction = "INCOMING",
                timestamp = now - 1000 * 60 * 60 * 2,
                durationSec = 412,
                latencyAvgMs = 15,
                bytesTransferredKb = 1840,
                isE2EEVerified = true
            ),
            CallLogEntity(
                id = "call-2",
                peerName = "Elena Vance",
                peerHandle = "@elena.vance",
                callType = "SCREEN_SHARE",
                direction = "OUTGOING",
                timestamp = now - 1000 * 60 * 60 * 5,
                durationSec = 780,
                latencyAvgMs = 18,
                bytesTransferredKb = 4250,
                isE2EEVerified = true
            ),
            CallLogEntity(
                id = "call-3",
                peerName = "Sarah Lin",
                peerHandle = "@sarah.qkd",
                callType = "AUDIO",
                direction = "OUTGOING",
                timestamp = now - 1000 * 60 * 60 * 18,
                durationSec = 195,
                latencyAvgMs = 13,
                bytesTransferredKb = 280,
                isE2EEVerified = true
            ),
            CallLogEntity(
                id = "call-4",
                peerName = "Marcus Chen",
                peerHandle = "@marcus.ops",
                callType = "AUDIO",
                direction = "MISSED",
                timestamp = now - 1000 * 60 * 60 * 28,
                durationSec = 0,
                latencyAvgMs = 24,
                bytesTransferredKb = 0,
                isE2EEVerified = false
            )
        )
        callLogDao.insertCallLogs(seedCalls)
    }

    private fun createSeedMessage(convId: String, senderId: String, text: String, timestamp: Long, isFromMe: Boolean): MessageEntity {
        val payload = CryptoManager.encryptMessage(convId, text)
        return MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = convId,
            senderId = senderId,
            text = text,
            ciphertextHex = payload.cipherHex,
            ivHex = payload.ivHex,
            authTagHex = payload.authTagHex,
            timestamp = timestamp,
            isFromMe = isFromMe,
            status = "READ"
        )
    }
}
