package com.dubois.aura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dubois.aura.model.Battle
import com.dubois.aura.model.BattleAction
import com.dubois.aura.model.BattleRules
import com.dubois.aura.model.BattleSupporter
import com.dubois.aura.model.ComboTracker
import com.dubois.aura.repository.BattleRepository
import com.dubois.aura.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * ViewModel des battles d'aura : battles en cours des amis, historique, et
 * battle ouverte (score temps réel, taps, combo, jauge de hype).
 *
 * Les taps sont accumulés localement puis envoyés par lots toutes les
 * [FLUSH_INTERVAL_MS] pour limiter les écritures Firestore.
 */
class BattleViewModel : ViewModel() {

    private val battleRepo = BattleRepository()
    private val userRepo = UserRepository()

    val myUid: String? get() = userRepo.currentUser()?.uid

    /** Battles marquées « live » (toutes ; filtrage amis/expiration côté appelant). */
    private val _liveBattles = MutableStateFlow<List<Battle>>(emptyList())
    val liveBattles: StateFlow<List<Battle>> = _liveBattles

    private val _history = MutableStateFlow<List<Battle>>(emptyList())
    val history: StateFlow<List<Battle>> = _history

    /** Battle ouverte à l'écran (null = aucune). */
    private val _current = MutableStateFlow<Battle?>(null)
    val current: StateFlow<Battle?> = _current

    private val _supporters = MutableStateFlow<List<BattleSupporter>>(emptyList())
    val supporters: StateFlow<List<BattleSupporter>> = _supporters

    /** Points tapés localement pas encore envoyés (affichés en optimiste). */
    private val _pendingPoints = MutableStateFlow(0)
    val pendingPoints: StateFlow<Int> = _pendingPoints

    private val _combo = MutableStateFlow(0)
    val combo: StateFlow<Int> = _combo

    /** Meilleur combo de l'utilisateur sur la battle ouverte. */
    private val _myBestCombo = MutableStateFlow(0)
    val myBestCombo: StateFlow<Int> = _myBestCombo

    /** Instant à partir duquel chaque bouton à cooldown est de nouveau dispo. */
    private val _readyAt = MutableStateFlow<Map<BattleAction, Long>>(emptyMap())
    val readyAt: StateFlow<Map<BattleAction, Long>> = _readyAt

    /** Horloge rafraîchie pendant qu'une battle est ouverte (chrono, cooldowns). */
    private val _now = MutableStateFlow(nowMs())
    val now: StateFlow<Long> = _now

    /** Taps par seconde, tous supporters confondus, sur les dernières secondes. */
    private val _hype = MutableStateFlow(0f)
    val hype: StateFlow<Float> = _hype

    private val _launching = MutableStateFlow(false)
    val launching: StateFlow<Boolean> = _launching

    /** Aura créditée à l'utilisateur à la clôture d'une de ses battles. */
    private val _auraEarned = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val auraEarned: SharedFlow<Int> = _auraEarned

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private var liveJob: Job? = null
    private var sessionJobs: List<Job> = emptyList()
    private var comboResetJob: Job? = null
    private val finishJobs = mutableMapOf<String, Job>()

    private var tracker = ComboTracker()
    private var pendingTaps = 0
    private var myName = ""
    private val tapSamples = ArrayDeque<Pair<Long, Int>>()

    /** Démarre l'écoute des battles live, charge l'historique et clôture mes battles expirées. */
    fun load() {
        // (Re)démarre l'écoute si elle n'existe pas ou a été coupée (ex. déconnexion).
        if (liveJob?.isActive != true) {
            liveJob = viewModelScope.launch {
                battleRepo.observeLiveBattles()
                    .catch { _message.value = it.message }
                    .collect { battles ->
                        _liveBattles.value = battles
                        battles.filter { it.hostUid == myUid }.forEach { scheduleFinish(it) }
                    }
            }
        }
        refreshHistory()
    }

    private fun refreshHistory() {
        viewModelScope.launch {
            runCatching { battleRepo.history() }
                .onSuccess { _history.value = it }
                .onFailure { _message.value = it.message }
        }
    }

    /** Lance une battle (ou rouvre celle déjà en cours) et l'affiche. */
    fun launch(myName: String) {
        if (_launching.value) return
        viewModelScope.launch {
            _launching.value = true
            battleRepo.startBattle()
                .onSuccess {
                    scheduleFinish(it)
                    open(it, myName)
                }
                .onFailure { _message.value = it.message }
            _launching.value = false
        }
    }

    /** Ouvre une battle à l'écran (en direct si elle est en cours, sinon résultats). */
    fun open(battle: Battle, myName: String) {
        closeSession()
        this.myName = myName
        tracker = ComboTracker()
        pendingTaps = 0
        tapSamples.clear()
        _pendingPoints.value = 0
        _combo.value = 0
        _myBestCombo.value = 0
        _readyAt.value = emptyMap()
        _hype.value = 0f
        _supporters.value = emptyList()
        _current.value = battle
        _now.value = nowMs()

        sessionJobs = listOf(
            viewModelScope.launch {
                battleRepo.observeBattle(battle.id)
                    .catch { _message.value = it.message }
                    .collect { _current.value = it }
            },
            viewModelScope.launch {
                battleRepo.observeSupporters(battle.id)
                    .catch { _message.value = it.message }
                    .collect { _supporters.value = it }
            },
            // Horloge + jauge de hype.
            viewModelScope.launch {
                while (isActive) {
                    val now = nowMs()
                    _now.value = now
                    sampleHype(now)
                    delay(TICK_MS)
                }
            },
            // Envoi groupé des taps ; dernier envoi garanti à la fin du chrono.
            viewModelScope.launch {
                while (isActive) {
                    delay(FLUSH_INTERVAL_MS)
                    flush()
                }
            },
        )
    }

    /** Ferme l'écran de battle (les taps restants sont envoyés). */
    fun close() {
        val battle = _current.value
        viewModelScope.launch { flush(battle) }
        closeSession()
        _current.value = null
        refreshHistory()
    }

    /** Annule la battle ouverte (hôte uniquement) : aucune aura n'est créditée. */
    fun cancel() {
        val battle = _current.value ?: return
        if (battle.hostUid != myUid) return
        viewModelScope.launch {
            battleRepo.cancelBattle(battle.id)
                .onSuccess { cancelled ->
                    finishJobs.remove(battle.id)?.cancel()
                    _message.value = if (cancelled) "Battle annulée" else "Cette battle est déjà terminée"
                    close()
                }
                .onFailure { _message.value = it.message }
        }
    }

    private fun closeSession() {
        sessionJobs.forEach { it.cancel() }
        sessionJobs = emptyList()
        comboResetJob?.cancel()
    }

    /**
     * Tap sur un bouton de soutien. Renvoie les points gagnés (pour l'animation
     * « +X »), ou null si le tap est refusé (battle finie, cooldown, anti-spam).
     */
    fun tap(action: BattleAction): Int? {
        val battle = _current.value ?: return null
        val now = nowMs()
        if (!battle.isLive(now) || battle.hostUid == myUid) return null
        if (now < (_readyAt.value[action] ?: 0L)) return null

        val combo = tracker.hit(now) ?: return null
        val points = BattleRules.pointsFor(action, combo, battle.remainingMs(now))
        pendingTaps += 1
        _pendingPoints.value += points
        _combo.value = combo
        _myBestCombo.value = tracker.best
        if (action.cooldownMs > 0) _readyAt.value += action to (now + action.cooldownMs)

        comboResetJob?.cancel()
        comboResetJob = viewModelScope.launch {
            delay(BattleRules.COMBO_WINDOW_MS + 20)
            if (tracker.current(nowMs()) == 0) _combo.value = 0
        }
        return points
    }

    private suspend fun flush(battle: Battle? = _current.value) {
        if (battle == null) return
        val points = _pendingPoints.value
        val taps = pendingTaps
        if (points == 0 && taps == 0) return
        _pendingPoints.value = 0
        pendingTaps = 0
        battleRepo.sendTaps(battle, myName, points, taps, tracker.best)
            .onFailure { _message.value = "Taps non pris en compte : ${it.message}" }
    }

    private fun sampleHype(now: Long) {
        val totalTaps = (_current.value?.taps ?: 0) + pendingTaps
        tapSamples.addLast(now to totalTaps)
        while (tapSamples.size > 1 && now - tapSamples.first().first > HYPE_WINDOW_MS) {
            tapSamples.removeFirst()
        }
        val (oldestAt, oldestTaps) = tapSamples.first()
        val seconds = (now - oldestAt) / 1000f
        _hype.value = if (seconds > 0f) (totalTaps - oldestTaps) / seconds else 0f
    }

    /** Programme la clôture (et le crédit d'aura) d'une de mes battles à la fin du chrono. */
    private fun scheduleFinish(battle: Battle) {
        if (battle.hostUid != myUid || battle.status != "live") return
        if (finishJobs[battle.id]?.isActive == true) return
        finishJobs[battle.id] = viewModelScope.launch {
            val wait = battle.endsAt + BattleRules.GRACE_MS - nowMs()
            if (wait > 0) delay(wait)
            battleRepo.finishBattle(battle.id)
                .onSuccess { aura ->
                    if (aura > 0) _auraEarned.emit(aura)
                    refreshHistory()
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun clearMessage() { _message.value = null }

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val FLUSH_INTERVAL_MS = 400L
        const val TICK_MS = 100L
        const val HYPE_WINDOW_MS = 2_500L
    }
}
