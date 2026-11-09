package com.yperez.turnoya.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.yperez.turnoya.data.User
import com.yperez.turnoya.data.apiCall
import com.yperez.turnoya.ui.auth.LoginScreen
import com.yperez.turnoya.ui.auth.RegisterScreen
import com.yperez.turnoya.ui.client.BusinessScreen
import com.yperez.turnoya.ui.client.HomeScreen
import com.yperez.turnoya.ui.client.MyTurnsScreen
import com.yperez.turnoya.ui.client.TurnDetailScreen
import com.yperez.turnoya.ui.common.NotificationsScreen
import com.yperez.turnoya.ui.common.ProfileScreen
import com.yperez.turnoya.ui.components.TyLogo
import com.yperez.turnoya.ui.owner.BusinessEditorScreen
import com.yperez.turnoya.ui.owner.ManageBusinessScreen
import com.yperez.turnoya.ui.owner.PanelScreen
import com.yperez.turnoya.ui.theme.TyColors
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val TURNS = "turns"
    const val NOTIFS = "notifications"
    const val PROFILE = "profile"
    const val PANEL = "panel"
    const val BUSINESS = "business/{id}?sector={sector}"
    const val TURN = "turn/{id}"
    const val MANAGE = "manage/{id}"
    const val EDITOR = "editor?id={id}"
    fun business(id: Int, sector: Int? = null) = "business/$id" + (sector?.let { "?sector=$it" } ?: "")
    fun turn(id: Int) = "turn/$id"
    fun manage(id: Int) = "manage/$id"
    fun editor(id: Int? = null) = "editor" + (id?.let { "?id=$it" } ?: "")
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

/** Extrae el código de un QR de TurnoYa: turnoya://qr/{codigo} (o solo el código). */
fun parseQrCode(raw: String?): String? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    Regex("""turnoya://qr/([A-Za-z0-9_\-]+)""").find(text)?.let { return it.groupValues[1] }
    Regex("""/qr/([A-Za-z0-9_\-]+)""").find(text)?.let { return it.groupValues[1] }
    return if (Regex("""^[A-Za-z0-9_\-]{20,}$""").matches(text)) text else null
}

@Composable
fun AppRoot(vm: AppViewModel, openNotifications: Boolean) {
    val state by vm.session.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        when (val s = state) {
            SessionState.Unknown -> Box(Modifier.fillMaxSize().background(TyColors.headerGradient), contentAlignment = Alignment.Center) {
                TyLogo(dark = true, height = 140.dp)
            }
            SessionState.LoggedOut -> AuthNav(vm, snackbar)
            is SessionState.LoggedIn -> MainNav(vm, s.session.user, snackbar, openNotifications)
        }
    }
}

@Composable
private fun AuthNav(vm: AppViewModel, snackbar: SnackbarHostState) {
    val nav = rememberNavController()
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)) { _ ->
        NavHost(nav, startDestination = Routes.LOGIN) {
            composable(Routes.LOGIN) { LoginScreen(vm, onRegister = { nav.navigate(Routes.REGISTER) }) }
            composable(Routes.REGISTER) { RegisterScreen(vm, onBack = { nav.popBackStack() }) }
        }
    }
}

@Composable
private fun MainNav(vm: AppViewModel, user: User, snackbar: SnackbarHostState, openNotifications: Boolean) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val unread by vm.unread.collectAsState()
    val pendingQr by vm.pendingQr.collectAsState()

    val tabs = when {
        user.isClient -> listOf(
            Tab(Routes.HOME, "Inicio", Icons.Default.Explore),
            Tab(Routes.TURNS, "Mis turnos", Icons.Default.ConfirmationNumber),
            Tab(Routes.NOTIFS, "Avisos", Icons.Default.Notifications),
            Tab(Routes.PROFILE, "Perfil", Icons.Default.Person),
        )
        else -> listOf(
            Tab(Routes.PANEL, "Panel", Icons.Default.Dashboard),
            Tab(Routes.HOME, "Explorar", Icons.Default.Explore),
            Tab(Routes.NOTIFS, "Avisos", Icons.Default.Notifications),
            Tab(Routes.PROFILE, "Perfil", Icons.Default.Person),
        )
    }
    val start = tabs.first().route

    // Escaneo de QR (RF06): abre directamente el negocio/sector
    suspend fun openQr(code: String) {
        apiCall { resolveQr(code) }
            .onSuccess { nav.navigate(Routes.business(it.businessId, it.sectorId)) }
            .onFailure { snackbar.showSnackbar("Código QR no válido para TurnoYa") }
    }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val code = parseQrCode(result.contents)
        when {
            result.contents == null -> Unit
            code == null -> scope.launch { snackbar.showSnackbar("Ese QR no es de TurnoYa") }
            else -> scope.launch { openQr(code) }
        }
    }
    fun launchScanner() = scanner.launch(
        ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Apunta al código QR del negocio")
            .setBeepEnabled(false).setOrientationLocked(false),
    )

    LaunchedEffect(pendingQr) {
        val code = pendingQr ?: return@LaunchedEffect
        // Se lanza en el scope de la pantalla: limpiar pendingQr reinicia este efecto y no debe cancelar la apertura
        scope.launch { openQr(code) }
        vm.setPendingQr(null)
    }
    LaunchedEffect(openNotifications) { if (openNotifications) nav.navigate(Routes.NOTIFS) }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route in tabs.map { it.route }
    val barSpace: Dp = if (showBar) 88.dp else 0.dp

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = barSpace)) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            NavHost(nav, startDestination = start, modifier = Modifier.fillMaxSize()) {
                composable(Routes.HOME) { HomeScreen(user, onOpenBusiness = { nav.navigate(Routes.business(it)) }, bottomPadding = barSpace) }
                composable(Routes.TURNS) {
                    MyTurnsScreen(onOpenTurn = { nav.navigate(Routes.turn(it)) }, onExplore = { nav.navigateTab(Routes.HOME) }, bottomPadding = barSpace)
                }
                composable(Routes.NOTIFS) {
                    NotificationsScreen(
                        vm,
                        onOpenTurn = { nav.navigate(Routes.turn(it)) },
                        onOpenBusiness = { id -> nav.navigate(if (user.isClient) Routes.business(id) else Routes.manage(id)) },
                        onOpenProfile = { nav.navigateTab(Routes.PROFILE) },
                        bottomPadding = barSpace,
                    )
                }
                composable(Routes.PROFILE) { ProfileScreen(user, vm, bottomPadding = barSpace) }
                composable(Routes.PANEL) {
                    PanelScreen(user, onManage = { nav.navigate(Routes.manage(it)) }, onCreate = { nav.navigate(Routes.editor()) }, bottomPadding = barSpace)
                }
                composable(
                    Routes.BUSINESS,
                    arguments = listOf(
                        navArgument("id") { type = NavType.IntType },
                        navArgument("sector") { type = NavType.StringType; nullable = true; defaultValue = null },
                    ),
                ) { e ->
                    val id = e.arguments!!.getInt("id")
                    BusinessScreen(
                        businessId = id,
                        initialSectorId = e.arguments?.getString("sector")?.toIntOrNull(),
                        user = user,
                        onBack = { nav.popBackStack() },
                        onTurnCreated = { turnId ->
                            nav.navigate(Routes.turn(turnId)) { popUpTo(Routes.BUSINESS) { inclusive = true } }
                        },
                    )
                }
                composable(Routes.TURN, arguments = listOf(navArgument("id") { type = NavType.IntType })) { e ->
                    TurnDetailScreen(e.arguments!!.getInt("id"), onBack = { nav.popBackStack() }, onOpenBusiness = { nav.navigate(Routes.business(it)) })
                }
                composable(Routes.MANAGE, arguments = listOf(navArgument("id") { type = NavType.IntType })) { e ->
                    val id = e.arguments!!.getInt("id")
                    ManageBusinessScreen(id, user, onBack = { nav.popBackStack() }, onEdit = { nav.navigate(Routes.editor(id)) })
                }
                composable(
                    Routes.EDITOR,
                    arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) { e ->
                    val id = e.arguments?.getString("id")?.toIntOrNull()
                    BusinessEditorScreen(id, onBack = { nav.popBackStack() }, onSaved = { savedId ->
                        if (id == null) nav.navigate(Routes.manage(savedId)) { popUpTo(Routes.PANEL) }
                        else nav.popBackStack()
                    })
                }
            }

            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                BottomBar(tabs, route, unread, showScan = true, onScan = ::launchScanner) { nav.navigateTab(it) }
            }
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Barra inferior flotante con el botón de escanear QR al centro. */
@Composable
private fun BottomBar(tabs: List<Tab>, current: String?, unread: Int, showScan: Boolean, onScan: () -> Unit, onSelect: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Surface(
            Modifier.fillMaxWidth().height(68.dp).shadow(18.dp, RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { i, t ->
                    if (showScan && i == tabs.size / 2) Spacer(Modifier.size(64.dp))
                    BarItem(t, selected = current == t.route, badge = if (t.route == Routes.NOTIFS) unread else 0) { onSelect(t.route) }
                }
            }
        }
        if (showScan) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-18).dp)
                    .size(64.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(TyColors.brandGradient)
                    .clickable(onClick = onScan),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.QrCodeScanner, "Escanear QR", tint = Color.White, modifier = Modifier.size(30.dp)) }
        }
    }
}

@Composable
private fun BarItem(t: Tab, selected: Boolean, badge: Int, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BadgedBox(badge = { if (badge > 0) Badge(containerColor = TyColors.Orange) { Text(if (badge > 9) "9+" else "$badge") } }) {
            Icon(t.icon, t.label, tint = color)
        }
        Text(t.label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        Box(Modifier.padding(top = 2.dp).size(width = if (selected) 18.dp else 0.dp, height = 3.dp).clip(RoundedCornerShape(2.dp)).background(color))
    }
}
