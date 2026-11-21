package com.dubois.aura.util

import androidx.compose.runtime.compositionLocalOf

typealias ShareProfileAction = (name: String, tierName: String, score: Int, rank: Int, tierHex: String) -> Unit
typealias ShareBattleAction = (hostName: String, battleScore: Int, supporters: Int, bestCombo: Int, auraAwarded: Int, accentHex: String) -> Unit
typealias InviteToBattleAction = (hostName: String) -> Unit
typealias ShareGroupInviteAction = (groupName: String, token: String) -> Unit

val LocalShareProfile = compositionLocalOf<ShareProfileAction> { { _, _, _, _, _ -> } }
val LocalShareBattle = compositionLocalOf<ShareBattleAction> { { _, _, _, _, _, _ -> } }
val LocalInviteToBattle = compositionLocalOf<InviteToBattleAction> { { _ -> } }
val LocalShareGroupInvite = compositionLocalOf<ShareGroupInviteAction> { { _, _ -> } }
