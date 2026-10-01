package md.alexlab.finpulse.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class GoogleAuthTokenResult(
    val idToken: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null
)

class GoogleAuthManager(
    private val context: Context,
    private val credentialManager: CredentialManager = CredentialManager.create(context)
) {

    suspend fun signInWithGoogle(
        activityContext: Context,
        webClientId: String
    ): Result<GoogleAuthTokenResult> {
        if (webClientId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Google Web Client ID is blank. Configure your OAuth 2.0 Web Client ID.")
            )
        }

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(
                    GoogleAuthTokenResult(
                        idToken = googleIdTokenCredential.idToken,
                        email = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName,
                        photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                )
            } else {
                Result.failure(IllegalStateException("Unexpected credential type returned: ${credential::class.java.name}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(OperationCanceledException("Google Sign-in was cancelled by the user"))
        } catch (e: NoCredentialException) {
            Result.failure(IllegalStateException("No Google credentials available on this device", e))
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

class OperationCanceledException(message: String) : Exception(message)
