package com.example.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.database.Chore
import com.example.database.DataStoreManager
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class ChoreSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val currentUserAuth = FirebaseAuth.getInstance().currentUser ?: return Result.success()
        val dsm = DataStoreManager(applicationContext)
        val familyId = dsm.familyIdFlow.firstOrNull().orEmpty()
        val userName = dsm.userNameFlow.firstOrNull().orEmpty()
        if (familyId.isBlank() || userName.isBlank() || currentUserAuth.uid.isBlank()) {
            return Result.success()
        }

        return try {
            val snapshot = FirebaseDatabase.getInstance().reference
                .child("families")
                .child(familyId)
                .child("chores")
                .get()
                .await()

            val chores = mutableListOf<Chore>()
            for (child in snapshot.children) {
                val chore = child.getValue(Chore::class.java) ?: continue
                val key = child.key ?: continue
                chores.add(chore.copy(id = key))
            }

            if (!TaskNotificationHelper.isFamilySeeded(applicationContext, familyId)) {
                TaskNotificationHelper.markFamilySeeded(
                    applicationContext,
                    familyId,
                    chores.map { it.id }
                )
                return Result.success()
            }

            chores.forEach { chore ->
                if (TaskNotificationHelper.isChoreTargetedToUser(chore, userName) &&
                    !TaskNotificationHelper.hasNotifiedChore(applicationContext, chore.id)
                ) {
                    TaskNotificationHelper.showTaskNotification(
                        context = applicationContext,
                        choreId = chore.id,
                        title = chore.title,
                        assignedTo = chore.assignedTo,
                        createdBy = chore.createdBy,
                        priority = chore.priority
                    )
                }
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isTransientNetworkError(e) && runAttemptCount < MAX_TRANSIENT_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun isTransientNetworkError(error: Throwable): Boolean {
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < 8) {
            if (current is SecurityException) return false
            if (current is DatabaseException) {
                val msg = current.message.orEmpty().lowercase()
                if (msg.contains("permission")) return false
                if (msg.contains("network") || msg.contains("disconnected") || msg.contains("timeout") || msg.contains("unavailable")) {
                    return true
                }
            }
            if (current is FirebaseNetworkException ||
                current is IOException ||
                current is SocketTimeoutException ||
                current is UnknownHostException
            ) {
                return true
            }
            val msg = current.message.orEmpty().lowercase()
            if (msg.contains("network") ||
                msg.contains("timeout") ||
                msg.contains("timed out") ||
                msg.contains("unable to resolve host") ||
                msg.contains("failed to connect") ||
                msg.contains("connection reset") ||
                msg.contains("disconnected")
            ) {
                return true
            }
            current = current.cause
            depth++
        }
        return false
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "fami_chore_sync_worker"
        private const val MAX_TRANSIENT_RETRIES = 5

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<ChoreSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
