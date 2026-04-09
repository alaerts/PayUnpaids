package com.ubimatic.payunpaids.domain

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapAndMailFilter @Inject constructor() {

    private val pattern = Regex("""snapandmail_\d{4}-\d{2}-\d{2}_\d{6}\.pdf""")

    fun isSnapAndMail(filename: String): Boolean {
        return pattern.matches(filename)
    }
}
