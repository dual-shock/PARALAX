package com.dual_shock.paralax.ui.dashboard

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object TimeZoneHelper {
    private val UTC_ZONE = TimeZone.getTimeZone("UTC")

    fun getCurrentLocalHour(): Int {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }

    fun localToUtc(dayOffset: Int, localHour: Int): Pair<String, Int> {
        val localCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, localHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        val utcCal = Calendar.getInstance(UTC_ZONE).apply {
            timeInMillis = localCal.timeInMillis
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply {
            timeZone = UTC_ZONE
        }
        return Pair(dateFormat.format(utcCal.time), utcCal.get(Calendar.HOUR_OF_DAY))
    }

    fun buildUtcMatchPrefix(dayOffset: Int, localHour: Int): String {
        val (utcDate, utcHour) = localToUtc(dayOffset, localHour)
        return "${utcDate}T${String.format("%02d", utcHour)}"
    }

    fun buildUtcTimestamp(dayOffset: Int, localHour: Int): String {
        val (utcDate, utcHour) = localToUtc(dayOffset, localHour)
        return "${utcDate}T${String.format("%02d", utcHour)}:00:00Z"
    }

    fun utcToLocal(utcTimestamp: String): Pair<Int, Int> {
        val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH).apply {
            timeZone = UTC_ZONE
        }
        val utcDate = utcFormat.parse(utcTimestamp) ?: return 0 to 0
        val localCal = Calendar.getInstance().apply { time = utcDate }
        val localHour = localCal.get(Calendar.HOUR_OF_DAY)

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val localDay = Calendar.getInstance().apply {
            time = utcDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayOffset = ((localDay.timeInMillis - today.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

        return localHour to dayOffset
    }
}