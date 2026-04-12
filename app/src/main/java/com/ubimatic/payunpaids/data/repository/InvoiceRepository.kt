package com.ubimatic.payunpaids.data.repository

import android.util.Base64
import android.util.Log
import com.ubimatic.payunpaids.data.local.CredentialStore
import com.ubimatic.payunpaids.data.remote.OdooXmlRpcClient
import com.ubimatic.payunpaids.domain.SnapAndMailFilter
import com.ubimatic.payunpaids.domain.model.Invoice
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(
    private val odooClient: OdooXmlRpcClient,
    private val credentialStore: CredentialStore,
    private val snapAndMailFilter: SnapAndMailFilter,
) {
    companion object {
        private const val TAG = "InvoiceRepository"
    }

    private var uid: Int? = null

    private val url: String get() = credentialStore.getUrl()
    private val db: String get() = credentialStore.getDatabase()
    private val username: String get() = credentialStore.getUsername()
    private val apiKey: String get() = credentialStore.getApiKey()

    suspend fun authenticate(): Int {
        val result = odooClient.authenticate(url, db, username, apiKey)
            ?: throw IllegalStateException("Authentication failed")
        uid = result
        return result
    }

    suspend fun fetchUnpaidInvoices(): List<Invoice> {
        val currentUid = uid ?: authenticate()

        val domain = listOf(
            listOf("state", "=", "posted"),
            listOf("payment_state", "not in", listOf("paid", "in_payment")),
            listOf("move_type", "=", "in_invoice"),
        )

        val fields = listOf(
            "id", "name", "partner_id", "amount_residual",
            "invoice_date_due", "payment_reference", "partner_bank_id",
            "invoice_date", "state",
        )

        Log.d(TAG, "Fetching unpaid invoices with domain: $domain")
        val records = odooClient.searchRead(
            url, db, currentUid, apiKey,
            model = "account.move",
            domain = domain,
            fields = fields,
            order = "invoice_date_due asc, invoice_date asc",
        )
        Log.d(TAG, "Fetched ${records.size} invoice records")
        records.forEachIndexed { i, r -> Log.d(TAG, "  [$i] id=${r["id"]} name=${r["name"]} amount=${r["amount_residual"]} state=${r["state"]}") }

        return records.map { record ->
            val partnerId = record["partner_id"]
            val partnerName = when (partnerId) {
                is List<*> -> partnerId.getOrNull(1)?.toString() ?: ""
                else -> ""
            }

            val partnerBankId = record["partner_bank_id"]
            val bankId = when (partnerBankId) {
                is List<*> -> (partnerBankId.getOrNull(0) as? Int)
                is Int -> partnerBankId
                else -> null
            }

            val iban = if (bankId != null) {
                fetchIban(currentUid, bankId)
            } else null

            Invoice(
                id = (record["id"] as? Int) ?: 0,
                name = record["name"]?.toString() ?: "",
                partnerName = partnerName,
                amountResidual = when (val amt = record["amount_residual"]) {
                    is Double -> amt
                    is Int -> amt.toDouble()
                    else -> 0.0
                },
                invoiceDateDue = record["invoice_date_due"]?.toString()?.takeIf { it != "false" },
                invoiceDate = record["invoice_date"]?.toString()?.takeIf { it != "false" },
                paymentReference = record["payment_reference"]?.toString()?.takeIf { it != "false" },
                partnerIban = iban,
                state = record["state"]?.toString() ?: "",
            )
        }
    }

    private suspend fun fetchIban(currentUid: Int, bankId: Int): String? {
        val banks = odooClient.read(
            url, db, currentUid, apiKey,
            model = "res.partner.bank",
            ids = listOf(bankId),
            fields = listOf("acc_number"),
        )
        return banks.firstOrNull()?.get("acc_number")?.toString()
    }

    suspend fun fetchAttachments(invoiceId: Int): List<Pair<String, ByteArray>> {
        val currentUid = uid ?: authenticate()

        val domain = listOf(
            listOf("res_model", "=", "account.move"),
            listOf("res_id", "=", invoiceId),
        )

        val attachments = odooClient.searchRead(
            url, db, currentUid, apiKey,
            model = "ir.attachment",
            domain = domain,
            fields = listOf("name", "datas"),
        )

        return attachments.mapNotNull { att ->
            val name = att["name"]?.toString() ?: return@mapNotNull null
            val datasBase64 = att["datas"]?.toString() ?: return@mapNotNull null
            if (datasBase64 == "false") return@mapNotNull null
            val data = Base64.decode(datasBase64, Base64.DEFAULT)
            name to data
        }
    }

    suspend fun fetchInvoicesWithAttachments(): Pair<List<Invoice>, Int> {
        val invoices = fetchUnpaidInvoices()
        Log.d(TAG, "Total unpaid invoices: ${invoices.size}")
        var autoPayCount = 0

        val remaining = invoices.mapNotNull { invoice ->
            val attachments = fetchAttachments(invoice.id)
            Log.d(TAG, "Invoice ${invoice.name}: ${attachments.size} attachments [${attachments.map { it.first }}]")

            // Check for snap-and-mail PDFs
            val isSnapAndMail = attachments.any { (filename, _) ->
                snapAndMailFilter.isSnapAndMail(filename)
            }

            if (isSnapAndMail) {
                Log.d(TAG, "Auto-marking invoice ${invoice.name} as paid (snap-and-mail)")
                markAsPaid(invoice.id)
                autoPayCount++
                null
            } else {
                // Attach the first PDF if available
                val pdf = attachments.firstOrNull { (name, _) -> name.endsWith(".pdf", ignoreCase = true) }
                invoice.copy(
                    pdfData = pdf?.second,
                    pdfFilename = pdf?.first,
                )
            }
        }

        return remaining to autoPayCount
    }

    suspend fun markAsPaid(invoiceId: Int, journalId: Int? = null) {
        val currentUid = uid ?: authenticate()

        try {
            val context = mapOf<String, Any>(
                "active_model" to "account.move",
                "active_ids" to listOf(invoiceId),
                "dont_redirect_to_payments" to true,
            )

            val wizardValues = mutableMapOf<String, Any>(
                "group_payment" to false,
                "payment_difference_handling" to "reconcile",
            )
            if (journalId != null) {
                wizardValues["journal_id"] = journalId
            }

            val wizardId = odooClient.create(
                url, db, currentUid, apiKey,
                model = "account.payment.register",
                values = wizardValues,
                context = context,
            )

            if (wizardId != null) {
                Log.d(TAG, "Created payment wizard $wizardId for invoice $invoiceId (journal=$journalId)")
                odooClient.callMethod(
                    url, db, currentUid, apiKey,
                    model = "account.payment.register",
                    method = "action_create_payments",
                    ids = listOf(wizardId),
                    context = context,
                )
                Log.d(TAG, "Payment created for invoice $invoiceId, reconciling...")

                // Force reconciliation: find unreconciled move lines and reconcile them
                reconcileInvoicePayment(currentUid, invoiceId)
            } else {
                Log.w(TAG, "Could not create payment wizard for invoice $invoiceId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register payment for invoice $invoiceId: ${e.message}")
            throw e
        }
    }

    /**
     * After creating a payment, reconcile the outstanding move lines so the
     * invoice transitions from "in_payment" to "paid".
     */
    private suspend fun reconcileInvoicePayment(currentUid: Int, invoiceId: Int) {
        try {
            // Find unreconciled payable/receivable lines on the invoice
            val invoiceLines = odooClient.searchRead(
                url, db, currentUid, apiKey,
                model = "account.move.line",
                domain = listOf(
                    listOf("move_id", "=", invoiceId),
                    listOf("account_type", "in", listOf("liability_payable", "asset_receivable")),
                    listOf("reconciled", "=", false),
                ),
                fields = listOf("id"),
            )
            Log.d(TAG, "Invoice $invoiceId: ${invoiceLines.size} unreconciled lines")

            if (invoiceLines.isEmpty()) {
                Log.d(TAG, "Invoice $invoiceId already reconciled")
                return
            }

            // Use js_assign_outstanding_line on the invoice to auto-match
            // This finds the matching payment line and reconciles it
            for (line in invoiceLines) {
                val lineId = (line["id"] as? Int) ?: continue
                try {
                    odooClient.callMethod(
                        url, db, currentUid, apiKey,
                        model = "account.move",
                        method = "js_assign_outstanding_line",
                        ids = listOf(invoiceId),
                        context = mapOf("line_id" to lineId),
                    )
                    Log.d(TAG, "Attempted reconciliation for line $lineId")
                } catch (e: Exception) {
                    Log.w(TAG, "js_assign_outstanding_line failed for line $lineId: ${e.message}")
                }
            }

            // Verify final state
            val result = odooClient.searchRead(
                url, db, currentUid, apiKey,
                model = "account.move",
                domain = listOf(listOf("id", "=", invoiceId)),
                fields = listOf("payment_state"),
            )
            val finalState = result.firstOrNull()?.get("payment_state")?.toString()
            Log.d(TAG, "Invoice $invoiceId final payment_state: $finalState")
        } catch (e: Exception) {
            Log.w(TAG, "Reconciliation attempt failed for invoice $invoiceId: ${e.message}")
            // Non-fatal: the payment was created, just not reconciled yet
        }
    }

    suspend fun fetchJournals(): List<Pair<Int, String>> {
        val currentUid = uid ?: authenticate()
        val journals = odooClient.searchRead(
            url, db, currentUid, apiKey,
            model = "account.journal",
            domain = listOf(listOf("type", "=", "bank")),
            fields = listOf("id", "name"),
        )
        return journals.map { j ->
            val id = (j["id"] as? Int) ?: 0
            val name = j["name"]?.toString() ?: ""
            id to name
        }
    }
}
