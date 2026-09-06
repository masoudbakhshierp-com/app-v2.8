package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.GpsLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GpsLogDao {
    @Query("SELECT * FROM gps_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentGpsLogs(): Flow<List<GpsLogEntity>>

    @Query("SELECT * FROM gps_logs")
    suspend fun getAllGpsLogsDirect(): List<GpsLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsLog(log: GpsLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsLogs(logs: List<GpsLogEntity>)

    @Query("SELECT * FROM gps_logs WHERE isSynced = 0")
    suspend fun getUnsyncedGpsLogs(): List<GpsLogEntity>

    @Query("UPDATE gps_logs SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markLogsAsSynced(ids: List<Long>)
}
