package com.ubimatic.payunpaids.ui.invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubimatic.payunpaids.domain.model.Invoice
import com.ubimatic.payunpaids.ui.theme.DarkSurface
import com.ubimatic.payunpaids.ui.theme.TextMuted
import com.ubimatic.payunpaids.ui.theme.TextPrimary

@Composable
fun InvoiceFallbackScreen(
    invoice: Invoice,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(12.dp))
                .padding(20.dp),
        ) {
            Text(
                text = "Invoice Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(16.dp))

            DetailRow("Supplier", invoice.partnerName)
            DetailRow("Invoice", invoice.name)
            DetailRow("Amount Due", String.format("\u20AC%.2f", invoice.amountResidual))
            DetailRow("Due Date", invoice.invoiceDateDue ?: "N/A")
            DetailRow("Invoice Date", invoice.invoiceDate ?: "N/A")
            DetailRow("IBAN", invoice.partnerIban ?: "N/A")
            DetailRow("Communication", invoice.paymentReference ?: "None")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = TextPrimary,
        )
    }
}
