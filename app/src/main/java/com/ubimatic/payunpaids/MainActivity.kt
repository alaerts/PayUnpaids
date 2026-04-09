package com.ubimatic.payunpaids

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ubimatic.payunpaids.ui.navigation.AppNavigation
import com.ubimatic.payunpaids.ui.invoice.InvoiceViewModel
import com.ubimatic.payunpaids.ui.theme.PayUnpaidsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val invoiceViewModel: InvoiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PayUnpaidsTheme {
                AppNavigation(invoiceViewModel = invoiceViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        invoiceViewModel.onResumeFromPayment()
    }
}
