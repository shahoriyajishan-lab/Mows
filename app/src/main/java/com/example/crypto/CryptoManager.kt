package com.example.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class CipherPayload(
    val plainText: String,
    val cipherHex: String,
    val ivHex: String,
    val authTagHex: String,
    val ratchetStep: Int,
    val keyFingerprint: String
)

object CryptoManager {
    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private val secureRandom = SecureRandom()
    private val sessionKeys = mutableMapOf<String, SecretKey>()
    private val sessionRatchets = mutableMapOf<String, Int>()

    init {
        // Pre-initialize default conversation session keys
        getOrCreateKey("alex")
        getOrCreateKey("elena")
        getOrCreateKey("marcus")
    }

    fun getOrCreateKey(peerId: String): SecretKey {
        return sessionKeys.getOrPut(peerId) {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(AES_KEY_SIZE, secureRandom)
            keyGen.generateKey()
        }
    }

    fun rotateSessionKey(peerId: String): String {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE, secureRandom)
        val newKey = keyGen.generateKey()
        sessionKeys[peerId] = newKey
        val currentStep = sessionRatchets.getOrDefault(peerId, 1) + 1
        sessionRatchets[peerId] = currentStep
        return getKeyFingerprint(newKey)
    }

    fun getRatchetStep(peerId: String): Int {
        return sessionRatchets.getOrDefault(peerId, 1)
    }

    fun encryptMessage(peerId: String, plainText: String): CipherPayload {
        val key = getOrCreateKey(peerId)
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        // GCM appends tag to cipherBytes at the end (last 16 bytes)
        val tagSize = 16
        val actualCipher = if (cipherBytes.size >= tagSize) {
            cipherBytes.copyOfRange(0, cipherBytes.size - tagSize)
        } else cipherBytes
        val tagBytes = if (cipherBytes.size >= tagSize) {
            cipherBytes.copyOfRange(cipherBytes.size - tagSize, cipherBytes.size)
        } else ByteArray(0)

        val step = sessionRatchets.getOrPut(peerId) { 1 }
        sessionRatchets[peerId] = step + 1

        return CipherPayload(
            plainText = plainText,
            cipherHex = bytesToHex(actualCipher),
            ivHex = bytesToHex(iv),
            authTagHex = bytesToHex(tagBytes),
            ratchetStep = step,
            keyFingerprint = getKeyFingerprint(key)
        )
    }

    fun decryptMessage(peerId: String, cipherHex: String, ivHex: String, authTagHex: String): String {
        return try {
            val key = getOrCreateKey(peerId)
            val iv = hexToBytes(ivHex)
            val cipherBytes = hexToBytes(cipherHex)
            val tagBytes = if (authTagHex.isNotEmpty()) hexToBytes(authTagHex) else ByteArray(0)

            val fullCipherPayload = ByteArray(cipherBytes.size + tagBytes.size)
            System.arraycopy(cipherBytes, 0, fullCipherPayload, 0, cipherBytes.size)
            System.arraycopy(tagBytes, 0, fullCipherPayload, cipherBytes.size, tagBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val plainBytes = cipher.doFinal(fullCipherPayload)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "[Decryption Failed: Authentic Key Required]"
        }
    }

    fun generateSafetyNumber(peerId: String, localId: String = "me"): String {
        val combined = "$peerId:$localId:cipherwire_v1_secure_channel"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(combined.toByteArray(Charsets.UTF_8))

        // Convert first 30 bytes into 12 5-digit decimal blocks
        val sb = StringBuilder()
        for (i in 0 until 12) {
            val offset = (i * 2) % (hash.size - 2)
            val byte1 = hash[offset].toInt() and 0xFF
            val byte2 = hash[offset + 1].toInt() and 0xFF
            val value = (byte1 * 256 + byte2) % 100000
            val formatted = String.format("%05d", value)
            sb.append(formatted)
            if (i < 11) sb.append(" ")
        }
        return sb.toString()
    }

    fun getKeyFingerprint(key: SecretKey): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(key.encoded ?: "default_seed".toByteArray())
        return bytesToHex(digest.copyOfRange(0, 8)).uppercase().chunked(4).joinToString(":")
    }

    // Generates a 21x21 pseudo QR matrix boolean pattern based on safety string
    fun generateQrMatrix(data: String): List<List<Boolean>> {
        val size = 21
        val matrix = MutableList(size) { MutableList(size) { false } }
        val hash = MessageDigest.getInstance("SHA-256").digest(data.toByteArray())

        // Finder patterns in corners
        fun drawFinder(top: Int, left: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    matrix[top + r][left + c] = isBorder || isCenter
                }
            }
        }
        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // Fill remaining with hash bits + data
        var byteIdx = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= size - 8
                val inBottomLeft = r >= size - 8 && c < 8
                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                    val hByte = hash[byteIdx % hash.size].toInt()
                    val bit = (hByte shr ((r + c) % 8)) and 1
                    matrix[r][c] = (bit == 1) || ((r * c) % 3 == 0)
                    byteIdx++
                }
            }
        }
        return matrix
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.replace(" ", "")
        val result = ByteArray(clean.length / 2)
        for (i in result.indices) {
            val index = i * 2
            val v = clean.substring(index, index + 2).toInt(16)
            result[i] = v.toByte()
        }
        return result
    }
}
