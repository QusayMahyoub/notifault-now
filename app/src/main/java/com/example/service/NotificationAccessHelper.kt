package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat

object NotificationAccessHelper {

    fun isNotificationAccessEnabled(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        if (enabledPackages.contains(context.packageName)) {
            return true
        }
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val componentName = ComponentName(context, NotificationCaptureService::class.java).flattenToString()
        return flat.split(":").any {
            it.equals(componentName, ignoreCase = true) ||
                it.startsWith("${context.packageName}/")
        }
    }

    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun requestRebindIfEnabled(context: Context) {
        if (isNotificationAccessEnabled(context)) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(context, NotificationCaptureService::class.java)
                )
            } catch (_: Exception) {
                // Safe no-op on environments where rebind is restricted
            }
        }
    }
}
