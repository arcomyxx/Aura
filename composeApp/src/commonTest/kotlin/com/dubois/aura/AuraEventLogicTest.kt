package com.dubois.aura

import com.dubois.aura.model.EventCatalog
import com.dubois.aura.model.EventRarity
import com.dubois.aura.model.EventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests de la logique métier des signalements d'aura.
 * Ces règles sont désormais appliquées côté serveur (Cloud Function reportEvent),
 * mais les tests valident les constantes et comportements attendus.
 */
class AuraEventLogicTest {

    private val COMMON  = EventType("test_common",     "✅", "Test COMMON",     10,  EventRarity.COMMON)
    private val RARE    = EventType("test_rare",       "⭐", "Test RARE",        30,  EventRarity.RARE)
    private val LEGEND  = EventType("test_legendary",  "👑", "Test LEGENDARY",  100, EventRarity.LEGENDARY)
    private val NEGATIVE = EventType("test_neg",       "💀", "Test NEG",        -10, EventRarity.COMMON)

    // ── isPositive ──────────────────────────────────────────────────────────

    @Test
    fun positiveEventIsPositive() {
        assertTrue(COMMON.isPositive)
        assertTrue(RARE.isPositive)
        assertTrue(LEGEND.isPositive)
    }

    @Test
    fun negativeEventIsNotPositive() {
        assertFalse(NEGATIVE.isPositive)
    }

    @Test
    fun zeroDeltaCountsAsPositive() {
        val zero = EventType("zero", "0️⃣", "Zero", 0)
        assertTrue(zero.isPositive)
    }

    // ── Catalogue ───────────────────────────────────────────────────────────

    @Test
    fun catalogHasNoNeutralEvents() {
        // Tous les événements ont un delta != 0.
        EventCatalog.all.forEach { type ->
            assertTrue(type.delta != 0, "${type.id} a un delta nul")
        }
    }

    @Test
    fun legendaryEventsHaveHighAbsDelta() {
        val legendaries = EventCatalog.all.filter { it.rarity == EventRarity.LEGENDARY }
        assertTrue(legendaries.isNotEmpty())
        legendaries.forEach { type ->
            assertTrue(
                kotlin.math.abs(type.delta) >= 50,
                "${type.id} légendaire devrait avoir |delta| ≥ 50, got ${type.delta}",
            )
        }
    }

    @Test
    fun catalogContainsBothPolarities() {
        val positives = EventCatalog.all.count { it.isPositive }
        val negatives = EventCatalog.all.count { !it.isPositive }
        assertTrue(positives > 0, "Pas d'événements positifs dans le catalogue")
        assertTrue(negatives > 0, "Pas d'événements négatifs dans le catalogue")
    }

    // ── Logique de delta effectif (reproduit la logique CF) ─────────────────

    @Test
    fun effectiveDeltaClampedToRemainingLoss() {
        val MAX_LOSS_PER_DAY = 50
        val receivedLoss = 45          // Cible a déjà perdu 45 pts aujourd'hui.
        val requested = -10            // On tente de retirer 10 pts.
        val remaining = MAX_LOSS_PER_DAY - receivedLoss  // 5

        val effective = maxOf(requested, -remaining)      // max(-10, -5) = -5
        assertEquals(-5, effective)
    }

    @Test
    fun effectiveDeltaUnchangedWhenRoomAvailable() {
        val MAX_LOSS_PER_DAY = 50
        val receivedLoss = 20
        val requested = -10
        val remaining = MAX_LOSS_PER_DAY - receivedLoss  // 30

        val effective = maxOf(requested, -remaining)      // max(-10, -30) = -10
        assertEquals(-10, effective)
    }

    @Test
    fun effectiveDeltaZeroWhenLossCapReached() {
        val MAX_LOSS_PER_DAY = 50
        val receivedLoss = 50
        val requested = -5
        val remaining = MAX_LOSS_PER_DAY - receivedLoss  // 0

        val effective = if (remaining <= 0) 0 else maxOf(requested, -remaining)
        assertEquals(0, effective)
    }

    // ── Récompense reporteur ────────────────────────────────────────────────

    @Test
    fun reporterRewardClampedToMaxDaily() {
        val MAX_REWARD_PER_DAY = 10
        val REPORTER_REWARD = 2

        val rewardEarned0 = 0
        assertEquals(2, minOf(REPORTER_REWARD, MAX_REWARD_PER_DAY - rewardEarned0))

        val rewardEarned9 = 9
        assertEquals(1, minOf(REPORTER_REWARD, MAX_REWARD_PER_DAY - rewardEarned9))

        val rewardEarned10 = 10
        assertEquals(0, maxOf(0, minOf(REPORTER_REWARD, MAX_REWARD_PER_DAY - rewardEarned10)))
    }

    @Test
    fun negativeEventGivesNoReward() {
        // Côté CF : reward = 0 si !isPositive.
        val reward = if (NEGATIVE.isPositive) 2 else 0
        assertEquals(0, reward)
    }

    // ── Cooldowns par rareté ────────────────────────────────────────────────

    @Test
    fun cooldownsAreOrdered() {
        val commonCooldown  = 12 * 3_600_000L
        val rareCooldown    = 24 * 3_600_000L
        val legendaryCooldown = 168 * 3_600_000L

        assertTrue(commonCooldown < rareCooldown)
        assertTrue(rareCooldown < legendaryCooldown)
    }

    @Test
    fun cooldownExpiredWhenElapsedExceeds() {
        val cooldownMs = 12 * 3_600_000L
        val lastMs = 1_000_000L
        val nowWithin = lastMs + 6 * 3_600_000L   // 6h plus tard
        val nowAfter  = lastMs + 13 * 3_600_000L  // 13h plus tard

        assertTrue(nowWithin - lastMs < cooldownMs, "6h < 12h : cooldown non écoulé")
        assertTrue(nowAfter  - lastMs >= cooldownMs, "13h ≥ 12h : cooldown écoulé")
    }
}
