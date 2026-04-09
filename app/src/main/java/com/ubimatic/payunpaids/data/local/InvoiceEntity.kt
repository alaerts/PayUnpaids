package com.ubimatic.payunpaids.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: Int,
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
    val isPaid: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InvoiceEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id
}
