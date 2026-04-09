package com.ubimatic.payunpaids.ui.invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubimatic.payunpaids.domain.model.Bank
import com.ubimatic.payunpaids.ui.theme.DarkBorder
import com.ubimatic.payunpaids.ui.theme.DarkBorderLight
import com.ubimatic.payunpaids.ui.theme.DarkSurface
import com.ubimatic.payunpaids.ui.theme.TextPrimary
import com.ubimatic.payunpaids.ui.theme.TextSecondary

private data class BankBrand(
    val label: String,
    val background: Color,
    val textColor: Color = Color.White,
    val subtitle: String? = null,
)

private val bankBrands = mapOf(
    Bank.ING to BankBrand("ING", Color(0xFFE87722)),
    Bank.BNP_PARIBAS_FORTIS to BankBrand("BNP", Color(0xFF004B28)),
    Bank.KBC to BankBrand("KBC", Color(0xFF009FE3)),
    Bank.BELFIUS to BankBrand("BEL", Color(0xFFCC0033)),
    Bank.KEYTRADE to BankBrand(
        "KEY",
        DarkBorder,
        TextPrimary,
        "Copies details to clipboard",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentBottomSheet(
    amount: Double,
    onBankSelected: (Bank) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = "Pay \u20AC${String.format("%.2f", amount)}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
            )
            Text(
                text = "Choose your banking app",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Bank.entries.forEach { bank ->
                val brand = bankBrands[bank] ?: return@forEach
                val isKeytrade = bank == Bank.KEYTRADE

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 7.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(brand.background)
                        .then(
                            if (isKeytrade) Modifier.background(DarkBorder)
                            else Modifier
                        )
                        .clickable { onBankSelected(bank) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isKeytrade) Color(0xFFF0F0F0) else Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = brand.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isKeytrade) Color(0xFF444444) else brand.background,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = bank.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = brand.textColor,
                            )
                            if (brand.subtitle != null) {
                                Text(
                                    text = brand.subtitle,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                    Text(
                        text = "\u2192",
                        fontSize = 12.sp,
                        color = brand.textColor.copy(alpha = 0.5f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
