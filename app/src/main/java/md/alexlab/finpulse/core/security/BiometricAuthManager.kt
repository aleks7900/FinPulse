package md.alexlab.finpulse.core.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    AVAILABLE,
    NONE_ENROLLED,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED
}

object BiometricAuthManager {

    fun checkAvailability(context: Context, allowDeviceCredential: Boolean = false): BiometricAvailability {
        val authenticators = if (allowDeviceCredential && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        }

        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SECURITY_UPDATE_REQUIRED
            else -> BiometricAvailability.UNSUPPORTED
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        negativeButtonText: String = "Cancel",
        allowDeviceCredential: Boolean = false,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)

        if (!subtitle.isNullOrBlank()) {
            promptInfoBuilder.setSubtitle(subtitle)
        }

        if (allowDeviceCredential && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else if (allowDeviceCredential && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            promptInfoBuilder.setDeviceCredentialAllowed(true)
        } else {
            promptInfoBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            promptInfoBuilder.setNegativeButtonText(negativeButtonText)
        }

        try {
            biometricPrompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            onError(-1, e.message ?: "Authentication error")
        }
    }
}
