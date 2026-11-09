package com.yperez.turnoya

import android.app.Application
import com.yperez.turnoya.data.ApiClient
import com.yperez.turnoya.data.SessionStore
import com.yperez.turnoya.notifications.Notifier
import com.yperez.turnoya.notifications.NotificationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.osmdroid.config.Configuration

class TurnoYaApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var sessionStore: SessionStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionStore = SessionStore(this)

        // OpenStreetMap exige identificar la app; la caché de mapas va en el almacenamiento interno
        Configuration.getInstance().apply {
            userAgentValue = "TurnoYa/${BuildConfig.VERSION_NAME} (Android)"
            osmdroidBasePath = filesDir.resolve("osmdroid")
            osmdroidTileCache = cacheDir.resolve("osmdroid-tiles")
        }

        // Mantiene el cliente HTTP sincronizado con la sesión y la URL guardadas
        sessionStore.serverUrl.onEach { ApiClient.baseUrl = it }.launchIn(appScope)
        sessionStore.session.onEach { ApiClient.token = it?.token }.launchIn(appScope)

        Notifier.createChannel(this)
        NotificationWorker.schedule(this)
    }

    companion object {
        lateinit var instance: TurnoYaApp
            private set
    }
}
