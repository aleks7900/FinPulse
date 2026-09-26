package com.finpulse.app.domain.usecase.csv

import com.finpulse.app.domain.model.ImportProfile
import com.finpulse.app.domain.repository.ImportProfileRepository
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
