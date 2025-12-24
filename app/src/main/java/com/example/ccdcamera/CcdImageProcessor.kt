package com.example.ccdcamera

import android.graphics.*
import android.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random

object CcdRetroProcessor {

    /**
     * @param drawDate Tarih damgası açılsın mı
     */
    fun process(
        path: String,
        drawDate: Boolean
    ): Bitmap {
        val raw = BitmapFactory.decodeFile(path)
            ?: error("Bitmap decode edilemedi")

        val rotated = rotateAccordingToExif(raw, path)
        if (rotated != raw) raw.recycle()

        val out = applyDigicamLook(rotated)

        if (drawDate) drawDateStamp(out)

        return out
    }

    // --------------------------------------------------
    // ORIENTATION (DİKEY / YATAY TAM DOĞRU)
    // --------------------------------------------------

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
            bitmap,
            0, 0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    // --------------------------------------------------
    // DIGICAM / CCD LOOK (RETRO AMA TEMİZ)
    // --------------------------------------------------

    private fun applyDigicamLook(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // 1️⃣ Renk karakteri (sıcak, pastel, flat)
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        basePaint.colorFilter = ColorMatrixColorFilter(
            ColorMatrix(floatArrayOf(
                1.07f, 0.02f, 0f,    0f, 12f,
                0f,    1.03f, 0f,    0f, 7f,
                0f,    0.02f, 0.90f, 0f, -4f,
                0f,    0f,    0f,    1f, 0f
            ))
        )
        canvas.drawBitmap(src, 0f, 0f, basePaint)

        // 2️⃣ Hafif softness (cheap lens hissi)
        val down = Bitmap.createScaledBitmap(
            out,
            (w * 0.965f).toInt(),
            (h * 0.965f).toInt(),
            true
        )
        val soft = Bitmap.createScaledBitmap(down, w, h, true)
        canvas.drawBitmap(soft, 0f, 0f, Paint().apply { alpha = 115 })
        down.recycle()
        soft.recycle()

        // 3️⃣ Çok hafif RGB channel misalignment
        val shiftPaint = Paint().apply { alpha = 35 }
        canvas.drawBitmap(out, 0.6f, 0f, shiftPaint)
        canvas.drawBitmap(out, -0.6f, 0f, shiftPaint)

        // 4️⃣ Vinyet (çok kontrollü)
        drawVignette(canvas, w, h)

        // 5️⃣ Dijital CCD noise (film grain DEĞİL)
        drawDigitalNoise(canvas, w, h)

        // 6️⃣ Hafif satır hissi (sensör readout)
        drawScanLines(canvas, w, h)

        // 7️⃣ Highlight compression (eski DR hissi)
        canvas.drawRect(
            0f, 0f, w.toFloat(), h.toFloat(),
            Paint().apply {
                color = Color.WHITE
                alpha = 10
            }
        )

        return out
    }

    // --------------------------------------------------
    // EFFECT HELPERS (TEMİZ)
    // --------------------------------------------------

    private fun drawVignette(canvas: Canvas, w: Int, h: Int) {
        val cx = w / 2f
        val cy = h / 2f
        val maxDist = sqrt(cx * cx + cy * cy)
        val paint = Paint()

        for (y in 0 until h step 6) {
            for (x in 0 until w step 6) {
                val dx = x - cx
                val dy = y - cy
                val dist = sqrt(dx * dx + dy * dy)
                val factor = (dist / maxDist).coerceIn(0f, 1f)
                val alpha = (factor * 70).toInt()
                paint.color = Color.argb(alpha, 0, 0, 0)
                canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
            }
        }
    }

    private fun drawDigitalNoise(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint()
        val count = min(16000, (w * h) / 32)

        repeat(count) {
            val x = Random.nextInt(w)
            val y = Random.nextInt(h)

            val v = if (Random.nextBoolean()) 255 else 0
            paint.color = Color.rgb(v, v, v)
            paint.alpha = 11

            canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
        }
    }

    private fun drawScanLines(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint()
        for (y in 0 until h step 9) {
            paint.color = Color.WHITE
            paint.alpha = Random.nextInt(3, 7)
            canvas.drawLine(0f, y.toFloat(), w.toFloat(), y.toFloat(), paint)
        }
    }

    // --------------------------------------------------
    // DATE STAMP (DIGICAM TARZI)
    // --------------------------------------------------

    private fun drawDateStamp(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            .format(Date())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 255, 200, 0)
            textSize = bitmap.width * 0.048f
            typeface = Typeface.MONOSPACE
            setShadowLayer(3f, 2f, 2f, Color.BLACK)
        }

        val paddingX = bitmap.width * 0.04f
        val paddingY = bitmap.height * 0.065f  // 🔑 yukarı alıyoruz

        canvas.drawText(
            date,
            bitmap.width - paddingX - paint.measureText(date),
            bitmap.height - paddingY,
            paint
        )

    }
}
