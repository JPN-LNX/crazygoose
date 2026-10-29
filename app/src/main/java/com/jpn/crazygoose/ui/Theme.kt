package com.jpn.crazygoose.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jpn.crazygoose.game.CellType

// Ambiance sportive et dynamique : orange vif, vert pelouse, bleu ciel, lisibles en plein soleil.
private val Orange = Color(0xFFE2611B)
private val Grass = Color(0xFF2F8A3B)
private val Sky = Color(0xFF2C7FD0)

private val LightColors = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    secondary = Grass,
    onSecondary = Color.White,
    tertiary = Sky,
    background = Color(0xFFF6F7F2),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8ECE0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF27A36),
    onPrimary = Color(0xFF1A0D04),
    secondary = Color(0xFF62C26C),
    tertiary = Color(0xFF6FB2F0),
    background = Color(0xFF151912),
    surface = Color(0xFF1E231A),
    surfaceVariant = Color(0xFF2C3327),
)

/** Couleurs des pions, une par joueur (10 joueurs au maximum). */
val PawnColors = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF8E24AA),
    Color(0xFFFB8C00), Color(0xFF00ACC1), Color(0xFFD81B60), Color(0xFF6D4C41), Color(0xFF546E7A),
)

fun pawnColor(index: Int): Color = PawnColors[index.mod(PawnColors.size)]

fun cellColor(type: CellType): Color = when (type) {
    CellType.DEPART, CellType.ARRIVEE -> Color(0xFF2F8A3B)
    CellType.EXERCICE -> Color(0xFFDDE7D0)
    CellType.OIE -> Color(0xFFFFE082)
    CellType.BONUS -> Color(0xFFA5D6A7)
    CellType.RECUL -> Color(0xFFFFAB91)
    CellType.PRISON -> Color(0xFFB0BEC5)
    CellType.DEFI_COLLECTIF -> Color(0xFFFFCC80)
    CellType.PAUSE_EAU -> Color(0xFF81D4FA)
    CellType.JOKER -> Color(0xFFCE93D8)
    CellType.DUEL -> Color(0xFFEF9A9A)
}

/** Pictogramme court affiché sur la case. */
fun cellSymbol(type: CellType): String = when (type) {
    CellType.DEPART -> "GO"
    CellType.ARRIVEE -> "★"
    CellType.EXERCICE -> ""
    CellType.OIE -> "Oie"
    CellType.BONUS -> "+"
    CellType.RECUL -> "−"
    CellType.PRISON -> "#"
    CellType.DEFI_COLLECTIF -> "Tous"
    CellType.PAUSE_EAU -> "Eau"
    CellType.JOKER -> "?"
    CellType.DUEL -> "VS"
}

@Composable
fun CrazyGooseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
