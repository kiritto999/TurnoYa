package com.yperez.turnoya.ui.owner

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.yperez.turnoya.R
import com.yperez.turnoya.data.Business
import com.yperez.turnoya.data.QrCode
import com.yperez.turnoya.data.QrRequest
import com.yperez.turnoya.data.Sector
import com.yperez.turnoya.data.apiCall
import com.yperez.turnoya.ui.Load
import com.yperez.turnoya.ui.LocalSnackbar
import com.yperez.turnoya.ui.components.EmptyState
import com.yperez.turnoya.ui.components.ErrorBox
import com.yperez.turnoya.ui.components.LoadingBox
import com.yperez.turnoya.ui.components.TyButton
import com.yperez.turnoya.ui.components.TyCard
import com.yperez.turnoya.ui.components.TyOutlinedButton
import com.yperez.turnoya.ui.rememberResource
import com.yperez.turnoya.util.formatDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Códigos QR únicos por negocio o sector (RF05, RNF07), con póster listo para imprimir. */
@Composable
fun QrTab(b: Business, sectors: List<Sector>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    val codes = rememberResource(b.id, "qr") { apiCall { qrCodes(b.id) } }
    var sectorId by remember { mutableStateOf<Int?>(null) }
    var creating by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Imprime el QR y pégalo en tu mostrador. El cliente lo escanea con TurnoYa y llega directo a pedir su turno.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (sectors.isNotEmpty()) {
            item {
                Text("Generar QR para:", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChipTy("Todo el negocio", sectorId == null) { sectorId = null } }
                    items(sectors) { s -> FilterChipTy(s.nombre, sectorId == s.id) { sectorId = s.id } }
                }
            }
        }
        item {
            TyButton("Generar código QR", icon = Icons.Default.QrCode2, loading = creating, modifier = Modifier.fillMaxWidth(), onClick = {
                scope.launch {
                    creating = true
                    apiCall { createQr(b.id, QrRequest(sectorId)) }
                        .onSuccess { codes.reload(silent = true) }
                        .onFailure { snackbar.showSnackbar(it.message ?: "Error") }
                    creating = false
                }
            })
        }
        when (val s = codes.state) {
            is Load.Loading -> item { Box(Modifier.height(200.dp)) { LoadingBox() } }
            is Load.Err -> item { ErrorBox(s.message, onRetry = { codes.reload() }) }
            is Load.Ok -> if (s.data.isEmpty()) {
                item { EmptyState(Icons.Default.QrCode2, "Aún no tienes códigos", "Genera uno para tu negocio o para cada sector.") }
            } else items(s.data, key = { it.id }) { qr ->
                val sectorName = sectors.firstOrNull { it.id == qr.sectorId }?.nombre
                QrCard(qr, sectorName) {
                    scope.launch {
                        runCatching { shareQrPoster(context, b, sectorName, qr) }
                            .onFailure { snackbar.showSnackbar("No se pudo compartir: ${it.message}") }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCard(qr: QrCode, sectorName: String?, onShare: () -> Unit) {
    val context = LocalContext.current
    val bmp = remember(qr.codigo) { qrBitmap(qrContent(qr), 600, BitmapFactory.decodeResource(context.resources, R.drawable.logo_marca)) }
    TyCard {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(sectorName ?: "Todo el negocio", style = MaterialTheme.typography.titleMedium)
            Text("Creado ${formatDateTime(qr.fecha)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(androidx.compose.ui.graphics.Color.White).padding(10.dp)) {
                Image(bmp.asImageBitmap(), "Código QR", Modifier.size(200.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row { TyOutlinedButton("Compartir / imprimir", onShare, icon = Icons.Default.Share) }
        }
    }
}

fun qrContent(qr: QrCode) = qr.urlDestino.ifBlank { "turnoya://qr/${qr.codigo}" }

/** QR con corrección alta para poder poner el logo al centro sin perder lectura. */
fun qrBitmap(content: String, size: Int, mark: Bitmap? = null): Bitmap {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H, EncodeHintType.MARGIN to 1)
    val matrix = com.google.zxing.MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bmp = BarcodeEncoder().createBitmap(matrix).copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bmp)
    val box = size * 0.22f
    val l = (size - box) / 2f
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    canvas.drawRoundRect(RectF(l - 6, l - 6, l + box + 6, l + box + 6), 18f, 18f, p)
    if (mark != null) {
        val mw = box * 0.86f
        val mh = mw * mark.height / mark.width
        canvas.drawBitmap(mark, null, RectF((size - mw) / 2f, (size - mh) / 2f, (size + mw) / 2f, (size + mh) / 2f), Paint(Paint.FILTER_BITMAP_FLAG))
    }
    return bmp
}

private suspend fun shareQrPoster(context: Context, b: Business, sectorName: String?, qr: QrCode) {
    val file = withContext(Dispatchers.Default) {
        val w = 1240
        val h = 1754 // proporción A4
        val poster = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(poster)
        c.drawColor(android.graphics.Color.WHITE)
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_turnoya)
        val lw = 520f
        val lh = lw * logo.height / logo.width
        c.drawBitmap(logo, null, RectF((w - lw) / 2, 90f, (w + lw) / 2, 90f + lh), Paint(Paint.FILTER_BITMAP_FLAG))
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; color = 0xFF16213A.toInt() }
        text.textSize = 72f; text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        var y = 90f + lh + 120f
        c.drawText(b.nombre.take(28), w / 2f, y, text)
        if (sectorName != null) {
            text.textSize = 48f; text.color = 0xFF0A6FCB.toInt(); y += 70f
            c.drawText(sectorName.take(36), w / 2f, y, text)
        }
        val qrSize = 820
        val qrBmp = qrBitmap(qrContent(qr), qrSize)  // el logo se dibuja abajo, más grande
        y += 60f
        c.drawBitmap(qrBmp, (w - qrSize) / 2f, y, null)
        val mark = BitmapFactory.decodeResource(context.resources, R.drawable.logo_marca)
        val ms = qrSize * 0.17f
        val mh = ms * mark.height / mark.width
        c.drawBitmap(mark, null, RectF((w - ms) / 2, y + (qrSize - mh) / 2, (w + ms) / 2, y + (qrSize + mh) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
        y += qrSize + 110f
        text.color = 0xFF16213A.toInt(); text.textSize = 60f
        c.drawText("Escanea y pide tu turno", w / 2f, y, text)
        text.typeface = Typeface.DEFAULT; text.textSize = 40f; text.color = 0xFF6B7A99.toInt(); y += 64f
        c.drawText("Sin filas · te avisamos cuando sea tu turno", w / 2f, y, text)
        val dir = File(context.cacheDir, "qr").apply { mkdirs() }
        File(dir, "turnoya_qr_${b.id}_${qr.id}.png").also { f -> f.outputStream().use { poster.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Pide tu turno en ${b.nombre} con TurnoYa")
        clipData = android.content.ClipData.newRawUri("QR", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Compartir código QR"))
}
