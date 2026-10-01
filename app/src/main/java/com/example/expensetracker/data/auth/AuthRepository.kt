package com.paolorossi.expensetracker.data.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
)

/**
 * Google Sign-In via Android Credential Manager + Firebase Auth (Design/01 §2,
 * Design/03 "Auth flow detail"). No email/password or email-link.
 */
interface AuthRepository {
    val currentUser: AuthUser?

    fun observeAuthState(): Flow<AuthUser?>

    suspend fun signInWithGoogle(activity: Activity): Result<AuthUser>

    fun signOut()
}

class FirebaseGoogleAuthRepository(
    private val auth: FirebaseAuth,
    private val webClientId: String,
) : AuthRepository {
    override val currentUser: AuthUser? get() = auth.currentUser?.toAuthUser()

    override fun observeAuthState(): Flow<AuthUser?> =
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }

    override suspend fun signInWithGoogle(activity: Activity): Result<AuthUser> =
        runCatching {
            check(webClientId.isNotBlank()) {
                "WEB_CLIENT_ID is not configured — see 00-prerequisites.md §3.3"
            }
            val credentialManager = CredentialManager.create(activity)
            val googleIdOption =
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            val request =
                GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential
            check(
                credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            ) { "Unexpected credential type: ${credential.type}" }

            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(firebaseCredential).await()
            result.user?.toAuthUser() ?: error("Sign-in succeeded but no user was returned")
        }

    override fun signOut() = auth.signOut()
}

private fun FirebaseUser.toAuthUser(): AuthUser =
    AuthUser(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName ?: email ?: "Unknown",
    )
