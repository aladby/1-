package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {
    // Merchants
    @Query("SELECT * FROM merchants")
    fun getAllMerchants(): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE type = :type")
    fun getMerchantsByType(type: String): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE id = :id")
    suspend fun getMerchantByIdSync(id: String): MerchantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchants(merchants: List<MerchantEntity>)

    // Products
    @Query("SELECT * FROM products WHERE merchantId = :merchantId")
    fun getProductsForMerchant(merchantId: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    // Cart Items
    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteCartItem(productId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // Orders
    @Query("SELECT * FROM orders ORDER BY orderTime DESC")
    fun getOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status != 'DELIVERED' ORDER BY orderTime DESC LIMIT 1")
    fun getActiveOrder(): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getOrderById(id: String): Flow<OrderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status, progressPercent = :progress WHERE id = :id")
    suspend fun updateOrderStatus(id: String, status: String, progress: Float)
}
