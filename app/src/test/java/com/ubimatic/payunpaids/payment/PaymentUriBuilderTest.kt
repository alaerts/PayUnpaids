package com.ubimatic.payunpaids.payment

import com.ubimatic.payunpaids.domain.model.Bank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentUriBuilderTest {

    private val builder = PaymentUriBuilder()

    private val iban = "BE68539007547034"
    private val amount = 1234.56
    private val name = "Supplier NV"
    private val communication = "+++123/4567/89012+++"

    @Test
    fun `ING deep link has correct scheme and params`() {
        val uri = builder.buildUriString(Bank.ING, iban, amount, name, communication)
        assertNotNull(uri)
        assertTrue(uri!!.startsWith("ing-homebank://payment?"))
        assertTrue(uri.contains("iban=$iban"))
        assertTrue(uri.contains("amount=1234.56"))
        assertTrue(uri.contains("currency=EUR"))
    }

    @Test
    fun `BNP deep link has correct scheme`() {
        val uri = builder.buildUriString(Bank.BNP_PARIBAS_FORTIS, iban, amount, name, communication)
        assertNotNull(uri)
        assertTrue(uri!!.startsWith("bnpparibasfortis://payment?"))
    }

    @Test
    fun `KBC deep link has correct scheme`() {
        val uri = builder.buildUriString(Bank.KBC, iban, amount, name, communication)
        assertNotNull(uri)
        assertTrue(uri!!.startsWith("kbc-mobile://payment?"))
    }

    @Test
    fun `Belfius deep link has correct scheme`() {
        val uri = builder.buildUriString(Bank.BELFIUS, iban, amount, name, communication)
        assertNotNull(uri)
        assertTrue(uri!!.startsWith("belfius://payment?"))
    }

    @Test
    fun `Keytrade returns null URI`() {
        val uri = builder.buildUriString(Bank.KEYTRADE, iban, amount, name, communication)
        assertNull(uri)
    }

    @Test
    fun `clipboard payload contains all fields`() {
        val payload = builder.buildClipboardPayload(iban, amount, name, communication)
        assertTrue(payload.contains(iban))
        assertTrue(payload.contains("1234.56"))
        assertTrue(payload.contains(name))
        assertTrue(payload.contains(communication))
    }

    @Test
    fun `deep link without communication omits parameter`() {
        val uri = builder.buildUriString(Bank.ING, iban, amount, name, null)
        assertNotNull(uri)
        assertTrue(!uri!!.contains("communication"))
    }

    @Test
    fun `all banks produce correct scheme`() {
        val expected = mapOf(
            Bank.ING to "ing-homebank",
            Bank.BNP_PARIBAS_FORTIS to "bnpparibasfortis",
            Bank.KBC to "kbc-mobile",
            Bank.BELFIUS to "belfius",
        )
        expected.forEach { (bank, scheme) ->
            val uri = builder.buildUriString(bank, iban, amount, name, communication)
            assertNotNull("$bank should produce a URI", uri)
            assertEquals("$bank scheme", scheme, uri!!.substringBefore("://"))
        }
    }
}
