package com.ubimatic.payunpaids.domain.model

enum class Bank(
    val displayName: String,
    val packageName: String,
    val deepLinkScheme: String?,
) {
    ING("ING", "com.ing.banking", "ing-homebank"),
    BNP_PARIBAS_FORTIS("BNP Paribas Fortis", "com.bnpparibasfortis.easybanking", "bnpparibasfortis"),
    KBC("KBC", "com.kbc.mobilebanking", "kbc-mobile"),
    BELFIUS("Belfius", "be.belfius.directmobile", "belfius"),
    KEYTRADE("Keytrade", "com.keytrade.mobile", null),
}
