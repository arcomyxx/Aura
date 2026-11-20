package com.dubois.aura.repository

import com.dubois.aura.model.AuraEvent
import com.dubois.aura.model.DailyLimits
import com.dubois.aura.model.EventRarity
import com.dubois.aura.model.EventType
import com.dubois.aura.model.RarityRecord
import com.dubois.aura.model.UserProfile
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.firestore
import kotlin.time.Clock

private const val MAX_POSITIVES_PER_DAY = 3
private const val MAX_NEGATIVES_PER_DAY = 1
private const val MAX_REWARD_PER_DAY = 10
private const val MAX_LOSS_PER_DAY = 50
private const val REPORTER_REWARD = 2

// Fenêtre de cooldown par rareté en millisecondes
private val RARITY_COOLDOWN_MS = mapOf(
    EventRarity.COMMON    to  12 * 3_600_000L,  // 2×/jour ≈ toutes les 12 h
    EventRarity.RARE      to  24 * 3_600_000L,  // 1×/jour
    EventRarity.LEGENDARY to 168 * 3_600_000L,  // 1×/semaine
)

/**
 * Signalements d'événements d'aura. Un événement est écrit dans la
 * sous-collection `events` de la cible et incrémente son `auraScore`.
 * Applique les limites anti-abus côté client, stockées dans Firestore.
 */
class EventRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private fun myUid(): String? = auth.currentUser?.uid

    // Retourne "YYYY-MM-DD" UTC à partir du timestamp courant, sans dépendance kotlinx-datetime.
    private fun today(): String {
        val ms = Clock.System.now().toEpochMilliseconds()
        val days = (ms / 86_400_000L).toInt()
        // Algorithme civil de Richards pour convertir un jour julien en date grégorienne
        var n = days + 2440588          // epoch Unix -> jour julien
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

    suspend fun myDailyLimits(): DailyLimits {
        val uid = myUid() ?: return DailyLimits()
        val limits = loadDailyLimits(uid)
        val todayStr = today()
        return if (limits.date != todayStr) DailyLimits(date = todayStr) else limits
    }

    private suspend fun loadDailyLimits(uid: String): DailyLimits {
        return try {
            val doc = firestore.collection("users").document(uid)
                .collection("limits").document("daily").get()
            if (doc.exists) doc.data(DailyLimits.serializer()) else DailyLimits()
        } catch (e: Exception) {
            DailyLimits()
        }
    }

    private suspend fun saveDailyLimits(uid: String, limits: DailyLimits) {
        firestore.collection("users").document(uid)
            .collection("limits").document("daily")
            .set(DailyLimits.serializer(), limits)
    }

    private suspend fun loadRarityRecord(uid: String, targetUid: String, typeId: String): RarityRecord {
        return try {
            val doc = firestore.collection("users").document(uid)
                .collection("limits").document("rarity_${targetUid}_$typeId").get()
            if (doc.exists) doc.data(RarityRecord.serializer()) else RarityRecord()
        } catch (e: Exception) {
            RarityRecord()
        }
    }

    private suspend fun saveRarityRecord(uid: String, targetUid: String, typeId: String, record: RarityRecord) {
        firestore.collection("users").document(uid)
            .collection("limits").document("rarity_${targetUid}_$typeId")
            .set(RarityRecord.serializer(), record)
    }

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

            val now = Clock.System.now().toEpochMilliseconds()
            val todayStr = today()

            // --- Limites quotidiennes globales ---
            var myLimits = loadDailyLimits(myUid)
            if (myLimits.date != todayStr) myLimits = DailyLimits(date = todayStr)

            if (type.isPositive && myLimits.positivesSent >= MAX_POSITIVES_PER_DAY) {
                return Result.failure(IllegalStateException("Tu as atteint ta limite de $MAX_POSITIVES_PER_DAY signalements positifs aujourd'hui"))
            }
            if (!type.isPositive && myLimits.negativesSent >= MAX_NEGATIVES_PER_DAY) {
                return Result.failure(IllegalStateException("Tu as atteint ta limite de $MAX_NEGATIVES_PER_DAY signalement négatif aujourd'hui"))
            }

            // --- Limite par rareté (donneur→cible) ---
            val cooldown = RARITY_COOLDOWN_MS[type.rarity] ?: 0L
            val rarityRecord = loadRarityRecord(myUid, targetUid, type.id)
            if (now - rarityRecord.lastMs < cooldown) {
                val remaining = ((cooldown - (now - rarityRecord.lastMs)) / 3_600_000L) + 1
                val msg = when (type.rarity) {
                    EventRarity.LEGENDARY -> "Cet événement légendaire ne peut être signalé qu'une fois par semaine à la même personne"
                    EventRarity.RARE -> "Cet événement rare ne peut être signalé qu'une fois par jour à la même personne"
                    EventRarity.COMMON -> "Tu as déjà signalé cet événement récemment (attends encore ~${remaining}h)"
                }
                return Result.failure(IllegalStateException(msg))
            }

            // --- Plafond de perte journalière pour la cible ---
            var targetLimits = loadDailyLimits(targetUid)
            if (targetLimits.date != todayStr) targetLimits = DailyLimits(date = todayStr)

            val effectiveDelta = if (!type.isPositive) {
                val remainingLoss = MAX_LOSS_PER_DAY - targetLimits.receivedLoss
                if (remainingLoss <= 0) 0 else maxOf(type.delta, -remainingLoss)
            } else {
                type.delta
            }

            val me = firestore.collection("users").document(myUid)
                .get().data(UserProfile.serializer())

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
                delta = effectiveDelta,
                description = description.trim(),
                createdAt = now,
            )
            docRef.set(AuraEvent.serializer(), event)

            // Applique le delta effectif au score de la cible.
            if (effectiveDelta != 0) {
                firestore.collection("users").document(targetUid)
                    .update(mapOf("auraScore" to FieldValue.increment(effectiveDelta)))
            }

            // Récompense le signaleur pour un signalement positif.
            val reward = if (type.isPositive) {
                val availableReward = MAX_REWARD_PER_DAY - myLimits.rewardEarned
                minOf(REPORTER_REWARD, availableReward).coerceAtLeast(0)
            } else 0

            if (reward > 0) {
                firestore.collection("users").document(myUid)
                    .update(mapOf("auraScore" to FieldValue.increment(reward)))
            }

            // Mise à jour des compteurs.
            saveDailyLimits(myUid, myLimits.copy(
                positivesSent = if (type.isPositive) myLimits.positivesSent + 1 else myLimits.positivesSent,
                negativesSent = if (!type.isPositive) myLimits.negativesSent + 1 else myLimits.negativesSent,
                rewardEarned = myLimits.rewardEarned + reward,
            ))
            if (!type.isPositive && effectiveDelta != 0) {
                saveDailyLimits(targetUid, targetLimits.copy(
                    receivedLoss = targetLimits.receivedLoss + (-effectiveDelta),
                ))
            }
            saveRarityRecord(myUid, targetUid, type.id, RarityRecord(lastMs = now))

            Result.success(Unit)
        } catch (e: Exception) {
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
