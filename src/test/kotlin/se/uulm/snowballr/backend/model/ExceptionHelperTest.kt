package se.uulm.snowballr.backend.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ExceptionHelperTest {
    @Test
    fun `When a single ID is displayed with default identifier type, then it is formatted correctly`() {
        val output = displayEntityIds(listOf("123"))

        assertEquals("with ID '123'", output)
    }

    @Test
    fun `When multiple IDs are displayed, then they are formatted with commas and and`() {
        val output = displayEntityIds(listOf(1, 2, 3), IdentifierType.LOCAL_ID)

        assertEquals("with local ID '1', '2' and '3'", output)
    }

    @Test
    fun `When an empty list is displayed, then an exception is thrown`() {
        assertThrows<IllegalArgumentException> { displayEntityIds(emptyList()) }
    }
}
