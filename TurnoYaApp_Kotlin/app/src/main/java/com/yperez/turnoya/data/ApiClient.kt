package com.yperez.turnoya.data

import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP único. La URL del servidor y el token se leen en cada petición, así se puede
 * cambiar el servidor (p. ej. nueva URL del túnel) sin reiniciar la app.
 */
object ApiClient {
    @Volatile var baseUrl: String = "http://localhost/"
    @Volatile var token: String? = null

    private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emite cuando el servidor responde 401 (token vencido): la app cierra sesión. */
    val unauthorized = _unauthorized.asSharedFlow()

    private val hostRewrite = Interceptor { chain ->
        val req = chain.request()
        val target = baseUrl.toHttpUrlOrNull()
        val newUrl = if (target != null) {
            val prefix = target.encodedPath.trimEnd('/')
            req.url.newBuilder()
                .scheme(target.scheme)
                .host(target.host)
                .port(target.port)
                .encodedPath(prefix + req.url.encodedPath)
                .build()
        } else req.url
        val builder = req.newBuilder().url(newUrl)
        token?.let { builder.header("Authorization", "Bearer $it") }
        val response = chain.proceed(builder.build())
        if (response.code == 401 && token != null) _unauthorized.tryEmit(Unit)
        response
    }

    private val http = OkHttpClient.Builder()
        .addInterceptor(hostRewrite)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val api: TurnoYaApi = Retrofit.Builder()
        .baseUrl("http://localhost/")
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TurnoYaApi::class.java)
}

/** Ejecuta una llamada y convierte cualquier error en un mensaje entendible en español. */
suspend fun <T> apiCall(block: suspend TurnoYaApi.() -> T): Result<T> = try {
    Result.success(ApiClient.api.block())
} catch (e: HttpException) {
    Result.failure(ApiError(parseDetail(e), e.code()))
} catch (e: UnknownHostException) {
    Result.failure(ApiError("No se encuentra el servidor. Revisa tu conexión o la URL del servidor.", 0))
} catch (e: SocketTimeoutException) {
    Result.failure(ApiError("El servidor tardó demasiado en responder. Intenta de nuevo.", 0))
} catch (e: IOException) {
    Result.failure(ApiError("Sin conexión con el servidor. ¿Está encendido?", 0))
} catch (e: Exception) {
    Result.failure(ApiError(e.message ?: "Error inesperado", -1))
}

class ApiError(message: String, val code: Int) : Exception(message)

private fun parseDetail(e: HttpException): String {
    val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val fallback = when (e.code()) {
        401 -> "Tu sesión expiró. Ingresa de nuevo."
        403 -> "No tienes permiso para hacer esto."
        404 -> "No encontrado."
        409 -> "Conflicto con el estado actual."
        in 500..599 -> "El servidor tuvo un problema. Intenta más tarde."
        else -> "Error ${e.code()}"
    }
    if (body.isNullOrBlank()) return fallback
    return runCatching {
        val detail = JsonParser.parseString(body).asJsonObject.get("detail")
        when {
            detail == null -> fallback
            detail.isJsonPrimitive -> detail.asString
            detail.isJsonArray -> detail.asJsonArray.joinToString("\n") { item ->
                val o = item.asJsonObject
                val field = o.getAsJsonArray("loc")?.lastOrNull()?.asString
                val msg = o.get("msg")?.asString ?: ""
                friendlyValidation(field, msg)
            }
            else -> fallback
        }
    }.getOrDefault(fallback)
}

private fun friendlyValidation(field: String?, msg: String): String {
    val name = when (field) {
        "email" -> "Correo"
        "password" -> "Contraseña"
        "nombre" -> "Nombre"
        "precio" -> "Precio"
        else -> field?.replaceFirstChar { it.uppercase() } ?: "Dato"
    }
    val text = when {
        "valid email" in msg -> "no es un correo válido"
        "at least" in msg -> "es demasiado corto"
        "at most" in msg -> "es demasiado largo"
        else -> msg
    }
    return "$name: $text"
}
