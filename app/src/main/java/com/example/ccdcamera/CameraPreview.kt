package com.example.ccdcamera

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

@SuppressLint("RestrictedApi")
@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    lensFacing: Int,
    modifier: Modifier = Modifier,
    onCameraReady: (Camera) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val previewView = PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER // crop yok
            }

            bindCamera(
                context = context,
                previewView = previewView,
                imageCapture = imageCapture,
                lensFacing = lensFacing,
                onCameraReady = onCameraReady
            )

            previewView
        }
    )
}

private fun bindCamera(
    context: Context,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    lensFacing: Int,
    onCameraReady: (Camera) -> Unit
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

        provider.unbindAll()
        val camera = provider.bindToLifecycle(
            context as LifecycleOwner,
            selector,
            preview,
            imageCapture
        )

        onCameraReady(camera)

    }, ContextCompat.getMainExecutor(context))
}
