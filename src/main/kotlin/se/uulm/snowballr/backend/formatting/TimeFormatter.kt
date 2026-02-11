package se.uulm.snowballr.backend.formatting

fun daysToHumanReadable(days: Int): String {
    return when (days) {
        0 -> "today"
        1 -> "tomorrow"
        else -> "in $days days"
    }
}
