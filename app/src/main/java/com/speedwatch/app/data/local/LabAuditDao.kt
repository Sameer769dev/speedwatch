package com.speedwatch.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.speedwatch.app.data.model.LabAudit
import kotlinx.coroutines.flow.Flow

@Dao
interface LabAuditDao {
    @Query("SELECT * FROM lab_audits ORDER BY timestamp DESC")
    fun getAllAudits(): Flow<List<LabAudit>>

    @Insert
    suspend fun insertAudit(audit: LabAudit)

    @Query("DELETE FROM lab_audits")
    suspend fun clearAudits()
}
