package com.example.data.system

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurePreferences(context: Context) {
    private val TAG = "SecurePreferences"
    private val sharedPrefs = context.getSharedPreferences("mixy_secure_prefs", Context.MODE_PRIVATE)

    private val PROVIDER = "AndroidKeyStore"
    private val ALIAS = "MixyOS_Nvidia_Key_Alias"
    private val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        try {
            val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
            if (!keyStore.containsAlias(ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
                val spec = KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
                Log.d(TAG, "Android Keystore key generated successfully under alias: $ALIAS")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Keystore", e)
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
            val entry = keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve secret key", e)
            null
        }
    }

    fun encrypt(plainText: String): String? {
        if (plainText.isEmpty()) return ""
        return try {
            val secretKey = getSecretKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Format: iv_length (1 byte) + iv + ciphertext
            val combined = ByteArray(1 + iv.size + encryptedBytes.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, 1 + iv.size, encryptedBytes.size)
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption error", e)
            null
        }
    }

    fun decrypt(encryptedText: String): String? {
        if (encryptedText.isEmpty()) return ""
        return try {
            val secretKey = getSecretKey() ?: return null
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            if (combined.isEmpty()) return ""
            
            val ivSize = combined[0].toInt()
            val iv = ByteArray(ivSize)
            System.arraycopy(combined, 1, iv, 0, ivSize)
            
            val encryptedBytes = ByteArray(combined.size - 1 - ivSize)
            System.arraycopy(combined, 1 + ivSize, encryptedBytes, 0, encryptedBytes.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption error", e)
            null
        }
    }

    fun saveNvidiaApiKey(key: String) {
        val encrypted = encrypt(key)
        if (encrypted != null) {
            sharedPrefs.edit().putString("nvidia_api_key", encrypted).apply()
            Log.d(TAG, "Nvidia API Key encrypted and saved.")
        } else {
            Log.e(TAG, "Failed to save API Key due to encryption failure.")
        }
    }

    fun getNvidiaApiKey(): String {
        val encrypted = sharedPrefs.getString("nvidia_api_key", "") ?: ""
        if (encrypted.isEmpty()) return ""
        return decrypt(encrypted) ?: ""
    }

    fun clearNvidiaApiKey() {
        sharedPrefs.edit().remove("nvidia_api_key").apply()
        Log.d(TAG, "Nvidia API Key removed.")
    }

    fun saveUserName(name: String) {
        sharedPrefs.edit().putString("operator_user_name", name).apply()
        Log.d(TAG, "User name saved: $name")
    }

    fun getUserName(): String {
        return sharedPrefs.getString("operator_user_name", "Shivam") ?: "Shivam"
    }

    fun saveSelectedVoice(voiceId: String) {
        sharedPrefs.edit().putString("selected_kokoro_voice", voiceId).apply()
        Log.d(TAG, "Selected Kokoro voice saved: $voiceId")
    }

    fun getSelectedVoice(): String {
        return sharedPrefs.getString("selected_kokoro_voice", "am_adam") ?: "am_adam"
    }
}
