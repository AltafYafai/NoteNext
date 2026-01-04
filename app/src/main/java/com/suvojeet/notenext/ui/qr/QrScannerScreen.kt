package com.suvojeet.notenext.ui.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Patterns
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.suvojeet.notenext.util.QrCodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * A beautiful QR Scanner screen using CameraX and ML Kit.
 * Scans QR codes to import notes shared from other devices.
 * Also supports scanning generic QR codes and picking images from gallery.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onBackClick: () -> Unit,
    onNoteScanned: (title: String, content: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(true) }
    var showRawQrDialog by remember { mutableStateOf(false) }
    var scannedRawContent by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Helper to handle scanned data from camera or gallery
    fun handleScannedData(data: String) {
        val noteData = QrCodeUtils.decodeQrData(data)
        if (noteData != null) {
            onNoteScanned(noteData.t, noteData.c)
        } else {
            scannedRawContent = data
            showRawQrDialog = true
            // Keep isScanning false while dialog is shown
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isScanning = false // Pause camera
            scope.launch(Dispatchers.IO) {
                try {
                    val inputImage = InputImage.fromFilePath(context, uri)
                    withContext(Dispatchers.Main) {
                        val scanner = BarcodeScanning.getClient()
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val qr = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                if (qr?.rawValue != null) {
                                    handleScannedData(qr.rawValue!!)
                                } else {
                                    Toast.makeText(context, "No QR code found in image", Toast.LENGTH_SHORT).show()
                                    isScanning = true
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to scan image", Toast.LENGTH_SHORT).show()
                                isScanning = true
                            }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
                        isScanning = true
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Camera Preview
            CameraPreviewView(
                isFlashOn = isFlashOn,
                isScanning = isScanning && !showRawQrDialog,
                onQrCodeScanned = { data ->
                    isScanning = false
                    handleScannedData(data)
                }
            )

            // Scan Overlay
            ScanOverlay()

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Gallery Button
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Pick from Gallery",
                            tint = Color.White
                        )
                    }

                    // Flash Button
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Flash",
                            tint = if (isFlashOn) Color(0xFFFFD700) else Color.White
                        )
                    }
                }
            }

            // Bottom Instructions
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = "Point camera at a QR code",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Scan notes or generic QR codes",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Raw QR Dialog
            if (showRawQrDialog) {
                RawQrContentDialog(
                    content = scannedRawContent,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(scannedRawContent))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = {
                        showRawQrDialog = false
                        isScanning = true
                    }
                )
            }

        } else {
            // Permission denied UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📷",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Please grant camera permission to scan QR codes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                ) {
                    Text("Grant Permission")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onBackClick) {
                    Text("Go Back", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun RawQrContentDialog(
    content: String,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scanned QR Code") },
        text = {
            SelectionContainer {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Open Link if URL
                if (Patterns.WEB_URL.matcher(content).matches()) {
                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open")
                    }
                }
                
                TextButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CameraPreviewView(
    isFlashOn: Boolean,
    isScanning: Boolean,
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    
    // Capture latest state for analyzer
    val scanningState = rememberUpdatedState(isScanning)
    val onQrScannedState = rememberUpdatedState(onQrCodeScanned)

    // Update flash when state changes
    LaunchedEffect(isFlashOn) {
        camera?.cameraControl?.enableTorch(isFlashOn)
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()
            val barcodeScanner = BarcodeScanning.getClient()

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    @androidx.camera.core.ExperimentalGetImage
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && scanningState.value) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    if (barcode.format == Barcode.FORMAT_QR_CODE) {
                                        barcode.rawValue?.let { onQrScannedState.value(it) }
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScanOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val scanAreaSize = size.width * 0.7f
        val left = (size.width - scanAreaSize) / 2
        val top = (size.height - scanAreaSize) / 2
        val cornerLength = 40.dp.toPx()
        val strokeWidth = 4.dp.toPx()

        // Dim overlay
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            size = size
        )

        // Clear scan area
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(scanAreaSize, scanAreaSize),
            cornerRadius = CornerRadius(16.dp.toPx()),
            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
        )

        // Corner brackets (top-left)
        drawLine(
            color = Color(0xFF6200EE),
            start = Offset(left, top + cornerLength),
            end = Offset(left, top),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color(0xFF6200EE),
            start = Offset(left, top),
            end = Offset(left + cornerLength, top),
            strokeWidth = strokeWidth
        )

        // Top-right
        drawLine(
            color = Color(0xFF6200EE),
            start = Offset(left + scanAreaSize - cornerLength, top),
            end = Offset(left + scanAreaSize, top),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color(0xFF6200EE),
            start = Offset(left + scanAreaSize, top),
            end = Offset(left + scanAreaSize, top + cornerLength),
            strokeWidth = strokeWidth
        )

        // Bottom-left
        drawLine(
            color = Color(0xFF6200EE),
            start = Offset(left, top + scanAreaSize - cornerLength),
            end = Offset(left, top + scanAreaSize),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color(0xFF6200EE),
            start = Offset(left, top + scanAreaSize),
            end = Offset(left + cornerLength, top + scanAreaSize),
            strokeWidth = strokeWidth
        )

        // Bottom-right
        drawLine(
            color = Color(0xFF6200EE),
            start = Offset(left + scanAreaSize - cornerLength, top + scanAreaSize),
            end = Offset(left + scanAreaSize, top + scanAreaSize),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color(0xFF6200EE),
            start = Offset(left + scanAreaSize, top + scanAreaSize - cornerLength),
            end = Offset(left + scanAreaSize, top + scanAreaSize),
            strokeWidth = strokeWidth
        )

        // Animated scan line
        val lineY = top + (scanAreaSize * animatedOffset)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF6200EE),
                    Color(0xFF6200EE),
                    Color.Transparent
                )
            ),
            start = Offset(left + 20.dp.toPx(), lineY),
            end = Offset(left + scanAreaSize - 20.dp.toPx(), lineY),
            strokeWidth = 2.dp.toPx()
        )
    }
}