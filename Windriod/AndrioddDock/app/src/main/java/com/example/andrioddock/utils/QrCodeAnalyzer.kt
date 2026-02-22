package com.example.andrioddock.utils

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(private val onQrCodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
    companion object { private const val TAG = "QrCodeAnalyzer" }
    private val scanner = BarcodeScanning.getClient()
    private var isProcessing = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing) { imageProxy.close(); return }
        val mediaImage = imageProxy.image
        if (mediaImage == null) { imageProxy.close(); return }
        isProcessing = true
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    if (barcode.valueType == Barcode.TYPE_TEXT || barcode.valueType == Barcode.TYPE_URL) {
                        barcode.rawValue?.let { value -> Log.d(TAG, "QR Code detected: $value"); onQrCodeDetected(value) }
                    }
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Barcode scanning failed", e) }
            .addOnCompleteListener { isProcessing = false; imageProxy.close() }
    }
}
