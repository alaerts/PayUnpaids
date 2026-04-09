package com.ubimatic.payunpaids.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.ubimatic.payunpaids.domain.model.Bank
import com.ubimatic.payunpaids.domain.model.Invoice
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PaymentDispatcher"

@Singleton
class PaymentDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uriBuilder: PaymentUriBuilder,
) {
    fun dispatch(invoice: Invoice, bank: Bank): Boolean {
        val iban = invoice.partnerIban ?: return false

        return if (bank == Bank.KEYTRADE) {
            dispatchClipboard(invoice, iban)
        } else {
            dispatchDeepLink(invoice, bank, iban)
        }
    }

    private fun dispatchDeepLink(invoice: Invoice, bank: Bank, iban: String): Boolean {
        val uriString = uriBuilder.buildUriString(
            bank = bank,
            iban = iban,
            amount = invoice.amountResidual,
            name = invoice.partnerName,
            communication = invoice.paymentReference,
        ) ?: return false

        Log.d(TAG, "Launching deep link: $uriString")

        val uri = Uri.parse(uriString)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Check if any app can handle this intent
        val canResolve = intent.resolveActivity(context.packageManager) != null
        Log.d(TAG, "Can resolve intent: $canResolve")

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch ${bank.displayName}: ${e.message}")
            Toast.makeText(
                context,
                "${bank.displayName} app not found. Is it installed?",
                Toast.LENGTH_LONG,
            ).show()
            false
        }
    }

    private fun dispatchClipboard(invoice: Invoice, iban: String): Boolean {
        val payload = uriBuilder.buildClipboardPayload(
            iban = iban,
            amount = invoice.amountResidual,
            name = invoice.partnerName,
            communication = invoice.paymentReference,
        )

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Payment details", payload))
        Toast.makeText(context, "Payment details copied to clipboard", Toast.LENGTH_SHORT).show()

        val intent = context.packageManager.getLaunchIntentForPackage("com.keytrade.mobile")
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Keytrade app not found", Toast.LENGTH_SHORT).show()
        }
        return true
    }
}
