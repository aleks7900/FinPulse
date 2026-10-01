package md.alexlab.finpulse.domain.repository

import md.alexlab.finpulse.domain.model.sync.CloudUser
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<CloudUser?>
    val isAuthenticated: Boolean
        get() = currentUser.value != null

    suspend fun signInWithGoogleIdToken(idToken: String): Result<CloudUser>
    suspend fun signOut(): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}
