package com.ubimatic.payunpaids.payment

import com.ubimatic.payunpaids.domain.model.Bank
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentUriBuilder @Inject constructor() {

    /**
     * Builds a BEP (Belgian Electronic Payment) URI that triggers the system
     * app chooser showing all installed Belgian banking apps.
     * Scheme: bepgenapp://dotx?iban=...&amount=...&currency=EUR&name=...&communication=...
     */
    fun buildBepUri(
        iban: String,
        amount: Double,
        name: String,
        communication: String?,
    ): String {
        val amountStr = String.format("%.2f", amount)
        val params = mutableListOf(
            "iban" to iban,
            "amount" to amountStr,
            "currency" to "EUR",
            "name" to name,
        )
        if (!communication.isNullOrBlank()) {
            params.add("communication" to communication)
        }
        val query = params.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, "UTF-8")}"
        }
        return "bepgenapp://dotx?$query"
    }

    /**
     * Builds a bank-specific deep link URI (legacy fallback).
     */
    fun buildUriString(
        bank: Bank,
        iban: String,
        amount: Double,
        name: String,
        communication: String?,
    ): String? {
        val scheme = bank.deepLinkScheme ?: return null
        val amountStr = String.format("%.2f", amount)
        val params = mutableListOf(
            "iban" to iban,
            "amount" to amountStr,
            "currency" to "EUR",
            "name" to name,
        )
        if (!communication.isNullOrBlank()) {
            params.add("communication" to communication)
        }
        val query = params.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, "UTF-8")}"
        }
        return "$scheme://payment?$query"
    }

    fun buildClipboardPayload(
        iban: String,
        amount: Double,
        name: String,
        communication: String?,
    ): String {
        return buildString {
            appendLine("IBAN: $iban")
            appendLine("Amount: ${String.format("%.2f", amount)} EUR")
            appendLine("Name: $name")
            if (!communication.isNullOrBlank()) {
                appendLine("Communication: $communication")
            }
        }
    }
}
