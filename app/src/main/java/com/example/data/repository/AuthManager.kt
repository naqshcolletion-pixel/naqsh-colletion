package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.local.StaffEntity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class ActiveSessionRole(
    val isOwner: Boolean = true,
    val staff: StaffEntity? = null
) {
    val actorName: String get() = if (isOwner) "NASIR ALI" else (staff?.name ?: "Staff")
    val actorRole: String get() = if (isOwner) "OWNER" else "STAFF"
    val canMakeSales: Boolean get() = isOwner || (staff?.canMakeSales == true)
    val canManageInventory: Boolean get() = isOwner || (staff?.canManageInventory == true)
    val canManagePurchases: Boolean get() = isOwner || (staff?.canManagePurchases == true)
    val canManageCustomers: Boolean get() = isOwner || (staff?.canManageCustomers == true)
    val canViewBasicReports: Boolean get() = isOwner || (staff?.canViewBasicReports == true)
    val canProcessReturns: Boolean get() = isOwner || (staff?.canProcessReturns == true)
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class AuthManager(
    private val auth: FirebaseAuth = Firebase.auth
) {
    fun currentUser(): FirebaseUser? = auth.currentUser

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        if (auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onUnauthenticated()
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        scope.launch {
            try {
                val cred = credentialManager.getCredential(context, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
        scope.launch {
            try {
                val activity = context as? Activity
                    ?: run {
                        onAuthError("Activity context required for Google Sign-In")
                        return@launch
                    }
                val cred = credentialManager.getCredential(activity, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun sendPhoneOtp(
        activity: Activity,
        phoneNumberE164: String,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null,
        onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onAutoVerified: (PhoneAuthCredential) -> Unit,
        onError: (String) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                onAutoVerified(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w("PhoneAuth", "Phone OTP verification failed: ${e.message}")
                onError(formatPhoneAuthError(e))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                onCodeSent(verificationId, token)
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumberE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken)
        }
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    suspend fun verifyOtpCode(verificationId: String, smsCode: String): Result<FirebaseUser?> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, smsCode.trim())
            val res = auth.signInWithCredential(credential).await()
            Result.success(res.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<FirebaseUser?> {
        return try {
            val res = auth.signInWithCredential(credential).await()
            Result.success(res.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOwnerPhoneWithCredential(verificationId: String, smsCode: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
            val credential = PhoneAuthProvider.getCredential(verificationId, smsCode.trim())
            user.updatePhoneNumber(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("Auth", "Failed to clear credential state: ${e.message}")
                } finally {
                    auth.signOut()
                    onSignOutComplete()
                }
            }
        }
    }

    private fun formatPhoneAuthError(e: FirebaseException): String {
        val raw = e.message.orEmpty()
        return when {
            raw.contains("Integrity API error", ignoreCase = true) ||
                raw.contains("reCAPTCHA", ignoreCase = true) ||
                raw.contains("siteKey", ignoreCase = true) ||
                raw.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
                raw.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
                raw.contains("not allowed", ignoreCase = true) -> {
                "SMS OTP requires Firebase Console -> Authentication -> Sign-in method -> 'Phone' enabled " +
                    "(with Play Integrity / reCAPTCHA or a Firebase Test Phone Number for emulators). " +
                    "You can use 'Sign in with Google' right above immediately."
            }
            raw.contains("TOO_MANY_REQUESTS", ignoreCase = true) ||
                raw.contains("QUOTA_EXCEEDED", ignoreCase = true) -> {
                "Too many OTP attempts. Please wait a bit before retrying or sign in with Google."
            }
            raw.contains("INVALID_PHONE_NUMBER", ignoreCase = true) -> {
                "Invalid phone number format. Include country code, e.g. +919471477887."
            }
            else -> e.localizedMessage ?: "OTP delivery failed. Ensure Phone Sign-In is enabled in Firebase Console."
        }
    }
}
