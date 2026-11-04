package com.doxod.app.ui.util

import com.doxod.app.data.WorkRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Генерация HTML-отчёта. */
object HtmlReport {

    private fun esc(s: String): String = buildString {
        for (ch in s) when (ch) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(ch)
        }
    }

    private fun nl2br(s: String): String = esc(s).replace("\n", "<br>")

    /**
     * В отчёт попадают только строки, где непусто хотя бы одно из model/work/net/gross
     * (строки только с фото не попадают).
     */
    fun reportInRows(rows: List<WorkRow>): Set<String> =
        rows.filter { it.model.isNotBlank() || it.work.isNotBlank() || it.net.isNotBlank() || it.gross.isNotBlank() }
            .map { it.id }.toSet()

    fun generate(y: Int, m: Int, rows: List<WorkRow>): String {
        val blocks = DateUtils.blockCount(y, m)
        val monthNameUpper = DateUtils.monthName(m).uppercase(Locale.getDefault())
        val now = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Date())

        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html lang=\"ru\"><head><meta charset=\"UTF-8\">")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        sb.append("<title>Отчет: ").append(esc(DateUtils.monthName(m))).append(" ").append(y).append("</title>")
        sb.append(CSS)
        sb.append("</head><body>")

        // Шапка документа
        sb.append("<div class=\"doc-head\"><div>")
        sb.append("<div class=\"brand\">Do<span>X</span>oD</div>")
        sb.append("<div class=\"brand-sub\">Учет выполненных работ и доходов</div>")
        sb.append("</div><div style=\"text-align:right\">")
        sb.append("<div class=\"doc-title\">Отчет: ").append(esc(DateUtils.monthName(m))).append(" ").append(y).append("</div>")
        sb.append("<div class=\"doc-formed\">Сформирован: ").append(esc(now)).append("</div>")
        sb.append("</div></div>")
        sb.append("<div class=\"gold-line\"></div>")

        var totalNet = 0.0
        var totalGross = 0.0

        for (b in 0 until blocks) {
            val blockRows = rows.filter { it.block == b }
                .filter { it.model.isNotBlank() || it.work.isNotBlank() || it.net.isNotBlank() || it.gross.isNotBlank() }
                .sortedBy { it.ord }
            val (d0, d1) = DateUtils.blockDays(y, m, b)
            val monday = DateUtils.blockMonday(y, m, b)
            var blockSum = 0.0
            for (r in rows.filter { it.block == b }) blockSum += MoneyUtils.parse(r.net)
            totalNet += blockSum
            for (r in rows.filter { it.block == b }) totalGross += MoneyUtils.parse(r.gross)

            sb.append("<div class=\"week\">")
            sb.append("<div class=\"week-head\">")
            sb.append("<div><span class=\"week-badge\">Блок ").append(b + 1).append("</span> ")
            sb.append("<span class=\"week-title\">Неделя ").append(esc(monday)).append("</span> ")
            sb.append("<span class=\"week-range\">(").append(d0).append("–").append(d1).append(" ")
                .append(esc(monthNameUpper)).append(")</span></div>")
            sb.append("<div class=\"week-sum\">Сумма (чистая): <b>")
                .append(esc(MoneyUtils.format(blockSum))).append("&nbsp;₽</b></div>")
            sb.append("</div>")

            sb.append("<table><thead><tr>")
            sb.append("<th style=\"width:40px\">№</th><th style=\"width:90px\">Число</th>")
            sb.append("<th style=\"width:140px\">Модель</th><th>Работы</th>")
            sb.append("<th style=\"width:110px;text-align:right\">Чистая</th>")
            sb.append("<th style=\"width:110px;text-align:right\">Грязная</th>")
            sb.append("</tr></thead><tbody>")

            if (blockRows.isEmpty()) {
                sb.append("<tr><td colspan=\"6\" class=\"empty-week\">Нет записей за эту неделю</td></tr>")
            } else {
                blockRows.forEachIndexed { i, r ->
                    sb.append("<tr>")
                    sb.append("<td>").append(i + 1).append("</td>")
                    sb.append("<td>").append(esc(r.date)).append("</td>")
                    sb.append("<td class=\"model\">").append(esc(r.model)).append("</td>")
                    sb.append("<td class=\"work\">").append(nl2br(r.work)).append("</td>")
                    sb.append("<td class=\"net\">").append(esc(MoneyUtils.format(MoneyUtils.parse(r.net)))).append("&nbsp;₽</td>")
                    sb.append("<td class=\"gross\">").append(esc(MoneyUtils.format(MoneyUtils.parse(r.gross)))).append("&nbsp;₽</td>")
                    sb.append("</tr>")
                }
            }
            sb.append("</tbody><tfoot><tr>")
            sb.append("<td colspan=\"4\">Сумма за неделю (чистая):</td>")
            sb.append("<td class=\"foot-sum\">").append(esc(MoneyUtils.format(blockSum))).append("&nbsp;₽</td><td></td>")
            sb.append("</tr></tfoot></table></div>")
        }

        // Итоговая карточка
        sb.append("<div class=\"total-card\">")
        sb.append("<div class=\"total-col\"><div class=\"total-label\">Итого чистая за месяц</div>")
        sb.append("<div class=\"total-net\">").append(esc(MoneyUtils.format(totalNet))).append("&nbsp;₽</div></div>")
        sb.append("<div class=\"total-col\"><div class=\"total-label\">Итого грязная за месяц</div>")
        sb.append("<div class=\"total-gross\">").append(esc(MoneyUtils.format(totalGross))).append("&nbsp;₽</div></div>")
        sb.append("</div>")

        sb.append("</body></html>")
        return sb.toString()
    }

    private val CSS = """
<style>
*{box-sizing:border-box}
body{background:#fff;color:#222;font-family:-apple-system,'Segoe UI',Roboto,Arial,sans-serif;margin:0;padding:16px;font-size:14px}
.doc-head{display:flex;justify-content:space-between;align-items:flex-start}
.brand{font-family:Georgia,'Times New Roman',serif;font-style:italic;font-size:28px;font-weight:bold;color:#222}
.brand span{color:#C49F4B}
.brand-sub{font-size:12px;color:#777;margin-top:2px}
.doc-title{font-family:Georgia,'Times New Roman',serif;font-style:italic;font-size:22px;color:#222}
.doc-formed{font-size:12px;color:#777;margin-top:4px}
.gold-line{border-top:3px double #E3C37A;margin:10px 0 18px 0}
.week{margin-bottom:22px;page-break-inside:avoid}
.week-head{display:flex;justify-content:space-between;align-items:center;background:#F4F2EB;border-left:4px solid #E3C37A;padding:8px 12px;border-radius:6px 6px 0 0}
.week-badge{background:#0B0A0F;color:#E3C37A;font-size:11px;font-weight:bold;padding:2px 8px;border-radius:4px;margin-right:6px}
.week-title{font-weight:bold;font-size:15px}
.week-range{color:#666;font-size:13px}
.week-sum{font-size:13px;color:#333;white-space:nowrap;margin-left:12px}
table{width:100%;border-collapse:collapse;margin-top:6px}
th{background:#F8F8F8;border:1px solid #DCDCDC;padding:6px 8px;text-align:left;font-size:12px;color:#555}
td{border:1px solid #DCDCDC;padding:6px 8px;vertical-align:top}
tbody tr:nth-child(even){background:#F9F9FB}
td.model{font-weight:bold}
td.work{white-space:pre-wrap}
td.net{color:#1B5E20;font-weight:bold;text-align:right;white-space:nowrap}
td.gross{text-align:right;white-space:nowrap}
.empty-week{text-align:center;font-style:italic;color:#999}
tfoot td{background:#FAF8F2;border-top:2px solid #E3C37A}
.foot-sum{font-weight:bold;text-align:right;color:#1B5E20;white-space:nowrap}
.total-card{background:#0B0A0F;border:2px solid #E3C37A;border-radius:8px;padding:16px;display:flex;gap:16px;margin-top:24px}
.total-col{flex:1;text-align:center}
.total-label{color:#A5A3B3;font-size:12px;margin-bottom:6px}
.total-net{font-family:Georgia,serif;font-size:24px;color:#E3C37A;font-weight:bold}
.total-gross{font-family:Georgia,serif;font-size:24px;color:#fff;font-weight:bold}
@media print{
 .week{page-break-inside:avoid}
 .total-card{background:#F4F2EB;color:#111;border-color:#888}
 .total-net{color:#8a6d1d}
 .total-gross{color:#111}
 .total-label{color:#444}
}
</style>
"""
}
