package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.DriverDao
import com.example.data.local.dao.DriverSettlementDao
import com.example.data.local.dao.GpsLogDao
import com.example.data.local.dao.OrderDao
import com.example.data.local.dao.SyncQueueDao
import com.example.data.local.entities.CarpetItemEntity
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.DriverSettlementEntity
import com.example.data.local.entities.GpsLogEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.SyncQueueEntity

@Database(
    entities = [
        DriverEntity::class,
        OrderEntity::class,
        CarpetItemEntity::class,
        ChatMessageEntity::class,
        GpsLogEntity::class,
        SyncQueueEntity::class,
        DriverSettlementEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class ZomorrodDatabase : RoomDatabase() {
    abstract fun driverDao(): DriverDao
    abstract fun orderDao(): OrderDao
    abstract fun driverSettlementDao(): DriverSettlementDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun gpsLogDao(): GpsLogDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: ZomorrodDatabase? = null

        fun getDatabase(context: Context): ZomorrodDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZomorrodDatabase::class.java,
                    "zomorrod_driver_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
