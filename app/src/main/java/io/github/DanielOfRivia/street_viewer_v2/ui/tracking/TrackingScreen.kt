package io.github.DanielOfRivia.street_viewer_v2.ui.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.DanielOfRivia.street_viewer_v2.R
import io.github.DanielOfRivia.street_viewer_v2.domain.model.TrackingStopReason
import io.github.DanielOfRivia.street_viewer_v2.ui.theme.Street_viewer_v2Theme

@Composable
fun TrackingScreen(
    uiState: TrackingUiState,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = uiState.pointCount.toString(), style = MaterialTheme.typography.displayLarge)
        Text(
            text = stringResource(R.string.tracking_point_count_label),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(32.dp))

        TrackingActionButton(
            uiState = uiState,
            onStartClick = onStartClick,
            onStopClick = onStopClick,
            onOpenSettingsClick = onOpenSettingsClick,
        )

        if (uiState.permissionState == LocationPermissionState.Unknown ||
            uiState.permissionState == LocationPermissionState.Denied
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.tracking_permission_rationale),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }

        if (!uiState.isTracking && uiState.stopReason != null &&
            uiState.stopReason != TrackingStopReason.USER_REQUESTED
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(uiState.stopReason.messageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TrackingActionButton(
    uiState: TrackingUiState,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
) {
    when {
        uiState.isTracking -> Button(onClick = onStopClick) {
            Text(stringResource(R.string.tracking_stop))
        }
        uiState.permissionState == LocationPermissionState.PermanentlyDenied -> Button(onClick = onOpenSettingsClick) {
            Text(stringResource(R.string.tracking_open_settings))
        }
        else -> Button(onClick = onStartClick) {
            Text(stringResource(R.string.tracking_start))
        }
    }
}

private fun TrackingStopReason.messageRes(): Int = when (this) {
    TrackingStopReason.PERMISSION_MISSING -> R.string.tracking_stop_reason_permission_missing
    TrackingStopReason.START_FOREGROUND_FAILED -> R.string.tracking_stop_reason_start_foreground_failed
    TrackingStopReason.LOCATION_REQUEST_FAILED -> R.string.tracking_stop_reason_location_request_failed
    TrackingStopReason.USER_REQUESTED -> R.string.tracking_stop_reason_user_requested
}

@Preview(showBackground = true)
@Composable
private fun TrackingScreenIdlePreview() {
    Street_viewer_v2Theme {
        TrackingScreen(
            uiState = TrackingUiState(),
            onStartClick = {},
            onStopClick = {},
            onOpenSettingsClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackingScreenTrackingPreview() {
    Street_viewer_v2Theme {
        TrackingScreen(
            uiState = TrackingUiState(
                isTracking = true,
                pointCount = 42,
                permissionState = LocationPermissionState.Granted,
            ),
            onStartClick = {},
            onStopClick = {},
            onOpenSettingsClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackingScreenPermanentlyDeniedPreview() {
    Street_viewer_v2Theme {
        TrackingScreen(
            uiState = TrackingUiState(permissionState = LocationPermissionState.PermanentlyDenied),
            onStartClick = {},
            onStopClick = {},
            onOpenSettingsClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackingScreenStoppedWithReasonPreview() {
    Street_viewer_v2Theme {
        TrackingScreen(
            uiState = TrackingUiState(
                pointCount = 17,
                stopReason = TrackingStopReason.PERMISSION_MISSING,
                permissionState = LocationPermissionState.Denied,
            ),
            onStartClick = {},
            onStopClick = {},
            onOpenSettingsClick = {},
        )
    }
}
