package com.speedwatch.app.data.repository

import com.speedwatch.app.data.local.IspSettingsDao
import com.speedwatch.app.data.local.LabAuditDao
import com.speedwatch.app.data.local.SpeedLogDao
import com.speedwatch.app.data.model.IspSettings
import com.speedwatch.app.data.model.LabAudit
import com.speedwatch.app.data.model.SpeedLog
import kotlinx.coroutines.flow.Flow

class SpeedRepository(
    private val speedLogDao: SpeedLogDao,
    private val ispSettingsDao: IspSettingsDao,
    private val labAuditDao: LabAuditDao
) {
    val allLogs: Flow<List<SpeedLog>> = speedLogDao.getAllLogs()
    val ispSettings: Flow<IspSettings?> = ispSettingsDao.getSettings()
    val allAudits: Flow<List<LabAudit>> = labAuditDao.getAllAudits()

    suspend fun insertLog(log: SpeedLog) {
        speedLogDao.insertLog(log)
    }

    suspend fun insertAudit(audit: LabAudit) {
        labAuditDao.insertAudit(audit)
    }

    suspend fun saveIspSettings(settings: IspSettings) {
        ispSettingsDao.upsertSettings(settings)
    }

    suspend fun setPremium(isPremium: Boolean) {
        val current = ispSettingsDao.getSettingsInternal() ?: return
        ispSettingsDao.upsertSettings(current.copy(isPremium = isPremium))
    }

    suspend fun setCheckFrequency(hours: Int) {
        val current = ispSettingsDao.getSettingsInternal() ?: return
        ispSettingsDao.upsertSettings(current.copy(checkFrequencyHours = hours))
    }

    suspend fun setAllowMobileBackground(allow: Boolean) {
        val current = ispSettingsDao.getSettingsInternal() ?: return
        ispSettingsDao.upsertSettings(current.copy(allowMobileBackgroundTests = allow))
    }

    suspend fun setDataUsageCap(capMB: Int) {
        val current = ispSettingsDao.getSettingsInternal() ?: return
        ispSettingsDao.upsertSettings(current.copy(dataUsageCapMB = capMB))
    }

    suspend fun setUsageAlerts(enabled: Boolean) {
        val current = ispSettingsDao.getSettingsInternal() ?: return
        ispSettingsDao.upsertSettings(current.copy(dataUsageAlertEnabled = enabled))
    }

    suspend fun setThemePreference(theme: String) {
        val current = ispSettingsDao.getSettingsInternal() ?: return
        ispSettingsDao.upsertSettings(current.copy(themePreference = theme))
    }

    suspend fun setSpeedDropAlertsEnabled(enabled: Boolean) {
        val current = ispSettingsDao.getSettingsInternal() ?: return
        ispSettingsDao.upsertSettings(current.copy(speedDropAlertsEnabled = enabled))
    }

    suspend fun setReportAlertsEnabled(enabled: Boolean) {
        val current = ispSettingsDao.getSettingsInternal() ?: return
        ispSettingsDao.upsertSettings(current.copy(reportAlertsEnabled = enabled))
    }

    suspend fun setStatusBarMonitorEnabled(enabled: Boolean) {
        val current = ispSettingsDao.getSettingsInternal() ?: return
        ispSettingsDao.upsertSettings(current.copy(statusBarMonitorEnabled = enabled))
    }

    suspend fun setShowDownloadSpeed(show: Boolean) {
        val current = ispSettingsDao.getSettingsInternal() ?: return
        ispSettingsDao.upsertSettings(current.copy(showDownloadSpeed = show))
    }

    suspend fun setShowUploadSpeed(show: Boolean) {
        val current = ispSettingsDao.getSettingsInternal() ?: return
        ispSettingsDao.upsertSettings(current.copy(showUploadSpeed = show))
    }

    suspend fun setShowPing(show: Boolean) {
        val current = ispSettingsDao.getSettingsInternal() ?: return
        ispSettingsDao.upsertSettings(current.copy(showPing = show))
    }

    suspend fun clearHistory() {
        speedLogDao.clearLogs()
    }

    suspend fun clearAudits() {
        labAuditDao.clearAudits()
    }
}
