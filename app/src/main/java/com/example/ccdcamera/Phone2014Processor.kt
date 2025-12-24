package com.example.ccdcamera

import android.graphics.*
import android.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

object Phone2014Processor {

    // ---- 2014 "telefon işlemesi" karakteri (buradaki sayılar tadı belirler) ----
    private const val NR_MIX = 0.62f          // Noise reduction karışımı (0..1)
    private const val SHARP_AMOUNT = 1.75f    // Unsharp miktarı (halo için yüksek)
    private const val SHARP_THRESHOLD = 10    // Düz bölgede halo azalsın
    private const val CONTRAST = 1.18f        // Kontrast (2014 daha sert)
    private const val BRIGHTNESS = -6f        // Bir tık karart (crushed blacks hissi)
    private const val SATURATION = 1.18f      // Plastik doygunluk
    private const val JPEG_QUALITY = 74       // “Telefon jpeg” hissi için 65–80 arası güzel

    /**
     * @param drawDate foto üstüne tarih basılsın mı
     */
    fun process(path: String, drawDate: Boolean): Bitmap {
        val raw = BitmapFactory.decodeFile(path) ?: error("Bitmap decode edilemedi")

        val rotated = rotateAccordingToExif(raw, path)
        if (rotated != raw) raw.recycle()

        // 1) NR (detay öldürme / plastik)
        val nr = noiseReductionSmear(rotated)

        // 2) Halo sharpen (ringing)
        val sharp = unsharpMaskHalo(nr)

        // 3) Ton eğrisi + kontrast + doygunluk (2014 tadı)
        val graded = toneAndColor(sharp)

        // 4) JPEG artefaktı (en kritik “telefon” hissi)
        val jpegd = jpegRoundTrip(graded, JPEG_QUALITY)

        // 5) Tarih (opsiyonel)
        if (drawDate) drawDateStamp(jpegd)

        // cleanup (aynı objeyi döndürmediğimiz için)
        if (nr != rotated) rotated.recycle()
        if (sharp != nr) nr.recycle()
        if (graded != sharp) sharp.recycle()

        return jpegd
    }

    // ---------------- ORIENTATION ----------------

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

    // ---------------- 1) NOISE REDUCTION SMEAR ----------------
    // 2014 telefonlar "chroma noise yok ama doku da yok" -> downscale/upscale + blend

    private fun noiseReductionSmear(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height

        // ağır blur benzeri: küçük yap büyüt
        val small = Bitmap.createScaledBitmap(src, (w * 0.45f).toInt(), (h * 0.45f).toInt(), true)
        val blur = Bitmap.createScaledBitmap(small, w, h, true)
        small.recycle()

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // Orijinal + "blur" karışımı: NR hissi
        canvas.drawBitmap(src, 0f, 0f, null)
        canvas.drawBitmap(blur, 0f, 0f, Paint().apply { alpha = (NR_MIX * 255).toInt() })

        blur.recycle()
        return out
    }

    // ---------------- 2) UNSHARP MASK (HALO) ----------------

    private fun unsharpMaskHalo(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height

        // blur görüntü (high-pass için)
        val small = Bitmap.createScaledBitmap(src, (w * 0.70f).toInt(), (h * 0.70f).toInt(), true)
        val blur = Bitmap.createScaledBitmap(small, w, h, true)
        small.recycle()

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        val origPixels = IntArray(w * h)
        val blurPixels = IntArray(w * h)
        src.getPixels(origPixels, 0, w, 0, 0, w, h)
        blur.getPixels(blurPixels, 0, w, 0, 0, w, h)
        blur.recycle()

        for (i in origPixels.indices) {
            val o = origPixels[i]
            val b = blurPixels[i]

            val or = (o shr 16) and 0xFF
            val og = (o shr 8) and 0xFF
            val ob = o and 0xFF

            val br = (b shr 16) and 0xFF
            val bg = (b shr 8) and 0xFF
            val bb = b and 0xFF

            // fark (edge strength)
            val dr = or - br
            val dg = og - bg
            val db = ob - bb

            // düz bölgede sharpen azalt (yoksa “aşırı modern” değil “aşırı çöp” oluyor)
            val edge = max(max(abs(dr), abs(dg)), abs(db))
            val amount = if (edge < SHARP_THRESHOLD) SHARP_AMOUNT * 0.55f else SHARP_AMOUNT

            val nr = clamp(or + (dr * amount).toInt())
            val ng = clamp(og + (dg * amount).toInt())
            val nb = clamp(ob + (db * amount).toInt())

            origPixels[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
        }

        out.setPixels(origPixels, 0, w, 0, 0, w, h)
        return out
    }

    // ---------------- 3) TONE + COLOR (2014) ----------------

    private fun toneAndColor(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)

        // basit saturate (HSV üzerinden çok ağır olur; burada hızlı approx)
        // - kontrast + brightness
        // - doygunluğu “plastik”e çek
        for (i in px.indices) {
            val c = px[i]
            var r = (c shr 16) and 0xFF
            var g = (c shr 8) and 0xFF
            var b = c and 0xFF

            // contrast/brightness (crushed blacks hissi)
            r = clamp(((r - 128) * CONTRAST + 128 + BRIGHTNESS).roundToInt())
            g = clamp(((g - 128) * CONTRAST + 128 + BRIGHTNESS).roundToInt())
            b = clamp(((b - 128) * CONTRAST + 128 + BRIGHTNESS).roundToInt())

            // saturation approx: luminance'e göre uzaklaştır
            val y = (0.299f * r + 0.587f * g + 0.114f * b)
            r = clamp((y + (r - y) * SATURATION).roundToInt())
            g = clamp((y + (g - y) * SATURATION).roundToInt())
            b = clamp((y + (b - y) * SATURATION).roundToInt())

            // 2014 telefonlarda mavi/yeşil biraz “pop” (çok az)
            g = clamp((g * 1.02f).roundToInt())
            b = clamp((b * 1.03f).roundToInt())

            px[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    // ---------------- 4) JPEG ROUND TRIP (artefakt) ----------------

    private fun jpegRoundTrip(src: Bitmap, quality: Int): Bitmap {
        val baos = ByteArrayOutputStream()
        src.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), baos)
        val bytes = baos.toByteArray()

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return src

        // decode edilen bitmap genelde immutable olabilir; güvene al:
        return decoded.copy(Bitmap.Config.ARGB_8888, true).also {
            decoded.recycle()
        }
    }

    // ---------------- DATE STAMP (opsiyonel) ----------------

    private fun drawDateStamp(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(235, 255, 210, 0)
            textSize = bitmap.width * 0.048f
            typeface = Typeface.MONOSPACE
            setShadowLayer(3f, 2f, 2f, Color.BLACK)
        }

        val pad = bitmap.width * 0.04f
        canvas.drawText(
            date,
            bitmap.width - pad - paint.measureText(date),
            bitmap.height - pad,
            paint
        )
    }

    private fun clamp(v: Int): Int = v.coerceIn(0, 255)
}
