package com.example.ccdcamera

import android.graphics.*
import kotlin.math.min
import kotlin.random.Random

/**
 * CCD hissi için:
 * - sıcak ton + hafif channel imbalance
 * - düşük kontrast / highlight clipping (patlayan beyaz)
 * - dijital noise (film grain değil)
 * - hafif yumuşatma (cheap lens)
 */
fun applyCcdEffect(bitmap: Bitmap): Bitmap {
    val w = bitmap.width
    val h = bitmap.height

    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Renk tonunu CCD gibi sıcaklaştır
    val colorMatrix = ColorMatrix().apply {
        set(floatArrayOf(
            1.15f, 0f,    0f,    0f, 12f,
            0f,    1.08f, 0f,    0f, 6f,
            0f,    0f,    0.95f, 0f, -2f,
            0f,    0f,    0f,    1f, 0f
        ))
    }
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)

    // Highlights biraz patlamalı ama doğal
    val clipPaint = Paint()
    clipPaint.color = Color.WHITE
    clipPaint.alpha = 12
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), clipPaint)

    // Sıcak kontrast / soft midtone için blend
    val overlay = Paint()
    overlay.colorFilter = PorterDuffColorFilter(Color.argb(30, 255, 240, 200), PorterDuff.Mode.OVERLAY)
    canvas.drawBitmap(result, 0f, 0f, overlay)

    // Çok hafif “CCD digitalesque” noise
    val noisePaint = Paint()
    repeat((w * h / 1000).coerceAtMost(15000)) {
        val x = Random.nextInt(w)
        val y = Random.nextInt(h)
        val alpha = Random.nextInt(8, 18)
        noisePaint.color = Color.argb(alpha, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
        canvas.drawPoint(x.toFloat(), y.toFloat(), noisePaint)
    }

    return result
}
