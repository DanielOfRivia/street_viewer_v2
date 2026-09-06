package io.github.DanielOfRivia.street_viewer_v2.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.DanielOfRivia.street_viewer_v2.R
import io.github.DanielOfRivia.street_viewer_v2.ui.map.MapRoute
import io.github.DanielOfRivia.street_viewer_v2.ui.tracking.TrackingRoute

private enum class AppTab {
    TRACK,
    MAP,
}

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.TRACK) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == AppTab.TRACK,
                    onClick = { selectedTab = AppTab.TRACK },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_track)) },
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.MAP,
                    onClick = { selectedTab = AppTab.MAP },
                    icon = { Icon(Icons.Default.Place, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_map)) },
                )
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        when (selectedTab) {
            AppTab.TRACK -> TrackingRoute(modifier = contentModifier)
            AppTab.MAP -> MapRoute(modifier = contentModifier)
        }
    }
}
