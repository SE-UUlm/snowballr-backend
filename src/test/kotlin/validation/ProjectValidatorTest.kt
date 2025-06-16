package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.assertInvalidResult
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.TooLongField
import snowballr.ProjectOuterClass.Project.Create

class ProjectValidatorTest {
    @Nested
    inner class CreateRequest {
        private val validCreateRequestBuilder: Create.Builder =
            Create
                .newBuilder()
                .setName("Valid Project Name")

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validCreateRequestBuilder.build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When a blank name is validated, then the 'BlankField' issue is returned`() {
            val request =
                validCreateRequestBuilder
                    .setName("")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult(result, BlankField::class.java)
        }

        @Test
        fun `When a too long name is validated, then the 'TooLongField' issue is returned`() {
            val request =
                validCreateRequestBuilder
                    .setName("a".repeat(PROJECT_NAME_MAX_LENGTH + 1))
                    .build()
            val result = validateRequest(request)

            assertInvalidResult(result, TooLongField::class.java)
        }
    }
}
