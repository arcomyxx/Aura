package com.dubois.aura.repository

import com.dubois.aura.model.UserProfile
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
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
        val profile = doc.data(UserProfile.serializer())
        // Injecte l'email depuis Firebase Auth pour l'utilisateur courant uniquement.
        return if (profile.uid == auth.currentUser?.uid) {
            profile.copy(email = auth.currentUser?.email ?: "")
        } else {
            profile.copy(email = "")
        }
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
     * Récupère les profils d'une liste d'UIDs (par lots de 30, limite Firestore whereIn).
     * Trie par score décroissant côté client.
     */
    suspend fun fetchProfiles(uids: List<String>): List<UserProfile> {
        if (uids.isEmpty()) return emptyList()
        return uids.chunked(30).flatMap { batch ->
            firestore.collection("users")
                .where { "uid" inArray batch }
                .get().documents
                .map { it.data(UserProfile.serializer()) }
                .filter { it.uid.isNotEmpty() }
        }.sortedWith(compareByDescending<UserProfile> { it.auraScore }.thenByDescending { it.isPremium })
    }

    /** Connexion via Google Sign-In ; crée le profil Firestore si c'est la première fois. */
    suspend fun loginWithGoogle(idToken: String): Result<UserProfile> {
        return try {
            val credential = GoogleAuthProvider.credential(idToken, null)
            val result = auth.signInWithCredential(credential)
            val user = result.user!!
            val uid = user.uid
            val docRef = firestore.collection("users").document(uid)
            val doc = docRef.get()
            if (!doc.exists) {
                val profile = UserProfile(
                    uid = uid,
                    displayName = user.displayName ?: "",
                    auraScore = 100,
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                )
                docRef.set(UserProfile.serializer(), profile)
                Result.success(profile)
            } else {
                Result.success(doc.data(UserProfile.serializer()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Supprime le compte de l'utilisateur courant : efface le document Firestore
     * puis supprime le compte Firebase Auth. La suppression des sous-collections
     * (events, friends, limits) doit être faite par une Cloud Function pour éviter
     * les limitations côté client, mais l'accès aux données est déjà bloqué dès
     * que le document principal est supprimé.
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(IllegalStateException("Non connecté"))
            val uid = user.uid
            // Supprime le profil public en premier (bloque l'accès immédiatement).
            firestore.collection("users").document(uid).delete()
            // Supprime le compte Auth (révoque tous les tokens).
            user.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() = auth.signOut()

    fun currentUser() = auth.currentUser
}