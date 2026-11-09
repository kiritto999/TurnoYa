package com.yperez.turnoya.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.yperez.turnoya.ui.components.categoryColor
import com.yperez.turnoya.ui.components.categoryEmoji
import com.yperez.turnoya.util.LatLng
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

data class MapMarker(
    val id: Int,
    val position: LatLng,
    val title: String,
    val category: String?,
    val badge: Int? = null, // turnos en espera
)

/** Estado del mapa: permite mover la cámara y leer el centro (para elegir ubicación). */
@Stable
class OsmMapState(val initialCenter: LatLng, val initialZoom: Double) {
    internal var mapView: MapView? = null
    var center by mutableStateOf(initialCenter)
        internal set
    var isMoving by mutableStateOf(false)
        internal set

    fun animateTo(target: LatLng, zoom: Double? = null) {
        val mv = mapView ?: return
        if (zoom != null) mv.controller.animateTo(GeoPoint(target.lat, target.lng), zoom, 600L)
        else mv.controller.animateTo(GeoPoint(target.lat, target.lng))
    }
}

@Composable
fun rememberOsmMapState(center: LatLng, zoom: Double = 15.0) = remember { OsmMapState(center, zoom) }

/**
 * Mapa interactivo de OpenStreetMap (gratis, sin API key) envuelto para Compose.
 * Dibuja pines con el color y emoji de la categoría, y el punto azul del usuario.
 */
@Composable
fun OsmMap(
    state: OsmMapState,
    modifier: Modifier = Modifier,
    markers: List<MapMarker> = emptyList(),
    selectedId: Int? = null,
    userLocation: LatLng? = null,
    onMarkerClick: (Int) -> Unit = {},
    onMapClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val markerClick by rememberUpdatedState(onMarkerClick)
    val mapClick by rememberUpdatedState(onMapClick)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 4.0
            maxZoomLevel = 19.5
            controller.setZoom(state.initialZoom)
            controller.setCenter(GeoPoint(state.initialCenter.lat, state.initialCenter.lng))
            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean { sync(this@apply, state); return false }
                override fun onZoom(event: ZoomEvent?): Boolean { sync(this@apply, state); return false }
            })
        }
    }
    state.mapView = mapView

    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        onDispose {
            lifecycle.removeObserver(obs)
            mapView.onDetach()
            state.mapView = null
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { mv ->
            mv.overlays.clear()
            // Toque en zona vacía del mapa
            mv.overlays.add(object : Overlay() {
                override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
                    mapClick?.invoke(); return false
                }
            })
            userLocation?.let { loc ->
                mv.overlays.add(Marker(mv).apply {
                    position = GeoPoint(loc.lat, loc.lng)
                    icon = userDot(mv.context)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setInfoWindow(null)
                    setOnMarkerClickListener { _, _ -> true }
                })
            }
            markers.sortedBy { it.id == selectedId }.forEach { m ->
                mv.overlays.add(Marker(mv).apply {
                    position = GeoPoint(m.position.lat, m.position.lng)
                    title = m.title
                    icon = pinDrawable(mv.context, m.category, m.badge, selected = m.id == selectedId)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setInfoWindow(null)
                    setOnMarkerClickListener { _, _ -> markerClick(m.id); true }
                })
            }
            mv.invalidate()
        },
    )
}

private fun sync(mv: MapView, state: OsmMapState) {
    val c = mv.mapCenter
    state.center = LatLng(c.latitude, c.longitude)
}

private val pinCache = HashMap<String, BitmapDrawable>()

/** Pin tipo gota con el color de la categoría, emoji al centro y contador de turnos en espera. */
fun pinDrawable(context: Context, category: String?, badge: Int?, selected: Boolean): BitmapDrawable {
    val key = "$category|$badge|$selected"
    pinCache[key]?.let { return it }
    val d = context.resources.displayMetrics.density
    val scale = if (selected) 1.25f else 1f
    val w = (46 * d * scale).toInt()
    val h = (58 * d * scale).toInt()
    val bmp = Bitmap.createBitmap(w + (14 * d).toInt(), h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val color = categoryColor(category).toArgb()
    val r = w / 2f
    val cx = r
    val p = Paint(Paint.ANTI_ALIAS_FLAG)

    // sombra
    p.color = 0x33000000
    c.drawOval(RectF(cx - r * 0.35f, h - 6 * d, cx + r * 0.35f, h.toFloat()), p)
    // gota
    val path = Path().apply {
        addCircle(cx, r, r - 2 * d, Path.Direction.CW)
        moveTo(cx - r * 0.55f, r + r * 0.6f)
        lineTo(cx, h - 4 * d)
        lineTo(cx + r * 0.55f, r + r * 0.6f)
        close()
    }
    p.color = color
    c.drawPath(path, p)
    p.style = Paint.Style.STROKE
    p.strokeWidth = (if (selected) 3.5f else 2.5f) * d
    p.color = android.graphics.Color.WHITE
    c.drawCircle(cx, r, r - 2 * d, p)
    p.style = Paint.Style.FILL
    // círculo interior blanco con emoji
    c.drawCircle(cx, r, r * 0.62f, p)
    val emoji = categoryEmoji(category) ?: "🏪"
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = r * 0.8f; textAlign = Paint.Align.CENTER }
    val fm = tp.fontMetrics
    c.drawText(emoji, cx, r - (fm.ascent + fm.descent) / 2f, tp)
    // contador
    if (badge != null && badge > 0) {
        val br = 9.5f * d * scale
        val bx = w + 14 * d - br - 1 * d
        val by = br + 1 * d
        p.color = 0xFFFF8A1F.toInt()
        c.drawCircle(bx, by, br, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2 * d; p.color = android.graphics.Color.WHITE
        c.drawCircle(bx, by, br, p); p.style = Paint.Style.FILL
        val bt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE; textSize = 11 * d * scale; textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val bfm = bt.fontMetrics
        c.drawText(if (badge > 99) "99+" else badge.toString(), bx, by - (bfm.ascent + bfm.descent) / 2f, bt)
    }
    return BitmapDrawable(context.resources, bmp).also { pinCache[key] = it }
}

private fun userDot(context: Context): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val s = (26 * d).toInt()
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = 0x330A6FCB; c.drawCircle(s / 2f, s / 2f, s / 2f, p)
    p.color = android.graphics.Color.WHITE; c.drawCircle(s / 2f, s / 2f, 8 * d, p)
    p.color = 0xFF0A6FCB.toInt(); c.drawCircle(s / 2f, s / 2f, 6 * d, p)
    return BitmapDrawable(context.resources, bmp)
}
