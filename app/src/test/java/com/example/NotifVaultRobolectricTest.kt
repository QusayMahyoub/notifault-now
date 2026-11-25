package com.example

import android.app.Notification
import android.content.Context
import android.os.Bundle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NotifVaultDatabase
import com.example.data.local.NotificationEntity
import com.example.data.local.PreferencesManager
import com.example.data.repository.NotificationRepository
import com.example.service.NotificationParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotifVaultRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: NotifVaultDatabase
    private lateinit var repository: NotificationRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, NotifVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NotificationRepository(database.notificationDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `parser extracts MessagingStyle multi-message bundles`() {
        val msg1 = Bundle().apply {
            putCharSequence("sender", "أحمد")
            putCharSequence("text", "السلام عليكم")
            putLong("time", 1_700_000_001_000L)
        }
        val msg2 = Bundle().apply {
            putCharSequence("sender", "أحمد")
            putCharSequence("text", "هل أنت متاح الآن؟")
            putLong("time", 1_700_000_005_000L)
        }
        val notification = Notification.Builder(context, "test")
            .setContentTitle("أحمد")
            .build().apply {
                extras.putParcelableArray(Notification.EXTRA_MESSAGES, arrayOf(msg1, msg2))
            }

        val parsed = NotificationParser.parseNotification(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            notification = notification,
            postTime = 1_700_000_005_000L,
            notificationKey = "0|com.whatsapp|1|null|100",
            ignoreSensitiveOtp = true
        )

        assertEquals(2, parsed.size)
        assertEquals("أحمد", parsed[0].sender)
        assertEquals("السلام عليكم", parsed[0].message)
        assertEquals(1_700_000_001_000L, parsed[0].receivedAt)
        assertEquals("هل أنت متاح الآن؟", parsed[1].message)
    }

    @Test
    fun `parser extracts EXTRA_BIG_TEXT and EXTRA_TEXT_LINES and ignores empty or ongoing`() {
        val bigNotification = Notification.Builder(context, "test")
            .setContentTitle("سارة")
            .setContentText("رسالة قصيرة")
            .build().apply {
                extras.putCharSequence(
                    Notification.EXTRA_BIG_TEXT,
                    "هذه رسالة طويلة تحتوي على تفاصيل الاجتماع الكاملة غدًا الساعة العاشرة."
                )
            }

        val bigParsed = NotificationParser.parseNotification(
            packageName = "org.telegram.messenger",
            appName = "Telegram",
            notification = bigNotification,
            postTime = 1_700_000_100_000L,
            notificationKey = "tg_1",
            ignoreSensitiveOtp = true
        )
        assertEquals(1, bigParsed.size)
        assertTrue(bigParsed[0].message.contains("تفاصيل الاجتماع الكاملة"))

        val linesNotification = Notification.Builder(context, "test")
            .setContentTitle("مجموعة العمل")
            .build().apply {
                extras.putCharSequenceArray(
                    Notification.EXTRA_TEXT_LINES,
                    arrayOf("علي: تم إرسال التقرير", "منى: شكرًا لك")
                )
            }
        val linesParsed = NotificationParser.parseNotification(
            packageName = "com.facebook.orca",
            appName = "Facebook Messenger",
            notification = linesNotification,
            postTime = 1_700_000_200_000L,
            notificationKey = "fb_1",
            ignoreSensitiveOtp = true
        )
        assertEquals(1, linesParsed.size)
        assertTrue(linesParsed[0].message.contains("تم إرسال التقرير"))

        val emptyNotification = Notification.Builder(context, "test")
            .setContentTitle("WhatsApp")
            .setContentText("   ")
            .build()
        val emptyParsed = NotificationParser.parseNotification(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            notification = emptyNotification,
            postTime = 1_700_000_300_000L,
            notificationKey = "empty_1",
            ignoreSensitiveOtp = true
        )
        assertTrue(emptyParsed.isEmpty())

        val ongoingNotification = Notification.Builder(context, "test")
            .setContentTitle("WhatsApp")
            .setContentText("مكالمة جارية")
            .setOngoing(true)
            .build()
        val ongoingParsed = NotificationParser.parseNotification(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            notification = ongoingNotification,
            postTime = 1_700_000_400_000L,
            notificationKey = "ongoing_1",
            ignoreSensitiveOtp = true
        )
        assertTrue(ongoingParsed.isEmpty())
    }

    @Test
    fun `parser filters sensitive OTP codes when enabled`() {
        val otpNotification = Notification.Builder(context, "test")
            .setContentTitle("Auth Service")
            .setContentText("رمز التحقق الخاص بك هو 482910 لا تشاركه مع أحد")
            .build()

        val skipped = NotificationParser.parseNotification(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            notification = otpNotification,
            postTime = 1_700_000_500_000L,
            notificationKey = "otp_1",
            ignoreSensitiveOtp = true
        )
        assertTrue(skipped.isEmpty())

        val allowedWhenDisabled = NotificationParser.parseNotification(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            notification = otpNotification,
            postTime = 1_700_000_500_000L,
            notificationKey = "otp_1",
            ignoreSensitiveOtp = false
        )
        assertEquals(1, allowedWhenDisabled.size)
    }

    @Test
    fun `repository prevents duplicates and supports search favorite and delete`() = runTest {
        val first = NotificationEntity(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            sender = "خالد",
            title = "خالد",
            message = "موعدنا غدًا الساعة الخامسة",
            receivedAt = 1_700_000_000_000L,
            notificationKey = "wa_key_1"
        )
        assertTrue(repository.saveIfNotDuplicate(first))
        // Duplicate timestamp / key should be rejected
        assertFalse(repository.saveIfNotDuplicate(first.copy(receivedAt = 1_700_000_001_000L)))

        val second = NotificationEntity(
            packageName = "org.telegram.messenger",
            appName = "Telegram",
            sender = "نورة",
            title = "نورة",
            message = "تم رفع ملف التصميم النهائي",
            receivedAt = 1_700_000_500_000L,
            notificationKey = "tg_key_1"
        )
        assertTrue(repository.saveIfNotDuplicate(second))

        val all = repository.allNewestFirst.first()
        assertEquals(2, all.size)
        assertEquals("نورة", all[0].sender)

        // Local search by sender, message, and appName
        assertEquals(1, repository.searchNotifications("خالد").first().size)
        assertEquals(1, repository.searchNotifications("التصميم").first().size)
        assertEquals(1, repository.searchNotifications("Telegram").first().size)

        // Favorite toggle
        val targetId = all[0].id
        repository.toggleFavorite(targetId, false)
        assertEquals(1, repository.favorites.first().size)

        // Delete single item
        repository.deleteById(targetId)
        assertEquals(1, repository.totalCount.first())

        // Delete all
        repository.deleteAll()
        assertEquals(0, repository.totalCount.first())
    }

    @Test
    fun `preferences manager enforces opt-in monitoring and app exclusion`() = runTest {
        val prefs = PreferencesManager(context)
        assertFalse(prefs.shouldCapturePackage("com.whatsapp"))

        prefs.setPackageMonitored("com.whatsapp", true)
        assertTrue(prefs.shouldCapturePackage("com.whatsapp"))

        // Excluding an app must immediately stop capturing it
        prefs.setPackageExcluded("com.whatsapp", true)
        assertFalse(prefs.shouldCapturePackage("com.whatsapp"))
    }
}
