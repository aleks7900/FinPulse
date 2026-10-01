package md.alexlab.finpulse.core.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DateFormatterUtils {

    fun formatDate(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val localDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
        return localDate.format(formatter)
    }

    fun formatDateTime(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val zonedDateTime = Instant.ofEpochMilli(millis).atZone(zone)
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale.getDefault())
        return zonedDateTime.format(formatter)
    }

    fun formatMonthYear(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val zonedDateTime = Instant.ofEpochMilli(millis).atZone(zone)
        val formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
        return zonedDateTime.format(formatter)
    }
}
