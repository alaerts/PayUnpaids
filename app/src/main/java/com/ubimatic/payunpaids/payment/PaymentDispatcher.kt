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
    fun getInstalledBanks(): List<Bank> {
        return Bank.entries.filter(::isBankInstalled)
    }

    fun isBankInstalled(bank: Bank): Boolean {
        return bank.packageNames.any { pkg ->
            context.packageManager.getLaunchIntentForPackage(pkg) != null
        }
    }

    fun dispatch(invoice: Invoice, bank: Bank): Boolean {
        val iban = invoice.partnerIban ?: return false

        // Try deep link first
        if (bank.deepLinkScheme != null) {
            val uriString = uriBuilder.buildUriString(
                bank = bank,
                iban = iban,
                amount = invoice.amountResidual,
                name = invoice.partnerName,
                communication = invoice.paymentReference,
            )
            if (uriString != null && tryDeepLink(uriString)) {
                return true
            }
            Log.d(TAG, "Deep link failed for ${bank.displayName}, falling back to clipboard")
        }

        // Fallback: copy payment details to clipboard and open the banking app
        return dispatchClipboard(invoice, bank, iban)
    }

    private fun tryDeepLink(uriString: String): Boolean {
        Log.d(TAG, "Trying deep link: $uriString")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val canResolve = intent.resolveActivity(context.packageManager) != null
        Log.d(TAG, "Can resolve: $canResolve")

        if (!canResolve) return false

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Deep link launch failed: ${e.message}")
            false
        }
    }

    private fun dispatchClipboard(invoice: Invoice, bank: Bank, iban: String): Boolean {
        val payload = uriBuilder.buildClipboardPayload(
            iban = iban,
            amount = invoice.amountResidual,
            name = invoice.partnerName,
            communication = invoice.paymentReference,
        )

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Payment details", payload))

        // Try each known package name for this bank
        for (pkg in bank.packageNames) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                Log.d(TAG, "Launching ${bank.displayName} via package $pkg")
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                Toast.makeText(
                    context,
                    "Payment details copied \u2014 paste in ${bank.displayName}",
                    Toast.LENGTH_LONG,
                ).show()
                return true
            }
        }

        Log.w(TAG, "No package found for ${bank.displayName}: tried ${bank.packageNames}")
        Toast.makeText(
            context,
            "${bank.displayName} is not installed",
            Toast.LENGTH_LONG,
        ).show()
        return false
    }
}
