package com.yperez.turnoya.ui.owner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yperez.turnoya.data.Business
import com.yperez.turnoya.data.BusinessRequest
import com.yperez.turnoya.data.User
import com.yperez.turnoya.data.apiCall
import com.yperez.turnoya.ui.Load
import com.yperez.turnoya.ui.LocalSnackbar
import com.yperez.turnoya.ui.PollEffect
import com.yperez.turnoya.ui.client.WaitChip
import com.yperez.turnoya.ui.components.BusinessAvatar
import com.yperez.turnoya.ui.components.CATEGORIES
import com.yperez.turnoya.ui.components.EmptyState
import com.yperez.turnoya.ui.components.ErrorBox
import com.yperez.turnoya.ui.components.LoadingBox
import com.yperez.turnoya.ui.components.SectionTitle
import com.yperez.turnoya.ui.components.TyButton
import com.yperez.turnoya.ui.components.TyLogo
import com.yperez.turnoya.ui.components.TyTextField
import com.yperez.turnoya.ui.components.TyTopBar
import com.yperez.turnoya.ui.map.OsmMap
import com.yperez.turnoya.ui.map.pinDrawable
import com.yperez.turnoya.ui.map.rememberOsmMapState
import com.yperez.turnoya.ui.rememberResource
import com.yperez.turnoya.ui.theme.TyColors
import com.yperez.turnoya.util.DEFAULT_LOCATION
import com.yperez.turnoya.util.LatLng
import com.yperez.turnoya.util.currentLocation
import com.yperez.turnoya.util.hasLocationPermission
import kotlinx.coroutines.launch

/** Inicio para dueños y empleados: sus negocios con el estado de la fila en vivo. */
@Composable
fun PanelScreen(user: User, onManage: (Int) -> Unit, onCreate: () -> Unit, bottomPadding: androidx.compose.ui.unit.Dp) {
    val res = rememberResource("mine") { apiCall { myBusinesses() } }
    PollEffect(10_000) { res.reload(silent = true) }
    com.yperez.turnoya.ui.components.LightStatusBarIcons()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(contentPadding = PaddingValues(bottom = bottomPadding + 96.dp)) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(TyColors.headerGradient).statusBarsPadding().padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    TyLogo(dark = true, height = 52.dp)
                    Spacer(Modifier.height(16.dp))
                    Text("Hola, ${user.nombre.substringBefore(' ')} 👋", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (user.isOwner) "Así va la fila de tus negocios hoy" else "Negocios donde atiendes turnos",
                        color = Color.White.copy(alpha = 0.75f),
                    )
                    Spacer(Modifier.height(16.dp))
                    val list = res.data.orEmpty()
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeaderStat("${list.sumOf { it.turnosEnEspera ?: 0 }}", "en fila ahora", Modifier.weight(1f))
                        HeaderStat("${list.size}", if (list.size == 1) "negocio" else "negocios", Modifier.weight(1f))
                    }
                }
            }
            when (val s = res.state) {
                is Load.Loading -> item { Box(Modifier.height(240.dp)) { LoadingBox() } }
                is Load.Err -> item { ErrorBox(s.message, onRetry = { res.reload() }) }
                is Load.Ok -> if (s.data.isEmpty()) {
                    item {
                        if (user.isOwner) {
                            EmptyState(Icons.Default.AddBusiness, "Registra tu primer negocio", "Ubícalo en el mapa, arma tu catálogo y empieza a recibir turnos.") {
                                TyButton("Crear negocio", onCreate, icon = Icons.Default.Add, modifier = Modifier.fillMaxWidth())
                            }
                        } else {
                            EmptyState(Icons.Default.Storefront, "Aún no atiendes ningún negocio", "Cuando un dueño te invite con tu correo, acepta la invitación en tu Perfil.")
                        }
                    }
                } else {
                    item { SectionTitle(if (user.isOwner) "Mis negocios" else "Donde trabajo", Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) }
                    items(s.data, key = { it.id }) { b -> PanelBusinessCard(b) { onManage(b.id) } }
                }
            }
        }
        if (user.isOwner && res.data?.isNotEmpty() == true) {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Default.AddBusiness, null) },
                text = { Text("Nuevo negocio") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = bottomPadding + 36.dp),
            )
        }
    }
}

@Composable
private fun HeaderStat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.1f)).padding(14.dp)) {
        Text(value, color = Color.White, style = MaterialTheme.typography.headlineMedium)
        Text(label, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PanelBusinessCard(b: Business, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BusinessAvatar(b.nombre, b.categoria, 56.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(b.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(b.categoria, if (b.miRol == "empleado") "Empleado" else null, if (b.estado != "activo") b.estado else null).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WaitChip(b.turnosEnEspera ?: 0)
                if (!b.hasLocation) {
                    Spacer(Modifier.width(6.dp))
                    Text("📍 Sin ubicación", style = MaterialTheme.typography.labelMedium, color = TyColors.OrangeDark)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Gestionar", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Crear o editar el perfil del negocio (RF02) con selector de ubicación en el mapa. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusinessEditorScreen(businessId: Int?, onBack: () -> Unit, onSaved: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    val existing = rememberResource(businessId) {
        if (businessId == null) Result.success(null) else apiCall { business(businessId) }
    }

    var loaded by rememberSaveable { mutableStateOf(businessId == null) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var horario by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lng by rememberSaveable { mutableStateOf<Double?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existing.data) {
        val b = existing.data
        if (b != null && !loaded) {
            nombre = b.nombre; categoria = b.categoria.orEmpty(); descripcion = b.descripcion.orEmpty()
            direccion = b.direccion.orEmpty(); telefono = b.telefono.orEmpty(); horario = b.horario.orEmpty()
            lat = b.latitud; lng = b.longitud
            loaded = true
        }
    }

    if (businessId != null && !loaded) {
        when (val s = existing.state) {
            is Load.Err -> { Column { TyTopBar("Editar negocio", onBack); ErrorBox(s.message, onRetry = { existing.reload() }) } }
            else -> LoadingBox()
        }
        return
    }

    val start = if (lat != null && lng != null) LatLng(lat!!, lng!!) else DEFAULT_LOCATION
    val mapState = rememberOsmMapState(start, if (lat != null) 17.0 else 13.0)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) scope.launch { currentLocation(context)?.let { mapState.animateTo(it, 17.5) } }
    }
    LaunchedEffect(Unit) {
        if (lat == null && hasLocationPermission(context)) currentLocation(context)?.let { mapState.animateTo(it, 17.0) }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TyTopBar(if (businessId == null) "Nuevo negocio" else "Editar negocio", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp)) {
            Text("Ubicación en el mapa", style = MaterialTheme.typography.titleMedium)
            Text("Mueve el mapa hasta que el pin quede sobre tu local.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(24.dp))) {
                OsmMap(mapState, Modifier.fillMaxSize())
                // Pin fijo al centro (estilo apps de domicilios)
                val pin = remember(categoria) { pinDrawable(context, categoria, null, true).bitmap }
                androidx.compose.foundation.Image(
                    pin.asImageBitmap(), null,
                    modifier = Modifier.align(Alignment.Center).offset(y = (-28).dp).size(52.dp, 64.dp),
                )
                SmallFloatingActionButton(
                    onClick = {
                        if (hasLocationPermission(context)) scope.launch { currentLocation(context)?.let { mapState.animateTo(it, 17.5) } }
                        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                ) { Icon(Icons.Default.MyLocation, "Mi ubicación", tint = MaterialTheme.colorScheme.primary) }
                val c = mapState.center
                Surface(
                    shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                ) {
                    Text(String.format(java.util.Locale.US, "%.5f, %.5f", c.lat, c.lng), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            TyTextField(nombre, { nombre = it; error = null }, "Nombre del negocio *", leading = Icons.Default.Storefront)
            Spacer(Modifier.height(16.dp))
            Text("Categoría", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORIES.forEach { c ->
                    val active = categoria.equals(c.name, ignoreCase = true)
                    Text(
                        "${c.emoji} ${c.name}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                            .clickable { categoria = c.name }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            TyTextField(direccion, { direccion = it }, "Dirección", leading = Icons.Default.Place, placeholder = "Cra 7 # 72-41, Bogotá")
            Spacer(Modifier.height(12.dp))
            TyTextField(horario, { horario = it }, "Horario", leading = Icons.Default.Schedule, placeholder = "Lun a Sáb 8:00 a.m. - 6:00 p.m.")
            Spacer(Modifier.height(12.dp))
            TyTextField(telefono, { telefono = it }, "Teléfono", leading = Icons.Default.Phone, keyboardType = KeyboardType.Phone)
            Spacer(Modifier.height(12.dp))
            TyTextField(descripcion, { if (it.length <= 500) descripcion = it }, "Descripción", leading = Icons.Default.Description, singleLine = false, minLines = 3)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(24.dp))
        }
        Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
            TyButton(
                if (businessId == null) "Crear negocio" else "Guardar cambios",
                icon = Icons.Default.CheckCircle,
                loading = saving,
                modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                onClick = {
                    if (nombre.trim().length < 2) { error = "Escribe el nombre del negocio"; return@TyButton }
                    val center = mapState.center
                    val req = BusinessRequest(
                        nombre = nombre.trim(),
                        categoria = categoria.ifBlank { null },
                        horario = horario.trim().ifBlank { null },
                        descripcion = descripcion.trim().ifBlank { null },
                        direccion = direccion.trim().ifBlank { null },
                        telefono = telefono.trim().ifBlank { null },
                        latitud = center.lat,
                        longitud = center.lng,
                        logoUrl = existing.data?.logoUrl,
                    )
                    scope.launch {
                        saving = true
                        val r = if (businessId == null) apiCall { createBusiness(req) } else apiCall { updateBusiness(businessId, req) }
                        r.onSuccess {
                            snackbar.showSnackbar(if (businessId == null) "¡Negocio creado! Ya aparece en el mapa." else "Cambios guardados")
                            onSaved(it.id)
                        }.onFailure { error = it.message }
                        saving = false
                    }
                },
            )
        }
    }
}
