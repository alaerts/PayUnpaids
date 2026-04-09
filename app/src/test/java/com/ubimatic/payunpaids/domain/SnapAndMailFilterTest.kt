package com.ubimatic.payunpaids.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapAndMailFilterTest {

    private val filter = SnapAndMailFilter()

    @Test
    fun `matches valid snap and mail filename`() {
        assertTrue(filter.isSnapAndMail("snapandmail_2024-03-15_143022.pdf"))
    }

    @Test
    fun `matches another valid filename`() {
        assertTrue(filter.isSnapAndMail("snapandmail_2023-12-01_090000.pdf"))
    }

    @Test
    fun `rejects regular PDF filename`() {
        assertFalse(filter.isSnapAndMail("invoice_2024.pdf"))
    }

    @Test
    fun `rejects partial match`() {
        assertFalse(filter.isSnapAndMail("snapandmail_2024-03-15.pdf"))
    }

    @Test
    fun `rejects wrong extension`() {
        assertFalse(filter.isSnapAndMail("snapandmail_2024-03-15_143022.txt"))
    }

    @Test
    fun `rejects empty string`() {
        assertFalse(filter.isSnapAndMail(""))
    }

    @Test
    fun `rejects similar but wrong prefix`() {
        assertFalse(filter.isSnapAndMail("snap_and_mail_2024-03-15_143022.pdf"))
    }
}
