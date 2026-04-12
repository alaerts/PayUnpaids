package com.ubimatic.payunpaids.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_overrides")
data class InvoiceOverrideEntity(
    @PrimaryKey val invoiceId: Int,
    val iban: String? = null,
    val communication: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val TTL_MILLIS = 4L * 30 * 24 * 60 * 60 * 1000 // ~4 months
    }

    val isExpired: Boolean
        get() = System.currentTimeMillis() - createdAt > TTL_MILLIS
}
