package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY receivedAt DESC, id DESC")
    fun getAllNotificationsNewestFirst(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY receivedAt ASC, id ASC")
    fun getAllNotificationsOldestFirst(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isFavorite = 1 ORDER BY receivedAt DESC, id DESC")
    fun getFavoriteNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY receivedAt DESC, id DESC LIMIT :limit")
    fun getRecentNotifications(limit: Int): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE isFavorite = 1")
    fun getFavoriteCount(): Flow<Int>

    @Query(
        """
        SELECT packageName, appName, COUNT(*) AS count 
        FROM notifications 
        GROUP BY packageName 
        ORDER BY MAX(receivedAt) DESC
        """
    )
    fun getDistinctAppsWithCount(): Flow<List<AppNotificationSummary>>

    @Query(
        """
        SELECT * FROM notifications 
        WHERE sender LIKE '%' || :query || '%' 
           OR message LIKE '%' || :query || '%' 
           OR appName LIKE '%' || :query || '%'
           OR title LIKE '%' || :query || '%'
        ORDER BY receivedAt DESC, id DESC
        """
    )
    fun searchNotifications(query: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    fun getNotificationById(id: Long): Flow<NotificationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM notifications 
            WHERE packageName = :packageName 
              AND sender = :sender 
              AND message = :message 
              AND receivedAt BETWEEN :fromTime AND :toTime
        )
        """
    )
    suspend fun existsDuplicate(
        packageName: String,
        sender: String,
        message: String,
        fromTime: Long,
        toTime: Long
    ): Boolean

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM notifications 
            WHERE notificationKey = :notificationKey 
              AND message = :message 
              AND receivedAt >= :sinceTime
        )
        """
    )
    suspend fun existsByKeyAndMessage(
        notificationKey: String,
        message: String,
        sinceTime: Long
    ): Boolean

    @Query("UPDATE notifications SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
}
