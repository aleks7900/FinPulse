package com.finpulse.app.domain.repository

import com.finpulse.app.domain.model.Transaction
import com.finpulse.app.domain.model.backup.ExportFilterParams
import com.finpulse.app.domain.model.backup.FinPulseFullBackup
import com.finpulse.app.domain.model.backup.RestoreSummary

interface BackupRepository {
    suspend fun createFullBackup(): FinPulseFullBackup
    suspend fun restoreFullBackup(backup: FinPulseFullBackup): RestoreSummary
    suspend fun getFilteredTransactions(params: ExportFilterParams): List<Transaction>
}
