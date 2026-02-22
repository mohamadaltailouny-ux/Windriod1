package com.example.andrioddock.ui.viewmodels

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.andrioddock.data.AppInfo
import com.example.andrioddock.data.ConnectionInfo
import com.example.andrioddock.service.DockServerService
import com.example.andrioddock.utils.AppListProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class SetupUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 5,
    val isUsbConnected: Boolean = false,
    val filesCopied: Boolean = false,
    val isConnectedToPC: Boolean = false,
    val connectionInfo: ConnectionInfo? = null,
    val installedApps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val copyProgress: Float = 0f
)

class SetupViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()
    private val context: Context get() = getApplication()
    private val appListProvider = AppListProvider(context)

    companion object {
        private val PC_SOFTWARE_FILES = listOf("dock-setup-windows.exe", "dock-setup-linux.appimage", "dock-setup-macos.dmg")
    }

    fun nextStep() { _uiState.update { if (it.currentStep < it.totalSteps - 1) it.copy(currentStep = it.currentStep + 1) else it } }
    fun previousStep() { _uiState.update { if (it.currentStep > 0) it.copy(currentStep = it.currentStep - 1) else it } }
    fun goToStep(step: Int) { _uiState.update { it.copy(currentStep = step.coerceIn(0, it.totalSteps - 1)) } }
    fun setUsbConnected(connected: Boolean) { _uiState.update { it.copy(isUsbConnected = connected) } }

    fun copyPCSoftwareToDownloads() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, copyProgress = 0f) }
            try {
                withContext(Dispatchers.IO) {
                    val totalFiles = PC_SOFTWARE_FILES.size
                    var copiedFiles = 0
                    PC_SOFTWARE_FILES.forEach { fileName ->
                        copyAssetToDownloads(fileName)
                        copiedFiles++
                        _uiState.update { it.copy(copyProgress = copiedFiles.toFloat() / totalFiles) }
                    }
                }
                _uiState.update { it.copy(filesCopied = true, isLoading = false, copyProgress = 1f) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Fehler beim Kopieren: ${e.message}") }
            }
        }
    }

    private fun copyAssetToDownloads(fileName: String) {
        val assetManager = context.assets
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName))
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AndrioddDock")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: throw Exception("Konnte Datei nicht erstellen: $fileName")
            resolver.openOutputStream(uri)?.use { outputStream -> assetManager.open(fileName).use { inputStream -> inputStream.copyTo(outputStream) } }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dockDir = File(downloadsDir, "AndrioddDock")
            if (!dockDir.exists()) dockDir.mkdirs()
            val outputFile = File(dockDir, fileName)
            FileOutputStream(outputFile).use { outputStream -> assetManager.open(fileName).use { inputStream -> inputStream.copyTo(outputStream) } }
        }
    }

    private fun getMimeType(fileName: String): String = when {
        fileName.endsWith(".exe") -> "application/x-msdownload"
        fileName.endsWith(".appimage") -> "application/x-executable"
        fileName.endsWith(".dmg") -> "application/x-apple-diskimage"
        else -> "application/octet-stream"
    }

    fun connectToPC(connectionInfo: ConnectionInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val serviceIntent = Intent(context, DockServerService::class.java).apply {
                    putExtra(DockServerService.EXTRA_PC_IP, connectionInfo.ipAddress)
                    putExtra(DockServerService.EXTRA_PC_PORT, connectionInfo.port)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent) else context.startService(serviceIntent)
                val apps = withContext(Dispatchers.IO) { appListProvider.getInstalledApps() }
                _uiState.update { it.copy(isConnectedToPC = true, connectionInfo = connectionInfo, installedApps = apps, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Verbindungsfehler: ${e.message}") }
            }
        }
    }

    fun disconnect() {
        context.stopService(Intent(context, DockServerService::class.java))
        _uiState.update { it.copy(isConnectedToPC = false, connectionInfo = null) }
    }

    fun refreshAppList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val apps = withContext(Dispatchers.IO) { appListProvider.getInstalledApps() }
            _uiState.update { it.copy(installedApps = apps, isLoading = false) }
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
}
