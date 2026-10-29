package com.jpn.crazygoose.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Échauffement guidé d'environ 3 minutes : 6 mouvements de 30 secondes, tout le monde ensemble. */
private val WARMUP = listOf(
    "Marche sur place" to "Marche sur place en balançant les bras.",
    "Rotations des épaules" to "Fais tourner tes épaules vers l'arrière, puis vers l'avant.",
    "Rotations des bras" to "Bras tendus, fais de grands cercles.",
    "Rotations du bassin" to "Mains sur les hanches, dessine des cercles avec ton bassin.",
    "Montées de genoux" to "Monte un genou puis l'autre, doucement.",
    "Petits sauts" to "Fais de petits sauts sur place, sans forcer.",
)

private const val STEP_SECONDS = 30

@Composable
fun WarmupScreen(speech: Speech, onDone: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) }

    if (!started) {
        ScreenScaffold("Échauffement") {
            Text("Un échauffement de 3 minutes, tous ensemble, avant de commencer ?", fontSize = 18.sp)
            BigButton("Faire l'échauffement") {
                started = true
                speech.say(WARMUP[0].second)
            }
            SecondaryButton("Commencer sans échauffement", onClick = onDone)
        }
        return
    }

    val (name, instruction) = WARMUP[step]
    ScreenScaffold("Échauffement ${step + 1}/${WARMUP.size}") {
        Text(name, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(instruction, fontSize = 20.sp)
        CountdownTimer(STEP_SECONDS, key = step) {
            if (step < WARMUP.lastIndex) {
                step++
                speech.say(WARMUP[step].second)
            } else {
                onDone()
            }
        }
        SecondaryButton("Passer l'échauffement") {
            speech.stop()
            onDone()
        }
    }
}
