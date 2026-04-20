package com.ordemservico.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ordemservico.app.ui.viewmodels.AuthViewModel
import com.ordemservico.app.ui.viewmodels.OrderViewModel

data class ItemForm(
    val description: String = "",
    val quantity: String = "1",
    val unitPrice: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditOrderScreen(
    authViewModel: AuthViewModel,
    orderViewModel: OrderViewModel,
    orderId: String?,
    onSuccess: (String) -> Unit,
    onBack: () -> Unit
) {
    val authState by authViewModel.state.collectAsState()
    val orderState by orderViewModel.state.collectAsState()
    val isEditing = orderId != null

    // Form state
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var clientAddress by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf(ItemForm())) }
    var initialized by remember { mutableStateOf(false) }

    // Carrega dados ao editar
    LaunchedEffect(orderId) {
        if (isEditing && !initialized) {
            orderViewModel.loadOrder(orderId!!)
        }
    }

    LaunchedEffect(orderState.selectedOrder) {
        if (isEditing && !initialized && orderState.selectedOrder != null) {
            val order = orderState.selectedOrder!!
            title = order.title
            description = order.description
            notes = order.notes
            clientName = order.clientName
            clientPhone = order.clientPhone
            clientEmail = order.clientEmail
            clientAddress = order.clientAddress
            items = if (order.items.isNotEmpty()) {
                order.items.map {
                    ItemForm(it.description, it.quantity.toString(), it.unitPrice.toString())
                }
            } else listOf(ItemForm())
            initialized = true
        }
    }

    // Total calculado
    val total = items.sumOf {
        val qty = it.quantity.toDoubleOrNull() ?: 0.0
        val price = it.unitPrice.toDoubleOrNull() ?: 0.0
        qty * price
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Editar Ordem" else "Nova Ordem",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // ── Dados da OS ────────────────────────────────────────
            item {
                SectionHeader(icon = "📋", title = "Dados da Ordem")
            }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título da OS *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição do serviço") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }

            // ── Dados do Cliente ──────────────────────────────────
            item { Spacer(Modifier.height(8.dp)) }
            item {
                SectionHeader(icon = "👤", title = "Dados do Cliente")
            }
            item {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nome do cliente *") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = clientPhone,
                    onValueChange = { clientPhone = it },
                    label = { Text("Telefone / WhatsApp") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = clientEmail,
                    onValueChange = { clientEmail = it },
                    label = { Text("E-mail do cliente") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = clientAddress,
                    onValueChange = { clientAddress = it },
                    label = { Text("Endereço") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            // ── Itens do Orçamento ────────────────────────────────
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(icon = "🧾", title = "Itens do Orçamento")
                    TextButton(onClick = { items = items + ItemForm() }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Adicionar")
                    }
                }
            }

            itemsIndexed(items) { index, item ->
                ItemFormCard(
                    item = item,
                    index = index,
                    canDelete = items.size > 1,
                    onUpdate = { updated ->
                        items = items.toMutableList().also { it[index] = updated }
                    },
                    onDelete = {
                        items = items.toMutableList().also { it.removeAt(index) }
                    }
                )
            }

            // Total
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total do Orçamento", fontWeight = FontWeight.Bold)
                        Text(
                            "R$ %.2f".format(total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── Observações ───────────────────────────────────────
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader(icon = "📝", title = "Observações") }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações adicionais") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            // Erro
            if (orderState.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            orderState.error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Botão salvar
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val userId = authState.userId ?: return@Button
                        val formItems = items
                            .filter { it.description.isNotBlank() }
                            .map { it.description to (
                                (it.quantity.toDoubleOrNull() ?: 1.0) to
                                (it.unitPrice.toDoubleOrNull() ?: 0.0)
                            )}

                        if (isEditing) {
                            orderViewModel.updateOrder(
                                orderId = orderId!!,
                                userId = userId,
                                clientName = clientName,
                                clientPhone = clientPhone,
                                clientEmail = clientEmail,
                                clientAddress = clientAddress,
                                title = title,
                                description = description,
                                notes = notes,
                                items = formItems,
                                onSuccess = { onSuccess(orderId) }
                            )
                        } else {
                            orderViewModel.createOrder(
                                userId = userId,
                                clientName = clientName,
                                clientPhone = clientPhone,
                                clientEmail = clientEmail,
                                clientAddress = clientAddress,
                                title = title,
                                description = description,
                                notes = notes,
                                items = formItems,
                                onSuccess = onSuccess
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = title.isNotBlank() && clientName.isNotBlank() && !orderState.isSaving
                ) {
                    if (orderState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isEditing) "Salvar Alterações" else "Criar Ordem de Serviço")
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon)
        Spacer(Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outline.copy(0.3f))
}

@Composable
private fun ItemFormCard(
    item: ItemForm,
    index: Int,
    canDelete: Boolean,
    onUpdate: (ItemForm) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Item ${index + 1}", fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = item.description,
                onValueChange = { onUpdate(item.copy(description = it)) },
                label = { Text("Descrição do item *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.quantity,
                    onValueChange = { onUpdate(item.copy(quantity = it)) },
                    label = { Text("Qtd") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = item.unitPrice,
                    onValueChange = { onUpdate(item.copy(unitPrice = it)) },
                    label = { Text("Valor Unit. (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
            }
            val subtotal = (item.quantity.toDoubleOrNull() ?: 0.0) * (item.unitPrice.toDoubleOrNull() ?: 0.0)
            if (subtotal > 0) {
                Text(
                    "Subtotal: R$ %.2f".format(subtotal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            }
        }
    }
}
