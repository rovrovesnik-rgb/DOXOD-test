package com.doxod.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doxod.app.ui.theme.DarkSurfaceVariant
import com.doxod.app.ui.theme.GoldBorder
import com.doxod.app.ui.theme.GoldPrimary
import com.doxod.app.ui.theme.GoldSubtle
import com.doxod.app.ui.theme.SuccessGreen
import com.doxod.app.ui.theme.TextSecondary
import com.doxod.app.ui.theme.TextTertiary
import com.doxod.app.ui.util.DateUtils
import com.doxod.app.ui.util.MoneyUtils

/** Заголовок блока-недели: «Неделя dd-MM-yy», диапазон дней, чистая сумма. */
@Composable
fun BlockHeader(
    year: Int,
    month: Int,
    block: Int,
    netSum: Double,
    modifier: Modifier = Modifier,
) {
    val (d0, d1) = DateUtils.blockDays(year, month, block)
    val monday = DateUtils.blockMonday(year, month, block)
    val monthUpper = DateUtils.monthName(month).uppercase()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, GoldBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(GoldSubtle)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text(
                        "Неделя $monday",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "$d0–$d1 $monthUpper",
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("ЧИСТАЯ", fontSize = 10.sp, color = TextTertiary)
                Text(
                    MoneyUtils.formatWithRub(netSum),
                    style = MaterialTheme.typography.titleMedium,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
