package com.jpn.crazygoose.game

import kotlin.math.roundToInt

/** Niveau sportif d'un joueur : règle la durée et le nombre de répétitions de ses exercices. */
enum class Level(val label: String, val seconds: Int, val repsFactor: Double) {
    DEBUTANT("Débutant", 20, 0.5),
    INTERMEDIAIRE("Intermédiaire", 30, 1.0),
    CONFIRME("Confirmé", 45, 1.5);

    fun reps(base: Int): Int = maxOf(1, (base * repsFactor).roundToInt())
}

enum class AgeGroup(val label: String) {
    AGE_3_5("3-5 ans"),
    AGE_6_9("6-9 ans"),
    AGE_10_13("10-13 ans"),
    AGE_14_17("14-17 ans"),
    ADULTE("Adulte"),
    SENIOR("Senior"),
}

enum class Equipment(val label: String) {
    AUCUN("Sans matériel"),
    BALLON("Ballon"),
    TRAMPOLINE("Trampoline"),
    CORDE("Corde à sauter"),
    BATON("Bâton"),
    SEAU("Seau"),
    PLOTS("Cônes ou plots"),
    CERCEAU("Cerceau"),
    FRISBEE("Frisbee"),
    ELASTIQUE("Élastique de résistance"),
    BANC("Banc ou chaise"),
    CRAIE("Craie"),
    RAQUETTES("Raquettes et balle"),
    COUSSIN("Coussin"),
}

enum class Category(val label: String) {
    CARDIO("Cardio"),
    RENFORCEMENT("Renforcement"),
    SOUPLESSE("Souplesse"),
    EQUILIBRE("Équilibre"),
    COORDINATION("Coordination"),
    ADRESSE("Adresse"),
}

/**
 * Un exercice de la bibliothèque. Tous les exercices sont chronométrés selon le niveau du joueur ;
 * quand [baseReps] est renseigné, un nombre de répétitions s'affiche en plus.
 */
data class Exercise(
    val id: String,
    val name: String,
    val instruction: String,
    val category: Category,
    val equipment: Equipment = Equipment.AUCUN,
    val minAge: AgeGroup = AgeGroup.AGE_3_5,
    val baseReps: Int? = null,
) {
    fun repsFor(level: Level): Int? = baseReps?.let(level::reps)
}

data class Player(
    val id: String,
    val name: String,
    val ageGroup: AgeGroup,
    val level: Level,
    val colorIndex: Int,
)

enum class CellType(val label: String, val description: String) {
    DEPART("Départ", "Tous les joueurs partent d'ici."),
    EXERCICE("Exercice", "Fais l'exercice tiré au sort."),
    OIE("L'Oie", "Fais l'exercice, puis relance le dé."),
    BONUS("Bonus", "Avance de 2 à 4 cases."),
    RECUL("Recul", "Recule de 2 à 4 cases."),
    PRISON("Prison", "Fais le défi de sortie ou passe ton prochain tour."),
    DEFI_COLLECTIF("Défi collectif", "Tout le monde fait l'exercice en même temps."),
    PAUSE_EAU("Pause eau", "Tout le monde boit un verre d'eau et souffle 30 secondes."),
    JOKER("Joker", "Choisis ton exercice parmi trois."),
    DUEL("Duel", "Défie un joueur : le premier qui termine avance d'une case."),
    ARRIVEE("Arrivée", "Le premier joueur arrivé ici gagne."),
}

/** Durée de partie : elle fixe la longueur du plateau. */
enum class GameLength(val label: String, val cells: Int) {
    COURTE("Courte, environ 20 min", 24),
    MOYENNE("Moyenne, environ 40 min", 42),
    LONGUE("Longue, environ 1 h", 63),
}

/** Plateau : la case 0 est le départ, la dernière case est l'arrivée. */
data class Board(val cells: List<CellType>) {
    val last: Int get() = cells.lastIndex
    operator fun get(index: Int): CellType = cells[index]
}

data class GameSettings(
    val length: GameLength = GameLength.MOYENNE,
    val equipment: Set<Equipment> = setOf(Equipment.AUCUN),
    /** Il faut tomber pile sur l'arrivée, sinon on recule du surplus. */
    val exactFinish: Boolean = true,
    /** Un arbitre désigné peut déclarer un exercice non fait. */
    val referee: Boolean = false,
)

data class PlayerStats(val exercises: Int = 0, val activeSeconds: Int = 0)
