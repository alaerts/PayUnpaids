package com.ubimatic.payunpaids.ui.alldone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubimatic.payunpaids.domain.model.SessionStats
import com.ubimatic.payunpaids.ui.theme.Amber
import com.ubimatic.payunpaids.ui.theme.AmberDark
import com.ubimatic.payunpaids.ui.theme.DarkBorder
import com.ubimatic.payunpaids.ui.theme.DarkBorderLight
import com.ubimatic.payunpaids.ui.theme.DarkSurface
import com.ubimatic.payunpaids.ui.theme.Green
import com.ubimatic.payunpaids.ui.theme.TextMuted
import com.ubimatic.payunpaids.ui.theme.TextPrimary
import com.ubimatic.payunpaids.ui.theme.TextSecondary

@Composable
fun AllDoneScreen(
    stats: SessionStats,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Green check circle
        Column(
            modifier = Modifier
                .size(68.dp)
                .background(Green.copy(alpha = 0.15f), CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "\u2713",
                fontSize = 30.sp,
                color = Green,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "All caught up",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
        )

        Text(
            text = "No unpaid invoices remaining",
            fontSize = 13.sp,
            color = TextSecondary,
        )

        if (stats.total > 0) {
            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                if (stats.autoPaid > 0) {
                    StatRow("Auto-paid (SnapAndMail)", stats.autoPaid, Green)
                }
                if (stats.paidViaBank > 0) {
                    StatRow("Paid via banking app", stats.paidViaBank, TextPrimary)
                }
                if (stats.markedManually > 0) {
                    StatRow("Marked paid manually", stats.markedManually, TextPrimary)
                }

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Total processed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                    )
                    Text(
                        text = "${stats.total} invoices",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Amber,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Last synced: just now",
            fontSize = 11.sp,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(
                containerColor = Amber,
                contentColor = AmberDark,
            ),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                "\u21BA Refresh",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun StatRow(label: String, count: Int, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(
            text = count.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
        )
    }
}
