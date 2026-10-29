package com.jpn.crazygoose.game

import kotlin.random.Random

/** Ce que l'écran doit afficher et quelle action il attend du joueur. */
sealed interface Phase {
    val player: Int

    /** Le joueur lance le dé. */
    data class AwaitRoll(override val player: Int) : Phase

    /** Exercice à faire : par le joueur seul, ou par tout le monde ([everyone]). */
    data class DoExercise(
        override val player: Int,
        val exercise: Exercise,
        val cell: CellType,
        val everyone: Boolean = false,
        val replay: Boolean = false,
    ) : Phase

    /** Effet d'une case sans exercice (Bonus, Recul, Pause eau), à confirmer. */
    data class Notice(override val player: Int, val cell: CellType, val message: String) : Phase

    /** Case Joker : le joueur choisit parmi [options]. */
    data class ChooseJoker(override val player: Int, val options: List<Exercise>) : Phase

    /** Case Duel : le joueur choisit un adversaire, puis on indique qui a fini en premier. */
    data class Duel(override val player: Int, val exercise: Exercise) : Phase

    /** Case Prison : défi de sortie, sinon le joueur passe son prochain tour. */
    data class Prison(override val player: Int, val exercise: Exercise) : Phase

    data class Finished(override val player: Int) : Phase
}

data class LastMove(val player: Int, val dice: Int, val from: Int, val to: Int, val bounced: Boolean)

/** État sauvegardable entre deux tours, pour mettre une partie en pause et la reprendre. */
data class GameSnapshot(
    val board: Board,
    val players: List<Player>,
    val settings: GameSettings,
    val positions: List<Int>,
    val skipNext: List<Boolean>,
    val current: Int,
    val stats: List<PlayerStats>,
)

/**
 * Moteur d'une partie, sans dépendance à Android. L'interface appelle une action
 * selon la [phase] en cours ; une action appelée hors de sa phase est ignorée.
 */
class Game private constructor(
    val board: Board,
    val players: List<Player>,
    val settings: GameSettings,
    private val positions: IntArray,
    private val skipNext: BooleanArray,
    private val stats: Array<PlayerStats>,
    current: Int,
    private val random: Random,
    pool: List<Exercise>,
) {
    private val picker = ExercisePicker(pool, settings.equipment, random)

    var phase: Phase = Phase.AwaitRoll(current)
        private set

    var lastMove: LastMove? = null
        private set

    fun position(player: Int): Int = positions[player]
    fun skipsNextTurn(player: Int): Boolean = skipNext[player]
    fun stats(player: Int): PlayerStats = stats[player]

    /** Classement : position décroissante, puis exercices faits. */
    fun ranking(): List<Int> = players.indices.sortedWith(
        compareByDescending<Int> { positions[it] }.thenByDescending { stats[it].exercises },
    )

    fun snapshot(): GameSnapshot? {
        val awaiting = phase as? Phase.AwaitRoll ?: return null
        return GameSnapshot(board, players, settings, positions.toList(), skipNext.toList(), awaiting.player, stats.toList())
    }

    fun roll(dice: Int = random.nextInt(1, 7)): Int? {
        val p = (phase as? Phase.AwaitRoll)?.player ?: return null
        require(dice in 1..6)
        val from = positions[p]
        val (to, bounced) = target(from, dice)
        positions[p] = to
        lastMove = LastMove(p, dice, from, to, bounced)
        resolveLanding(p)
        return dice
    }

    fun exerciseDone() {
        val current = phase as? Phase.DoExercise ?: return
        val doers = if (current.everyone) players.indices.toList() else listOf(current.player)
        doers.forEach { credit(it) }
        endTurn(current.player, replay = current.replay)
    }

    /** Réservé à l'arbitre : l'exercice n'a pas été fait, le joueur recule d'une case. */
    fun exerciseNotDone() {
        val current = phase as? Phase.DoExercise ?: return
        if (!settings.referee || current.everyone) return
        positions[current.player] = maxOf(0, positions[current.player] - 1)
        endTurn(current.player, replay = false)
    }

    fun acknowledge() {
        val current = phase as? Phase.Notice ?: return
        endTurn(current.player, replay = false)
    }

    fun chooseJoker(exercise: Exercise) {
        val current = phase as? Phase.ChooseJoker ?: return
        if (exercise !in current.options) return
        phase = Phase.DoExercise(current.player, exercise, CellType.JOKER)
    }

    /** [winner] : indice du joueur qui a fini en premier, ou null si personne ne gagne le duel. */
    fun duelResult(opponent: Int, winner: Int?) {
        val current = phase as? Phase.Duel ?: return
        if (opponent == current.player || opponent !in players.indices) return
        credit(current.player)
        credit(opponent)
        if (winner == current.player || winner == opponent) {
            positions[winner] = target(positions[winner], 1).first
            if (positions[winner] == board.last) {
                phase = Phase.Finished(winner)
                return
            }
        }
        endTurn(current.player, replay = false)
    }

    fun prisonResult(challengeDone: Boolean) {
        val current = phase as? Phase.Prison ?: return
        if (challengeDone) credit(current.player) else skipNext[current.player] = true
        endTurn(current.player, replay = false)
    }

    private fun resolveLanding(p: Int) {
        if (positions[p] == board.last) {
            phase = Phase.Finished(p)
            return
        }
        val player = players[p]
        phase = when (val cell = board[positions[p]]) {
            CellType.EXERCICE -> Phase.DoExercise(p, picker.pick(player.ageGroup), cell)
            CellType.OIE -> Phase.DoExercise(p, picker.pick(player.ageGroup), cell, replay = true)
            CellType.DEFI_COLLECTIF -> Phase.DoExercise(p, picker.pick(youngest()), cell, everyone = true)
            CellType.JOKER -> Phase.ChooseJoker(p, picker.pickSeveral(player.ageGroup, 3))
            CellType.DUEL -> Phase.Duel(p, picker.pick(youngest()))
            CellType.PRISON -> Phase.Prison(p, picker.pick(player.ageGroup))
            CellType.BONUS -> {
                val steps = random.nextInt(2, 5)
                positions[p] = target(positions[p], steps).first
                if (positions[p] == board.last) Phase.Finished(p)
                else Phase.Notice(p, cell, "${player.name} avance de $steps cases.")
            }
            CellType.RECUL -> {
                val steps = random.nextInt(2, 5)
                positions[p] = maxOf(0, positions[p] - steps)
                Phase.Notice(p, cell, "${player.name} recule de $steps cases.")
            }
            CellType.PAUSE_EAU -> Phase.Notice(p, cell, cell.description)
            CellType.DEPART, CellType.ARRIVEE -> Phase.Notice(p, cell, cell.description)
        }
    }

    private fun target(from: Int, steps: Int): Pair<Int, Boolean> {
        val raw = from + steps
        if (raw <= board.last) return raw to false
        return if (settings.exactFinish) (board.last - (raw - board.last)) to true else board.last to false
    }

    private fun credit(p: Int) {
        val s = stats[p]
        stats[p] = s.copy(exercises = s.exercises + 1, activeSeconds = s.activeSeconds + players[p].level.seconds)
    }

    private fun youngest(): AgeGroup = players.minOf { it.ageGroup }

    private fun endTurn(p: Int, replay: Boolean) {
        if (replay) {
            phase = Phase.AwaitRoll(p)
            return
        }
        var next = (p + 1) % players.size
        repeat(players.size) {
            if (!skipNext[next]) {
                phase = Phase.AwaitRoll(next)
                return
            }
            skipNext[next] = false
            next = (next + 1) % players.size
        }
        phase = Phase.AwaitRoll(next)
    }

    companion object {
        fun start(
            players: List<Player>,
            settings: GameSettings,
            random: Random = Random.Default,
            pool: List<Exercise> = ExerciseLibrary.base,
        ): Game {
            require(players.size in 2..10) { "Une partie se joue de 2 à 10 joueurs." }
            val board = BoardGenerator.generate(settings.length, random)
            return Game(
                board, players, settings,
                IntArray(players.size), BooleanArray(players.size),
                Array(players.size) { PlayerStats() },
                0, random, pool,
            )
        }

        fun resume(snapshot: GameSnapshot, random: Random = Random.Default, pool: List<Exercise> = ExerciseLibrary.base): Game =
            Game(
                snapshot.board, snapshot.players, snapshot.settings,
                snapshot.positions.toIntArray(), snapshot.skipNext.toBooleanArray(),
                snapshot.stats.toTypedArray(), snapshot.current, random, pool,
            )
    }
}
