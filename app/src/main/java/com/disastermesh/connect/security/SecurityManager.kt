package com.disastermesh.connect.security

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

/**
 * SecurityManager — Lightweight E2E encryption for mesh packets.
 * Uses AES-256-GCM for message encryption.
 * RSA key pair stored in Android Keystore for identity.
 * No server dependency — all keys generated locally.
 */
@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val RSA_ALIAS = "DisasterMeshIdentityKey"
        private const val AES_ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val AES_KEY_SIZE = 256
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    // ─────────────────────────────────────────────────────
    // DEVICE IDENTITY KEY PAIR (RSA in Android Keystore)
    // ─────────────────────────────────────────────────────

    /**
     * Generate RSA identity key pair in secure Android Keystore.
     * Called once on first launch.
     */
    fun generateIdentityKeyPair() {
        if (keyStore.containsAlias(RSA_ALIAS)) return // Already exists
        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER
        )
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

    /**
     * Export public key as Base64 for inclusion in identity packets.
     */
    fun getPublicKeyBase64(): String {
        val certificate = keyStore.getCertificate(RSA_ALIAS) ?: run {
            generateIdentityKeyPair()
            keyStore.getCertificate(RSA_ALIAS)!!
        }
        return Base64.encodeToString(certificate.publicKey.encoded, Base64.NO_WRAP)
    }

    // ─────────────────────────────────────────────────────
    // AES-256-GCM MESSAGE ENCRYPTION
    // ─────────────────────────────────────────────────────

    /**
     * Generate a random AES-256 session key.
     * In production: derive shared key via ECDH or include in RSA-encrypted header.
     */
    fun generateAesKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE, SecureRandom())
        return keyGen.generateKey()
    }

    /**
     * Encrypt plaintext with AES-256-GCM.
     * Returns Base64(IV + CipherText).
     */
    fun encrypt(plaintext: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + cipherBytes
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt AES-256-GCM ciphertext.
     * Expects Base64(IV + CipherText).
     */
    fun decrypt(encryptedBase64: String, key: SecretKey): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }

    /**
     * Derive a deterministic session key from two device IDs.
     * Used for P2P encryption without key exchange round-trip.
     * Both sides produce the same key independently.
     */
    fun deriveSessionKey(deviceIdA: String, deviceIdB: String): SecretKey {
        val sorted = listOf(deviceIdA, deviceIdB).sorted()
        val combined = "${sorted[0]}:${sorted[1]}:DisasterMesh"
        val digest = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray())
        return SecretKeySpec(digest, "AES")
    }

    // ─────────────────────────────────────────────────────
    // PHONE NUMBER HASHING
    // ─────────────────────────────────────────────────────

    /**
     * One-way hash phone number for private contact matching.
     * Never store raw phone in plaintext.
     */
    fun hashPhoneNumber(phone: String): String {
        val normalized = phone.replace(Regex("[^0-9+]"), "")
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}
