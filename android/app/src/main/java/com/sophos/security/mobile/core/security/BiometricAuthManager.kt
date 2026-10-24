package com.sophos.security.mobile.core.security

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthManager {

    fun canAuthenticate(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Throwable) {
            false
        }
    }

    fun isBiometricEnrolled(): Boolean {
        return EncryptedStorageManager.hasBiometricCredentials()
    }

    fun enrollCredentials(userId: String, secret: String): Boolean {
        return EncryptedStorageManager.saveBiometricCredentials(userId, secret)
    }

    fun getEnrolledCredentials(): Pair<String, String>? {
        return EncryptedStorageManager.getBiometricCredentials()
    }

    fun disableBiometric() {
        EncryptedStorageManager.clearBiometricCredentials()
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Fingerprint Authentication",
        subtitle: String = "Touch the fingerprint sensor to authenticate",
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .build()

            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to open biometric sensor")
        }
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
