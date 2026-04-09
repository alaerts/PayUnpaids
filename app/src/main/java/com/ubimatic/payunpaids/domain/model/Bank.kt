package com.ubimatic.payunpaids.domain.model

enum class Bank(
    val displayName: String,
    val packageNames: List<String>,
    val deepLinkScheme: String?,
) {
    ING("ING", listOf("com.ing.banking"), "ing-homebank"),
    BNP_PARIBAS_FORTIS("BNP Paribas Fortis / Hello Bank", listOf("com.bnpp.easybanking", "com.bnpparibasfortis.easybanking"), "bnpparibasfortis"),
    KBC("KBC", listOf("com.kbc.mobile.android.phone", "com.kbc.mobilebanking"), "kbc-mobile"),
    BELFIUS("Belfius", listOf("be.belfius.directmobile"), "belfius"),
    KEYTRADE("Keytrade", listOf("be.keytradebank.phone", "com.keytrade.mobile"), null),
}
