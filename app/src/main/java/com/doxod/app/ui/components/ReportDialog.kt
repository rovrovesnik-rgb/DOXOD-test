package com.doxod.app.ui.components

import android.content.Context
import android.content.Intent
import android.print.PrintManager
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.doxod.app.R
import com.doxod.app.ui.theme.DarkBackground
import com.doxod.app.ui.theme.DarkSurface
import com.doxod.app.ui.theme.DarkSurfaceElevated
import com.doxod.app.ui.theme.GoldBorder
import com.doxod.app.ui.theme.GoldLight
import com.doxod.app.ui.theme.GoldPrimary
import com.doxod.app.ui.theme.TextSecondary
import java.io.File

/**
 * Полноэкранный предпросмотр HTML-отчёта: WebView (JS выключен),
 * кнопки «Печать / PDF», «Поделиться», «Сохранить».
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ReportDialog(
    title: String,
    fileName: String,
    html: String,
    onClose: () -> Unit,
    onSave: (suggestedName: String) -> Unit,
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            color = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleLarge, color = GoldLight)
                        Text("Предпросмотр документа", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_close), "Закрыть", tint = GoldLight, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))

                // Белый контейнер под WebView
                val webViewRef = remember { mutableStateOf<WebView?>(null) }
                DisposableEffect(Unit) {
                    onDispose { webViewRef.value?.destroy() }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, DarkSurfaceElevated, RoundedCornerShape(12.dp)),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                settings.javaScriptEnabled = false
                                setBackgroundColor(0xFFFFFFFF.toInt())
                                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                                webViewRef.value = this
                            }
                        },
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionBtn(modifier = Modifier.weight(1f), icon = R.drawable.ic_print, label = "Печать / PDF", filled = true) {
                        doPrint(context, webViewRef.value, fileName.removeSuffix(".html"))
                    }
                    ActionBtn(modifier = Modifier.weight(1f), icon = R.drawable.ic_share, label = "Поделиться", filled = false) {
                        shareHtml(context, fileName, html)
                    }
                    ActionBtn(modifier = Modifier.weight(1f), icon = R.drawable.ic_save, label = "Сохранить", filled = false) {
                        onSave(fileName)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionBtn(
    modifier: Modifier = Modifier,
    icon: Int,
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (filled) Modifier.background(GoldPrimary)
                else Modifier.border(1.dp, GoldBorder, RoundedCornerShape(10.dp))
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(icon), null,
                tint = if (filled) DarkBackground else GoldPrimary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (filled) FontWeight.Bold else FontWeight.Medium,
                color = if (filled) DarkBackground else GoldLight,
                maxLines = 1,
            )
        }
    }
}

private fun doPrint(context: Context, webView: WebView?, docName: String) {
    val wv = webView ?: return
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    try {
        printManager.print(docName, wv.createPrintDocumentAdapter(docName), null)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Не удалось открыть печать", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun shareHtml(context: Context, fileName: String, html: String) {
    try {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val f = File(dir, fileName)
        f.writeText(html, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Отчет: ${fileName.removeSuffix(".html").removePrefix("Отчет_")}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться отчетом"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Не удалось поделиться", android.widget.Toast.LENGTH_SHORT).show()
    }
}
