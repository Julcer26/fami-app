package com.example.notifications

import com.example.database.DataStoreManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FamiMessagingService : FirebaseMessagingService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank()) return
        serviceScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val dsm = DataStoreManager(applicationContext)
                val familyId = dsm.familyIdFlow.firstOrNull().orEmpty()
                val userName = dsm.userNameFlow.firstOrNull().orEmpty()
                if (familyId.isNotBlank() && userName.isNotBlank()) {
                    val payload = mapOf(
                        "token" to token,
                        "userName" to userName,
                        "normalizedName" to TaskNotificationHelper.normalizeAlias(userName),
                        "uid" to uid,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    FirebaseDatabase.getInstance().reference
                        .child("families")
                        .child(familyId)
                        .child("fcm_tokens")
                        .child(uid)
                        .setValue(payload)
                        .await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val choreId = data["choreId"].orEmpty().ifBlank { data["chore_id"].orEmpty() }
        val title = data["title"].orEmpty().ifBlank { remoteMessage.notification?.title ?: "Nueva tarea" }
        val assignedTo = data["assignedTo"].orEmpty()
            .ifBlank { data["assigned_to"].orEmpty() }
            .ifBlank { "Todos" }
        val createdBy = data["createdBy"].orEmpty().ifBlank { data["created_by"].orEmpty() }
        val priority = data["priority"].orEmpty().ifBlank { "Medio" }

        // Si ya fue notificada por el listener en tiempo real, evitamos duplicados
        if (choreId.isNotBlank() && TaskNotificationHelper.hasNotifiedChore(applicationContext, choreId)) {
            return
        }

        TaskNotificationHelper.showTaskNotification(
            context = applicationContext,
            choreId = choreId,
            title = title,
            assignedTo = assignedTo,
            createdBy = createdBy,
            priority = priority
        )
    }
}
