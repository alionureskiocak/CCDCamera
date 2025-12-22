package com.example.ccdcamera

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun CameraScreen() {
    val context = LocalContext.current

    // ---------------- STATE ----------------

    var lensFacing by remember {
        mutableStateOf(CameraSelector.LENS_FACING_BACK)
    }

    var showDate by remember {
        mutableStateOf(true)
    }

    var previewBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // ---------------- PREVIEW SCREEN ----------------

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

    // ---------------- CAMERA UI ----------------

    Box(modifier = Modifier.fillMaxSize()) {

        CameraPreview(
            imageCapture = imageCapture,
            lensFacing = lensFacing,
            modifier = Modifier.fillMaxSize()
        )

        // ---------- TOP BAR ----------

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
                label = {
                    Text(if (showDate) "DATE ON" else "DATE OFF")
                }
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

        // ---------- BOTTOM LOGO (SHUTTER) ----------

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
                        onResult = { bitmap ->
                            previewBitmap = bitmap
                        },
                        onError = {
                            // istersen toast koyarsın
                        }
                    )
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
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

@Composable
private fun PhotoPreviewScreen(
    bitmap: Bitmap,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Button(
                onClick = onDiscard,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray
                )
            ) {
                Text("✖ DISCARD")
            }

            Button(
                onClick = onSave
            ) {
                Text("✔ SAVE")
            }
        }
    }
}
