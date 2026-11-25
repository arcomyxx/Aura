package com.dubois.aura.repository

import com.dubois.aura.model.AuraEvent
import com.dubois.aura.model.DailyLimits
import com.dubois.aura.model.EventType
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import kotlin.time.Clock

// Retourne "YYYY-MM-DD" UTC sans dépendance kotlinx-datetime.
private fun todayUtc(): String {
    val ms = Clock.System.now().toEpochMilliseconds()
    val days = (ms / 86_400_000L).toInt()
    var n = days + 2440588
    val a = (4 * n + 3) / 146097
    n -= 146097 * a / 4
    val b = (4 * n + 3) / 1461
    n -= 1461 * b / 4
    val m = (5 * n + 2) / 153
    val day = n - (153 * m + 2) / 5 + 1
    val month = m + (if (m < 10) 3 else -9)
    val year = 100 * a + b + (if (m < 10) 0 else 1)
    return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

/**
 * Signalements d'événements d'aura. L'écriture et les limites anti-abus
 * sont déléguées à la Cloud Function `reportEvent` (validation serveur).
 */
class EventRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private fun myUid(): String? = auth.currentUser?.uid

    suspend fun myDailyLimits(): DailyLimits {
        val uid = myUid() ?: return DailyLimits()
        return try {
            val doc = firestore.collection("users").document(uid)
                .collection("limits").document("daily").get()
            val limits = if (doc.exists) doc.data(DailyLimits.serializer()) else DailyLimits()
            if (limits.date != todayUtc()) DailyLimits(date = todayUtc()) else limits
        } catch (e: Exception) {
            DailyLimits()
        }
    }

    /** Signale un [type] d'événement sur l'utilisateur [targetUid] via Cloud Function. */
    suspend fun reportEvent(
        targetUid: String,
        type: EventType,
        description: String,
    ): Result<Unit> {
        return try {
            if (myUid() == null) return Result.failure(IllegalStateException("Non connecté"))
            Firebase.functions
                .httpsCallable("reportEvent")
                .invoke(
                    mapOf(
                        "targetUid" to targetUid,
                        "typeId" to type.id,
                        "emoji" to type.emoji,
                        "label" to type.label,
                        "delta" to type.delta,
                        "description" to description.trim(),
                        "rarity" to type.rarity.name,
                    )
                )
            Result.success(Unit)
        } catch (e: Exception) {
            // La CF renvoie des HttpsError avec un message localisé.
            Result.failure(e)
        }
    }

    /**
     * Modifie un signalement dont l'utilisateur courant est l'auteur (type et/ou
     * description). Si le type change, ajuste le score de la cible du différentiel
     * de delta de façon atomique.
     */
    suspend fun updateEvent(
        event: AuraEvent,
        newType: EventType,
        newDescription: String,
    ): Result<Unit> {
        return try {
            val myUid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            if (event.authorUid != myUid) {
                return Result.failure(IllegalStateException("Tu ne peux modifier que tes propres signalements"))
            }

            firestore.collection("users").document(event.targetUid)
                .collection("events").document(event.id)
                .update(
                    mapOf(
                        "typeId" to newType.id,
                        "emoji" to newType.emoji,
                        "label" to newType.label,
                        "delta" to newType.delta,
                        "description" to newDescription.trim(),
                    )
                )

            val deltaDiff = newType.delta - event.delta
            if (deltaDiff != 0) {
                firestore.collection("users").document(event.targetUid)
                    .update(mapOf("auraScore" to FieldValue.increment(deltaDiff)))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Supprime un signalement dont l'utilisateur courant est l'auteur et annule
     * son effet sur le score de la cible.
     */
    suspend fun deleteEvent(event: AuraEvent): Result<Unit> {
        return try {
            val myUid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            if (event.authorUid != myUid) {
                return Result.failure(IllegalStateException("Tu ne peux supprimer que tes propres signalements"))
            }

            firestore.collection("users").document(event.targetUid)
                .collection("events").document(event.id)
                .delete()

            firestore.collection("users").document(event.targetUid)
                .update(mapOf("auraScore" to FieldValue.increment(-event.delta)))

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
