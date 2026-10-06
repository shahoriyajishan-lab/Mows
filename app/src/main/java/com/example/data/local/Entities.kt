package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val peerName: String,
    val peerHandle: String,
    val peerAvatarRes: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val unreadCount: Int = 0,
    val isVerified: Boolean = false,
    val safetyNumber: String,
    val isOnline: Boolean = true,
    val securityLevel: String = "AES-256-GCM Ratchet",
    val latencyMs: Int = 16
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val ciphertextHex: String,
    val ivHex: String,
    val authTagHex: String = "",
    val timestamp: Long,
    val isFromMe: Boolean,
    val isVoiceNote: Boolean = false,
    val voiceDurationSec: Int = 0,
    val isSelfDestruct: Boolean = false,
    val expiresAt: Long = 0L,
    val status: String = "READ",
    val mediaType: String? = null
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val peerName: String,
    val peerHandle: String,
    val callType: String, // AUDIO, VIDEO, SCREEN_SHARE
    val direction: String, // INCOMING, OUTGOING, MISSED
    val timestamp: Long,
    val durationSec: Int,
    val latencyAvgMs: Int = 18,
    val bytesTransferredKb: Long = 120,
    val isE2EEVerified: Boolean = true
)
