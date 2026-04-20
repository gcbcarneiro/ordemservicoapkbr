package com.ordemservico.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceOrder(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("client_name") val clientName: String = "",
    @SerialName("client_phone") val clientPhone: String = "",
    @SerialName("client_email") val clientEmail: String = "",
    @SerialName("client_address") val clientAddress: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "pending",
    val notes: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val items: List<ServiceItem> = emptyList(),
    val images: List<OrderImage> = emptyList()
) {
    val totalAmount: Double
        get() = items.sumOf { it.quantity * it.unitPrice }

    val statusLabel: String
        get() = when (status) {
            "pending" -> "Pendente"
            "approved" -> "Aprovado"
            "in_progress" -> "Em Andamento"
            "completed" -> "Concluído"
            "cancelled" -> "Cancelado"
            else -> status
        }
}

@Serializable
data class ServiceItem(
    val id: String = "",
    @SerialName("order_id") val orderId: String = "",
    val description: String = "",
    val quantity: Double = 1.0,
    @SerialName("unit_price") val unitPrice: Double = 0.0
) {
    val total: Double get() = quantity * unitPrice
}

@Serializable
data class OrderImage(
    val id: String = "",
    @SerialName("order_id") val orderId: String = "",
    val url: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

// Para inserção (sem id e timestamps)
@Serializable
data class NewServiceOrder(
    @SerialName("user_id") val userId: String,
    @SerialName("client_name") val clientName: String,
    @SerialName("client_phone") val clientPhone: String,
    @SerialName("client_email") val clientEmail: String,
    @SerialName("client_address") val clientAddress: String,
    val title: String,
    val description: String,
    val status: String = "pending",
    val notes: String = ""
)

@Serializable
data class NewServiceItem(
    @SerialName("order_id") val orderId: String,
    val description: String,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double
)

@Serializable
data class NewOrderImage(
    @SerialName("order_id") val orderId: String,
    val url: String
)
