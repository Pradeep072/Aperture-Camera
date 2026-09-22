package com.aperture.camera.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.pdf.PdfDocument
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class DocumentEnhancementFilter(val title: String) {
    MAGIC_COLOR("Magic Color"),
    BLACK_AND_WHITE("B & W"),
    GRAYSCALE("Grayscale"),
    ORIGINAL("Original")
}

data class ScannedDocumentPage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val originalBitmap: Bitmap,
    val corners: List<PointF>,
    val enhancement: DocumentEnhancementFilter = DocumentEnhancementFilter.MAGIC_COLOR,
    val rotationDegrees: Int = 0
)

object DocumentScannerHelper {

    private const val TAG = "DocumentScannerHelper"

    /**
     * Automatically detects 4 quad corners (TL, TR, BR, BL) on the bitmap.
     * Falls back to a smart 5% margin rectangle if no dominant polygon is identified.
     */
    fun detectDocumentCorners(bitmap: Bitmap): List<PointF> {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        try {
            // Downscale for fast corner boundary scanning
            val scale = 400f / max(w, h)
            val smallW = (w * scale).toInt().coerceAtLeast(50)
            val smallH = (h * scale).toInt().coerceAtLeast(50)
            val smallBitmap = Bitmap.createScaledBitmap(bitmap, smallW, smallH, false)

            val pixels = IntArray(smallW * smallH)
            smallBitmap.getPixels(pixels, 0, smallW, 0, 0, smallW, smallH)
            smallBitmap.recycle()

            // Calculate luminance variance along boundaries
            var minX = smallW
            var maxX = 0
            var minY = smallH
            var maxY = 0

            var totalLum = 0L
            val lums = IntArray(pixels.size)
            for (i in pixels.indices) {
                val c = pixels[i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val lum = (r * 299 + g * 587 + b * 114) / 1000
                lums[i] = lum
                totalLum += lum
            }
            val avgLum = (totalLum / pixels.size).toInt()
            val threshold = (avgLum * 0.85).toInt()

            for (y in 0 until smallH) {
                for (x in 0 until smallW) {
                    val lum = lums[y * smallW + x]
                    if (lum < threshold) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }

            val marginW = smallW * 0.05f
            val marginH = smallH * 0.05f

            if (maxX - minX > smallW * 0.35f && maxY - minY > smallH * 0.35f) {
                val invScale = 1f / scale
                val left = (minX * invScale).coerceIn(0f, w * 0.15f)
                val right = (maxX * invScale).coerceIn(w * 0.85f, w)
                val top = (minY * invScale).coerceIn(0f, h * 0.15f)
                val bottom = (maxY * invScale).coerceIn(h * 0.85f, h)

                return listOf(
                    PointF(left, top),          // Top-Left
                    PointF(right, top),         // Top-Right
                    PointF(right, bottom),      // Bottom-Right
                    PointF(left, bottom)        // Bottom-Left
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Corner detection fallback: ${e.message}")
        }

        // Default smart 8% margin
        val padX = w * 0.08f
        val padY = h * 0.08f
        return listOf(
            PointF(padX, padY),
            PointF(w - padX, padY),
            PointF(w - padX, h - padY),
            PointF(padX, h - padY)
        )
    }

    /**
     * Warps and crops the given quadrilateral corners into a straight flat rectangular document bitmap.
     */
    suspend fun cropAndWarpDocument(
        bitmap: Bitmap,
        corners: List<PointF>
    ): Bitmap = withContext(Dispatchers.Default) {
        if (corners.size != 4) return@withContext bitmap

        val tl = corners[0]
        val tr = corners[1]
        val br = corners[2]
        val bl = corners[3]

        // Compute width & height of target warped rectangle
        val topWidth = hypot((tr.x - tl.x).toDouble(), (tr.y - tl.y).toDouble()).toFloat()
        val bottomWidth = hypot((br.x - bl.x).toDouble(), (br.y - bl.y).toDouble()).toFloat()
        val targetWidth = max(topWidth, bottomWidth).toInt().coerceAtLeast(100)

        val leftHeight = hypot((bl.x - tl.x).toDouble(), (bl.y - tl.y).toDouble()).toFloat()
        val rightHeight = hypot((br.x - tr.x).toDouble(), (br.y - tr.y).toDouble()).toFloat()
        val targetHeight = max(leftHeight, rightHeight).toInt().coerceAtLeast(100)

        val srcPoints = floatArrayOf(
            tl.x, tl.y,
            tr.x, tr.y,
            br.x, br.y,
            bl.x, bl.y
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat()
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        canvas.drawBitmap(bitmap, matrix, paint)
        output
    }

    /**
     * Applies document tone enhancements (Magic Color, High-Contrast B&W, Grayscale, or Original).
     */
    suspend fun applyEnhancement(
        bitmap: Bitmap,
        filter: DocumentEnhancementFilter
    ): Bitmap = withContext(Dispatchers.Default) {
        when (filter) {
            DocumentEnhancementFilter.ORIGINAL -> bitmap

            DocumentEnhancementFilter.GRAYSCALE -> {
                val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                val cm = ColorMatrix()
                cm.setSaturation(0f)
                // Boost contrast slightly
                val scale = 1.25f
                val translate = (-0.5f * scale + 0.5f) * 255f
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                contrastMatrix.preConcat(cm)
                paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                output
            }

            DocumentEnhancementFilter.MAGIC_COLOR -> {
                // Enhances paper brightness, removes yellow/shadow cast, boosts ink color
                val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                // High contrast + bright paper white stretch + vibrant ink
                val contrast = 1.35f
                val brightness = 20f
                val magicMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, brightness,
                        0f, contrast, 0f, 0f, brightness,
                        0f, 0f, contrast, 0f, brightness,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                val satMatrix = ColorMatrix()
                satMatrix.setSaturation(1.15f)
                magicMatrix.preConcat(satMatrix)

                paint.colorFilter = ColorMatrixColorFilter(magicMatrix)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                output
            }

            DocumentEnhancementFilter.BLACK_AND_WHITE -> {
                // Adaptive contrast binarization for clean crisp text
                val w = bitmap.width
                val h = bitmap.height
                val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val pixels = IntArray(w * h)
                bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

                // Quick two-pass luminance thresholding
                for (i in pixels.indices) {
                    val c = pixels[i]
                    val r = (c shr 16) and 0xFF
                    val g = (c shr 8) and 0xFF
                    val b = c and 0xFF
                    val lum = (r * 77 + g * 150 + b * 29) shr 8

                    // High threshold stretch to turn grey paper pure white while text remains pure dark black
                    val mapped = when {
                        lum > 145 -> 0xFFFFFFFF.toInt()
                        lum < 95 -> 0xFF000000.toInt()
                        else -> {
                            val factor = (lum - 95) / 50f
                            val v = (factor * 255).toInt()
                            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
                        }
                    }
                    pixels[i] = mapped
                }

                output.setPixels(pixels, 0, w, 0, 0, w, h)
                output
            }
        }
    }

    /**
     * Rotates a bitmap by 90-degree increments.
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Converts a list of processed document page Bitmaps into a compact, lightweight multi-page PDF document.
     */
    suspend fun generatePdf(bitmaps: List<Bitmap>): ByteArray = withContext(Dispatchers.Default) {
        val pdfDocument = PdfDocument()
        val out = ByteArrayOutputStream()

        try {
            for ((index, originalBmp) in bitmaps.withIndex()) {
                // Downscale for compact PDF size (max 1500 x 2000 for 200 DPI crisp document rendering)
                val maxDim = 1500f
                val origW = originalBmp.width.toFloat()
                val origH = originalBmp.height.toFloat()
                val scale = if (max(origW, origH) > maxDim) maxDim / max(origW, origH) else 1.0f

                val scaledW = (origW * scale).toInt().coerceAtLeast(100)
                val scaledH = (origH * scale).toInt().coerceAtLeast(100)
                val scaledBmp = Bitmap.createScaledBitmap(originalBmp, scaledW, scaledH, true)

                // Compress to JPEG byte stream to drastically reduce PDF payload
                val jpegStream = ByteArrayOutputStream()
                scaledBmp.compress(Bitmap.CompressFormat.JPEG, 82, jpegStream)
                val jpegBytes = jpegStream.toByteArray()
                val compressedBmp = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: scaledBmp

                // Standard A4 aspect ratio reference: 595 x 842 pt
                val pageW = 595
                val pageH = (pageW * (origH / origW)).toInt().coerceIn(400, 1200)

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val scaleMatrix = Matrix()
                scaleMatrix.setScale(pageW.toFloat() / compressedBmp.width.toFloat(), pageH.toFloat() / compressedBmp.height.toFloat())

                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(compressedBmp, scaleMatrix, paint)

                pdfDocument.finishPage(page)

                if (scaledBmp != originalBmp) scaledBmp.recycle()
                if (compressedBmp != scaledBmp && compressedBmp != originalBmp) compressedBmp.recycle()
            }

            pdfDocument.writeTo(out)
        } finally {
            pdfDocument.close()
        }

        out.toByteArray()
    }

    /**
     * Compresses bitmap to high-quality JPEG byte array.
     */
    fun compressToJpeg(bitmap: Bitmap, quality: Int = 95): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
}
