package se.uulm.snowballr.backend.model.exception.internal

import io.grpc.Status
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AccessRuleCheckFailedExceptionTest {
    @Test
    fun `When creating an AccessRuleCheckFailedException, then metadata is correct`() {
        val exception = AccessRuleCheckFailedException()

        assertEquals(
            "Access rule check failed without throwing a specified exception. " +
                "Please add a more specific exception to each path in the rule chain.",
            exception.message,
        )
        assertEquals(Status.INTERNAL, exception.getGrpcStatus())
    }
}
