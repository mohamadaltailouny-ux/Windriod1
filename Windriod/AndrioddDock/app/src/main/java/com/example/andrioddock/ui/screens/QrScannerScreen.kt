package com.example.andrioddock.ui.screens

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.andrioddock.data.ConnectionInfo
import com.example.andrioddock.utils.QrCodeAnalyzer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(onQrCodeScanned: (ConnectionInfo) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasScanned by remember { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var cameraExecutor: ExecutorService? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) { cameraExecutor = Executors.newSingleThreadExecutor(); onDispose { cameraExecutor?.shutdown() } }

    Scaffold(topBar = { TopAppBar(title = { Text("QR-Code scannen") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Zurück") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                cameraPermissionState.status.isGranted -> {
                    AndroidView(factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val imageAnalyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also { analysis ->
                                analysis.setAnalyzer(cameraExecutor ?: Executors.newSingleThreadExecutor(), QrCodeAnalyzer { qrCode ->
                                    if (!hasScanned) { hasScanned = true; parseQrCode(qrCode)?.let { onQrCodeScanned(it) } }
                                })
                            }
                            try { cameraProvider.unbindAll(); cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer) }
                            catch (e: Exception) { Log.e("QrScanner", "Camera binding failed", e) }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }, modifier = Modifier.fillMaxSize())
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Box(modifier = Modifier.size(250.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.1f))) }
                    Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.6f)).padding(24.dp)) { Text(text = "Richten Sie die Kamera auf den QR-Code auf Ihrem PC-Bildschirm", color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                }
                cameraPermissionState.status.shouldShowRationale -> PermissionRationale { cameraPermissionState.launchPermissionRequest() }
                else -> { LaunchedEffect(Unit) { cameraPermissionState.launchPermissionRequest() }; PermissionRationale { cameraPermissionState.launchPermissionRequest() } }
            }
        }
    }
}

@Composable
private fun PermissionRationale(onRequestPermission: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Kamera-Berechtigung erforderlich", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Um den QR-Code auf Ihrem PC zu scannen, benötigt die App Zugriff auf Ihre Kamera.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRequestPermission) { Text("Berechtigung erteilen") }
    }
}

private fun parseQrCode(qrCode: String): ConnectionInfo? {
    return try {
        val uri = android.net.Uri.parse(qrCode)
        if (uri.scheme == "andrioddock" && uri.host == "connect") { val ip = uri.getQueryParameter("ip") ?: return null; val port = uri.getQueryParameter("port")?.toIntOrNull() ?: 38300; ConnectionInfo(ipAddress = ip, port = port) }
        else { val parts = qrCode.split(":"); if (parts.size == 2) ConnectionInfo(ipAddress = parts[0], port = parts[1].toIntOrNull() ?: 38300) else null }
    } catch (e: Exception) { Log.e("QrScanner", "Failed to parse QR code: $qrCode", e); null }
}
