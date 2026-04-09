package com.ubimatic.payunpaids.ui.invoice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ubimatic.payunpaids.data.local.CredentialStore
import com.ubimatic.payunpaids.data.repository.SyncRepository
import com.ubimatic.payunpaids.domain.model.Bank
import com.ubimatic.payunpaids.domain.model.Invoice
import com.ubimatic.payunpaids.domain.model.SessionStats
import com.ubimatic.payunpaids.payment.PaymentDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvoiceUiState(
    val invoices: List<Invoice> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showPaySheet: Boolean = false,
    val allDone: Boolean = false,
    val stats: SessionStats = SessionStats(),
    val hasCredentials: Boolean = true,
    val pendingPayment: Boolean = false,
)

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val paymentDispatcher: PaymentDispatcher,
    private val credentialStore: CredentialStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceUiState())
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()

    val currentInvoice: Invoice?
        get() {
            val state = _uiState.value
            return state.invoices.getOrNull(state.currentIndex)
        }

    init {
        checkCredentialsAndSync()
    }

    fun checkCredentialsAndSync() {
        if (!credentialStore.hasCredentials()) {
            _uiState.update { it.copy(hasCredentials = false, isLoading = false) }
            return
        }
        _uiState.update { it.copy(hasCredentials = true) }
        sync()
    }

    fun sync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val (invoices, autoPayCount) = syncRepository.sync()
                _uiState.update {
                    it.copy(
                        invoices = invoices,
                        currentIndex = 0,
                        isLoading = false,
                        allDone = invoices.isEmpty(),
                        stats = it.stats.copy(autoPaid = autoPayCount),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
            }
        }
    }

    fun goToNext() {
        _uiState.update { state ->
            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.invoices.size) {
                state.copy(allDone = true)
            } else {
                state.copy(currentIndex = nextIndex)
            }
        }
    }

    fun goToPrevious() {
        _uiState.update { state ->
            val prevIndex = (state.currentIndex - 1).coerceAtLeast(0)
            state.copy(currentIndex = prevIndex)
        }
    }

    fun markCurrentAsPaid() {
        val invoice = currentInvoice ?: return
        viewModelScope.launch {
            try {
                syncRepository.markAsPaid(invoice.id)
                _uiState.update { state ->
                    val newInvoices = state.invoices.filter { it.id != invoice.id }
                    val newIndex = state.currentIndex.coerceAtMost((newInvoices.size - 1).coerceAtLeast(0))
                    state.copy(
                        invoices = newInvoices,
                        currentIndex = newIndex,
                        allDone = newInvoices.isEmpty(),
                        stats = state.stats.copy(markedManually = state.stats.markedManually + 1),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showPaySheet() {
        _uiState.update { it.copy(showPaySheet = true) }
    }

    fun hidePaySheet() {
        _uiState.update { it.copy(showPaySheet = false) }
    }

    fun payWithBank(bank: Bank) {
        val invoice = currentInvoice ?: return
        _uiState.update { it.copy(showPaySheet = false, pendingPayment = true) }
        paymentDispatcher.dispatch(invoice, bank)
    }

    fun onResumeFromPayment() {
        if (!_uiState.value.pendingPayment) return
        _uiState.update { it.copy(pendingPayment = false) }

        val invoice = currentInvoice ?: return
        viewModelScope.launch {
            try {
                syncRepository.markAsPaid(invoice.id)
                _uiState.update { state ->
                    val newInvoices = state.invoices.filter { it.id != invoice.id }
                    val newIndex = state.currentIndex.coerceAtMost((newInvoices.size - 1).coerceAtLeast(0))
                    state.copy(
                        invoices = newInvoices,
                        currentIndex = newIndex,
                        allDone = newInvoices.isEmpty(),
                        stats = state.stats.copy(paidViaBank = state.stats.paidViaBank + 1),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
