package com.suvojeet.notenext.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.NoteVersion
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoUtils {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "NoteNextSecretKey"
    private const val AUTH_KEY_ALIAS = "NoteNextAuthSecretKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun getSecretKey(alias: String = KEY_ALIAS, requireAuth: Boolean = false): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(alias, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            
        if (requireAuth) {
            builder.setUserAuthenticationRequired(true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(300, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
            } else {
                builder.setUserAuthenticationValidityDurationSeconds(300)
            }
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    fun getEncryptionCipher(isLocked: Boolean): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val alias = if (isLocked) AUTH_KEY_ALIAS else KEY_ALIAS
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(alias, isLocked))
        return cipher
    }

    fun getDecryptionCipher(iv: ByteArray, isLocked: Boolean): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val alias = if (isLocked) AUTH_KEY_ALIAS else KEY_ALIAS
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(alias, isLocked), GCMParameterSpec(128, iv))
        return cipher
    }

    fun encryptNote(note: Note): Note {
        if (note.isEncrypted) return note

        val isLocked = note.isLocked
        
        // Encrypt title
        val cipherTitle = getEncryptionCipher(isLocked)
        val ivTitle = cipherTitle.iv
        val encryptedTitle = Base64.encodeToString(cipherTitle.doFinal(note.title.toByteArray()), Base64.DEFAULT)

        // Encrypt content
        val cipherContent = getEncryptionCipher(isLocked)
        val ivContent = cipherContent.iv
        val encryptedContent = Base64.encodeToString(cipherContent.doFinal(note.content.toByteArray()), Base64.DEFAULT)

        // Combine IVs: ivTitle:ivContent
        val combinedIv = Base64.encodeToString(ivTitle, Base64.DEFAULT) + ":" + Base64.encodeToString(ivContent, Base64.DEFAULT)

        return note.copy(
            title = encryptedTitle,
            content = encryptedContent,
            iv = combinedIv,
            isEncrypted = true
        )
    }

    /**
     * Decrypts a note. 
     * If isLocked is true, this WILL FAIL unless called after biometric authentication 
     * or if the key doesn't require auth (legacy notes).
     */
    fun decryptNote(note: Note, authenticatedCipherTitle: Cipher? = null, authenticatedCipherContent: Cipher? = null): Note {
        if (!note.isEncrypted || note.iv == null) return note

        return try {
            val ivs = note.iv.split(":")
            val isLocked = note.isLocked && note.iv.contains(":") // Old single-IV notes might not be easily auth-bindable without migration

            val (decryptedTitle, decryptedContent) = if (ivs.size == 2) {
                val ivTitle = Base64.decode(ivs[0], Base64.DEFAULT)
                val ivContent = Base64.decode(ivs[1], Base64.DEFAULT)
                
                val title = if (authenticatedCipherTitle != null) {
                    String(authenticatedCipherTitle.doFinal(Base64.decode(note.title, Base64.DEFAULT)))
                } else {
                    val cipherTitle = getDecryptionCipher(ivTitle, isLocked)
                    String(cipherTitle.doFinal(Base64.decode(note.title, Base64.DEFAULT)))
                }
                
                val content = if (authenticatedCipherContent != null) {
                    String(authenticatedCipherContent.doFinal(Base64.decode(note.content, Base64.DEFAULT)))
                } else {
                    val cipherContent = getDecryptionCipher(ivContent, isLocked)
                    String(cipherContent.doFinal(Base64.decode(note.content, Base64.DEFAULT)))
                }
                
                Pair(title, content)
            } else {
                val iv = Base64.decode(note.iv, Base64.DEFAULT)
                val cipher = getDecryptionCipher(iv, isLocked)
                val title = String(cipher.doFinal(Base64.decode(note.title, Base64.DEFAULT)))
                
                // Re-init for content
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(if (isLocked) AUTH_KEY_ALIAS else KEY_ALIAS, isLocked), GCMParameterSpec(128, iv))
                val content = String(cipher.doFinal(Base64.decode(note.content, Base64.DEFAULT)))
                Pair(title, content)
            }

            note.copy(
                title = decryptedTitle,
                content = decryptedContent,
                iv = null,
                isEncrypted = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            note.copy(
                title = if (note.title.length > 20) "⚠ Decryption Failed" else note.title,
                content = "Unable to decrypt this note. It may require biometric authentication or the key was lost.",
                isEncrypted = true
            )
        }
    }

    fun encryptNoteVersion(version: NoteVersion, isLocked: Boolean): NoteVersion {
        if (version.isEncrypted) return version

        // Encrypt title
        val cipherTitle = getEncryptionCipher(isLocked)
        val ivTitle = cipherTitle.iv
        val encryptedTitle = Base64.encodeToString(cipherTitle.doFinal(version.title.toByteArray()), Base64.DEFAULT)

        // Encrypt content
        val cipherContent = getEncryptionCipher(isLocked)
        val ivContent = cipherContent.iv
        val encryptedContent = Base64.encodeToString(cipherContent.doFinal(version.content.toByteArray()), Base64.DEFAULT)

        val combinedIv = Base64.encodeToString(ivTitle, Base64.DEFAULT) + ":" + Base64.encodeToString(ivContent, Base64.DEFAULT)

        return version.copy(
            title = encryptedTitle,
            content = encryptedContent,
            iv = combinedIv,
            isEncrypted = true
        )
    }

    fun decryptNoteVersion(version: NoteVersion, isLocked: Boolean): NoteVersion {
        if (!version.isEncrypted || version.iv == null) return version

        return try {
            val ivs = version.iv.split(":")
            
            val (decryptedTitle, decryptedContent) = if (ivs.size == 2) {
                val ivTitle = Base64.decode(ivs[0], Base64.DEFAULT)
                val ivContent = Base64.decode(ivs[1], Base64.DEFAULT)
                
                val cipherTitle = getDecryptionCipher(ivTitle, isLocked)
                val title = String(cipherTitle.doFinal(Base64.decode(version.title, Base64.DEFAULT)))
                
                val cipherContent = getDecryptionCipher(ivContent, isLocked)
                val content = String(cipherContent.doFinal(Base64.decode(version.content, Base64.DEFAULT)))
                
                Pair(title, content)
            } else {
                val iv = Base64.decode(version.iv, Base64.DEFAULT)
                val cipher = getDecryptionCipher(iv, isLocked)
                val title = String(cipher.doFinal(Base64.decode(version.title, Base64.DEFAULT)))
                
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(if (isLocked) AUTH_KEY_ALIAS else KEY_ALIAS, isLocked), GCMParameterSpec(128, iv))
                val content = String(cipher.doFinal(Base64.decode(version.content, Base64.DEFAULT)))
                Pair(title, content)
            }

            version.copy(
                title = decryptedTitle,
                content = decryptedContent,
                iv = null,
                isEncrypted = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            version.copy(
                title = if (version.title.length > 20) "⚠ Decryption Failed" else version.title,
                content = "Unable to decrypt this version.",
                isEncrypted = true
            )
        }
    }
}
