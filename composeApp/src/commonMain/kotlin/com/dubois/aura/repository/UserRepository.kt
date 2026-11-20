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
                auraScore = 100,
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
        val docRef = firestore.collection("users").document(uid)
        val doc = docRef.get()
        if (!doc.exists) {
            // Compte Auth existant sans profil Firestore (ex: echec d'ecriture lors de l'inscription).
            // On recree un profil par defaut pour ne pas bloquer l'utilisateur.
            val fallback = UserProfile(
                uid = uid,
                email = auth.currentUser?.email ?: "",
                createdAt = Clock.System.now().toEpochMilliseconds(),
            )
            docRef.set(UserProfile.serializer(), fallback)
            return fallback
        }
        return doc.data(UserProfile.serializer())
    }

    suspend fun updateProfile(
        uid: String,
        name: String,
        bio: String,
        photoUrl: String,
        themeColor: String,
    ): Result<Unit> {
        return try {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "displayName" to name,
                    "bio" to bio,
                    "photoUrl" to photoUrl,
                    "themeColor" to themeColor,
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tous les utilisateurs, triés par score d'aura décroissant.
     * Sert au classement global et aux scores affichés dans l'onglet Amis.
     */
    suspend fun allUsersRanked(): List<UserProfile> {
        return firestore.collection("users")
            .get().documents
            .map { it.data(UserProfile.serializer()) }
            .filter { it.uid.isNotEmpty() }
            .sortedByDescending { it.auraScore }
    }

    suspend fun logout() = auth.signOut()

    fun currentUser() = auth.currentUser
}