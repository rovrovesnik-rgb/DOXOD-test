package com.doxod.app.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayOutputStream

/** Обработка изображений: сэмплирование, EXIF-поворот, масштабирование. */
object ImageUtils {

    const val MAX_SIDE = 1280
    const val THUMB_SIDE = 240
    const val QUALITY_MAIN = 72
    const val QUALITY_THUMB = 60

    /** Прочитать байты, декодировать с inSampleSize (половина стороны ещё >= maxSide), применить EXIF. */
    fun decodeSampled(bytes: ByteArray, maxSide: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        var sample = 1
        // пока половина стороны ещё >= maxSide — увеличиваем степень двойки
        while (boundsOpts.outWidth / (sample * 2) >= maxSide || boundsOpts.outHeight / (sample * 2) >= maxSide) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
        val oriented = applyExif(decoded, bytes, sample)
        if (oriented !== decoded) decoded.recycle()
        return oriented
    }

    private fun applyExif(bitmap: Bitmap, bytes: ByteArray, sample: Int): Bitmap {
        val orientation = try {
            ExifInterface(java.io.ByteArrayInputStream(bytes)).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.preScale(-1f, 1f); matrix.postRotate(90f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.preScale(-1f, 1f); matrix.postRotate(-90f) }
            else -> return bitmap
        }
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: OutOfMemoryError) {
            bitmap
        }
    }

    /** Масштабировать так, чтобы максимальная сторона стала <= maxSide. */
    fun scaleToMaxSide(src: Bitmap, maxSide: Int): Bitmap {
        val side = maxOf(src.width, src.height)
        if (side <= maxSide) return src
        val ratio = maxSide.toFloat() / side
        val scaled = Bitmap.createScaledBitmap(
            src, (src.width * ratio).toInt().coerceAtLeast(1),
            (src.height * ratio).toInt().coerceAtLeast(1), true
        )
        if (scaled !== src) src.recycle()
        return scaled
    }

    fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    /**
     * Полный цикл для одного входного файла:
     * returns Pair(mainBytes, thumbBytes) или null, если не удалось.
     * Битмапы recycle()-ятся.
     */
    fun processPhoto(bytes: ByteArray): Pair<ByteArray, ByteArray>? {
        val full = decodeSampled(bytes, MAX_SIDE) ?: return null
        try {
            val main = scaleToMaxSide(full, MAX_SIDE)
            val mainBytes = encodeJpeg(main, QUALITY_MAIN)
            if (main !== full) main.recycle()

            val forThumb = decodeSampled(bytes, THUMB_SIDE) ?: return null
            return try {
                val thumb = scaleToMaxSide(forThumb, THUMB_SIDE)
                val thumbBytes = encodeJpeg(thumb, QUALITY_THUMB)
                if (thumb !== forThumb) thumb.recycle()
                Pair(mainBytes, thumbBytes)
            } finally {
                if (!forThumb.isRecycled) forThumb.recycle()
            }
        } finally {
            if (!full.isRecycled) full.recycle()
        }
    }

    /** Декодировать миниатюру/blob в битмап (для показа). */
    fun decodeBitmap(bytes: ByteArray): Bitmap? =
        try { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) } catch (e: Exception) { null }

    /** Построить миниатюру из готового основного blob (для импорта бэкапа). */
    fun thumbFromMain(mainBytes: ByteArray): ByteArray? {
        val bmp = decodeSampled(mainBytes, THUMB_SIDE) ?: return null
        return try {
            val thumb = scaleToMaxSide(bmp, THUMB_SIDE)
            val b = encodeJpeg(thumb, QUALITY_THUMB)
            if (thumb !== bmp) thumb.recycle()
            b
        } finally {
            if (!bmp.isRecycled) bmp.recycle()
        }
    }
}
