package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.DriverSettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverSettlementDao {
    @Query("SELECT * FROM driver_settlements ORDER BY createdAt DESC")
    fun getAllSettlementsFlow(): Flow<List<DriverSettlementEntity>>

    @Query("SELECT * FROM driver_settlements ORDER BY createdAt DESC")
    suspend fun getAllSettlementsDirect(): List<DriverSettlementEntity>

    @Query("SELECT * FROM driver_settlements WHERE isSynced = 0")
    suspend fun getUnsyncedSettlements(): List<DriverSettlementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: DriverSettlementEntity)

    @Update
    suspend fun updateSettlement(settlement: DriverSettlementEntity)

    @Query("UPDATE driver_settlements SET isSynced = 1, status = 'approved' WHERE id = :id")
    suspend fun markAsApprovedAndSynced(id: String)
}
