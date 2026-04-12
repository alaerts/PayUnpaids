package com.ubimatic.payunpaids.di

import android.content.Context
import androidx.room.Room
import com.ubimatic.payunpaids.data.local.AppDatabase
import com.ubimatic.payunpaids.data.local.InvoiceDao
import com.ubimatic.payunpaids.data.local.InvoiceOverrideDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "payunpaids.db",
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideInvoiceDao(database: AppDatabase): InvoiceDao {
        return database.invoiceDao()
    }

    @Provides
    fun provideInvoiceOverrideDao(database: AppDatabase): InvoiceOverrideDao {
        return database.invoiceOverrideDao()
    }
}
