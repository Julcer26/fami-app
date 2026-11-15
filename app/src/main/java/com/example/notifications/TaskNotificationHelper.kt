package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.database.Chore
import java.text.Normalizer

object TaskNotificationHelper {
    const val CHANNEL_ID = "fami_tasks_channel"
    private const val PREFS_NAME = "fami_task_notifications_prefs"
    private const val KEY_NOTIFIED_IDS = "notified_chore_ids"
    private const val KEY_SEEDED_FAMILIES = "seeded_families"

    fun normalizeAlias(raw: String): String {
        val base = raw.trim().substringBefore("@").trim()
        val normalized = Normalizer.normalize(base, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return normalized.lowercase()
    }

    fun isAssignedToAll(assignedTo: String): Boolean {
        val clean = normalizeAlias(assignedTo)
        return clean == "todos" || clean == "all" || clean == "familia"
    }

    fun isChoreTargetedToUser(chore: Chore, currentUser: String): Boolean {
        if (chore.isPrivate || chore.isCompleted) return false
        val currentClean = normalizeAlias(currentUser)
        if (currentClean.isBlank()) return false

        val creatorClean = normalizeAlias(chore.createdBy)
        if (creatorClean.isNotBlank() && creatorClean == currentClean) {
            return false
        }

        val assignedClean = normalizeAlias(chore.assignedTo)
        if (assignedClean.isBlank()) return false

        return isAssignedToAll(assignedClean) || assignedClean == currentClean
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Tareas del Hogar",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Avisos cuando alguien te asigna una tarea o crea una tarea para todos."
                    enableVibration(true)
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isFamilySeeded(context: Context, familyId: String): Boolean {
        val set = prefs(context).getStringSet(KEY_SEEDED_FAMILIES, emptySet()) ?: emptySet()
        return set.contains(familyId)
    }

    fun markFamilySeeded(context: Context, familyId: String, existingIds: Collection<String>) {
        val p = prefs(context)
        val seeded = (p.getStringSet(KEY_SEEDED_FAMILIES, emptySet()) ?: emptySet()).toMutableSet()
        seeded.add(familyId)
        val known = (p.getStringSet(KEY_NOTIFIED_IDS, emptySet()) ?: emptySet()).toMutableSet()
        known.addAll(existingIds.filter { it.isNotBlank() })
        val trimmedKnown = if (known.size > 300) known.toList().takeLast(250).toSet() else known
        p.edit()
            .putStringSet(KEY_SEEDED_FAMILIES, seeded)
            .putStringSet(KEY_NOTIFIED_IDS, trimmedKnown)
            .apply()
    }

    fun hasNotifiedChore(context: Context, choreId: String): Boolean {
        if (choreId.isBlank()) return false
        val set = prefs(context).getStringSet(KEY_NOTIFIED_IDS, emptySet()) ?: emptySet()
        return set.contains(choreId)
    }

    fun markChoreNotified(context: Context, choreId: String) {
        if (choreId.isBlank()) return
        val p = prefs(context)
        val known = (p.getStringSet(KEY_NOTIFIED_IDS, emptySet()) ?: emptySet()).toMutableSet()
        known.add(choreId)
        val trimmed = if (known.size > 300) known.toList().takeLast(250).toSet() else known
        p.edit().putStringSet(KEY_NOTIFIED_IDS, trimmed).apply()
    }

    fun showTaskNotification(
        context: Context,
        choreId: String,
        title: String,
        assignedTo: String,
        createdBy: String,
        priority: String = "Medio"
    ) {
        if (choreId.isNotBlank() && hasNotifiedChore(context, choreId)) return
        ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", "tareas")
            putExtra("chore_id", choreId)
        }
        val reqCode = (choreId.ifBlank { title }).hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            reqCode,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isForAll = isAssignedToAll(assignedTo)
        val notifTitle = if (isForAll) {
            "🏡 Nueva tarea para Todos"
        } else {
            "📌 Nueva tarea asignada a ti"
        }
        val creatorLabel = createdBy.trim().substringBefore("@").ifBlank { "Un familiar" }
        val prioTag = priority.ifBlank { "Medio" }
        val notifBody = "$creatorLabel agregó: \"$title\" (Prioridad: $prioTag)"

        val largeIcon = runCatching {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        }.getOrNull()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_fami_notif)
            .setColor(0xFFFF6B57.toInt())
            .setContentTitle(notifTitle)
            .setContentText(notifBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(reqCode, builder.build())
            if (choreId.isNotBlank()) {
                markChoreNotified(context, choreId)
            }
        }
    }
}
