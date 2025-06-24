package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import io.grpc.health.v1.HealthCheckRequest
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.assertInvalidResult
import se.uulm.snowballr.backend.model.UnknownRequest

class ValidatorTest {
    @Test
    fun `When an unknown request is validated, then the 'UnknownRequest' issue is returned`() {
        val result = validateRequest(object {})

        assertInvalidResult(result, UnknownRequest::class.java)
    }

    @Test
    fun `When healthcheck request is validated, then no issue is returned`() {
        val result = validateRequest(HealthCheckRequest.getDefaultInstance())

        EitherAssert.assertThat(result).isRight()
    }
}
