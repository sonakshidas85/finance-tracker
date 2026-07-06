package com.budgettracker.app.ui

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Shared app-lock helper, factored out of [com.budgettracker.app.MainActivity] so that
 * `QuickAddSpendActivity` can require the SAME BiometricPrompt check before letting the user see
 * or edit anything - otherwise the widget's quick-add popup would be a bypass around the app lock
 * the user turned on. Both call sites just call [showBiometricGate] on themselves; the
 * prompt-building code (title, allowed authenticators) lives in exactly one place.
 *
 * `FragmentActivity` (not plain `ComponentActivity`) is required because `BiometricPrompt`'s
 * constructor needs a `FragmentActivity` to host its confirmation UI - `ComponentActivity` is a
 * `FragmentActivity` subclass already for any Activity using `androidx.activity`/AndroidX Compose,
 * so this is a no-op type constraint for both existing Activities.
 */
fun FragmentActivity.showBiometricGate(onSuccess: () -> Unit, onFailure: () -> Unit = {}) {
    val executor = ContextCompat.getMainExecutor(this)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }

        // onAuthenticationFailed (wrong credential, retryable) and onAuthenticationError
        // (cancelled, locked out, etc.) both route to onFailure - MainActivity keeps its
        // LockGateScreen placeholder up either way; QuickAddSpendActivity just finish()es so the
        // small popup doesn't get stuck on screen.
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            onFailure()
        }
    }
    val prompt = BiometricPrompt(this, executor, callback)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock gremlin")
        .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        .build()
    prompt.authenticate(promptInfo)
}
