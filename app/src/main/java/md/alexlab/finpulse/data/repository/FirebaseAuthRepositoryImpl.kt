package md.alexlab.finpulse.data.repository

import md.alexlab.finpulse.core.datastore.UserPreferencesDataStore
import md.alexlab.finpulse.data.cloud.awaitTask
import md.alexlab.finpulse.domain.model.sync.CloudUser
import md.alexlab.finpulse.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FirebaseAuthRepositoryImpl(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val firebaseAuthProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val errorReporter: md.alexlab.finpulse.core.reporting.ErrorReporter = md.alexlab.finpulse.core.reporting.NoOpErrorReporter(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : AuthRepository {

    private val _currentUser = MutableStateFlow<CloudUser?>(null)
    override val currentUser: StateFlow<CloudUser?> = _currentUser.asStateFlow()

    init {
        scope.launch {
            try {
                val auth = runCatching { firebaseAuthProvider() }.getOrNull()
                val firebaseUser = auth?.currentUser
                if (firebaseUser != null) {
                    val user = CloudUser(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        isAnonymous = firebaseUser.isAnonymous
                    )
                    _currentUser.value = user
                    errorReporter.setUserContext(user.uid)
                    userPreferencesDataStore.setUserSession(
                        uid = user.uid,
                        email = user.email,
                        displayName = user.displayName,
                        photoUrl = user.photoUrl
                    )
                } else {
                    val prefs = userPreferencesDataStore.userPreferencesFlow.first()
                    if (!prefs.currentUserId.isNullOrBlank()) {
                        _currentUser.value = CloudUser(
                            uid = prefs.currentUserId,
                            email = prefs.userEmail,
                            displayName = prefs.userDisplayName,
                            photoUrl = prefs.userPhotoUrl
                        )
                    }
                }
            } catch (_: Throwable) {
                // Keep local state if Firebase is offline
            }
        }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<CloudUser> = runCatching {
        try {
            val auth = firebaseAuthProvider()
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).awaitTask()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user was null after sign in")

            val cloudUser = CloudUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                photoUrl = firebaseUser.photoUrl?.toString(),
                isAnonymous = firebaseUser.isAnonymous
            )

            userPreferencesDataStore.setUserSession(
                uid = cloudUser.uid,
                email = cloudUser.email,
                displayName = cloudUser.displayName,
                photoUrl = cloudUser.photoUrl
            )

            errorReporter.setUserContext(cloudUser.uid)
            errorReporter.log("User successfully signed in with Google")

            _currentUser.value = cloudUser
            cloudUser
        } catch (t: Throwable) {
            errorReporter.recordException(
                throwable = t,
                context = md.alexlab.finpulse.core.reporting.DiagnosticContext.build(
                    feature = md.alexlab.finpulse.core.reporting.DiagnosticContext.FEATURE_AUTH,
                    operation = md.alexlab.finpulse.core.reporting.DiagnosticContext.OP_SIGN_IN_GOOGLE
                )
            )
            throw t
        }
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        runCatching { firebaseAuthProvider().signOut() }
        userPreferencesDataStore.clearUserSession()
        errorReporter.clearUserContext()
        errorReporter.log("User signed out")
        _currentUser.value = null
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        try {
            val auth = firebaseAuthProvider()
            auth.currentUser?.delete()?.awaitTask()
            userPreferencesDataStore.clearUserSession()
            errorReporter.clearUserContext()
            errorReporter.log("User deleted account")
            _currentUser.value = null
        } catch (t: Throwable) {
            errorReporter.recordException(
                throwable = t,
                context = md.alexlab.finpulse.core.reporting.DiagnosticContext.build(
                    feature = md.alexlab.finpulse.core.reporting.DiagnosticContext.FEATURE_AUTH,
                    operation = "delete_account"
                )
            )
            throw t
        }
    }
}
