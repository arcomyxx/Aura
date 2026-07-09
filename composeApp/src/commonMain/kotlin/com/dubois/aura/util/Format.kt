package com.dubois.aura.util

import kotlin.math.abs
import kotlin.math.round

private const val THIN_SPACE = ' ' // espace fine insécable, comme "11 200"

/** "11200" -> "11 200", "-80" -> "-80". */
fun Int.grouped(): String {
    val negative = this < 0
    val digits = abs(this).toString()
    val sb = StringBuilder()
    val remainder = digits.length % 3
    for (i in digits.indices) {
        if (i != 0 && (i - remainder) % 3 == 0) sb.append(THIN_SPACE)
        sb.append(digits[i])
    }
    return if (negative) "-$sb" else sb.toString()
}

/** Score signé : "+150", "-80". */
fun Int.signed(): String = if (this >= 0) "+${grouped()}" else grouped()

/** Version compacte pour la jauge : 8420 -> "8.4k", 8000 -> "8k". */
fun Int.compact(): String {
    if (abs(this) < 1_000) return toString()
    val value = this / 1_000.0
    val rounded = round(value * 10) / 10
    val text = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    return "${text}k"
}

/** Temps écoulé façon "maintenant", "12min", "2h", "1j". */
fun relativeTime(createdAt: Long, now: Long): String {
    val diff = now - createdAt
    if (diff < 0) return "maintenant"
    val minutes = diff / 60_000
    if (minutes < 1) return "maintenant"
    if (minutes < 60) return "${minutes}min"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h"
    val days = hours / 24
    return "${days}j"
}
