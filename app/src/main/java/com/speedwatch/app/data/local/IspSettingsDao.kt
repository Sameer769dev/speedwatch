package com.speedwatch.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.speedwatch.app.data.model.IspSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface IspSettingsDao {
    @Query("SELECT * FROM isp_settings WHERE id = 0 LIMIT 1")
    fun getSettings(): Flow<IspSettings?>

    @Query("SELECT * FROM isp_settings WHERE id = 0 LIMIT 1")
    suspend fun getSettingsInternal(): IspSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: IspSettings)
}
