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
        val iban = invoice.effectiveIban ?: return false

        // Strategy 1: BEP interop deep link (works across all Belgian banks)
        if (tryBepPayment(invoice, iban)) return true

        // Strategy 2: bank-specific deep link
        if (bank.deepLinkScheme != null) {
            val uriString = uriBuilder.buildUriString(
                bank = bank, iban = iban,
                amount = invoice.amountResidual,
                name = invoice.partnerName,
                communication = invoice.effectiveCommunication,
            )
            if (uriString != null && tryLaunchUri(uriString)) return true
        }

        // Strategy 3: clipboard + launch banking app by package name
        return dispatchClipboard(invoice, bank, iban)
    }

    private fun tryBepPayment(invoice: Invoice, iban: String): Boolean {
        val bepUri = uriBuilder.buildBepUri(
            iban = iban,
            amount = invoice.amountResidual,
            name = invoice.partnerName,
            communication = invoice.effectiveCommunication,
        )
        Log.d(TAG, "Trying BEP deep link: $bepUri")
        return tryLaunchUri(bepUri)
    }

    private fun tryLaunchUri(uriString: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val canResolve = intent.resolveActivity(context.packageManager) != null
        Log.d(TAG, "URI: $uriString → can resolve: $canResolve")

        if (!canResolve) return false

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Launch failed: ${e.message}")
            false
        }
    }

    private fun dispatchClipboard(invoice: Invoice, bank: Bank, iban: String): Boolean {
        val payload = uriBuilder.buildClipboardPayload(
            iban = iban,
            amount = invoice.amountResidual,
            name = invoice.partnerName,
            communication = invoice.effectiveCommunication,
        )

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Payment details", payload))

        for (pkg in bank.packageNames) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                Log.d(TAG, "Clipboard fallback: launching ${bank.displayName} via $pkg")
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

        Log.w(TAG, "No package found for ${bank.displayName}")
        Toast.makeText(context, "${bank.displayName} is not installed", Toast.LENGTH_LONG).show()
        return false
    }
}
