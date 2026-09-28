package com.finpulse.app.presentation.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finpulse.app.R
import com.finpulse.app.core.auth.GoogleAuthManager
import com.finpulse.app.core.datastore.UserPreferencesDataStore
import com.finpulse.app.core.work.SyncWorker
import com.finpulse.app.domain.model.sync.CloudUser
import com.finpulse.app.domain.model.sync.SyncStatus
import com.finpulse.app.domain.repository.AuthRepository
import com.finpulse.app.domain.repository.CloudSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val isAuthenticated: Boolean = false,
    val currentUser: CloudUser? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val lastSyncTimestamp: Long = 0L,
    val pendingChangesCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val errorMessageRes: Int? = null,
    val successMessageRes: Int? = null,
    val successMessageArgs: List<String> = emptyList(),
    val showSignOutConfirmDialog: Boolean = false
)

class AccountViewModel(
    private val authRepository: AuthRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val googleAuthManager: GoogleAuthManager,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        // Collect current auth user
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        isAuthenticated = user != null,
                        currentUser = user
                    )
                }
            }
        }

        // Collect sync status
        viewModelScope.launch {
            cloudSyncRepository.syncStatus.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }

        // Collect last sync timestamp
        viewModelScope.launch {
            cloudSyncRepository.lastSyncTimestamp.collect { ts ->
                _uiState.update { it.copy(lastSyncTimestamp = ts) }
            }
        }

        // Collect pending changes count
        viewModelScope.launch {
            cloudSyncRepository.pendingChangesCount.collect { count ->
                _uiState.update { it.copy(pendingChangesCount = count) }
            }
        }
    }

    fun signInWithGoogle(activityContext: Context, webClientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val tokenResult = googleAuthManager.signInWithGoogle(activityContext, webClientId).getOrThrow()
                val user = authRepository.signInWithGoogleIdToken(tokenResult.idToken).getOrThrow()

                // Trigger initial migration / sync for newly signed-in user
                cloudSyncRepository.handleAccountSwitch(previousUid = null, newUid = user.uid)

                SyncWorker.schedulePeriodicSync(activityContext)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessageRes = R.string.account_signed_in_as,
                        successMessageArgs = listOf(user.displayName ?: user.email ?: user.uid)
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = t.localizedMessage,
                        errorMessageRes = if (t.localizedMessage == null) R.string.account_signin_failed else null
                    )
                }
            }
        }
    }

    fun performSync(context: Context? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, errorMessageRes = null) }
            val result = cloudSyncRepository.performFullSync()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessageRes = if (result.isSuccess) R.string.account_sync_success else null,
                    errorMessage = if (result.isFailure) result.exceptionOrNull()?.localizedMessage else null,
                    errorMessageRes = if (result.isFailure && result.exceptionOrNull()?.localizedMessage == null) R.string.account_sync_failed else null
                )
            }
            if (context != null) {
                SyncWorker.enqueueOneTimeSync(context)
            }
        }
    }

    fun requestSignOut() {
        if (_uiState.value.pendingChangesCount > 0) {
            _uiState.update { it.copy(showSignOutConfirmDialog = true) }
        } else {
            confirmSignOut(force = true)
        }
    }

    fun dismissSignOutDialog() {
        _uiState.update { it.copy(showSignOutConfirmDialog = false) }
    }

    fun confirmSignOut(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showSignOutConfirmDialog = false) }
            authRepository.signOut()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessageRes = R.string.account_signout_success
                )
            }
        }
    }

    fun syncNow(context: Context? = null) {
        performSync(context)
    }

    fun signOut() {
        requestSignOut()
    }

    fun confirmSignOutWithSync(context: Context? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showSignOutConfirmDialog = false) }
            cloudSyncRepository.performFullSync()
            authRepository.signOut()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessageRes = R.string.account_sync_signout_success
                )
            }
        }
    }

    fun dismissMessages() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = null,
                errorMessageRes = null,
                successMessageRes = null,
                successMessageArgs = emptyList()
            )
        }
    }

    fun clearMessages() {
        dismissMessages()
    }
}
