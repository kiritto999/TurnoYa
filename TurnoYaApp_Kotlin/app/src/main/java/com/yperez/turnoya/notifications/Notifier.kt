package com.yperez.turnoya.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yperez.turnoya.MainActivity
import com.yperez.turnoya.R
import com.yperez.turnoya.TurnoYaApp
import com.yperez.turnoya.data.ApiClient
import com.yperez.turnoya.data.apiCall
import java.util.concurrent.TimeUnit

/** Notificaciones locales (RF12/RF13) a partir de las notificaciones guardadas en la API. */
object Notifier {
    private const val CHANNEL = "turnos"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_HIGH)
            ch.description = context.getString(R.string.notif_channel_desc)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, id: Int, title: String, text: String) {
        if (!canNotify(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_NOTIFICATIONS)
        }
        val pi = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_turnoya)
            .setColor(0xFF0A6FCB.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    /** Consulta la API y muestra como notificación del sistema las no leídas que aún no se mostraron. */
    suspend fun checkNow(context: Context) {
        val store = TurnoYaApp.instance.sessionStore
        val session = store.currentSession() ?: return
        ApiClient.token = session.token
        ApiClient.baseUrl = store.currentServerUrl()
        val list = apiCall { notifications() }.getOrNull() ?: return
        val last = store.lastNotifiedId()
        // Primera revisión tras ingresar: no inundar con el historial, solo marca hasta dónde se vio
        if (last == null) {
            store.setLastNotifiedId(list.maxOfOrNull { it.id } ?: 0)
            return
        }
        val fresh = list.filter { !it.leido && it.id > last }.sortedBy { it.id }
        fresh.takeLast(3).forEach { n ->
            val title = when (n.tipo) {
                "nuevo_turno" -> "Nueva solicitud de turno"
                "invitacion" -> "Invitación de trabajo"
                else -> "Tu turno"
            }
            show(context, n.id, title, n.mensaje)
        }
        fresh.maxOfOrNull { it.id }?.let { store.setLastNotifiedId(it) }
    }
}

/** Revisa notificaciones en segundo plano (mínimo cada 15 min por límites de Android). */
class NotificationWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Notifier.checkNow(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("turnoya-notifs", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
