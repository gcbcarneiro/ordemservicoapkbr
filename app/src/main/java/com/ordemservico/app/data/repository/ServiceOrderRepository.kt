package com.ordemservico.app.data.repository

import android.content.Context
import android.net.Uri
import com.ordemservico.app.SupabaseClient
import com.ordemservico.app.data.models.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ServiceOrderRepository {

    private val db = SupabaseClient.client.postgrest
    private val storage = SupabaseClient.client.storage
    private val realtime = SupabaseClient.client.realtime

    // ─── Orders ────────────────────────────────────────────────────────────

    suspend fun getOrders(userId: String): Result<List<ServiceOrder>> = runCatching {
        db.from("service_orders")
            .select(Columns.raw("*, service_items(*), order_images(*)")) {
                filter { eq("user_id", userId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<ServiceOrder>()
    }

    suspend fun getOrder(orderId: String): Result<ServiceOrder> = runCatching {
        db.from("service_orders")
            .select(Columns.raw("*, service_items(*), order_images(*)")) {
                filter { eq("id", orderId) }
                single()
            }
            .decodeSingle<ServiceOrder>()
    }

    suspend fun createOrder(order: NewServiceOrder): Result<ServiceOrder> = runCatching {
        db.from("service_orders")
            .insert(order) { select(Columns.raw("*, service_items(*), order_images(*)")) }
            .decodeSingle<ServiceOrder>()
    }

    suspend fun updateOrder(orderId: String, order: NewServiceOrder): Result<ServiceOrder> = runCatching {
        db.from("service_orders")
            .update(order) {
                filter { eq("id", orderId) }
                select(Columns.raw("*, service_items(*), order_images(*)"))
            }
            .decodeSingle<ServiceOrder>()
    }

    suspend fun updateStatus(orderId: String, status: String): Result<Unit> = runCatching {
        db.from("service_orders")
            .update(mapOf("status" to status, "updated_at" to "now()")) {
                filter { eq("id", orderId) }
            }
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> = runCatching {
        db.from("service_orders")
            .delete { filter { eq("id", orderId) } }
    }

    // ─── Items ──────────────────────────────────────────────────────────────

    suspend fun addItems(items: List<NewServiceItem>): Result<Unit> = runCatching {
        if (items.isNotEmpty()) {
            db.from("service_items").insert(items)
        }
    }

    suspend fun deleteItemsByOrder(orderId: String): Result<Unit> = runCatching {
        db.from("service_items")
            .delete { filter { eq("order_id", orderId) } }
    }

    // ─── Images ─────────────────────────────────────────────────────────────

    suspend fun uploadImage(
        context: Context,
        orderId: String,
        uri: Uri,
        userId: String
    ): Result<String> = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: throw Exception("Não foi possível ler o arquivo")

        val ext = context.contentResolver.getType(uri)?.substringAfter("/") ?: "jpg"
        val path = "$userId/$orderId/${UUID.randomUUID()}.$ext"

        storage.from("order-images").upload(path, bytes) { upsert = false }

        val publicUrl = storage.from("order-images").publicUrl(path)

        db.from("order_images").insert(NewOrderImage(orderId = orderId, url = publicUrl))

        publicUrl
    }

    suspend fun deleteImage(imageId: String): Result<Unit> = runCatching {
        db.from("order_images").delete { filter { eq("id", imageId) } }
    }

    // ─── Realtime ────────────────────────────────────────────────────────────

    fun observeOrderChanges(userId: String): Flow<Unit> {
        val channel = realtime.channel("orders-$userId")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "service_orders"
        }.map { }
    }
}
