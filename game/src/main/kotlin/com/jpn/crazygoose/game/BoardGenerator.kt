package com.jpn.crazygoose.game

import kotlin.random.Random

/**
 * Génère un plateau : environ une case sur trois est spéciale, aucune sur les deux premières cases,
 * et jamais deux cases Recul ou Prison d'affilée.
 */
object BoardGenerator {
    val SPECIAL_TYPES = listOf(
        CellType.OIE,
        CellType.BONUS,
        CellType.RECUL,
        CellType.PRISON,
        CellType.DEFI_COLLECTIF,
        CellType.PAUSE_EAU,
        CellType.JOKER,
        CellType.DUEL,
    )

    private val PENALTIES = setOf(CellType.RECUL, CellType.PRISON)

    fun generate(length: GameLength, random: Random = Random.Default): Board {
        val last = length.cells
        val cells = MutableList(last + 1) { CellType.EXERCICE }
        cells[0] = CellType.DEPART
        cells[last] = CellType.ARRIVEE

        val specialCount = last / 3
        val positions = (3 until last).shuffled(random).take(specialCount).sorted()

        val bag = ArrayDeque<CellType>()
        for (position in positions) {
            if (bag.isEmpty()) bag.addAll(SPECIAL_TYPES.shuffled(random))
            val previousIsPenalty = cells[position - 1] in PENALTIES
            val pick = bag.firstOrNull { !(previousIsPenalty && it in PENALTIES) } ?: CellType.BONUS
            bag.remove(pick)
            cells[position] = pick
        }
        return Board(cells)
    }
}
