package com.yperez.turnoya.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.yperez.turnoya.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "turnoya_session")

data class Session(val token: String, val user: User)

/** Guarda token, usuario y la URL del servidor (que cambia con el túnel de Cloudflare). */
class SessionStore(private val context: Context) {
    private val gson = Gson()
    private val kToken = stringPreferencesKey("token")
    private val kUser = stringPreferencesKey("user")
    private val kServer = stringPreferencesKey("server_url")
    private val kLastNotif = intPreferencesKey("last_notified_id")

    val session: Flow<Session?> = context.dataStore.data.map { p ->
        val token = p[kToken]
        val user = p[kUser]?.let { runCatching { gson.fromJson(it, User::class.java) }.getOrNull() }
        if (token != null && user != null) Session(token, user) else null
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { it[kServer] ?: BuildConfig.DEFAULT_API_URL }

    suspend fun currentServerUrl(): String = serverUrl.first()
    suspend fun currentSession(): Session? = session.first()

    suspend fun save(token: String, user: User) {
        context.dataStore.edit {
            it[kToken] = token
            it[kUser] = gson.toJson(user)
        }
    }

    suspend fun updateUser(user: User) {
        context.dataStore.edit { it[kUser] = gson.toJson(user) }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(kToken); it.remove(kUser); it.remove(kLastNotif)
        }
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[kServer] = normalizeUrl(url) }
    }

    /** null = aún no se ha revisado nunca en esta sesión. */
    suspend fun lastNotifiedId(): Int? = context.dataStore.data.first()[kLastNotif]
    suspend fun setLastNotifiedId(id: Int) {
        context.dataStore.edit { it[kLastNotif] = id }
    }
    suspend fun resetLastNotified() {
        context.dataStore.edit { it.remove(kLastNotif) }
    }

    companion object {
        fun normalizeUrl(raw: String): String {
            var url = raw.trim()
            if (url.isEmpty()) return BuildConfig.DEFAULT_API_URL
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
            if (!url.endsWith("/")) url += "/"
            return url
        }
    }
}
