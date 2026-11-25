package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NotificationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NotifVaultDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: NotifVaultDatabase? = null

        fun getInstance(context: Context): NotifVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NotifVaultDatabase::class.java,
                    "notif_vault_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
