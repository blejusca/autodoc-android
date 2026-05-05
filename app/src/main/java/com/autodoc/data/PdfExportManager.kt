package com.autodoc.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.autodoc.ui.CarUi
import com.autodoc.ui.localizedText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

object PdfExportManager {

    suspend fun exportCarPdfToDownloads(
        context: Context,
        car: CarUi
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()

                val pageWidth = 595
                val pageHeight = 842
                val margin = 38
                val bottomLimit = pageHeight - 62

                var pageNumber = 1
                var y = 32

                var page = createPage(pdfDocument, pageWidth, pageHeight, pageNumber)
                var canvas = page.canvas

                val navy = Color.rgb(17, 24, 39)
                val lightCard = Color.rgb(249, 250, 251)
                val border = Color.rgb(220, 224, 230)
                val muted = Color.rgb(90, 97, 110)
                val red = Color.rgb(220, 38, 38)
                val green = Color.rgb(22, 163, 74)
                val orange = Color.rgb(217, 119, 6)

                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 21f
                    color = Color.WHITE
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 9.5f
                    color = Color.WHITE
                }

                val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 12.8f
                    color = navy
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 8.8f
                    color = muted
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 10.2f
                    color = navy
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 9.2f
                    color = navy
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }

                val tableBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 9.2f
                    color = navy
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 8.8f
                    color = muted
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 8.2f
                    color = muted
                }

                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = border
                    strokeWidth = 1f
                }

                val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = lightCard
                }

                val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = navy
                }

                fun finishCurrentPage() {
                    drawFooter(canvas, footerPaint, pageWidth, pageHeight, margin, pageNumber)
                    pdfDocument.finishPage(page)
                }

                fun newPage() {
                    finishCurrentPage()
                    pageNumber++
                    page = createPage(pdfDocument, pageWidth, pageHeight, pageNumber)
                    canvas = page.canvas
                    y = 32
                }

                fun ensureSpace(required: Int) {
                    if (y + required > bottomLimit) {
                        newPage()
                    }
                }

                fun drawHeader() {
                    canvas.drawRect(RectF(0f, 0f, pageWidth.toFloat(), 82f), headerPaint)

                    canvas.drawText(localizedText("Raport mașină", "Vehicle report"), margin.toFloat(), 36f, titlePaint)
                    canvas.drawText(
                        "CarGuard Business • generat la ${LocalDate.now()}",
                        margin.toFloat(),
                        55f,
                        subtitlePaint
                    )

                    y = 106
                }

                fun drawSectionTitle(title: String) {
                    ensureSpace(22)
                    canvas.drawText(title, margin.toFloat(), y.toFloat(), sectionPaint)
                    y += 8
                    canvas.drawLine(
                        margin.toFloat(),
                        y.toFloat(),
                        (pageWidth - margin).toFloat(),
                        y.toFloat(),
                        linePaint
                    )
                    y += 11
                }

                fun drawInfoBox(
                    label: String,
                    value: String,
                    left: Float,
                    top: Float,
                    width: Float,
                    height: Float
                ) {
                    val rect = RectF(left, top, left + width, top + height)

                    canvas.drawRoundRect(rect, 7f, 7f, cardPaint)

                    canvas.drawRoundRect(
                        rect,
                        7f,
                        7f,
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = border
                            style = Paint.Style.STROKE
                            strokeWidth = 1f
                        }
                    )

                    canvas.drawText(label, left + 9f, top + 14f, labelPaint)
                    canvas.drawText(value.ifBlank { "Nespecificat" }, left + 9f, top + 32f, valuePaint)
                }

                fun drawFullInfoBox(label: String, value: String) {
                    ensureSpace(39)
                    drawInfoBox(
                        label = label,
                        value = value,
                        left = margin.toFloat(),
                        top = y.toFloat(),
                        width = (pageWidth - margin * 2).toFloat(),
                        height = 37f
                    )
                    y += 43
                }

                fun drawWrappedText(text: String, maxLength: Int = 100) {
                    val lines = splitText(text.ifBlank { "Nespecificat" }, maxLength)
                    lines.forEach { line ->
                        ensureSpace(12)
                        canvas.drawText(line, margin.toFloat(), y.toFloat(), normalPaint)
                        y += 12
                    }
                }

                fun statusColor(daysLeft: Int): Int {
                    return when {
                        daysLeft < 0 -> red
                        daysLeft <= 7 -> orange
                        else -> green
                    }
                }

                drawHeader()

                drawSectionTitle(localizedText("Date mașină", "Vehicle details"))

                val boxGap = 9f
                val boxWidth = ((pageWidth - margin * 2) - boxGap) / 2f
                val boxHeight = 37f

                ensureSpace(84)

                drawInfoBox("Marca", car.brand, margin.toFloat(), y.toFloat(), boxWidth, boxHeight)
                drawInfoBox("Model", car.model, margin + boxWidth + boxGap, y.toFloat(), boxWidth, boxHeight)

                y += 43

                drawInfoBox("Numar inmatriculare", car.plate, margin.toFloat(), y.toFloat(), boxWidth, boxHeight)
                drawInfoBox("An", car.year.toString(), margin + boxWidth + boxGap, y.toFloat(), boxWidth, boxHeight)

                y += 43

                drawFullInfoBox("Motorizare", car.engine)

                y += 2

                drawSectionTitle(localizedText("Date client / proprietar", "Client / owner details"))
                drawFullInfoBox(localizedText("Nume client", "Client name"), car.ownerName)
                drawFullInfoBox("Telefon", car.ownerPhone)
                drawFullInfoBox("Email", car.ownerEmail)

                y += 8

                canvas.drawLine(
                    margin.toFloat(),
                    y.toFloat(),
                    (pageWidth - margin).toFloat(),
                    y.toFloat(),
                    linePaint
                )

                y += 12

                drawSectionTitle(localizedText("Documente", "Documents"))

                if (car.documents.isEmpty()) {
                    ensureSpace(16)
                    canvas.drawText(localizedText("Nu există documente adăugate.", "No documents added."), margin.toFloat(), y.toFloat(), normalPaint)
                    y += 16
                } else {
                    val tableLeft = margin
                    val tableRight = pageWidth - margin
                    val rowHeight = 20

                    val colType = tableLeft + 6
                    val colDate = tableLeft + 120
                    val colStatus = tableLeft + 232
                    val colReminderRight = tableRight - 6

                    ensureSpace(30)

                    canvas.drawText(localizedText("Document", "Document"), colType.toFloat(), y.toFloat(), tableHeaderPaint)
                    canvas.drawText(localizedText("Expiră", "Expires"), colDate.toFloat(), y.toFloat(), tableHeaderPaint)
                    canvas.drawText(localizedText("Status", "Status"), colStatus.toFloat(), y.toFloat(), tableHeaderPaint)

                    val reminderHeader = localizedText("Notificare", "Notification")
                    canvas.drawText(
                        reminderHeader,
                        colReminderRight - tableHeaderPaint.measureText(reminderHeader),
                        y.toFloat(),
                        tableHeaderPaint
                    )

                    y += 7

                    canvas.drawLine(
                        tableLeft.toFloat(),
                        y.toFloat(),
                        tableRight.toFloat(),
                        y.toFloat(),
                        linePaint
                    )

                    y += 14

                    car.documents.sortedBy { it.expiryDateMillis }.forEach { doc ->
                        ensureSpace(rowHeight + 2)

                        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = 8.8f
                            color = statusColor(doc.daysLeft)
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }

                        val statusText = documentStatusText(doc.daysLeft)
                        val reminderText = "${doc.reminderDaysBefore} zile"

                        canvas.drawText(doc.type, colType.toFloat(), y.toFloat(), tableBoldPaint)
                        canvas.drawText(formatDate(doc.expiryDateMillis), colDate.toFloat(), y.toFloat(), normalPaint)
                        canvas.drawText(" $statusText", colStatus.toFloat(), y.toFloat(), statusPaint)

                        canvas.drawText(
                            reminderText,
                            colReminderRight - tableBoldPaint.measureText(reminderText),
                            y.toFloat(),
                            tableBoldPaint
                        )

                        y += rowHeight

                        canvas.drawLine(
                            tableLeft.toFloat(),
                            (y - 10).toFloat(),
                            tableRight.toFloat(),
                            (y - 10).toFloat(),
                            linePaint
                        )
                    }
                }

                if (car.ownerNotes.isNotBlank()) {
                    y += 2
                    drawSectionTitle(localizedText("Observații client", "Client notes"))
                    drawWrappedText(car.ownerNotes)
                }

                finishCurrentPage()

                val fileName = buildPdfFileName(car)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver

                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }

                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext false

                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    } ?: return@withContext false

                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                } else {
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!dir.exists()) dir.mkdirs()

                    val file = File(dir, fileName)
                    FileOutputStream(file).use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                }

                pdfDocument.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun createPage(
        pdf: PdfDocument,
        width: Int,
        height: Int,
        number: Int
    ): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(width, height, number).create()
        return pdf.startPage(info)
    }

    private fun drawFooter(
        canvas: Canvas,
        paint: Paint,
        pageWidth: Int,
        pageHeight: Int,
        margin: Int,
        pageNumber: Int
    ) {
        canvas.drawLine(
            margin.toFloat(),
            (pageHeight - 48).toFloat(),
            (pageWidth - margin).toFloat(),
            (pageHeight - 48).toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(229, 231, 235)
                strokeWidth = 1f
            }
        )

        canvas.drawText(
            "Raport generat automat de CarGuard Business",
            margin.toFloat(),
            (pageHeight - 29).toFloat(),
            paint
        )

        canvas.drawText(
            "Pagina $pageNumber",
            (pageWidth - margin - 45).toFloat(),
            (pageHeight - 29).toFloat(),
            paint
        )
    }

    private fun buildPdfFileName(car: CarUi): String {
        val cleanPlate = car.plate
            .ifBlank { localizedText("mașină", "vehicle") }
            .replace(Regex("[^A-Za-z0-9_-]"), "_")

        return "raport_${cleanPlate}_${LocalDate.now()}.pdf"
    }

    private fun splitText(text: String, maxLength: Int): List<String> {
        if (text.length <= maxLength) return listOf(text)

        val result = mutableListOf<String>()
        var remaining = text.trim()

        while (remaining.length > maxLength) {
            val splitIndex = remaining
                .take(maxLength)
                .lastIndexOf(' ')
                .takeIf { it > 0 } ?: maxLength

            result.add(remaining.take(splitIndex).trim())
            remaining = remaining.drop(splitIndex).trim()
        }

        if (remaining.isNotBlank()) {
            result.add(remaining)
        }

        return result
    }

    private fun formatDate(millis: Long): String {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }

    private fun documentStatusText(daysLeft: Int): String {
        return when {
            daysLeft < 0 -> {
                val days = abs(daysLeft)
                "expirat de " + days + " " + if (days == 1) "zi" else "zile"
            }

            daysLeft == 0 -> "expira azi"
            daysLeft == 1 -> "expira maine"
            else -> "in " + daysLeft + " zile"
        }
    }
}