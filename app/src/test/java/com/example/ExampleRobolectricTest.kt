package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CipherWire", appName)
    }

    @Test
    fun `test end to end encryption and decryption`() {
        val peerId = "alex"
        val plainText = "Confidential E2EE Payload: Opus 18kbps & AV1"
        val encrypted = CryptoManager.encryptMessage(peerId, plainText)

        assertNotNull(encrypted.cipherHex)
        assertTrue(encrypted.cipherHex.isNotEmpty())
        assertTrue(encrypted.ivHex.isNotEmpty())

        val decrypted = CryptoManager.decryptMessage(
            peerId = peerId,
            cipherHex = encrypted.cipherHex,
            ivHex = encrypted.ivHex,
            authTagHex = encrypted.authTagHex
        )

        assertEquals(plainText, decrypted)
    }

    @Test
    fun `test safety number fingerprint generation`() {
        val safetyNumber = CryptoManager.generateSafetyNumber("elena")
        assertNotNull(safetyNumber)
        val blocks = safetyNumber.split(" ")
        assertEquals(12, blocks.size)
        assertTrue(blocks.all { it.length == 5 })
    }
}
