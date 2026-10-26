package com.dubois.aura.model

import kotlinx.serialization.Serializable

/**
 * Battle d'aura, stockée dans `battles/{battleId}`. L'hôte est « sur scène »
 * pendant [BattleRules.DURATION_MS] ; ses amis le rejoignent et tapent pour
 * faire grimper [score] en temps réel. À la fin, l'hôte encaisse
 * [BattleRules.auraFor] (une fraction du score) dans son `auraScore`.
 *
 * `status` : "live" | "ended" | "cancelled". Une battle dont [endsAt] est
 * dépassé est considérée terminée même si l'hôte ne l'a pas encore clôturée.
 * Une battle annulée par l'hôte ne rapporte aucune aura.
 */
@Serializable
data class Battle(
    val id: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val hostThemeColor: String = "",
    val status: String = "live",
    val startedAt: Long = 0L,
    val endsAt: Long = 0L,
    val score: Int = 0,
    val taps: Int = 0,
    val maxCombo: Int = 0,
    val supporterUids: List<String> = emptyList(),
    val auraAwarded: Int = 0,
) {
    val isCancelled: Boolean get() = status == "cancelled"

    fun isLive(now: Long): Boolean = status == "live" && now < endsAt
    fun remainingMs(now: Long): Long = (endsAt - now).coerceAtLeast(0L)
}

/**
 * Contribution d'un supporter, stockée dans `battles/{battleId}/supporters/{uid}`.
 */
@Serializable
data class BattleSupporter(
    val uid: String = "",
    val name: String = "",
    val points: Int = 0,
    val taps: Int = 0,
    val bestCombo: Int = 0,
)

/** Bouton de soutien proposé aux supporters pendant une battle. */
enum class BattleAction(
    val emoji: String,
    val label: String,
    val basePoints: Int,
    val cooldownMs: Long,
) {
    HYPE("🔥", "Hype", 10, 0L),
    CHARISME("✨", "Charisme", 50, 2_500L),
    AURA_MAX("👑", "Aura Max", 200, 8_000L),
}

/** Constantes et formules d'une battle (sans dépendance Firebase/UI). */
object BattleRules {
    const val DURATION_MS = 60_000L

    /** Les 10 dernières secondes comptent double. */
    const val FINAL_RUSH_MS = 10_000L

    /** Délai max entre deux taps pour prolonger le combo. */
    const val COMBO_WINDOW_MS = 700L

    /** Anti-spam : taps plus rapprochés ignorés (≈ 16 taps/s max). */
    const val MIN_TAP_INTERVAL_MS = 60L

    /** Marge laissée aux derniers envois après [Battle.endsAt] avant clôture. */
    const val GRACE_MS = 4_000L

    /** Part du score final convertie en aura réelle pour l'hôte. */
    fun auraFor(score: Int): Int = score / 10

    /** Multiplicateur de combo : x1, puis x2 dès 10, x3 dès 25, x5 dès 50. */
    fun comboMultiplier(combo: Int): Int = when {
        combo >= 50 -> 5
        combo >= 25 -> 3
        combo >= 10 -> 2
        else -> 1
    }

    /** Nom affiché du palier de combo courant (null sous x2). */
    fun comboTitle(combo: Int): String? = when {
        combo >= 50 -> "AURA OVERDRIVE"
        combo >= 25 -> "INARRÊTABLE"
        combo >= 10 -> "EN FEU"
        else -> null
    }

    fun isFinalRush(remainingMs: Long): Boolean = remainingMs in 1..FINAL_RUSH_MS

    /** Points rapportés par une action, compte tenu du combo et du rush final. */
    fun pointsFor(action: BattleAction, combo: Int, remainingMs: Long): Int {
        val rush = if (isFinalRush(remainingMs)) 2 else 1
        return action.basePoints * comboMultiplier(combo) * rush
    }
}

/**
 * Suivi local du combo d'un supporter : chaque tap à moins de
 * [BattleRules.COMBO_WINDOW_MS] du précédent prolonge la série.
 */
class ComboTracker {
    var combo: Int = 0
        private set
    var best: Int = 0
        private set
    private var lastTapAt: Long = Long.MIN_VALUE

    /** Combo courant si on n'a pas tapé depuis [now] (0 si la fenêtre est passée). */
    fun current(now: Long): Int = if (now - lastTapAt <= BattleRules.COMBO_WINDOW_MS) combo else 0

    /**
     * Enregistre un tap à l'instant [now]. Renvoie le combo après ce tap, ou
     * null si le tap est ignoré (trop rapproché du précédent).
     */
    fun hit(now: Long): Int? {
        if (lastTapAt != Long.MIN_VALUE && now - lastTapAt < BattleRules.MIN_TAP_INTERVAL_MS) return null
        combo = if (now - lastTapAt <= BattleRules.COMBO_WINDOW_MS) combo + 1 else 1
        lastTapAt = now
        if (combo > best) best = combo
        return combo
    }
}
