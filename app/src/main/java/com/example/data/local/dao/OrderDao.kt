package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.CarpetItemEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.model.OrderWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders ORDER BY routeOrder ASC, createdAt DESC")
    fun getAllOrdersWithItems(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderWithItemsById(orderId: String): OrderWithItems?

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun observeOrderWithItemsById(orderId: String): Flow<OrderWithItems?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarpetItem(item: CarpetItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarpetItems(items: List<CarpetItemEntity>)

    @Query("DELETE FROM carpet_items WHERE id = :itemId")
    suspend fun deleteCarpetItemById(itemId: Long)

    @Query("DELETE FROM carpet_items WHERE orderId = :orderId")
    suspend fun deleteAllItemsForOrder(orderId: String)

    @Query("UPDATE orders SET rackCode = :rackCode, status = 'DELIVERED_TO_WORKSHOP', isSynced = 0, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateRackCode(orderId: String, rackCode: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET paidAmount = :paidAmount, discountAmount = :discountAmount, paymentMethod = :paymentMethod, status = 'DELIVERED_SETTLED', isSynced = 0, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateSettlement(orderId: String, paidAmount: Long, discountAmount: Long, paymentMethod: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET status = :status, isSynced = 0, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET status = 'OFFICE_SETTLED', isSynced = 0, updatedAt = :updatedAt WHERE status = 'DELIVERED_SETTLED'")
    suspend fun archiveSettledOrders(updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM orders WHERE isSynced = 0")
    fun getUnsyncedOrdersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int

    @Query("SELECT * FROM orders")
    suspend fun getAllOrdersDirect(): List<OrderEntity>

    @Query("SELECT * FROM carpet_items")
    suspend fun getAllCarpetItemsDirect(): List<CarpetItemEntity>

    @Query("SELECT * FROM orders WHERE isSynced = 0")
    suspend fun getUnsyncedOrders(): List<OrderEntity>

    @Query("UPDATE orders SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markOrdersAsSynced(ids: List<String>)
}
