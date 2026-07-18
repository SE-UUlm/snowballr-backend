package se.uulm.snowballr.backend.model.exception

import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.invalidargument.InvalidUUIDException

class SnowballRExceptionTest {
    @Test
    fun `When getting grpc status from a SnowballR exception, then the configured status is returned`() {
        val exception = InvalidUUIDException(EntityType.USER, "invalid-uuid")

        assertEquals(Status.INVALID_ARGUMENT, exception.getGrpcStatus())
    }
}
