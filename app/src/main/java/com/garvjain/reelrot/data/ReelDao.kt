package com.garvjain.reelrot.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelDao {
    @Insert
    suspend fun insert(event: ReelEvent)

    @Query("SELECT COUNT(*) FROM reel_events WHERE platform = :platform")
    fun getCountByApp(platform: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM reel_events WHERE timestamp >= :startOfDay")
    fun getTodayCount(startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM reel_events WHERE platform = :platform AND timestamp >= :startOfDay")
    fun getTodayCountByApp(platform: String, startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM reel_events WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getCountInRange(startTime: Long, endTime: Long): Int
}
