package com.yperez.turnoya.ui.client

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yperez.turnoya.data.Turn
import com.yperez.turnoya.data.TurnStatusRequest
import com.yperez.turnoya.data.TurnStates
import com.yperez.turnoya.data.apiCall
import com.yperez.turnoya.ui.Load
import com.yperez.turnoya.ui.LocalSnackbar
import com.yperez.turnoya.ui.PollEffect
import com.yperez.turnoya.ui.components.EmptyState
import com.yperez.turnoya.ui.components.ErrorBox
import com.yperez.turnoya.ui.components.LoadingBox
import com.yperez.turnoya.ui.components.PulseDot
import com.yperez.turnoya.ui.components.SectionTitle
import com.yperez.turnoya.ui.components.StatusPill
import com.yperez.turnoya.ui.components.TyButton
import com.yperez.turnoya.ui.components.TyCard
import com.yperez.turnoya.ui.components.TyOutlinedButton
import com.yperez.turnoya.ui.components.TyTopBar
import com.yperez.turnoya.ui.rememberResource
import com.yperez.turnoya.ui.theme.TyColors
import com.yperez.turnoya.util.formatDateTime
import com.yperez.turnoya.util.formatPrice
import com.yperez.turnoya.util.formatWait
import com.yperez.turnoya.util.timeAgo
import com.yperez.turnoya.util.turnStateLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTurnsScreen(onOpenTurn: (Int) -> Unit, onExplore: () -> Unit, bottomPadding: androidx.compose.ui.unit.Dp) {
    val res = rememberResource("myturns") { apiCall { myTurns() } }
    PollEffect(10_000) { res.reload(silent = true) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TyTopBar("Mis turnos")
        when (val s = res.state) {
            is Load.Loading -> LoadingBox()
            is Load.Err -> ErrorBox(s.message, onRetry = { res.reload() })
            is Load.Ok -> {
                val active = s.data.filter { it.isActive }
                val history = s.data.filterNot { it.isActive }
                PullToRefreshBox(isRefreshing = res.refreshing, onRefresh = { res.reload() }) {
                    if (s.data.isEmpty()) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            EmptyState(
                                Icons.Default.ConfirmationNumber, "Aún no tienes turnos",
                                "Busca un negocio en el mapa o escanea su código QR para pedir tu turno.",
                            ) { TyButton("Explorar el mapa", onExplore, icon = Icons.Default.Map, modifier = Modifier.fillMaxWidth()) }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding + 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (active.isNotEmpty()) {
                                item { SectionTitle("En curso") }
                                items(active, key = { it.id }) { ActiveTurnCard(it) { onOpenTurn(it.id) } }
                            }
                            if (history.isNotEmpty()) {
                                item { Spacer(Modifier.height(8.dp)); SectionTitle("Historial") }
                                items(history, key = { it.id }) { HistoryRow(it) { onOpenTurn(it.id) } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveTurnCard(t: Turn, onClick: () -> Unit) {
    val called = t.estado == TurnStates.CALLED || t.estado == TurnStates.SERVING
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (called) TyColors.hotGradient else TyColors.ctaGradient)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulseDot(Color.White)
                Spacer(Modifier.width(6.dp))
                Text(turnStateLabel(t.estado), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(6.dp))
            Text(t.nombreNegocio ?: "Negocio", color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.nombreSector ?: "General", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    called -> "¡Acércate ahora!"
                    (t.personasAdelante ?: 0) == 0 -> "Eres el siguiente · ${formatWait(t.tiempoEsperaMin)}"
                    else -> "${t.personasAdelante} antes que tú · ${formatWait(t.tiempoEsperaMin)}"
                },
                color = Color.White, style = MaterialTheme.typography.titleSmall,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TURNO", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
            Text("#${t.numero}", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun HistoryRow(t: Turn, onClick: () -> Unit) {
    TyCard(onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) { Text("#${t.numero}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.nombreNegocio ?: "Negocio", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatDateTime(t.fechaSolicitud), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(t.estado)
        }
    }
}

/** Seguimiento en vivo de un turno (RF10): se actualiza solo cada 5 segundos. */
@Composable
fun TurnDetailScreen(turnId: Int, onBack: () -> Unit, onOpenBusiness: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    val res = rememberResource(turnId) { apiCall { turn(turnId) } }
    PollEffect(5_000, turnId) { if (res.data?.isActive != false) res.reload(silent = true) }
    var confirmCancel by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TyTopBar("Tu turno", onBack = onBack)
        when (val s = res.state) {
            is Load.Loading -> LoadingBox()
            is Load.Err -> ErrorBox(s.message, onRetry = { res.reload() })
            is Load.Ok -> {
                val t = s.data
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    TicketCard(t)
                    Spacer(Modifier.height(16.dp))
                    if (t.isActive) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile(Icons.Default.Groups, "${t.personasAdelante ?: 0}", "antes que tú", Modifier.weight(1f))
                            StatTile(Icons.Default.Timer, formatWait(t.tiempoEsperaMin), "espera aprox.", Modifier.weight(1f), tint = TyColors.Orange)
                        }
                        Spacer(Modifier.height(16.dp))
                        ProgressSteps(t.estado)
                        Spacer(Modifier.height(16.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Te avisaremos con una notificación cuando sea tu turno. Puedes salir de la fila física.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    TyOutlinedButton("Ver el negocio", { onOpenBusiness(t.businessId) }, Modifier.fillMaxWidth(), icon = Icons.Default.Storefront)
                    if (t.estado == TurnStates.PENDING || t.estado == TurnStates.CALLED) {
                        Spacer(Modifier.height(10.dp))
                        TyOutlinedButton("Cancelar turno", { confirmCancel = true }, Modifier.fillMaxWidth(), icon = Icons.Default.Cancel, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(32.dp))
                }
                if (confirmCancel) {
                    AlertDialog(
                        onDismissRequest = { confirmCancel = false },
                        title = { Text("¿Cancelar tu turno #${t.numero}?") },
                        text = { Text("Perderás tu lugar en la fila de ${t.nombreNegocio ?: "este negocio"}.") },
                        confirmButton = {
                            TextButton(onClick = {
                                confirmCancel = false
                                scope.launch {
                                    apiCall { setTurnStatus(t.id, TurnStatusRequest(TurnStates.CANCELLED)) }
                                        .onSuccess { res.set(it); snackbar.showSnackbar("Turno cancelado") }
                                        .onFailure { snackbar.showSnackbar(it.message ?: "No se pudo cancelar") }
                                }
                            }) { Text("Sí, cancelar", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Mantener") } },
                    )
                }
            }
        }
    }
}

/** Ticket con el número grande, troquelado y línea punteada (inspirado en el logo). */
@Composable
private fun TicketCard(t: Turn) {
    val called = t.estado == TurnStates.CALLED || t.estado == TurnStates.SERVING
    val bg by animateColorAsState(
        when {
            called -> TyColors.Orange
            t.isActive -> TyColors.Blue
            t.estado == TurnStates.DONE -> TyColors.MintDark
            else -> Color(0xFF8A96B0)
        }, label = "ticket",
    )
    val brush = if (called) TyColors.hotGradient else Brush.linearGradient(listOf(bg, bg.copy(alpha = 0.85f)))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(brush).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(t.nombreNegocio ?: "Negocio", color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(t.nombreSector ?: "General", color = Color.White.copy(alpha = 0.85f))
        Spacer(Modifier.height(12.dp))
        Text("TU TURNO", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
        Text("#${t.numero}", color = Color.White, fontSize = 84.sp, fontWeight = FontWeight.Black, lineHeight = 90.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (t.isActive) { PulseDot(Color.White); Spacer(Modifier.width(8.dp)) }
            Text(
                when (t.estado) {
                    TurnStates.CALLED -> "¡Es tu turno! Acércate"
                    else -> turnStateLabel(t.estado)
                },
                color = Color.White, style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(18.dp))
        Canvas(Modifier.fillMaxWidth().height(2.dp)) {
            drawLine(
                Color.White.copy(alpha = 0.6f), Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)),
            )
        }
        Spacer(Modifier.height(14.dp))
        TicketLine("Solicitado", formatDateTime(t.fechaSolicitud))
        t.nombreItem?.let { TicketLine("Servicio", it) }
        t.precio?.let { TicketLine("Precio", formatPrice(it)) }
        t.motivo?.takeIf { it.isNotBlank() }?.let { TicketLine("Nota", it) }
        t.fechaAtencion?.let { TicketLine("Atendido", formatDateTime(it)) }
    }
}

@Composable
private fun TicketLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProgressSteps(estado: String) {
    val step = when (estado) {
        TurnStates.PENDING -> 0
        TurnStates.CALLED -> 1
        TurnStates.SERVING -> 2
        else -> 3
    }
    val labels = listOf("En fila", "Llamado", "Atendiendo")
    val progress by animateFloatAsState(step / 2f, label = "p")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        val track = MaterialTheme.colorScheme.surfaceVariant
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            drawRoundRect(track, size = size, cornerRadius = CornerRadius(8f, 8f))
            drawRoundRect(
                Brush.horizontalGradient(listOf(TyColors.Blue, TyColors.Mint)),
                size = Size(size.width * progress.coerceIn(0.05f, 1f), size.height),
                cornerRadius = CornerRadius(8f, 8f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { i, l ->
                Text(
                    l, style = MaterialTheme.typography.labelMedium,
                    color = if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (i == step) FontWeight.ExtraBold else FontWeight.Normal,
                )
            }
        }
    }
}

