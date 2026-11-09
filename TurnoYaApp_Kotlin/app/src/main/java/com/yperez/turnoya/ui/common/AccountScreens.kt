package com.yperez.turnoya.ui.common

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yperez.turnoya.BuildConfig
import com.yperez.turnoya.data.AppNotification
import com.yperez.turnoya.data.Employee
import com.yperez.turnoya.data.User
import com.yperez.turnoya.data.apiCall
import com.yperez.turnoya.notifications.Notifier
import com.yperez.turnoya.ui.AppViewModel
import com.yperez.turnoya.ui.Load
import com.yperez.turnoya.ui.LocalSnackbar
import com.yperez.turnoya.ui.PollEffect
import com.yperez.turnoya.ui.auth.ServerDialog
import com.yperez.turnoya.ui.components.EmptyState
import com.yperez.turnoya.ui.components.ErrorBox
import com.yperez.turnoya.ui.components.LoadingBox
import com.yperez.turnoya.ui.components.SectionTitle
import com.yperez.turnoya.ui.components.TyButton
import com.yperez.turnoya.ui.components.TyCard
import com.yperez.turnoya.ui.components.TyLogo
import com.yperez.turnoya.ui.components.TyOutlinedButton
import com.yperez.turnoya.ui.components.TyTopBar
import com.yperez.turnoya.ui.rememberResource
import com.yperez.turnoya.ui.theme.TyColors
import com.yperez.turnoya.util.timeAgo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(vm: AppViewModel, onOpenTurn: (Int) -> Unit, onOpenBusiness: (Int) -> Unit, onOpenProfile: () -> Unit, bottomPadding: androidx.compose.ui.unit.Dp) {
    val scope = rememberCoroutineScope()
    val res = rememberResource("notifs") { apiCall { notifications() } }
    PollEffect(15_000) { res.reload(silent = true) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TyTopBar("Avisos") {
            if (res.data?.any { !it.leido } == true) {
                TextButton(onClick = {
                    scope.launch {
                        apiCall { readAll() }
                        res.reload(silent = true)
                        vm.refreshUnread()
                    }
                }) {
                    Icon(Icons.Default.DoneAll, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Marcar leídas")
                }
            }
        }
        NotificationPermissionBanner()
        when (val s = res.state) {
            is Load.Loading -> LoadingBox()
            is Load.Err -> ErrorBox(s.message, onRetry = { res.reload() })
            is Load.Ok -> PullToRefreshBox(isRefreshing = res.refreshing, onRefresh = { res.reload() }) {
                if (s.data.isEmpty()) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        EmptyState(Icons.Default.NotificationsNone, "Todo tranquilo", "Aquí verás cuando tu turno avance o llegue una solicitud nueva.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding + 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(s.data, key = { it.id }) { n ->
                            NotificationRow(n) {
                                scope.launch {
                                    if (!n.leido) { apiCall { markRead(n.id) }; vm.refreshUnread(); res.reload(silent = true) }
                                    when {
                                        n.tipo == "invitacion" -> onOpenProfile()
                                        n.tipo == "cambio_turno" && n.turnId != null -> onOpenTurn(n.turnId)
                                        n.businessId != null -> onOpenBusiness(n.businessId)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit) {
    val (icon, color) = when (n.tipo) {
        "nuevo_turno" -> Icons.Default.PersonAdd to TyColors.Blue
        "invitacion" -> Icons.Default.Work to TyColors.MintDark
        else -> Icons.Default.ConfirmationNumber to TyColors.Orange
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (n.leido) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(n.mensaje, style = MaterialTheme.typography.bodyMedium, fontWeight = if (n.leido) FontWeight.Normal else FontWeight.SemiBold)
            Text(timeAgo(n.fecha), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!n.leido) Box(Modifier.size(10.dp).clip(CircleShape).background(TyColors.Orange))
    }
}

@Composable
private fun NotificationPermissionBanner() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(Notifier.canNotify(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    if (granted || Build.VERSION.SDK_INT < 33) return
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
        Spacer(Modifier.width(10.dp))
        Text("Activa las notificaciones para saber cuándo te llaman.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        TextButton(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Activar") }
    }
}

@Composable
fun ProfileScreen(user: User, vm: AppViewModel, bottomPadding: androidx.compose.ui.unit.Dp) {
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    val invitations = rememberResource("invites") { apiCall { myInvitations() } }
    var showServer by remember { mutableStateOf(false) }
    val server by vm.serverUrl.collectAsState()
    com.yperez.turnoya.ui.components.LightStatusBarIcons()

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()),
    ) {
        Column(
            Modifier.fillMaxWidth().background(TyColors.headerGradient).statusBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(84.dp).clip(CircleShape).background(TyColors.brandGradient), contentAlignment = Alignment.Center) {
                Text(user.nombre.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.headlineLarge)
            }
            Spacer(Modifier.height(12.dp))
            Text(user.nombre, color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text(user.email, color = Color.White.copy(alpha = 0.75f))
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    user.isOwner -> "🏪 Dueño de negocio"
                    user.isEmployee -> "🧑‍💼 Empleado"
                    user.rol == "superadmin" -> "🛡️ Superadministrador"
                    else -> "🙋 Cliente"
                },
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }

        Column(Modifier.padding(16.dp)) {
            val pending = invitations.data.orEmpty()
            if (pending.isNotEmpty()) {
                SectionTitle("Invitaciones de trabajo")
                Spacer(Modifier.height(8.dp))
                pending.forEach { inv ->
                    InvitationCard(
                        inv,
                        onAccept = {
                            scope.launch {
                                apiCall { acceptInvitation(inv.id) }
                                    .onSuccess {
                                        snackbar.showSnackbar("¡Ahora eres parte de ${inv.nombreNegocio}!")
                                        vm.refreshUser(); invitations.reload()
                                    }
                                    .onFailure { snackbar.showSnackbar(it.message ?: "Error") }
                            }
                        },
                        onReject = {
                            scope.launch {
                                apiCall { rejectInvitation(inv.id) }.onSuccess { invitations.reload() }
                                    .onFailure { snackbar.showSnackbar(it.message ?: "Error") }
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            SectionTitle("Cuenta")
            Spacer(Modifier.height(8.dp))
            TyCard {
                Column {
                    ProfileItem(Icons.Default.Email, "Correo", user.email)
                    ProfileItem(Icons.Default.Badge, "Tipo de cuenta", user.rol.replaceFirstChar { it.uppercase() })
                    ProfileItem(Icons.Default.Dns, "Servidor", server.removePrefix("https://").removeSuffix("/"), onClick = { showServer = true })
                }
            }
            if (user.isClient) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "¿Trabajas en un negocio? Pide al dueño que te invite con tu correo; la invitación aparecerá aquí.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            TyOutlinedButton("Cerrar sesión", { vm.logout() }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Filled.Logout, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                TyLogo(height = 56.dp)
                Text("Versión ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(bottomPadding + 16.dp))
        }
    }
    if (showServer) ServerDialog(vm) { showServer = false }
}

@Composable
private fun InvitationCard(inv: Employee, onAccept: () -> Unit, onReject: () -> Unit) {
    TyCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(TyColors.brandGradient), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Work, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(inv.nombreNegocio ?: "Negocio", style = MaterialTheme.typography.titleMedium)
                    Text("Te invitó a gestionar sus turnos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TyOutlinedButton("Rechazar", onReject, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TyButton("Aceptar", onAccept, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProfileItem(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        }
        if (onClick != null) Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
