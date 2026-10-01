package md.alexlab.finpulse.domain.usecase.backup

import md.alexlab.finpulse.domain.model.backup.FinPulseFullBackup
import md.alexlab.finpulse.domain.model.backup.RestoreSummary
import md.alexlab.finpulse.domain.repository.BackupRepository

class RestoreBackupUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(backup: FinPulseFullBackup): RestoreSummary {
        return backupRepository.restoreFullBackup(backup)
    }
}
