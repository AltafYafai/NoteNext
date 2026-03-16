package com.suvojeet.notenext.data.backup

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val SALT_LENGTH_BYTE = 16
    private const val PBKDF2_ITERATIONS_V1 = 10000
    private const val PBKDF2_ITERATIONS = 600000
    private const val KEY_LENGTH_BIT = 256

    // Headers to identify encrypted files
    private const val ENCRYPTED_FILE_HEADER_V1 = "NOTENEXT_ENC_V1"
    private const val ENCRYPTED_FILE_HEADER = "NOTENEXT_ENC_V2"

    fun encryptStream(inputStream: InputStream, outputStream: OutputStream, password: String) {
        val salt = ByteArray(SALT_LENGTH_BYTE)
        SecureRandom().nextBytes(salt)

        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        // Write Header
        outputStream.write(ENCRYPTED_FILE_HEADER.toByteArray(Charsets.UTF_8))
        // Write Salt and IV
        outputStream.write(salt)
        outputStream.write(iv)

        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val encrypted = cipher.update(buffer, 0, bytesRead)
            if (encrypted != null) {
                outputStream.write(encrypted)
            }
        }
        val finalBytes = cipher.doFinal()
        if (finalBytes != null) {
            outputStream.write(finalBytes)
        }
    }

    fun encryptFile(inputFile: File, outputStream: OutputStream, password: String) {
        outputStream.use { out ->
            FileInputStream(inputFile).use { input ->
                encryptStream(input, out, password)
            }
        }
    }

    /**
     * Decrypts an encrypted stream to a target file.
     * @return true if decryption was successful, false if it failed (e.g. wrong password).
     * Throws exception for IO errors.
     */
    fun decryptFile(inputStream: InputStream, outputFile: File, password: String) {
        inputStream.use { objIn ->
            // Read Header (V1 and V2 headers are the same length)
            val headerBytes = ByteArray(ENCRYPTED_FILE_HEADER.length)
            val headerRead = objIn.read(headerBytes)
            val header = String(headerBytes, Charsets.UTF_8)

            val iterations = when {
                headerRead == ENCRYPTED_FILE_HEADER.length && header == ENCRYPTED_FILE_HEADER -> PBKDF2_ITERATIONS
                headerRead == ENCRYPTED_FILE_HEADER_V1.length && header == ENCRYPTED_FILE_HEADER_V1 -> PBKDF2_ITERATIONS_V1
                else -> throw IllegalArgumentException("Invalid file format or not an encrypted backup.")
            }

            // Read Salt and IV
            val salt = ByteArray(SALT_LENGTH_BYTE)
            if (objIn.read(salt) != SALT_LENGTH_BYTE) throw IllegalArgumentException("Corrupted file (missing salt).")

            val iv = ByteArray(IV_LENGTH_BYTE)
            if (objIn.read(iv) != IV_LENGTH_BYTE) throw IllegalArgumentException("Corrupted file (missing IV).")

            val secretKey = deriveKey(password, salt, iterations)
            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

            FileOutputStream(outputFile).use { out ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (objIn.read(buffer).also { bytesRead = it } != -1) {
                    val decrypted = cipher.update(buffer, 0, bytesRead)
                    if (decrypted != null) {
                        out.write(decrypted)
                    }
                }
                val finalBytes = cipher.doFinal()
                if (finalBytes != null) {
                    out.write(finalBytes)
                }
            }
        }
    }
    
    fun isEncrypted(inputStream: InputStream): Boolean {
        try {
            val headerBytes = ByteArray(ENCRYPTED_FILE_HEADER.length)
            val bytesRead = inputStream.read(headerBytes)
            if (bytesRead != ENCRYPTED_FILE_HEADER.length) return false

            val header = String(headerBytes, Charsets.UTF_8)
            return header == ENCRYPTED_FILE_HEADER || header == ENCRYPTED_FILE_HEADER_V1
        } catch (e: Exception) {
            return false
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int = PBKDF2_ITERATIONS): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BIT)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, KEY_ALGORITHM)
    }
}
