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

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        
        val iv = Base64.encodeToString(cipher.iv, Base64.DEFAULT)
        val encryptedTitle = Base64.encodeToString(cipher.doFinal(note.title.toByteArray()), Base64.DEFAULT)
        val encryptedContent = Base64.encodeToString(cipher.doFinal(note.content.toByteArray()), Base64.DEFAULT)

        return note.copy(
            title = encryptedTitle,
            content = encryptedContent,
            iv = iv,
            isEncrypted = true
        )
    }

    fun decryptNote(note: Note): Note {
        if (!note.isEncrypted || note.iv == null) return note

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, Base64.decode(note.iv, Base64.DEFAULT))
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        val decryptedTitle = String(cipher.doFinal(Base64.decode(note.title, Base64.DEFAULT)))
        val decryptedContent = String(cipher.doFinal(Base64.decode(note.content, Base64.DEFAULT)))

        return note.copy(
            title = decryptedTitle,
            content = decryptedContent,
            iv = null,
            isEncrypted = false
        )
    }

    fun encryptNoteVersion(version: NoteVersion): NoteVersion {
        if (version.isEncrypted) return version

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

        val iv = Base64.encodeToString(cipher.iv, Base64.DEFAULT)
        val encryptedTitle = Base64.encodeToString(cipher.doFinal(version.title.toByteArray()), Base64.DEFAULT)
        val encryptedContent = Base64.encodeToString(cipher.doFinal(version.content.toByteArray()), Base64.DEFAULT)

        return version.copy(
            title = encryptedTitle,
            content = encryptedContent,
            iv = iv,
            isEncrypted = true
        )
    }

    fun decryptNoteVersion(version: NoteVersion): NoteVersion {
        if (!version.isEncrypted || version.iv == null) return version

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, Base64.decode(version.iv, Base64.DEFAULT))
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        val decryptedTitle = String(cipher.doFinal(Base64.decode(version.title, Base64.DEFAULT)))
        val decryptedContent = String(cipher.doFinal(Base64.decode(version.content, Base64.DEFAULT)))

        return version.copy(
            title = decryptedTitle,
            content = decryptedContent,
            iv = null,
            isEncrypted = false
        )
    }
}
