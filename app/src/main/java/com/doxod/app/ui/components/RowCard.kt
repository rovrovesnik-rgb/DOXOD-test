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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doxod.app.R
import com.doxod.app.data.WorkRow
import com.doxod.app.ui.Field
import com.doxod.app.ui.theme.DarkBackground
import com.doxod.app.ui.theme.DarkSurface
import com.doxod.app.ui.theme.DarkSurfaceElevated
import com.doxod.app.ui.theme.ErrorRed
import com.doxod.app.ui.theme.GoldBorder
import com.doxod.app.ui.theme.GoldLight
import com.doxod.app.ui.theme.GoldPrimary
import com.doxod.app.ui.theme.SuccessGreen
import com.doxod.app.ui.theme.TextPrimary
import com.doxod.app.ui.theme.TextSecondary
import com.doxod.app.ui.theme.TextTertiary
import com.doxod.app.ui.util.DateUtils
import com.doxod.app.ui.util.MoneyUtils

/**
 * Карточка строки. Значения полей берутся напрямую из WorkRow (единый источник),
 * запись — через onEdit/commit, с debounce в ViewModel, чтобы курсор не прыгал.
 */
@Composable
fun RowCard(
    row: WorkRow,
    number: Int,
    isDraft: Boolean,
    onEdit: (Field, String) -> Unit,
    onCommit: (Field) -> Unit,
    onAddFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onViewPhotos: () -> Unit,
    onDelete: () -> Unit,
) {
    val filled = !isDraft
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) DarkSurface else Color(0x991B1926)) // DarkSurfaceVariant alpha 0.6
            .border(
                width = if (filled) 1.dp else 0.5.dp,
                color = if (filled) GoldBorder else DarkSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            // Верхний ряд: номер, дата, кнопки
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (filled) GoldPrimary else DarkSurfaceElevated),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        number.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (filled) DarkBackground else TextSecondary,
                    )
                }
                Spacer(Modifier.width(8.dp))
                LabeledField(
                    label = "Дата",
                    value = row.date,
                    placeholder = "ДД-ММ-ГГ",
                    modifier = Modifier.width(115.dp).height(52.dp),
                    textStyle = TextStyle(fontSize = 13.sp, color = TextPrimary),
                    error = !DateUtils.isValidDate(row.date),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    onValueChange = { nv -> onEdit(Field.DATE, DateUtils.onDateInput(row.date, nv)) },
                    onFocusChanged = { hasFocus -> if (!hasFocus) onCommit(Field.DATE) },
                )
                Spacer(Modifier.weight(1f))
                IconBtn(R.drawable.ic_add_photo, GoldPrimary, onAddFromGallery)
                Spacer(Modifier.width(4.dp))
                IconBtn(R.drawable.ic_camera, GoldLight, onTakePhoto)
                Spacer(Modifier.width(4.dp))
                // collections с бейджем
                Box {
                    IconBtn(
                        R.drawable.ic_collections,
                        if (row.photoCount > 0) GoldPrimary else TextTertiary,
                        onViewPhotos,
                    )
                    if (row.photoCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 1.dp, end = 1.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary),
                        ) {
                            Text(
                                row.photoCount.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBackground,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                IconBtn(R.drawable.ic_delete, if (filled) ErrorRed else TextTertiary, iconSize = 18, onClick = onDelete)
            }

            Spacer(Modifier.height(8.dp))

            LabeledField(
                label = "Модель",
                value = row.model,
                placeholder = "Например: iPhone 13 Pro",
                modifier = Modifier.fillMaxWidth().height(52.dp),
                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary),
                error = false,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                onValueChange = { nv -> onEdit(Field.MODEL, nv) },
                onFocusChanged = {},
            )
            Spacer(Modifier.height(6.dp))
            LabeledField(
                label = "Работы",
                value = row.work,
                placeholder = "Замена дисплея, чистка сеточки...",
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp, max = 130.dp),
                textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, lineHeight = 18.sp),
                error = false,
                minLines = 2,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                onValueChange = { nv -> onEdit(Field.WORK, nv) },
                onFocusChanged = {},
            )
            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LabeledField(
                    label = "ЧИСТАЯ",
                    value = row.net,
                    placeholder = "1 234,5",
                    modifier = Modifier.weight(1f).height(56.dp),
                    textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SuccessGreen),
                    labelColor = SuccessGreen,
                    labelBold = true,
                    focusGold = false,
                    error = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    onValueChange = { nv -> onEdit(Field.NET, MoneyUtils.onMoneyInput(nv)) },
                    onFocusChanged = { hasFocus -> if (!hasFocus) onCommit(Field.NET) },
                )
                LabeledField(
                    label = "ГРЯЗНАЯ",
                    value = row.gross,
                    placeholder = "1 234,5",
                    modifier = Modifier.weight(1f).height(56.dp),
                    textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = GoldLight),
                    labelColor = GoldLight,
                    labelBold = false,
                    focusGold = true,
                    error = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    onValueChange = { nv -> onEdit(Field.GROSS, MoneyUtils.onMoneyInput(nv)) },
                    onFocusChanged = { hasFocus -> if (!hasFocus) onCommit(Field.GROSS) },
                )
            }
        }
    }
}

@Composable
private fun IconBtn(iconRes: Int, tint: Color, onClick: () -> Unit, iconSize: Int = 20) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize.dp))
    }
}

/** Поле с плавающим label и рамкой; значение всегда приходит из state (single source of truth). */
@Composable
private fun LabeledField(
    label: String,
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle,
    labelColor: Color = TextTertiary,
    labelBold: Boolean = false,
    focusGold: Boolean = true,
    error: Boolean,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        error -> ErrorRed
        focused && focusGold -> GoldPrimary
        focused -> SuccessGreen
        else -> GoldBorder
    }
    Column(modifier = modifier) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = if (labelBold) FontWeight.Bold else if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkBackground)
                .border(width = if (focused || error) 1.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = textStyle,
                keyboardOptions = keyboardOptions,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(if (focusGold) GoldPrimary else SuccessGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusEvent { st ->
                        if (st.hasFocus != focused) {
                            focused = st.hasFocus
                            onFocusChanged(st.hasFocus)
                        }
                    },
                decorationBox = { inner: @Composable () -> Unit ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (minLines > 1) Alignment.TopStart else Alignment.CenterStart,
                    ) {
                        if (value.isEmpty()) {
                            Text(placeholder, fontSize = 13.sp, color = TextTertiary, maxLines = 1)
                        }
                        inner()
                    }
                },
            )
        }
    }
}

