package com.example.data.repository

import com.example.data.local.AppNotificationSummary
import com.example.data.local.NotificationDao
import com.example.data.local.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationDao) {

    val allNewestFirst: Flow<List<NotificationEntity>> = dao.getAllNotificationsNewestFirst()
    val allOldestFirst: Flow<List<NotificationEntity>> = dao.getAllNotificationsOldestFirst()
    val favorites: Flow<List<NotificationEntity>> = dao.getFavoriteNotifications()
    val totalCount: Flow<Int> = dao.getTotalCount()
    val favoriteCount: Flow<Int> = dao.getFavoriteCount()
    val distinctApps: Flow<List<AppNotificationSummary>> = dao.getDistinctAppsWithCount()

    fun getRecentNotifications(limit: Int = 5): Flow<List<NotificationEntity>> =
        dao.getRecentNotifications(limit)

    fun searchNotifications(query: String): Flow<List<NotificationEntity>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            dao.getAllNotificationsNewestFirst()
        } else {
            dao.searchNotifications(trimmed)
        }
    }

    fun getNotificationById(id: Long): Flow<NotificationEntity?> =
        dao.getNotificationById(id)

    suspend fun saveIfNotDuplicate(notification: NotificationEntity): Boolean {
        val cleanMessage = notification.message.trim()
        if (cleanMessage.isEmpty()) return false

        val normalized = notification.copy(
            sender = notification.sender.trim(),
            title = notification.title.trim(),
            message = cleanMessage,
            appName = notification.appName.trim().ifEmpty { notification.packageName }
        )

        val windowMs = 5_000L
        val duplicateByTime = dao.existsDuplicate(
            packageName = normalized.packageName,
            sender = normalized.sender,
            message = normalized.message,
            fromTime = normalized.receivedAt - windowMs,
            toTime = normalized.receivedAt + windowMs
        )
        if (duplicateByTime) return false

        val key = normalized.notificationKey
        if (!key.isNullOrBlank()) {
            val duplicateByKey = dao.existsByKeyAndMessage(
                notificationKey = key,
                message = normalized.message,
                sinceTime = normalized.receivedAt - 120_000L
            )
            if (duplicateByKey) return false
        }

        dao.insertNotification(normalized)
        return true
    }

    suspend fun toggleFavorite(id: Long, currentFavorite: Boolean) {
        dao.updateFavorite(id, !currentFavorite)
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        dao.updateFavorite(id, isFavorite)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteByPackage(packageName: String) {
        dao.deleteByPackage(packageName)
    }

    suspend fun deleteAll() {
        dao.deleteAllNotifications()
    }
}
