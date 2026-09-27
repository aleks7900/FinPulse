package com.finpulse.app.domain.usecase.backup

import com.finpulse.app.domain.model.backup.FinPulseFullBackup
import com.finpulse.app.domain.model.backup.RestoreSummary
import com.finpulse.app.domain.repository.BackupRepository

class RestoreBackupUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(backup: FinPulseFullBackup): RestoreSummary {
        return backupRepository.restoreFullBackup(backup)
    }
}
