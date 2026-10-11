package com.dubois.aura.model

/**
 * Type d'événement sélectionnable dans la feuille « Signaler un événement ».
 * [delta] est le montant proposé par défaut ; l'utilisateur peut le modifier
 * (le signe reste celui du type).
 */
data class EventType(
    val id: String,
    val emoji: String,
    val label: String,
    val delta: Int,
) {
    val isPositive: Boolean get() = delta >= 0
}

/** Catalogue des types d'événements proposés (voir maquette « Signaler »). */
object EventCatalog {
    val all: List<EventType> = listOf(
        EventType("geste_noble",        "⚡", "Geste noble",          200),
        EventType("charisme",           "✨", "Moment de charisme",   150),
        EventType("acte_courageux",     "🔥", "Acte courageux",  300),
        EventType("gaffe_sociale",      "💀", "Gaffe sociale",  -100),
        EventType("comportement_genant","🥵", "Comportement gênant", -80),
        EventType("sortie_aura",        "📉", "Sortie d'aura",  -150),
    )
}
