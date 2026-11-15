package com.example.notifications

import com.example.database.DataStoreManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FamiMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank()) return
        serviceScope.launch {
            runCatching {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@runCatching
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
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val choreId = data["choreId"].orEmpty()
        val title = data["title"].orEmpty().ifBlank { remoteMessage.notification?.title ?: "Nueva tarea" }
        val assignedTo = data["assignedTo"].orEmpty().ifBlank { "Todos" }
        val createdBy = data["createdBy"].orEmpty()
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
