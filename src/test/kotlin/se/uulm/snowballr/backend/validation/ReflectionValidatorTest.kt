package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import io.grpc.reflection.v1.ServerReflectionRequest
import org.junit.jupiter.api.Test

class ReflectionValidatorTest {
    @Test
    fun `When a server reflection request is validated, then no issue is returned`() {
        val request = ServerReflectionRequest.newBuilder().build()
        val result = validateRequest(request)

        EitherAssert.assertThat(result).isRight()
    }
}
