package com.example.andrioddock.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.andrioddock.ui.viewmodels.SetupUiState
import com.example.andrioddock.ui.viewmodels.SetupViewModel

@Composable
fun SetupWizardScreen(viewModel: SetupViewModel, onNavigateToQrScanner: () -> Unit, onNavigateToConnected: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        StepProgressIndicator(currentStep = uiState.currentStep, totalSteps = uiState.totalSteps)
        Spacer(modifier = Modifier.height(24.dp))
        AnimatedContent(targetState = uiState.currentStep, transitionSpec = {
            if (targetState > initialState) slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            else slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
        }, modifier = Modifier.weight(1f), label = "step") { step ->
            when (step) {
                0 -> WelcomeStep()
                1 -> UsbConnectionStep(uiState)
                2 -> CopyFilesStep(uiState = uiState, onCopyFiles = { viewModel.copyPCSoftwareToDownloads() })
                3 -> InstallPCStep()
                4 -> ConnectStep(onScanQrCode = onNavigateToQrScanner)
            }
        }
        uiState.errorMessage?.let { error ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(text = error, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
            }
        }
        NavigationButtons(currentStep = uiState.currentStep, totalSteps = uiState.totalSteps, canProceed = canProceedToNextStep(uiState), isLoading = uiState.isLoading, onPrevious = { viewModel.previousStep() }, onNext = { viewModel.nextStep() })
    }
}

@Composable
private fun StepProgressIndicator(currentStep: Int, totalSteps: Int) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            repeat(totalSteps) { index ->
                StepDot(stepNumber = index + 1, isCompleted = index < currentStep, isCurrent = index == currentStep)
                if (index < totalSteps - 1) Box(modifier = Modifier.weight(1f).height(2.dp).background(if (index < currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Schritt ${currentStep + 1} von $totalSteps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepDot(stepNumber: Int, isCompleted: Boolean, isCurrent: Boolean) {
    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(when { isCompleted -> MaterialTheme.colorScheme.primary; isCurrent -> MaterialTheme.colorScheme.primaryContainer; else -> MaterialTheme.colorScheme.surfaceVariant }), contentAlignment = Alignment.Center) {
        if (isCompleted) Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
        else Text(text = stepNumber.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun WelcomeStep() { StepContent(icon = Icons.Default.Link, title = "Willkommen bei AndrioddDock", description = "Mit dieser App verbinden Sie Ihr Android-Gerät mit Ihrem PC und können Ihre Apps direkt vom Desktop aus starten.\n\nDieser Assistent führt Sie durch die Einrichtung.") }

@Composable
private fun UsbConnectionStep(uiState: SetupUiState) {
    StepContent(icon = Icons.Default.Usb, title = "USB-Verbindung", description = "Verbinden Sie Ihr Android-Gerät per USB-Kabel mit Ihrem PC.\n\nStellen Sie sicher, dass der USB-Modus auf 'Dateiübertragung' (MTP) eingestellt ist.") {
        Card(colors = CardDefaults.cardColors(containerColor = if (uiState.isUsbConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = if (uiState.isUsbConnected) Icons.Default.Check else Icons.Default.Usb, contentDescription = null, tint = if (uiState.isUsbConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = if (uiState.isUsbConnected) "USB verbunden" else "Warte auf USB-Verbindung...", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun CopyFilesStep(uiState: SetupUiState, onCopyFiles: () -> Unit) {
    StepContent(icon = Icons.Default.ContentCopy, title = "PC-Software kopieren", description = "Kopieren Sie die PC-Software in den Download-Ordner Ihres Handys.") {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (uiState.isLoading) { LinearProgressIndicator(progress = { uiState.copyProgress }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)); Text(text = "Kopiere Dateien... ${(uiState.copyProgress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium) }
            else if (uiState.filesCopied) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column { Text(text = "Dateien kopiert!", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold); Text(text = "Speicherort: Downloads/AndrioddDock", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            } else Button(onClick = onCopyFiles, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.ContentCopy, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Dateien kopieren") }
        }
    }
}

@Composable
private fun InstallPCStep() {
    StepContent(icon = Icons.Default.Download, title = "PC-Software installieren", description = "Öffnen Sie auf Ihrem PC den Download-Ordner Ihres Handys und installieren Sie die passende Software:") {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InstallInstructionCard(platform = "Windows", instruction = "Führen Sie dock-setup-windows.exe aus")
            InstallInstructionCard(platform = "Linux", instruction = "Machen Sie dock-setup-linux.appimage ausführbar und starten Sie es")
            InstallInstructionCard(platform = "macOS", instruction = "Öffnen Sie dock-setup-macos.dmg und ziehen Sie die App in Applications")
        }
    }
}

@Composable
private fun InstallInstructionCard(platform: String, instruction: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) { Text(text = platform, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(text = instruction, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun ConnectStep(onScanQrCode: () -> Unit) {
    StepContent(icon = Icons.Default.QrCodeScanner, title = "Mit PC verbinden", description = "Starten Sie die PC-Software. Ein QR-Code wird angezeigt. Scannen Sie diesen Code, um die Verbindung herzustellen.") {
        Button(onClick = onScanQrCode, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Icon(Icons.Default.QrCodeScanner, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("QR-Code scannen") }
    }
}

@Composable
private fun StepContent(icon: ImageVector, title: String, description: String, additionalContent: @Composable (() -> Unit)? = null) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        additionalContent?.let { Spacer(modifier = Modifier.height(24.dp)); it() }
    }
}

@Composable
private fun NavigationButtons(currentStep: Int, totalSteps: Int, canProceed: Boolean, isLoading: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        if (currentStep > 0) OutlinedButton(onClick = onPrevious, enabled = !isLoading) { Text("Zurück") } else Spacer(modifier = Modifier.width(1.dp))
        if (currentStep < totalSteps - 1) Button(onClick = onNext, enabled = canProceed && !isLoading) { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Weiter") }
    }
}

private fun canProceedToNextStep(uiState: SetupUiState): Boolean = when (uiState.currentStep) { 0 -> true; 1 -> true; 2 -> uiState.filesCopied; 3 -> true; 4 -> true; else -> true }
