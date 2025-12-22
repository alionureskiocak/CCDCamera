package com.example.ccdcamera

import android.graphics.*
import android.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import kotlin.random.Random

object CcdImageProcessor {

    fun process(
        path: String,
        drawDate: Boolean
    ): Bitmap {
        val raw = BitmapFactory.decodeFile(path)
            ?: error("Bitmap decode edilemedi")

        val rotated = rotateAccordingToExif(raw, path)
        if (rotated != raw) raw.recycle()

        val ccd = applyCcdEffect(rotated)

        if (drawDate) drawDateStamp(ccd)

        return ccd
    }

    // orientation
    private fun rotateAccordingToExif(bitmap: Bitmap, path: String): Bitmap {
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val angle = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        if (angle == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(angle) }
        return Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height,
            matrix, true
        )
    }

    // CCD look
    private fun applyCcdEffect(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(
            ColorMatrix(floatArrayOf(
                1.12f, 0.03f, 0f, 0f, 10f,
                0f, 1.05f, 0f, 0f, 6f,
                0f, 0.02f, 0.92f, 0f, -2f,
                0f, 0f, 0f, 1f, 0f
            ))
        )
        canvas.drawBitmap(src, 0f, 0f, paint)

        // highlight softness
        canvas.drawRect(
            0f, 0f, w.toFloat(), h.toFloat(),
            Paint().apply {
                color = Color.WHITE
                alpha = 14
            }
        )

        // digital CCD noise
        val noisePaint = Paint()
        val count = min(14000, (w * h) / 35)

        repeat(count) {
            val x = Random.nextInt(w)
            val y = Random.nextInt(h)
            val v = if (Random.nextBoolean()) 255 else 0
            noisePaint.color = Color.rgb(v, v, v)
            noisePaint.alpha = 12
            canvas.drawPoint(x.toFloat(), y.toFloat(), noisePaint)
        }

        return out
    }

    // date
    private fun drawDateStamp(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)

        val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            .format(Date())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 255, 200, 0)
            textSize = bitmap.width * 0.045f
            typeface = Typeface.MONOSPACE
            setShadowLayer(4f, 3f, 3f, Color.BLACK)
        }

        val padding = bitmap.width * 0.04f
        canvas.drawText(
            date,
            bitmap.width - padding - paint.measureText(date),
            bitmap.height - padding,
            paint
        )
    }
}
