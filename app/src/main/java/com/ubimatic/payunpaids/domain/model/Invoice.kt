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
) {
    val hasStructuredCommunication: Boolean
        get() = !paymentReference.isNullOrBlank()

    val hasIban: Boolean
        get() = !partnerIban.isNullOrBlank()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Invoice) return false
        return id == other.id
    }

    override fun hashCode(): Int = id
}
