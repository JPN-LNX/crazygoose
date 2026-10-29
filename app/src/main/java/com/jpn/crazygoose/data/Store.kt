package com.jpn.crazygoose.data

import android.content.Context
import com.jpn.crazygoose.game.AgeGroup
import com.jpn.crazygoose.game.Board
import com.jpn.crazygoose.game.CellType
import com.jpn.crazygoose.game.Equipment
import com.jpn.crazygoose.game.GameLength
import com.jpn.crazygoose.game.GameSettings
import com.jpn.crazygoose.game.GameSnapshot
import com.jpn.crazygoose.game.Level
import com.jpn.crazygoose.game.Player
import com.jpn.crazygoose.game.PlayerStats
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Une partie terminée, pour l'historique. */
data class PlayedGame(val endedAt: Long, val winner: String, val results: List<Result>) {
    data class Result(val name: String, val exercises: Int, val activeSeconds: Int)
}

/**
 * Tout ce que l'application conserve, uniquement sur le téléphone : aucune donnée n'est envoyée.
 * Les fichiers sont en JSON dans le dossier privé de l'application.
 */
class Store(context: Context) {
    private val dir = context.filesDir
    private val prefs = context.getSharedPreferences("crazygoose", Context.MODE_PRIVATE)

    var warningAccepted: Boolean
        get() = prefs.getBoolean("warning_accepted", false)
        set(value) = prefs.edit().putBoolean("warning_accepted", value).apply()

    fun loadPlayers(): List<Player> = read("players.json")?.let { json ->
        JSONArray(json).objects().map(::playerFrom)
    } ?: emptyList()

    fun savePlayers(players: List<Player>) =
        write("players.json", JSONArray(players.map(::playerJson)).toString())

    fun loadSavedGame(): GameSnapshot? = read("saved_game.json")?.let { json ->
        runCatching { snapshotFrom(JSONObject(json)) }.getOrNull()
    }

    fun saveGame(snapshot: GameSnapshot) = write("saved_game.json", snapshotJson(snapshot).toString())

    fun clearSavedGame() {
        File(dir, "saved_game.json").delete()
    }

    fun loadHistory(): List<PlayedGame> = read("history.json")?.let { json ->
        JSONArray(json).objects().map { o ->
            PlayedGame(
                o.getLong("endedAt"),
                o.getString("winner"),
                o.getJSONArray("results").objects().map {
                    PlayedGame.Result(it.getString("name"), it.getInt("exercises"), it.getInt("activeSeconds"))
                },
            )
        }
    } ?: emptyList()

    fun addToHistory(game: PlayedGame) {
        val all = listOf(game) + loadHistory()
        val json = JSONArray(all.take(MAX_HISTORY).map { g ->
            JSONObject()
                .put("endedAt", g.endedAt)
                .put("winner", g.winner)
                .put("results", JSONArray(g.results.map {
                    JSONObject().put("name", it.name).put("exercises", it.exercises).put("activeSeconds", it.activeSeconds)
                }))
        })
        write("history.json", json.toString())
    }

    /** Efface profils, partie en cours et historique. */
    fun eraseAll() {
        listOf("players.json", "saved_game.json", "history.json").forEach { File(dir, it).delete() }
    }

    private fun read(name: String): String? = File(dir, name).takeIf { it.exists() }?.readText()

    private fun write(name: String, content: String) {
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(content)
        tmp.renameTo(File(dir, name))
    }

    private fun playerJson(p: Player) = JSONObject()
        .put("id", p.id)
        .put("name", p.name)
        .put("ageGroup", p.ageGroup.name)
        .put("level", p.level.name)
        .put("color", p.colorIndex)

    private fun playerFrom(o: JSONObject) = Player(
        o.getString("id"),
        o.getString("name"),
        AgeGroup.valueOf(o.getString("ageGroup")),
        Level.valueOf(o.getString("level")),
        o.getInt("color"),
    )

    private fun snapshotJson(s: GameSnapshot) = JSONObject()
        .put("board", JSONArray(s.board.cells.map { it.name }))
        .put("players", JSONArray(s.players.map(::playerJson)))
        .put("length", s.settings.length.name)
        .put("equipment", JSONArray(s.settings.equipment.map { it.name }))
        .put("exactFinish", s.settings.exactFinish)
        .put("referee", s.settings.referee)
        .put("positions", JSONArray(s.positions))
        .put("skipNext", JSONArray(s.skipNext))
        .put("current", s.current)
        .put("stats", JSONArray(s.stats.map { JSONObject().put("exercises", it.exercises).put("activeSeconds", it.activeSeconds) }))

    private fun snapshotFrom(o: JSONObject) = GameSnapshot(
        board = Board(o.getJSONArray("board").strings().map(CellType::valueOf)),
        players = o.getJSONArray("players").objects().map(::playerFrom),
        settings = GameSettings(
            length = GameLength.valueOf(o.getString("length")),
            equipment = o.getJSONArray("equipment").strings().map(Equipment::valueOf).toSet(),
            exactFinish = o.getBoolean("exactFinish"),
            referee = o.getBoolean("referee"),
        ),
        positions = o.getJSONArray("positions").let { a -> List(a.length()) { a.getInt(it) } },
        skipNext = o.getJSONArray("skipNext").let { a -> List(a.length()) { a.getBoolean(it) } },
        current = o.getInt("current"),
        stats = o.getJSONArray("stats").objects().map { PlayerStats(it.getInt("exercises"), it.getInt("activeSeconds")) },
    )

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
    private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }

    private companion object {
        const val MAX_HISTORY = 50
    }
}
