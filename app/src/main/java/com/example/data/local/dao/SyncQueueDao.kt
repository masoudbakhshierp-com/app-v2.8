package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY timestamp ASC")
    fun getPendingQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingItemsList(): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM sync_queue ORDER BY timestamp DESC")
    fun getAllQueueHistory(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue")
    suspend fun getAllSyncQueueDirect(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueueItem(item: SyncQueueEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueueItems(items: List<SyncQueueEntity>)

    @Update
    suspend fun updateSyncQueueItem(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = 'SYNCED' WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("UPDATE sync_queue SET status = 'SYNCED' WHERE status = 'PENDING'")
    suspend fun markAllAsSynced()

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun clearSyncedQueue()
}
