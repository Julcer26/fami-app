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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
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

        return runCatching {
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
                return@runCatching Result.success()
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
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "fami_chore_sync_worker"

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
