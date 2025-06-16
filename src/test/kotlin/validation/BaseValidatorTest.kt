package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.assertInvalidResult
import se.uulm.snowballr.backend.model.BlankField
import snowballr.Base

class BaseValidatorTest {
    @Nested
    inner class IdRequest {
        @Test
        fun `When a valid ID request is validated, then no issue is returned`() {
            val request =
                Base.Id
                    .newBuilder()
                    .setId("1")
                    .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When a blank ID request is validated, then the 'BlankField' issue is returned`() {
            val request =
                Base.Id
                    .newBuilder()
                    .setId("")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult(result, BlankField::class.java)
        }
    }
}
