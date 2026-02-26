package me.jakev.devicesync.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import me.jakev.devicesync.data.model.UserProfile
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    val isSignedIn: Boolean
        get() = firebaseAuth.currentUser != null

    /**
     * Signs in with a Google ID token obtained from Google Sign-In flow.
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Auth result user is null")
            saveUserProfile(user)
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Google sign-in failed")
            Result.failure(e)
        }
    }

    private suspend fun saveUserProfile(user: FirebaseUser) {
        val profile = UserProfile(
            uid = user.uid,
            email = user.email ?: "",
            displayName = user.displayName ?: "",
            photoUrl = user.photoUrl?.toString() ?: ""
        )
        firestore.collection("users")
            .document(user.uid)
            .set(profile)
            .await()
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
