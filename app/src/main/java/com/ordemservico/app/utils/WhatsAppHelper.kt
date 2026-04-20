package com.ordemservico.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ordemservico.app.data.models.ServiceOrder
import java.io.File
import java.text.NumberFormat
import java.util.Locale

object WhatsAppHelper {

    private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    /**
     * Compartilha o PDF da ordem de serviço diretamente no WhatsApp.
     * Se o cliente tiver número, abre conversa com ele.
     * Caso contrário, abre o seletor padrão de compartilhamento.
     */
    fun sharePdf(context: Context, order: ServiceOrder, pdfFile: File) {
        val pdfUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        // Mensagem de texto junto com o PDF
        val message = buildMessage(order)

        val phone = order.clientPhone
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace("+", "")
            .let { if (!it.startsWith("55")) "55$it" else it }

        val hasWhatsApp = isWhatsAppInstalled(context)

        if (hasWhatsApp && order.clientPhone.isNotBlank()) {
            // Envia diretamente para o número do cliente
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra("jid", "$phone@s.whatsapp.net")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Enviar orçamento via WhatsApp"))
        } else {
            // Compartilhamento genérico
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                if (hasWhatsApp) setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar orçamento"))
        }
    }

    /**
     * Compartilha apenas a mensagem de texto (sem PDF) via WhatsApp
     */
    fun shareTextOnly(context: Context, order: ServiceOrder) {
        val message = buildMessage(order)
        val phone = order.clientPhone
            .replace(Regex("[^0-9]"), "")
            .let { if (!it.startsWith("55")) "55$it" else it }

        val intent = if (order.clientPhone.isNotBlank() && isWhatsAppInstalled(context)) {
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
        }
        context.startActivity(intent)
    }

    private fun buildMessage(order: ServiceOrder): String {
        val sb = StringBuilder()
        sb.appendLine("🔧 *ORDEM DE SERVIÇO - ORÇAMENTO*")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📋 *${order.title}*")
        sb.appendLine()
        sb.appendLine("👤 *Cliente:* ${order.clientName}")
        if (order.clientAddress.isNotBlank()) sb.appendLine("📍 *Endereço:* ${order.clientAddress}")
        sb.appendLine()

        if (order.description.isNotBlank()) {
            sb.appendLine("📝 *Descrição:*")
            sb.appendLine(order.description)
            sb.appendLine()
        }

        if (order.items.isNotEmpty()) {
            sb.appendLine("🧾 *Itens do Orçamento:*")
            order.items.forEach { item ->
                val qty = if (item.quantity == item.quantity.toLong().toDouble())
                    item.quantity.toLong().toString() else "%.1f".format(item.quantity)
                sb.appendLine("• ${item.description} — $qty x ${currency.format(item.unitPrice)} = *${currency.format(item.total)}*")
            }
            sb.appendLine()
            sb.appendLine("💰 *TOTAL: ${currency.format(order.totalAmount)}*")
        }

        if (order.notes.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("📌 *Obs:* ${order.notes}")
        }

        sb.appendLine()
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("_Orçamento gerado pelo app Ordem de Serviço_")

        return sb.toString()
    }

    private fun isWhatsAppInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo("com.whatsapp", 0)
        true
    } catch (e: Exception) { false }
}
