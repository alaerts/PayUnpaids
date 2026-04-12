package com.ubimatic.payunpaids.ui.invoice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ubimatic.payunpaids.data.local.CredentialStore
import com.ubimatic.payunpaids.data.local.InvoiceOverrideDao
import com.ubimatic.payunpaids.data.local.InvoiceOverrideEntity
import com.ubimatic.payunpaids.data.repository.SyncRepository
import com.ubimatic.payunpaids.domain.PdfTextExtractor
import com.ubimatic.payunpaids.domain.model.Bank
import com.ubimatic.payunpaids.domain.model.Invoice
import com.ubimatic.payunpaids.domain.model.SessionStats
import com.ubimatic.payunpaids.payment.PaymentDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class InvoiceUiState(
    val invoices: List<Invoice> = emptyList(),
    val paidIds: Set<Int> = emptySet(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showJournalPicker: Boolean = false,
    val showOverrideSheet: Boolean = false,
    val detectedIbans: List<String> = emptyList(),
    val detectedComms: List<String> = emptyList(),
    val journals: List<Pair<Int, String>> = emptyList(),
    val selectedJournalId: Int? = null,
    val selectedBank: Bank? = null,
    val hasCredentials: Boolean = true,
    val pendingPayment: Boolean = false,
    val stats: SessionStats = SessionStats(),
) {
    val allDone: Boolean
        get() = invoices.isNotEmpty() && paidIds.containsAll(invoices.map { it.id })

    val unpaidCount: Int
        get() = invoices.count { it.id !in paidIds }
}

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val paymentDispatcher: PaymentDispatcher,
    private val credentialStore: CredentialStore,
    private val overrideDao: InvoiceOverrideDao,
    private val pdfTextExtractor: PdfTextExtractor,
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
        val savedBankName = credentialStore.getSelectedBank()
        val savedBank = savedBankName?.let { name -> Bank.entries.find { it.name == name } }
        _uiState.update { it.copy(hasCredentials = true, selectedBank = savedBank) }
        sync()
    }

    fun sync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Clean up expired overrides
                overrideDao.deleteExpired()

                val (invoices, autoPayCount) = syncRepository.sync()
                val journals = try { syncRepository.fetchJournals() } catch (_: Exception) { emptyList() }

                // Apply stored overrides to invoices
                val invoicesWithOverrides = invoices.map { invoice ->
                    val override = overrideDao.getOverride(invoice.id)
                    if (override != null && !override.isExpired) {
                        invoice.copy(
                            overrideIban = override.iban,
                            overrideCommunication = override.communication,
                        )
                    } else invoice
                }

                // Auto-extract IBAN/communication from PDFs for invoices missing them
                val enrichedInvoices = invoicesWithOverrides.map { invoice ->
                    if (invoice.pdfData != null && (!invoice.hasIban || !invoice.hasStructuredCommunication)) {
                        autoExtract(invoice)
                    } else invoice
                }

                _uiState.update {
                    it.copy(
                        invoices = enrichedInvoices,
                        paidIds = emptySet(),
                        currentIndex = 0,
                        isLoading = false,
                        stats = SessionStats(autoPaid = autoPayCount),
                        journals = journals,
                        selectedJournalId = journals.firstOrNull()?.first,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
            }
        }
    }

    private suspend fun autoExtract(invoice: Invoice): Invoice {
        val pdfData = invoice.pdfData ?: return invoice
        val info = pdfTextExtractor.extract(pdfData)

        var updated = invoice
        if (!updated.hasIban && info.ibans.isNotEmpty()) {
            updated = updated.copy(overrideIban = info.ibans.first())
            saveOverride(updated)
        }
        if (!updated.hasStructuredCommunication && info.communications.isNotEmpty()) {
            updated = updated.copy(overrideCommunication = info.communications.first())
            saveOverride(updated)
        }
        return updated
    }

    private suspend fun saveOverride(invoice: Invoice) {
        overrideDao.upsert(
            InvoiceOverrideEntity(
                invoiceId = invoice.id,
                iban = invoice.overrideIban,
                communication = invoice.overrideCommunication,
            )
        )
    }

    /** Open the override sheet to let user pick IBAN/communication from PDF text */
    fun showOverrideSheet() {
        val invoice = currentInvoice ?: return
        viewModelScope.launch {
            val pdfData = invoice.pdfData
            val ibans: List<String>
            val comms: List<String>
            if (pdfData != null) {
                val info = withContext(Dispatchers.Default) { pdfTextExtractor.extract(pdfData) }
                ibans = info.ibans
                comms = info.communications
            } else {
                ibans = emptyList()
                comms = emptyList()
            }
            _uiState.update {
                it.copy(
                    showOverrideSheet = true,
                    detectedIbans = ibans,
                    detectedComms = comms,
                )
            }
        }
    }

    fun hideOverrideSheet() {
        _uiState.update { it.copy(showOverrideSheet = false) }
    }

    fun setOverrideIban(iban: String) {
        val invoice = currentInvoice ?: return
        val idx = _uiState.value.currentIndex
        _uiState.update { state ->
            val newInvoices = state.invoices.toMutableList()
            newInvoices[idx] = invoice.copy(overrideIban = iban)
            state.copy(invoices = newInvoices)
        }
        viewModelScope.launch { saveOverride(_uiState.value.invoices[idx]) }
    }

    fun setOverrideCommunication(comm: String) {
        val invoice = currentInvoice ?: return
        val idx = _uiState.value.currentIndex
        _uiState.update { state ->
            val newInvoices = state.invoices.toMutableList()
            newInvoices[idx] = invoice.copy(overrideCommunication = comm)
            state.copy(invoices = newInvoices)
        }
        viewModelScope.launch { saveOverride(_uiState.value.invoices[idx]) }
    }

    fun setCurrentIndex(index: Int) {
        _uiState.update { it.copy(currentIndex = index.coerceIn(0, (it.invoices.size - 1).coerceAtLeast(0))) }
    }

    fun goToNext() {
        _uiState.update { state ->
            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.invoices.size) state else state.copy(currentIndex = nextIndex)
        }
    }

    fun goToPrevious() {
        _uiState.update { state ->
            state.copy(currentIndex = (state.currentIndex - 1).coerceAtLeast(0))
        }
    }

    fun togglePaid() {
        val invoice = currentInvoice ?: return
        val isPaid = invoice.id in _uiState.value.paidIds

        if (isPaid) {
            _uiState.update { state ->
                state.copy(
                    paidIds = state.paidIds - invoice.id,
                    stats = state.stats.copy(markedManually = (state.stats.markedManually - 1).coerceAtLeast(0)),
                )
            }
        } else {
            val journals = _uiState.value.journals
            if (journals.size > 1) {
                _uiState.update { it.copy(showJournalPicker = true) }
            } else {
                doMarkPaid(invoice.id, journals.firstOrNull()?.first)
            }
        }
    }

    fun selectJournalAndPay(journalId: Int) {
        _uiState.update { it.copy(showJournalPicker = false, selectedJournalId = journalId) }
        val invoice = currentInvoice ?: return
        doMarkPaid(invoice.id, journalId)
    }

    fun hideJournalPicker() {
        _uiState.update { it.copy(showJournalPicker = false) }
    }

    private fun doMarkPaid(invoiceId: Int, journalId: Int?) {
        _uiState.update { state ->
            state.copy(
                paidIds = state.paidIds + invoiceId,
                stats = state.stats.copy(markedManually = state.stats.markedManually + 1),
            )
        }
        viewModelScope.launch {
            try {
                syncRepository.markAsPaid(invoiceId, journalId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Odoo: ${e.message}") }
            }
        }
    }

    fun pay() {
        val invoice = currentInvoice ?: return
        val bank = _uiState.value.selectedBank ?: return
        _uiState.update { it.copy(pendingPayment = true) }
        paymentDispatcher.dispatch(invoice, bank)
    }

    fun onResumeFromPayment() {
        if (!_uiState.value.pendingPayment) return
        _uiState.update { it.copy(pendingPayment = false) }

        val invoice = currentInvoice ?: return
        _uiState.update { state ->
            state.copy(
                paidIds = state.paidIds + invoice.id,
                stats = state.stats.copy(paidViaBank = state.stats.paidViaBank + 1),
            )
        }
        viewModelScope.launch {
            try {
                syncRepository.markAsPaid(invoice.id, _uiState.value.selectedJournalId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Odoo: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
