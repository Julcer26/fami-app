package com.example

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object OtaUpdateManager {

    private const val FAMI_MANIFEST_URL =
        "https://raw.githubusercontent.com/Julcer26/fami-app/main/fami-version.json"
    private const val PREFS_NAME = "fami_ota_prefs"
    private const val KEY_DISMISSED_VERSION = "dismissed_version_code"

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersionCode: Int,
        val latestVersionName: String = "",
        val downloadUrl: String
    )

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

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
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DISMISSED_VERSION, versionCode)
            .apply()
    }

    suspend fun checkForUpdates(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val currentVersionCode = getInstalledVersionCode(context)
            val currentVersionName = getInstalledVersionName(context)

            val request = Request.Builder()
                .url("$FAMI_MANIFEST_URL?t=${System.currentTimeMillis()}")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext UpdateInfo(false, currentVersionCode, currentVersionName, "")
                val bodyStr = response.body?.string().orEmpty()
                if (bodyStr.isBlank()) return@withContext UpdateInfo(false, currentVersionCode, currentVersionName, "")

                val json = JSONObject(bodyStr)
                val pkg = json.optString("packageName", "")
                if (pkg != "app.somosfami" && pkg != context.packageName) {
                    return@withContext UpdateInfo(false, currentVersionCode, currentVersionName, "")
                }

                val latestCode = json.optInt("latestVersionCode", 0)
                val latestName = json.optString("latestVersionName", "")
                val downloadUrl = json.optString("latestApkUrl", "")

                val dismissedCode = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getInt(KEY_DISMISSED_VERSION, 0)

                val isStrictlyNewer = latestCode > currentVersionCode &&
                    latestCode > dismissedCode &&
                    (latestName.isBlank() || !latestName.equals(currentVersionName, ignoreCase = true))

                UpdateInfo(
                    isUpdateAvailable = isStrictlyNewer && downloadUrl.isNotBlank(),
                    latestVersionCode = latestCode,
                    latestVersionName = latestName,
                    downloadUrl = downloadUrl
                )
            }
        } catch (_: Exception) {
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

    fun downloadAndInstallUpdate(context: Context, url: String, versionCode: Int = 0) {
        if (!canInstallPackages(context)) {
            android.widget.Toast.makeText(
                context,
                "Activa 'Instalar aplicaciones desconocidas' para Fami y vuelve a intentar.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            openInstallPermissionSettings(context)
            return
        }

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Actualización de Fami")
            setDescription("Descargando la nueva versión de Fami...")
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "fami-update.apk")
            setMimeType("application/vnd.android.package-archive")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }

        val oldFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "fami-update.apk")
        if (oldFile.exists()) {
            oldFile.delete()
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val launched = installApk(context)
                    if (launched && versionCode > 0) {
                        dismissUpdateForVersion(context, versionCode)
                    }
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {}
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(context: Context): Boolean {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "fami-update.apk")
        if (!file.exists()) return false

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
