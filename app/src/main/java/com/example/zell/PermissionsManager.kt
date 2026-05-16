package com.example.zell

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Manages Camera, Microphone, and Storage permissions
 * Provides composable wrappers for safe feature access
 */

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionWrapper(
    onPermissionGranted: @Composable () -> Unit,
    onPermissionDenied: @Composable () -> Unit = { PermissionDeniedUI("Camera") }
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        onPermissionGranted()
    } else {
        onPermissionDenied()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MicrophonePermissionWrapper(
    onPermissionGranted: @Composable () -> Unit,
    onPermissionDenied: @Composable () -> Unit = { PermissionDeniedUI("Microphone") }
) {
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (!micPermissionState.status.isGranted) {
            micPermissionState.launchPermissionRequest()
        }
    }

    if (micPermissionState.status.isGranted) {
        onPermissionGranted()
    } else {
        onPermissionDenied()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionWrapper(
    onPermissionGranted: @Composable () -> Unit,
    onPermissionDenied: @Composable () -> Unit = { PermissionDeniedUI("Location") }
) {
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    if (locationPermissionState.status.isGranted) {
        onPermissionGranted()
    } else {
        onPermissionDenied()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionWrapper(
    onPermissionGranted: @Composable () -> Unit,
    onPermissionDenied: @Composable () -> Unit = { /* Gracefully handle denied */ }
) {
    val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    LaunchedEffect(Unit) {
        if (!notificationPermissionState.status.isGranted) {
            notificationPermissionState.launchPermissionRequest()
        }
    }

    if (notificationPermissionState.status.isGranted) {
        onPermissionGranted()
    } else {
        onPermissionDenied()
    }
}

@Composable
fun PermissionDeniedUI(permissionName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Permission Required",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "$permissionName Permission Required",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "This feature requires $permissionName permission to work properly. Please enable it in app settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { /* User should go to settings */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Settings")
                }
            }
        }
    }
}

/**
 * Check if permission is granted without requesting
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberHasPermission(permission: String): Boolean {
    val permissionState = rememberPermissionState(permission)
    return permissionState.status.isGranted
}
