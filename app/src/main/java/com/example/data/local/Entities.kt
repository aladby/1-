package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // "RESTAURANT" or "STORE"
    val category: String,
    val rating: Double,
    val deliveryTime: String,
    val deliveryFee: Double,
    val imageUrl: String,
    val isPopular: Boolean
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val category: String
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: String,
    val productName: String,
    val productPrice: Double,
    val quantity: Int,
    val merchantId: String,
    val merchantName: String
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val merchantName: String,
    val subtotal: Double,
    val deliveryFee: Double,
    val totalAmount: Double,
    val orderTime: Long,
    val status: String, // "PLACED", "PREPARING", "ON_THE_WAY", "DELIVERED"
    val paymentMethod: String,
    val deliveryAddress: String,
    val progressPercent: Float,
    val driverName: String,
    val driverPhone: String
)
