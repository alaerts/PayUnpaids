package com.ubimatic.payunpaids.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoices WHERE isPaid = 0 ORDER BY invoiceDateDue ASC, invoiceDate ASC")
    fun getUnpaidInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE isPaid = 0 ORDER BY invoiceDateDue ASC, invoiceDate ASC")
    suspend fun getUnpaidInvoicesList(): List<InvoiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<InvoiceEntity>)

    @Query("UPDATE invoices SET isPaid = 1 WHERE id = :id")
    suspend fun markAsPaid(id: Int)

    @Query("DELETE FROM invoices")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM invoices WHERE isPaid = 0")
    suspend fun getUnpaidCount(): Int
}
