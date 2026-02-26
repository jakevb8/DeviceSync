package me.jakev.devicesync.data.model

/**
 * Firestore user profile document stored under users/{uid}
 */
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
