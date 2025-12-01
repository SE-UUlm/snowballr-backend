package se.uulm.snowballr.backend.model

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.exception.invalidargument.InvalidUUIDException

class ParserTest {
    @Nested
    inner class ParseUUID {
        @Test
        fun `When a valid UUID is passed, then it is correctly parsed`() {
            val uuid = "123e4567-e89b-12d3-a456-426614174000"
            assertDoesNotThrow { parseUUID(uuid, EntityType.USER) }
        }

        @Test
        fun `When an invalid UUID is passed, then an InvalidUUIDException is thrown`() {
            val uuid = "invalid-uuid"
            assertThrows<InvalidUUIDException> { parseUUID(uuid, EntityType.USER) }
        }
    }
}
