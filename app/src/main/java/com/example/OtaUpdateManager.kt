package com.example

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object OtaUpdateManager {

    private const val TAG = "OtaUpdateManager"
    private const val FAMI_MANIFEST_URL =
        "https://raw.githubusercontent.com/Julcer26/fami-app/main/fami-version.json"
    private const val PREFS_NAME = "fami_ota_prefs"
    private const val KEY_DISMISSED_VERSION = "dismissed_version_code"
    private const val TRUSTED_SIGNER_SHA256 =
        "91D52BF95A1D868565BCA247023F6B9B61A9C17EFA0CAF368DAC6F8B6D555992"

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersionCode: Int,
        val latestVersionName: String = "",
        val downloadUrl: String,
        val expectedApkSha256: String = ""
    )

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    private val receiverLock = Any()
    @Volatile
    private var activeReceiver: BroadcastReceiver? = null
    @Volatile
    private var activeDownloadId: Long = -1L

    fun getInstalledVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(pInfo).toInt()
        } catch (_: Exception) {
            BuildConfig.VERSION_CODE
        }
    }

    fun getInstalledVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME
        }
    }

    fun dismissUpdateForVersion(context: Context, versionCode: Int) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DISMISSED_VERSION, versionCode)
            .apply()
    }

    fun unregisterDownloadReceiver(context: Context) {
        val appContext = context.applicationContext
        synchronized(receiverLock) {
            activeReceiver?.let { receiver ->
                try {
                    appContext.unregisterReceiver(receiver)
                } catch (_: IllegalArgumentException) {
                } catch (_: Exception) {
                }
            }
            activeReceiver = null
            activeDownloadId = -1L
        }
    }

    internal fun compareVersionNames(a: String, b: String): Int {
        val segsA = extractNumericVersionSegments(a)
        val segsB = extractNumericVersionSegments(b)
        if (segsA.isEmpty() || segsB.isEmpty()) {
            return a.trim().compareTo(b.trim(), ignoreCase = true)
        }
        val maxLen = maxOf(segsA.size, segsB.size)
        for (i in 0 until maxLen) {
            val valA = segsA.getOrElse(i) { 0 }
            val valB = segsB.getOrElse(i) { 0 }
            if (valA != valB) return valA.compareTo(valB)
        }
        return 0
    }

    private fun extractNumericVersionSegments(version: String): List<Int> {
        val clean = version.trim().removePrefix("v").removePrefix("V").substringBefore("-")
        if (clean.isBlank()) return emptyList()
        return clean.split(".")
            .mapNotNull { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }
    }

    suspend fun checkForUpdates(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        try {
            val currentVersionCode = getInstalledVersionCode(appContext)
            val currentVersionName = getInstalledVersionName(appContext)

            val request = Request.Builder()
                .url("$FAMI_MANIFEST_URL?t=${System.currentTimeMillis()}")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateInfo(false, currentVersionCode, currentVersionName, "")
                }
                val bodyStr = response.body?.string().orEmpty()
                if (bodyStr.isBlank()) {
                    return@withContext UpdateInfo(false, currentVersionCode, currentVersionName, "")
                }

                val json = JSONObject(bodyStr)
                val pkg = json.optString("packageName", "")
                if (pkg != "app.somosfami" && pkg != appContext.packageName) {
                    return@withContext UpdateInfo(false, currentVersionCode, currentVersionName, "")
                }

                val latestCode = json.optInt("latestVersionCode", 0)
                val latestName = json.optString("latestVersionName", "").trim()
                val downloadUrl = json.optString("latestApkUrl", "").trim()
                val expectedSha256 = json.optString("sha256", "")
                    .ifBlank { json.optString("apkSha256", "") }
                    .trim()
                    .uppercase()

                val dismissedCode = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getInt(KEY_DISMISSED_VERSION, 0)

                val nameCmp = if (latestName.isNotBlank() && currentVersionName.isNotBlank()) {
                    compareVersionNames(latestName, currentVersionName)
                } else {
                    0
                }
                val isNewerThanInstalled = latestCode > currentVersionCode ||
                    (latestCode == currentVersionCode && nameCmp > 0)
                val notDowngrade = latestCode >= currentVersionCode && nameCmp >= 0
                val notDismissed = latestCode > dismissedCode ||
                    (latestCode == dismissedCode && nameCmp > 0 && latestCode > currentVersionCode)
                val isHttpsDownload = downloadUrl.startsWith("https://", ignoreCase = true)

                val isStrictlyNewer = isNewerThanInstalled && notDowngrade && notDismissed && isHttpsDownload

                UpdateInfo(
                    isUpdateAvailable = isStrictlyNewer,
                    latestVersionCode = latestCode,
                    latestVersionName = latestName,
                    downloadUrl = if (isHttpsDownload) downloadUrl else "",
                    expectedApkSha256 = expectedSha256
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error checking for updates", e)
            UpdateInfo(false, 0, "", "")
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun downloadAndInstallUpdate(
        context: Context,
        url: String,
        versionCode: Int = 0,
        expectedApkSha256: String = ""
    ) {
        val appContext = context.applicationContext
        if (!url.startsWith("https://", ignoreCase = true)) {
            Toast.makeText(
                appContext,
                "URL de actualización no segura.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!canInstallPackages(appContext)) {
            Toast.makeText(
                appContext,
                "Activa 'Instalar aplicaciones desconocidas' para Fami y vuelve a intentar.",
                Toast.LENGTH_LONG
            ).show()
            openInstallPermissionSettings(appContext)
            return
        }

        unregisterDownloadReceiver(appContext)

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Actualización de Fami")
            setDescription("Descargando la nueva versión de Fami...")
            setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, "fami-update.apk")
            setMimeType("application/vnd.android.package-archive")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }

        val targetFile = File(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "fami-update.apk")
        if (targetFile.exists()) {
            targetFile.delete()
        }

        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != downloadId) return

                unregisterDownloadReceiver(appContext)

                val query = DownloadManager.Query().setFilterById(downloadId)
                var status = -1
                try {
                    downloadManager.query(query)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            if (idx >= 0) status = cursor.getInt(idx)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed querying DownloadManager status", e)
                }

                if (status != DownloadManager.STATUS_SUCCESSFUL) {
                    if (targetFile.exists()) targetFile.delete()
                    Toast.makeText(
                        appContext,
                        "No se pudo completar la descarga de la actualización.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val isValid = verifyDownloadedApk(appContext, targetFile, expectedApkSha256)
                        withContext(Dispatchers.Main) {
                            if (isValid) {
                                val launched = installApk(appContext, targetFile)
                                if (launched && versionCode > 0) {
                                    dismissUpdateForVersion(appContext, versionCode)
                                }
                            } else {
                                if (targetFile.exists()) targetFile.delete()
                                Toast.makeText(
                                    appContext,
                                    "Actualización bloqueada: la firma del APK no es válida.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Error verifying downloaded APK", e)
                        if (targetFile.exists()) targetFile.delete()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                appContext,
                                "Error al verificar el paquete de actualización.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }

        synchronized(receiverLock) {
            activeReceiver = onComplete
            activeDownloadId = downloadId
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                appContext.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        }
    }

    private fun verifyDownloadedApk(
        appContext: Context,
        apkFile: File,
        expectedApkSha256: String
    ): Boolean {
        if (!apkFile.exists() || apkFile.length() <= 0L) return false

        if (expectedApkSha256.isNotBlank()) {
            val actualFileHash = computeFileSha256(apkFile)
            if (!actualFileHash.equals(expectedApkSha256.trim(), ignoreCase = true)) {
                Log.e(TAG, "APK SHA-256 mismatch: expected=$expectedApkSha256 actual=$actualFileHash")
                return false
            }
        }

        val pm = appContext.packageManager
        val archiveInfo = getPackageArchiveInfoCompat(pm, apkFile.absolutePath) ?: return false
        if (archiveInfo.packageName != appContext.packageName) {
            Log.e(TAG, "APK package mismatch: ${archiveInfo.packageName}")
            return false
        }

        val currentVersionCode = getInstalledVersionCode(appContext).toLong()
        val archiveVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo)
        if (archiveVersionCode < currentVersionCode) {
            Log.e(TAG, "APK downgrade blocked: archive=$archiveVersionCode current=$currentVersionCode")
            return false
        }

        val archiveSigners = extractSignerSha256Set(archiveInfo)
        if (archiveSigners.isEmpty()) {
            Log.e(TAG, "Downloaded APK has no valid signing certificates")
            return false
        }

        val installedInfo = getInstalledPackageInfoWithSignersCompat(pm, appContext.packageName)
        val trustedSigners = installedInfo?.let { extractSignerSha256Set(it) }
            ?.takeIf { it.isNotEmpty() }
            ?: setOf(TRUSTED_SIGNER_SHA256)

        val signerMatches = archiveSigners.any { it in trustedSigners }
        if (!signerMatches) {
            Log.e(TAG, "APK signer mismatch: archive=$archiveSigners trusted=$trustedSigners")
        }
        return signerMatches
    }

    @Suppress("DEPRECATION")
    private fun getPackageArchiveInfoCompat(pm: PackageManager, archivePath: String): PackageInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageArchiveInfo(archivePath, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            pm.getPackageArchiveInfo(archivePath, PackageManager.GET_SIGNATURES)
        }
    }

    @Suppress("DEPRECATION")
    private fun getInstalledPackageInfoWithSignersCompat(pm: PackageManager, packageName: String): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun extractSignerSha256Set(packageInfo: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            packageInfo.signatures
        } ?: return emptySet()

        val md = MessageDigest.getInstance("SHA-256")
        return signatures.map { sig ->
            md.digest(sig.toByteArray()).joinToString("") { "%02X".format(it) }
        }.toSet()
    }

    private fun computeFileSha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        FileInputStream(file).use { fis ->
            while (true) {
                val read = fis.read(buffer)
                if (read <= 0) break
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02X".format(it) }
    }

    private fun installApk(appContext: Context, file: File): Boolean {
        if (!file.exists()) return false

        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            appContext.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching APK installer", e)
            false
        }
    }
}
