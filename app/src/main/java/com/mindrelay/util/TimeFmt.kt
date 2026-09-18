package com.mindrelay.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private val MINUTE = 60_000L
private val HOUR = 60 * MINUTE
private val DAY = 24 * HOUR

private fun now() = Clock.System.now().toEpochMilliseconds()

private fun at(t: Long) = Instant.fromEpochMilliseconds(t).toLocalDateTime(TimeZone.currentSystemDefault())

/** "just now", "2 hours ago", "yesterday", "3 days ago", else "d/m". */
fun relativeAgo(ts: Long): String {
    val diff = now() - ts
    return when {
        diff < MINUTE -> "just now"
        diff < HOUR -> "${diff / MINUTE} min ago"
        diff < 2 * HOUR -> "1 hour ago"
        diff < DAY -> "${diff / HOUR} hours ago"
        diff < 2 * DAY -> "yesterday"
        diff < 7 * DAY -> "${diff / DAY} days ago"
        else -> "on ${dateSlash(ts)}"
    }
}

fun dateSlash(ts: Long): String {
    val d = at(ts).date
    return "${d.dayOfMonth}/${d.monthNumber}"
}

/** "09:12" */
fun clockTime(ts: Long): String {
    val t = at(ts).time
    return "%02d:%02d".format(t.hour, t.minute)
}

fun workedAgo(ts: Long?): String = ts?.let { relativeAgo(it) } ?: "Never"

/** "1h 20m" duration between two instants. */
fun durationText(startedAt: Long, endedAt: Long?): String {
    val end = endedAt ?: now()
    val minutes = ((end - startedAt) / MINUTE).coerceAtLeast(0)
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

/** Stopwatch "00:00:00" for an active session chip. */
fun stopwatch(startedAt: Long): String {
    val seconds = ((now() - startedAt) / 1000).coerceAtLeast(0)
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

/** Revisit dates are stored as epoch day Int (room-safe). */
fun epochDayToMillis(day: Int, tz: TimeZone = TimeZone.currentSystemDefault()): Long =
    LocalDate.fromEpochDays(day).atStartOfDayIn(tz).toInstant(tz).toEpochMilliseconds()

fun millisToDate(ts: Long): LocalDate = at(ts).date

/** Inverse of [epochDayToMillis] for editing forms. */
fun millisToEpochDay(ts: Long, tz: TimeZone = TimeZone.currentSystemDefault()): Int =
    Instant.fromEpochMilliseconds(ts).toLocalDateTime(tz).date.toEpochDays()

fun nowEpochDay(): Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays()

fun localDateToEpochDay(date: LocalDate): Int = date.toEpochDays()

fun epochDayToLocalDate(day: Int): LocalDate = LocalDate.fromEpochDays(day)
