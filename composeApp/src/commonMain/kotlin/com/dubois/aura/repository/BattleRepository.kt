package com.dubois.aura.repository

import com.dubois.aura.model.Battle
import com.dubois.aura.model.BattleRules
import com.dubois.aura.model.BattleSupporter
import com.dubois.aura.model.UserProfile
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * Battles d'aura en temps réel (`battles/{id}` + sous-collection `supporters`).
 * Les supporters n'écrivent que des incréments ; seul l'hôte clôture sa battle
 * et encaisse l'aura gagnée.
 */
class BattleRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val battles get() = firestore.collection("battles")

    private fun myUid(): String? = auth.currentUser?.uid

    /** Lance une battle de [BattleRules.DURATION_MS] dont l'utilisateur courant est l'hôte. */
    suspend fun startBattle(): Result<Battle> {
        return try {
            val myUid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            val now = Clock.System.now().toEpochMilliseconds()
            val alreadyLive = myBattles(myUid).firstOrNull { it.isLive(now) }
            if (alreadyLive != null) return Result.success(alreadyLive)

            val me = firestore.collection("users").document(myUid)
                .get().data(UserProfile.serializer())
            val docRef = battles.document
            val battle = Battle(
                id = docRef.id,
                hostUid = myUid,
                hostName = me.displayName,
                hostThemeColor = me.themeColor,
                status = "live",
                startedAt = now,
                endsAt = now + BattleRules.DURATION_MS,
            )
            docRef.set(Battle.serializer(), battle)
            Result.success(battle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Flux temps réel d'une battle. */
    fun observeBattle(battleId: String): Flow<Battle> =
        battles.document(battleId).snapshots.map { it.data(Battle.serializer()) }

    /** Flux temps réel des supporters d'une battle, meilleurs contributeurs d'abord. */
    fun observeSupporters(battleId: String): Flow<List<BattleSupporter>> =
        battles.document(battleId).collection("supporters").snapshots.map { snap ->
            snap.documents
                .map { it.data(BattleSupporter.serializer()) }
                .sortedByDescending { it.points }
        }

    /** Flux temps réel de toutes les battles encore marquées « live » (filtrage amis côté client). */
    fun observeLiveBattles(): Flow<List<Battle>> =
        battles.where { "status" equalTo "live" }.snapshots.map { snap ->
            snap.documents.map { it.data(Battle.serializer()) }
        }

    /**
     * Envoie un lot de taps accumulés : incrémente le score de la battle et la
     * contribution du supporter dans une seule écriture atomique.
     */
    suspend fun sendTaps(
        battle: Battle,
        myName: String,
        points: Int,
        taps: Int,
        bestCombo: Int,
    ): Result<Unit> {
        return try {
            val myUid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            val battleRef = battles.document(battle.id)
            val supporterRef = battleRef.collection("supporters").document(myUid)

            val battleUpdate = buildList<Pair<String, Any?>> {
                add("score" to FieldValue.increment(points))
                add("taps" to FieldValue.increment(taps))
                add("supporterUids" to FieldValue.arrayUnion(myUid))
                if (bestCombo > battle.maxCombo) add("maxCombo" to bestCombo)
            }
            firestore.batch()
                .update(battleRef, *battleUpdate.toTypedArray())
                .set(
                    supporterRef,
                    mapOf(
                        "uid" to myUid,
                        "name" to myName,
                        "points" to FieldValue.increment(points),
                        "taps" to FieldValue.increment(taps),
                        "bestCombo" to bestCombo,
                    ),
                    merge = true,
                )
                .commit()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clôture une battle dont l'utilisateur courant est l'hôte et lui crédite
     * l'aura gagnée. Transactionnel : une battle n'est créditée qu'une fois,
     * même si plusieurs appareils tentent de la clôturer.
     * Renvoie l'aura créditée (0 si déjà clôturée).
     */
    suspend fun finishBattle(battleId: String): Result<Int> {
        return try {
            val myUid = myUid() ?: return Result.failure(IllegalStateException("Non connecté"))
            val battleRef = battles.document(battleId)
            val userRef = firestore.collection("users").document(myUid)
            val awarded = firestore.runTransaction {
                val battle = get(battleRef).data(Battle.serializer())
                if (battle.hostUid != myUid || battle.status != "live") return@runTransaction 0
                val aura = BattleRules.auraFor(battle.score)
                update(battleRef, "status" to "ended", "auraAwarded" to aura)
                if (aura > 0) update(userRef, "auraScore" to FieldValue.increment(aura))
                aura
            }
            Result.success(awarded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Battles dont [uid] est l'hôte. */
    suspend fun myBattles(uid: String): List<Battle> =
        battles.where { "hostUid" equalTo uid }
            .get().documents
            .map { it.data(Battle.serializer()) }

    /** Historique : battles hébergées ou soutenues par l'utilisateur courant, plus récentes d'abord. */
    suspend fun history(limit: Int = 30): List<Battle> {
        val myUid = myUid() ?: return emptyList()
        val supported = battles.where { "supporterUids" contains myUid }
            .get().documents
            .map { it.data(Battle.serializer()) }
        return (myBattles(myUid) + supported)
            .distinctBy { it.id }
            .sortedByDescending { it.startedAt }
            .take(limit)
    }
}
