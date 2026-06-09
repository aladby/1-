package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MerchantEntity::class,
        ProductEntity::class,
        CartItemEntity::class,
        OrderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DeliveryDatabase : RoomDatabase() {
    abstract fun deliveryDao(): DeliveryDao

    companion object {
        @Volatile
        private var INSTANCE: DeliveryDatabase? = null

        fun getDatabase(context: Context): DeliveryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DeliveryDatabase::class.java,
                    "delivery_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
