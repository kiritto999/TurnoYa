package com.yperez.turnoya.ui.components

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Íconos claros en la barra de estado mientras la pantalla tenga un encabezado oscuro. */
@Composable
fun LightStatusBarIcons(enabled: Boolean = true) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(enabled) {
        var ctx = view.context
        while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
        val window = (ctx as? Activity)?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previous = controller.isAppearanceLightStatusBars
            if (enabled) controller.isAppearanceLightStatusBars = false
            onDispose { controller.isAppearanceLightStatusBars = previous }
        }
    }
}
