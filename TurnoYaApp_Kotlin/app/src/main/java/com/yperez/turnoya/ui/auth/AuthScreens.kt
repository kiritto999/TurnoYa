package com.yperez.turnoya.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yperez.turnoya.data.Roles
import com.yperez.turnoya.ui.AppViewModel
import com.yperez.turnoya.ui.components.TyButton
import com.yperez.turnoya.ui.components.TyLogo
import com.yperez.turnoya.ui.components.TyTextField
import com.yperez.turnoya.ui.theme.TyColors
import kotlinx.coroutines.launch

/** Encabezado oscuro con el logotipo, común a ingreso y registro. */
@Composable
private fun AuthScaffold(
    vm: AppViewModel,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    var showServer by remember { mutableStateOf(false) }
    com.yperez.turnoya.ui.components.LightStatusBarIcons()
    Box(Modifier.fillMaxSize().background(TyColors.headerGradient)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 24.dp, bottom = 28.dp), contentAlignment = Alignment.Center) {
                TyLogo(dark = true, height = 120.dp)
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = LocalConfiguration.current.screenHeightDp.dp * 0.7f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp, vertical = 28.dp)
                    .navigationBarsPadding(),
            ) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                content()
            }
        }
        IconButton(onClick = { showServer = true }, modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Settings, "Servidor", tint = Color.White.copy(alpha = 0.7f))
        }
    }
    if (showServer) ServerDialog(vm) { showServer = false }
}

@Composable
fun LoginScreen(vm: AppViewModel, onRegister: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AuthScaffold(vm, "¡Hola de nuevo! 👋", "Ingresa para pedir y seguir tus turnos sin hacer fila.") {
        TyTextField(email, { email = it; error = null }, "Correo electrónico", leading = Icons.Default.Email, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        TyTextField(password, { password = it; error = null }, "Contraseña", leading = Icons.Default.Lock, isPassword = true)
        AnimatedVisibility(error != null) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.height(24.dp))
        TyButton(
            "Ingresar",
            onClick = {
                if (email.isBlank() || password.isBlank()) { error = "Escribe tu correo y contraseña"; return@TyButton }
                scope.launch {
                    loading = true
                    vm.login(email, password).onFailure { error = it.message }
                    loading = false
                }
            },
            loading = loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("¿No tienes cuenta?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onRegister) { Text("Regístrate", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun RegisterScreen(vm: AppViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var nombre by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var rol by rememberSaveable { mutableStateOf(Roles.CLIENT) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AuthScaffold(vm, "Crea tu cuenta", "Elige cómo vas a usar TurnoYa.") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoleCard("Soy cliente", "Pido turnos y los sigo", Icons.Default.Person, rol == Roles.CLIENT, Modifier.weight(1f)) { rol = Roles.CLIENT }
            RoleCard("Tengo un negocio", "Gestiono mi fila", Icons.Default.Storefront, rol == Roles.OWNER, Modifier.weight(1f)) { rol = Roles.OWNER }
        }
        Spacer(Modifier.height(20.dp))
        TyTextField(nombre, { nombre = it; error = null }, "Nombre completo", leading = Icons.Default.Person)
        Spacer(Modifier.height(12.dp))
        TyTextField(email, { email = it; error = null }, "Correo electrónico", leading = Icons.Default.Email, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        TyTextField(password, { password = it; error = null }, "Contraseña (mín. 6 caracteres)", leading = Icons.Default.Lock, isPassword = true)
        Spacer(Modifier.height(12.dp))
        TyTextField(confirm, { confirm = it; error = null }, "Confirmar contraseña", leading = Icons.Default.Lock, isPassword = true)
        AnimatedVisibility(error != null) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.height(24.dp))
        TyButton(
            "Crear cuenta",
            onClick = {
                error = when {
                    nombre.trim().length < 2 -> "Escribe tu nombre"
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "El correo no es válido"
                    password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
                    password != confirm -> "Las contraseñas no coinciden"
                    else -> null
                }
                if (error != null) return@TyButton
                scope.launch {
                    loading = true
                    vm.register(nombre, email, password, rol).onFailure { error = it.message }
                    loading = false
                }
            },
            loading = loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Ya tengo cuenta, ingresar") }
    }
}

@Composable
private fun RoleCard(title: String, subtitle: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(if (selected) TyColors.ctaGradient else TyColors.brandGradient),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Color.White) }
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Permite cambiar la URL del servidor (el túnel de Cloudflare cambia al reiniciar la VM). */
@Composable
fun ServerDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val current by vm.serverUrl.collectAsState()
    var url by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Dns, null) },
        title = { Text("Servidor de TurnoYa") },
        text = {
            Column {
                Text(
                    "Pega aquí la URL del servidor. En la VM puedes verla con el comando turnoya-url.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                TyTextField(url, { url = it }, "URL", keyboardType = KeyboardType.Uri, placeholder = "https://...trycloudflare.com")
                Spacer(Modifier.height(4.dp))
                Text("Emulador con la VM local: http://10.0.2.2:8080", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { vm.setServerUrl(url); onDismiss() }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
