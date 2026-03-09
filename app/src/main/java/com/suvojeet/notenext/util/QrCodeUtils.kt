package com.suvojeet.notenext.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Utility class for QR code generation and decoding for note sharing.
 * Uses JSON + GZIP compression to maximize data capacity within QR code limits.
 */
object QrCodeUtils {

    private val json = Json { ignoreUnknownKeys = true }

    // Maximum characters before compression warning (QR code limit ~2KB after encoding)
    private const val MAX_CONTENT_LENGTH = 1500
    private const val QR_SIZE = 512

    /**
     * Data class representing note data for QR encoding.
     */
    @Serializable
    data class NoteQrData(
        val t: String, // title
        val c: String  // content
    )

    /**
     * Generates a branded QR code bitmap from note title and content.
     *
     * @param title Note title
     * @param content Note content
     * @param size QR code size in pixels (default 512)
     * @param logo Optional logo bitmap to draw in the center
     * @return Bitmap of the QR code, or null if generation fails
     */
    fun generateQrCode(
        title: String,
        content: String,
        size: Int = QR_SIZE,
        logo: Bitmap? = null
    ): Bitmap? {
        return try {
            val noteData = NoteQrData(t = title, c = content)
            val jsonData = json.encodeToString(noteData)
            val compressedData = compressData(jsonData)

            // Use High error correction if logo is present to ensure readability
            val errorCorrection = if (logo != null) ErrorCorrectionLevel.H else ErrorCorrectionLevel.M

            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to errorCorrection,
                EncodeHintType.MARGIN to 1, // Minimize default margin, we add our own
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(compressedData, BarcodeFormat.QR_CODE, size, size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            
            // Branding configuration
            val qrColor = 0xFF6200EE.toInt() // Primary Brand Color
            val backgroundColor = Color.WHITE
            val padding = 24
            val textHeight = 50
            
            // Calculate final bitmap dimensions (original QR + padding + text area)
            val finalWidth = width + (padding * 2)
            val finalHeight = height + (padding * 2) + textHeight

            val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            
            // Draw background
            canvas.drawColor(backgroundColor)

            // Draw QR pixels
            val pixelPaint = Paint().apply {
                color = qrColor
                style = Paint.Style.FILL
            }

            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (bitMatrix[x, y]) {
                        canvas.drawRect(
                            (x + padding).toFloat(),
                            (y + padding).toFloat(),
                            (x + padding + 1).toFloat(),
                            (y + padding + 1).toFloat(),
                            pixelPaint
                        )
                    }
                }
            }

            // Draw Logo
            if (logo != null) {
                val logoSize = size / 5 // 20% of QR size
                val scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)
                
                val logoX = (finalWidth - logoSize) / 2f
                val logoY = (height + (padding * 2) - logoSize) / 2f
                
                // Draw white background for logo (quiet zone)
                val logoBgPaint = Paint().apply {
                    color = backgroundColor
                    style = Paint.Style.FILL
                }
                val logoPadding = 8f
                val logoBgRect = RectF(
                    logoX - logoPadding,
                    logoY - logoPadding,
                    logoX + logoSize + logoPadding,
                    logoY + logoSize + logoPadding
                )
                canvas.drawRect(logoBgRect, logoBgPaint)

                // Draw logo
                canvas.drawBitmap(scaledLogo, logoX, logoY, null)
            }

            // Draw "NoteNext" text
            val textPaint = Paint().apply {
                color = qrColor
                textSize = 40f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val textX = finalWidth / 2f
            val textY = finalHeight - padding - 10f
            canvas.drawText("NoteNext", textX, textY, textPaint)

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes QR code data string back to NoteQrData.
     *
     * @param data The compressed/encoded data from QR scan
     * @return NoteQrData if successful, null otherwise
     */
    fun decodeQrData(data: String): NoteQrData? {
        return try {
            val decompressedJson = decompressData(data)
            json.decodeFromString<NoteQrData>(decompressedJson)
        } catch (e: Exception) {
            // Try parsing as plain JSON (for older/simple QR codes)
            try {
                json.decodeFromString<NoteQrData>(data)
            } catch (e2: Exception) {
                e2.printStackTrace()
                null
            }
        }
    }

    /**
     * Checks if the note content is within QR code size limits.
     *
     * @param title Note title
     * @param content Note content
     * @return true if the note can be encoded, false if it's too large
     */
    fun isWithinSizeLimit(title: String, content: String): Boolean {
        return (title.length + content.length) <= MAX_CONTENT_LENGTH
    }

    /**
     * Gets the estimated size percentage used (0-100+).
     * Values over 100 indicate the note is too large.
     */
    fun getSizePercentage(title: String, content: String): Int {
        val totalLength = title.length + content.length
        return (totalLength * 100) / MAX_CONTENT_LENGTH
    }

    /**
     * Compresses data using GZIP and encodes to Base64.
     */
    private fun compressData(data: String): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        GZIPOutputStream(byteArrayOutputStream).use { gzip ->
            gzip.write(data.toByteArray(Charsets.UTF_8))
        }
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
    }

    /**
     * Decompresses Base64-encoded GZIP data.
     */
    private fun decompressData(compressedData: String): String {
        val bytes = Base64.getDecoder().decode(compressedData)
        return GZIPInputStream(bytes.inputStream()).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
