package com.ubimatic.payunpaids.domain.model

data class Invoice(
    val id: Int,
    val name: String,
    val partnerName: String,
    val amountResidual: Double,
    val invoiceDateDue: String?,
    val invoiceDate: String?,
    val paymentReference: String?,
    val partnerIban: String?,
    val state: String,
    val pdfData: ByteArray? = null,
    val pdfFilename: String? = null,
    // Overrides (from PDF extraction or manual entry)
    val overrideIban: String? = null,
    val overrideCommunication: String? = null,
) {
    /** Effective IBAN: override > Odoo field */
    val effectiveIban: String?
        get() = overrideIban?.takeIf { it.isNotBlank() } ?: partnerIban

    /** Effective communication: override > Odoo field */
    val effectiveCommunication: String?
        get() = overrideCommunication?.takeIf { it.isNotBlank() } ?: paymentReference

    val hasStructuredCommunication: Boolean
        get() = !effectiveCommunication.isNullOrBlank()

    val hasIban: Boolean
        get() = !effectiveIban.isNullOrBlank()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Invoice) return false
        return id == other.id
    }

    override fun hashCode(): Int = id
}
