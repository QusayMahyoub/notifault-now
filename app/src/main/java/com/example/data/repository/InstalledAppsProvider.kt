package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class MonitoredAppInfo(
    val packageName: String,
    val appName: String,
    val isInstalled: Boolean,
    val isFeaturedMessaging: Boolean
)

class InstalledAppsProvider(private val context: Context) {

    private val iconCache = ConcurrentHashMap<String, Drawable?>()
    private val labelCache = ConcurrentHashMap<String, String>()

    companion object {
        val FEATURED_MESSAGING_APPS = listOf(
            "com.whatsapp" to "WhatsApp",
            "com.whatsapp.w4b" to "WhatsApp Business",
            "org.telegram.messenger" to "Telegram",
            "com.facebook.orca" to "Facebook Messenger"
        )

        val FEATURED_PACKAGE_MAP: Map<String, String> = FEATURED_MESSAGING_APPS.toMap()
    }

    suspend fun loadAvailableApps(): List<MonitoredAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val selfPackage = context.packageName
        val discovered = linkedMapOf<String, MonitoredAppInfo>()

        // 1. Add featured messaging apps first so they are always visible and easy to toggle
        for ((pkg, defaultLabel) in FEATURED_MESSAGING_APPS) {
            val installedLabel = getInstalledAppLabelOrNull(pm, pkg)
            val appName = installedLabel ?: defaultLabel
            labelCache[pkg] = appName
            discovered[pkg] = MonitoredAppInfo(
                packageName = pkg,
                appName = appName,
                isInstalled = installedLabel != null,
                isFeaturedMessaging = true
            )
        }

        // 2. Discover launchable installed apps via PackageManager (Play-compliant via <queries>)
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val otherApps = mutableListOf<MonitoredAppInfo>()
        for (info in resolveInfos) {
            val pkg = info.activityInfo?.packageName ?: continue
            if (pkg == selfPackage || discovered.containsKey(pkg)) continue
            val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { pkg }
            labelCache[pkg] = label
            otherApps.add(
                MonitoredAppInfo(
                    packageName = pkg,
                    appName = label,
                    isInstalled = true,
                    isFeaturedMessaging = false
                )
            )
        }

        otherApps.sortBy { it.appName.lowercase() }
        for (app in otherApps) {
            discovered[app.packageName] = app
        }

        discovered.values.toList()
    }

    fun resolveAppName(packageName: String): String {
        labelCache[packageName]?.let { return it }
        val pm = context.packageManager
        val installed = getInstalledAppLabelOrNull(pm, packageName)
        val resolved = installed ?: FEATURED_PACKAGE_MAP[packageName] ?: packageName
        labelCache[packageName] = resolved
        return resolved
    }

    fun getAppIconOrNull(packageName: String): Drawable? {
        if (iconCache.containsKey(packageName)) {
            return iconCache[packageName]
        }
        val icon = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Exception) {
            null
        }
        if (icon != null) {
            iconCache[packageName] = icon
        }
        return icon
    }

    private fun getInstalledAppLabelOrNull(pm: PackageManager, packageName: String): String? {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString().trim().ifEmpty { packageName }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
