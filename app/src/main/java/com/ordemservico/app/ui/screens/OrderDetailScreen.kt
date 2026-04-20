package com.ordemservico.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ordemservico.app.data.models.ServiceOrder
import com.ordemservico.app.ui.theme.statusColor
import com.ordemservico.app.ui.viewmodels.AuthViewModel
import com.ordemservico.app.ui.viewmodels.OrderViewModel
import com.ordemservico.app.utils.PdfGenerator
import com.ordemservico.app.utils.WhatsAppHelper
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    authViewModel: AuthViewModel,
    orderViewModel: OrderViewModel,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val authState by authViewModel.state.collectAsState()
    val orderState by orderViewModel.state.collectAsState()
    val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var isPdfGenerating by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { orderViewModel.uploadImage(context, orderId, it, authState.userId ?: "") }
    }

    LaunchedEffect(orderId) {
        orderViewModel.loadOrder(orderId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir Ordem") },
            text = { Text("Tem certeza que deseja excluir esta ordem? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    orderViewModel.deleteOrder(orderId, authState.userId ?: "")
                    onBack()
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(orderState.selectedOrder?.title ?: "Detalhes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null, tint = Color.White)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        val order = orderState.selectedOrder

        if (orderState.isLoading || order == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Status + Ações rápidas ─────────────────────────────
            item {
                Card(elevation = CardDefaults.cardElevation(3.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Status", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(statusColor(order.status).copy(alpha = 0.15f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(order.statusLabel, color = statusColor(order.status),
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                            Box {
                                OutlinedButton(onClick = { showStatusMenu = true }) {
                                    Text("Alterar Status")
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                DropdownMenu(
                                    expanded = showStatusMenu,
                                    onDismissRequest = { showStatusMenu = false }
                                ) {
                                    listOf(
                                        "pending" to "Pendente",
                                        "approved" to "Aprovado",
                                        "in_progress" to "Em Andamento",
                                        "completed" to "Concluído",
                                        "cancelled" to "Cancelado"
                                    ).forEach { (value, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                showStatusMenu = false
                                                orderViewModel.updateStatus(orderId, value, authState.userId ?: "")
                                                orderViewModel.loadOrder(orderId)
                                            },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp))
                                                        .background(statusColor(value))
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))
                        Spacer(Modifier.height(12.dp))

                        // Botões de ação
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Gerar PDF
                            Button(
                                onClick = {
                                    isPdfGenerating = true
                                    try {
                                        val pdf = PdfGenerator.generate(context, order)
                                        WhatsAppHelper.sharePdf(context, order, pdf)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isPdfGenerating = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isPdfGenerating
                            ) {
                                if (isPdfGenerating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text("PDF + WhatsApp", style = MaterialTheme.typography.labelMedium)
                            }

                            // Mensagem de texto
                            OutlinedButton(
                                onClick = { WhatsAppHelper.shareTextOnly(context, order) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("💬", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.width(4.dp))
                                Text("Só texto", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // ── Dados do Cliente ──────────────────────────────────
            item {
                DetailCard(title = "👤 Dados do Cliente") {
                    InfoRow("Nome", order.clientName)
                    if (order.clientPhone.isNotBlank()) InfoRow("Telefone", order.clientPhone)
                    if (order.clientEmail.isNotBlank()) InfoRow("E-mail", order.clientEmail)
                    if (order.clientAddress.isNotBlank()) InfoRow("Endereço", order.clientAddress)
                }
            }

            // ── Descrição ─────────────────────────────────────────
            if (order.description.isNotBlank()) {
                item {
                    DetailCard(title = "📋 Descrição") {
                        Text(order.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ── Itens ─────────────────────────────────────────────
            if (order.items.isNotEmpty()) {
                item {
                    DetailCard(title = "🧾 Itens do Orçamento") {
                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.description, fontWeight = FontWeight.Medium)
                                    val qty = if (item.quantity == item.quantity.toLong().toDouble())
                                        item.quantity.toLong().toString() else "%.1f".format(item.quantity)
                                    Text("$qty × ${currency.format(item.unitPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                }
                                Text(currency.format(item.total), fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Geral", fontWeight = FontWeight.Bold)
                            Text(currency.format(order.totalAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // ── Observações ───────────────────────────────────────
            if (order.notes.isNotBlank()) {
                item {
                    DetailCard(title = "📝 Observações") {
                        Text(order.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ── Imagens ───────────────────────────────────────────
            item {
                DetailCard(title = "📷 Fotos da OS") {
                    if (order.images.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            order.images.forEach { image ->
                                AsyncImage(
                                    model = image.url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Adicionar Foto")
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(0.2f))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
            modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f))
    }
}
