package com.hz6826.campusnet.remote

import java.util.Locale

/** 字节数转成人看的单位；和本体里的实现保持一致。 */
internal fun formatBytes(bytes: Double): String {
    if (bytes <= 0) return "0 B"
    var value = bytes
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return String.format(Locale.CHINA, "%.2f %s", value, units[index])
}

/** 秒转「X 天 Y 小时 Z 分」。 */
internal fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "0 秒"
    val d = seconds / 86400
    val h = (seconds % 86400) / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val parts = mutableListOf<String>()
    if (d > 0) parts += "$d 天"
    if (h > 0 || d > 0) parts += "$h 小时"
    if (m > 0 || h > 0 || d > 0) parts += "$m 分"
    if (s > 0 && parts.isEmpty()) parts += "$s 秒"
    if (parts.isEmpty()) parts += "0 秒"
    return parts.joinToString(" ")
}
