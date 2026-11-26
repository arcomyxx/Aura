package com.dubois.aura.repository

import com.dubois.aura.model.Friend
import com.dubois.aura.model.FriendRequest
import com.dubois.aura.model.UserProfile
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import kotlin.time.Clock

class FriendRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private fun myUid(): String? = auth.currentUser?.uid

    /**
     * Recherche des utilisateurs par nom d'affichage OU adresse email (correspondance exacte).
     * On lance deux requêtes mono-champ (pas d'index composite requis) puis on fusionne.
     * On exclut son propre compte.
     */
    suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        return try {
            val me = myUid()
            val q = query.trim()
            if (q.isEmpty()) return Result.success(emptyList())

            val users = firestore.collection("users")
            val byName = users.where { "displayName" equalTo q }.get().documents

            val merged = byName
                .map { it.data(UserProfile.serializer()) }
                .filter { it.uid.isNotEmpty() && it.uid != me }
                .distinctBy { it.uid }

            Result.success(merged)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Crée une demande d'ami en attente vers [target]. */
    suspend fun sendFriendRequest(target: UserProfile): Result<Unit> {
        return try {
            val myUid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            if (target.uid == myUid) {
                return Result.failure(IllegalStateException("Impossible de s'ajouter soi-même"))
            }

            val me = firestore.collection("users").document(myUid)
                .get().data(UserProfile.serializer())

            val docRef = firestore.collection("friendRequests").document
            val request = FriendRequest(
                id = docRef.id,
                fromUid = myUid,
                fromName = me.displayName,
                toUid = target.uid,
                toName = target.displayName,
                status = "pending",
                createdAt = Clock.System.now().toEpochMilliseconds(),
            )
            docRef.set(FriendRequest.serializer(), request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Demandes reçues et encore en attente. Le filtre `status` est fait côté client. */
    suspend fun incomingRequests(): List<FriendRequest> {
        val myUid = myUid() ?: return emptyList()
        return firestore.collection("friendRequests")
            .where { "toUid" equalTo myUid }
            .get().documents
            .map { it.data(FriendRequest.serializer()) }
            .filter { it.status == "pending" }
            .sortedByDescending { it.createdAt }
    }

    /**
     * Accepte une demande : marque la demande comme acceptée puis inscrit chaque
     * utilisateur dans la liste d'amis de l'autre.
     */
    suspend fun acceptRequest(request: FriendRequest): Result<Unit> {
        return try {
            val myUid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            val now = Clock.System.now().toEpochMilliseconds()

            firestore.collection("friendRequests").document(request.id)
                .update(mapOf("status" to "accepted"))

            // L'expéditeur devient mon ami.
            firestore.collection("users").document(myUid)
                .collection("friends").document(request.fromUid)
                .set(
                    Friend.serializer(),
                    Friend(request.fromUid, request.fromName, since = now)
                )

            // Je deviens l'ami de l'expéditeur.
            firestore.collection("users").document(request.fromUid)
                .collection("friends").document(myUid)
                .set(
                    Friend.serializer(),
                    Friend(request.toUid, request.toName, since = now)
                )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Refuse une demande d'ami. */
    suspend fun declineRequest(request: FriendRequest): Result<Unit> {
        return try {
            firestore.collection("friendRequests").document(request.id)
                .update(mapOf("status" to "declined"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Liste des amis confirmés de l'utilisateur courant. */
    suspend fun friends(): List<Friend> {
        val myUid = myUid() ?: return emptyList()
        return firestore.collection("users").document(myUid)
            .collection("friends")
            .get().documents
            .map { it.data(Friend.serializer()) }
            .sortedBy { it.displayName }
    }
}
