package com.dubois.aura

import com.dubois.aura.model.BattleAction
import com.dubois.aura.model.BattleRules
import com.dubois.aura.model.ComboTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BattleRulesTest {

    @Test
    fun comboGrowsWithinWindowAndResetsAfter() {
        val tracker = ComboTracker()
        assertEquals(1, tracker.hit(1_000))
        assertEquals(2, tracker.hit(1_300))
        assertEquals(3, tracker.hit(1_900))
        // Fenêtre dépassée -> nouvelle série, mais le record est conservé.
        assertEquals(1, tracker.hit(3_000))
        assertEquals(3, tracker.best)
        assertEquals(0, tracker.current(5_000))
    }

    @Test
    fun tooFastTapsAreIgnored() {
        val tracker = ComboTracker()
        tracker.hit(1_000)
        assertNull(tracker.hit(1_020))
        assertEquals(2, tracker.hit(1_100))
    }

    @Test
    fun multiplierTiers() {
        assertEquals(1, BattleRules.comboMultiplier(9))
        assertEquals(2, BattleRules.comboMultiplier(10))
        assertEquals(3, BattleRules.comboMultiplier(25))
        assertEquals(5, BattleRules.comboMultiplier(80))
    }

    @Test
    fun pointsApplyComboAndFinalRush() {
        assertEquals(10, BattleRules.pointsFor(BattleAction.HYPE, combo = 1, remainingMs = 30_000))
        assertEquals(30, BattleRules.pointsFor(BattleAction.HYPE, combo = 30, remainingMs = 30_000))
        assertEquals(60, BattleRules.pointsFor(BattleAction.HYPE, combo = 30, remainingMs = 5_000))
        // Battle finie : plus de bonus de rush.
        assertEquals(200, BattleRules.pointsFor(BattleAction.AURA_MAX, combo = 1, remainingMs = 0))
    }

    @Test
    fun auraConversion() {
        assertEquals(0, BattleRules.auraFor(9))
        assertEquals(123, BattleRules.auraFor(1_234))
    }
}
