package com.ubimatic.payunpaids.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ubimatic.payunpaids.data.local.CredentialStore
import com.ubimatic.payunpaids.data.remote.OdooException
import com.ubimatic.payunpaids.data.remote.OdooXmlRpcClient
import com.ubimatic.payunpaids.domain.model.Bank
import com.ubimatic.payunpaids.payment.PaymentDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val url: String = "",
    val username: String = "",
    val apiKey: String = "",
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val testSuccess: Boolean = false,
    val isSaved: Boolean = false,
    val installedBanks: List<Bank> = emptyList(),
    val selectedBank: Bank? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val credentialStore: CredentialStore,
    private val odooClient: OdooXmlRpcClient,
    private val paymentDispatcher: PaymentDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCredentials()
        detectInstalledBanks()
    }

    private fun loadCredentials() {
        val savedBankName = credentialStore.getSelectedBank()
        val savedBank = savedBankName?.let { name -> Bank.entries.find { it.name == name } }
        _uiState.update {
            it.copy(
                url = credentialStore.getUrl(),
                username = credentialStore.getUsername(),
                apiKey = credentialStore.getApiKey(),
                selectedBank = savedBank,
            )
        }
    }

    private fun detectInstalledBanks() {
        val installed = paymentDispatcher.getInstalledBanks()
        _uiState.update { state ->
            state.copy(
                installedBanks = installed,
                selectedBank = state.selectedBank ?: installed.firstOrNull(),
            )
        }
    }

    fun updateUrl(url: String) {
        _uiState.update { it.copy(url = url, testResult = null, isSaved = false) }
    }

    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username, testResult = null, isSaved = false) }
    }

    fun updateApiKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey, testResult = null, isSaved = false) }
    }

    fun selectBank(bank: Bank) {
        _uiState.update { it.copy(selectedBank = bank, isSaved = false) }
    }

    fun testConnection() {
        val state = _uiState.value
        if (state.url.isBlank() || state.username.isBlank() || state.apiKey.isBlank()) {
            _uiState.update { it.copy(testResult = "All fields are required", testSuccess = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }
            try {
                val url = state.url.trimEnd('/')
                val host = android.net.Uri.parse(url).host ?: ""
                val db = host.substringBefore(".")

                val version = odooClient.version(url)
                val uid = odooClient.authenticate(url, db, state.username, state.apiKey)
                if (uid != null) {
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            testResult = "Connected (UID: $uid, DB: $db, Odoo $version)",
                            testSuccess = true,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            testResult = "Odoo $version reachable, but login rejected (DB: $db). Check username and API key.",
                            testSuccess = false,
                        )
                    }
                }
            } catch (e: OdooException) {
                _uiState.update {
                    it.copy(isTesting = false, testResult = "Odoo error: ${e.message}", testSuccess = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isTesting = false, testResult = "Connection failed: ${e.message}", testSuccess = false)
                }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        credentialStore.save(state.url, state.username, state.apiKey)
        state.selectedBank?.let { credentialStore.saveSelectedBank(it.name) }
        _uiState.update { it.copy(isSaved = true) }
    }
}
