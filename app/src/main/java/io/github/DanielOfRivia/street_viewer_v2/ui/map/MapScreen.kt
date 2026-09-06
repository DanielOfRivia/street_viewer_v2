package io.github.DanielOfRivia.street_viewer_v2.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import io.github.DanielOfRivia.street_viewer_v2.R
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.ui.theme.Street_viewer_v2Theme

private const val DEFAULT_ZOOM = 15f

@Composable
fun MapRoute(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(uiState = uiState, modifier = modifier)
}

@Composable
fun MapScreen(
    uiState: MapUiState,
    modifier: Modifier = Modifier,
) {
    val newest = uiState.newestPoint
    if (newest == null) {
        EmptyMapState(modifier = modifier.fillMaxSize())
        return
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(newest.toLatLng(), DEFAULT_ZOOM)
    }

    // The initial camera position is already set above via CameraPosition.fromLatLngZoom,
    // which doesn't need the native map to exist yet. CameraUpdateFactory does need it
    // (it's initialized when GoogleMap's underlying MapView attaches), so animating on the
    // very first composition throws "CameraUpdateFactory is not initialized" — only animate
    // for point updates that arrive after the map itself has actually been composed once.
    var hasCenteredOnce by remember { mutableStateOf(false) }
    LaunchedEffect(newest.id) {
        if (hasCenteredOnce) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(newest.toLatLng()))
        } else {
            hasCenteredOnce = true
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
    ) {
        Polyline(points = uiState.points.map { it.toLatLng() })
        Marker(
            state = MarkerState(position = newest.toLatLng()),
            title = stringResource(R.string.map_newest_position_marker_title),
        )
    }
}

@Composable
private fun EmptyMapState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.map_empty_state),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}

private fun LocationPoint.toLatLng() = LatLng(latitude, longitude)

@Preview(showBackground = true)
@Composable
private fun MapScreenEmptyPreview() {
    Street_viewer_v2Theme {
        MapScreen(uiState = MapUiState())
    }
}

@Preview(showBackground = true)
@Composable
private fun MapScreenWithTrackPreview() {
    Street_viewer_v2Theme {
        MapScreen(
            uiState = MapUiState(
                points = listOf(
                    LocationPoint(id = 1, latitude = 43.6532, longitude = -79.3832, timestampMillis = 0L, accuracyMeters = 8f),
                    LocationPoint(id = 2, latitude = 43.6540, longitude = -79.3820, timestampMillis = 30_000L, accuracyMeters = 6f),
                    LocationPoint(id = 3, latitude = 43.6548, longitude = -79.3805, timestampMillis = 60_000L, accuracyMeters = 7f),
                ),
            ),
        )
    }
}
