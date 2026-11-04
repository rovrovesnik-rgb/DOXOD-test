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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.doxod.app.ui.theme.DarkSurface
import com.doxod.app.ui.theme.ErrorRed
import com.doxod.app.ui.theme.GoldBorder
import com.doxod.app.ui.theme.GoldLight
import com.doxod.app.ui.theme.GoldPrimary
import com.doxod.app.ui.theme.TextSecondary

/** Диалог подтверждения в стиле приложения (тёмная поверхность, скругление 16 dp). */
@Composable
fun ConfirmDialog(
    title: String,
    titleColor: Color = GoldLight,
    message: String,
    confirmText: String,
    confirmColor: Color = GoldPrimary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .border(1.dp, GoldBorder, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = titleColor)
            Spacer(Modifier.height(10.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = ArrangementEnd,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("Отмена", fontWeight = FontWeight.Medium, color = TextSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(confirmColor)
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(confirmText, fontWeight = FontWeight.Bold, color = Color.Black.takeIf { false } ?: DarkSurfaceCopy)
                }
            }
        }
    }
}

private val DarkSurfaceCopy = Color(0xFF0B0A0F)

private val ArrangementEnd = androidx.compose.foundation.layout.Arrangement.End

/** Простой информационный диалог с одной кнопкой. */
@Composable
fun InfoDialog(
    title: String,
    message: String,
    buttonText: String = "Понятно",
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .border(1.dp, GoldBorder, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = ErrorRed)
            Spacer(Modifier.height(10.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Start)
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = ArrangementEnd) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(GoldPrimary)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(buttonText, fontWeight = FontWeight.Bold, color = DarkSurfaceCopy)
                }
            }
        }
    }
}
