package md.alexlab.finpulse.domain.repository

import md.alexlab.finpulse.domain.model.Transaction
import md.alexlab.finpulse.domain.model.backup.ExportFilterParams
import md.alexlab.finpulse.domain.model.backup.FinPulseFullBackup
import md.alexlab.finpulse.domain.model.backup.RestoreSummary

interface BackupRepository {
    suspend fun createFullBackup(): FinPulseFullBackup
    suspend fun restoreFullBackup(backup: FinPulseFullBackup): RestoreSummary
    suspend fun getFilteredTransactions(params: ExportFilterParams): List<Transaction>
}
