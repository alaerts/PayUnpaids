package com.ubimatic.payunpaids.domain.model

data class SessionStats(
    val autoPaid: Int = 0,
    val paidViaBank: Int = 0,
    val markedManually: Int = 0,
) {
    val total: Int get() = autoPaid + paidViaBank + markedManually
}
