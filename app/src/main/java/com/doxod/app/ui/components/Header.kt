package com.doxod.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doxod.app.R
import com.doxod.app.ui.theme.DarkBackground
import com.doxod.app.ui.theme.DarkSurface
import com.doxod.app.ui.theme.DarkSurfaceVariant
import com.doxod.app.ui.theme.GoldBorder
import com.doxod.app.ui.theme.GoldLight
import com.doxod.app.ui.theme.GoldPrimary
import com.doxod.app.ui.theme.GoldSubtle
import com.doxod.app.ui.theme.TextPrimary
import com.doxod.app.ui.theme.TextSecondary
import com.doxod.app.ui.theme.TextTertiary
import com.doxod.app.ui.util.DateUtils

@Composable
fun GoldIconButton(
    iconRes: Int,
    tint: Color,
    sizeDp: Int,
    iconDp: Int,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconDp.dp),
        )
    }
}

/** Шапка экрана: бренд, выбор года/месяца, фильтр «Только с фото», счётчик. */
@Composable
fun Header(
    year: Int,
    month: Int,
    onlyPhotos: Boolean,
    rowsWithPhoto: Int,
    filledRows: Int,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onMonth: (Int) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleFilter: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(DarkSurface)
            .border(1.dp, GoldBorder, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Do", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("X", style = MaterialTheme.typography.headlineMedium, color = GoldPrimary, fontWeight = FontWeight.Bold)
                    Text("oD", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GoldSubtle)
                            .border(1.dp, GoldBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "${month + 1} ${DateUtils.monthName(month)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldLight,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text("Учет работ", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            }
            // Выбор года
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GoldIconButton(R.drawable.ic_chevron_left, GoldPrimary, 32, 20, Color.Transparent, onPrevYear)
                Text(
                    year.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(56.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                GoldIconButton(R.drawable.ic_chevron_right, GoldPrimary, 32, 20, Color.Transparent, onNextYear)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Ряд выбора месяца
        val scrollState = rememberScrollState()
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoldIconButton(R.drawable.ic_chevron_left, GoldPrimary, 36, 20, DarkSurfaceVariant, onPrevMonth)
            Spacer(Modifier.width(6.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (m in 0..11) {
                    val selected = m == month
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) GoldPrimary else DarkSurfaceVariant)
                            .clickable { onMonth(m) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            DateUtils.monthShort(m),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) DarkBackground else TextSecondary,
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            GoldIconButton(R.drawable.ic_chevron_right, GoldPrimary, 36, 20, DarkSurfaceVariant, onNextMonth)
        }
        LaunchedEffect(month) {
            // прокрутка к выбранному месяцу (~68 dp на таб)
            val target = month * 68
            scrollState.scrollToValue(target)
        }

        Spacer(Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Switch «Только с фото» (текст кликабельный)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleFilter(!onlyPhotos) },
            ) {
                SwitchSimple(checked = onlyPhotos, onToggle = { onToggleFilter(!onlyPhotos) })
                Spacer(Modifier.width(8.dp))
                Text(
                    "Только с фото",
                    fontSize = 13.sp,
                    color = if (onlyPhotos) GoldLight else TextSecondary,
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "Строк с фото: $rowsWithPhoto из $filledRows",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (rowsWithPhoto > 0) GoldLight else TextTertiary,
                )
            }
        }
    }
}

/** Прокрутка к значению без launch-зависимостей. */
private suspend fun androidx.compose.foundation.ScrollState.scrollToValue(px: Int) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { scrollTo(px) }
}

/** Простой switch через Box (без экспериментальных API). */
@Composable
private fun SwitchSimple(checked: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(CircleShape)
            .background(if (checked) GoldPrimary else DarkSurfaceVariant)
            .border(1.dp, GoldBorder, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (checked) DarkBackground else TextTertiary),
        )
    }
}
