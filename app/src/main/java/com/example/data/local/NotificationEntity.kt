package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["receivedAt"]),
        Index(value = ["isFavorite"]),
        Index(value = ["sender"]),
        Index(value = ["packageName", "receivedAt"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val sender: String,
    val title: String,
    val message: String,
    val receivedAt: Long,
    val isFavorite: Boolean = false,
    val notificationKey: String? = null
)

data class AppNotificationSummary(
    val packageName: String,
    val appName: String,
    val count: Int
)
