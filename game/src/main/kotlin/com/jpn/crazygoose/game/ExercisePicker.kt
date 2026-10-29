package com.jpn.crazygoose.game

import kotlin.random.Random

/** Tire des exercices réalisables avec le matériel coché et adaptés à l'âge, en évitant les répétitions récentes. */
class ExercisePicker(
    private val pool: List<Exercise>,
    equipment: Set<Equipment>,
    private val random: Random,
) {
    private val available = equipment + Equipment.AUCUN
    private val recent = ArrayDeque<String>()

    fun candidates(youngest: AgeGroup): List<Exercise> {
        val matching = pool.filter { it.equipment in available && it.minAge <= youngest }
        return matching.ifEmpty { pool.filter { it.equipment == Equipment.AUCUN && it.minAge == AgeGroup.AGE_3_5 } }
    }

    fun pick(youngest: AgeGroup): Exercise = pickSeveral(youngest, 1).first()

    fun pickSeveral(youngest: AgeGroup, count: Int): List<Exercise> {
        val all = candidates(youngest)
        val fresh = all.filter { it.id !in recent }.shuffled(random)
        val stale = all.filter { it.id in recent }.shuffled(random)
        val picked = (fresh + stale).take(count)
        picked.forEach(::remember)
        return picked
    }

    private fun remember(exercise: Exercise) {
        recent.addLast(exercise.id)
        while (recent.size > RECENT_MEMORY) recent.removeFirst()
    }

    private companion object {
        const val RECENT_MEMORY = 6
    }
}
