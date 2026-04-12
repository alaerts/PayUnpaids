package com.ubimatic.payunpaids.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface InvoiceOverrideDao {

    @Query("SELECT * FROM invoice_overrides WHERE invoiceId = :invoiceId")
    suspend fun getOverride(invoiceId: Int): InvoiceOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: InvoiceOverrideEntity)

    @Query("DELETE FROM invoice_overrides WHERE createdAt < :cutoff")
    suspend fun deleteExpired(cutoff: Long = System.currentTimeMillis() - InvoiceOverrideEntity.TTL_MILLIS)
}
