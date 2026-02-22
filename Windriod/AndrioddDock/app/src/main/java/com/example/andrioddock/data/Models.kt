package com.example.andrioddock.data

data class AppInfo(
    val appName: String,
    val packageName: String,
    val iconBase64: String
)

data class ConnectionInfo(
    val ipAddress: String,
    val port: Int = 38300
)

data class TcpMessage(
    val command: String,
    val data: Map<String, Any>? = null
)

data class TcpResponse(
    val success: Boolean,
    val command: String,
    val data: Any? = null,
    val error: String? = null
)
