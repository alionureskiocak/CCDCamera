package com.example.ccdcamera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import java.io.File
import java.io.OutputStream
fun takePhotoWithCcdPreview(
    context: Context,
    imageCapture: ImageCapture,
    showDate: Boolean,
    onResult: (Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    val tempFile = File(context.cacheDir, "raw_${System.currentTimeMillis()}.jpg")

    imageCapture.takePicture(
        ImageCapture.OutputFileOptions.Builder(tempFile).build(),
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val bitmap = CcdImageProcessor.process(
                        path = tempFile.absolutePath,
                        drawDate = showDate
                    )
                    tempFile.delete()
                    onResult(bitmap)
                } catch (e: Exception) {
                    tempFile.delete()
                    onError(e.message ?: "CCD işlem hatası")
                }
            }

            override fun onError(exc: ImageCaptureException) {
                tempFile.delete()
                onError(exc.message ?: "Kamera hatası")
            }
        }
    )
}

internal fun saveToGallery(
    context: Context,
    bitmap: Bitmap
): String {
    val resolver = context.contentResolver

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "CCD_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CCD Camera")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri = resolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values
    ) ?: throw IllegalStateException("MediaStore insert failed")

    resolver.openOutputStream(uri).use { out: OutputStream? ->
        if (out == null) throw IllegalStateException("OutputStream null")
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }

    if (Build.VERSION.SDK_INT >= 29) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    return uri.toString()
}
