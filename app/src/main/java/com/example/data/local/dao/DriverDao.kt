package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.DriverEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverDao {
    @Query("SELECT * FROM drivers WHERE id = :driverId LIMIT 1")
    fun getDriverFlow(driverId: String): Flow<DriverEntity?>

    @Query("SELECT * FROM drivers WHERE id = :driverId LIMIT 1")
    suspend fun getDriverDirect(driverId: String): DriverEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDriver(driver: DriverEntity)

    @Query("UPDATE drivers SET currentLat = :lat, currentLng = :lng, speed = :speed, batteryLevel = :battery, lastAppLogin = :timestamp WHERE id = :driverId")
    suspend fun updateTelemetry(driverId: String, lat: Double, lng: Double, speed: Float, battery: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE drivers SET totalCollectedCash = :cash, totalCollectedPos = :pos WHERE id = :driverId")
    suspend fun updateFinancials(driverId: String, cash: Long, pos: Long)
}
