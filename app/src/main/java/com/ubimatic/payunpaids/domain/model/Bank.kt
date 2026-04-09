package com.ubimatic.payunpaids.domain.model

enum class Bank(
    val displayName: String,
    val packageNames: List<String>,
    val deepLinkScheme: String?,
) {
    ING("ING", listOf("com.ing.banking", "com.ing.mobile.banking.android.activity"), "ing-homebank"),
    BNP_PARIBAS_FORTIS("BNP Paribas Fortis", listOf("com.bnpparibasfortis.easybanking", "be.bnpparibasfortis.easybanking"), "bnpparibasfortis"),
    KBC("KBC", listOf("com.kbc.mobilebanking"), "kbc-mobile"),
    BELFIUS("Belfius", listOf("be.belfius.directmobile"), "belfius"),
    KEYTRADE("Keytrade", listOf("com.keytrade.mobile"), null),
}
