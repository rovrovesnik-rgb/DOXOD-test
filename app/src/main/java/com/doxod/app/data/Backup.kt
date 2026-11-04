package com.doxod.app.data

import android.util.Base64
import com.doxod.app.ui.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** JSON-бэкап одного месяца: формат "uchet-rabot", version 2. */
object Backup {

    const val FORMAT_ID = "uchet-rabot"
    const val VERSION = 2

    data class Parsed(
        val y: Int,
        val m: Int,
        val rows: List<WorkRow>,
        val photos: List<PhotoJson>, // без декодирования blob
        val photoCount: Int,
    )

    data class PhotoJson(
        val id: String,
        val rowId: String,
        val name: String,
        val added: Long,
        val base64Data: String,
    )

    /** Собрать JSON для текущего состояния месяца (только заполненные строки и их фото). */
    fun buildJson(db: Db, y: Int, m: Int): String {
        val rows = db.monthRows(y, m).filter { it.isFilled() }
        val rowIds = rows.map { it.id }.toHashSet()
        val root = JSONObject()
        root.put("format", FORMAT_ID)
        root.put("version", VERSION)
        root.put("exportedAt", utcNow())
        root.put("y", y)
        root.put("m", m)

        val rowsArr = JSONArray()
        for (r in rows) {
            val o = JSONObject()
            o.put("id", r.id)
            o.put("y", r.y)
            o.put("m", r.m)
            o.put("block", r.block)
            o.put("order", r.ord)
            o.put("date", r.date)
            o.put("dateSet", r.dateSet)
            o.put("model", r.model)
            o.put("work", r.work)
            o.put("net", r.net)
            o.put("gross", r.gross)
            rowsArr.put(o)
        }
        root.put("rows", rowsArr)

        val photosArr = JSONArray()
        val c = db.readableDatabase.rawQuery(
            "SELECT id,rowId,name,added,blob FROM photos WHERE y=? AND m=?",
            arrayOf(y.toString(), m.toString())
        )
        c.use {
            while (it.moveToNext()) {
                if (!rowIds.contains(it.getString(1))) continue
                val blob = it.getBlob(4) ?: continue
                val o = JSONObject()
                o.put("id", it.getString(0))
                o.put("rowId", it.getString(1))
                o.put("name", it.getString(2) ?: "")
                o.put("added", it.getLong(3))
                o.put("data", "data:image/jpeg;base64," +
                        Base64.encodeToString(blob, Base64.NO_WRAP))
                photosArr.put(o)
            }
        }
        root.put("photos", photosArr)
        return root.toString(2)
    }

    private fun utcNow(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(java.util.Date())
    }

    /** Разобрать и проверить файл копии. Бросает исключение с текстом причины при ошибке. */
    fun parse(text: String): Parsed {
        val root = try { JSONObject(text) } catch (e: Exception) {
            throw IllegalArgumentException("Это не файл копии")
        }
        if (root.optString("format") != FORMAT_ID) throw IllegalArgumentException("Это не файл копии")
        if (root.optInt("version", -1) != VERSION) throw IllegalArgumentException("Это не файл копии")
        if (!root.has("y") || !root.has("m") || !root.has("rows") || !root.has("photos")) {
            throw IllegalArgumentException("Это не файл копии")
        }
        val y = root.getInt("y")
        val m = root.getInt("m")
        if (m < 0 || m > 11) throw IllegalArgumentException("Это не файл копии")

        val rows = ArrayList<WorkRow>()
        val rowsArr = root.getJSONArray("rows")
        for (i in 0 until rowsArr.length()) {
            val o = rowsArr.getJSONObject(i)
            rows.add(
                WorkRow(
                    id = o.getString("id"),
                    y = o.optInt("y", y),
                    m = o.optInt("m", m),
                    block = o.optInt("block", 0),
                    ord = o.optInt("order", i),
                    date = o.optString("date"),
                    dateSet = o.optBoolean("dateSet", false),
                    model = o.optString("model"),
                    work = o.optString("work"),
                    net = o.optString("net"),
                    gross = o.optString("gross"),
                )
            )
        }
        val photos = ArrayList<PhotoJson>()
        val phArr = root.getJSONArray("photos")
        for (i in 0 until phArr.length()) {
            val o = phArr.getJSONObject(i)
            val data = o.optString("data")
            val comma = data.indexOf(',')
            val b64 = if (comma >= 0) data.substring(comma + 1) else data
            photos.add(
                PhotoJson(
                    id = o.getString("id"),
                    rowId = o.getString("rowId"),
                    name = o.optString("name"),
                    added = o.optLong("added"),
                    base64Data = b64,
                )
            )
        }
        return Parsed(y, m, rows, photos, photos.size)
    }

    /** Имя файла для сохранения копии. */
    fun suggestedFileName(y: Int, m: Int): String =
        DateUtils.backupFileName(y, m, System.currentTimeMillis())
}
