package com.example.andrioddock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.andrioddock.ui.screens.ConnectedScreen
import com.example.andrioddock.ui.screens.QrScannerScreen
import com.example.andrioddock.ui.screens.SetupWizardScreen
import com.example.andrioddock.ui.viewmodels.SetupViewModel

sealed class Screen(val route: String) {
    object SetupWizard : Screen("setup_wizard")
    object QrScanner : Screen("qr_scanner")
    object Connected : Screen("connected")
}

@Composable
fun DockApp(viewModel: SetupViewModel = viewModel()) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = Screen.SetupWizard.route) {
        composable(Screen.SetupWizard.route) {
            SetupWizardScreen(
                viewModel = viewModel,
                onNavigateToQrScanner = { navController.navigate(Screen.QrScanner.route) },
                onNavigateToConnected = {
                    navController.navigate(Screen.Connected.route) { popUpTo(Screen.SetupWizard.route) { inclusive = true } }
                }
            )
        }
        composable(Screen.QrScanner.route) {
            QrScannerScreen(
                onQrCodeScanned = { connectionInfo ->
                    viewModel.connectToPC(connectionInfo)
                    navController.navigate(Screen.Connected.route) { popUpTo(Screen.SetupWizard.route) { inclusive = true } }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Connected.route) {
            ConnectedScreen(
                viewModel = viewModel,
                onDisconnect = {
                    viewModel.disconnect()
                    navController.navigate(Screen.SetupWizard.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }
    }
}
