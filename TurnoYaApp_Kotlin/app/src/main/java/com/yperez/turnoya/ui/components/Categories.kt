package com.yperez.turnoya.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Categorías sugeridas (RF11). El negocio puede escribir otra al registrarse. */
data class Category(val name: String, val emoji: String, val colors: List<Color>)

val CATEGORIES = listOf(
    Category("Comida", "🍔", listOf(Color(0xFFE5620F), Color(0xFFFF8A1F))),
    Category("Peluquerías", "💈", listOf(Color(0xFF7B3FE4), Color(0xFFB07CFF))),
    Category("Mercados", "🛒", listOf(Color(0xFF2FB57A), Color(0xFF79DEAC))),
    Category("Salud", "🩺", listOf(Color(0xFFE5484D), Color(0xFFFF8A8D))),
    Category("Bancos", "🏦", listOf(Color(0xFF0753A0), Color(0xFF2F9BFF))),
    Category("Registradurías", "🪪", listOf(Color(0xFF0E1630), Color(0xFF34466F))),
    Category("Belleza", "💅", listOf(Color(0xFFD63384), Color(0xFFFF7EB9))),
    Category("Talleres", "🔧", listOf(Color(0xFF5B6B8C), Color(0xFF94A3C2))),
    Category("Trámites", "📄", listOf(Color(0xFF0A6FCB), Color(0xFF47E8FF))),
    Category("Otros", "🏪", listOf(Color(0xFF0A6FCB), Color(0xFF79DEAC))),
)

private fun findCategory(name: String?): Category? {
    if (name.isNullOrBlank()) return null
    val n = name.trim().lowercase()
    return CATEGORIES.firstOrNull { it.name.lowercase() == n }
        ?: CATEGORIES.firstOrNull { n.startsWith(it.name.lowercase().take(5)) || it.name.lowercase().startsWith(n.take(5)) }
}

fun categoryEmoji(name: String?): String? = findCategory(name)?.emoji

fun categoryBrush(name: String?): Brush =
    Brush.linearGradient(findCategory(name)?.colors ?: listOf(Color(0xFF0A6FCB), Color(0xFF79DEAC)))

fun categoryColor(name: String?): Color = findCategory(name)?.colors?.first() ?: Color(0xFF0A6FCB)
