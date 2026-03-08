package com.dubois.aura.repository

import com.dubois.aura.model.UserProfile
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlin.time.Clock

class UserRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    suspend fun register(email: String, password: String, name: String): Result<UserProfile> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password)
            val uid = result.user!!.uid

            val profile = UserProfile(
                uid = uid,
                displayName = name,
                email = email,
                createdAt = Clock.System.now().toEpochMilliseconds(),
            )

            firestore.collection("users")
                .document(uid)
                .set(UserProfile.serializer(), profile)

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<UserProfile> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password)
            val uid = result.user!!.uid
            val profile = getProfile(uid)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(uid: String): UserProfile {
        val doc = firestore.collection("users").document(uid).get()
        return doc.data(UserProfile.serializer())
    }

    suspend fun updateProfile(uid: String, name: String, bio: String): Result<Unit> {
        return try {
            firestore.collection("users").document(uid).update(
                mapOf("displayName" to name, "bio" to bio)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() = auth.signOut()

    fun currentUser() = auth.currentUser
}