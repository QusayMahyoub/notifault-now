package com.example.service

import android.app.Notification
import android.app.Person
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import com.example.data.local.NotificationEntity

object NotificationParser {

    private val NOISE_MESSAGES = setOf(
        "checking for new messages",
        "checking for messages",
        "whatsapp web is currently active",
        "backing up messages",
        "جاري البحث عن رسائل جديدة",
        "جارٍ البحث عن رسائل جديدة",
        "جارٍ التحقق من وجود رسائل جديدة",
        "نسخ احتياطي للرسائل"
    )

    private val OTP_KEYWORD_REGEX = Regex(
        pattern = "(?i)(otp|one[- ]time\\s*password|verification\\s*code|security\\s*code|2fa|auth\\s*code|passcode|reset\\s*password|رمز\\s*التحقق|كود\\s*التحقق|رمز\\s*التأكيد|كلمة\\s*المرور|رمز\\s*الأمان|كود\\s*التفعيل)"
    )

    private val OTP_CODE_REGEX = Regex("(?<!\\d)\\d{4,8}(?!\\d)")

    fun parse(
        sbn: StatusBarNotification,
        appName: String,
        ignoreSensitiveOtp: Boolean
    ): List<NotificationEntity> {
        val notification = sbn.notification ?: return emptyList()
        return parseNotification(
            packageName = sbn.packageName,
            appName = appName,
            notification = notification,
            postTime = sbn.postTime,
            notificationKey = sbn.key,
            ignoreSensitiveOtp = ignoreSensitiveOtp
        )
    }

    fun parseNotification(
        packageName: String,
        appName: String,
        notification: Notification,
        postTime: Long,
        notificationKey: String?,
        ignoreSensitiveOtp: Boolean
    ): List<NotificationEntity> {
        // Skip ongoing persistent notifications (e.g. active call, WhatsApp Web active, media players)
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) {
            return emptyList()
        }

        // Skip secret-visibility notifications when sensitive protection is enabled
        if (ignoreSensitiveOtp && notification.visibility == Notification.VISIBILITY_SECRET) {
            return emptyList()
        }

        val extras = notification.extras ?: Bundle.EMPTY

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
            ?.toString()?.trim().orEmpty()

        // 1. Handle MessagingStyle (EXTRA_MESSAGES) if present
        val messagingItems = extractMessagingStyleItems(
            extras = extras,
            fallbackSender = title.ifEmpty { conversationTitle.ifEmpty { appName } },
            fallbackTitle = conversationTitle.ifEmpty { title },
            fallbackTime = postTime
        )

        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

        val candidates = if (messagingItems.isNotEmpty()) {
            messagingItems.map { item ->
                NotificationEntity(
                    packageName = packageName,
                    appName = appName,
                    sender = item.sender.ifEmpty { appName },
                    title = item.title.ifEmpty { item.sender },
                    message = item.text,
                    receivedAt = item.timestamp,
                    isFavorite = false,
                    notificationKey = notificationKey
                )
            }
        } else {
            // Group summary notifications without EXTRA_MESSAGES are usually aggregate counts ("3 messages from 2 chats")
            if (isGroupSummary) {
                return emptyList()
            }

            val body = extractStandardBody(extras)
            if (body.isEmpty()) {
                emptyList()
            } else {
                val resolvedSender = when {
                    title.isNotEmpty() -> title
                    conversationTitle.isNotEmpty() -> conversationTitle
                    subText.isNotEmpty() -> subText
                    else -> appName
                }
                listOf(
                    NotificationEntity(
                        packageName = packageName,
                        appName = appName,
                        sender = resolvedSender,
                        title = title.ifEmpty { conversationTitle.ifEmpty { resolvedSender } },
                        message = body,
                        receivedAt = postTime,
                        isFavorite = false,
                        notificationKey = notificationKey
                    )
                )
            }
        }

        return candidates.filter { entity ->
            val msg = entity.message.trim()
            if (msg.isEmpty()) return@filter false
            if (isBoilerplateNoise(msg)) return@filter false
            if (ignoreSensitiveOtp && isLikelySensitiveOtp(entity.title, entity.sender, msg)) {
                return@filter false
            }
            true
        }
    }

    private data class MessagingItem(
        val sender: String,
        val title: String,
        val text: String,
        val timestamp: Long
    )

    private fun extractMessagingStyleItems(
        extras: Bundle,
        fallbackSender: String,
        fallbackTitle: String,
        fallbackTime: Long
    ): List<MessagingItem> {
        val rawParcelables: Array<Parcelable>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelableArray(Notification.EXTRA_MESSAGES, Parcelable::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            }

        if (rawParcelables.isNullOrEmpty()) return emptyList()

        val items = mutableListOf<MessagingItem>()
        for (parcelable in rawParcelables) {
            val bundle = parcelable as? Bundle ?: continue
            val text = bundle.getCharSequence("text")?.toString()?.trim().orEmpty()
            if (text.isEmpty()) continue

            val senderFromPerson = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    bundle.getParcelable("sender_person", Person::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    bundle.getParcelable("sender_person")
                }
                person?.name?.toString()?.trim().orEmpty()
            } else {
                ""
            }

            val senderLegacy = bundle.getCharSequence("sender")?.toString()?.trim().orEmpty()
            val resolvedSender = senderFromPerson.ifEmpty {
                senderLegacy.ifEmpty { fallbackSender }
            }

            val msgTime = bundle.getLong("time", 0L).let { if (it > 0L) it else fallbackTime }

            items.add(
                MessagingItem(
                    sender = resolvedSender,
                    title = fallbackTitle.ifEmpty { resolvedSender },
                    text = text,
                    timestamp = msgTime
                )
            )
        }
        return items
    }

    private fun extractStandardBody(extras: Bundle): String {
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        return when {
            bigText.isNotEmpty() && bigText.length >= text.length -> bigText
            text.isNotEmpty() -> text
            textLines.isNotEmpty() -> textLines.joinToString(separator = "\n")
            else -> ""
        }
    }

    internal fun isBoilerplateNoise(message: String): Boolean {
        val lower = message.trim().lowercase()
        return NOISE_MESSAGES.any { lower == it || lower.startsWith("$it.") }
    }

    internal fun isLikelySensitiveOtp(title: String, sender: String, message: String): Boolean {
        val combined = "$title $sender $message"
        val hasSensitiveKeyword = OTP_KEYWORD_REGEX.containsMatchIn(combined)
        if (!hasSensitiveKeyword) return false
        val hasNumericCode = OTP_CODE_REGEX.containsMatchIn(message)
        val isPasswordReset = combined.contains("password", ignoreCase = true) ||
            combined.contains("كلمة المرور") ||
            combined.contains("كلمة السر")
        return hasNumericCode || isPasswordReset
    }
}
