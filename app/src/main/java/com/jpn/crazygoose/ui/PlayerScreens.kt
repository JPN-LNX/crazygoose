package com.jpn.crazygoose.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpn.crazygoose.game.AgeGroup
import com.jpn.crazygoose.game.Level
import com.jpn.crazygoose.game.Player
import java.util.UUID

@Composable
fun PlayersScreen(players: List<Player>, onEdit: (Player) -> Unit, onAdd: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold("Joueurs", onBack = onBack) {
        if (players.isEmpty()) Text("Aucun joueur enregistré. Ajoutez les membres de la famille pour ne pas les ressaisir à chaque partie.", fontSize = 17.sp)
        players.forEach { p ->
            Card(Modifier.fillMaxWidth().clickable { onEdit(p) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Pawn(p, size = 40)
                    Column {
                        Text(p.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("${p.ageGroup.label} · ${p.level.label}")
                    }
                }
            }
        }
        BigButton("Ajouter un joueur", onClick = onAdd)
    }
}

/** Questionnaire de niveau : 3 questions notées de 0 à 2. */
private val LEVEL_QUESTIONS = listOf(
    "Combien de fois par semaine fais-tu du sport ?" to listOf("Jamais ou presque", "1 à 2 fois", "3 fois ou plus"),
    "Peux-tu courir 5 minutes sans t'arrêter ?" to listOf("Non", "Avec un peu de mal", "Facilement"),
    "Après avoir monté deux étages, tu es…" to listOf("Très essoufflé", "Un peu essoufflé", "En pleine forme"),
)

private fun levelFromScore(score: Int): Level = when {
    score <= 1 -> Level.DEBUTANT
    score <= 4 -> Level.INTERMEDIAIRE
    else -> Level.CONFIRME
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditPlayerScreen(
    initial: Player?,
    usedColors: Set<Int>,
    onSave: (Player) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var age by remember { mutableStateOf(initial?.ageGroup ?: AgeGroup.ADULTE) }
    var level by remember { mutableStateOf(initial?.level ?: Level.INTERMEDIAIRE) }
    var color by remember { mutableStateOf(initial?.colorIndex ?: (PawnColors.indices.firstOrNull { it !in usedColors } ?: 0)) }
    val answers = remember { mutableStateListOf<Int?>(null, null, null) }

    ScreenScaffold(if (initial == null) "Nouveau joueur" else "Modifier ${initial.name}", onBack = onBack) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(20) },
            label = { Text("Pseudo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionTitle("Âge")
        Choices(AgeGroup.entries, selected = age, label = { it.label }) { age = it }

        SectionTitle("Couleur du pion")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PawnColors.forEachIndexed { i, c ->
                Box(
                    Modifier
                        .size(44.dp)
                        .background(c, CircleShape)
                        .border(if (i == color) 4.dp else 0.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                        .clickable { color = i },
                )
            }
        }

        SectionTitle("Niveau")
        Text("Réponds aux trois questions pour qu'on te propose un niveau. Tu peux ensuite le changer.")
        LEVEL_QUESTIONS.forEachIndexed { qi, (question, options) ->
            Text(question, fontWeight = FontWeight.Bold)
            Choices(options.indices.toList(), selected = answers[qi], label = { options[it] }) { choice ->
                answers[qi] = choice
                if (answers.all { it != null }) level = levelFromScore(answers.sumOf { it ?: 0 })
            }
        }
        Text("Niveau retenu", fontWeight = FontWeight.Bold)
        Choices(Level.entries, selected = level, label = { "${it.label} (${it.seconds} s)" }) { level = it }

        BigButton("Enregistrer", enabled = name.isNotBlank()) {
            onSave(Player(initial?.id ?: UUID.randomUUID().toString(), name.trim(), age, level, color))
        }
        if (onDelete != null) SecondaryButton("Supprimer ce joueur", onClick = onDelete)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

/** Rangée de choix exclusifs, en grosses étiquettes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> Choices(options: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            Toggle(label(option), checked = option == selected) { onSelect(option) }
        }
    }
}

@Composable
fun Toggle(text: String, checked: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (checked) scheme.secondary else scheme.surface,
        contentColor = if (checked) scheme.onSecondary else scheme.onSurface,
        border = BorderStroke(1.dp, if (checked) scheme.secondary else scheme.outline),
    ) {
        Text(text, fontSize = 17.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp))
    }
}
