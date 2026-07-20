package com.speedwatch.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.speedwatch.app.data.model.IspSettings
import com.speedwatch.app.data.model.LabAudit
import com.speedwatch.app.data.model.SpeedLog

@Database(entities = [SpeedLog::class, IspSettings::class, LabAudit::class], version = 8, exportSchema = false)
abstract class SpeedWatchDatabase : RoomDatabase() {
    abstract fun speedLogDao(): SpeedLogDao
    abstract fun ispSettingsDao(): IspSettingsDao
    abstract fun labAuditDao(): LabAuditDao

    companion object {
        @Volatile
        private var INSTANCE: SpeedWatchDatabase? = null

        fun getDatabase(context: Context): SpeedWatchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpeedWatchDatabase::class.java,
                    "speedwatch_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
