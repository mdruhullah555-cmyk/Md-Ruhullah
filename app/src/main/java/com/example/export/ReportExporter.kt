package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.CategoryBudget
import com.example.data.model.CategoryCatalog
import com.example.data.model.FinanceFormatters
import com.example.data.model.RecurringPayment
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionType
import com.example.ui.i18n.AppLanguage
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExportReportPayload(
    val monthKey: String,
    val currencyCode: String,
    val userName: String,
    val language: AppLanguage = AppLanguage.BN,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val categoryTotals: List<Pair<String, Double>>,
    val transactions: List<TransactionItem>,
    val budgets: List<CategoryBudget>,
    val recurringPayments: List<RecurringPayment>
)

object ReportExporter {

    fun createShareablePdf(context: Context, payload: ExportReportPayload): Uri {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, "SpendWise_Report_${payload.monthKey}.pdf")
        FileOutputStream(file).use { out ->
            writePdfToStream(payload, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun createShareableExcel(context: Context, payload: ExportReportPayload): Uri {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, "SpendWise_Report_${payload.monthKey}.xlsx")
        FileOutputStream(file).use { out ->
            writeExcelXlsxToStream(payload, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun buildShareIntent(uri: Uri, mimeType: String, title: String): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, title)
    }

    fun writePdfToStream(payload: ExportReportPayload, outputStream: OutputStream) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val lang = payload.language
        val isBn = lang == AppLanguage.BN

        // Match the Indigo-Violet palette from the HTML report
        val headerBgPaint = Paint().apply { color = Color.rgb(91, 91, 240) }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(224, 226, 255)
            textSize = 11f
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(27, 35, 51)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(27, 35, 51)
            textSize = 10.5f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.rgb(107, 118, 144)
            textSize = 9.5f
            isAntiAlias = true
        }
        val incomePaint = Paint().apply {
            color = Color.rgb(18, 184, 134)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val expensePaint = Paint().apply {
            color = Color.rgb(240, 80, 110)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = Color.rgb(230, 233, 242)
            strokeWidth = 1f
        }
        val cardPaint = Paint().apply { color = Color.rgb(244, 246, 251) }

        var pageNumber = 1
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        val reportTitle = if (isBn) "দৈনিক আয়-ব্যয় · মাসিক রিপোর্ট" else "Daily Income & Expense Report"
        val monthLabel = FinanceFormatters.formatMonthDisplay(payload.monthKey, lang)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 94f, headerBgPaint)
        canvas.drawText(reportTitle, margin, 42f, titlePaint)
        canvas.drawText(
            "$monthLabel  •  ${payload.userName}",
            margin,
            66f,
            subtitlePaint
        )

        var y = 122f

        // Summary Card
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + 72f, 10f, 10f, cardPaint)
        val col1 = margin + 16f
        val col2 = margin + 185f
        val col3 = margin + 355f
        canvas.drawText(if (isBn) "মোট আয়" else "TOTAL INCOME", col1, y + 24f, mutedPaint)
        canvas.drawText(
            FinanceFormatters.formatCurrency(payload.totalIncome, payload.currencyCode, lang = lang),
            col1,
            y + 48f,
            incomePaint.apply { textSize = 14f }
        )
        canvas.drawText(if (isBn) "মোট খরচ" else "TOTAL EXPENSES", col2, y + 24f, mutedPaint)
        canvas.drawText(
            FinanceFormatters.formatCurrency(payload.totalExpense, payload.currencyCode, lang = lang),
            col2,
            y + 48f,
            expensePaint.apply { textSize = 14f }
        )
        canvas.drawText(if (isBn) "ব্যালেন্স" else "NET BALANCE", col3, y + 24f, mutedPaint)
        canvas.drawText(
            FinanceFormatters.formatCurrency(payload.netBalance, payload.currencyCode, lang = lang),
            col3,
            y + 48f,
            sectionPaint
        )

        y += 100f

        if (payload.categoryTotals.isNotEmpty()) {
            canvas.drawText(
                if (isBn) "খাত অনুযায়ী খরচ" else "Expenses by Category",
                margin,
                y,
                sectionPaint
            )
            y += 18f
            payload.categoryTotals.take(10).forEach { (cat, sum) ->
                val pct = if (payload.totalExpense > 0) (sum / payload.totalExpense * 100.0) else 0.0
                val catDisplay = CategoryCatalog.metaFor(cat).displayName(lang)
                canvas.drawText(catDisplay, margin, y, bodyPaint)
                canvas.drawText(
                    "${FinanceFormatters.formatCurrency(sum, payload.currencyCode, lang = lang)} (${String.format("%.1f", pct)}%)",
                    pageWidth - margin - 140f,
                    y,
                    bodyPaint
                )
                y += 16f
            }
            y += 14f
        }

        canvas.drawText(
            if (isBn) "সব লেনদেন (${FinanceFormatters.formatNumber(payload.transactions.size, lang)})"
            else "Transactions (${payload.transactions.size})",
            margin,
            y,
            sectionPaint
        )
        y += 14f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 16f
        canvas.drawText(if (isBn) "তারিখ" else "DATE", margin, y, mutedPaint)
        canvas.drawText(if (isBn) "বিবরণ" else "TITLE", margin + 84f, y, mutedPaint)
        canvas.drawText(if (isBn) "খাত" else "CATEGORY", margin + 275f, y, mutedPaint)
        canvas.drawText(if (isBn) "টাকা" else "AMOUNT", pageWidth - margin - 85f, y, mutedPaint)
        y += 8f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 16f

        incomePaint.textSize = 10.5f
        expensePaint.textSize = 10.5f

        payload.transactions.forEach { tx ->
            if (y > pageHeight - 54f) {
                pdfDocument.finishPage(page)
                pageNumber++
                page = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = page.canvas
                y = 48f
                canvas.drawText("SpendWise Report — Page $pageNumber", margin, y, mutedPaint)
                y += 24f
            }
            val dateText = FinanceFormatters.formatDateShort(tx.dateMillis, lang)
            val catDisplay = CategoryCatalog.metaFor(tx.category).displayName(lang)
            val titleText = (tx.title.ifBlank { catDisplay }).take(26)
            val isIncome = tx.parsedType == TransactionType.INCOME
            val amountText = FinanceFormatters.formatCurrency(
                tx.amount,
                payload.currencyCode,
                showSign = true,
                isPositive = isIncome,
                lang = lang
            )
            canvas.drawText(dateText, margin, y, mutedPaint)
            canvas.drawText(titleText, margin + 84f, y, bodyPaint)
            canvas.drawText(catDisplay.take(18), margin + 275f, y, mutedPaint)
            canvas.drawText(
                amountText,
                pageWidth - margin - 85f,
                y,
                if (isIncome) incomePaint else expensePaint
            )
            y += 18f
        }

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    fun writeExcelXlsxToStream(payload: ExportReportPayload, outputStream: OutputStream) {
        val lang = payload.language
        val summaryRows = buildList<List<CellVal>> {
            add(listOf(CellVal.Text("Daily Income & Expense (SpendWise) Monthly Report")))
            add(listOf(CellVal.Text("Month"), CellVal.Text(FinanceFormatters.formatMonthDisplay(payload.monthKey, lang))))
            add(listOf(CellVal.Text("Account"), CellVal.Text(payload.userName)))
            add(listOf(CellVal.Text("Currency"), CellVal.Text(payload.currencyCode)))
            add(emptyList())
            add(listOf(CellVal.Text("Metric"), CellVal.Text("Amount (${payload.currencyCode})")))
            add(listOf(CellVal.Text("Total Income"), CellVal.Num(payload.totalIncome)))
            add(listOf(CellVal.Text("Total Expenses"), CellVal.Num(payload.totalExpense)))
            add(listOf(CellVal.Text("Net Balance"), CellVal.Num(payload.netBalance)))
            add(emptyList())
            add(listOf(CellVal.Text("Category"), CellVal.Text("Spent (${payload.currencyCode})"), CellVal.Text("Share %")))
            payload.categoryTotals.forEach { (cat, amount) ->
                val pct = if (payload.totalExpense > 0) amount / payload.totalExpense * 100.0 else 0.0
                val catLabel = CategoryCatalog.metaFor(cat).displayName(lang)
                add(listOf(CellVal.Text(catLabel), CellVal.Num(amount), CellVal.Num((pct * 10.0).toInt() / 10.0)))
            }
        }

        val txRows = buildList<List<CellVal>> {
            add(
                listOf(
                    CellVal.Text("Date"),
                    CellVal.Text("Title"),
                    CellVal.Text("Type"),
                    CellVal.Text("Category"),
                    CellVal.Text("Amount (${payload.currencyCode})"),
                    CellVal.Text("Note")
                )
            )
            payload.transactions.forEach { tx ->
                val catLabel = CategoryCatalog.metaFor(tx.category).displayName(lang)
                add(
                    listOf(
                        CellVal.Text(FinanceFormatters.formatDateFull(tx.dateMillis, lang)),
                        CellVal.Text(tx.title),
                        CellVal.Text(tx.type),
                        CellVal.Text(catLabel),
                        CellVal.Num(tx.amount),
                        CellVal.Text(tx.note.orEmpty())
                    )
                )
            }
        }

        val planningRows = buildList<List<CellVal>> {
            add(listOf(CellVal.Text("Monthly Budgets (${payload.monthKey})")))
            add(listOf(CellVal.Text("Category"), CellVal.Text("Monthly Limit (${payload.currencyCode})")))
            payload.budgets.forEach { b ->
                val catLabel = if (b.category == "OVERALL") "OVERALL" else CategoryCatalog.metaFor(b.category).displayName(lang)
                add(listOf(CellVal.Text(catLabel), CellVal.Num(b.limitAmount)))
            }
            add(emptyList())
            add(listOf(CellVal.Text("Recurring Payments")))
            add(
                listOf(
                    CellVal.Text("Title"),
                    CellVal.Text("Type"),
                    CellVal.Text("Category"),
                    CellVal.Text("Frequency"),
                    CellVal.Text("Amount"),
                    CellVal.Text("Next Due"),
                    CellVal.Text("Active")
                )
            )
            payload.recurringPayments.forEach { r ->
                add(
                    listOf(
                        CellVal.Text(r.title),
                        CellVal.Text(r.type),
                        CellVal.Text(CategoryCatalog.metaFor(r.category).displayName(lang)),
                        CellVal.Text(r.frequency),
                        CellVal.Num(r.amount),
                        CellVal.Text(FinanceFormatters.formatDateFull(r.nextDueMillis, lang)),
                        CellVal.Text(if (r.active) "Yes" else "Paused")
                    )
                )
            }
        }

        ZipOutputStream(outputStream).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", contentTypesXml())
            writeZipEntry(zip, "_rels/.rels", rootRelsXml())
            writeZipEntry(zip, "xl/workbook.xml", workbookXml())
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            writeZipEntry(zip, "xl/styles.xml", stylesXml())
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", worksheetXml(summaryRows))
            writeZipEntry(zip, "xl/worksheets/sheet2.xml", worksheetXml(txRows))
            writeZipEntry(zip, "xl/worksheets/sheet3.xml", worksheetXml(planningRows))
        }
    }

    private sealed interface CellVal {
        data class Text(val value: String) : CellVal
        data class Num(val value: Double) : CellVal
    }

    private fun writeZipEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun escapeXml(raw: String): String {
        return raw
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun colRef(index: Int): String {
        var idx = index
        val sb = StringBuilder()
        while (idx >= 0) {
            sb.insert(0, ('A'.code + (idx % 26)).toChar())
            idx = idx / 26 - 1
        }
        return sb.toString()
    }

    private fun worksheetXml(rows: List<List<CellVal>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed { rowIdx, cells ->
            val rNum = rowIdx + 1
            sb.append("""<row r="$rNum">""")
            cells.forEachIndexed { colIdx, cell ->
                val ref = "${colRef(colIdx)}$rNum"
                when (cell) {
                    is CellVal.Text -> {
                        sb.append("""<c r="$ref" t="inlineStr"><is><t>${escapeXml(cell.value)}</t></is></c>""")
                    }
                    is CellVal.Num -> {
                        sb.append("""<c r="$ref"><v>${cell.value}</v></c>""")
                    }
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

    private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Summary" sheetId="1" r:id="rId1"/>
    <sheet name="Transactions" sheetId="2" r:id="rId2"/>
    <sheet name="Budgets &amp; Recurring" sheetId="3" r:id="rId3"/>
  </sheets>
</workbook>"""

    private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
  <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
  <borders count="1"><border/></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>"""
}
