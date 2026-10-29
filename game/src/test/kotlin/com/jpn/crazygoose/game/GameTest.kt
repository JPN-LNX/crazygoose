package com.jpn.crazygoose.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameTest {
    private val alice = Player("a", "Alice", AgeGroup.ADULTE, Level.CONFIRME, 0)
    private val bob = Player("b", "Bob", AgeGroup.AGE_6_9, Level.DEBUTANT, 1)
    private val chloe = Player("c", "Chloé", AgeGroup.AGE_10_13, Level.INTERMEDIAIRE, 2)

    /** Partie sur un plateau choisi, pour tester une case précise. */
    private fun gameOn(
        cells: List<CellType>,
        players: List<Player> = listOf(alice, bob),
        settings: GameSettings = GameSettings(),
        positions: List<Int> = List(players.size) { 0 },
    ) = Game.resume(
        GameSnapshot(
            Board(cells), players, settings, positions,
            List(players.size) { false }, 0, List(players.size) { PlayerStats() },
        ),
        Random(42),
    )

    private fun board(vararg special: Pair<Int, CellType>, size: Int = 24): List<CellType> =
        MutableList(size + 1) { CellType.EXERCICE }.also { cells ->
            cells[0] = CellType.DEPART
            cells[size] = CellType.ARRIVEE
            special.forEach { (i, type) -> cells[i] = type }
        }

    @Test
    fun boardRespectsGenerationRules() {
        repeat(200) { seed ->
            for (length in GameLength.values()) {
                val board = BoardGenerator.generate(length, Random(seed))
                assertEquals(length.cells + 1, board.cells.size)
                assertEquals(CellType.DEPART, board[0])
                assertEquals(CellType.ARRIVEE, board[board.last])
                assertEquals(CellType.EXERCICE, board[1])
                assertEquals(CellType.EXERCICE, board[2])
                assertEquals(length.cells / 3, board.cells.count { it in BoardGenerator.SPECIAL_TYPES })
                board.cells.zipWithNext().forEach { (a, b) ->
                    val penalties = setOf(CellType.RECUL, CellType.PRISON)
                    assertFalse("Deux pénalités d'affilée (seed $seed)", a in penalties && b in penalties)
                }
            }
        }
    }

    @Test
    fun exerciseCellCreditsPlayerAndPassesTurn() {
        val game = gameOn(board())
        game.roll(3)
        val phase = game.phase as Phase.DoExercise
        assertEquals(0, phase.player)
        game.exerciseDone()
        assertEquals(1, game.stats(0).exercises)
        assertEquals(Level.CONFIRME.seconds, game.stats(0).activeSeconds)
        assertEquals(Phase.AwaitRoll(1), game.phase)
    }

    @Test
    fun gooseLetsThePlayerRollAgain() {
        val game = gameOn(board(4 to CellType.OIE))
        game.roll(4)
        game.exerciseDone()
        assertEquals(Phase.AwaitRoll(0), game.phase)
    }

    @Test
    fun collectiveChallengeCreditsEveryoneWithAnExerciseForTheYoungest() {
        val game = gameOn(board(5 to CellType.DEFI_COLLECTIF), players = listOf(alice, bob, chloe))
        game.roll(5)
        val phase = game.phase as Phase.DoExercise
        assertTrue(phase.everyone)
        assertTrue(phase.exercise.minAge <= AgeGroup.AGE_6_9)
        game.exerciseDone()
        assertEquals(listOf(1, 1, 1), (0..2).map { game.stats(it).exercises })
    }

    @Test
    fun exactFinishBouncesBack() {
        val game = gameOn(board(), positions = listOf(22, 0))
        game.roll(5)
        assertEquals(21, game.position(0))
        assertTrue(game.lastMove!!.bounced)
    }

    @Test
    fun withoutExactFinishOvershootWins() {
        val game = gameOn(board(), settings = GameSettings(exactFinish = false), positions = listOf(22, 0))
        game.roll(5)
        assertEquals(Phase.Finished(0), game.phase)
    }

    @Test
    fun landingExactlyOnFinishWins() {
        val game = gameOn(board(), positions = listOf(20, 0))
        game.roll(4)
        assertEquals(Phase.Finished(0), game.phase)
        assertNull(game.roll(1))
    }

    @Test
    fun prisonFailureSkipsNextTurn() {
        val game = gameOn(board(3 to CellType.PRISON))
        game.roll(3)
        game.prisonResult(challengeDone = false)
        assertEquals(Phase.AwaitRoll(1), game.phase)
        game.roll(1)
        game.exerciseDone()
        // Alice passe son tour : c'est de nouveau à Bob.
        assertEquals(Phase.AwaitRoll(1), game.phase)
        assertFalse(game.skipsNextTurn(0))
    }

    @Test
    fun prisonChallengeDoneKeepsTurnOrder() {
        val game = gameOn(board(3 to CellType.PRISON))
        game.roll(3)
        game.prisonResult(challengeDone = true)
        game.roll(1)
        game.exerciseDone()
        assertEquals(Phase.AwaitRoll(0), game.phase)
    }

    @Test
    fun bonusAndSetbackMoveThePlayer() {
        val bonus = gameOn(board(3 to CellType.BONUS))
        bonus.roll(3)
        assertTrue(bonus.position(0) in 5..7)
        assertTrue(bonus.phase is Phase.Notice)

        val setback = gameOn(board(3 to CellType.RECUL))
        setback.roll(3)
        assertTrue(setback.position(0) in 0..1)
        setback.acknowledge()
        assertEquals(Phase.AwaitRoll(1), setback.phase)
    }

    @Test
    fun jokerOffersThreeDistinctExercises() {
        val game = gameOn(board(3 to CellType.JOKER))
        game.roll(3)
        val options = (game.phase as Phase.ChooseJoker).options
        assertEquals(3, options.distinctBy { it.id }.size)
        game.chooseJoker(options[1])
        assertEquals(options[1], (game.phase as Phase.DoExercise).exercise)
    }

    @Test
    fun duelWinnerAdvancesOneCell() {
        val game = gameOn(board(3 to CellType.DUEL), players = listOf(alice, bob, chloe))
        game.roll(3)
        game.duelResult(opponent = 2, winner = 2)
        assertEquals(1, game.position(2))
        assertEquals(1, game.stats(0).exercises)
        assertEquals(1, game.stats(2).exercises)
        assertEquals(Phase.AwaitRoll(1), game.phase)
    }

    @Test
    fun refereeCanRejectAnExercise() {
        val game = gameOn(board(), settings = GameSettings(referee = true))
        game.roll(3)
        game.exerciseNotDone()
        assertEquals(2, game.position(0))
        assertEquals(0, game.stats(0).exercises)
    }

    @Test
    fun withoutRefereeNobodyCanRejectAnExercise() {
        val game = gameOn(board())
        game.roll(3)
        game.exerciseNotDone()
        assertTrue(game.phase is Phase.DoExercise)
    }

    @Test
    fun snapshotOnlyBetweenTurnsAndResumesTheSameGame() {
        val game = Game.start(listOf(alice, bob), GameSettings(length = GameLength.COURTE), Random(1))
        assertNotNull(game.snapshot())
        game.roll(2)
        assertNull(game.snapshot())
        while (game.phase !is Phase.AwaitRoll) {
            when (val p = game.phase) {
                is Phase.DoExercise -> game.exerciseDone()
                is Phase.Notice -> game.acknowledge()
                is Phase.ChooseJoker -> game.chooseJoker(p.options.first())
                is Phase.Duel -> game.duelResult(1 - p.player, null)
                is Phase.Prison -> game.prisonResult(true)
                else -> error("phase inattendue $p")
            }
        }
        val snapshot = game.snapshot()!!
        val resumed = Game.resume(snapshot)
        assertEquals(game.board, resumed.board)
        assertEquals(game.position(0), resumed.position(0))
        assertEquals(game.phase, resumed.phase)
    }

    @Test
    fun levelScalesRepetitions() {
        assertEquals(10, Level.DEBUTANT.reps(20))
        assertEquals(20, Level.INTERMEDIAIRE.reps(20))
        assertEquals(30, Level.CONFIRME.reps(20))
        assertEquals(1, Level.DEBUTANT.reps(1))
    }

    @Test
    fun pickerRespectsEquipmentAndAge() {
        val picker = ExercisePicker(ExerciseLibrary.base, setOf(Equipment.BALLON), Random(3))
        val candidates = picker.candidates(AgeGroup.AGE_3_5)
        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.all { it.equipment in setOf(Equipment.AUCUN, Equipment.BALLON) })
        assertTrue(candidates.all { it.minAge == AgeGroup.AGE_3_5 })
    }

    @Test
    fun aFullRandomGameAlwaysEnds() {
        repeat(50) { seed ->
            val game = Game.start(listOf(alice, bob, chloe), GameSettings(length = GameLength.LONGUE), Random(seed))
            var steps = 0
            while (game.phase !is Phase.Finished) {
                when (val p = game.phase) {
                    is Phase.AwaitRoll -> game.roll()
                    is Phase.DoExercise -> game.exerciseDone()
                    is Phase.Notice -> game.acknowledge()
                    is Phase.ChooseJoker -> game.chooseJoker(p.options.first())
                    is Phase.Duel -> game.duelResult((p.player + 1) % 3, p.player)
                    is Phase.Prison -> game.prisonResult(seed % 2 == 0)
                    is Phase.Finished -> Unit
                }
                steps++
                assertTrue("Partie sans fin (seed $seed)", steps < 10_000)
            }
            assertEquals(game.board.last, game.position(game.phase.player))
        }
    }
}
