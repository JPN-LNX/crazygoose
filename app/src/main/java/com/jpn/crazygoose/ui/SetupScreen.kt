package com.jpn.crazygoose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpn.crazygoose.game.Equipment
import com.jpn.crazygoose.game.GameLength
import com.jpn.crazygoose.game.GameSettings
import com.jpn.crazygoose.game.Player

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    players: List<Player>,
    onAddPlayer: () -> Unit,
    onStart: (List<Player>, GameSettings) -> Unit,
    onBack: () -> Unit,
) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(players.take(10).map { it.id }) } }
    var length by remember { mutableStateOf(GameLength.MOYENNE) }
    val equipment = remember { mutableStateListOf(Equipment.AUCUN) }
    var exactFinish by remember { mutableStateOf(true) }
    var referee by remember { mutableStateOf(false) }

    ScreenScaffold("Nouvelle partie", onBack = onBack) {
        SectionTitle("Qui joue ? (2 à 10 joueurs)")
        if (players.size < 2) Text("Enregistrez au moins deux joueurs pour commencer.", fontSize = 17.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            players.forEach { p ->
                Toggle(p.name, checked = p.id in selected) {
                    if (p.id in selected) selected.remove(p.id) else if (selected.size < 10) selected.add(p.id)
                }
            }
        }
        SecondaryButton("Ajouter un joueur", onClick = onAddPlayer)

        SectionTitle("Durée")
        Choices(GameLength.entries, selected = length, label = { "${it.label} (${it.cells} cases)" }) { length = it }

        SectionTitle("Matériel disponible")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Equipment.entries.forEach { e ->
                Toggle(e.label, checked = e in equipment) {
                    if (e == Equipment.AUCUN) return@Toggle
                    if (e in equipment) equipment.remove(e) else equipment.add(e)
                }
            }
        }

        SectionTitle("Options")
        Option("Arrivée exacte : il faut tomber pile sur la dernière case", exactFinish) { exactFinish = it }
        Option("Un arbitre peut déclarer un exercice non fait", referee) { referee = it }

        val chosen = players.filter { it.id in selected }
        BigButton("C'est parti !", enabled = chosen.size in 2..10) {
            onStart(chosen, GameSettings(length, equipment.toSet(), exactFinish, referee))
        }
    }
}

@Composable
private fun Option(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
