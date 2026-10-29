package com.jpn.crazygoose.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Lit les consignes à voix haute avec la synthèse vocale du téléphone (pas de fichiers audio). */
class Speech(context: Context) : TextToSpeech.OnInitListener {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts.language = Locale.FRENCH
    }

    fun say(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "consigne")
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() = tts.shutdown()
}
