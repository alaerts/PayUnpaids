package com.ubimatic.payunpaids.domain

import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractedPaymentInfo(
    val ibans: List<String>,
    val communications: List<String>,
    val fullText: String,
)

@Singleton
class PdfTextExtractor @Inject constructor() {

    companion object {
        private const val TAG = "PdfTextExtractor"

        // Belgian IBAN: BE + 2 check digits + 12 digits
        private val IBAN_REGEX = Regex(
            """[A-Z]{2}\d{2}[\s]?\d{4}[\s]?\d{4}[\s]?\d{4}(?:[\s]?\d{0,4})?""",
        )

        // Belgian structured communication: +++ddd/dddd/ddddd+++ or ***ddd/dddd/ddddd***
        private val STRUCTURED_COMM_REGEX = Regex(
            """[+*]{3}\d{3}/\d{4}/\d{5}[+*]{3}""",
        )
    }

    fun extract(pdfData: ByteArray): ExtractedPaymentInfo {
        return try {
            val doc = PDDocument.load(ByteArrayInputStream(pdfData))
            val stripper = PDFTextStripper()
            val fullText = stripper.getText(doc)
            doc.close()

            val ibans = IBAN_REGEX.findAll(fullText)
                .map { it.value.replace("\\s".toRegex(), "") }
                .distinct()
                .toList()

            val comms = STRUCTURED_COMM_REGEX.findAll(fullText)
                .map { it.value }
                .distinct()
                .toList()

            Log.d(TAG, "Extracted ${ibans.size} IBANs, ${comms.size} communications")
            ExtractedPaymentInfo(ibans, comms, fullText)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract text from PDF: ${e.message}")
            ExtractedPaymentInfo(emptyList(), emptyList(), "")
        }
    }
}
