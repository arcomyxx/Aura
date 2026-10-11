package com.dubois.aura.model

/**
 * Type d'événement sélectionnable dans la feuille « Signaler un événement ».
 * Chaque type applique un [delta] fixe au score d'aura de la cible.
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
        // Positifs
        EventType("legende_vivante",    "👑", "Légende vivante",       1000),
        EventType("sauvetage",          "🦸", "Sauvetage héroïque",     500),
        EventType("acte_courageux",     "🔥", "Acte courageux",         300),
        EventType("victoire",           "🏆", "Victoire écrasante",     250),
        EventType("geste_noble",        "⚡", "Geste noble",            200),
        EventType("rizz",               "😏", "Rizz légendaire",        200),
        EventType("charisme",           "✨", "Moment de charisme",     150),
        EventType("drip",               "🕶️", "Drip impeccable",        120),
        EventType("punchline",          "🎤", "Punchline parfaite",     100),
        EventType("entraide",           "🤝", "Coup de main",           100),
        EventType("sang_froid",         "🧊", "Sang-froid",              80),
        EventType("blague",             "😂", "Blague réussie",          50),
        // Négatifs
        EventType("blague_ratee",       "🦗", "Blague ratée",           -50),
        EventType("retard",             "⏰", "En retard",              -50),
        EventType("ghosting",           "👻", "Ghosting",               -60),
        EventType("comportement_genant","🥵", "Comportement gênant",    -80),
        EventType("gaffe_sociale",      "💀", "Gaffe sociale",         -100),
        EventType("rateau",             "💔", "Râteau",                -120),
        EventType("chute",              "🍌", "Chute en public",       -150),
        EventType("sortie_aura",        "📉", "Sortie d'aura",         -150),
        EventType("mauvais_perdant",    "😤", "Mauvais perdant",       -200),
        EventType("mensonge",           "🤥", "Mensonge grillé",       -250),
        EventType("trahison",           "🐍", "Trahison",              -300),
        EventType("cringe_absolu",      "🤡", "Cringe absolu",         -500),
    )
}
