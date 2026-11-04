package com.doxod.app.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Даты: месяцы, блоки-недели, валидация и маска ДД-ММ-ГГ. */
object DateUtils {

    val MONTHS_FULL = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    val MONTHS_LOWER = listOf(
        "январь", "февраль", "март", "апрель", "май", "июнь",
        "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
    )
    val MONTHS_SHORT = listOf(
        "Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"
    )

    fun monthName(m: Int): String = MONTHS_FULL[m]
    fun monthShort(m: Int): String = MONTHS_SHORT[m]

    /** Число блоков: 4, если дней <= 28, иначе 5. */
    fun blockCount(y: Int, m: Int): Int = if (daysInMonth(y, m) <= 28) 4 else 5

    fun daysInMonth(y: Int, m: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, y)
        cal.set(Calendar.MONTH, m)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** Блок b = дни 7b+1 … min(7b+7, конец месяца). Возвращает (начало, конец) чисел. */
    fun blockDays(y: Int, m: Int, b: Int): Pair<Int, Int> {
        val start = b * 7 + 1
        val end = minOf(b * 7 + 7, daysInMonth(y, m))
        return start to end
    }

    /** Понедельник на/до первого дня блока, формат dd-MM-yy. */
    fun blockMonday(y: Int, m: Int, b: Int): String {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.YEAR, y)
        cal.set(Calendar.MONTH, m)
        cal.set(Calendar.DAY_OF_MONTH, blockDays(y, m, b).first)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        // сдвигаемся назад до понедельника
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return formatDmy(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }

    /** dd-MM-yy для текущего дня. */
    fun today(): String {
        val cal = Calendar.getInstance()
        return formatDmy(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }

    fun formatDmy(y: Int, m: Int, d: Int): String =
        String.format(Locale.US, "%02d-%02d-%02d", d, m + 1, y % 100)

    /** Пустая дата валидна; иначе ДД-ММ-ГГ: длина 8, NN-NN-NN, месяц 1–12, день по месяцу, год 2000+ГГ. */
    fun isValidDate(s: String): Boolean {
        if (s.isEmpty()) return true
        if (s.length != 8) return false
        if (!Regex("""\d{2}-\d{2}-\d{2}""").matches(s)) return false
        val d = s.substring(0, 2).toInt()
        val mo = s.substring(3, 5).toInt()
        val yy = s.substring(6, 8).toInt()
        if (mo < 1 || mo > 12) return false
        val y = 2000 + yy
        val max = daysInMonth(y, mo - 1)
        return d in 1..max
    }

    /**
     * Ввод даты: только цифры и «-», максимум 8 символов.
     * Если введено без дефисов больше 2 цифр — применяем маску (дефисы после 2-й и 4-й цифры, максимум 6 цифр).
     */
    fun onDateInput(old: String, new: String): String {
        // фильтруем разрешённые символы
        val filtered = buildString {
            for (ch in new) if (ch.isDigit() || ch == '-') append(ch)
            if (length > 8) setLength(8)
        }
        val digits = filtered.count { it.isDigit() }
        val dashes = filtered.count { it == '-' }
        return if (digits > 2 && dashes == 0) applyMask(filtered) else filtered
    }

    /** Применить маску ДД-ММ-ГГ к строке (максимум 6 цифр). */
    fun applyMask(s: String): String {
        val digits = StringBuilder()
        for (ch in s) if (ch.isDigit() && digits.length < 6) digits.append(ch)
        val d = digits.toString()
        return when {
            d.length <= 2 -> d
            d.length <= 4 -> "${d.substring(0, 2)}-${d.substring(2)}"
            else -> "${d.substring(0, 2)}-${d.substring(2, 4)}-${d.substring(4)}"
        }
    }

    /** Имя файла отчёта: Отчет_{Месяц}_{год}.html */
    fun reportFileName(y: Int, m: Int): String = "Отчет_${monthName(m)}_$y.html"

    /** Имя файла копии: копия_{месяц строчными}_{год}_{yyyy-MM-dd_HH-mm}.json */
    fun backupFileName(y: Int, m: Int, nowMs: Long): String {
        val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(java.util.Date(nowMs))
        return "копия_${MONTHS_LOWER[m]}_${y}_$ts.json"
    }

    /** dd-MM-yy HH:mm для времени последней копии. */
    fun formatBackupTime(ms: Long): String {
        val cal = Calendar.getInstance(); cal.timeInMillis = ms
        val hhmm = SimpleDateFormat("HH:mm", Locale.US).format(java.util.Date(ms))
        return "${formatDmy(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))} $hhmm"
    }

    /** dd.MM HH:mm для подписи миниатюры. */
    fun formatThumbTime(ms: Long): String =
        SimpleDateFormat("dd.MM HH:mm", Locale.US).format(java.util.Date(ms))
}
