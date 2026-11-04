package com.doxod.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.doxod.app.data.Backup
import com.doxod.app.ui.MainViewModel
import com.doxod.app.ui.components.BottomBar
import com.doxod.app.ui.components.BlockHeader
import com.doxod.app.ui.components.ConfirmDialog
import com.doxod.app.ui.components.GalleryDialog
import com.doxod.app.ui.components.Header
import com.doxod.app.ui.components.InfoDialog
import com.doxod.app.ui.components.ReportDialog
import com.doxod.app.ui.components.RowCard
import com.doxod.app.ui.theme.DarkBackground
import com.doxod.app.ui.theme.DoXoDTheme
import com.doxod.app.ui.theme.ErrorRed
import com.doxod.app.ui.util.DateUtils
import java.io.File

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    /** Версия состояния диалогов вне Compose — растёт при изменениях из колбэков. */
    private val dialogTick = mutableStateOf(0)
    private val mainHandler = Handler(Looper.getMainLooper())
    private fun bumpTick() = mainHandler.post { dialogTick.value++ }

    // Цели выбора фото
    private var galleryTargetRow: String? = null
    private var cameraTargetRow: String? = null
    private var cameraFile: File? = null

    // Диаложные состояния (вне Compose, читаются в Composition через dialogTick)
    private var backupDialogInfo: MainViewModel.BackupInfo? = null
    private var importPreviewData: MainViewModel.ImportPreview? = null
    private var errorDialogText: String? = null
    private var reportHtmlCache: String? = null

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val rowId = galleryTargetRow
        galleryTargetRow = null
        if (rowId == null || uris.isNullOrEmpty()) return@registerForActivityResult
        addPhotosFromUris(rowId, uris)
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val rowId = cameraTargetRow
        val file = cameraFile
        cameraTargetRow = null
        cameraFile = null
        if (rowId == null) return@registerForActivityResult
        if (ok && file != null && file.exists()) {
            addPhotosFromUris(rowId, listOf(Uri.fromFile(file)))
        }
    }

    // CreateDocument с MIME через переопределение createIntent (сигнатура Activity 1.5.1)
    private val createJsonDoc = registerForActivityResult(createDocContract("application/json")) { uri ->
        if (uri != null) writePending(uri)
    }

    private val createHtmlDoc = registerForActivityResult(createDocContract("text/html")) { uri ->
        if (uri != null) writePending(uri)
    }

    private val openJsonDoc = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) readImport(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DoXoDTheme { MainScreen() }
        }
    }

    // ---------- Фото ----------

    private fun launchGallery(rowId: String) {
        galleryTargetRow = rowId
        try {
            pickImages.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось открыть галерею", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchCamera(rowId: String) {
        try {
            val f = File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            cameraFile = f
            cameraTargetRow = rowId
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
            takePicture.launch(uri)
        } catch (e: Exception) {
            cameraTargetRow = null
            cameraFile = null
            Toast.makeText(this, "Не удалось открыть камеру", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addPhotosFromUris(rowId: String, uris: List<Uri>) {
        vm.persistRowNow(rowId) {
            Thread {
                val bytesList = ArrayList<ByteArray>()
                for (u in uris) {
                    try {
                        contentResolver.openInputStream(u)?.use { ins ->
                            val b = ins.readBytes()
                            if (b.isNotEmpty()) bytesList.add(b)
                        }
                    } catch (_: Exception) {
                    }
                }
                runOnUiThread {
                    if (bytesList.isEmpty()) {
                        errorDialogText = "Не удалось загрузить выбранные фото"
                        bumpTick()
                    } else {
                        vm.addPhotos(rowId, bytesList) { n ->
                            if (n > 0) Toast.makeText(this, "Добавлено фото: $n", Toast.LENGTH_SHORT).show()
                            else {
                                errorDialogText = "Не удалось загрузить выбранные фото"
                                bumpTick()
                            }
                        }
                    }
                }
            }.start()
        }
    }

    // ---------- Бэкап ----------

    private fun startSaveBackup() {
        vm.prepareExport { res ->
            res.onSuccess { info ->
                backupDialogInfo = info; bumpTick()
            }.onFailure { e ->
                errorDialogText = "Ошибка сохранения: ${e.message}"; bumpTick()
            }
        }
    }

    private fun confirmSaveBackup() {
        val info = backupDialogInfo ?: return
        backupDialogInfo = null
        val name = Backup.suggestedFileName(vm.state.value.year, vm.state.value.month)
        val bytes = info.json.toByteArray(Charsets.UTF_8)
        pendingPayload = bytes to {
            vm.onBackupWritten()
            Toast.makeText(this, "Резервная копия успешно сохранена", Toast.LENGTH_SHORT).show()
        }
        createJsonDoc.launch(name.removeSuffix(".json"))
    }

    private var pendingPayload: Pair<ByteArray, (() -> Unit)?> = ByteArray(0) to null

    private fun writePending(uri: Uri) {
        val (bytes, onDone) = pendingPayload
        pendingPayload = ByteArray(0) to null
        Thread {
            try {
                contentResolver.openOutputStream(uri)?.use { os -> os.write(bytes) }
                    ?: throw IllegalStateException("Нет доступа к файлу")
                runOnUiThread {
                    onDone?.invoke()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    errorDialogText = "Ошибка сохранения: ${e.message}"
                    bumpTick()
                }
            }
        }.start()
    }

    private fun startLoadBackup() {
        try {
            openJsonDoc.launch(arrayOf("application/json", "*/*"))
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось открыть проводник", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readImport(uri: Uri) {
        Thread {
            val bytes = try {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) { null }
            if (bytes == null) {
                runOnUiThread { errorDialogText = "Это не файл копии"; bumpTick() }
                return@Thread
            }
            val text = String(bytes, Charsets.UTF_8)
            runOnUiThread {
                vm.parseImport(text, bytes.size.toLong()) { res ->
                    res.onSuccess { p -> importPreviewData = p; bumpTick() }
                        .onFailure { errorDialogText = "Это не файл копии"; bumpTick() }
                }
            }
        }.start()
    }

    private fun applyImportConfirmed() {
        val preview = importPreviewData ?: return
        importPreviewData = null
        vm.applyImport(preview) { ok, msg ->
            if (ok) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            else { errorDialogText = msg; bumpTick() }
        }
    }

    private fun saveReportFile(name: String) {
        val html = reportHtmlCache ?: return
        pendingPayload = html.toByteArray(Charsets.UTF_8) to {
            Toast.makeText(this, "Отчет сохранен", Toast.LENGTH_SHORT).show()
        }
        createHtmlDoc.launch(name.removeSuffix(".html"))
    }

    // ---------- Compose ----------

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Composable
    private fun MainScreen() {
        val state by vm.state.collectAsState()
        val tick = dialogTick.value // перечитываем внешние диаложные флаги при bumpTick

        var deleteRowId by remember { mutableStateOf<String?>(null) }
        var deletePhotoId by remember { mutableStateOf<String?>(null) }
        var showReport by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = DarkBackground,
            bottomBar = {
                BottomBar(
                    lastBackupText = state.lastBackupMs?.let { DateUtils.formatBackupTime(it) },
                    onReport = {
                        reportHtmlCache = vm.buildReportHtml()
                        showReport = true
                    },
                    onSaveBackup = { startSaveBackup() },
                    onLoadBackup = { startLoadBackup() },
                )
            },
        ) { innerPadding ->
            val blocks = DateUtils.blockCount(state.year, state.month)
            val (x, y) = vm.stats()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Header(
                        year = state.year,
                        month = state.month,
                        onlyPhotos = state.onlyPhotos,
                        rowsWithPhoto = x,
                        filledRows = y,
                        onPrevYear = { vm.changeYear(-1) },
                        onNextYear = { vm.changeYear(1) },
                        onMonth = { vm.setMonth(it) },
                        onPrevMonth = { vm.prevMonthWrap() },
                        onNextMonth = { vm.nextMonthWrap() },
                        onToggleFilter = { vm.setOnlyPhotos(it) },
                    )
                }
                for (b in 0 until blocks) {
                    val rows = vm.rowsForBlock(b)
                    if (state.onlyPhotos && rows.none { it.photoCount > 0 }) continue
                    item(key = "hdr-$b") {
                        BlockHeader(
                            year = state.year,
                            month = state.month,
                            block = b,
                            netSum = vm.blockNetSum(b),
                        )
                    }
                    itemsIndexed(rows, key = { _, r -> r.id }) { i, r ->
                        RowCard(
                            row = r,
                            number = i + 1,
                            isDraft = !r.isFilled(),
                            onEdit = { f, v -> vm.editField(r.id, f, v) },
                            onCommit = { f -> vm.commitField(r.id, f) },
                            onAddFromGallery = { launchGallery(r.id) },
                            onTakePhoto = { launchCamera(r.id) },
                            onViewPhotos = { vm.openGallery(r.id) },
                            onDelete = {
                                if (vm.isRowFilled(r.id)) deleteRowId = r.id else vm.deleteRow(r.id)
                            },
                        )
                    }
                }
            }
        }

        // Галерея строки
        state.galleryRowId?.let { rid ->
            GalleryDialog(
                photos = state.galleryThumbs,
                oldestFirst = state.galleryOldestFirst,
                onSetOrder = { vm.setGalleryOrder(it) },
                onClose = { vm.closeGallery() },
                onAddFromGallery = { launchGallery(rid) },
                onTakePhoto = { launchCamera(rid) },
                onDeletePhoto = { pid -> deletePhotoId = pid },
                onLoadBlob = { pid, cb -> vm.loadPhotoBlob(pid, cb) },
            )
        }

        deletePhotoId?.let { pid ->
            ConfirmDialog(
                title = "Удаление фото",
                message = "Удалить это фото?",
                confirmText = "Удалить",
                confirmColor = ErrorRed,
                onConfirm = { deletePhotoId = null; vm.deletePhoto(pid) },
                onDismiss = { deletePhotoId = null },
            )
        }

        deleteRowId?.let { rid ->
            ConfirmDialog(
                title = "Удаление записи",
                message = "Удалить запись и все прикрепленные фото?",
                confirmText = "Удалить",
                confirmColor = ErrorRed,
                onConfirm = { deleteRowId = null; vm.deleteRow(rid) },
                onDismiss = { deleteRowId = null },
            )
        }

        if (showReport) {
            ReportDialog(
                title = "Отчет: ${DateUtils.monthName(state.month)} ${state.year}",
                fileName = DateUtils.reportFileName(state.year, state.month),
                html = reportHtmlCache ?: "",
                onClose = { showReport = false },
                onSave = { name -> saveReportFile(name) },
            )
        }

        backupDialogInfo?.let { info ->
            ConfirmDialog(
                title = "Сохранить копию",
                message = "Копия за ${DateUtils.monthName(state.month)} ${state.year}.\n" +
                        "Строк: ${info.rowCount} · фото: ${info.photoCount} · примерно ${info.sizeMb} МБ",
                confirmText = "Продолжить",
                onConfirm = { confirmSaveBackup() },
                onDismiss = { backupDialogInfo = null; bumpTick() },
            )
        }

        importPreviewData?.let { p ->
            val base = "Данные за ${DateUtils.monthName(p.parsed.m)} ${p.parsed.y} будут заменены данными из файла. " +
                    "Остальные месяцы не изменятся.\n\nФото в файле: ${p.parsed.photos.size}, " +
                    "будет удалено локальных фото: ${p.localPhotosToRemove}."
            ConfirmDialog(
                title = "Загрузить копию",
                message = if (p.tooBig) "⚠️ Внимание: размер файла больше 120 МБ. Обработка может занять время.\n\n$base" else base,
                confirmText = "Заменить",
                onConfirm = { applyImportConfirmed() },
                onDismiss = { importPreviewData = null; bumpTick() },
            )
        }

        errorDialogText?.let { msg ->
            InfoDialog(title = "Ошибка", message = msg, onDismiss = { errorDialogText = null; bumpTick() })
        }

        // предотвратить unused warning для tick
        if (tick < 0) return
    }
}

/**
 * CreateDocument с MIME: в activity-compose 1.5.1 конструктор CreateDocument(mimeType)
 * отсутствует, а createIntent принимает только input — поэтому свой простой контракт
 * на базе ACTION_CREATE_DOCUMENT.
 */
private class CreateDocContract(private val mimeType: String) : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: android.content.Context, input: String): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            putExtra(Intent.EXTRA_TITLE, input)
            setType(mimeType)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
}

private fun createDocContract(mime: String) = CreateDocContract(mime)
