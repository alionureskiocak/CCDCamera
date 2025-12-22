package com.example.ccdcamera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun CameraScreen() {
    val context = LocalContext.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var showDate by remember { mutableStateOf(true) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    Box(Modifier.fillMaxSize()) {

        CameraPreview(
            imageCapture = imageCapture,
            lensFacing = lensFacing,
            modifier = Modifier.fillMaxSize()
        )

        // ÜST BAR (minimal HUD)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AssistChip(
                onClick = { showDate = !showDate },
                label = { Text(if (showDate) "DATE ON" else "DATE OFF") }
            )

            IconButton(onClick = {
                lensFacing =
                    if (lensFacing == CameraSelector.LENS_FACING_BACK)
                        CameraSelector.LENS_FACING_FRONT
                    else
                        CameraSelector.LENS_FACING_BACK
            }) {
                Icon(Icons.Default.Cached, contentDescription = "Switch Camera")
            }
        }

        // ALT ORTA LOGO (çekim)
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
                    takePhotoWithCcd(
                        context = context,
                        imageCapture = imageCapture,
                        showDate = showDate,
                        onSaved = {},
                        onError = {}
                    )
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
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
