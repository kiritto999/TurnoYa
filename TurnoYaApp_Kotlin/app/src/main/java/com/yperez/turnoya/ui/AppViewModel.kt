package com.yperez.turnoya.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yperez.turnoya.TurnoYaApp
import com.yperez.turnoya.data.ApiClient
import com.yperez.turnoya.data.LoginRequest
import com.yperez.turnoya.data.RegisterRequest
import com.yperez.turnoya.data.Session
import com.yperez.turnoya.data.apiCall
import com.yperez.turnoya.notifications.Notifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Unknown : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val session: Session) : SessionState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = (app as TurnoYaApp).sessionStore

    val session: StateFlow<SessionState> = store.session
        .map { if (it == null) SessionState.LoggedOut else SessionState.LoggedIn(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Unknown)

    val serverUrl: StateFlow<String> = store.serverUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _unread = MutableStateFlow(0)
    val unread = _unread.asStateFlow()

    /** Código QR escaneado o recibido por enlace que falta abrir (si no había sesión). */
    private val _pendingQr = MutableStateFlow<String?>(null)
    val pendingQr = _pendingQr.asStateFlow()

    init {
        viewModelScope.launch { ApiClient.unauthorized.collect { logout() } }
        viewModelScope.launch {
            // Refresca el usuario al abrir (su rol puede haber cambiado al aceptar una invitación)
            val s = store.session.first() ?: return@launch
            ApiClient.token = s.token
            ApiClient.baseUrl = store.currentServerUrl()
            apiCall { me() }.onSuccess { store.updateUser(it) }
        }
        // Mientras la app está abierta revisa avisos cada 20 s (en segundo plano lo hace WorkManager)
        viewModelScope.launch {
            while (true) {
                if (session.value is SessionState.LoggedIn) {
                    apiCall { unreadCount() }.onSuccess { _unread.value = it.noLeidas }
                    Notifier.checkNow(getApplication())
                }
                delay(20_000)
            }
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        ApiClient.baseUrl = store.currentServerUrl()
        return apiCall { login(LoginRequest(email.trim(), password)) }.map {
            ApiClient.token = it.accessToken
            store.save(it.accessToken, it.user)
            store.resetLastNotified()
        }
    }

    suspend fun register(nombre: String, email: String, password: String, rol: String): Result<Unit> {
        ApiClient.baseUrl = store.currentServerUrl()
        return apiCall { register(RegisterRequest(nombre.trim(), email.trim(), password, rol)) }
            .fold({ login(email, password) }, { Result.failure(it) })
    }

    fun refreshUser() = viewModelScope.launch {
        apiCall { me() }.onSuccess { store.updateUser(it) }
    }

    fun refreshUnread() = viewModelScope.launch {
        apiCall { unreadCount() }.onSuccess { _unread.value = it.noLeidas }
    }

    fun logout() = viewModelScope.launch {
        store.clear()
        ApiClient.token = null
        _unread.value = 0
    }

    fun setServerUrl(url: String) = viewModelScope.launch { store.setServerUrl(url) }

    fun setPendingQr(code: String?) { _pendingQr.value = code }
}
