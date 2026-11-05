package com.example.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

object TimeFormatter {
    fun formatRelative(timestamp: Timestamp?): String {
        if (timestamp == null) return "Just now"
        val nowMillis = System.currentTimeMillis()
        val thenMillis = timestamp.toDate().time
        val diffSeconds = max(0L, (nowMillis - thenMillis) / 1000L)

        return when {
            diffSeconds < 45L -> "Just now"
            diffSeconds < 3600L -> "${diffSeconds / 60L}m ago"
            diffSeconds < 86400L -> "${diffSeconds / 3600L}h ago"
            diffSeconds < 604800L -> "${diffSeconds / 86400L}d ago"
            else -> SimpleDateFormat("MMM d", Locale.US).format(timestamp.toDate())
        }
    }
}
