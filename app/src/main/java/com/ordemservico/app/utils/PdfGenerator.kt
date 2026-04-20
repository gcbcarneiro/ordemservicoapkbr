package com.ordemservico.app.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.ordemservico.app.data.models.ServiceOrder
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    fun generate(context: Context, order: ServiceOrder): File {
        val document = PdfDocument()
        val pageWidth = 595  // A4 largura em pontos (72dpi)
        val pageHeight = 842 // A4 altura em pontos

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawPage(canvas, order, pageWidth, pageHeight)

        document.finishPage(page)

        // Salva no cache
        val pdfDir = File(context.cacheDir, "pdfs")
        pdfDir.mkdirs()
        val file = File(pdfDir, "OS_${order.id.take(8)}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun drawPage(canvas: Canvas, order: ServiceOrder, pageWidth: Int, pageHeight: Int) {
        var y = 0f
        val margin = 40f
        val contentWidth = pageWidth - margin * 2

        // ── Cabeçalho azul ────────────────────────────────────────
        val headerPaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("ORDEM DE SERVIÇO", margin, 48f, titlePaint)

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#BBDEFB")
            textSize = 13f
        }
        canvas.drawText("Orçamento • ${order.title}", margin, 72f, subtitlePaint)

        val datePaint = Paint().apply {
            color = Color.parseColor("#BBDEFB")
            textSize = 11f
        }
        val createdDate = try {
            val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = iso.parse(order.createdAt.take(19))
            dateFormat.format(date ?: Date())
        } catch (e: Exception) { dateFormat.format(Date()) }

        canvas.drawText("Data: $createdDate", margin, 92f, datePaint)
        canvas.drawText("Protocolo: #${order.id.take(8).uppercase()}", pageWidth - 180f, 92f, datePaint)

        y = 130f

        // ── Dados do Cliente ──────────────────────────────────────
        y = drawSection(canvas, "DADOS DO CLIENTE", y, margin, contentWidth)

        val labelPaint = Paint().apply { color = Color.parseColor("#666666"); textSize = 10f }
        val valuePaint = Paint().apply { color = Color.parseColor("#1A1A1A"); textSize = 12f; typeface = Typeface.DEFAULT_BOLD }
        val bodyPaint  = Paint().apply { color = Color.parseColor("#333333"); textSize = 12f }

        canvas.drawText("Nome:", margin, y, labelPaint); y += 16f
        canvas.drawText(order.clientName, margin, y, valuePaint); y += 20f
        if (order.clientPhone.isNotBlank()) {
            canvas.drawText("Telefone:", margin, y, labelPaint); y += 16f
            canvas.drawText(order.clientPhone, margin, y, bodyPaint); y += 20f
        }
        if (order.clientEmail.isNotBlank()) {
            canvas.drawText("E-mail:", margin, y, labelPaint); y += 16f
            canvas.drawText(order.clientEmail, margin, y, bodyPaint); y += 20f
        }
        if (order.clientAddress.isNotBlank()) {
            canvas.drawText("Endereço:", margin, y, labelPaint); y += 16f
            canvas.drawText(order.clientAddress, margin, y, bodyPaint); y += 20f
        }

        y += 10f

        // ── Descrição ─────────────────────────────────────────────
        if (order.description.isNotBlank()) {
            y = drawSection(canvas, "DESCRIÇÃO DO SERVIÇO", y, margin, contentWidth)
            y = drawWrappedText(canvas, order.description, margin, y, contentWidth, bodyPaint)
            y += 10f
        }

        // ── Status ────────────────────────────────────────────────
        y = drawSection(canvas, "STATUS", y, margin, contentWidth)
        val statusBgPaint = Paint().apply {
            color = statusColor(order.status)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(margin, y - 14f, margin + 120f, y + 6f, 8f, 8f, statusBgPaint)
        val statusTextPaint = Paint().apply { color = Color.WHITE; textSize = 11f; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText(order.statusLabel.uppercase(), margin + 8f, y, statusTextPaint)
        y += 20f

        // ── Itens ─────────────────────────────────────────────────
        if (order.items.isNotEmpty()) {
            y += 4f
            y = drawSection(canvas, "ITENS DO ORÇAMENTO", y, margin, contentWidth)

            // Cabeçalho da tabela
            val tableHeaderPaint = Paint().apply { color = Color.parseColor("#E3F2FD"); style = Paint.Style.FILL }
            canvas.drawRect(margin, y - 14f, margin + contentWidth, y + 8f, tableHeaderPaint)
            val thPaint = Paint().apply { color = Color.parseColor("#1565C0"); textSize = 10f; typeface = Typeface.DEFAULT_BOLD }
            canvas.drawText("DESCRIÇÃO", margin + 4f, y, thPaint)
            canvas.drawText("QTD", margin + contentWidth * 0.62f, y, thPaint)
            canvas.drawText("UNIT.", margin + contentWidth * 0.73f, y, thPaint)
            canvas.drawText("TOTAL", margin + contentWidth * 0.86f, y, thPaint)
            y += 20f

            // Linhas dos itens
            order.items.forEachIndexed { idx, item ->
                val rowBg = Paint().apply {
                    color = if (idx % 2 == 0) Color.WHITE else Color.parseColor("#FAFAFA")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(margin, y - 14f, margin + contentWidth, y + 8f, rowBg)
                canvas.drawText(item.description.take(42), margin + 4f, y, bodyPaint)
                canvas.drawText(item.quantity.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.1f".format(it) }, margin + contentWidth * 0.62f, y, bodyPaint)
                canvas.drawText(currency.format(item.unitPrice), margin + contentWidth * 0.71f, y, bodyPaint)
                val totalPaint = Paint().apply { color = Color.parseColor("#1565C0"); textSize = 12f; typeface = Typeface.DEFAULT_BOLD }
                canvas.drawText(currency.format(item.total), margin + contentWidth * 0.85f, y, totalPaint)
                y += 22f
            }

            // Linha separadora
            val linePaint = Paint().apply { color = Color.parseColor("#1565C0"); strokeWidth = 1.5f }
            canvas.drawLine(margin, y, margin + contentWidth, y, linePaint)
            y += 18f

            // Total geral
            val totalBgPaint = Paint().apply { color = Color.parseColor("#1565C0"); style = Paint.Style.FILL }
            canvas.drawRoundRect(margin + contentWidth * 0.6f, y - 18f, margin + contentWidth, y + 8f, 6f, 6f, totalBgPaint)
            val totalLabelPaint = Paint().apply { color = Color.WHITE; textSize = 11f }
            canvas.drawText("TOTAL GERAL:", margin + contentWidth * 0.62f, y - 4f, totalLabelPaint)
            val totalValuePaint = Paint().apply { color = Color.WHITE; textSize = 14f; typeface = Typeface.DEFAULT_BOLD }
            canvas.drawText(currency.format(order.totalAmount), margin + contentWidth * 0.78f, y - 4f, totalValuePaint)
            y += 24f
        }

        // ── Observações ───────────────────────────────────────────
        if (order.notes.isNotBlank()) {
            y += 4f
            y = drawSection(canvas, "OBSERVAÇÕES", y, margin, contentWidth)
            y = drawWrappedText(canvas, order.notes, margin, y, contentWidth, bodyPaint)
        }

        // ── Rodapé ────────────────────────────────────────────────
        val footerPaint = Paint().apply { color = Color.parseColor("#1565C0"); style = Paint.Style.FILL }
        canvas.drawRect(0f, pageHeight - 40f, pageWidth.toFloat(), pageHeight.toFloat(), footerPaint)
        val footerTextPaint = Paint().apply { color = Color.parseColor("#BBDEFB"); textSize = 10f }
        canvas.drawText("Gerado pelo app Ordem de Serviço • ${dateFormat.format(Date())}", margin, pageHeight - 18f, footerTextPaint)
        canvas.drawText("Protocolo: #${order.id.take(8).uppercase()}", pageWidth - 180f, pageHeight - 18f, footerTextPaint)
    }

    private fun drawSection(canvas: Canvas, title: String, y: Float, margin: Float, contentWidth: Float): Float {
        var currentY = y
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#E8EAF6")
            style = Paint.Style.FILL
        }
        canvas.drawRect(margin, currentY - 14f, margin + contentWidth, currentY + 6f, sectionPaint)
        val sectionTextPaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(title, margin + 6f, currentY, sectionTextPaint)
        return currentY + 22f
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint): Float {
        var currentY = y
        val words = text.split(" ")
        var line = ""
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(testLine) > maxWidth - 8f) {
                canvas.drawText(line, x, currentY, paint)
                currentY += 18f
                line = word
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, x, currentY, paint)
            currentY += 18f
        }
        return currentY
    }

    private fun statusColor(status: String): Int = when (status) {
        "pending"     -> Color.parseColor("#FF9800")
        "approved"    -> Color.parseColor("#4CAF50")
        "in_progress" -> Color.parseColor("#2196F3")
        "completed"   -> Color.parseColor("#9C27B0")
        "cancelled"   -> Color.parseColor("#F44336")
        else -> Color.GRAY
    }
}
