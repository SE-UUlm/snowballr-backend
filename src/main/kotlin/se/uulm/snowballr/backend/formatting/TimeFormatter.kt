package se.uulm.snowballr.backend.formatting

/**
 * Converts a number of days into a human-readable string.
 *
 * Examples:
 * - 0 -> "today"
 * - 1 -> "tomorrow"
 * - x for x > 1 -> "in x days"
 *
 * @param days The number of days to convert.
 * @return A human-readable string representing the number of days.
 */
fun daysToHumanReadable(days: Int): String {
    return when (days) {
        0 -> "today"
        1 -> "tomorrow"
        else -> "in $days days"
    }
}
