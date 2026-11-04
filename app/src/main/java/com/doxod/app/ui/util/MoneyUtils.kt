package com.doxod.app.ui.util

/** Парсинг и форматирование денежных сумм. */
object MoneyUtils {

    private const val NBSP = '\u00A0'

    /** Убрать обычные и неразрывные пробелы, запятую → точка, оставить только цифры, точку и минус. */
    fun parse(s: String): Double {
        val cleaned = s
            .replace(NBSP, ' ')
            .replace(" ", "")
            .replace(",", ".")
            .filter { it.isDigit() || it == '.' || it == '-' }
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    /**
     * Формат: 0 → «0»; целое → группы по 3 через пробел («1 234»);
     * дробное → до 2 знаков, разделитель запятая, группы через пробел («1 234,5»).
     */
    fun format(value: Double): String {
        if (value == 0.0) return "0"
        val neg = value < 0
        val abs = kotlin.math.abs(value)
        val rounded = Math.round(abs * 100.0) / 100.0
        val intPart = Math.floor(rounded).toLong()
        val frac = Math.round((rounded - intPart) * 100.0).toInt()
        val intStr = groupThousands(intPart.toString())
        return buildString {
            if (neg) append('-')
            append(intStr)
            when {
                frac % 100 == 0 -> { /* целое — без дробной части */ }
                frac % 10 == 0 -> append(',').append(frac / 10)
                else -> append(',').append(frac)
            }
        }
    }

    /** Сумма как «{format} ₽». */
    fun formatWithRub(value: Double): String = "${format(value)} ₽"

    private fun groupThousands(digits: String): String {
        val sb = StringBuilder()
        val n = digits.length
        for (i in digits.indices) {
            if (i > 0 && (n - i) % 3 == 0) sb.append(' ')
            sb.append(digits[i])
        }
        return sb.toString()
    }

    /** Ввод в полях суммы: разрешены цифры, запятая, точка, пробел (в т.ч. неразрывный), минус; максимум 12 символов. */
    fun onMoneyInput(new: String): String = buildString {
        for (ch in new) {
            if (ch.isDigit() || ch == ',' || ch == '.' || ch == ' ' || ch == NBSP || ch == '-') append(ch)
            if (length >= 12) break
        }
    }

    /**
     * Формат при потере фокуса. Если строка оканчивается на запятую/точку —
     * формат даёт «N,» (или «N.»), чтобы не стирать вводимый разделитель.
     */
    fun formatOnBlur(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty()) return ""
        val endsComma = t.endsWith(",")
        val endsDot = t.endsWith(".")
        val core = if (endsComma || endsDot) t.substring(0, t.length - 1) else t
        val formatted = format(parse(core))
        return when {
            endsComma -> "$formatted,"
            endsDot -> "$formatted."
            else -> formatted
        }
    }
}
