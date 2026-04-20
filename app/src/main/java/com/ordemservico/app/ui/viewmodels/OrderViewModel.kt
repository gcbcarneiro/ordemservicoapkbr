package com.ordemservico.app.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ordemservico.app.data.models.*
import com.ordemservico.app.data.repository.ServiceOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OrderUiState(
    val orders: List<ServiceOrder> = emptyList(),
    val selectedOrder: ServiceOrder? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class OrderViewModel : ViewModel() {

    private val repo = ServiceOrderRepository()

    private val _state = MutableStateFlow(OrderUiState())
    val state: StateFlow<OrderUiState> = _state

    fun loadOrders(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repo.getOrders(userId)
                .onSuccess { orders ->
                    _state.value = _state.value.copy(orders = orders, isLoading = false)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = it.message ?: "Erro ao carregar ordens"
                    )
                }
        }
    }

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            repo.getOrder(orderId)
                .onSuccess { order ->
                    _state.value = _state.value.copy(selectedOrder = order, isLoading = false)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = it.message
                    )
                }
        }
    }

    fun createOrder(
        userId: String,
        clientName: String,
        clientPhone: String,
        clientEmail: String,
        clientAddress: String,
        title: String,
        description: String,
        notes: String,
        items: List<Pair<String, Pair<Double, Double>>>, // (desc, (qty, price))
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)

            val newOrder = NewServiceOrder(
                userId = userId,
                clientName = clientName,
                clientPhone = clientPhone,
                clientEmail = clientEmail,
                clientAddress = clientAddress,
                title = title,
                description = description,
                notes = notes
            )

            repo.createOrder(newOrder)
                .onSuccess { order ->
                    if (items.isNotEmpty()) {
                        val newItems = items.map { (desc, qtyPrice) ->
                            NewServiceItem(
                                orderId = order.id,
                                description = desc,
                                quantity = qtyPrice.first,
                                unitPrice = qtyPrice.second
                            )
                        }
                        repo.addItems(newItems)
                    }
                    _state.value = _state.value.copy(isSaving = false)
                    onSuccess(order.id)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = it.message ?: "Erro ao criar ordem"
                    )
                }
        }
    }

    fun updateOrder(
        orderId: String,
        userId: String,
        clientName: String,
        clientPhone: String,
        clientEmail: String,
        clientAddress: String,
        title: String,
        description: String,
        notes: String,
        items: List<Pair<String, Pair<Double, Double>>>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)

            val updatedOrder = NewServiceOrder(
                userId = userId,
                clientName = clientName,
                clientPhone = clientPhone,
                clientEmail = clientEmail,
                clientAddress = clientAddress,
                title = title,
                description = description,
                notes = notes
            )

            repo.updateOrder(orderId, updatedOrder)
                .onSuccess {
                    repo.deleteItemsByOrder(orderId)
                    if (items.isNotEmpty()) {
                        val newItems = items.map { (desc, qtyPrice) ->
                            NewServiceItem(
                                orderId = orderId,
                                description = desc,
                                quantity = qtyPrice.first,
                                unitPrice = qtyPrice.second
                            )
                        }
                        repo.addItems(newItems)
                    }
                    _state.value = _state.value.copy(isSaving = false)
                    onSuccess()
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = it.message ?: "Erro ao atualizar ordem"
                    )
                }
        }
    }

    fun updateStatus(orderId: String, status: String, userId: String) {
        viewModelScope.launch {
            repo.updateStatus(orderId, status)
                .onSuccess { loadOrders(userId) }
                .onFailure {
                    _state.value = _state.value.copy(error = it.message)
                }
        }
    }

    fun deleteOrder(orderId: String, userId: String) {
        viewModelScope.launch {
            repo.deleteOrder(orderId)
                .onSuccess { loadOrders(userId) }
                .onFailure {
                    _state.value = _state.value.copy(error = it.message)
                }
        }
    }

    fun uploadImage(context: Context, orderId: String, uri: Uri, userId: String) {
        viewModelScope.launch {
            repo.uploadImage(context, orderId, uri, userId)
                .onSuccess { loadOrder(orderId) }
                .onFailure {
                    _state.value = _state.value.copy(error = it.message)
                }
        }
    }

    fun clearError() = run { _state.value = _state.value.copy(error = null) }
    fun clearSuccess() = run { _state.value = _state.value.copy(successMessage = null) }
}
