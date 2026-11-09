package com.yperez.turnoya.ui.owner

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yperez.turnoya.data.Business
import com.yperez.turnoya.data.CatalogItem
import com.yperez.turnoya.data.CatalogItemRequest
import com.yperez.turnoya.data.Employee
import com.yperez.turnoya.data.InviteRequest
import com.yperez.turnoya.data.Sector
import com.yperez.turnoya.data.SectorRequest
import com.yperez.turnoya.data.Turn
import com.yperez.turnoya.data.TurnStatusRequest
import com.yperez.turnoya.data.TurnStates
import com.yperez.turnoya.data.User
import com.yperez.turnoya.data.apiCall
import com.yperez.turnoya.ui.Load
import com.yperez.turnoya.ui.LocalSnackbar
import com.yperez.turnoya.ui.PollEffect
import com.yperez.turnoya.ui.client.StatTile
import com.yperez.turnoya.ui.components.BusinessAvatar
import com.yperez.turnoya.ui.components.EmptyState
import com.yperez.turnoya.ui.components.ErrorBox
import com.yperez.turnoya.ui.components.LoadingBox
import com.yperez.turnoya.ui.components.StatusPill
import com.yperez.turnoya.ui.components.TyButton
import com.yperez.turnoya.ui.components.TyCard
import com.yperez.turnoya.ui.components.TyOutlinedButton
import com.yperez.turnoya.ui.components.TyTextField
import com.yperez.turnoya.ui.components.TyTopBar
import com.yperez.turnoya.ui.rememberResource
import com.yperez.turnoya.ui.theme.TyColors
import com.yperez.turnoya.util.formatPrice
import com.yperez.turnoya.util.timeAgo
import kotlinx.coroutines.launch

private enum class ManageTab(val title: String, val icon: ImageVector, val ownerOnly: Boolean) {
    Queue("Fila", Icons.Default.Queue, false),
    Catalog("Catálogo", Icons.Default.Inventory2, true),
    Sectors("Sectores", Icons.Default.Category, true),
    Team("Equipo", Icons.Default.Groups, true),
    Qr("Códigos QR", Icons.Default.QrCode2, true),
}

@Composable
fun ManageBusinessScreen(businessId: Int, user: User, onBack: () -> Unit, onEdit: () -> Unit) {
    val business = rememberResource(businessId) { apiCall { business(businessId) } }
    val sectors = rememberResource(businessId, "sectors") { apiCall { sectors(businessId) } }
    val b = business.data
    val isOwner = b != null && (b.ownerId == user.id || user.rol == "superadmin")
    val tabs = ManageTab.entries.filter { !it.ownerOnly || isOwner }
    var tab by rememberSaveable { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TyTopBar(b?.nombre ?: "Negocio", onBack = onBack) {
            if (isOwner) IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar negocio") }
        }
        when (val s = business.state) {
            is Load.Loading -> LoadingBox()
            is Load.Err -> ErrorBox(s.message, onRetry = { business.reload() })
            is Load.Ok -> {
                if (tabs.size > 1) {
                    ScrollableTabRow(
                        selectedTabIndex = tab.coerceAtMost(tabs.lastIndex),
                        edgePadding = 12.dp,
                        containerColor = MaterialTheme.colorScheme.background,
                        divider = {},
                    ) {
                        tabs.forEachIndexed { i, t ->
                            Tab(
                                selected = tab == i,
                                onClick = { tab = i },
                                text = { Text(t.title, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal) },
                                icon = { Icon(t.icon, null, Modifier.size(20.dp)) },
                            )
                        }
                    }
                }
                val secs = sectors.data.orEmpty()
                when (tabs[tab.coerceAtMost(tabs.lastIndex)]) {
                    ManageTab.Queue -> QueueTab(s.data, secs)
                    ManageTab.Catalog -> CatalogTab(businessId, secs)
                    ManageTab.Sectors -> SectorsTab(businessId, secs) { sectors.reload(silent = true) }
                    ManageTab.Team -> TeamTab(businessId, secs)
                    ManageTab.Qr -> QrTab(s.data, secs)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ FILA (RF10)

@Composable
private fun QueueTab(b: Business, sectors: List<Sector>) {
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    var sectorId by rememberSaveable { mutableStateOf<Int?>(null) }
    val queue = rememberResource(b.id, sectorId) { apiCall { queue(b.id, sectorId) } }
    val stats = rememberResource(b.id, "stats") { apiCall { stats(b.id) } }
    PollEffect(5_000, b.id, sectorId) { queue.reload(silent = true); stats.reload(silent = true) }
    var busy by remember { mutableStateOf(false) }

    fun update(t: Turn, estado: String) = scope.launch {
        apiCall { setTurnStatus(t.id, TurnStatusRequest(estado)) }
            .onSuccess { queue.reload(silent = true); stats.reload(silent = true) }
            .onFailure { snackbar.showSnackbar(it.message ?: "Error") }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            val st = stats.data
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.Default.Groups, "${st?.enEspera ?: "–"}", "en espera", Modifier.weight(1f))
                StatTile(Icons.Default.TaskAlt, "${st?.atendidosHoy ?: "–"}", "atendidos hoy", Modifier.weight(1f), tint = TyColors.MintDark)
            }
        }
        if (sectors.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChipTy("Todos", sectorId == null) { sectorId = null } }
                    items(sectors) { s -> FilterChipTy(s.nombre, sectorId == s.id) { sectorId = s.id } }
                }
            }
        }
        item {
            TyButton(
                "Llamar siguiente turno",
                icon = Icons.Default.Campaign,
                loading = busy,
                brush = TyColors.hotGradient,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        busy = true
                        apiCall { callNext(b.id, sectorId) }
                            .onSuccess { snackbar.showSnackbar("Llamando al turno #${it.numero}${it.nombreCliente?.let { n -> " · $n" } ?: ""}"); queue.reload(silent = true); stats.reload(silent = true) }
                            .onFailure { snackbar.showSnackbar(it.message ?: "Error") }
                        busy = false
                    }
                },
            )
        }
        when (val s = queue.state) {
            is Load.Loading -> item { Box(Modifier.height(200.dp)) { LoadingBox() } }
            is Load.Err -> item { ErrorBox(s.message, onRetry = { queue.reload() }) }
            is Load.Ok -> if (s.data.isEmpty()) {
                item { EmptyState(Icons.Default.Queue, "No hay nadie en la fila", "Cuando un cliente pida turno aparecerá aquí al instante.") }
            } else {
                items(s.data, key = { it.id }) { t -> QueueRow(t, onState = { update(t, it) }) }
            }
        }
    }
}

@Composable
private fun QueueRow(t: Turn, onState: (String) -> Unit) {
    val highlight = t.estado != TurnStates.PENDING
    TyCard {
        Column(
            Modifier
                .then(if (highlight) Modifier.background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)) else Modifier)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (highlight) TyColors.hotGradient else TyColors.ctaGradient),
                    contentAlignment = Alignment.Center,
                ) { Text("#${t.numero}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.nombreCliente ?: "Cliente", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(t.nombreSector, t.nombreItem, timeAgo(t.fechaSolicitud)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                    )
                }
                StatusPill(t.estado)
            }
            if (!t.motivo.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("📝 ${t.motivo}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (t.estado) {
                    TurnStates.PENDING -> {
                        ActionChip("Llamar", Icons.Default.Campaign, TyColors.Orange, Modifier.weight(1f)) { onState(TurnStates.CALLED) }
                        ActionChip("Cancelar", Icons.Default.Close, TyColors.Danger, Modifier.weight(1f)) { onState(TurnStates.CANCELLED) }
                    }
                    TurnStates.CALLED -> {
                        ActionChip("Atender", Icons.Default.PlayArrow, TyColors.Blue, Modifier.weight(1f)) { onState(TurnStates.SERVING) }
                        ActionChip("No llegó", Icons.Default.PersonOff, TyColors.Danger, Modifier.weight(1f)) { onState(TurnStates.ABSENT) }
                    }
                    TurnStates.SERVING -> {
                        ActionChip("Finalizar", Icons.Default.CheckCircle, TyColors.MintDark, Modifier.weight(1f)) { onState(TurnStates.DONE) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(text: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.12f)).clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun FilterChipTy(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

// ------------------------------------------------------------------ CATÁLOGO (RF04)

@Composable
private fun CatalogTab(businessId: Int, sectors: List<Sector>) {
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    val items = rememberResource(businessId, "catalog") { apiCall { catalog(businessId) } }
    var editing by remember { mutableStateOf<CatalogItem?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CatalogItem?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TyButton("Agregar producto o servicio", { creating = true }, Modifier.fillMaxWidth(), icon = Icons.Default.Add) }
        when (val s = items.state) {
            is Load.Loading -> item { Box(Modifier.height(200.dp)) { LoadingBox() } }
            is Load.Err -> item { ErrorBox(s.message, onRetry = { items.reload() }) }
            is Load.Ok -> if (s.data.isEmpty()) {
                item { EmptyState(Icons.Default.Inventory2, "Tu catálogo está vacío", "Agrega servicios con precio y tiempo estimado: así calculamos la espera.") }
            } else items(s.data, key = { it.id }) { item ->
                TyCard {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOfNotNull(
                                    formatPrice(item.precio),
                                    item.tiempoEstimado?.let { "⏱ $it min" },
                                    sectors.firstOrNull { it.id == item.sectorId }?.nombre,
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!item.descripcion.isNullOrBlank()) Text(item.descripcion, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { editing = item }) { Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { deleting = item }) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        CatalogDialog(editing, sectors, onDismiss = { creating = false; editing = null }) { req ->
            scope.launch {
                val current = editing
                val r = if (current == null) apiCall { createItem(businessId, req) } else apiCall { updateItem(current.id, req) }
                r.onSuccess { creating = false; editing = null; items.reload(silent = true) }
                    .onFailure { snackbar.showSnackbar(it.message ?: "Error") }
            }
        }
    }
    deleting?.let { item ->
        ConfirmDialog("¿Eliminar \"${item.nombre}\"?", "Dejará de aparecer en el catálogo.", onDismiss = { deleting = null }) {
            scope.launch {
                apiCall { deleteItem(item.id) }.onSuccess { items.reload(silent = true) }.onFailure { snackbar.showSnackbar(it.message ?: "Error") }
                deleting = null
            }
        }
    }
}

@Composable
private fun CatalogDialog(item: CatalogItem?, sectors: List<Sector>, onDismiss: () -> Unit, onSave: (CatalogItemRequest) -> Unit) {
    var nombre by remember { mutableStateOf(item?.nombre.orEmpty()) }
    var descripcion by remember { mutableStateOf(item?.descripcion.orEmpty()) }
    var precio by remember { mutableStateOf(item?.precio?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()) }
    var tiempo by remember { mutableStateOf(item?.tiempoEstimado?.toString().orEmpty()) }
    var sectorId by remember { mutableStateOf(item?.sectorId) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Nuevo producto o servicio" else "Editar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TyTextField(nombre, { nombre = it; error = null }, "Nombre *")
                TyTextField(descripcion, { descripcion = it }, "Descripción", singleLine = false)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TyTextField(precio, { precio = it.filter { c -> c.isDigit() || c == '.' } }, "Precio", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                    TyTextField(tiempo, { tiempo = it.filter(Char::isDigit) }, "Minutos", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                }
                if (sectors.isNotEmpty()) {
                    Text("Sector", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item { FilterChipTy("Ninguno", sectorId == null) { sectorId = null } }
                        items(sectors) { s -> FilterChipTy(s.nombre, sectorId == s.id) { sectorId = s.id } }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isBlank()) { error = "Escribe el nombre"; return@TextButton }
                onSave(CatalogItemRequest(sectorId, nombre.trim(), descripcion.trim().ifBlank { null }, precio.toDoubleOrNull(), tiempo.toIntOrNull()))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

// ------------------------------------------------------------------ SECTORES (RF03)

@Composable
private fun SectorsTab(businessId: Int, sectors: List<Sector>, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    var editing by remember { mutableStateOf<Sector?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Sector?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                "Divide tu negocio en áreas (ej: Caja, Asesoría, Cortes). Cada sector tiene su propia fila y su propio QR.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
            )
        }
        item { TyButton("Agregar sector", { creating = true }, Modifier.fillMaxWidth(), icon = Icons.Default.Add) }
        if (sectors.isEmpty()) item { EmptyState(Icons.Default.Category, "Sin sectores", "Si tu negocio tiene una sola fila no necesitas sectores.") }
        items(sectors, key = { it.id }) { s ->
            TyCard {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.nombre, style = MaterialTheme.typography.titleMedium)
                        if (!s.descripcion.isNullOrBlank()) Text(s.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { editing = s }) { Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = { deleting = s }) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    if (creating || editing != null) {
        var nombre by remember { mutableStateOf(editing?.nombre.orEmpty()) }
        var desc by remember { mutableStateOf(editing?.descripcion.orEmpty()) }
        AlertDialog(
            onDismissRequest = { creating = false; editing = null },
            title = { Text(if (editing == null) "Nuevo sector" else "Editar sector") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TyTextField(nombre, { nombre = it }, "Nombre *")
                    TyTextField(desc, { desc = it }, "Descripción")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nombre.isBlank()) return@TextButton
                    val req = SectorRequest(nombre.trim(), desc.trim().ifBlank { null })
                    val current = editing
                    scope.launch {
                        val r = if (current == null) apiCall { createSector(businessId, req) } else apiCall { updateSector(current.id, req) }
                        r.onSuccess { creating = false; editing = null; onChanged() }.onFailure { snackbar.showSnackbar(it.message ?: "Error") }
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { creating = false; editing = null }) { Text("Cancelar") } },
        )
    }
    deleting?.let { s ->
        ConfirmDialog("¿Eliminar el sector ${s.nombre}?", "Solo se puede borrar si no tiene turnos ni productos asociados.", onDismiss = { deleting = null }) {
            scope.launch {
                apiCall { deleteSector(s.id) }.onSuccess { r ->
                    if (r.isSuccessful) onChanged() else snackbar.showSnackbar("No se pudo eliminar: tiene datos asociados")
                }.onFailure { snackbar.showSnackbar(it.message ?: "Error") }
                deleting = null
            }
        }
    }
}

// ------------------------------------------------------------------ EQUIPO (RF07, RF08)

@Composable
private fun TeamTab(businessId: Int, sectors: List<Sector>) {
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    val team = rememberResource(businessId, "team") { apiCall { employees(businessId) } }
    var inviting by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<Employee?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                "Invita a tus empleados con el correo de su cuenta de TurnoYa. Ellos aceptan desde su Perfil y podrán gestionar la fila.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
            )
        }
        item { TyButton("Invitar empleado", { inviting = true }, Modifier.fillMaxWidth(), icon = Icons.Default.PersonAdd) }
        when (val s = team.state) {
            is Load.Loading -> item { Box(Modifier.height(200.dp)) { LoadingBox() } }
            is Load.Err -> item { ErrorBox(s.message, onRetry = { team.reload() }) }
            is Load.Ok -> if (s.data.isEmpty()) {
                item { EmptyState(Icons.Default.Groups, "Aún no tienes equipo", "Los empleados te ayudan a llamar y atender turnos.") }
            } else items(s.data, key = { it.id }) { e ->
                TyCard {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(50)).background(TyColors.brandGradient), contentAlignment = Alignment.Center) {
                            Text((e.nombre ?: "?").take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.nombre ?: "Empleado", style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOfNotNull(e.email, sectors.firstOrNull { it.id == e.sectorId }?.nombre).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val (txt, col) = when (e.estadoInvitacion) {
                            "aceptada" -> "Activo" to TyColors.MintDark
                            "rechazada" -> "Rechazó" to TyColors.Danger
                            else -> "Pendiente" to TyColors.Orange
                        }
                        Text(txt, color = col, style = MaterialTheme.typography.labelMedium, modifier = Modifier.background(col.copy(alpha = 0.12f), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp))
                        IconButton(onClick = { removing = e }) { Icon(Icons.Default.Delete, "Quitar", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    if (inviting) {
        var email by remember { mutableStateOf("") }
        var sectorId by remember { mutableStateOf<Int?>(null) }
        AlertDialog(
            onDismissRequest = { inviting = false },
            title = { Text("Invitar empleado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TyTextField(email, { email = it }, "Correo del empleado", keyboardType = KeyboardType.Email)
                    if (sectors.isNotEmpty()) {
                        Text("Asignar a sector (opcional)", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item { FilterChipTy("Todos", sectorId == null) { sectorId = null } }
                            items(sectors) { s -> FilterChipTy(s.nombre, sectorId == s.id) { sectorId = s.id } }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        apiCall { invite(businessId, InviteRequest(email.trim(), sectorId, "turnos")) }
                            .onSuccess { inviting = false; team.reload(silent = true); snackbar.showSnackbar("Invitación enviada a ${email.trim()}") }
                            .onFailure { snackbar.showSnackbar(it.message ?: "Error") }
                    }
                }) { Text("Invitar") }
            },
            dismissButton = { TextButton(onClick = { inviting = false }) { Text("Cancelar") } },
        )
    }
    removing?.let { e ->
        ConfirmDialog("¿Quitar a ${e.nombre ?: e.email}?", "Ya no podrá gestionar la fila de este negocio.", onDismiss = { removing = null }) {
            scope.launch {
                apiCall { removeEmployee(e.id) }.onSuccess { team.reload(silent = true) }.onFailure { snackbar.showSnackbar(it.message ?: "Error") }
                removing = null
            }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Confirmar", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
