package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.NotifVaultApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationCaptureService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val pkg = notification.packageName ?: return
        if (pkg == applicationContext.packageName) return

        val appContainer = (applicationContext as? NotifVaultApp)?.container ?: return

        serviceScope.launch {
            val shouldCapture = appContainer.preferencesManager.shouldCapturePackage(pkg)
            if (!shouldCapture) return@launch

            val ignoreOtp = appContainer.preferencesManager.isSensitiveOtpIgnored()
            val appName = appContainer.installedAppsProvider.resolveAppName(pkg)

            val parsedEntities = NotificationParser.parse(
                sbn = notification,
                appName = appName,
                ignoreSensitiveOtp = ignoreOtp
            )

            for (entity in parsedEntities) {
                appContainer.notificationRepository.saveIfNotDuplicate(entity)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Intentionally do NOT save notification removal events as new messages.
        // Saved notifications remain safely stored in the local Room database.
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
