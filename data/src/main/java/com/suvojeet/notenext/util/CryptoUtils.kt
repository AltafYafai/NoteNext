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
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return keyGenerator.generateKey()
    }

    fun encryptNote(note: Note): Note {
        if (note.isEncrypted) return note

        val secretKey = getSecretKey()
        
        // Encrypt title
        val cipherTitle = Cipher.getInstance(TRANSFORMATION)
        cipherTitle.init(Cipher.ENCRYPT_MODE, secretKey)
        val ivTitle = cipherTitle.iv
        val encryptedTitle = Base64.encodeToString(cipherTitle.doFinal(note.title.toByteArray()), Base64.DEFAULT)

        // Encrypt content
        val cipherContent = Cipher.getInstance(TRANSFORMATION)
        cipherContent.init(Cipher.ENCRYPT_MODE, secretKey)
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

    fun decryptNote(note: Note): Note {
        if (!note.isEncrypted || note.iv == null) return note

        return try {
            val ivs = note.iv.split(":")
            val secretKey = getSecretKey()
            
            val (decryptedTitle, decryptedContent) = if (ivs.size == 2) {
                // New format: separate IVs for title and content
                val ivTitle = Base64.decode(ivs[0], Base64.DEFAULT)
                val ivContent = Base64.decode(ivs[1], Base64.DEFAULT)
                
                val cipherTitle = Cipher.getInstance(TRANSFORMATION)
                cipherTitle.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, ivTitle))
                val title = String(cipherTitle.doFinal(Base64.decode(note.title, Base64.DEFAULT)))
                
                val cipherContent = Cipher.getInstance(TRANSFORMATION)
                cipherContent.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, ivContent))
                val content = String(cipherContent.doFinal(Base64.decode(note.content, Base64.DEFAULT)))
                
                Pair(title, content)
            } else {
                // Old (broken) format: same IV for both (likely to fail/crash)
                val iv = Base64.decode(note.iv, Base64.DEFAULT)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                val title = String(cipher.doFinal(Base64.decode(note.title, Base64.DEFAULT)))
                
                // We MUST re-init the cipher here if we want to try decrypting the content with the same IV
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
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
            // Return note with a clear error so the user sees the problem
            note.copy(
                title = if (note.title.length > 20) "⚠ Decryption Failed" else note.title,
                content = "Unable to decrypt this note. The encryption key may have changed (e.g., after a device reset). Original encrypted data is preserved.",
                isEncrypted = true
            )
        }
    }

    fun encryptNoteVersion(version: NoteVersion): NoteVersion {
        if (version.isEncrypted) return version

        val secretKey = getSecretKey()
        
        // Encrypt title
        val cipherTitle = Cipher.getInstance(TRANSFORMATION)
        cipherTitle.init(Cipher.ENCRYPT_MODE, secretKey)
        val ivTitle = cipherTitle.iv
        val encryptedTitle = Base64.encodeToString(cipherTitle.doFinal(version.title.toByteArray()), Base64.DEFAULT)

        // Encrypt content
        val cipherContent = Cipher.getInstance(TRANSFORMATION)
        cipherContent.init(Cipher.ENCRYPT_MODE, secretKey)
        val ivContent = cipherContent.iv
        val encryptedContent = Base64.encodeToString(cipherContent.doFinal(version.content.toByteArray()), Base64.DEFAULT)

        // Combine IVs: ivTitle:ivContent
        val combinedIv = Base64.encodeToString(ivTitle, Base64.DEFAULT) + ":" + Base64.encodeToString(ivContent, Base64.DEFAULT)

        return version.copy(
            title = encryptedTitle,
            content = encryptedContent,
            iv = combinedIv,
            isEncrypted = true
        )
    }

    fun decryptNoteVersion(version: NoteVersion): NoteVersion {
        if (!version.isEncrypted || version.iv == null) return version

        return try {
            val ivs = version.iv.split(":")
            val secretKey = getSecretKey()
            
            val (decryptedTitle, decryptedContent) = if (ivs.size == 2) {
                val ivTitle = Base64.decode(ivs[0], Base64.DEFAULT)
                val ivContent = Base64.decode(ivs[1], Base64.DEFAULT)
                
                val cipherTitle = Cipher.getInstance(TRANSFORMATION)
                cipherTitle.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, ivTitle))
                val title = String(cipherTitle.doFinal(Base64.decode(version.title, Base64.DEFAULT)))
                
                val cipherContent = Cipher.getInstance(TRANSFORMATION)
                cipherContent.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, ivContent))
                val content = String(cipherContent.doFinal(Base64.decode(version.content, Base64.DEFAULT)))
                
                Pair(title, content)
            } else {
                val iv = Base64.decode(version.iv, Base64.DEFAULT)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                val title = String(cipher.doFinal(Base64.decode(version.title, Base64.DEFAULT)))
                
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
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
                content = "Unable to decrypt this version. The encryption key may have changed.",
                isEncrypted = true
            )
        }
    }
}
