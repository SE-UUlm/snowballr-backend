package se.uulm.snowballr.backend.model.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.invalidargument.InvalidUUIDException
import se.uulm.snowballr.backend.model.exception.invalidargument.StageOutOfRangeException

class InvalidArgumentExceptionTest {
    @Nested
    inner class InvalidUUIDExceptions {
        @Test
        fun `When creating an InvalidUUIDException, then the message is correctly formatted`() {
            val uuid = "invalid-uuid"
            val exception = InvalidUUIDException(EntityType.USER, uuid)

            assertEquals("The ID '$uuid' of the user is not a valid UUID.", exception.message)
        }
    }

    @Nested
    inner class StageOutOfRangeExceptions {
        @Test
        fun `When creating a StageOutOfRangeException, then the message is correctly formatted`() {
            val exception = StageOutOfRangeException(100, 120)

            assertEquals("The stage 100 is not in the valid range from 0 to 120.", exception.message)
        }
    }
}
