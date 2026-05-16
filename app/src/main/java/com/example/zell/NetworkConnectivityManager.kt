package com.example.zell

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * Holds the single source of truth for network state across the whole app.
 * Shared at the NavHost level so every screen reacts to the same state.
 */
class NetworkViewModel : ViewModel() {
    private val _isConnected = mutableStateOf(true)
    val isConnected: State<Boolean> = _isConnected

    // Track whether we have *ever* gone offline this session so we can show
    // the "Back online!" banner when the connection is restored.
    private var _wentOffline = false

    // Drives the short-lived "Back online!" green banner.
    private val _showRestoredBanner = mutableStateOf(false)
    val showRestoredBanner: State<Boolean> = _showRestoredBanner

    fun updateConnectionState(connected: Boolean) {
        val wasConnected = _isConnected.value
        _isConnected.value = connected

        when {
            !connected -> {
                // Just lost connection — record that we went offline
                _wentOffline = true
                _showRestoredBanner.value = false
            }
            connected && _wentOffline && !wasConnected -> {
                // Connection just came back after being offline — show success toast
                _showRestoredBanner.value = true
            }
        }
    }

    /** Called by OfflineBanner after the "Back online!" banner has auto-dismissed. */
    fun onRestoredBannerDismissed() {
        _showRestoredBanner.value = false
        _wentOffline = false
    }
}

// ── Connectivity Manager ──────────────────────────────────────────────────────

/**
 * Wraps [ConnectivityManager] callbacks into observable Compose state.
 *
 * Lifecycle: call [startMonitoring] once in a [LaunchedEffect] and pair with
 * [stopMonitoring] in the [DisposableEffect] cleanup.
 */
class NetworkConnectivityManager(private val context: Context) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = mutableStateOf(true)
    val isConnected: State<Boolean> = _isConnected

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            _isConnected.value = true
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            _isConnected.value = false
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, caps)
            _isConnected.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    fun startMonitoring() {
        runCatching { cm.registerDefaultNetworkCallback(callback) }
    }

    fun stopMonitoring() {
        runCatching { cm.unregisterNetworkCallback(callback) }
    }

    fun isOnline(): Boolean {
        val net  = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

// ── Offline Banner ────────────────────────────────────────────────────────────

/**
 * System-level connectivity banner — floats at the top of the entire app.
 *
 * Two states:
 *  1. **Offline** (red)  — slides in from the top while there's no internet.
 *  2. **Restored** (green) — slides in briefly when connection returns, then
 *     auto-dismisses after [RESTORED_DURATION_MS] ms.
 *
 * Place this inside the root [Box] in MainActivity (after the [NavHost]) so it
 * overlays every screen automatically. No changes needed in individual screens.
 *
 * @param isConnected       True when the device has internet access.
 * @param showRestoredBanner True for the brief "Back online" state.
 * @param onRestoredDismissed Callback after the restored banner auto-hides.
 */
@Composable
fun OfflineBanner(
    isConnected: Boolean,
    showRestoredBanner: Boolean = false,
    onRestoredDismissed: () -> Unit = {}
) {
    // Auto-dismiss the "Back online" banner after 2.5 s
    LaunchedEffect(showRestoredBanner) {
        if (showRestoredBanner) {
            delay(RESTORED_DURATION_MS)
            onRestoredDismissed()
        }
    }

    // Slide down from the very top of the screen
    val enterAnim  = slideInVertically(tween(300)) { -it } + fadeIn(tween(300))
    val exitAnim   = slideOutVertically(tween(250)) { -it } + fadeOut(tween(250))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()          // sit below the system status bar
    ) {
        // ── Offline (red) ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = !isConnected,
            enter   = enterAnim,
            exit    = exitAnim
        ) {
            BannerRow(
                icon    = Icons.Default.CloudOff,
                message = "No internet connection",
                color   = OfflineRed
            )
        }

        // ── Back online (green) ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = showRestoredBanner && isConnected,
            enter   = enterAnim,
            exit    = exitAnim
        ) {
            BannerRow(
                icon    = Icons.Default.Wifi,
                message = "Back online! 🎉",
                color   = OnlineGreen
            )
        }
    }
}

@Composable
private fun BannerRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(
            text       = message,
            color      = Color.White,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private val OfflineRed  = Color(0xFFD32F2F)
private val OnlineGreen = Color(0xFF2E7D32)
private const val RESTORED_DURATION_MS = 2500L

// ── ConnectivityAwareScreen ───────────────────────────────────────────────────

/**
 * Wraps a screen so it shows a full-page offline state instead of a broken UI.
 *
 * Use this for screens where there is literally nothing to show offline
 * (e.g. Search, Compose). For screens like Chat that can show cached messages,
 * just use [OfflineBanner] and let the user read what's already loaded.
 *
 * @param isConnected   True when internet is available.
 * @param onRetry       Called when the user taps "Retry".
 * @param content       The normal screen content shown when online.
 */
@Composable
fun ConnectivityAwareScreen(
    isConnected: Boolean,
    onRetry: () -> Unit = {},
    content: @Composable () -> Unit
) {
    if (isConnected) {
        content()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = "Offline",
                    modifier = Modifier.size(72.dp),
                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "You're Offline",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Check your internet connection and try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = onRetry,
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry")
                }
            }
        }
    }
}
