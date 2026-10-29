package com.jpn.crazygoose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpn.crazygoose.game.CellType
import com.jpn.crazygoose.game.Exercise
import com.jpn.crazygoose.game.Game
import com.jpn.crazygoose.game.GameSnapshot
import com.jpn.crazygoose.game.Level
import com.jpn.crazygoose.game.Phase

private val CellInk = Color(0xFF1D2318)

@Composable
fun PlayScreen(
    game: Game,
    speech: Speech,
    onTurnStart: (GameSnapshot) -> Unit,
    onFinished: (Game) -> Unit,
    onQuit: () -> Unit,
) {
    // Le moteur n'est pas observable : ce compteur force l'affichage après chaque action.
    var version by remember(game) { mutableIntStateOf(0) }
    fun act(action: () -> Unit) {
        speech.stop()
        action()
        version++
    }

    val phase = version.let { game.phase }

    LaunchedEffect(version) {
        when (phase) {
            is Phase.AwaitRoll -> game.snapshot()?.let(onTurnStart)
            is Phase.Finished -> onFinished(game)
            else -> Unit
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1.2f).verticalScroll(rememberScrollState())) { BoardView(game, version) }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TopBar(game, phase, onQuit)
                        PhasePanel(game, phase, speech, ::act, onQuit)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TopBar(game, phase, onQuit)
                    PhasePanel(game, phase, speech, ::act, onQuit)
                    BoardView(game, version)
                }
            }
        }
    }
}

@Composable
private fun TopBar(game: Game, phase: Phase, onQuit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val player = game.players[phase.player]
        Pawn(player, size = 36)
        Text(
            if (phase is Phase.Finished) "${player.name} a gagné !" else "Au tour de ${player.name}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
        )
        if (phase !is Phase.Finished) TextButton(onClick = onQuit) { Text("Pause") }
    }
}

@Composable
private fun PhasePanel(game: Game, phase: Phase, speech: Speech, act: (() -> Unit) -> Unit, onQuit: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            game.lastMove?.takeIf { phase !is Phase.AwaitRoll || it.player != phase.player }?.let { move ->
                val who = game.players[move.player].name
                val cell = game.board[move.to]
                Text(
                    "$who a fait ${move.dice} : case ${move.to}, ${cell.label}" + if (move.bounced) " (rebond sur l'arrivée)" else "",
                    fontSize = 16.sp,
                )
            }
            when (phase) {
                is Phase.AwaitRoll -> {
                    if (game.lastMove?.player == phase.player && game.board[game.position(phase.player)] == CellType.OIE) {
                        Text("L'Oie : relance le dé !", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Case ${game.position(phase.player)} sur ${game.board.last}", fontSize = 17.sp)
                    BigButton("Lancer le dé") { act { game.roll() } }
                }
                is Phase.DoExercise -> ExercisePanel(game, phase, speech, act)
                is Phase.Notice -> {
                    Text(phase.cell.label, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text(phase.message, fontSize = 20.sp)
                    BigButton("OK") { act { game.acknowledge() } }
                }
                is Phase.ChooseJoker -> {
                    Text("Joker : choisis ton exercice", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    phase.options.forEach { ex -> SecondaryButton(ex.name) { act { game.chooseJoker(ex) } } }
                }
                is Phase.Duel -> DuelPanel(game, phase, speech, act)
                is Phase.Prison -> {
                    val player = game.players[phase.player]
                    Text("Prison", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Défi de sortie, sinon tu passes ton prochain tour.", fontSize = 18.sp)
                    ExerciseCard(phase.exercise, player.level, speech)
                    CountdownTimer(player.level.seconds, key = phase)
                    BigButton("Défi fait, je sors !") { act { game.prisonResult(true) } }
                    SecondaryButton("Je passe mon prochain tour") { act { game.prisonResult(false) } }
                }
                is Phase.Finished -> FinishedPanel(game, onQuit)
            }
        }
    }
}

@Composable
private fun ExercisePanel(game: Game, phase: Phase.DoExercise, speech: Speech, act: (() -> Unit) -> Unit) {
    val player = game.players[phase.player]
    val title = when {
        phase.everyone -> "Défi collectif : tout le monde !"
        phase.replay -> "L'Oie : exercice, puis tu relances"
        else -> phase.cell.label
    }
    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    if (phase.everyone) {
        ExerciseCard(phase.exercise, null, speech)
        phase.exercise.baseReps?.let { base ->
            Text(game.players.joinToString(" · ") { "${it.name} : ${it.level.reps(base)}" }, fontSize = 17.sp)
        }
        CountdownTimer(Level.INTERMEDIAIRE.seconds, key = phase)
    } else {
        ExerciseCard(phase.exercise, player.level, speech)
        CountdownTimer(player.level.seconds, key = phase)
    }
    BigButton("Terminé") { act { game.exerciseDone() } }
    if (game.settings.referee && !phase.everyone) {
        SecondaryButton("Arbitre : exercice non fait (recule d'une case)") { act { game.exerciseNotDone() } }
    }
}

@Composable
private fun ExerciseCard(exercise: Exercise, level: Level?, speech: Speech) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(exercise.name, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        if (level != null) exercise.repsFor(level)?.let { Text("$it répétitions", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        Text(exercise.instruction, fontSize = 19.sp)
        Text("${exercise.category.label} · ${exercise.equipment.label}", fontSize = 15.sp)
        SecondaryButton("Écouter la consigne") { speech.say("${exercise.name}. ${exercise.instruction}") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DuelPanel(game: Game, phase: Phase.Duel, speech: Speech, act: (() -> Unit) -> Unit) {
    var opponent by remember(phase) { mutableStateOf<Int?>(null) }
    val me = phase.player
    Text("Duel !", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
    val other = opponent
    if (other == null) {
        Text("Choisis ton adversaire :", fontSize = 18.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            game.players.indices.filter { it != me }.forEach { i ->
                Toggle(game.players[i].name, checked = false) { opponent = i }
            }
        }
        return
    }
    Text("${game.players[me].name} contre ${game.players[other].name} : le premier qui termine avance d'une case.", fontSize = 18.sp)
    ExerciseCard(phase.exercise, null, speech)
    phase.exercise.baseReps?.let { base ->
        Text(listOf(me, other).joinToString(" · ") { "${game.players[it].name} : ${game.players[it].level.reps(base)}" }, fontSize = 17.sp)
    }
    Text("Qui a fini en premier ?", fontWeight = FontWeight.Bold)
    BigButton(game.players[me].name) { act { game.duelResult(other, me) } }
    BigButton(game.players[other].name) { act { game.duelResult(other, other) } }
    SecondaryButton("Égalité") { act { game.duelResult(other, null) } }
}

@Composable
private fun ColumnScope.FinishedPanel(game: Game, onQuit: () -> Unit) {
    Text("Fin de partie", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
    game.ranking().forEachIndexed { rank, i ->
        val p = game.players[i]
        val s = game.stats(i)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${rank + 1}.", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Pawn(p)
            Column {
                Text(p.name, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("${s.exercises} exercices · ${formatMinutes(s.activeSeconds)}")
            }
        }
    }
    val champion = game.players.indices.maxByOrNull { game.stats(it).exercises }
    champion?.let { Text("Badge « Infatigable » : ${game.players[it].name}, le plus d'exercices faits.", fontSize = 17.sp) }
    BigButton("Retour à l'accueil", onClick = onQuit)
}

/** Plateau en serpentin : les rangées alternent de gauche à droite puis de droite à gauche. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoardView(game: Game, version: Int) {
    version.let { }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = (maxWidth / 64.dp).toInt().coerceIn(6, 10)
        val cells = game.board.cells.indices.toList()
        val rows = cells.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEachIndexed { r, row ->
                val ordered = if (r % 2 == 0) row else row.reversed()
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Les cases manquantes de la dernière rangée gardent l'alignement.
                    val padding = columns - row.size
                    if (r % 2 == 1) repeat(padding) { Box(Modifier.weight(1f)) }
                    ordered.forEach { index ->
                        val type = game.board[index]
                        val here = game.players.indices.filter { game.position(it) == index }
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(cellColor(type), RoundedCornerShape(8.dp))
                                .border(1.dp, CellInk.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(3.dp),
                        ) {
                            Text("$index", fontSize = 10.sp, color = CellInk, modifier = Modifier.align(Alignment.TopStart))
                            Text(
                                cellSymbol(type),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CellInk,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center),
                            )
                            FlowRow(Modifier.align(Alignment.BottomCenter)) {
                                here.forEach { Pawn(game.players[it], size = 14) }
                            }
                        }
                    }
                    if (r % 2 == 0) repeat(padding) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}
