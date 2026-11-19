package com.bruincafe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.bruincafe.app.data.AppStorage
import com.bruincafe.app.ui.CafeViewModel
import com.bruincafe.app.ui.navigation.BruinCafeNavGraph
import com.bruincafe.app.ui.theme.BruinCafeTheme

/**
 * Single-Activity entry point (SwiftUI analog: `@main App` + `WindowGroup`).
 * The ViewModel lives here so the THEME can react to the customer's accessibility
 * choices (high contrast, text size) and restyle the whole app instantly.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: CafeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before the ViewModel is created: it loads order history and settings from storage.
        AppStorage.init(applicationContext)
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsState()
            BruinCafeTheme(
                highContrast = state.accessibility.highContrast,
                textScale = state.accessibility.textScale
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BruinCafeNavGraph(viewModel)
                }
            }
        }
    }
}
