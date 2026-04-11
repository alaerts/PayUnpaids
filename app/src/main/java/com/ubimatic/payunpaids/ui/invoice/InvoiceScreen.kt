package com.ubimatic.payunpaids.ui.invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubimatic.payunpaids.R
import com.ubimatic.payunpaids.ui.alldone.AllDoneScreen
import com.ubimatic.payunpaids.ui.theme.Accent
import com.ubimatic.payunpaids.ui.theme.Amber
import com.ubimatic.payunpaids.ui.theme.AmberDark
import com.ubimatic.payunpaids.ui.theme.Background
import com.ubimatic.payunpaids.ui.theme.DarkBorder
import com.ubimatic.payunpaids.ui.theme.DarkSurface
import com.ubimatic.payunpaids.ui.theme.Green
import com.ubimatic.payunpaids.ui.theme.Surface
import com.ubimatic.payunpaids.ui.theme.TextHint
import com.ubimatic.payunpaids.ui.theme.TextPrimary
import com.ubimatic.payunpaids.ui.theme.TextSecond
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

    // Easter egg: triple-tap the wordmark
    var tapCount by remember { mutableIntStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showEasterEgg) {
        EasterEggDialog(onDismiss = { showEasterEgg = false })
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
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = androidx.compose.ui.text.buildAnnotatedString {
                            withStyle(androidx.compose.ui.text.SpanStyle(color = Accent)) { append("Pay") }
                            append("Un")
                            withStyle(androidx.compose.ui.text.SpanStyle(color = Accent)) { append("paids") }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.clickable {
                            tapCount++
                            if (tapCount >= 3) {
                                showEasterEgg = true
                                tapCount = 0
                            }
                        },
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.settings_cd),
                            tint = TextSecond,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
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
                    ActionButton(
                        text = "\u2190 Prev",
                        color = TextPrimary,
                        bg = DarkBorder,
                        onClick = { if (state.currentIndex > 0) viewModel.goToPrevious() },
                        modifier = Modifier.weight(1f),
                    )

                    // Paid / Unpaid toggle
                    val isPaid = invoice.id in state.paidIds
                    ActionButton(
                        text = if (isPaid) "\u2713 Paid" else "Paid",
                        color = if (isPaid) AmberDark else TextPrimary,
                        bg = if (isPaid) Green else DarkBorder,
                        onClick = viewModel::togglePaid,
                        modifier = Modifier.weight(1f),
                    )

                    // Pay (hide if already paid or no bank selected)
                    if (invoice.hasIban && !isPaid && state.selectedBank != null) {
                        ActionButton(
                            text = "Pay",
                            color = AmberDark,
                            bg = Amber,
                            onClick = viewModel::pay,
                            modifier = Modifier.weight(1f),
                            bold = true,
                        )
                    }

                    // Next
                    ActionButton(
                        text = "Next \u2192",
                        color = TextPrimary,
                        bg = DarkBorder,
                        onClick = { if (state.currentIndex < state.invoices.size - 1) viewModel.goToNext() },
                        modifier = Modifier.weight(1f),
                    )
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
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                        initialPage = state.currentIndex,
                        pageCount = { state.invoices.size },
                    )

                    // Sync pager swipe → ViewModel index
                    LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.currentPage != state.currentIndex) {
                            viewModel.setCurrentIndex(pagerState.currentPage)
                        }
                    }
                    // Sync ViewModel index → pager (e.g. via Prev/Next buttons)
                    LaunchedEffect(state.currentIndex) {
                        if (pagerState.currentPage != state.currentIndex) {
                            pagerState.animateScrollToPage(state.currentIndex)
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Secondary header: invoice info + supplier
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Background)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = buildString {
                                    append("${state.currentIndex + 1}/${state.invoices.size}")
                                    if (state.paidIds.isNotEmpty()) {
                                        append(" (${state.paidIds.size} paid)")
                                    }
                                    append(" \u00B7 \u20AC${String.format("%.2f", invoice.amountResidual)}")
                                    invoice.invoiceDate?.let { append(" \u00B7 $it") }
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Text(
                                text = invoice.partnerName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecond,
                            )
                        }

                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .excludeFromSystemGestures(),
                        ) { pageIndex ->
                            val pageInvoice = state.invoices[pageIndex]
                            Column {
                                if (!pageInvoice.hasStructuredCommunication) {
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

                            if (!pageInvoice.hasIban) {
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

                            Box(modifier = Modifier.weight(1f)) {
                                if (pageInvoice.pdfData != null) {
                                    InvoicePdfScreen(
                                        invoiceId = pageInvoice.id,
                                        pdfData = pageInvoice.pdfData,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    InvoiceFallbackScreen(
                                        invoice = pageInvoice,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }

                                    if (pageInvoice.id in state.paidIds) {
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
        }
    }

    // Journal picker dialog (shown when multiple bank journals exist)
    if (state.showJournalPicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::hideJournalPicker,
            title = { Text("Pay from which account?", color = TextPrimary) },
            text = {
                Column {
                    state.journals.forEach { (id, name) ->
                        TextButton(
                            onClick = { viewModel.selectJournalAndPay(id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(name, color = TextPrimary, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = DarkSurface,
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    bg: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
) {
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(8.dp))
            .then(Modifier.clickable(onClick = onClick))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun EasterEggDialog(onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.easter_confirm),
                    color = Accent,
                )
            }
        },
        title = {
            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(androidx.compose.ui.text.SpanStyle(color = Accent)) { append("Pay") }
                    append("Un")
                    withStyle(androidx.compose.ui.text.SpanStyle(color = Accent)) { append("paids") }
                },
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        text = {
            Column {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.easter_crafted),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.easter_assisted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecond,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.easter_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextHint,
                )
            }
        },
        containerColor = Surface,
        textContentColor = TextPrimary,
    )
}

/**
 * Tells Android to exclude this area from system gestures (e.g. edge-swipe back).
 * Required for HorizontalPager to receive left/right swipes near the screen edges
 * on Android 10+.
 */
@Composable
private fun Modifier.excludeFromSystemGestures(): Modifier {
    val view = androidx.compose.ui.platform.LocalView.current
    return this.onGloballyPositioned { coords ->
        val bounds = coords.boundsInRoot()
        val rect = android.graphics.Rect(
            bounds.left.toInt(),
            bounds.top.toInt(),
            bounds.right.toInt(),
            bounds.bottom.toInt(),
        )
        // Requires API 29+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            view.systemGestureExclusionRects = listOf(rect)
        }
    }
}
