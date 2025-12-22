package com.example.ccdcamera

import android.annotation.SuppressLint
import android.content.Context
import android.util.Rational
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

@SuppressLint("RestrictedApi")
@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    lensFacing: Int,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val previewView = PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            bindCamera(
                context = context,
                previewView = previewView,
                imageCapture = imageCapture,
                lensFacing = lensFacing
            )

            previewView
        },
        update = { previewView ->
            bindCamera(
                context = previewView.context,
                previewView = previewView,
                imageCapture = imageCapture,
                lensFacing = lensFacing
            )
        }
    )
}
private fun bindCamera(
    context: Context,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    lensFacing: Int
) {
    val providerFuture = ProcessCameraProvider.getInstance(context)

    providerFuture.addListener({
        val provider = providerFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // 🔑 KRİTİK KISIM
        val viewPort = ViewPort.Builder(
            Rational(previewView.width, previewView.height),
            previewView.display.rotation
        ).build()

        val useCaseGroup = UseCaseGroup.Builder()
            .setViewPort(viewPort)
            .addUseCase(preview)
            .addUseCase(imageCapture)
            .build()

        provider.unbindAll()
        provider.bindToLifecycle(
            context as LifecycleOwner,
            selector,
            useCaseGroup
        )

    }, ContextCompat.getMainExecutor(context))
}
