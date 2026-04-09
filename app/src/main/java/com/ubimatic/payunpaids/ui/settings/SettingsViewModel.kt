package com.ubimatic.payunpaids.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ubimatic.payunpaids.data.local.CredentialStore
import com.ubimatic.payunpaids.data.remote.OdooException
import com.ubimatic.payunpaids.data.remote.OdooXmlRpcClient
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
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val credentialStore: CredentialStore,
    private val odooClient: OdooXmlRpcClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCredentials()
    }

    private fun loadCredentials() {
        _uiState.update {
            it.copy(
                url = credentialStore.getUrl(),
                username = credentialStore.getUsername(),
                apiKey = credentialStore.getApiKey(),
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

                android.util.Log.d("SettingsVM", "Authenticating: url=$url db=$db user=${state.username}")

                // Step 1: verify endpoint is reachable
                val version = odooClient.version(url)
                android.util.Log.d("SettingsVM", "Odoo version: $version")

                // Step 2: list databases to verify DB name
                val databases = odooClient.listDatabases(url)
                android.util.Log.d("SettingsVM", "Available databases: $databases")
                android.util.Log.d("SettingsVM", "Using DB: $db, match=${databases.contains(db)}")

                // Step 3: authenticate
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
        _uiState.update { it.copy(isSaved = true) }
    }
}
