package com.example.andrioddock.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log

fun base64ToBitmap(base64String: String): Bitmap? {
    return try {
        if (base64String.isBlank()) return null
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) { Log.e("Base64Utils", "Error decoding base64 to bitmap", e); null }
}

fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
    return try {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream)
        Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) { Log.e("Base64Utils", "Error encoding bitmap to base64", e); "" }
}
