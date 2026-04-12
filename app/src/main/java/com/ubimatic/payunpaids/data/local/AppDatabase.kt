package com.ubimatic.payunpaids.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [InvoiceEntity::class, InvoiceOverrideEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceOverrideDao(): InvoiceOverrideDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS invoice_overrides (
                        invoiceId INTEGER NOT NULL PRIMARY KEY,
                        iban TEXT,
                        communication TEXT,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }
    }
}
