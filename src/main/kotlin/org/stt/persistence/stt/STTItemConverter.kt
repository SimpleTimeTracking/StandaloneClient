package org.stt.persistence.stt

import org.stt.model.TimeTrackingItem

import java.time.LocalDateTime

internal class STTItemConverter {
    fun lineToTimeTrackingItem(line: String): TimeTrackingItem {
        val start = parseDate(line.substring(0, 19))!!
        val end = if (line.length >= 39) parseDate(line.substring(20, 39)) else null
        val activityStart = if (end == null) 20 else 40
        val activity = if (line.length > activityStart)
            unescape(line.substring(activityStart))
        else
            ""
        return TimeTrackingItem(activity, start, end)
    }

    private fun unescape(activity: String): String {
        val chars = activity.toCharArray()
        val n = chars.size
        val b = StringBuilder()
        var i = 0
        while (i < n) {
            val next = chars[i]
            if (next == '\\' && i + 1 < n) {
                i++
                if (chars[i] == 'n') {
                    b.append('\n')
                } else {
                    b.append(chars[i])
                }
            } else {
                b.append(next)
            }
            i++
        }
        return b.toString()
    }

    private fun parseDate(from: String): LocalDateTime? {
        val chars = from.toCharArray()
        if (chars[0] < '0' || chars[0] > '9'
                || chars[1] < '0' || chars[1] > '9'
                || chars[2] < '0' || chars[2] > '9'
                || chars[3] < '0' || chars[3] > '9'
                || chars[4] != '-'
                || chars[5] < '0' || chars[5] > '9'
                || chars[6] < '0' || chars[6] > '9'
                || chars[7] != '-'
                || chars[8] < '0' || chars[8] > '9'
                || chars[9] < '0' || chars[9] > '9'
                || chars[10] != '_'
                || chars[11] < '0' || chars[11] > '9'
                || chars[12] < '0' || chars[12] > '9'
                || chars[13] != ':'
                || chars[14] < '0' || chars[14] > '9'
                || chars[15] < '0' || chars[15] > '9'
                || chars[16] != ':'
                || chars[17] < '0' || chars[17] > '9'
                || chars[18] < '0' || chars[18] > '9') {
            return null
        }
        val y = (chars[0] - '0') * 1000 + (chars[1] - '0') * 100 + (chars[2] - '0') * 10 + (chars[3] - '0')
        val mo = (chars[5] - '0') * 10 + (chars[6] - '0')
        val d = (chars[8] - '0') * 10 + (chars[9] - '0')
        val h = (chars[11] - '0') * 10 + (chars[12] - '0')
        val mi = (chars[14] - '0') * 10 + (chars[15] - '0')
        val s = (chars[17] - '0') * 10 + (chars[18] - '0')
        return LocalDateTime.of(y, mo, d, h, mi, s)
    }

    fun timeTrackingItemToLine(item: TimeTrackingItem): String {
        val builder = StringBuilder(80)
        val start = item.start
        appendDateTime(builder, start)
        builder.append(' ')
        item.end?.let { endDateTime ->
            appendDateTime(builder, endDateTime)
            builder.append(' ')
        }

        escape(builder, item.activity)
        return builder.toString()
    }

    private fun escape(b: StringBuilder, activity: String) {
        val chars = activity.toCharArray()
        val n = chars.size
        var i = 0
        while (i < n) {
            val next = chars[i]
            if (next == '\r') {
                b.append("\\n")
                if (i + 1 < n && chars[i + 1] == '\n') {
                    i++
                }
            } else if (next == '\n') {
                b.append("\\n")
            } else if (next == '\\') {
                b.append("\\\\")
            } else {
                b.append(next)
            }
            i++
        }
    }

    private fun appendDateTime(b: StringBuilder, dt: LocalDateTime) {
        val y = dt.year
        val mo = dt.monthValue
        val d = dt.dayOfMonth
        val h = dt.hour
        val mi = dt.minute
        val s = dt.second
        b.append((y / 1000 % 10 + '0'.toInt()).toChar())
        b.append((y / 100 % 10 + '0'.toInt()).toChar())
        b.append((y / 10 % 10 + '0'.toInt()).toChar())
        b.append((y % 10 + '0'.toInt()).toChar())
        b.append('-')
        b.append((mo / 10 % 10 + '0'.toInt()).toChar())
        b.append((mo % 10 + '0'.toInt()).toChar())
        b.append('-')
        b.append((d / 10 % 10 + '0'.toInt()).toChar())
        b.append((d % 10 + '0'.toInt()).toChar())
        b.append('_')
        b.append((h / 10 % 10 + '0'.toInt()).toChar())
        b.append((h % 10 + '0'.toInt()).toChar())
        b.append(':')
        b.append((mi / 10 % 10 + '0'.toInt()).toChar())
        b.append((mi % 10 + '0'.toInt()).toChar())
        b.append(':')
        b.append((s / 10 % 10 + '0'.toInt()).toChar())
        b.append((s % 10 + '0'.toInt()).toChar())
    }
}
