package com.ubimatic.payunpaids.ui.invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubimatic.payunpaids.ui.alldone.AllDoneScreen
import com.ubimatic.payunpaids.ui.theme.Amber
import com.ubimatic.payunpaids.ui.theme.AmberDark
import com.ubimatic.payunpaids.ui.theme.DarkBorder
import com.ubimatic.payunpaids.ui.theme.DarkSurface
import com.ubimatic.payunpaids.ui.theme.Green
import com.ubimatic.payunpaids.ui.theme.TextPrimary
import com.ubimatic.payunpaids.ui.theme.TextSecondary
import com.ubimatic.payunpaids.ui.theme.WarningBg
import com.ubimatic.payunpaids.ui.theme.WarningText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    viewModel: InvoiceViewModel,
    onNavigateToSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (state.allDone && !state.isLoading) {
        AllDoneScreen(
            stats = state.stats,
            onRefresh = viewModel::sync,
        )
        return
    }

    val invoice = viewModel.currentInvoice

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (invoice != null) {
                        Column {
                            Text(
                                text = invoice.partnerName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                ),
                            )
                            Text(
                                text = buildString {
                                    append("${state.currentIndex + 1}/${state.invoices.size}")
                                    if (state.paidIds.isNotEmpty()) {
                                        append(" (${state.paidIds.size} paid)")
                                    }
                                    append(" \u00B7 \u20AC${String.format("%.2f", invoice.amountResidual)}")
                                    invoice.invoiceDate?.let { append(" \u00B7 $it") }
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                ),
                            )
                        }
                    } else {
                        Text("PayUnpaids", color = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = DarkSurface,
                        ),
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            if (invoice != null && !state.isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    // Prev
                    TextButton(
                        onClick = { if (state.currentIndex > 0) viewModel.goToPrevious() },
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkBorder, RoundedCornerShape(8.dp)),
                    ) {
                        Text("\u2190 Prev", color = TextSecondary, fontSize = 12.sp)
                    }

                    // Paid / Unpaid toggle
                    val isPaid = invoice.id in state.paidIds
                    TextButton(
                        onClick = viewModel::togglePaid,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isPaid) Green else DarkBorder,
                                RoundedCornerShape(8.dp),
                            ),
                    ) {
                        Text(
                            if (isPaid) "\u2713 Paid" else "\u2713 Paid",
                            color = if (isPaid) AmberDark else TextPrimary,
                            fontSize = 12.sp,
                        )
                    }

                    // Pay (hide if already paid)
                    if (invoice.hasIban && !isPaid) {
                        TextButton(
                            onClick = viewModel::showPaySheet,
                            modifier = Modifier
                                .weight(1f)
                                .background(Amber, RoundedCornerShape(8.dp)),
                        ) {
                            Text(
                                "Pay \u25BE",
                                color = AmberDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    // Next
                    TextButton(
                        onClick = { if (state.currentIndex < state.invoices.size - 1) viewModel.goToNext() },
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkBorder, RoundedCornerShape(8.dp)),
                    ) {
                        Text("Next \u2192", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Amber)
                    }
                }

                invoice != null -> {
                    Column {
                        // Warning: no structured communication
                        if (!invoice.hasStructuredCommunication) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(WarningBg)
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "\u26A0  No structured communication \u2014 verify before paying",
                                    color = WarningText,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        // Warning: no IBAN
                        if (!invoice.hasIban) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "\u26A0  No IBAN available \u2014 cannot pay electronically",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        // Content area
                        Box(modifier = Modifier.weight(1f)) {
                            if (invoice.pdfData != null) {
                                InvoicePdfScreen(
                                    invoiceId = invoice.id,
                                    pdfData = invoice.pdfData,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                InvoiceFallbackScreen(
                                    invoice = invoice,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            // Paid overlay
                            if (invoice.id in state.paidIds) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Green.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "\u2713 PAID",
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Green.copy(alpha = 0.4f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Payment bottom sheet
    if (state.showPaySheet) {
        PaymentBottomSheet(
            amount = invoice?.amountResidual ?: 0.0,
            onBankSelected = viewModel::payWithBank,
            onDismiss = viewModel::hidePaySheet,
        )
    }
}
