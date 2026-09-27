package io.github.DanielOfRivia.street_viewer_v2.ui.tracking

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.DanielOfRivia.street_viewer_v2.service.TrackingService

@Composable
fun TrackingRoute(
    modifier: Modifier = Modifier,
    viewModel: TrackingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasRequestedLocationPermission by rememberSaveable { mutableStateOf(false) }

    fun refreshPermissionState() {
        viewModel.onPermissionStateChanged(
            when {
                context.hasLocationPermission() -> LocationPermissionState.Granted
                hasRequestedLocationPermission && context.isLocationPermissionPermanentlyDenied() ->
                    LocationPermissionState.PermanentlyDenied
                hasRequestedLocationPermission -> LocationPermissionState.Denied
                else -> LocationPermissionState.Unknown
            },
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Best effort: the foreground service can run without a visible notification,
        // so we start tracking regardless of whether this was granted.
        TrackingService.start(context)
    }

    val trackingPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (Manifest.permission.ACCESS_FINE_LOCATION in result) hasRequestedLocationPermission = true
        refreshPermissionState()
        // Only location decides whether tracking can start -- activity recognition just lets it
        // switch GPS off while stationary, so tracking still starts if that one was denied.
        if (context.hasLocationPermission()) {
            requestNotificationPermissionThenStart(context, notificationPermissionLauncher)
        }
    }

    LaunchedEffect(Unit) {
        refreshPermissionState()
        // Only auto-resumes when permission is already granted -- deliberately doesn't pop
        // a permission request on a bare app open, which would be surprising if the user
        // opened the app for an unrelated reason. If permission was lost, the screen just
        // shows its normal Start button for the user to tap through the request flow.
        if (context.hasLocationPermission() && viewModel.shouldAutoResumeTracking()) {
            requestNotificationPermissionThenStart(context, notificationPermissionLauncher)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissionState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    TrackingScreen(
        uiState = uiState,
        onStartClick = {
            val missing = context.missingTrackingPermissions()
            if (missing.isEmpty()) {
                requestNotificationPermissionThenStart(context, notificationPermissionLauncher)
            } else {
                trackingPermissionLauncher.launch(missing)
            }
        },
        onStopClick = { TrackingService.stop(context) },
        onOpenSettingsClick = { context.startActivity(appSettingsIntent(context)) },
        onSyncClick = viewModel::onSyncClick,
        modifier = modifier,
    )
}

private fun requestNotificationPermissionThenStart(
    context: Context,
    notificationPermissionLauncher: ActivityResultLauncher<String>,
) {
    val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
    if (needsNotificationPermission) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        TrackingService.start(context)
    }
}

private fun Context.hasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

// Activity recognition is asked for here, alongside location, even when location is already
// granted -- otherwise anyone who granted location before it was added would never be asked.
// Once the user has permanently denied it, the system returns immediately without a dialog.
private fun Context.missingTrackingPermissions(): Array<String> = buildList {
    if (!hasLocationPermission()) {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    val activityRecognition = ContextCompat.checkSelfPermission(this@missingTrackingPermissions, Manifest.permission.ACTIVITY_RECOGNITION)
    if (activityRecognition != PackageManager.PERMISSION_GRANTED) {
        add(Manifest.permission.ACTIVITY_RECOGNITION)
    }
}.toTypedArray()

private fun Context.isLocationPermissionPermanentlyDenied(): Boolean {
    val activity = findActivity() ?: return false
    val fineRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.ACCESS_FINE_LOCATION,
    )
    val coarseRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    return !fineRationale && !coarseRationale
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun appSettingsIntent(context: Context) = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", context.packageName, null),
)
