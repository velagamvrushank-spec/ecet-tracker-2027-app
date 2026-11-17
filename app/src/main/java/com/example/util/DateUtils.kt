package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private const val DAY_MS = 86_400_000L

    fun todayEpochDay(): Long = epochMillisToLocalEpochDay(System.currentTimeMillis())

    fun epochMillisToLocalEpochDay(epochMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMillis
        return civilToEpochDay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun utcMillisToEpochDay(utcMillis: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = utcMillis
        return civilToEpochDay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun epochDayToUtcMillis(epochDay: Long): Long = epochDay * DAY_MS

    fun civilToEpochDay(year: Int, month: Int, day: Int): Long {
        var y = year.toLong()
        val m = month.toLong()
        val d = day.toLong()
        y -= if (m <= 2) 1 else 0
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    data class CivilDate(val year: Int, val month: Int, val day: Int)

    fun epochDayToCivil(epochDay: Long): CivilDate {
        val z = epochDay + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        var y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + (if (mp < 10) 3 else -9)
        y += if (m <= 2) 1 else 0
        return CivilDate(y.toInt(), m.toInt(), d.toInt())
    }

    fun epochDayToLocalCalendar(epochDay: Long): Calendar {
        val civil = epochDayToCivil(epochDay)
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, civil.year)
            set(Calendar.MONTH, civil.month - 1)
            set(Calendar.DAY_OF_MONTH, civil.day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun formatEpochDay(epochDay: Long): String {
        val cal = epochDayToLocalCalendar(epochDay)
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun formatShortDate(epochDay: Long): String {
        val cal = epochDayToLocalCalendar(epochDay)
        return SimpleDateFormat("dd MMM", Locale.getDefault()).format(cal.time)
    }

    fun formatWeekdayShort(epochDay: Long): String {
        val cal = epochDayToLocalCalendar(epochDay)
        return SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
    }

    fun formatMonthYear(year: Int, month: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun formatTimeOfDay(epochMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
    }

    fun dayOfWeekIndexMondayStart(epochDay: Long): Int {
        // 1970-01-01 was Thursday (index 3 in Mon=0..Sun=6)
        return (((epochDay + 3) % 7 + 7) % 7).toInt()
    }

    fun daysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun lastNDays(n: Int, endingAt: Long = todayEpochDay()): List<Long> {
        return ((endingAt - n + 1)..endingAt).toList()
    }

    fun formatDurationHms(durationMs: Long): String {
        val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatHoursCompact(totalMinutes: Int): String {
        val safeMinutes = totalMinutes.coerceAtLeast(0)
        val hours = safeMinutes / 60
        val mins = safeMinutes % 60
        return when {
            hours == 0 -> "${mins}m"
            mins == 0 -> "${hours}h"
            else -> "${hours}h ${mins}m"
        }
    }

    fun formatHoursDecimal(totalSeconds: Long): String {
        val hours = totalSeconds / 3600.0
        return String.format(Locale.US, "%.1fh", hours)
    }
}
