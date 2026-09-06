package io.github.DanielOfRivia.street_viewer_v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.DanielOfRivia.street_viewer_v2.ui.AppRoot
import io.github.DanielOfRivia.street_viewer_v2.ui.theme.Street_viewer_v2Theme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Street_viewer_v2Theme {
                AppRoot()
            }
        }
    }
}
