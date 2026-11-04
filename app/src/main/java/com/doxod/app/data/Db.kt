package com.doxod.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Db(context: Context) : SQLiteOpenHelper(context.applicationContext, "doxod.db", null, 1) {

    companion object {
        private const val CREATE_ROWS = """
            CREATE TABLE work_rows(
                id TEXT PRIMARY KEY,
                y INTEGER NOT NULL,
                m INTEGER NOT NULL,
                block INTEGER NOT NULL,
                ord INTEGER NOT NULL,
                date TEXT NOT NULL DEFAULT '',
                dateSet INTEGER NOT NULL DEFAULT 0,
                model TEXT NOT NULL DEFAULT '',
                work TEXT NOT NULL DEFAULT '',
                net TEXT NOT NULL DEFAULT '',
                gross TEXT NOT NULL DEFAULT ''
            )"""
        private const val CREATE_PHOTOS = """
            CREATE TABLE photos(
                id TEXT PRIMARY KEY,
                rowId TEXT NOT NULL,
                y INTEGER NOT NULL,
                m INTEGER NOT NULL,
                name TEXT NOT NULL DEFAULT '',
                added INTEGER NOT NULL,
                blob BLOB,
                thumb BLOB
            )"""
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_ROWS)
        db.execSQL("CREATE INDEX idx_rows YM ON work_rows(y,m)")
        db.execSQL("CREATE INDEX idx_rows ORDER ON work_rows(y,m,block,ord)")
        db.execSQL(CREATE_PHOTOS)
        db.execSQL("CREATE INDEX idx_photos_row ON photos(rowId)")
        db.execSQL("CREATE INDEX idx_photos YM ON photos(y,m)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Данные не удаляем. Миграций пока нет.
    }

    // ---------- Строки ----------

    /** Строки месяца (ORDER BY block, ord) со счётчиком фото. */
    fun monthRows(y: Int, m: Int): List<WorkRow> {
        val out = ArrayList<WorkRow>()
        val c = readableDatabase.rawQuery(
            """SELECT r.id,r.y,r.m,r.block,r.ord,r.date,r.dateSet,r.model,r.work,r.net,r.gross,
               (SELECT COUNT(*) FROM photos p WHERE p.rowId=r.id)
               FROM work_rows r WHERE r.y=? AND r.m=? ORDER BY r.block,r.ord""",
            arrayOf(y.toString(), m.toString())
        )
        c.use {
            while (it.moveToNext()) {
                out.add(
                    WorkRow(
                        id = it.getString(0),
                        y = it.getInt(1),
                        m = it.getInt(2),
                        block = it.getInt(3),
                        ord = it.getInt(4),
                        date = it.getString(5) ?: "",
                        dateSet = it.getInt(6) != 0,
                        model = it.getString(7) ?: "",
                        work = it.getString(8) ?: "",
                        net = it.getString(9) ?: "",
                        gross = it.getString(10) ?: "",
                        photoCount = it.getInt(11),
                    )
                )
            }
        }
        return out
    }

    fun upsertRow(r: WorkRow) {
        val v = ContentValues().apply {
            put("id", r.id); put("y", r.y); put("m", r.m)
            put("block", r.block); put("ord", r.ord)
            put("date", r.date); put("dateSet", if (r.dateSet) 1 else 0)
            put("model", r.model); put("work", r.work)
            put("net", r.net); put("gross", r.gross)
        }
        writableDatabase.replace("work_rows", null, v)
    }

    /** Удалить строку вместе с её фото (в транзакции). */
    fun deleteRowWithPhotos(id: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("photos", "rowId=?", arrayOf(id))
            db.delete("work_rows", "id=?", arrayOf(id))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun photoCountForRow(id: String): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM photos WHERE rowId=?", arrayOf(id)
        ).use { c -> return if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    // ---------- Фото ----------

    /** Миниатюры строки без blob, ORDER BY added. */
    fun rowThumbs(rowId: String): List<PhotoThumb> {
        val out = ArrayList<PhotoThumb>()
        readableDatabase.rawQuery(
            "SELECT id,rowId,name,added,thumb FROM photos WHERE rowId=? ORDER BY added",
            arrayOf(rowId)
        ).use { c ->
            while (c.moveToNext()) {
                val t = c.getBlob(4) ?: continue
                out.add(PhotoThumb(c.getString(0), c.getString(1), c.getString(2) ?: "", c.getLong(3), t))
            }
        }
        return out
    }

    /** Основной blob по id — отдельным запросом (лимит CursorWindow ~2 МБ на строку). */
    fun photoBlob(id: String): ByteArray? {
        readableDatabase.rawQuery("SELECT blob FROM photos WHERE id=?", arrayOf(id)).use { c ->
            return if (c.moveToFirst()) c.getBlob(0) else null
        }
    }

    fun allMonthPhotos(y: Int, m: Int): List<PhotoThumb> {
        val out = ArrayList<PhotoThumb>()
        readableDatabase.rawQuery(
            "SELECT id,rowId,name,added,thumb FROM photos WHERE y=? AND m=? ORDER BY added",
            arrayOf(y.toString(), m.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val t = c.getBlob(4) ?: ByteArray(0)
                out.add(PhotoThumb(c.getString(0), c.getString(1), c.getString(2) ?: "", c.getLong(3), t))
            }
        }
        return out
    }

    fun countMonthPhotos(y: Int, m: Int): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM photos WHERE y=? AND m=?", arrayOf(y.toString(), m.toString())
        ).use { c -> return if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    fun insertPhoto(p: PhotoFull) {
        val v = ContentValues().apply {
            put("id", p.id); put("rowId", p.rowId); put("y", p.y); put("m", p.m)
            put("name", p.name); put("added", p.added); put("blob", p.blob); put("thumb", p.thumb)
        }
        writableDatabase.insert("photos", null, v)
    }

    fun deletePhoto(id: String) {
        writableDatabase.delete("photos", "id=?", arrayOf(id))
    }

    /** Все id строк месяца. */
    fun monthRowIds(y: Int, m: Int): Set<String> {
        val out = HashSet<String>()
        readableDatabase.rawQuery("SELECT id FROM work_rows WHERE y=? AND m=?", arrayOf(y.toString(), m.toString()))
            .use { c -> while (c.moveToNext()) out.add(c.getString(0)) }
        return out
    }

    /** Удалить фото месяца, чьи rowId не в списке. */
    fun deleteOrphanMonthPhotos(y: Int, m: Int, keepRowIds: Collection<String>) {
        val db = writableDatabase
        if (keepRowIds.isEmpty()) {
            db.delete("photos", "y=? AND m=?", arrayOf(y.toString(), m.toString()))
            return
        }
        val placeholders = keepRowIds.joinToString(",") { "?" }
        db.delete(
            "photos",
            "y=? AND m=? AND rowId NOT IN ($placeholders)",
            arrayOf(y.toString(), m.toString()) + keepRowIds.toTypedArray(),
        )
    }

    /** Полное восстановление месяца из бэкапа — одна транзакция. */
    fun restoreMonth(y: Int, m: Int, rows: List<WorkRow>, photosToInsert: List<PhotoFull>, keepRowIds: Collection<String>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("work_rows", "y=? AND m=?", arrayOf(y.toString(), m.toString()))
            for (r in rows) {
                val v = ContentValues().apply {
                    put("id", r.id); put("y", r.y); put("m", r.m)
                    put("block", r.block); put("ord", r.ord)
                    put("date", r.date); put("dateSet", if (r.dateSet) 1 else 0)
                    put("model", r.model); put("work", r.work)
                    put("net", r.net); put("gross", r.gross)
                }
                db.insert("work_rows", null, v)
            }
            val placeholders = keepRowIds.joinToString(",") { "?" }
            if (keepRowIds.isEmpty()) {
                db.delete("photos", "y=? AND m=?", arrayOf(y.toString(), m.toString()))
            } else {
                db.delete(
                    "photos",
                    "y=? AND m=? AND rowId NOT IN ($placeholders)",
                    arrayOf(y.toString(), m.toString()) + keepRowIds.toTypedArray(),
                )
            }
            for (p in photosToInsert) {
                val v = ContentValues().apply {
                    put("id", p.id); put("rowId", p.rowId); put("y", p.y); put("m", p.m)
                    put("name", p.name); put("added", p.added); put("blob", p.blob); put("thumb", p.thumb)
                }
                db.insert("photos", null, v)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Существующие id фото месяца (для пропуска уже имеющихся при импорте). */
    fun monthPhotoIds(y: Int, m: Int): Set<String> {
        val out = HashSet<String>()
        readableDatabase.rawQuery("SELECT id FROM photos WHERE y=? AND m=?", arrayOf(y.toString(), m.toString()))
            .use { c -> while (c.moveToNext()) out.add(c.getString(0)) }
        return out
    }
}

/** Фото целиком (для вставки/экспорта). */
data class PhotoFull(
    val id: String,
    val rowId: String,
    val y: Int,
    val m: Int,
    val name: String,
    val added: Long,
    val blob: ByteArray,
    val thumb: ByteArray,
)
