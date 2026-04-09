package com.ubimatic.payunpaids.data.repository

import com.ubimatic.payunpaids.data.local.InvoiceDao
import com.ubimatic.payunpaids.data.local.InvoiceEntity
import com.ubimatic.payunpaids.domain.model.Invoice
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val invoiceDao: InvoiceDao,
) {
    suspend fun sync(): Pair<List<Invoice>, Int> {
        val (invoices, autoPayCount) = invoiceRepository.fetchInvoicesWithAttachments()

        // Cache to local DB
        invoiceDao.clearAll()
        invoiceDao.insertAll(invoices.map { it.toEntity() })

        return invoices to autoPayCount
    }

    suspend fun markAsPaid(invoiceId: Int) {
        invoiceDao.markAsPaid(invoiceId)
        invoiceRepository.markAsPaid(invoiceId)
    }

    private fun Invoice.toEntity() = InvoiceEntity(
        id = id,
        name = name,
        partnerName = partnerName,
        amountResidual = amountResidual,
        invoiceDateDue = invoiceDateDue,
        invoiceDate = invoiceDate,
        paymentReference = paymentReference,
        partnerIban = partnerIban,
        state = state,
        pdfData = pdfData,
        pdfFilename = pdfFilename,
    )
}
