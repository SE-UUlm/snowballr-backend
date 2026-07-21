package se.uulm.snowballr.backend.formatting

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TimeFormatterTest {
    @Nested
    inner class DaysToHumanReadable {
        @Test
        fun `When 0 days are formatted, then 'today' is returned`() {
            assertEquals("today", daysToHumanReadable(0))
        }

        @Test
        fun `When 1 day is formatted, then 'tomorrow' is returned`() {
            assertEquals("tomorrow", daysToHumanReadable(1))
        }

        @Test
        fun `When more than 1 day is formatted, then 'in x days' is returned`() {
            assertEquals("in 5 days", daysToHumanReadable(5))
        }
    }
}
