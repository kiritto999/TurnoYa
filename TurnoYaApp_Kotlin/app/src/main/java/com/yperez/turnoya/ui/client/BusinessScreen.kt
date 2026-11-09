package com.yperez.turnoya.ui.client

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yperez.turnoya.data.Business
import com.yperez.turnoya.data.CatalogItem
import com.yperez.turnoya.data.Sector
import com.yperez.turnoya.data.TurnRequest
import com.yperez.turnoya.data.User
import com.yperez.turnoya.data.apiCall
import com.yperez.turnoya.ui.Load
import com.yperez.turnoya.ui.LocalSnackbar
import com.yperez.turnoya.ui.PollEffect
import com.yperez.turnoya.ui.components.BusinessAvatar
import com.yperez.turnoya.ui.components.ErrorBox
import com.yperez.turnoya.ui.components.LoadingBox
import com.yperez.turnoya.ui.components.TyButton
import com.yperez.turnoya.ui.components.TyTextField
import com.yperez.turnoya.ui.components.categoryBrush
import com.yperez.turnoya.ui.map.MapMarker
import com.yperez.turnoya.ui.map.OsmMap
import com.yperez.turnoya.ui.map.rememberOsmMapState
import com.yperez.turnoya.ui.rememberResource
import com.yperez.turnoya.util.LatLng
import com.yperez.turnoya.util.formatPrice
import com.yperez.turnoya.util.formatWait
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private data class BusinessBundle(
    val business: Business,
    val sectors: List<Sector>,
    val catalog: List<CatalogItem>,
    val waiting: Int,
    val waitMinutes: Int,
    val myActiveTurnId: Int?,
)

@Composable
fun BusinessScreen(
    businessId: Int,
    initialSectorId: Int?,
    user: User,
    onBack: () -> Unit,
    onTurnCreated: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    val res = rememberResource(businessId) {
        runCatching {
            coroutineScope {
                val b = async { apiCall { business(businessId) }.getOrThrow() }
                val s = async { apiCall { sectors(businessId) }.getOrDefault(emptyList()) }
                val c = async { apiCall { catalog(businessId) }.getOrDefault(emptyList()) }
                val q = async { if (user.isClient) apiCall { queue(businessId) }.getOrDefault(emptyList()) else emptyList() }
                val mine = async { if (user.isClient) apiCall { myTurns() }.getOrDefault(emptyList()) else emptyList() }
                val queue = q.await()
                val pending = queue.filter { it.estado == "pendiente" }
                BusinessBundle(
                    b.await(), s.await(), c.await(),
                    waiting = b.await().turnosEnEspera ?: pending.size,
                    waitMinutes = queue.maxOfOrNull { (it.tiempoEsperaMin ?: 0) } ?: 0,
                    myActiveTurnId = mine.await().firstOrNull { it.businessId == businessId && it.isActive }?.id,
                )
            }
        }
    }
    PollEffect(15_000, businessId) { res.reload(silent = true) }

    var sectorId by rememberSaveable { mutableStateOf(initialSectorId) }
    var itemId by rememberSaveable { mutableStateOf<Int?>(null) }
    var motivo by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    when (val s = res.state) {
        is Load.Loading -> LoadingBox()
        is Load.Err -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
            ErrorBox(s.message, onRetry = { res.reload() })
        }
        is Load.Ok -> {
            val data = s.data
            val b = data.business
            val items = data.catalog.filter { sectorId == null || it.sectorId == null || it.sectorId == sectorId }
            val listState = rememberLazyListState()
            val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 300 } }
            com.yperez.turnoya.ui.components.LightStatusBarIcons(enabled = !scrolled)
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 140.dp)) {
                    item { Header(b, onBack) }
                    item { QueueStatus(data.waiting, data.waitMinutes) }
                    item { InfoSection(b) }
                    if (data.sectors.isNotEmpty()) {
                        item {
                            Text("Elige el área", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item { SectorChip("General", sectorId == null) { sectorId = null; itemId = null } }
                                items(data.sectors) { sec -> SectorChip(sec.nombre, sectorId == sec.id) { sectorId = sec.id; itemId = null } }
                            }
                        }
                    }
                    item {
                        Text(
                            if (items.isEmpty()) "Catálogo" else "¿Qué necesitas? (opcional)",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp),
                        )
                    }
                    if (items.isEmpty()) {
                        item {
                            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text("Este negocio aún no publica productos o servicios.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    items(items, key = { it.id }) { item ->
                        CatalogRow(item, selected = itemId == item.id, selectable = user.isClient) {
                            itemId = if (itemId == item.id) null else item.id
                            // El servicio pertenece a un área: el turno va a esa fila
                            if (itemId != null && item.sectorId != null) sectorId = item.sectorId
                        }
                    }
                    if (user.isClient && data.myActiveTurnId == null) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            TyTextField(
                                motivo, { if (it.length <= 255) motivo = it }, "Motivo o nota (opcional)",
                                modifier = Modifier.padding(horizontal = 20.dp), singleLine = false, minLines = 2,
                                placeholder = "Ej: corte y barba, reclamo, retiro…",
                            )
                        }
                    }
                }

                // Fondo bajo la barra de estado cuando el contenido se desplaza
                if (scrolled) {
                    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding())
                }
                // Barra inferior con la acción principal
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ) {
                    Column(Modifier.padding(16.dp).navigationBarsPadding()) {
                        when {
                            !user.isClient -> Text(
                                "Estás viendo el negocio como ${user.rol}. Solo las cuentas de cliente pueden pedir turnos.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            b.estado != "activo" -> Text("Este negocio no está recibiendo turnos ahora.", color = MaterialTheme.colorScheme.error)
                            data.myActiveTurnId != null -> TyButton(
                                "Ver mi turno en este negocio",
                                onClick = { onTurnCreated(data.myActiveTurnId) },
                                icon = Icons.Default.ConfirmationNumber,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            else -> {
                                val chosen = data.catalog.firstOrNull { it.id == itemId }
                                if (chosen != null) {
                                    Row(Modifier.padding(bottom = 10.dp)) {
                                        Text(chosen.nombre, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(formatPrice(chosen.precio), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                TyButton(
                                    "Pedir turno ya",
                                    icon = Icons.Default.ConfirmationNumber,
                                    loading = sending,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        scope.launch {
                                            sending = true
                                            apiCall { requestTurn(businessId, TurnRequest(sectorId, itemId, motivo.trim().ifBlank { null })) }
                                                .onSuccess { onTurnCreated(it.id) }
                                                .onFailure { snackbar.showSnackbar(it.message ?: "No se pudo pedir el turno") }
                                            sending = false
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(b: Business, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(170.dp).background(categoryBrush(b.categoria)))
        // Ticket decorativo de fondo
        Icon(
            Icons.Default.ConfirmationNumber, null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.size(180.dp).align(Alignment.TopEnd).padding(top = 10.dp),
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier.statusBarsPadding().padding(8.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.25f)),
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) }
        Surface(
            modifier = Modifier.padding(top = 120.dp, start = 16.dp, end = 16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                BusinessAvatar(b.nombre, b.categoria, 60.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(b.nombre, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(b.categoria ?: "Negocio", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (b.estado == "activo") {
                    Text(
                        "Abierto", color = Color(0xFF0E7A4E), style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.background(Color(0xFFDDF7EA), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueStatus(waiting: Int, minutes: Int) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(Icons.Default.Groups, "$waiting", "en fila", Modifier.weight(1f))
        StatTile(Icons.Default.Timer, if (waiting == 0) "0 min" else formatWait(minutes), "espera aprox.", Modifier.weight(1f))
    }
}

@Composable
fun StatTile(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Row(
        modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoSection(b: Business) {
    val context = LocalContext.current
    Column(Modifier.padding(horizontal = 20.dp)) {
        if (!b.descripcion.isNullOrBlank()) {
            Text(b.descripcion, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
        if (!b.direccion.isNullOrBlank()) InfoLine(Icons.Default.Place, b.direccion)
        if (!b.horario.isNullOrBlank()) InfoLine(Icons.Default.AccessTime, b.horario)
        if (!b.telefono.isNullOrBlank()) InfoLine(Icons.Default.Phone, b.telefono) {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${b.telefono}")))
        }
        if (b.hasLocation) {
            Spacer(Modifier.height(10.dp))
            val pos = LatLng(b.latitud!!, b.longitud!!)
            val mapState = rememberOsmMapState(pos, 16.0)
            Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp))) {
                OsmMap(mapState, Modifier.fillMaxSize(), markers = listOf(MapMarker(b.id, pos, b.nombre, b.categoria)))
                Text(
                    "Cómo llegar",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                        .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            val uri = Uri.parse("geo:${pos.lat},${pos.lng}?q=${pos.lat},${pos.lng}(${Uri.encode(b.nombre)})")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectorChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

@Composable
private fun CatalogRow(item: CatalogItem, selected: Boolean, selectable: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .then(if (selectable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.nombre, style = MaterialTheme.typography.titleMedium)
            if (!item.descripcion.isNullOrBlank()) {
                Text(item.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatPrice(item.precio), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                if (item.tiempoEstimado != null) {
                    Text("  ·  ⏱ ${item.tiempoEstimado} min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (selectable) {
            Icon(
                if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}
