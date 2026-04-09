package com.ubimatic.payunpaids.payment

import com.ubimatic.payunpaids.domain.model.Bank
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentUriBuilder @Inject constructor() {

    fun buildUriString(
        bank: Bank,
        iban: String,
        amount: Double,
        name: String,
        communication: String?,
    ): String? {
        val scheme = when (bank) {
            Bank.ING -> "ing-homebank"
            Bank.BNP_PARIBAS_FORTIS -> "bnpparibasfortis"
            Bank.KBC -> "kbc-mobile"
            Bank.BELFIUS -> "belfius"
            Bank.KEYTRADE -> return null
        }

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
