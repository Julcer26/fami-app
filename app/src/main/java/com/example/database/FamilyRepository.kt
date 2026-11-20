package com.example.database

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.api.BcvExchangeRateService
import com.example.currency.CountryCurrencyConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction as RtdbTransaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

class FamilyRepository {

    val db = FirebaseDatabase.getInstance().apply {
        try {
            setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Persistence already enabled
        }
    }.reference

    val _familyId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    
    fun setFamilyId(id: String?) {
        _familyId.value = id?.takeIf { it.isNotBlank() }
    }

    suspend fun registerUserMapping(uid: String, familyId: String, userName: String) {
        val updates = mapOf(
            "familyId" to familyId,
            "userName" to userName
        )
        db.child("users").child(uid).setValue(updates).await()
    }

    suspend fun registerFamilyMember(familyId: String, uid: String) {
        db.child("families").child(familyId).child("members").child(uid).setValue(true).await()
    }

    suspend fun getUserMapping(uid: String): DataSnapshot {
        return db.child("users").child(uid).get().await()
    }

    suspend fun checkFamilyExists(familyId: String): Boolean {
        val snapshot = db.child("families").child(familyId).get().await()
        return snapshot.exists()
    }

    suspend fun registerFcmToken(token: String, userName: String) {
        if (token.isBlank() || userName.isBlank()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val fid = _familyId.value ?: return
        val payload = mapOf(
            "token" to token,
            "userName" to userName,
            "normalizedName" to com.example.notifications.TaskNotificationHelper.normalizeAlias(userName),
            "uid" to uid,
            "updatedAt" to System.currentTimeMillis()
        )
        runCatching {
            db.child("families").child(fid).child("fcm_tokens").child(uid).setValue(payload).await()
        }
    }

    private fun getFamilyRootRef(): com.google.firebase.database.DatabaseReference? {
        val fid = _familyId.value ?: return null
        return db.child("families").child(fid)
    }

    private fun getFamilyRef(childNode: String): com.google.firebase.database.DatabaseReference? {
        val fid = _familyId.value ?: return null
        return db.child("families").child(fid).child(childNode)
    }

    private suspend fun runRtdbTransaction(
        ref: com.google.firebase.database.DatabaseReference,
        transform: (MutableData) -> RtdbTransaction.Result
    ): DataSnapshot? = suspendCancellableCoroutine { cont ->
        ref.runTransaction(object : RtdbTransaction.Handler {
            override fun doTransaction(currentData: MutableData): RtdbTransaction.Result {
                return transform(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (!cont.isActive) return
                if (error != null) {
                    cont.resumeWithException(error.toException())
                } else if (!committed) {
                    cont.resumeWithException(IllegalStateException("Transacción RTDB abortada"))
                } else {
                    cont.resume(currentData)
                }
            }
        })
    }

    inline fun <reified T> observeList(childNode: String): Flow<List<T>> = _familyId.flatMapLatest { fid ->
        if (fid == null) return@flatMapLatest flowOf(emptyList())
        val ref = db.child("families").child(fid).child(childNode)
        callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<T>()
                    for (child in snapshot.children) {
                        val obj = child.getValue(T::class.java)
                        if (obj != null) {
                            val key = child.key ?: ""
                            val finalObj = when {
                                obj is Transaction -> (obj as Transaction).copy(id = key) as T
                                obj is Chore -> (obj as Chore).copy(id = key) as T
                                obj is ShoppingItem -> (obj as ShoppingItem).copy(id = key) as T
                                obj is ActivityLog -> (obj as ActivityLog).copy(id = key) as T
                                obj is CategoryDetail -> (obj as CategoryDetail).copy(categoryName = key) as T
                                obj is RecurringPayment -> (obj as RecurringPayment).copy(id = key) as T
                                obj is Goal -> (obj as Goal).copy(id = key) as T
                                obj is Note -> (obj as Note).copy(id = key) as T
                                obj is Wallet -> (obj as Wallet).copy(id = key) as T
                                else -> obj
                            }
                            
                            // Let's use reflection as a fallback just in case:
                            try {
                                val field = finalObj!!::class.java.getDeclaredField("id")
                                field.isAccessible = true
                                val currentId = field.get(finalObj) as? String
                                if (currentId.isNullOrEmpty()) {
                                    field.set(finalObj, key)
                                }
                            } catch (e: Exception) {
                                // Ignore
                            }
                            list.add(finalObj)
                        }
                    }
                    trySend(list)
                }
                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }
    }

    val allTransactions: Flow<List<Transaction>> = observeList("transactions")
    val allCategories: Flow<List<CategoryDetail>> = observeList("categories")
    val allChores: Flow<List<Chore>> = observeList("chores")
    val allShoppingItems: Flow<List<ShoppingItem>> = observeList("shopping_items")
    val allProfiles: Flow<List<UserProfile>> = observeList("user_profiles")
    val allSettings: Flow<List<AppSetting>> = observeList("app_settings")
    val allLogs: Flow<List<ActivityLog>> = observeList("activity_logs")
    val allGoals: Flow<List<Goal>> = observeList("goals")
    val allNotes: Flow<List<Note>> = observeList("notes")
    val allWallets: Flow<List<Wallet>> = observeList("wallets")
    val allRecurringPayments: Flow<List<RecurringPayment>> = observeList("recurring_payments")

    suspend fun insertRecurringPayment(payment: RecurringPayment, user: String) {
        val ref = getFamilyRef("recurring_payments") ?: return
        val pushRef = if (payment.id.isEmpty()) ref.push() else ref.child(payment.id)
        val finalized = payment.copy(id = pushRef.key ?: "")
        pushRef.setValue(finalized).await()
        insertLog(ActivityLog(tag = "FINANZAS", message = "$user configuró el pago recurrente '${payment.name}'", user = user))
    }

    suspend fun deleteRecurringPayment(payment: RecurringPayment, user: String) {
        val ref = getFamilyRef("recurring_payments") ?: return
        if (payment.id.isNotEmpty()) {
            ref.child(payment.id).removeValue().await()
            insertLog(ActivityLog(tag = "FINANZAS", message = "$user eliminó el pago recurrente '${payment.name}'", user = user))
        }
    }

    suspend fun markRecurringPaymentPaid(
        payment: RecurringPayment,
        monthKey: String,
        amount: Double,
        currency: String,
        date: Long,
        user: String,
        walletId: String = ""
    ): String? {
        val familyRoot = getFamilyRootRef() ?: return "No hay familia activa"
        val normalizedCurrency = CountryCurrencyConfig.normalizeCurrencyCode(currency)
        val rateToUse = resolveUsdRate(normalizedCurrency, date)
        val parsedAmountInUsd = if (normalizedCurrency == "USD") amount else amount / rateToUse

        val txKey = familyRoot.child("transactions").push().key
            ?: throw IllegalStateException("No se pudo generar ID de transacción")
        val payId = payment.id.ifBlank {
            familyRoot.child("recurring_payments").push().key
                ?: throw IllegalStateException("No se pudo generar ID de pago recurrente")
        }
        val logKey = familyRoot.child("activity_logs").push().key
            ?: UUID.randomUUID().toString()

        val finalizedTx = Transaction(
            id = txKey,
            amount = parsedAmountInUsd,
            category = payment.category.ifBlank { "Servicios" },
            description = "${payment.name} ($monthKey)",
            currency = normalizedCurrency,
            originalAmount = amount,
            bcvRateUsed = rateToUse,
            isIncome = false,
            isRecurring = true,
            date = date,
            balanceDate = date,
            registeredBy = user,
            walletId = walletId
        )

        val newPaidMonths = payment.paidMonths.toMutableMap().apply { put(monthKey, true) }
        val newPaidTxIds = payment.paidTransactionIds.toMutableMap().apply { put(monthKey, txKey) }
        val updatedPayment = payment.copy(
            id = payId,
            paidMonths = newPaidMonths,
            paidTransactionIds = newPaidTxIds
        )

        // Pre-resolve wallet conversion rate if wallet currency differs before mutating DB
        val preResolvedWalletRate = preResolveWalletConversionRate(walletId, normalizedCurrency)

        val sym = CountryCurrencyConfig.getCurrencySymbol(normalizedCurrency)
        val payLog = ActivityLog(
            id = logKey,
            timestamp = System.currentTimeMillis(),
            tag = "FINANZAS",
            message = "$user registró pago de '${payment.name}' ($monthKey): $sym ${String.format(Locale.US, "%.2f", amount)} $normalizedCurrency",
            user = user
        )

        val multiPath = mapOf<String, Any?>(
            "transactions/$txKey" to finalizedTx,
            "recurring_payments/$payId" to updatedPayment,
            "activity_logs/$logKey" to payLog
        )
        familyRoot.updateChildren(multiPath).await()

        if (walletId.isNotBlank()) {
            try {
                applyWalletBalanceDelta(
                    walletId = walletId,
                    txAmountUsd = finalizedTx.amount,
                    txOriginalAmount = finalizedTx.originalAmount,
                    txCurrency = finalizedTx.currency,
                    sign = -1.0,
                    preResolvedTargetRate = preResolvedWalletRate
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                runCatching {
                    familyRoot.updateChildren(
                        mapOf<String, Any?>(
                            "transactions/$txKey" to null,
                            "recurring_payments/$payId" to payment
                        )
                    ).await()
                }
                throw e
            }
        }

        val effectiveDate = if (finalizedTx.balanceDate > 0L) finalizedTx.balanceDate else finalizedTx.date
        return checkBudgetAlert(finalizedTx.category, user, effectiveDate)
    }

    suspend fun markRecurringPaymentUnpaid(
        payment: RecurringPayment,
        monthKey: String,
        user: String
    ) {
        val familyRoot = getFamilyRootRef() ?: return
        if (payment.id.isBlank()) return

        val txId = payment.paidTransactionIds[monthKey].orEmpty()
        val storedTx = if (txId.isNotBlank()) {
            familyRoot.child("transactions").child(txId).get().await().getValue(Transaction::class.java)
        } else null

        val preResolvedWalletRate = if (storedTx != null && storedTx.walletId.isNotBlank() && !storedTx.isTransfer) {
            preResolveWalletConversionRate(storedTx.walletId, storedTx.currency)
        } else 0.0

        val newPaidMonths = payment.paidMonths.toMutableMap().apply { remove(monthKey) }
        val newPaidTxIds = payment.paidTransactionIds.toMutableMap().apply { remove(monthKey) }
        val updatedPayment = payment.copy(
            paidMonths = newPaidMonths,
            paidTransactionIds = newPaidTxIds
        )

        val logKey = familyRoot.child("activity_logs").push().key ?: UUID.randomUUID().toString()
        val unpaidLog = ActivityLog(
            id = logKey,
            timestamp = System.currentTimeMillis(),
            tag = "FINANZAS",
            message = "$user desmarcó el pago de '${payment.name}' ($monthKey)",
            user = user
        )

        val multiPath = mutableMapOf<String, Any?>(
            "recurring_payments/${payment.id}" to updatedPayment,
            "activity_logs/$logKey" to unpaidLog
        )
        if (txId.isNotBlank()) {
            multiPath["transactions/$txId"] = null
        }
        familyRoot.updateChildren(multiPath).await()

        if (storedTx != null && storedTx.walletId.isNotBlank() && !storedTx.isTransfer) {
            val revertSign = if (storedTx.isIncome) -1.0 else 1.0
            applyWalletBalanceDelta(
                walletId = storedTx.walletId,
                txAmountUsd = storedTx.amount,
                txOriginalAmount = storedTx.originalAmount,
                txCurrency = storedTx.currency,
                sign = revertSign,
                preResolvedTargetRate = preResolvedWalletRate
            )
        }

        if (storedTx != null && storedTx.receiptUrl.startsWith("https://")) {
            runCatching {
                FirebaseStorage.getInstance().getReferenceFromUrl(storedTx.receiptUrl).delete().await()
            }
        }
    }

    private suspend fun resolveUsdRate(
        currency: String,
        date: Long,
        fallbackRate: Double = 0.0
    ): Double {
        val clean = CountryCurrencyConfig.normalizeCurrencyCode(currency)
        if (clean == "USD") return 1.0
        if (clean == "VES") {
            val historicRate = BcvExchangeRateService.fetchRateForDate(date)
            if (historicRate > 0.0) return historicRate
            val cached = BcvExchangeRateService.getCachedRate("VES")
            if (cached != null && cached > 0.0) return cached
            val savedRate = getSettingValueByKey("bcv_rate")?.toDoubleOrNull() ?: 0.0
            if (savedRate > 0.0) return savedRate
            if (fallbackRate > 0.0) return fallbackRate
            throw IllegalStateException("No se pudo obtener la tasa de cambio BCV para VES. Verifica tu conexión.")
        }
        val fetched = BcvExchangeRateService.fetchUsdToCurrencyRate(clean)
        if (fetched > 0.0) return fetched
        val cached = BcvExchangeRateService.getCachedRate(clean)
        if (cached != null && cached > 0.0) return cached
        if (fallbackRate > 0.0) return fallbackRate
        throw IllegalStateException("No se pudo obtener la tasa de cambio para $clean. Verifica tu conexión.")
    }

    private suspend fun preResolveWalletConversionRate(walletId: String, txCurrency: String): Double {
        if (walletId.isBlank()) return 1.0
        val walletsRef = getFamilyRef("wallets") ?: return 1.0
        val wCurrRaw = walletsRef.child(walletId).child("currency").get().await().getValue(String::class.java)
            ?: return 1.0
        val wCurr = CountryCurrencyConfig.normalizeCurrencyCode(wCurrRaw)
        val normTx = CountryCurrencyConfig.normalizeCurrencyCode(txCurrency)
        if (wCurr == normTx || wCurr == "USD") return 1.0
        return resolveUsdRate(wCurr, System.currentTimeMillis())
    }

    private suspend fun applyWalletBalanceDelta(
        walletId: String,
        txAmountUsd: Double,
        txOriginalAmount: Double,
        txCurrency: String,
        sign: Double,
        preResolvedTargetRate: Double? = null
    ) {
        if (walletId.isBlank()) return
        val walletsRef = getFamilyRef("wallets") ?: return
        val targetRate = preResolvedTargetRate ?: preResolveWalletConversionRate(walletId, txCurrency)
        val normTxCurr = CountryCurrencyConfig.normalizeCurrencyCode(txCurrency)

        runRtdbTransaction(walletsRef.child(walletId)) { currentData ->
            if (currentData.value == null) {
                return@runRtdbTransaction RtdbTransaction.success(currentData)
            }
            val wCurrRaw = (currentData.child("currency").value as? String)?.takeIf { it.isNotBlank() } ?: "USD"
            val normWalletCurr = CountryCurrencyConfig.normalizeCurrencyCode(wCurrRaw)
            val delta = when {
                normWalletCurr == normTxCurr -> txOriginalAmount * sign
                normWalletCurr == "USD" -> txAmountUsd * sign
                else -> (txAmountUsd * targetRate) * sign
            }
            val currentBal = (currentData.child("balance").value as? Number)?.toDouble() ?: 0.0
            val newBalance = ((currentBal + delta) * 100.0).roundToInt() / 100.0
            currentData.child("balance").value = newBalance
            RtdbTransaction.success(currentData)
        }
    }

    suspend fun insertTransaction(
        transaction: Transaction,
        user: String,
        isNewEntry: Boolean = transaction.id.isEmpty()
    ): String? {
        val familyRoot = getFamilyRootRef() ?: return "No hay familia activa"
        val transactionsRef = familyRoot.child("transactions")
        val normalizedCurrency = CountryCurrencyConfig.normalizeCurrencyCode(transaction.currency)

        val previousTx: Transaction? = if (!isNewEntry && transaction.id.isNotEmpty()) {
            transactionsRef.child(transaction.id).get().await().getValue(Transaction::class.java)
        } else {
            null
        }

        val fallbackRate = if (previousTx != null && previousTx.currency.equals(normalizedCurrency, ignoreCase = true) && previousTx.bcvRateUsed > 0.0) {
            previousTx.bcvRateUsed
        } else if (transaction.bcvRateUsed > 0.0 && normalizedCurrency != "USD") {
            transaction.bcvRateUsed
        } else {
            0.0
        }
        val rateToUse = resolveUsdRate(normalizedCurrency, transaction.date, fallbackRate = fallbackRate)
        val parsedAmountInUsd = if (normalizedCurrency == "USD") {
            transaction.originalAmount
        } else {
            transaction.originalAmount / rateToUse
        }

        val txKey = if (transaction.id.isEmpty()) {
            transactionsRef.push().key ?: throw IllegalStateException("No se pudo generar ID de transacción")
        } else {
            transaction.id
        }
        val finalizedTx = transaction.copy(
            id = txKey,
            currency = normalizedCurrency,
            amount = parsedAmountInUsd,
            bcvRateUsed = rateToUse
        )

        // Pre-resolve any wallet conversion rates before mutating DB
        val prevWalletRate = if (!finalizedTx.isTransfer && previousTx != null && previousTx.walletId.isNotBlank() && !previousTx.isTransfer) {
            preResolveWalletConversionRate(previousTx.walletId, previousTx.currency)
        } else 1.0
        val newWalletRate = if (!finalizedTx.isTransfer && finalizedTx.walletId.isNotBlank()) {
            preResolveWalletConversionRate(finalizedTx.walletId, finalizedTx.currency)
        } else 1.0

        val sym = CountryCurrencyConfig.getCurrencySymbol(finalizedTx.currency)
        val actionType = when {
            finalizedTx.isTransfer -> "transferencia"
            finalizedTx.isIncome -> "ingreso"
            else -> "gasto"
        }
        val categorySuffix = if (finalizedTx.isIncome || finalizedTx.isTransfer) "" else " [${finalizedTx.category}]"
        val logDetail = if (finalizedTx.currency != "USD") {
            "$user registró $actionType de $sym ${finalizedTx.originalAmount} ${finalizedTx.currency} ($${String.format(Locale.US, "%.2f", finalizedTx.amount)} USD) en '${finalizedTx.description}'$categorySuffix"
        } else {
            "$user registró $actionType de $${String.format(Locale.US, "%.2f", finalizedTx.amount)} USD en '${finalizedTx.description}'$categorySuffix"
        }

        val logKey = familyRoot.child("activity_logs").push().key ?: UUID.randomUUID().toString()
        val logEntry = ActivityLog(
            id = logKey,
            timestamp = System.currentTimeMillis(),
            tag = "FINANZAS",
            message = logDetail,
            user = user
        )

        familyRoot.updateChildren(
            mapOf<String, Any?>(
                "transactions/$txKey" to finalizedTx,
                "activity_logs/$logKey" to logEntry
            )
        ).await()

        // Revert old wallet balance impact if editing, then apply new wallet balance impact atomically via runTransaction
        if (!finalizedTx.isTransfer) {
            if (previousTx != null && previousTx.walletId.isNotBlank() && !previousTx.isTransfer) {
                val revertSign = if (previousTx.isIncome) -1.0 else 1.0
                applyWalletBalanceDelta(
                    walletId = previousTx.walletId,
                    txAmountUsd = previousTx.amount,
                    txOriginalAmount = previousTx.originalAmount,
                    txCurrency = previousTx.currency,
                    sign = revertSign,
                    preResolvedTargetRate = prevWalletRate
                )
            }
            if (finalizedTx.walletId.isNotBlank()) {
                val applySign = if (finalizedTx.isIncome) 1.0 else -1.0
                applyWalletBalanceDelta(
                    walletId = finalizedTx.walletId,
                    txAmountUsd = finalizedTx.amount,
                    txOriginalAmount = finalizedTx.originalAmount,
                    txCurrency = finalizedTx.currency,
                    sign = applySign,
                    preResolvedTargetRate = newWalletRate
                )
            }
        }

        if (finalizedTx.isIncome || finalizedTx.isTransfer) {
            return null
        }
        val effectiveDate = if (finalizedTx.balanceDate > 0L) finalizedTx.balanceDate else finalizedTx.date
        return checkBudgetAlert(finalizedTx.category, user, effectiveDate)
    }

    suspend fun deleteTransaction(transaction: Transaction, user: String) {
        val familyRoot = getFamilyRootRef() ?: return
        val transactionsRef = familyRoot.child("transactions")
        val targetId = if (transaction.id.isNotEmpty()) transaction.id else {
            val snapshot = transactionsRef.get().await()
            var foundId = ""
            for (child in snapshot.children) {
                val dbTx = child.getValue(Transaction::class.java)
                if (dbTx != null && dbTx.description == transaction.description && dbTx.date == transaction.date && dbTx.amount == transaction.amount) {
                    foundId = child.key ?: ""
                    break
                }
            }
            foundId
        }
        if (targetId.isNotEmpty()) {
            val storedTx = transactionsRef.child(targetId).get().await().getValue(Transaction::class.java) ?: transaction
            val preResolvedWalletRate = if (storedTx.walletId.isNotBlank() && !storedTx.isTransfer) {
                preResolveWalletConversionRate(storedTx.walletId, storedTx.currency)
            } else 1.0

            val typeStr = when {
                storedTx.isTransfer -> "la transferencia"
                storedTx.isIncome -> "el ingreso"
                else -> "el gasto"
            }
            val sym = CountryCurrencyConfig.getCurrencySymbol(storedTx.currency)
            val logKey = familyRoot.child("activity_logs").push().key ?: UUID.randomUUID().toString()
            val delLog = ActivityLog(
                id = logKey,
                timestamp = System.currentTimeMillis(),
                tag = "FINANZAS",
                message = "$user eliminó $typeStr de $sym ${storedTx.originalAmount} ${storedTx.currency} ('${storedTx.description}')",
                user = user
            )

            familyRoot.updateChildren(
                mapOf<String, Any?>(
                    "transactions/$targetId" to null,
                    "activity_logs/$logKey" to delLog
                )
            ).await()

            if (storedTx.walletId.isNotBlank() && !storedTx.isTransfer) {
                val revertSign = if (storedTx.isIncome) -1.0 else 1.0
                applyWalletBalanceDelta(
                    walletId = storedTx.walletId,
                    txAmountUsd = storedTx.amount,
                    txOriginalAmount = storedTx.originalAmount,
                    txCurrency = storedTx.currency,
                    sign = revertSign,
                    preResolvedTargetRate = preResolvedWalletRate
                )
            }

            if (storedTx.receiptUrl.startsWith("https://")) {
                runCatching {
                    FirebaseStorage.getInstance().getReferenceFromUrl(storedTx.receiptUrl).delete().await()
                }
            }
        }
    }

    private fun safeFirebaseKey(raw: String): String {
        return raw.trim().replace(Regex("[.#\\$\\[\\]/]"), "_")
    }

    suspend fun insertCategory(category: CategoryDetail, user: String) {
        val categoriesRef = getFamilyRef("categories") ?: return
        val key = safeFirebaseKey(category.categoryName)
        if (key.isNotEmpty()) {
            categoriesRef.child(key).setValue(category).await()
            insertLog(ActivityLog(tag = "CONFIGURACIÓN", message = "${user} configuró la categoría '${category.categoryName}' con límite de $${category.limitAmount} USD", user = user))
        }
    }

    suspend fun deleteCategory(category: CategoryDetail, user: String) {
        val categoriesRef = getFamilyRef("categories") ?: return
        val key = safeFirebaseKey(category.categoryName)
        if (key.isNotEmpty()) {
            categoriesRef.child(key).removeValue().await()
            insertLog(ActivityLog(tag = "CONFIGURACIÓN", message = "${user} eliminó la categoría de presupuesto: '${category.categoryName}'", user = user))
        }
    }

    suspend fun insertProfile(profile: UserProfile, creator: String) {
        val profilesRef = getFamilyRef("user_profiles") ?: return
        val key = safeFirebaseKey(profile.name)
        if (key.isNotEmpty()) {
            profilesRef.child(key).setValue(profile).await()
            insertLog(ActivityLog(tag = "CONFIGURACIÓN", message = "${creator} creó un nuevo perfil de convivencia: '${profile.name}'", user = creator))
        }
    }

    suspend fun deleteProfile(profile: UserProfile, user: String) {
        val profilesRef = getFamilyRef("user_profiles") ?: return
        val key = safeFirebaseKey(profile.name)
        if (key.isNotEmpty()) {
            profilesRef.child(key).removeValue().await()
            insertLog(ActivityLog(tag = "CONFIGURACIÓN", message = "${user} eliminó el perfil: '${profile.name}'", user = user))
        }
    }

    suspend fun insertSetting(key: String, value: String, user: String) {
        val settingsRef = getFamilyRef("app_settings") ?: return
        val safeKey = safeFirebaseKey(key)
        if (safeKey.isNotEmpty()) {
            val storedValue = if (safeKey == "gemini_api_key" && value.isNotBlank()) {
                com.example.security.LocalCryptoBox.encrypt(value.trim())
            } else {
                value
            }
            settingsRef.child(safeKey).setValue(AppSetting(safeKey, storedValue)).await()
            if (safeKey == "bcv_rate") {
                insertLog(ActivityLog(tag = "FINANZAS", message = "${user} actualizó la tasa de cambio oficial del BCV a: ${value} Bs/$", user = user))
            } else if (safeKey == "total_limit_usd") {
                insertLog(ActivityLog(tag = "FINANZAS", message = "${user} reestableció el límite presupuestario mensual global a: $${value} USD", user = user))
            }
        }
    }

    suspend fun updateBcvRateFromLiveApi(user: String): Double {
        val liveRate = BcvExchangeRateService.fetchOfficialRate()
        if (liveRate > 0.0) {
            insertSetting("bcv_rate", String.format(java.util.Locale.US, "%.2f", liveRate), "Sistema")
            insertLog(ActivityLog(tag = "SISTEMA", message = "Sincronizada tasa real oficial de BCV automáticamente de internet: ${String.format(java.util.Locale.US, "%.2f", liveRate)} Bs", user = "Sistema"))
            return liveRate
        }
        return 0.0
    }

    suspend fun updateRateForCountry(countryCode: String, currency: String, user: String): Double {
        val liveRate = BcvExchangeRateService.fetchOfficialRateForCountry(countryCode, currency)
        if (liveRate > 0.0) {
            insertSetting("bcv_rate", String.format(java.util.Locale.US, "%.2f", liveRate), "Sistema Fami")
            insertLog(ActivityLog(tag = "SISTEMA", message = "Sincronizada tasa oficial para $countryCode ($currency): ${String.format(java.util.Locale.US, "%.2f", liveRate)}", user = "Sistema Fami"))
            return liveRate
        }
        return 0.0
    }

    suspend fun insertChore(chore: Chore, creator: String) {
        val choresRef = getFamilyRef("chores") ?: return
        val pushRef = if (chore.id.isEmpty()) choresRef.push() else choresRef.child(chore.id)
        val finalChore = chore.copy(id = pushRef.key ?: "")
        pushRef.setValue(finalChore).await()
        insertLog(ActivityLog(tag = "TAREAS", message = "Nueva tarea [Prioridad: ${finalChore.priority}] creada por ${creator}: '${finalChore.title}' asignada a [${finalChore.assignedTo}]", user = creator))
    }

    suspend fun updateChoreStatus(id: String, title: String, isCompleted: Boolean, user: String) {
        val choresRef = getFamilyRef("chores") ?: return
        if (id.isNotEmpty()) {
            val date = if (isCompleted) System.currentTimeMillis() else null
            val updates = mapOf<String, Any?>(
                "isCompleted" to isCompleted,
                "completedDate" to date,
                "completed" to null
            )
            choresRef.child(id).updateChildren(updates).await()
            val statusText = if (isCompleted) "completó" else "reabrió"
            insertLog(ActivityLog(tag = "TAREAS", message = "${user} ${statusText} la tarea: '${title}'", user = user))
        }
    }

    suspend fun deleteChore(chore: Chore, user: String) {
        val choresRef = getFamilyRef("chores") ?: return
        val targetId = if (chore.id.isNotEmpty()) chore.id else {
            val snapshot = choresRef.get().await()
            var foundId = ""
            for (child in snapshot.children) {
                val dbChore = child.getValue(Chore::class.java)
                if (dbChore != null && dbChore.title == chore.title && dbChore.createdDate == chore.createdDate) {
                    foundId = child.key ?: ""
                    break
                }
            }
            foundId
        }
        if (targetId.isNotEmpty()) {
            choresRef.child(targetId).removeValue().await()
            insertLog(ActivityLog(tag = "TAREAS", message = "${user} eliminó la tarea de prioridad ${chore.priority}: '${chore.title}'", user = user))
        }
    }

    suspend fun insertShoppingItem(item: ShoppingItem, user: String) {
        val shoppingItemsRef = getFamilyRef("shopping_items") ?: return
        val pushRef = if (item.id.isEmpty()) shoppingItemsRef.push() else shoppingItemsRef.child(item.id)
        val finalItem = item.copy(id = pushRef.key ?: "")
        pushRef.setValue(finalItem).await()
        insertLog(ActivityLog(tag = "COMPRAS", message = "${user} añadió a lista de compras: '${finalItem.name}' (${finalItem.quantity})", user = user))
    }

    suspend fun updateShoppingItemStatus(id: String, name: String, isBought: Boolean, user: String) {
        val shoppingItemsRef = getFamilyRef("shopping_items") ?: return
        if (id.isNotEmpty()) {
            val updates = mapOf<String, Any?>(
                "isBought" to isBought,
                "bought" to null
            )
            shoppingItemsRef.child(id).updateChildren(updates).await()
            val statusText = if (isBought) "marcó como comprado" else "desmarcó"
            insertLog(ActivityLog(tag = "COMPRAS", message = "${user} ${statusText} el artículo de compras: '${name}'", user = user))
        }
    }

    suspend fun deleteShoppingItem(item: ShoppingItem, user: String) {
        val shoppingItemsRef = getFamilyRef("shopping_items") ?: return
        val targetId = if (item.id.isNotEmpty()) item.id else {
            val snapshot = shoppingItemsRef.get().await()
            var foundId = ""
            for (child in snapshot.children) {
                val dbItem = child.getValue(ShoppingItem::class.java)
                if (dbItem != null && dbItem.name == item.name && dbItem.dateAdded == item.dateAdded) {
                    foundId = child.key ?: ""
                    break
                }
            }
            foundId
        }
        if (targetId.isNotEmpty()) {
            shoppingItemsRef.child(targetId).removeValue().await()
            insertLog(ActivityLog(tag = "COMPRAS", message = "${user} eliminó de la lista de compras: '${item.name}'", user = user))
        }
    }

    suspend fun insertGoal(goal: Goal, user: String) {
        val goalsRef = getFamilyRef("goals") ?: return
        val pushRef = if (goal.id.isEmpty()) goalsRef.push() else goalsRef.child(goal.id)
        val finalGoal = goal.copy(id = pushRef.key ?: "")
        pushRef.setValue(finalGoal).await()
        insertLog(ActivityLog(tag = "METAS", message = "${user} configuró la meta/deseo: '${finalGoal.title}'", user = user))
    }

    suspend fun deleteGoal(goal: Goal, user: String) {
        val goalsRef = getFamilyRef("goals") ?: return
        val targetId = if (goal.id.isNotEmpty()) goal.id else return
        goalsRef.child(targetId).removeValue().await()
        insertLog(ActivityLog(tag = "METAS", message = "${user} eliminó la meta: '${goal.title}'", user = user))
    }

    suspend fun insertNote(note: Note, user: String) {
        val notesRef = getFamilyRef("notes") ?: return
        val pushRef = if (note.id.isEmpty()) notesRef.push() else notesRef.child(note.id)
        val finalNote = note.copy(id = pushRef.key ?: "")
        pushRef.setValue(finalNote).await()
        insertLog(ActivityLog(tag = "NOTAS", message = "${user} guardó una nota: '${finalNote.title}'", user = user))
    }

    suspend fun deleteNote(note: Note, user: String) {
        val notesRef = getFamilyRef("notes") ?: return
        val targetId = if (note.id.isNotEmpty()) note.id else return
        notesRef.child(targetId).removeValue().await()
        insertLog(ActivityLog(tag = "NOTAS", message = "${user} eliminó la nota: '${note.title}'", user = user))
    }

    data class SupportTicketResult(
        val ticketId: String,
        val webhookDelivered: Boolean
    )

    companion object {
        // URL de Google Apps Script (Web App) de la hoja "Fami - Soporte y Sugerencias".
        // También puede sobreescribirse dinámicamente en Firebase RTDB (/config/support_sheet_webhook).
        private const val DEFAULT_SUPPORT_WEBHOOK_URL = ""
    }

    suspend fun submitSupportTicket(
        type: String,
        section: String,
        subject: String,
        description: String,
        appVersion: String,
        deviceInfo: String,
        user: String
    ): SupportTicketResult {
        val currentFirebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val uid = currentFirebaseUser?.uid
        val userEmail = currentFirebaseUser?.email.orEmpty()
        val familyId = _familyId.value ?: ""
        val ticketId = "FAM-${System.currentTimeMillis().toString().takeLast(6)}"
        val payload = mapOf(
            "id" to ticketId,
            "type" to type,
            "section" to section,
            "subject" to subject,
            "description" to description,
            "appVersion" to appVersion,
            "deviceInfo" to deviceInfo,
            "user" to user,
            "userEmail" to userEmail,
            "familyId" to familyId,
            "timestamp" to System.currentTimeMillis()
        )
        if (!uid.isNullOrBlank()) {
            com.google.firebase.database.FirebaseDatabase.getInstance().reference
                .child("users").child(uid).child("support_tickets").push().setValue(payload).await()
        }
        getFamilyRef("support_tickets")?.push()?.setValue(payload)?.await()

        // Sincronización con Google Sheets + Correo (hola@somosfami.app) vía Apps Script
        val webhookDelivered = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val globalHook = runCatching {
                    db.child("config").child("support_sheet_webhook").get().await()
                        .getValue(String::class.java)?.trim().orEmpty()
                }.getOrDefault("")
                val familyHook = if (globalHook.isBlank()) {
                    runCatching {
                        getFamilyRef("support_sheet_webhook")?.get()?.await()
                            ?.getValue(String::class.java)?.trim().orEmpty()
                    }.getOrDefault("")
                } else ""
                val webhookUrl = listOf(globalHook, familyHook, DEFAULT_SUPPORT_WEBHOOK_URL)
                    .firstOrNull { it.startsWith("https://") }
                    .orEmpty()
                if (webhookUrl.isNotBlank()) {
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .build()
                    val jsonBody = org.json.JSONObject(payload).toString()
                    val mediaType = with(okhttp3.MediaType.Companion) { "application/json; charset=utf-8".toMediaType() }
                    val body = with(okhttp3.RequestBody.Companion) { jsonBody.toRequestBody(mediaType) }
                    val req = okhttp3.Request.Builder()
                        .url(webhookUrl)
                        .post(body)
                        .build()
                    client.newCall(req).execute().use { resp ->
                        resp.isSuccessful
                    }
                } else {
                    false
                }
            }.getOrDefault(false)
        }

        insertLog(
            ActivityLog(
                tag = "SOPORTE",
                message = "$user envió $type ($section): $subject",
                user = user
            )
        )
        return SupportTicketResult(ticketId = ticketId, webhookDelivered = webhookDelivered)
    }

    suspend fun insertWallet(wallet: Wallet, user: String) {
        val walletsRef = getFamilyRef("wallets") ?: return
        val pushRef = if (wallet.id.isEmpty()) walletsRef.push() else walletsRef.child(wallet.id)
        val finalWallet = wallet.copy(id = pushRef.key ?: "")
        pushRef.setValue(finalWallet).await()
        insertLog(ActivityLog(tag = "FINANZAS", message = "$user actualizó la billetera '${finalWallet.name}' (${finalWallet.currency})", user = user))
    }

    suspend fun deleteWallet(wallet: Wallet, user: String) {
        val walletsRef = getFamilyRef("wallets") ?: return
        val targetId = if (wallet.id.isNotEmpty()) wallet.id else return
        walletsRef.child(targetId).removeValue().await()
        insertLog(ActivityLog(tag = "FINANZAS", message = "$user eliminó la billetera '${wallet.name}'", user = user))
    }

    suspend fun adjustWalletBalance(walletId: String, delta: Double, user: String) {
        val walletsRef = getFamilyRef("wallets") ?: return
        if (walletId.isBlank()) return

        val committedSnapshot = runRtdbTransaction(walletsRef.child(walletId)) { currentData ->
            if (currentData.value == null) {
                return@runRtdbTransaction RtdbTransaction.success(currentData)
            }
            val curBalance = (currentData.child("balance").value as? Number)?.toDouble() ?: 0.0
            val updated = ((curBalance + delta) * 100.0).roundToInt() / 100.0
            currentData.child("balance").value = updated
            RtdbTransaction.success(currentData)
        } ?: return

        val wallet = committedSnapshot.getValue(Wallet::class.java)?.copy(id = walletId) ?: return
        val sym = CountryCurrencyConfig.getCurrencySymbol(wallet.currency)
        insertLog(
            ActivityLog(
                tag = "FINANZAS",
                message = "$user ajustó saldo de '${wallet.name}' a $sym ${String.format(Locale.US, "%.2f", wallet.balance)} ${wallet.currency}",
                user = user
            )
        )
    }

    suspend fun transferBetweenWallets(
        fromWallet: Wallet,
        toWallet: Wallet,
        fromAmount: Double,
        toAmount: Double,
        note: String,
        user: String
    ) {
        val familyRoot = getFamilyRootRef() ?: return
        val walletsRef = familyRoot.child("wallets")
        if (fromWallet.id.isBlank() || toWallet.id.isBlank() ||
            fromWallet.id == toWallet.id || fromAmount <= 0.0 || toAmount <= 0.0
        ) return

        // Pre-resolve rate before mutating balances
        val freshFromSnapshot = walletsRef.child(fromWallet.id).get().await()
        val initialFromCurr = (freshFromSnapshot.child("currency").value as? String)?.takeIf { it.isNotBlank() }
            ?: fromWallet.currency
        val normalizedFromCurr = CountryCurrencyConfig.normalizeCurrencyCode(initialFromCurr)
        val nowMs = System.currentTimeMillis()
        val rateUsed = resolveUsdRate(normalizedFromCurr, nowMs)
        val usdEquiv = if (normalizedFromCurr == "USD") fromAmount else fromAmount / rateUsed

        // Atomic single-transaction update of both source and destination wallet balances
        val committedWallets = runRtdbTransaction(walletsRef) { currentData ->
            if (currentData.value == null) {
                return@runRtdbTransaction RtdbTransaction.success(currentData)
            }
            val fromNode = currentData.child(fromWallet.id)
            val toNode = currentData.child(toWallet.id)
            if (fromNode.value == null || toNode.value == null) {
                return@runRtdbTransaction RtdbTransaction.abort()
            }
            val curFromBal = (fromNode.child("balance").value as? Number)?.toDouble() ?: 0.0
            val curToBal = (toNode.child("balance").value as? Number)?.toDouble() ?: 0.0
            fromNode.child("balance").value = ((curFromBal - fromAmount) * 100.0).roundToInt() / 100.0
            toNode.child("balance").value = ((curToBal + toAmount) * 100.0).roundToInt() / 100.0
            RtdbTransaction.success(currentData)
        } ?: throw IllegalStateException("No se pudo completar la transferencia atómica")

        val freshFrom = committedWallets.child(fromWallet.id).getValue(Wallet::class.java)?.copy(id = fromWallet.id) ?: fromWallet
        val freshTo = committedWallets.child(toWallet.id).getValue(Wallet::class.java)?.copy(id = toWallet.id) ?: toWallet
        val finalFromCurr = CountryCurrencyConfig.normalizeCurrencyCode(freshFrom.currency)
        val finalToCurr = CountryCurrencyConfig.normalizeCurrencyCode(freshTo.currency)

        val txKey = familyRoot.child("transactions").push().key
            ?: throw IllegalStateException("No se pudo generar ID de transferencia")
        val logKey = familyRoot.child("activity_logs").push().key ?: UUID.randomUUID().toString()

        val desc = note.trim().ifBlank { "Transferencia: ${freshFrom.name} → ${freshTo.name}" }
        val transferTx = Transaction(
            id = txKey,
            amount = usdEquiv,
            category = "Transferencia",
            description = desc,
            date = nowMs,
            balanceDate = nowMs,
            registeredBy = user,
            currency = finalFromCurr,
            originalAmount = fromAmount,
            bcvRateUsed = rateUsed,
            isIncome = false,
            walletId = freshFrom.id,
            targetWalletId = freshTo.id,
            isTransfer = true
        )

        val symFrom = CountryCurrencyConfig.getCurrencySymbol(finalFromCurr)
        val symTo = CountryCurrencyConfig.getCurrencySymbol(finalToCurr)
        val transferLog = ActivityLog(
            id = logKey,
            timestamp = System.currentTimeMillis(),
            tag = "FINANZAS",
            message = "$user transfirió $symFrom ${String.format(Locale.US, "%.2f", fromAmount)} $finalFromCurr de '${freshFrom.name}' a '${freshTo.name}' ($symTo ${String.format(Locale.US, "%.2f", toAmount)} $finalToCurr)",
            user = user
        )

        try {
            familyRoot.updateChildren(
                mapOf<String, Any?>(
                    "transactions/$txKey" to transferTx,
                    "activity_logs/$logKey" to transferLog
                )
            ).await()
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            // Compensating rollback of wallet balances if multi-path tx+log write fails
            runCatching {
                runRtdbTransaction(walletsRef) { currentData ->
                    if (currentData.value == null) return@runRtdbTransaction RtdbTransaction.success(currentData)
                    val fromNode = currentData.child(fromWallet.id)
                    val toNode = currentData.child(toWallet.id)
                    if (fromNode.value != null && toNode.value != null) {
                        val bFrom = (fromNode.child("balance").value as? Number)?.toDouble() ?: 0.0
                        val bTo = (toNode.child("balance").value as? Number)?.toDouble() ?: 0.0
                        fromNode.child("balance").value = ((bFrom + fromAmount) * 100.0).roundToInt() / 100.0
                        toNode.child("balance").value = ((bTo - toAmount) * 100.0).roundToInt() / 100.0
                    }
                    RtdbTransaction.success(currentData)
                }
            }
            throw e
        }
    }

    suspend fun insertLog(log: ActivityLog) {
        val logsRef = getFamilyRef("activity_logs") ?: return
        val pushRef = if (log.id.isEmpty()) logsRef.push() else logsRef.child(log.id)
        pushRef.setValue(log.copy(id = pushRef.key ?: "")).await()
    }

    private suspend fun getSettingValueByKey(key: String): String? {
        val settingsRef = getFamilyRef("app_settings") ?: return null
        val safeKey = safeFirebaseKey(key)
        if (safeKey.isEmpty()) return null
        val snapshot = settingsRef.child(safeKey).get().await()
        return snapshot.getValue(AppSetting::class.java)?.value
    }

    private suspend fun getCategoryByName(name: String): CategoryDetail? {
        val categoriesRef = getFamilyRef("categories") ?: return null
        val safeKey = safeFirebaseKey(name)
        if (safeKey.isEmpty()) return null
        val snapshot = categoriesRef.child(safeKey).get().await()
        return snapshot.getValue(CategoryDetail::class.java)
    }

    private suspend fun checkBudgetAlert(
        category: String,
        user: String,
        txTimestamp: Long = System.currentTimeMillis()
    ): String? {
        val currentCal = Calendar.getInstance()
        val curYear = currentCal.get(Calendar.YEAR)
        val curMonth = currentCal.get(Calendar.MONTH)

        val txCal = Calendar.getInstance().apply { timeInMillis = txTimestamp }
        if (txCal.get(Calendar.YEAR) != curYear || txCal.get(Calendar.MONTH) != curMonth) {
            return null
        }

        val monthKey = String.format(Locale.US, "%04d-%02d", curYear, curMonth + 1)
        val transactionsRef = getFamilyRef("transactions") ?: return null
        val transactionsSnapshot = transactionsRef.get().await()
        val transactions = mutableListOf<Transaction>()
        val itemCal = Calendar.getInstance()
        for (child in transactionsSnapshot.children) {
            child.getValue(Transaction::class.java)?.let { tx ->
                if (!tx.isIncome && !tx.isTransfer) {
                    val effectiveDate = if (tx.balanceDate > 0L) tx.balanceDate else tx.date
                    itemCal.timeInMillis = effectiveDate
                    if (itemCal.get(Calendar.YEAR) == curYear && itemCal.get(Calendar.MONTH) == curMonth) {
                        transactions.add(tx)
                    }
                }
            }
        }

        val catDetail = getCategoryByName(category)
        val catLimit = catDetail?.limitsPerMonth?.get(monthKey)?.takeIf { it > 0.0 }
            ?: catDetail?.limitAmount
            ?: 0.0

        if (catLimit > 0.0) {
            val catSpent = transactions.sumOf { tx ->
                if (tx.items.isNotEmpty()) {
                    val rate = if (!tx.currency.equals("USD", ignoreCase = true) && tx.bcvRateUsed > 0.0) tx.bcvRateUsed else 1.0
                    tx.items.filter { it.category.equals(category, ignoreCase = true) }.sumOf { it.amount / rate }
                } else if (tx.category.equals(category, ignoreCase = true)) {
                    tx.amount
                } else {
                    0.0
                }
            }
            val percentage = (catSpent / catLimit) * 100
            if (percentage >= 100) {
                val msg = "⚠️ ¡LÍMITE EXCEDIDO! Gasto en '$category' ($${String.format(Locale.US, "%.2f", catSpent)} USD) superó el límite de $${String.format(Locale.US, "%.2f", catLimit)} USD"
                insertLog(ActivityLog(tag = "ALERTA", message = msg, user = "Sistema"))
                return msg
            } else if (percentage >= 85) {
                val msg = "🚨 ALERTA GASTO: El gasto en '$category' ha alcanzado el ${percentage.roundToInt()}% ($${String.format(Locale.US, "%.2f", catSpent)} / $${String.format(Locale.US, "%.2f", catLimit)} USD)"
                insertLog(ActivityLog(tag = "ALERTA", message = msg, user = "Sistema"))
                return msg
            }
        }

        val totalLimitObj = getSettingValueByKey("total_limit_usd")
        val totalLimit = totalLimitObj?.toDoubleOrNull() ?: 1800.0
        val totalSpent = transactions.sumOf { it.amount }
        val totalPercentage = if (totalLimit > 0.0) (totalSpent / totalLimit) * 100 else 0.0

        if (totalPercentage >= 100) {
            val msg = "⚠️ ¡PRESUPUESTO GLOBAL AGOTADO! Se han gastado $${totalSpent.roundToInt()} USD de un límite total de $${totalLimit.roundToInt()} USD"
            insertLog(ActivityLog(tag = "ALERTA", message = msg, user = "Sistema"))
            return msg
        } else if (totalPercentage >= 85) {
            val msg = "🚨 ADVERTENCIA GLOBAL: Hemos gastado el ${totalPercentage.roundToInt()}% del presupuesto total ($${String.format(Locale.US, "%.2f", totalSpent)} / $${totalLimit} USD)"
            insertLog(ActivityLog(tag = "ALERTA", message = msg, user = "Sistema"))
            return msg
        }

        return null
    }

    suspend fun verifyJoinedFamilyHasExistingData(familyId: String, joiningUid: String): Boolean {
        val famRef = db.child("families").child(familyId)
        val membersSnap = famRef.child("members").get().await()
        val hasOtherMembers = membersSnap.children.any {
            it.key != joiningUid && it.getValue(Boolean::class.java) == true
        }
        if (hasOtherMembers) return true
        val hasSettings = famRef.child("app_settings").limitToFirst(1).get().await().exists()
        val hasCategories = famRef.child("categories").limitToFirst(1).get().await().exists()
        if (hasSettings || hasCategories) return true
        // Rollback orphan member entry if code did not belong to an initialized family
        runCatching { famRef.child("members").child(joiningUid).removeValue().await() }
        return false
    }

    suspend fun populateDefaultsIfNeeded() {
        val settingsRef = getFamilyRef("app_settings") ?: return
        val categoriesRef = getFamilyRef("categories") ?: return
        val logsRef = getFamilyRef("activity_logs") ?: return

        val hasSettings = settingsRef.limitToFirst(1).get().await().exists()
        if (!hasSettings) {
            settingsRef.child("bcv_rate").setValue(AppSetting("bcv_rate", "36.85")).await()
            settingsRef.child("total_limit_usd").setValue(AppSetting("total_limit_usd", "1800.0")).await()
            settingsRef.child("is_dark_theme").setValue(AppSetting("is_dark_theme", "false")).await()
        }

        val hasCategories = categoriesRef.limitToFirst(1).get().await().exists()
        if (!hasCategories) {
            insertCategory(CategoryDetail("Alimentos", 500.0, "#6750A4"), "Sistema")
            insertCategory(CategoryDetail("Servicios", 300.0, "#0284C7"), "Sistema")
            insertCategory(CategoryDetail("Transporte", 200.0, "#0D9488"), "Sistema")
            insertCategory(CategoryDetail("Hogar", 400.0, "#16A34A"), "Sistema")
            insertCategory(CategoryDetail("Entretenimiento", 200.0, "#EA580C"), "Sistema")
            insertCategory(CategoryDetail("Otros", 200.0, "#4B5563"), "Sistema")
        }

        val hasLogs = logsRef.limitToFirst(1).get().await().exists()
        if (!hasLogs) {
            insertLog(ActivityLog(timestamp = System.currentTimeMillis() - 3 * 3600000L, tag = "SISTEMA", message = "Espacio Fami inicializado correctamente.", user = "Sistema"))
        }
    }

    suspend fun uploadReceiptImage(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val fid = _familyId.value
        if (uid.isNullOrBlank() || fid.isNullOrBlank()) return@withContext null

        try {
            val maxDim = 1280
            val scale = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }
            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 78, baos)
            val data = baos.toByteArray()

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            val storageRef = FirebaseStorage.getInstance().reference
                .child("families")
                .child(fid)
                .child("receipts")
                .child("${uid}_${UUID.randomUUID()}.jpg")

            storageRef.putBytes(data, metadata).await()
            storageRef.downloadUrl.await().toString()
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.e("FamilyRepository", "Error uploading receipt image to Firebase Storage", e)
            null
        }
    }
}
