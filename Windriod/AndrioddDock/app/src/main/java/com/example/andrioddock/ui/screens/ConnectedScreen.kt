package com.example.andrioddock.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.andrioddock.data.AppInfo
import com.example.andrioddock.ui.viewmodels.SetupViewModel
import com.example.andrioddock.utils.base64ToBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedScreen(viewModel: SetupViewModel, onDisconnect: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("AndrioddDock") }, actions = { IconButton(onClick = { viewModel.refreshAppList() }) { Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren") } }) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            ConnectionStatusCard(ipAddress = uiState.connectionInfo?.ipAddress ?: "Nicht verbunden", port = uiState.connectionInfo?.port ?: 0, onDisconnect = onDisconnect)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Installierte Apps (${uiState.installedApps.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.isLoading) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else if (uiState.installedApps.isEmpty()) EmptyAppsState()
            else AppGrid(apps = uiState.installedApps)
        }
    }
}

@Composable
private fun ConnectionStatusCard(ipAddress: String, port: Int, onDisconnect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) { Text(text = "Verbunden mit PC", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(text = "$ipAddress:$port", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)) }
            Button(onClick = onDisconnect, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) { Icon(imageVector = Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Trennen", style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun EmptyAppsState() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(imageVector = Icons.Default.Android, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Keine Apps gefunden", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "Tippen Sie auf Aktualisieren, um die App-Liste neu zu laden", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}

@Composable
private fun AppGrid(apps: List<AppInfo>) {
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 80.dp), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
        items(apps, key = { it.packageName }) { app -> AppGridItem(app = app) }
    }
}

@Composable
private fun AppGridItem(app: AppInfo) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Card(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val bitmap = base64ToBitmap(app.iconBase64)
                if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = app.appName, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                else Icon(imageVector = Icons.Default.Android, contentDescription = app.appName, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = app.appName, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
