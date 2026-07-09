package com.dubois.aura.repository

import com.dubois.aura.model.AuraEvent
import com.dubois.aura.model.EventType
import com.dubois.aura.model.UserProfile
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.firestore
import kotlin.time.Clock

/**
 * Signalements d'événements d'aura. Un événement est écrit dans la
 * sous-collection `events` de la cible et incrémente son `auraScore`.
 */
class EventRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private fun myUid(): String? = auth.currentUser?.uid

    /** Signale un [type] d'événement sur l'utilisateur [targetUid]. */
    suspend fun reportEvent(
        targetUid: String,
        type: EventType,
        description: String,
    ): Result<Unit> {
        return try {
            val myUid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            if (targetUid == myUid) {
                return Result.failure(IllegalStateException("Impossible de se signaler soi-même"))
            }

            val me = firestore.collection("users").document(myUid)
                .get().data(UserProfile.serializer())

            val now = Clock.System.now().toEpochMilliseconds()
            val docRef = firestore.collection("users").document(targetUid)
                .collection("events").document

            val event = AuraEvent(
                id = docRef.id,
                targetUid = targetUid,
                authorUid = myUid,
                authorName = me.displayName,
                typeId = type.id,
                emoji = type.emoji,
                label = type.label,
                delta = type.delta,
                description = description.trim(),
                createdAt = now,
            )
            docRef.set(AuraEvent.serializer(), event)

            // Applique le delta au score de la cible de façon atomique.
            firestore.collection("users").document(targetUid)
                .update(mapOf("auraScore" to FieldValue.increment(type.delta)))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Événements récents reçus par [uid] (les plus récents d'abord). */
    suspend fun recentEvents(uid: String, limit: Int = 30): List<AuraEvent> {
        return firestore.collection("users").document(uid)
            .collection("events")
            .get().documents
            .map { it.data(AuraEvent.serializer()) }
            .sortedByDescending { it.createdAt }
            .take(limit)
    }
}
