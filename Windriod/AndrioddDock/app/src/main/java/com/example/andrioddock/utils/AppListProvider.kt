package com.example.andrioddock.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import com.example.andrioddock.data.AppInfo
import java.io.ByteArrayOutputStream

class AppListProvider(private val context: Context) {
    companion object {
        private const val TAG = "AppListProvider"
        private const val ICON_SIZE = 96
        private const val JPEG_QUALITY = 80
    }

    private val packageManager: PackageManager = context.packageManager

    fun getInstalledApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val resolveInfoList = packageManager.queryIntentActivities(mainIntent, 0)
            for (resolveInfo in resolveInfoList) {
                try {
                    val packageName = resolveInfo.activityInfo.packageName
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    if (isSystemApp(appInfo) && !hasLauncherActivity(packageName)) continue
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)
                    val iconBase64 = drawableToBase64(icon)
                    apps.add(AppInfo(appName = appName, packageName = packageName, iconBase64 = iconBase64))
                } catch (e: Exception) { Log.w(TAG, "Error processing app: ${resolveInfo.activityInfo.packageName}", e) }
            }
            apps.sortBy { it.appName.lowercase() }
        } catch (e: Exception) { Log.e(TAG, "Error getting installed apps", e) }
        return apps
    }

    private fun isSystemApp(appInfo: ApplicationInfo): Boolean = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    private fun hasLauncherActivity(packageName: String): Boolean = packageManager.getLaunchIntentForPackage(packageName) != null

    private fun drawableToBase64(drawable: Drawable): String {
        val bitmap = drawableToBitmap(drawable)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, ICON_SIZE, ICON_SIZE, true)
        return try {
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, JPEG_QUALITY, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } finally { if (scaledBitmap != bitmap) scaledBitmap.recycle() }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else ICON_SIZE
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else ICON_SIZE
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
