package se.uulm.snowballr.backend.model.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException

class FailedPreconditionExceptionTest {
    @Nested
    inner class EntityNotActiveExceptions {
        @Test
        fun `When creating an EntityNotActiveException, then the message is correctly formatted`() {
            val exception = EntityNotActiveException(EntityType.USER, "1234")

            assertEquals("The User with ID '1234' is not active.", exception.message)
        }
    }
}
