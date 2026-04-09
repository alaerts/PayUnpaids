package com.ubimatic.payunpaids.ui.invoice

import com.ubimatic.payunpaids.data.local.CredentialStore
import com.ubimatic.payunpaids.data.repository.SyncRepository
import com.ubimatic.payunpaids.domain.model.Invoice
import com.ubimatic.payunpaids.payment.PaymentDispatcher
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InvoiceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var syncRepository: SyncRepository
    private lateinit var paymentDispatcher: PaymentDispatcher
    private lateinit var credentialStore: CredentialStore

    private val testInvoices = listOf(
        Invoice(1, "INV/001", "Supplier A", 100.0, "2024-01-15", "2024-01-01", "+++123/4567/89012+++", "BE68539007547034", "posted"),
        Invoice(2, "INV/002", "Supplier B", 200.0, "2024-02-15", "2024-02-01", null, "BE68539007547035", "posted"),
        Invoice(3, "INV/003", "Supplier C", 300.0, "2024-03-15", "2024-03-01", "+++321/7654/21098+++", null, "posted"),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        syncRepository = mockk()
        paymentDispatcher = mockk()
        credentialStore = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): InvoiceViewModel {
        every { credentialStore.hasCredentials() } returns true
        coEvery { syncRepository.sync() } returns (testInvoices to 2)
        return InvoiceViewModel(syncRepository, paymentDispatcher, credentialStore)
    }

    @Test
    fun `initial sync loads invoices`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(3, state.invoices.size)
        assertEquals(0, state.currentIndex)
        assertFalse(state.isLoading)
        assertEquals(2, state.stats.autoPaid)
    }

    @Test
    fun `navigate next increments index`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.goToNext()
        assertEquals(1, vm.uiState.value.currentIndex)
    }

    @Test
    fun `navigate previous decrements index`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.goToNext()
        vm.goToPrevious()
        assertEquals(0, vm.uiState.value.currentIndex)
    }

    @Test
    fun `navigate previous does not go below zero`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.goToPrevious()
        assertEquals(0, vm.uiState.value.currentIndex)
    }

    @Test
    fun `navigate past last shows all done`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.goToNext()
        vm.goToNext()
        vm.goToNext()
        assertTrue(vm.uiState.value.allDone)
    }

    @Test
    fun `no credentials shows settings`() = runTest(testDispatcher) {
        every { credentialStore.hasCredentials() } returns false
        val vm = InvoiceViewModel(syncRepository, paymentDispatcher, credentialStore)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.hasCredentials)
    }

    @Test
    fun `mark paid removes invoice from list`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        coEvery { syncRepository.markAsPaid(1) } returns Unit
        vm.markCurrentAsPaid()
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.invoices.size)
        assertEquals(1, vm.uiState.value.stats.markedManually)
    }

    @Test
    fun `invoice without communication still has pay enabled if iban present`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.goToNext() // Move to INV/002 which has no communication but has IBAN
        val invoice = vm.currentInvoice!!
        assertFalse(invoice.hasStructuredCommunication)
        assertTrue(invoice.hasIban)
    }

    @Test
    fun `invoice without iban hides pay`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.goToNext()
        vm.goToNext() // Move to INV/003 which has no IBAN
        val invoice = vm.currentInvoice!!
        assertFalse(invoice.hasIban)
    }
}
