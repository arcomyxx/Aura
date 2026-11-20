package com.dubois.aura.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyLimits(
    val date: String = "",           // "YYYY-MM-DD"
    val positivesSent: Int = 0,      // max 3/jour
    val negativesSent: Int = 0,      // max 1/jour
    val rewardEarned: Int = 0,       // max +10/jour via récompenses signaleur
    val receivedLoss: Int = 0,       // perte reçue aujourd'hui (plafond -50)
)

@Serializable
data class RarityRecord(
    val lastMs: Long = 0L,           // timestamp du dernier signalement
)
