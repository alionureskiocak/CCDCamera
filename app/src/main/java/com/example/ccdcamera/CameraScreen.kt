package com.example.ccdcamera

import PhotoPreviewScreen
import android.graphics.Bitmap
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun CameraScreen() {
    val context = LocalContext.current

    // ---------------- STATE ----------------

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var showDate by remember { mutableStateOf(true) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var showZoomLabel by remember { mutableStateOf(false) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_4_3)
            .build()
    }

    // ---------------- PREVIEW AFTER SHOT ----------------

    previewBitmap?.let { bitmap ->
        PhotoPreviewScreen(
            bitmap = bitmap,
            onSave = {
                saveToGallery(context, bitmap)
                previewBitmap = null
            },
            onDiscard = {
                bitmap.recycle()
                previewBitmap = null
            }
        )
        return
    }

    // ---------------- AUTO HIDE ZOOM LABEL ----------------

    LaunchedEffect(showZoomLabel) {
        if (showZoomLabel) {
            delay(1500)
            showZoomLabel = false
        }
    }

    // ---------------- CAMERA UI ----------------

    Box(
        modifier = Modifier
            .fillMaxSize()

            // 🤏 PINCH TO ZOOM
            .pointerInput(camera) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    val cam = camera ?: return@detectTransformGestures
                    val zoomState = cam.cameraInfo.zoomState.value ?: return@detectTransformGestures

                    val newZoom = (zoomState.zoomRatio * zoomChange)
                        .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)

                    cam.cameraControl.setZoomRatio(newZoom)
                    zoomRatio = newZoom
                    showZoomLabel = true
                }
            }

            // 👆 DOUBLE TAP ZOOM
            .pointerInput(camera) {
                detectTapGestures(
                    onDoubleTap = {
                        val cam = camera ?: return@detectTapGestures
                        val zoomState = cam.cameraInfo.zoomState.value ?: return@detectTapGestures

                        val targetZoom = when {
                            zoomRatio < 1.5f -> 2f
                            zoomRatio < 3f -> 3f
                            else -> 1f
                        }.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)

                        cam.cameraControl.setZoomRatio(targetZoom)
                        zoomRatio = targetZoom
                        showZoomLabel = true
                    }
                )
            }
    ) {

        // ---------------- CAMERA PREVIEW ----------------

        CameraPreview(
            imageCapture = imageCapture,
            lensFacing = lensFacing,
            modifier = Modifier.fillMaxSize(),
            onCameraReady = { cam ->
                camera = cam
                zoomRatio = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
            }
        )

        // ---------------- ZOOM LABEL ----------------

        AnimatedVisibility(
            visible = showZoomLabel,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {

        Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "${(zoomRatio * 10).roundToInt() / 10f}x",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // ---------------- TOP BAR ----------------

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            AssistChip(
                onClick = { showDate = !showDate },
                label = { Text(if (showDate) "DATE ON" else "DATE OFF") }
            )

            IconButton(
                onClick = {
                    lensFacing =
                        if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT
                        else
                            CameraSelector.LENS_FACING_BACK
                }
            ) {
                Icon(
                    Icons.Default.Cached,
                    contentDescription = "Switch Camera",
                    tint = Color.White
                )
            }
        }

        // ---------------- SHUTTER ----------------

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.size(84.dp),
                onClick = {
                    takePhotoWithCcdPreview(
                        context = context,
                        imageCapture = imageCapture,
                        showDate = showDate,
                        onResult = { previewBitmap = it },
                        onError = {}
                    )
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "CCD",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}
