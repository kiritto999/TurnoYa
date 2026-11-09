package com.yperez.turnoya.util

import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val esCO = Locale.Builder().setLanguage("es").setRegion("CO").build()

fun formatPrice(value: Double?): String {
    if (value == null) return "Precio a convenir"
    val nf = NumberFormat.getCurrencyInstance(esCO)
    nf.maximumFractionDigits = 0
    return nf.format(value)
}

fun formatDistance(km: Double?): String? = when {
    km == null -> null
    km < 1 -> "${(km * 1000).toInt()} m"
    else -> String.format(esCO, "%.1f km", km)
}

fun formatWait(minutes: Int?): String = when {
    minutes == null -> "—"
    minutes <= 0 -> "Ya casi"
    minutes < 60 -> "~$minutes min"
    else -> "~${minutes / 60} h ${minutes % 60} min"
}

/** Las fechas llegan en UTC sin zona ("2026-11-09T01:03:31"). */
fun parseServerDate(raw: String?): LocalDateTime? = runCatching {
    LocalDateTime.parse(raw!!.take(19)).atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
}.getOrNull()

fun formatDateTime(raw: String?): String {
    val dt = parseServerDate(raw) ?: return ""
    return dt.format(DateTimeFormatter.ofPattern("d MMM, h:mm a", esCO))
}

fun timeAgo(raw: String?): String {
    val dt = parseServerDate(raw) ?: return ""
    val d = Duration.between(dt, LocalDateTime.now())
    return when {
        d.toMinutes() < 1 -> "Ahora"
        d.toMinutes() < 60 -> "Hace ${d.toMinutes()} min"
        d.toHours() < 24 -> "Hace ${d.toHours()} h"
        d.toDays() < 7 -> "Hace ${d.toDays()} d"
        else -> formatDateTime(raw)
    }
}

fun turnStateLabel(estado: String): String = when (estado) {
    "pendiente" -> "En espera"
    "llamado" -> "¡Es tu turno!"
    "atendiendo" -> "Atendiendo"
    "atendido" -> "Atendido"
    "cancelado" -> "Cancelado"
    "ausente" -> "Ausente"
    else -> estado.replaceFirstChar { it.uppercase() }
}
