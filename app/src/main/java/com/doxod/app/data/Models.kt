package com.doxod.app.data

/** Строка учёта работ. Суммы хранятся ровно как введены (строками). */
data class WorkRow(
    val id: String,
    val y: Int,
    val m: Int,          // 0..11
    val block: Int,      // 0..4
    val ord: Int,        // порядок в блоке
    val date: String,    // "ДД-ММ-ГГ" или ""
    val dateSet: Boolean,
    val model: String,
    val work: String,
    val net: String,
    val gross: String,
    val photoCount: Int = 0,
) {
    fun isFilled(): Boolean =
        model.isNotBlank() || work.isNotBlank() || net.isNotBlank() || gross.isNotBlank() || photoCount > 0
}

/** Миниатюра фото строки (без основного blob). */
data class PhotoThumb(
    val id: String,
    val rowId: String,
    val name: String,
    val added: Long,
    val thumb: ByteArray,
) {
    override fun equals(other: Any?): Boolean = other is PhotoThumb && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

/** Итоги месяца для шапки/счётчиков. */
data class MonthStats(
    val filledRows: Int,   // Y — заполненных строк
    val rowsWithPhoto: Int, // X — из них с фото
)
