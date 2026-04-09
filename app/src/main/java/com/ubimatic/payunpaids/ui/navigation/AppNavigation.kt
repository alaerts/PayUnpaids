package com.ubimatic.payunpaids.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ubimatic.payunpaids.ui.invoice.InvoiceScreen
import com.ubimatic.payunpaids.ui.invoice.InvoiceViewModel
import com.ubimatic.payunpaids.ui.settings.SettingsScreen

object Routes {
    const val INVOICE = "invoice"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(
    invoiceViewModel: InvoiceViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val state by invoiceViewModel.uiState.collectAsState()

    val startDestination = if (state.hasCredentials) Routes.INVOICE else Routes.SETTINGS

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.INVOICE) {
            InvoiceScreen(
                viewModel = invoiceViewModel,
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                    invoiceViewModel.checkCredentialsAndSync()
                },
            )
        }
    }
}
