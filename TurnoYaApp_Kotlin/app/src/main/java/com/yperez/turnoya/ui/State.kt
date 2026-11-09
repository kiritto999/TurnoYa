package com.yperez.turnoya.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ok<T>(val data: T) : Load<T>
    data class Err(val message: String) : Load<Nothing>
}

/** Datos remotos de una pantalla con recarga manual o silenciosa (para el seguimiento en vivo). */
@Stable
class Resource<T>(private val scope: CoroutineScope, private val loader: suspend () -> Result<T>) {
    var state: Load<T> by mutableStateOf(Load.Loading)
        private set
    var refreshing by mutableStateOf(false)
        private set
    private var job: Job? = null

    val data: T? get() = (state as? Load.Ok<T>)?.data

    fun reload(silent: Boolean = false) {
        job?.cancel()
        job = scope.launch {
            if (!silent) {
                if (state is Load.Ok) refreshing = true else state = Load.Loading
            }
            loader().fold(
                onSuccess = { state = Load.Ok(it) },
                onFailure = { if (!silent || state !is Load.Ok) state = Load.Err(it.message ?: "Error") },
            )
            refreshing = false
        }
    }

    fun set(value: T) { state = Load.Ok(value) }
}

@Composable
fun <T> rememberResource(vararg keys: Any?, loader: suspend () -> Result<T>): Resource<T> {
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(loader)
    val res = remember(*keys) { Resource(scope) { current() } }
    LaunchedEffect(res) { res.reload() }
    return res
}

/** Repite una acción cada [intervalMs] mientras la pantalla esté visible. */
@Composable
fun PollEffect(intervalMs: Long, vararg keys: Any?, action: suspend () -> Unit) {
    val current by rememberUpdatedState(action)
    LaunchedEffect(*keys) {
        while (true) {
            delay(intervalMs)
            current()
        }
    }
}

val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }
