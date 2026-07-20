package com.speedwatch.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.speedwatch.app.data.model.SpeedLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedLogDao {
    @Query("SELECT * FROM speed_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SpeedLog>>

    @Insert
    suspend fun insertLog(log: SpeedLog)

    @Query("DELETE FROM speed_logs")
    suspend fun clearLogs()
}
