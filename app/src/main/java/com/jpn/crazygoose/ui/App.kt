package com.jpn.crazygoose.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpn.crazygoose.data.PlayedGame
import com.jpn.crazygoose.data.Store
import com.jpn.crazygoose.game.CellType
import com.jpn.crazygoose.game.Game
import com.jpn.crazygoose.game.Player
import java.text.DateFormat
import java.util.Date

sealed interface Screen {
    data object Warning : Screen
    data object Home : Screen
    data object Players : Screen
    data class EditPlayer(val player: Player?) : Screen
    data object Setup : Screen
    data object Warmup : Screen
    data object Play : Screen
    data object History : Screen
    data object Rules : Screen
    data object Settings : Screen
}

@Composable
fun App(store: Store, speech: Speech) {
    var screen by remember { mutableStateOf(if (store.warningAccepted) Screen.Home else Screen.Warning) }
    var players by remember { mutableStateOf(store.loadPlayers()) }
    var game by remember { mutableStateOf<Game?>(null) }
    var hasSavedGame by remember { mutableStateOf(store.loadSavedGame() != null) }

    fun savePlayers(updated: List<Player>) {
        players = updated
        store.savePlayers(updated)
    }

    if (screen != Screen.Home && screen != Screen.Warning) BackHandler { screen = Screen.Home }

    when (val s = screen) {
        Screen.Warning -> WarningScreen {
            store.warningAccepted = true
            screen = Screen.Home
        }
        Screen.Home -> HomeScreen(
            hasSavedGame = hasSavedGame,
            onNewGame = { screen = Screen.Setup },
            onResume = {
                store.loadSavedGame()?.let { game = Game.resume(it); screen = Screen.Play }
            },
            onPlayers = { screen = Screen.Players },
            onHistory = { screen = Screen.History },
            onRules = { screen = Screen.Rules },
            onSettings = { screen = Screen.Settings },
        )
        Screen.Players -> PlayersScreen(
            players = players,
            onEdit = { screen = Screen.EditPlayer(it) },
            onAdd = { screen = Screen.EditPlayer(null) },
            onBack = { screen = Screen.Home },
        )
        is Screen.EditPlayer -> EditPlayerScreen(
            initial = s.player,
            usedColors = players.filter { it.id != s.player?.id }.map { it.colorIndex }.toSet(),
            onSave = { saved ->
                savePlayers(if (players.any { it.id == saved.id }) players.map { if (it.id == saved.id) saved else it } else players + saved)
                screen = Screen.Players
            },
            onDelete = s.player?.let { p -> { savePlayers(players - p); screen = Screen.Players } },
            onBack = { screen = Screen.Players },
        )
        Screen.Setup -> SetupScreen(
            players = players,
            onAddPlayer = { screen = Screen.EditPlayer(null) },
            onStart = { selected, settings ->
                game = Game.start(selected, settings)
                store.clearSavedGame()
                hasSavedGame = false
                screen = Screen.Warmup
            },
            onBack = { screen = Screen.Home },
        )
        Screen.Warmup -> WarmupScreen(speech = speech, onDone = { screen = Screen.Play })
        Screen.Play -> game?.let { g ->
            PlayScreen(
                game = g,
                speech = speech,
                onTurnStart = { store.saveGame(it); hasSavedGame = true },
                onFinished = { finished ->
                    store.clearSavedGame()
                    hasSavedGame = false
                    store.addToHistory(
                        PlayedGame(
                            endedAt = System.currentTimeMillis(),
                            winner = finished.players[finished.phase.player].name,
                            results = finished.players.indices.map {
                                PlayedGame.Result(finished.players[it].name, finished.stats(it).exercises, finished.stats(it).activeSeconds)
                            },
                        ),
                    )
                },
                onQuit = { screen = Screen.Home },
            )
        } ?: run { screen = Screen.Home }
        Screen.History -> HistoryScreen(store.loadHistory()) { screen = Screen.Home }
        Screen.Rules -> RulesScreen { screen = Screen.Home }
        Screen.Settings -> SettingsScreen(
            onErase = {
                store.eraseAll()
                players = emptyList()
                hasSavedGame = false
            },
            onBack = { screen = Screen.Home },
        )
    }
}

@Composable
private fun WarningScreen(onAccept: () -> Unit) {
    ScreenScaffold("Avant de jouer") {
        Text("Crazy Goose fait bouger toute la famille. Pour jouer en sécurité :", fontSize = 18.sp)
        listOf(
            "En cas de doute sur votre santé, demandez conseil à un médecin avant de jouer.",
            "Un adulte surveille les enfants pendant toute la partie.",
            "Arrêtez immédiatement en cas de douleur, de vertige ou de malaise.",
            "Jouez dans un espace dégagé, sur un sol non glissant, et buvez régulièrement.",
        ).forEach { Text("•  $it", fontSize = 17.sp) }
        Spacer(Modifier.height(8.dp))
        BigButton("J'ai compris, on joue !", onClick = onAccept)
    }
}

@Composable
private fun HomeScreen(
    hasSavedGame: Boolean,
    onNewGame: () -> Unit,
    onResume: () -> Unit,
    onPlayers: () -> Unit,
    onHistory: () -> Unit,
    onRules: () -> Unit,
    onSettings: () -> Unit,
) {
    ScreenScaffold("Crazy Goose") {
        Text("Le jeu de l'oie qui fait bouger toute la famille.", fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        BigButton("Nouvelle partie", onClick = onNewGame)
        if (hasSavedGame) SecondaryButton("Reprendre la partie en cours", onClick = onResume)
        SecondaryButton("Joueurs", onClick = onPlayers)
        SecondaryButton("Historique", onClick = onHistory)
        SecondaryButton("Règles du jeu", onClick = onRules)
        SecondaryButton("Réglages", onClick = onSettings)
    }
}

@Composable
private fun RulesScreen(onBack: () -> Unit) {
    ScreenScaffold("Règles du jeu", onBack = onBack) {
        Text(
            "Chacun à son tour lance le dé et avance son pion. Sur une case exercice, le joueur fait l'exercice " +
                "pendant la durée affichée, adaptée à son niveau. Il n'y a ni réussite ni échec : le but est de bouger ! " +
                "Le premier arrivé sur la dernière case gagne. Avec l'arrivée exacte, il faut tomber pile, sinon on recule du surplus.",
            fontSize = 17.sp,
        )
        Text("Les cases spéciales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        CellType.entries.filter { it != CellType.DEPART && it != CellType.EXERCICE }.forEach {
            Text("${it.label} : ${it.description}", fontSize = 17.sp)
        }
    }
}

@Composable
private fun HistoryScreen(history: List<PlayedGame>, onBack: () -> Unit) {
    ScreenScaffold("Historique", onBack = onBack) {
        if (history.isEmpty()) Text("Aucune partie terminée pour l'instant.", fontSize = 17.sp)
        val format = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
        history.forEach { g ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(format.format(Date(g.endedAt)), fontWeight = FontWeight.Bold)
                    Text("Gagnant : ${g.winner}")
                    g.results.forEach { Text("${it.name} : ${it.exercises} exercices, ${formatMinutes(it.activeSeconds)}") }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(onErase: () -> Unit, onBack: () -> Unit) {
    var erased by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    ScreenScaffold("Réglages", onBack = onBack) {
        Text("Toutes les données restent sur ce téléphone. Rien n'est envoyé sur Internet.", fontSize = 17.sp)
        when {
            erased -> Text("Toutes les données ont été effacées.", fontWeight = FontWeight.Bold)
            confirm -> {
                Text("Effacer les profils, la partie en cours et l'historique ? Cette action est définitive.")
                BigButton("Oui, tout effacer") { onErase(); erased = true }
                SecondaryButton("Annuler") { confirm = false }
            }
            else -> SecondaryButton("Effacer toutes les données") { confirm = true }
        }
    }
}

fun formatMinutes(seconds: Int): String = if (seconds < 60) "$seconds s d'activité" else "${seconds / 60} min ${seconds % 60} s d'activité"
