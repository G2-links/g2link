package com.g2link.connect.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val RSA_ALIAS = "G2LinkIdentityKey"
        private const val AES_ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val AES_KEY_SIZE = 256
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    fun generateIdentityKeyPair() {
        if (keyStore.containsAlias(RSA_ALIAS)) return
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER)
        kpg.initialize(
            KeyGenParameterSpec.Builder(
                RSA_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setUserAuthenticationRequired(false)
                .build()
        )
        kpg.generateKeyPair()
    }

    fun getPublicKeyBase64(): String {
        val certificate = keyStore.getCertificate(RSA_ALIAS) ?: run {
            generateIdentityKeyPair()
            keyStore.getCertificate(RSA_ALIAS)!!
        }
        return Base64.encodeToString(certificate.publicKey.encoded, Base64.NO_WRAP)
    }

    fun generateAesKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE, SecureRandom())
        return keyGen.generateKey()
    }

    fun encrypt(plaintext: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + cipherBytes, Base64.NO_WRAP)
    }

    fun decrypt(encryptedBase64: String, key: SecretKey): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }

    fun deriveSessionKey(deviceIdA: String, deviceIdB: String): SecretKey {
        val sorted = listOf(deviceIdA, deviceIdB).sorted()
        val combined = "${sorted[0]}:${sorted[1]}:G2Link"
        val digest = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray())
        return SecretKeySpec(digest, "AES")
    }

    fun hashPhoneNumber(phone: String): String {
        val normalized = phone.replace(Regex("[^0-9+]"), "")
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}
