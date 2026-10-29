package com.jpn.crazygoose.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Lit les consignes à voix haute avec la synthèse vocale du téléphone (pas de fichiers audio). */
class Speech(context: Context) {
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) engine().language = Locale.FRENCH
    }

    private fun engine() = tts

    fun say(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "consigne")
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() = tts.shutdown()
}
