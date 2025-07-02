package se.uulm.snowballr.backend.model

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ParserTest {
    @Nested
    inner class ParseUUID {
        @Test
        fun `When a valid UUID is passed, then it is correctly parsed`() {
            val uuid = "123e4567-e89b-12d3-a456-426614174000"
            assertDoesNotThrow { parseUUID(uuid, "test") }
        }

        @Test
        fun `When an invalid UUID is passed, then an exception is thrown`() {
            val uuid = "invalid-uuid"
            assertThrows<SnowballRException.InvalidIdException.UUID> { parseUUID(uuid, "test") }
        }
    }
}
