package com.doxod.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.doxod.app.ui.theme.DarkSurfaceElevated
import com.doxod.app.ui.theme.GoldBorder
import com.doxod.app.ui.theme.GoldLight
import com.doxod.app.ui.theme.GoldPrimary
import com.doxod.app.ui.theme.TextPrimary
import com.doxod.app.ui.theme.TextSecondary

/** Нижняя панель: «Отчет», «Сохранить», «Загрузить» + время последней копии. */
@Composable
fun BottomBar(
    lastBackupText: String?,
    onReport: () -> Unit,
    onSaveBackup: () -> Unit,
    onLoadBackup: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(DarkSurface)
            .border(1.dp, GoldBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = if (lastBackupText == null) "Последняя копия: нет" else "Последняя копия: $lastBackupText",
            fontSize = 11.sp,
            color = if (lastBackupText == null) TextSecondary.copy(alpha = 0.7f) else GoldLight,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Отчет — залитая золотом, вес 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GoldPrimary)
                    .clickable(onClick = onReport),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_assessment),
                        contentDescription = null,
                        tint = DarkBackground,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Отчет", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
                }
            }
            // Сохранить — outlined золото, вес 1.2
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, GoldBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onSaveBackup),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cloud_upload),
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Сохранить", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = GoldLight)
                }
            }
            // Загрузить — outlined вторичная, вес 1.2
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, DarkSurfaceElevated, RoundedCornerShape(10.dp))
                    .clickable(onClick = onLoadBackup),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cloud_download),
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Загрузить", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                }
            }
        }
    }
}
