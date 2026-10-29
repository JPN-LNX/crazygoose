package com.jpn.crazygoose

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jpn.crazygoose.data.Store
import com.jpn.crazygoose.ui.App
import com.jpn.crazygoose.ui.CrazyGooseTheme
import com.jpn.crazygoose.ui.Speech

class MainActivity : ComponentActivity() {
    private lateinit var speech: Speech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // L'écran reste allumé pendant la partie : le téléphone est souvent posé dans l'herbe.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        speech = Speech(this)
        val store = Store(this)
        setContent {
            CrazyGooseTheme {
                App(store, speech)
            }
        }
    }

    override fun onDestroy() {
        speech.shutdown()
        super.onDestroy()
    }
}
