package com.ubimatic.payunpaids.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [InvoiceEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
}
