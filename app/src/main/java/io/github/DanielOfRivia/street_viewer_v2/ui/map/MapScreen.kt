package io.github.DanielOfRivia.street_viewer_v2.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LatLon
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedStreetRun
import io.github.DanielOfRivia.street_viewer_v2.ui.theme.Street_viewer_v2Theme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val DEFAULT_ZOOM = 15f
private val VisitedStreetColor = Color(0xFF00C853)

@Composable
fun MapRoute(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(uiState = uiState, onDateSelected = viewModel::onDateSelected, modifier = modifier)
}

@Composable
fun MapScreen(
    uiState: MapUiState,
    onDateSelected: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.newestPoint == null) {
            EmptyMapState(isToday = uiState.selectedDate == LocalDate.now(), modifier = Modifier.fillMaxSize())
        } else {
            TrackMap(uiState = uiState, modifier = Modifier.fillMaxSize())
        }

        DateSelectorBar(
            selectedDate = uiState.selectedDate,
            isLoading = uiState.isLoading,
            onDateSelected = onDateSelected,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
        )

        uiState.errorMessage?.let { message ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(R.string.map_history_load_failed, message),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TrackMap(uiState: MapUiState, modifier: Modifier = Modifier) {
    val newest = requireNotNull(uiState.newestPoint)

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
        modifier = modifier,
        cameraPositionState = cameraPositionState,
    ) {
        Polyline(points = uiState.points.map { it.toLatLng() })
        uiState.visitedStreetRuns.forEach { run ->
            Polyline(
                points = run.points.map { it.toLatLng() },
                color = VisitedStreetColor,
                width = 12f,
            )
        }
        Marker(
            state = MarkerState(position = newest.toLatLng()),
            title = stringResource(R.string.map_newest_position_marker_title),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectorBar(
    selectedDate: LocalDate,
    isLoading: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 4.dp,
    ) {
        val todayLabel = stringResource(R.string.map_date_today)
        Row(
            modifier = Modifier
                .clickable { showPicker = true }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = selectedDate.toDisplayLabel(todayLabel), style = MaterialTheme.typography.bodyMedium)
            if (isLoading) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toUtcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) {
                    Text(stringResource(R.string.map_date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.map_date_picker_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun LocalDate.toDisplayLabel(todayLabel: String): String =
    if (this == LocalDate.now()) {
        todayLabel
    } else {
        format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

@Composable
private fun EmptyMapState(isToday: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(
                if (isToday) R.string.map_empty_state else R.string.map_empty_state_historical,
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}

private fun LocationPoint.toLatLng() = LatLng(latitude, longitude)
private fun LatLon.toLatLng() = LatLng(latitude, longitude)

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
                visitedStreetRuns = listOf(
                    VisitedStreetRun(
                        points = listOf(
                            LatLon(43.6531, -79.3833),
                            LatLon(43.6541, -79.3819),
                        ),
                    ),
                ),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MapScreenHistoricalErrorPreview() {
    Street_viewer_v2Theme {
        MapScreen(
            uiState = MapUiState(
                selectedDate = LocalDate.now().minusDays(3),
                errorMessage = "Server returned HTTP 500",
            ),
        )
    }
}
