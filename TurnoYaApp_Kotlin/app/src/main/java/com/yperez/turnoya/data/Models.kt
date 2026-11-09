package com.yperez.turnoya.data

import com.google.gson.annotations.SerializedName

// ---------- Auth ----------
data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(
    val nombre: String,
    val email: String,
    val password: String,
    val rol: String, // "cliente" | "dueño"
)

data class User(
    @SerializedName("id_usuario") val id: Int,
    val nombre: String,
    val email: String,
    val rol: String,
    val estado: String,
) {
    val isOwner get() = rol == Roles.OWNER
    val isEmployee get() = rol == Roles.EMPLOYEE
    val isClient get() = rol == Roles.CLIENT
    val isStaff get() = isOwner || isEmployee || rol == Roles.ADMIN
}

object Roles {
    const val CLIENT = "cliente"
    const val OWNER = "dueño"
    const val EMPLOYEE = "empleado"
    const val ADMIN = "superadmin"
}

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    val user: User,
)

// ---------- Negocios ----------
data class Business(
    @SerializedName("id_negocio") val id: Int,
    @SerializedName("id_dueno") val ownerId: Int,
    val nombre: String,
    @SerializedName("logo_url") val logoUrl: String?,
    val categoria: String?,
    val horario: String?,
    val descripcion: String?,
    val direccion: String?,
    val telefono: String?,
    val latitud: Double?,
    val longitud: Double?,
    val estado: String,
    @SerializedName("distancia_km") val distanciaKm: Double?,
    @SerializedName("turnos_en_espera") val turnosEnEspera: Int?,
    @SerializedName("mi_rol") val miRol: String?,
) {
    val hasLocation get() = latitud != null && longitud != null
}

data class BusinessRequest(
    val nombre: String,
    val categoria: String?,
    val horario: String?,
    val descripcion: String?,
    val direccion: String?,
    val telefono: String?,
    val latitud: Double?,
    val longitud: Double?,
    @SerializedName("logo_url") val logoUrl: String? = null,
)

data class BusinessStats(
    @SerializedName("en_espera") val enEspera: Int,
    val llamados: Int,
    @SerializedName("atendidos_hoy") val atendidosHoy: Int,
    @SerializedName("cancelados_hoy") val canceladosHoy: Int,
    @SerializedName("total_hoy") val totalHoy: Int,
)

// ---------- Sectores y catálogo ----------
data class Sector(
    @SerializedName("id_sector") val id: Int,
    @SerializedName("id_negocio") val businessId: Int,
    val nombre: String,
    val descripcion: String?,
)

data class SectorRequest(val nombre: String, val descripcion: String?)

data class CatalogItem(
    @SerializedName("id_item") val id: Int,
    @SerializedName("id_negocio") val businessId: Int,
    @SerializedName("id_sector") val sectorId: Int?,
    val nombre: String,
    val descripcion: String?,
    val precio: Double?,
    @SerializedName("tiempo_estimado") val tiempoEstimado: Int?,
    val estado: String,
)

data class CatalogItemRequest(
    @SerializedName("id_sector") val sectorId: Int?,
    val nombre: String,
    val descripcion: String?,
    val precio: Double?,
    @SerializedName("tiempo_estimado") val tiempoEstimado: Int?,
)

// ---------- Turnos ----------
data class Turn(
    @SerializedName("id_turno") val id: Int,
    @SerializedName("id_cliente") val clientId: Int,
    @SerializedName("id_negocio") val businessId: Int,
    @SerializedName("id_sector") val sectorId: Int?,
    @SerializedName("id_item") val itemId: Int?,
    @SerializedName("numero_turno") val numero: Int,
    val motivo: String?,
    val estado: String,
    @SerializedName("fecha_solicitud") val fechaSolicitud: String,
    @SerializedName("fecha_atencion") val fechaAtencion: String?,
    @SerializedName("nombre_negocio") val nombreNegocio: String?,
    @SerializedName("nombre_sector") val nombreSector: String?,
    @SerializedName("nombre_item") val nombreItem: String?,
    @SerializedName("nombre_cliente") val nombreCliente: String?,
    val precio: Double?,
    @SerializedName("personas_adelante") val personasAdelante: Int?,
    @SerializedName("tiempo_espera_min") val tiempoEsperaMin: Int?,
) {
    val isActive get() = estado in TurnStates.ACTIVE
}

object TurnStates {
    const val PENDING = "pendiente"
    const val CALLED = "llamado"
    const val SERVING = "atendiendo"
    const val DONE = "atendido"
    const val CANCELLED = "cancelado"
    const val ABSENT = "ausente"
    val ACTIVE = setOf(PENDING, CALLED, SERVING)
}

data class TurnRequest(
    @SerializedName("id_sector") val sectorId: Int?,
    @SerializedName("id_item") val itemId: Int?,
    val motivo: String?,
)

data class TurnStatusRequest(val estado: String)

// ---------- QR ----------
data class QrCode(
    @SerializedName("id_qr") val id: Int,
    @SerializedName("id_negocio") val businessId: Int,
    @SerializedName("id_sector") val sectorId: Int?,
    @SerializedName("codigo_unico") val codigo: String,
    @SerializedName("url_destino") val urlDestino: String,
    @SerializedName("fecha_generacion") val fecha: String,
)

data class QrRequest(@SerializedName("id_sector") val sectorId: Int?)

// ---------- Empleados ----------
data class Employee(
    @SerializedName("id_empleado_negocio") val id: Int,
    @SerializedName("id_usuario") val userId: Int,
    @SerializedName("id_negocio") val businessId: Int,
    @SerializedName("id_sector") val sectorId: Int?,
    val permisos: String?,
    @SerializedName("estado_invitacion") val estadoInvitacion: String,
    val nombre: String?,
    val email: String?,
    @SerializedName("nombre_negocio") val nombreNegocio: String?,
    @SerializedName("categoria_negocio") val categoriaNegocio: String?,
)

data class InviteRequest(
    val email: String,
    @SerializedName("id_sector") val sectorId: Int?,
    val permisos: String?,
)

// ---------- Notificaciones ----------
data class AppNotification(
    @SerializedName("id_notificacion") val id: Int,
    @SerializedName("id_turno") val turnId: Int?,
    @SerializedName("id_negocio") val businessId: Int?,
    val tipo: String,
    val mensaje: String,
    val leido: Boolean,
    @SerializedName("fecha_envio") val fecha: String,
)

data class UnreadCount(@SerializedName("no_leidas") val noLeidas: Int)
