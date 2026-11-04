package com.doxod.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.doxod.app.R
import com.doxod.app.data.PhotoThumb
import com.doxod.app.ui.util.DateUtils
import com.doxod.app.ui.util.ImageUtils
import com.doxod.app.ui.theme.DarkBackground
import com.doxod.app.ui.theme.DarkSurface
import com.doxod.app.ui.theme.DarkSurfaceVariant
import com.doxod.app.ui.theme.ErrorRed
import com.doxod.app.ui.theme.GoldBorder
import com.doxod.app.ui.theme.GoldLight
import com.doxod.app.ui.theme.GoldPrimary
import com.doxod.app.ui.theme.TextSecondary
import com.doxod.app.ui.theme.TextTertiary

/**
 * Полноэкранная фотогалерея строки: сетка 3 колонки, чипы порядка, добавление, удаление.
 */
@Composable
fun GalleryDialog(
    photos: List<PhotoThumb>,
    oldestFirst: Boolean,
    onSetOrder: (Boolean) -> Unit,
    onClose: () -> Unit,
    onAddFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onDeletePhoto: (String) -> Unit,
    onLoadBlob: (String, (ByteArray?) -> Unit) -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder),
        ) {
            var viewerIndex by remember { mutableStateOf<Int?>(null) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
            ) {
                // Заголовок + кнопка закрытия
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Фотогалерея", style = MaterialTheme.typography.titleLarge, color = GoldLight)
                        Text(
                            "Прикреплено фото: ${photos.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Закрыть",
                            tint = GoldLight,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                // Чипы порядка (Box+clickable, без FilterChip)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrderChip("Сначала старые", selected = oldestFirst) { onSetOrder(true) }
                    OrderChip("Сначала новые", selected = !oldestFirst) { onSetOrder(false) }
                }
                Spacer(Modifier.height(10.dp))

                if (photos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Нет прикрепленных фото", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Сетка 3 колонки через chunked(3) (без LazyVerticalGrid)
                        val chunks = photos.chunked(3)
                        items(count = chunks.size) { ci ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (p in chunks[ci]) {
                                    ThumbCell(
                                        modifier = Modifier.weight(1f),
                                        photo = p,
                                        onClick = { viewerIndex = photos.indexOf(p) },
                                        onDelete = { onDeletePhoto(p.id) },
                                    )
                                }
                                // пустые ячейки для неполного ряда
                                repeat(3 - chunks[ci].size) {
                                    Spacer(Modifier.weight(1f).height(1.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, GoldBorder, RoundedCornerShape(10.dp))
                            .clickable(onClick = onAddFromGallery),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_add_photo), null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Из галереи", color = GoldLight, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, GoldBorder, RoundedCornerShape(10.dp))
                            .clickable(onClick = onTakePhoto),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_camera), null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Камера", color = GoldLight, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Полноэкранный просмотрщик поверх галереи
            viewerIndex?.let { startIdx ->
                FullscreenViewer(
                    photos = photos,
                    startIndex = startIdx.coerceIn(0, photos.size - 1),
                    onLoadBlob = onLoadBlob,
                    onDelete = { id ->
                        onDeletePhoto(id)
                        // если это было последнее фото — закрыть просмотр
                        if (photos.size <= 1) viewerIndex = null
                    },
                    onClose = { viewerIndex = null },
                )
            }
        }
    }
}

@Composable
private fun OrderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) GoldPrimary.copy(alpha = 0.15f) else DarkSurfaceVariant)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) GoldPrimary else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) GoldLight else TextSecondary,
        )
    }
}

@Composable
private fun ThumbCell(modifier: Modifier = Modifier, photo: PhotoThumb, onClick: () -> Unit, onDelete: () -> Unit) {
    val bmp = remember(photo.thumb) { ImageUtils.decodeBitmap(photo.thumb) }
    DisposableEffect(bmp) {
        onDispose { bmp?.recycle() }
    }
    Box(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkBackground)
            .border(1.dp, GoldBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        if (bmp != null) {
            androidx.compose.foundation.Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = photo.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // подпись времени
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xCC0B0A0F))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        ) {
            Text(DateUtils.formatThumbTime(photo.added), fontSize = 9.sp, color = GoldLight)
        }
        // кнопка удаления
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xCC0B0A0F))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = "Удалить фото",
                tint = ErrorRed,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

/**
 * Просмотр одного фото: чёрный фон, свайпы влево-вправо, кнопки листания,
 * счётчик «i / n», удалить/закрыть. Blob грузится отдельно по id.
 */
@Composable
private fun FullscreenViewer(
    photos: List<PhotoThumb>,
    startIndex: Int,
    onLoadBlob: (String, (ByteArray?) -> Unit) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    var index by remember { mutableStateOf(startIndex) }
    var state by remember { mutableStateOf<Pair<String, Any?>>("" to null) } // photoId -> bitmap|error
    val current = photos.getOrNull(index) ?: run { onClose(); return }

    // загрузка blob при смене индекса
    LaunchedEffect(current.id) {
        onLoadBlob(current.id) { bytes ->
            val bmp = bytes?.let { ImageUtils.decodeBitmap(it) }
            state = current.id to (bmp ?: "error")
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .pointerInput(current.id, photos.size) {
                    detectHorizontalDragGestures(
                        onDragEnd = { /* порог в onHorizontalDrag */ },
                    ) { _, dragAmount ->
                        // листаем по куму не делаем — реагируем на каждый жест кратко
                    }
                },
        ) {
            // центр: фото или текст ошибки
            val st = state
            Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                when {
                    st.first != current.id -> Text("Загрузка…", color = TextTertiary)
                    st.second == "error" -> Text("Не удалось загрузить фото", color = ErrorRed)
                    else -> {
                        val bmp = st.second as? android.graphics.Bitmap
                        if (bmp != null && !bmp.isRecycled) {
                            androidx.compose.foundation.Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = current.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            // свайп: отдельный слой с накоплением смещения
            var acc by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(current.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (acc < -80f && index < photos.size - 1) index++
                                else if (acc > 80f && index > 0) index--
                                acc = 0f
                            },
                            onDragCancel = { acc = 0f },
                        ) { _, dragAmount -> acc += dragAmount }
                    },
            ) {}

            // верхняя плашка со счётчиком
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC13121B))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text("${index + 1} / ${photos.size}", color = GoldLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            // круглые кнопки справа сверху: удалить, закрыть
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoundActionButton(R.drawable.ic_delete, ErrorRed) { onDelete(current.id) }
                RoundActionButton(R.drawable.ic_close, GoldLight) { onClose() }
            }

            // кнопки листания
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC13121B))
                        .clickable { index-- },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_chevron_left), "Назад", tint = GoldLight, modifier = Modifier.size(22.dp))
                }
            }
            if (index < photos.size - 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC13121B))
                        .clickable { index++ },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_chevron_right), "Вперёд", tint = GoldLight, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun RoundActionButton(iconRes: Int, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0xCC13121B))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), null, tint = tint, modifier = Modifier.size(20.dp))
    }
}
