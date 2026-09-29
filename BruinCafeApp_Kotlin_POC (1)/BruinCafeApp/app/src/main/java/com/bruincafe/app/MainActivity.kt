package com.bruincafe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bruincafe.app.ui.navigation.BruinCafeNavGraph
import com.bruincafe.app.ui.theme.BruinCafeTheme

/**
 * Single-Activity entry point. All UI lives in Compose from here down --
 * the direct equivalent of an `@main App` / `WindowGroup { ContentView() }`
 * in a SwiftUI app.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BruinCafeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BruinCafeNavGraph()
                }
            }
        }
    }
}
