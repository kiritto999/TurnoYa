package com.yperez.turnoya.ui.client

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yperez.turnoya.data.Business
import com.yperez.turnoya.data.User
import com.yperez.turnoya.data.apiCall
import com.yperez.turnoya.ui.Load
import com.yperez.turnoya.ui.components.BusinessAvatar
import com.yperez.turnoya.ui.components.CATEGORIES
import com.yperez.turnoya.ui.components.EmptyState
import com.yperez.turnoya.ui.components.ErrorBox
import com.yperez.turnoya.ui.components.TyMark
import com.yperez.turnoya.ui.map.MapMarker
import com.yperez.turnoya.ui.map.OsmMap
import com.yperez.turnoya.ui.map.rememberOsmMapState
import com.yperez.turnoya.ui.rememberResource
import com.yperez.turnoya.ui.theme.TyColors
import com.yperez.turnoya.util.DEFAULT_LOCATION
import com.yperez.turnoya.util.LatLng
import com.yperez.turnoya.util.currentLocation
import com.yperez.turnoya.util.formatDistance
import com.yperez.turnoya.util.hasLocationPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SHEET_PEEK = 210.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(user: User, onOpenBusiness: (Int) -> Unit, bottomPadding: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mapState = rememberOsmMapState(DEFAULT_LOCATION, 13.0)
    var myLocation by remember { mutableStateOf<LatLng?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var debounced by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(query) { delay(350); debounced = query }

    val origin = myLocation
    val businesses = rememberResource(debounced, category, origin) {
        apiCall { businesses(nombre = debounced.ifBlank { null }, categoria = category, lat = origin?.lat, lng = origin?.lng) }
    }

    suspend fun locate(animate: Boolean) {
        currentLocation(context)?.let {
            myLocation = it
            if (animate) mapState.animateTo(it, 15.0)
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) scope.launch { locate(true) }
    }
    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) locate(true)
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val list = businesses.data.orEmpty()
    // Si no hay GPS, centra el mapa en los negocios encontrados
    LaunchedEffect(list.isNotEmpty(), myLocation) {
        if (myLocation == null) list.firstOrNull { it.hasLocation }?.let { mapState.animateTo(LatLng(it.latitud!!, it.longitud!!), 13.5) }
    }
    val selected = list.firstOrNull { it.id == selectedId }

    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true),
    )

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = SHEET_PEEK + bottomPadding,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = MaterialTheme.colorScheme.background,
        sheetShadowElevation = 16.dp,
        sheetContent = {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                category != null -> category!!
                                debounced.isNotBlank() -> "Resultados"
                                else -> "Cerca de ti"
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            "${list.size} lugares · pide tu turno sin hacer fila",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (businesses.refreshing) androidx.compose.material3.CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
                Spacer(Modifier.height(12.dp))
                when (val s = businesses.state) {
                    is Load.Loading -> Column(Modifier.padding(horizontal = 16.dp)) { repeat(3) { SkeletonCard() } }
                    is Load.Err -> ErrorBox(s.message, onRetry = { businesses.reload() })
                    is Load.Ok -> if (s.data.isEmpty()) {
                        EmptyState(Icons.Default.SearchOff, "No encontramos lugares", "Prueba con otro nombre o categoría.")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding + 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.height(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.75f),
                        ) {
                            items(s.data, key = { it.id }) { b ->
                                BusinessRow(b, highlighted = b.id == selectedId) {
                                    selectedId = b.id
                                    onOpenBusiness(b.id)
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding() * 0)) {
            OsmMap(
                state = mapState,
                modifier = Modifier.fillMaxSize(),
                markers = list.filter { it.hasLocation }.map {
                    MapMarker(it.id, LatLng(it.latitud!!, it.longitud!!), it.nombre, it.categoria, it.turnosEnEspera)
                },
                selectedId = selectedId,
                userLocation = myLocation,
                onMarkerClick = { id ->
                    selectedId = id
                    list.firstOrNull { it.id == id }?.let { mapState.animateTo(LatLng(it.latitud!!, it.longitud!!)) }
                },
                onMapClick = { selectedId = null },
            )

            // Encabezado flotante: saludo + buscador + categorías
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp)) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TyMark(size = 28.dp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Hola, ${user.nombre.substringBefore(' ')}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (myLocation != null) "📍 Tu ubicación actual" else "📍 Activa tu ubicación",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SearchBar(query, { query = it }, Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(10.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CATEGORIES) { c ->
                        val active = category == c.name
                        Row(
                            Modifier
                                .shadow(4.dp, RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .background(if (active) Brush.horizontalGradient(c.colors) else Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)))
                                .clickable { category = if (active) null else c.name }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(c.emoji)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                c.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Tarjeta del negocio seleccionado en el mapa
            AnimatedVisibility(
                visible = selected != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = SHEET_PEEK + bottomPadding + 12.dp, start = 16.dp, end = 16.dp),
            ) {
                selected?.let { SelectedBusinessCard(it, onOpen = { onOpenBusiness(it.id) }, onClose = { selectedId = null }) }
            }

            SmallFloatingActionButton(
                onClick = {
                    if (hasLocationPermission(context)) scope.launch { locate(true) }
                    else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = SHEET_PEEK + bottomPadding + 12.dp + if (selected != null) 120.dp else 0.dp),
            ) { Icon(Icons.Default.MyLocation, "Mi ubicación") }
        }
    }
}

@Composable
private fun SearchBar(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text("¿A dónde vas? Busca un negocio…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                Icon(Icons.Default.Close, "Borrar", modifier = Modifier.clip(CircleShape).clickable { onChange("") })
            }
        }
    }
}

@Composable
fun BusinessRow(b: Business, highlighted: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(if (highlighted) 2.dp else 1.dp, if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BusinessAvatar(b.nombre, b.categoria, 56.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(b.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(b.categoria, formatDistance(b.distanciaKm)).joinToString(" · ").ifBlank { "Negocio" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WaitChip(b.turnosEnEspera ?: 0)
                if (!b.horario.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.AccessTime, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(2.dp))
                    Text(b.horario, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WaitChip(waiting: Int) {
    val (bg, fg, text) = when {
        waiting == 0 -> Triple(Color(0xFFDDF7EA), Color(0xFF0E7A4E), "Sin fila")
        waiting < 5 -> Triple(Color(0xFFD6E8FB), TyColors.BlueDark, "$waiting en fila")
        else -> Triple(Color(0xFFFFE6D1), Color(0xFFB24A00), "$waiting en fila")
    }
    Row(
        Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Groups, null, tint = fg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SelectedBusinessCard(b: Business, onOpen: () -> Unit, onClose: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BusinessAvatar(b.nombre, b.categoria, 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(b.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(b.direccion, formatDistance(b.distanciaKm)).joinToString(" · ").ifBlank { b.categoria ?: "" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(Icons.Default.Close, "Cerrar", Modifier.clip(CircleShape).clickable(onClick = onClose).padding(4.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WaitChip(b.turnosEnEspera ?: 0)
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(14.dp)).background(TyColors.ctaGradient).clickable(onClick = onOpen)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.NearMe, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ver y pedir turno", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SkeletonCard() {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).height(82.dp).clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}
