package com.yperez.turnoya.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yperez.turnoya.R
import com.yperez.turnoya.ui.theme.TyColors

@Composable
fun TyLogo(modifier: Modifier = Modifier, dark: Boolean = false, height: Dp = 96.dp) {
    Image(
        painter = painterResource(if (dark) R.drawable.logo_turnoya_dark else R.drawable.logo_turnoya),
        contentDescription = "TurnoYa",
        modifier = modifier.height(height),
    )
}

@Composable
fun TyMark(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Image(painterResource(R.drawable.logo_marca), contentDescription = null, modifier = modifier.size(size))
}

/** Botón principal con degradado de marca. */
@Composable
fun TyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    brush: Brush = TyColors.ctaGradient,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(brush, RoundedCornerShape(16.dp))
                .alpha(if (enabled) 1f else 0.5f),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun TyOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = MaterialTheme.colorScheme.primary) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.6f)),
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    error: String? = null,
    placeholder: String? = null,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
        leadingIcon = leading?.let { { Icon(it, null) } },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Mostrar contraseña")
                }
            }
        } else null,
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = minLines,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TyTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            TyOutlinedButton("Reintentar", onRetry)
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/** Píldora de color para el estado de un turno. */
@Composable
fun StatusPill(estado: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (estado) {
        "pendiente" -> Color(0xFFD6E8FB) to TyColors.BlueDark
        "llamado" -> Color(0xFFFFE6D1) to Color(0xFFB24A00)
        "atendiendo" -> Color(0xFFDDF7EA) to Color(0xFF0E7A4E)
        "atendido" -> Color(0xFFE6ECF5) to TyColors.Muted
        else -> Color(0xFFFDE2E3) to TyColors.Danger
    }
    Text(
        com.yperez.turnoya.util.turnStateLabel(estado),
        color = fg,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp),
            )
        }
    }
}

@Composable
fun TyCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
    ) { content() }
}

/** Avatar circular con la inicial del negocio y un color estable según la categoría. */
@Composable
fun BusinessAvatar(name: String, category: String?, size: Dp = 52.dp) {
    val emoji = categoryEmoji(category)
    Box(
        Modifier.size(size).clip(RoundedCornerShape(16.dp)).background(categoryBrush(category)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji ?: name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun PulseDot(color: Color, size: Dp = 10.dp) {
    val t = rememberInfiniteTransition(label = "pulse")
    val a by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Box(Modifier.size(size).alpha(a).clip(CircleShape).background(color))
}
