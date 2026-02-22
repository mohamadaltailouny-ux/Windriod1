package com.example.andrioddock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.andrioddock.MainActivity
import com.example.andrioddock.R
import com.example.andrioddock.data.TcpResponse
import com.example.andrioddock.utils.AppListProvider
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class DockServerService : Service() {
    companion object {
        const val TAG = "DockServerService"
        const val EXTRA_PC_IP = "pc_ip"
        const val EXTRA_PC_PORT = "pc_port"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "dock_server_channel"
        private const val SERVER_PORT = 38300
        const val CMD_GET_APPS = "GET_APPS"
        const val CMD_START_APP = "START_APP"
        const val CMD_PING = "PING"
        const val CMD_GET_DEVICE_INFO = "GET_DEVICE_INFO"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val gson = Gson()
    private lateinit var appListProvider: AppListProvider
    private val connectedClients = mutableListOf<Socket>()

    override fun onCreate() { super.onCreate(); appListProvider = AppListProvider(this); createNotificationChannel() }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { startForeground(NOTIFICATION_ID, createNotification()); if (!isRunning) startServer(); return START_STICKY }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { super.onDestroy(); stopServer(); serviceScope.cancel() }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "AndrioddDock Server", NotificationManager.IMPORTANCE_LOW).apply { description = "Hintergrundverbindung für PC-Dock"; setShowBadge(false) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("AndrioddDock aktiv").setContentText("Verbunden mit PC-Dock").setSmallIcon(R.drawable.ic_dock_notification).setContentIntent(pendingIntent).setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW).build()
    }

    private fun startServer() {
        isRunning = true
        serviceScope.launch {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                Log.i(TAG, "Server started on port $SERVER_PORT")
                while (isRunning) {
                    try { val clientSocket = serverSocket?.accept() ?: break; Log.i(TAG, "Client connected: ${clientSocket.inetAddress}"); connectedClients.add(clientSocket); handleClient(clientSocket) }
                    catch (e: Exception) { if (isRunning) Log.e(TAG, "Error accepting client", e) }
                }
            } catch (e: Exception) { Log.e(TAG, "Server error", e) }
        }
    }

    private fun stopServer() {
        isRunning = false
        connectedClients.forEach { try { it.close() } catch (e: Exception) { Log.e(TAG, "Error closing client socket", e) } }
        connectedClients.clear()
        try { serverSocket?.close() } catch (e: Exception) { Log.e(TAG, "Error closing server socket", e) }
        serverSocket = null
    }

    private fun handleClient(clientSocket: Socket) {
        serviceScope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val writer = PrintWriter(clientSocket.getOutputStream(), true)
                while (isRunning && !clientSocket.isClosed) {
                    val line = reader.readLine() ?: break
                    Log.d(TAG, "Received: $line")
                    val response = processCommand(line)
                    val jsonResponse = gson.toJson(response)
                    writer.println(jsonResponse)
                    Log.d(TAG, "Sent: $jsonResponse")
                }
            } catch (e: Exception) { Log.e(TAG, "Client handler error", e) }
            finally { connectedClients.remove(clientSocket); try { clientSocket.close() } catch (e: Exception) { Log.e(TAG, "Error closing client", e) } }
        }
    }

    private suspend fun processCommand(commandLine: String): TcpResponse {
        return try {
            val parts = commandLine.trim().split(" ", limit = 2)
            val command = parts[0].uppercase()
            val argument = parts.getOrNull(1)
            when (command) {
                CMD_PING -> TcpResponse(success = true, command = CMD_PING, data = "PONG")
                CMD_GET_APPS -> { val apps = withContext(Dispatchers.IO) { appListProvider.getInstalledApps() }; TcpResponse(success = true, command = CMD_GET_APPS, data = apps) }
                CMD_START_APP -> { if (argument.isNullOrBlank()) TcpResponse(success = false, command = CMD_START_APP, error = "Package name required") else { startApp(argument); TcpResponse(success = true, command = CMD_START_APP, data = argument) } }
                CMD_GET_DEVICE_INFO -> { val deviceInfo = mapOf("manufacturer" to Build.MANUFACTURER, "model" to Build.MODEL, "android_version" to Build.VERSION.RELEASE, "sdk_version" to Build.VERSION.SDK_INT); TcpResponse(success = true, command = CMD_GET_DEVICE_INFO, data = deviceInfo) }
                else -> TcpResponse(success = false, command = command, error = "Unknown command: $command")
            }
        } catch (e: Exception) { Log.e(TAG, "Command processing error", e); TcpResponse(success = false, command = "ERROR", error = e.message ?: "Unknown error") }
    }

    private fun startApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) { launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(launchIntent); Log.i(TAG, "Started app: $packageName") }
        else Log.w(TAG, "No launch intent for: $packageName")
    }
}
