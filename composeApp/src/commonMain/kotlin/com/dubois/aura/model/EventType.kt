package com.dubois.aura.model

enum class EventRarity { COMMON, RARE, LEGENDARY }

/**
 * Type d'événement sélectionnable dans la feuille « Signaler un événement ».
 * Chaque type applique un [delta] fixe au score d'aura de la cible.
 * [rarity] détermine la limite de signalements par donneur→cible :
 *   COMMON = 2×/jour, RARE = 1×/jour, LEGENDARY = 1×/semaine
 */
data class EventType(
    val id: String,
    val emoji: String,
    val label: String,
    val delta: Int,
    val rarity: EventRarity = EventRarity.COMMON,
) {
    val isPositive: Boolean get() = delta >= 0
}

/** Catalogue des types d'événements proposés (voir maquette « Signaler »). */
object EventCatalog {
    val all: List<EventType> = listOf(
        // Positifs
        EventType("legende_vivante",    "👑", "Légende vivante",       100, EventRarity.LEGENDARY),
        EventType("sauvetage",          "🦸", "Sauvetage héroïque",     50, EventRarity.LEGENDARY),
        EventType("acte_courageux",     "🔥", "Acte courageux",         30, EventRarity.RARE),
        EventType("victoire",           "🏆", "Victoire écrasante",     25, EventRarity.RARE),
        EventType("geste_noble",        "⚡", "Geste noble",            20, EventRarity.RARE),
        EventType("rizz",               "😏", "Rizz légendaire",        20, EventRarity.RARE),
        EventType("charisme",           "✨", "Moment de charisme",     15, EventRarity.RARE),
        EventType("drip",               "🕶️", "Drip impeccable",        12, EventRarity.RARE),
        EventType("punchline",          "🎤", "Punchline parfaite",     10, EventRarity.COMMON),
        EventType("entraide",           "🤝", "Coup de main",           10, EventRarity.COMMON),
        EventType("sang_froid",         "🧊", "Sang-froid",              8, EventRarity.COMMON),
        EventType("blague",             "😂", "Blague réussie",          5, EventRarity.COMMON),
        // Négatifs
        EventType("blague_ratee",       "🦗", "Blague ratée",           -5,  EventRarity.COMMON),
        EventType("retard",             "⏰", "En retard",              -5,  EventRarity.COMMON),
        EventType("ghosting",           "👻", "Ghosting",               -6,  EventRarity.COMMON),
        EventType("comportement_genant","🥵", "Comportement gênant",    -8,  EventRarity.COMMON),
        EventType("gaffe_sociale",      "💀", "Gaffe sociale",         -10,  EventRarity.COMMON),
        EventType("rateau",             "💔", "Râteau",                -12,  EventRarity.COMMON),
        EventType("chute",              "🍌", "Chute en public",       -15,  EventRarity.RARE),
        EventType("sortie_aura",        "📉", "Sortie d'aura",         -15,  EventRarity.RARE),
        EventType("mauvais_perdant",    "😤", "Mauvais perdant",       -20,  EventRarity.RARE),
        EventType("mensonge",           "🤥", "Mensonge grillé",       -25,  EventRarity.RARE),
        EventType("trahison",           "🐍", "Trahison",              -30,  EventRarity.LEGENDARY),
        EventType("cringe_absolu",      "🤡", "Cringe absolu",         -50,  EventRarity.LEGENDARY),
    )
}
