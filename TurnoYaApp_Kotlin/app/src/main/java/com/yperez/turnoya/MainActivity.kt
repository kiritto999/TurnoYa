package com.yperez.turnoya

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yperez.turnoya.ui.AppRoot
import com.yperez.turnoya.ui.AppViewModel
import com.yperez.turnoya.ui.SessionState
import com.yperez.turnoya.ui.parseQrCode
import com.yperez.turnoya.ui.theme.TurnoYaTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private var openNotifications by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { vm.session.value is SessionState.Unknown }
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            TurnoYaTheme {
                AppRoot(vm, openNotifications)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** turnoya://qr/{codigo} desde la cámara del teléfono, o toque en una notificación. */
    private fun handleIntent(intent: Intent?) {
        parseQrCode(intent?.dataString)?.let { vm.setPendingQr(it) }
        openNotifications = intent?.getStringExtra(EXTRA_OPEN) == OPEN_NOTIFICATIONS
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_NOTIFICATIONS = "notifications"
    }
}
