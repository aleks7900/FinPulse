package md.alexlab.finpulse.domain.usecase.csv

import md.alexlab.finpulse.domain.model.ImportProfile
import md.alexlab.finpulse.domain.repository.ImportProfileRepository
import kotlinx.coroutines.flow.Flow

class ManageImportProfilesUseCase(
    private val repository: ImportProfileRepository
) {
    fun getAllProfilesFlow(): Flow<List<ImportProfile>> = repository.getAllProfilesFlow()

    suspend fun getAllProfiles(): List<ImportProfile> = repository.getAllProfiles()

    suspend fun getProfileById(id: String): ImportProfile? = repository.getProfileById(id)

    suspend fun saveProfile(profile: ImportProfile) = repository.saveProfile(profile)

    suspend fun deleteProfile(id: String) = repository.deleteProfile(id)

    suspend fun seedDefaultProfilesIfNeeded() = repository.seedDefaultProfilesIfNeeded()
}
