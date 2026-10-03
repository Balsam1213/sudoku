package com.balsam.sudoku.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 秒数格式化为 mm:ss 或 h:mm:ss。 */
fun formatDuration(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun formatTimestamp(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val local = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val now = LocalDate.now(zone)
    return when (local.toLocalDate()) {
        now -> local.format(DateTimeFormatter.ofPattern("今天 HH:mm"))
        now.minusDays(1) -> local.format(DateTimeFormatter.ofPattern("昨天 HH:mm"))
        else -> local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }
}
