package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeFormatterUtil {
    private val arabicLocale = Locale("ar")

    fun formatRelativeDateTime(timestampMs: Long): String {
        val nowCal = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val timeFmt = SimpleDateFormat("hh:mm a", arabicLocale)
        val timeText = timeFmt.format(Date(timestampMs))

        val sameYear = nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
        val sameDay = sameYear && nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
        if (sameDay) {
            return "اليوم • $timeText"
        }

        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterdayCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
            yesterdayCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) {
            return "أمس • $timeText"
        }

        val dateFmt = if (sameYear) {
            SimpleDateFormat("d MMM", arabicLocale)
        } else {
            SimpleDateFormat("d MMM yyyy", arabicLocale)
        }
        return "${dateFmt.format(Date(timestampMs))} • $timeText"
    }

    fun formatFullDateTime(timestampMs: Long): String {
        val fmt = SimpleDateFormat("EEEE، d MMMM yyyy • hh:mm:ss a", arabicLocale)
        return fmt.format(Date(timestampMs))
    }
}
