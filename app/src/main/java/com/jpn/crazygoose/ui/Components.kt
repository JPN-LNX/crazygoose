package com.jpn.crazygoose.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jpn.crazygoose.game.Player
import kotlinx.coroutines.delay

/** Écran simple : titre, contenu défilant, bouton retour facultatif. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) TextButton(onClick = onBack) { Text("‹ Retour", fontSize = 18.sp) }
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            content()
        }
    }
}

/** Gros bouton, utilisable avec des mains occupées ou de loin. */
@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) { Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Text(text, fontSize = 18.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun Pawn(player: Player, size: Int = 28) {
    Box(
        modifier = Modifier.size(size.dp).background(pawnColor(player.colorIndex), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            player.name.take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.5).sp,
        )
    }
}

/**
 * Compte à rebours en très gros chiffres. Un bip retentit à la fin.
 * [key] relance le minuteur quand il change.
 */
@Composable
fun CountdownTimer(seconds: Int, key: Any, running: Boolean = true, onFinished: () -> Unit = {}) {
    var left by remember(key) { mutableIntStateOf(seconds) }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    LaunchedEffect(key, running) {
        while (running && left > 0) {
            delay(1000)
            left--
        }
        if (left == 0) {
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
            onFinished()
        }
    }
    Text(
        text = if (left > 0) "%d:%02d".format(left / 60, left % 60) else "Terminé !",
        fontSize = 88.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (left in 1..5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp).padding(vertical = 8.dp),
    )
}
