package com.ubimatic.payunpaids.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_NAME = "payunpaids_credentials"
        private const val KEY_URL = "odoo_url"
        private const val KEY_USERNAME = "odoo_username"
        private const val KEY_API_KEY = "odoo_api_key"
        private const val KEY_SELECTED_BANK = "selected_bank"
    }

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun hasCredentials(): Boolean {
        return getUrl().isNotBlank() && getUsername().isNotBlank() && getApiKey().isNotBlank()
    }

    fun getUrl(): String = prefs.getString(KEY_URL, "") ?: ""
    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""
    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""

    fun getDatabase(): String {
        val url = getUrl()
        // Extract subdomain: https://mycompany.odoo.com -> mycompany
        return try {
            val host = android.net.Uri.parse(url).host ?: ""
            host.substringBefore(".")
        } catch (e: Exception) {
            ""
        }
    }

    fun getSelectedBank(): String? = prefs.getString(KEY_SELECTED_BANK, null)

    fun saveSelectedBank(bankName: String) {
        prefs.edit().putString(KEY_SELECTED_BANK, bankName).apply()
    }

    fun save(url: String, username: String, apiKey: String) {
        prefs.edit()
            .putString(KEY_URL, url.trimEnd('/'))
            .putString(KEY_USERNAME, username)
            .putString(KEY_API_KEY, apiKey)
            .apply()
    }
}
