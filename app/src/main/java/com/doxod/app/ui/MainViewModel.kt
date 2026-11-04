package com.doxod.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.doxod.app.data.Backup
import com.doxod.app.data.Db
import com.doxod.app.data.PhotoFull
import com.doxod.app.data.PhotoThumb
import com.doxod.app.data.WorkRow
import com.doxod.app.ui.util.DateUtils
import com.doxod.app.ui.util.ImageUtils
import com.doxod.app.ui.util.MoneyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

/** Одно редактирование поля строки. */
enum class Field { DATE, MODEL, WORK, NET, GROSS }

data class UiState(
    val year: Int,
    val month: Int, // 0..11
    val rows: List<WorkRow>,
    val onlyPhotos: Boolean,
    val lastBackupMs: Long?,          // 0/null => нет копий
    val galleryRowId: String? = null,
    val galleryThumbs: List<PhotoThumb> = emptyList(),
    val galleryOldestFirst: Boolean = true,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val db = Db(app)
    private val prefs = app.getSharedPreferences("doxod_prefs", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val io = Dispatchers.IO

    private val cal = Calendar.getInstance()

    private val _state = MutableStateFlow(
        UiState(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH),
            rows = emptyList(),
            onlyPhotos = false,
            lastBackupMs = readLastBackup(),
        )
    )
    val state: StateFlow<UiState> = _state

    /** Стабильные UUID черновиков: ключ "y-m-block" -> id. */
    private val draftIds = HashMap<String, String>()

    /** Таймеры debounce на каждую строку. */
    private val saveTimers = HashMap<String, Job>()

    init {
        reloadMonth()
    }

    override fun onCleared() {
        super.onCleared()
        saveTimers.values.forEach { it.cancel() }
        scope.coroutineContext[Job]?.cancel()
    }

    private fun readLastBackup(): Long? {
        val s = prefs.getString("last_backup_time", null) ?: return null
        return s.toLongOrNull()
    }

    // ---------- Месяц / год ----------

    fun setMonth(m: Int) {
        if (m == _state.value.month) return
        switchMonth { _state.value.copy(month = m) }
    }

    fun nextMonthWrap() {
        val st = _state.value
        if (st.month == 11) { changeYear(1); setMonthDirect(0) } else setMonth(st.month + 1)
    }

    fun prevMonthWrap() {
        val st = _state.value
        if (st.month == 0) { changeYear(-1); setMonthDirect(11) } else setMonth(st.month - 1)
    }

    private fun setMonthDirect(m: Int) {
        switchMonth { _state.value.copy(month = m) }
    }

    fun changeYear(delta: Int) {
        switchMonth { _state.value.copy(year = _state.value.year + delta) }
    }

    fun jumpTo(y: Int, m: Int) {
        switchMonth { _state.value.copy(year = y, month = m) }
    }

    private fun switchMonth(update: () -> UiState) {
        scope.launch {
            flushAllTimersNow()
            _state.value = update()
            reloadMonth()
        }
    }

    private fun reloadMonth() {
        val y = _state.value.year
        val m = _state.value.month
        scope.launch {
            val rows = withIo { monthRowsWithDrafts(y, m) }
            _state.value = _state.value.copy(rows = rows)
        }
    }

    private suspend fun <T> withIo(block: suspend () -> T): T =
        kotlinx.coroutines.withContext(io) { block() }

    /** Строки месяца из БД + пустой черновик в конце каждого блока. */
    private fun monthRowsWithDrafts(y: Int, m: Int): List<WorkRow> {
        val saved = db.monthRows(y, m)
        val blocks = DateUtils.blockCount(y, m)
        val out = ArrayList<WorkRow>()
        for (b in 0 until blocks) {
            val blockSaved = saved.filter { it.block == b }.sortedBy { it.ord }
            out.addAll(blockSaved)
            val key = "$y-$m-$b"
            var draftId = draftIds[key] ?: UUID.randomUUID().toString().also { draftIds[key] = it }
            // если черновик стал сохранённой строкой — новый UUID для блока
            if (blockSaved.any { it.id == draftId }) {
                val newId = UUID.randomUUID().toString()
                draftIds[key] = newId
                out.add(emptyDraft(y, m, b, nextOrd(blockSaved), newId))
            } else if (draftId.isEmpty()) {
                // слот освобождён (удалена пустая сохранённая строка) — создаём новый черновик
                val newId = UUID.randomUUID().toString()
                draftIds[key] = newId
                out.add(emptyDraft(y, m, b, nextOrd(blockSaved), newId))
            } else {
                out.add(emptyDraft(y, m, b, nextOrd(blockSaved), draftId))
            }
        }
        return out
    }

    private fun nextOrd(blockSaved: List<WorkRow>): Int =
        (blockSaved.maxOfOrNull { it.ord } ?: 0) + 1

    private fun emptyDraft(y: Int, m: Int, b: Int, ord: Int, id: String) = WorkRow(
        id = id, y = y, m = m, block = b, ord = ord,
        date = "", dateSet = false, model = "", work = "", net = "", gross = "", photoCount = 0,
    )

    // ---------- Изменение полей ----------

    /** Сначала память (без прыжков курсора), затем запись в БД с debounce 250 мс на строку. */
    fun editField(rowId: String, field: Field, value: String) {
        val st = _state.value
        val idx = st.rows.indexOfFirst { it.id == rowId }
        if (idx < 0) return
        var r = st.rows[idx]
        val modelWasEmpty = r.model.isBlank()

        when (field) {
            Field.DATE -> r = r.copy(date = value, dateSet = value.isNotEmpty())
            Field.MODEL -> r = r.copy(model = value)
            Field.WORK -> r = r.copy(work = value)
            Field.NET -> r = r.copy(net = value)
            Field.GROSS -> r = r.copy(gross = value)
        }
        // первый ввод модели => дата = сегодня, если пуста и не выставлена вручную
        if (field == Field.MODEL && modelWasEmpty && value.isNotBlank() && r.date.isEmpty() && !r.dateSet) {
            r = r.copy(date = DateUtils.today(), dateSet = true)
        }
        // пересчёт заполненности без фото-индекса
        val filled = r.model.isNotBlank() || r.work.isNotBlank() || r.net.isNotBlank() || r.gross.isNotBlank() || r.photoCount > 0
        val newRows = ArrayList(st.rows)
        newRows[idx] = r
        _state.value = st.copy(rows = newRows)

        scheduleSave(r, filled)
    }

    /** Применить формат при потере фокуса (дата/деньги). */
    fun commitField(rowId: String, field: Field) {
        val r = _state.value.rows.firstOrNull { it.id == rowId } ?: return
        val cur = when (field) { Field.DATE -> r.date; Field.NET -> r.net; else -> r.gross }
        val formatted = when (field) {
            Field.DATE -> DateUtils.applyMask(cur)
            Field.NET -> MoneyUtils.formatOnBlur(cur)
            Field.GROSS -> MoneyUtils.formatOnBlur(cur)
            else -> return
        }
        if (formatted != cur) {
            // обновляем память немедленно и пишем в БД сразу (без debounce),
            // иначе отложенный таймер перезапишет результат старым значением
            saveTimers.remove(rowId)?.cancel()
            val idx = _state.value.rows.indexOfFirst { it.id == rowId }
            val nr = when (field) {
                Field.DATE -> r.copy(date = formatted)
                Field.NET -> r.copy(net = formatted)
                else -> r.copy(gross = formatted)
            }
            val newRows = ArrayList(_state.value.rows)
            newRows[idx] = nr
            _state.value = _state.value.copy(rows = newRows)
            scope.launch { withIo { db.upsertRow(nr) } }
        }
    }

    private fun scheduleSave(r: WorkRow, filled: Boolean) {
        saveTimers.remove(r.id)?.cancel()
        saveTimers[r.id] = scope.launch {
            delay(250)
            saveTimers.remove(r.id)
            withIo {
                if (!filled) {
                    db.deleteRowWithPhotos(r.id)
                } else {
                    db.upsertRow(r)
                }
            }
            // обновляем счётчики фото/заполненности без пересоздания списка
            refreshCounts()
        }
    }

    /** Незавершённые debounce-записи выполняем немедленно перед сменой месяца/экспортом. */
    private suspend fun flushAllTimersNow() {
        val ids = saveTimers.keys.toSet()
        saveTimers.values.forEach { it.cancel() }
        saveTimers.clear()
        if (ids.isEmpty()) return
        val snapshot = _state.value.rows.filter { it.id in ids }
        withIo {
            for (r in snapshot) {
                val filled = r.model.isNotBlank() || r.work.isNotBlank() || r.net.isNotBlank() ||
                        r.gross.isNotBlank() || db.photoCountForRow(r.id) > 0
                if (!filled) db.deleteRowWithPhotos(r.id) else db.upsertRow(r)
            }
        }
    }

    private fun WorkRow.isFilledForSave(db: Db): Boolean =
        model.isNotBlank() || work.isNotBlank() || net.isNotBlank() || gross.isNotBlank() || db.photoCountForRow(id) > 0

    private fun refreshCounts() {
        val y = _state.value.year; val m = _state.value.month
        scope.launch {
            val rows = withIo { monthRowsWithDrafts(y, m) }
            _state.value = _state.value.copy(rows = rows)
        }
    }

    // ---------- Удаление строк ----------

    fun isRowFilled(rowId: String): Boolean =
        _state.value.rows.firstOrNull { it.id == rowId }?.isFilled() == true

    fun deleteRow(rowId: String) {
        saveTimers.remove(rowId)?.cancel()
        val st = _state.value
        // Если удаляем сохранённую пустую строку (черновик под фото), освобождаем слот черновика блока
        val r = st.rows.firstOrNull { it.id == rowId }
        if (r != null && !r.isFilled()) draftIds["${r.y}-${r.m}-${r.block}"] = null
        _state.value = st.copy(rows = st.rows.filter { it.id != rowId })
        scope.launch {
            withIo { db.deleteRowWithPhotos(rowId) }
            refreshCounts()
        }
    }

    // ---------- Фильтр ----------

    fun setOnlyPhotos(v: Boolean) {
        _state.value = _state.value.copy(onlyPhotos = v)
    }

    // ---------- Статистика ----------

    fun stats(): Pair<Int, Int> { // X из Y
        val rows = _state.value.rows
        val filled = rows.filter { it.isFilled() }
        return filled.count { it.photoCount > 0 } to filled.size
    }

    fun blockNetSum(b: Int): Double =
        _state.value.rows.filter { it.block == b }.sumOf { MoneyUtils.parse(it.net) }

    fun rowsForBlock(b: Int): List<WorkRow> {
        val st = _state.value
        val all = st.rows.filter { it.block == b }
        return if (st.onlyPhotos) all.filter { it.photoCount > 0 } else all
    }

    // ---------- Фото ----------

    fun openGallery(rowId: String) {
        scope.launch {
            val thumbs = withIo { db.rowThumbs(rowId) }
            _state.value = _state.value.copy(galleryRowId = rowId, galleryThumbs = thumbs)
        }
    }

    fun closeGallery() {
        _state.value = _state.value.copy(galleryRowId = null, galleryThumbs = emptyList())
    }

    fun setGalleryOrder(oldestFirst: Boolean) {
        val list = _state.value.galleryThumbs
        _state.value = _state.value.copy(
            galleryOldestFirst = oldestFirst,
            galleryThumbs = if (oldestFirst) list else list.reversed(),
        )
    }

    fun deletePhoto(photoId: String) {
        scope.launch {
            withIo { db.deletePhoto(photoId) }
            _state.value.galleryRowId?.let { rid ->
                val thumbs = withIo { db.rowThumbs(rid) }
                _state.value = _state.value.copy(galleryThumbs = thumbs)
            }
            refreshCounts()
        }
    }

    /** Сохранить черновик строки перед добавлением фото; возвращает id строки для фото. */
    fun ensureRowSaved(rowId: String) {
        val r = _state.value.rows.firstOrNull { it.id == rowId } ?: return
        if (!r.isFilled()) {
            // черновик: сохраняем как пустую строку, чтобы к нему можно было крепить фото
            scope.launch { withIo { db.upsertRow(r) } }
        }
    }

    /** Есть ли у строки заполненные поля (без учёта фото). */
    fun rowHasText(rowId: String): Boolean {
        val r = _state.value.rows.firstOrNull { it.id == rowId } ?: return false
        return r.model.isNotBlank() || r.work.isNotBlank() || r.net.isNotBlank() || r.gross.isNotBlank()
    }

    /**
     * Гарантированно сохранить строку в БД (даже пустую — черновик под фото)
     * и только потом вызвать onDone. Used перед обработкой фото.
     */
    fun persistRowNow(rowId: String, onDone: () -> Unit) {
        saveTimers.remove(rowId)?.cancel()
        val r = _state.value.rows.firstOrNull { it.id == rowId } ?: run { onDone(); return }
        scope.launch {
            withIo { db.upsertRow(r) }
            onDone()
        }
    }

    /** Обработать выбранные/снятые фото для строки. Вызывать с IO-байтами. */
    fun addPhotos(rowId: String, byteSources: List<ByteArray>, onDone: (Int) -> Unit) {
        scope.launch {
            val added = withIo {
                var n = 0
                val y = _state.value.year; val m = _state.value.month
                for (bytes in byteSources) {
                    val processed = ImageUtils.processPhoto(bytes) ?: continue
                    val now = System.currentTimeMillis()
                    db.insertPhoto(
                        PhotoFull(
                            id = UUID.randomUUID().toString(),
                            rowId = rowId, y = y, m = m,
                            name = "IMG_$now.jpg", added = now,
                            blob = processed.first, thumb = processed.second,
                        )
                    )
                    n++
                }
                n
            }
            onDone(added)
            refreshCounts()
            _state.value.galleryRowId?.let { rid ->
                if (rid == rowId) {
                    val thumbs = withIo { db.rowThumbs(rid) }
                    _state.value = _state.value.copy(
                        galleryThumbs = if (_state.value.galleryOldestFirst) thumbs else thumbs.reversed()
                    )
                }
            }
        }
    }

    fun loadPhotoBlob(id: String, cb: (ByteArray?) -> Unit) {
        scope.launch { cb(withIo { db.photoBlob(id) }) }
    }

    // ---------- Бэкап ----------

    data class BackupInfo(val rowCount: Int, val photoCount: Int, val sizeMb: String, val json: String)

    fun prepareExport(onReady: (Result<BackupInfo>) -> Unit) {
        scope.launch {
            val y = _state.value.year; val m = _state.value.month
            val res = runCatching {
                flushAllTimersNow()
                withIo {
                    val json = Backup.buildJson(db, y, m)
                    val rowsN = db.monthRows(y, m).count { it.isFilled() }
                    val photosN = db.countMonthPhotos(y, m)
                    BackupInfo(rowsN, photosN, String.format(java.util.Locale.US, "%.1f", json.length / 1048576.0), json)
                }
            }
            onReady(res)
        }
    }

    fun onBackupWritten() {
        val now = System.currentTimeMillis()
        prefs.edit().putString("last_backup_time", now.toString()).apply()
        _state.value = _state.value.copy(lastBackupMs = now)
    }

    data class ImportPreview(val parsed: Backup.Parsed, val localPhotosToRemove: Int, val tooBig: Boolean)

    fun parseImport(text: String, sizeBytes: Long, onReady: (Result<ImportPreview>) -> Unit) {
        scope.launch {
            val res = runCatching {
                withIo {
                    val parsed = Backup.parse(text)
                    val local = db.countMonthPhotos(parsed.y, parsed.m)
                    ImportPreview(parsed, local, sizeBytes > 120L * 1048576L)
                }
            }
            onReady(res)
        }
    }

    fun applyImport(preview: ImportPreview, onDone: (ok: Boolean, msg: String) -> Unit) {
        scope.launch {
            val p = preview.parsed
            val result = runCatching {
                flushAllTimersNow()
                withIo {
                    // очистим черновики для этого месяца — пересоздадутся
                    val keepIds = p.rows.map { it.id }
                    val existingPhotoIds = db.monthPhotoIds(p.y, p.m)
                    val toInsert = ArrayList<PhotoFull>()
                    for (ph in p.photos) {
                        if (existingPhotoIds.contains(ph.id)) continue
                        val bytes = try {
                            android.util.Base64.decode(ph.base64Data, android.util.Base64.DEFAULT)
                        } catch (e: Exception) { continue }
                        val thumb = ImageUtils.thumbFromMain(bytes) ?: continue
                        toInsert.add(
                            PhotoFull(
                                id = ph.id, rowId = ph.rowId, y = p.y, m = p.m,
                                name = ph.name.ifEmpty { "IMG_${ph.added}.jpg" },
                                added = ph.added, blob = bytes, thumb = thumb,
                            )
                        )
                    }
                    db.restoreMonth(p.y, p.m, p.rows, toInsert, keepIds)
                }
            }
            draftIds.remove("${p.y}-${p.m}-0")
            for (b in 0 until 5) draftIds.remove("${p.y}-${p.m}-$b")
            if (result.isSuccess) {
                jumpTo(p.y, p.m)
                onDone(true, "Данные за ${DateUtils.monthName(p.m)} ${p.y} восстановлены")
            } else {
                onDone(false, "Ошибка сохранения: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // ---------- Отчёт ----------

    fun buildReportHtml(): String {
        val st = _state.value
        return HtmlReportProvider.generate(st.year, st.month, st.rows.filter { !isDraft(it) })
    }

    private fun isDraft(r: WorkRow): Boolean = !r.isFilled()

    fun context(): Context = getApplication()
}

/** Тонкая обёртка, чтобы не таскать импорт util в ViewModel-логику. */
private object HtmlReportProvider {
    fun generate(y: Int, m: Int, rows: List<WorkRow>): String =
        com.doxod.app.ui.util.HtmlReport.generate(y, m, rows)
}
