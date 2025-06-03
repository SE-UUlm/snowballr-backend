package se.uulm.snowballr.backend.validation

import arrow.core.Either
import `in`.rcard.assertj.arrowcore.EitherAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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

            EitherAssert.assertThat(result).isLeft()
            val value = (result as Either.Left).value
            assertThat(value.size).isEqualTo(1)
            val issue = value.first()
            assertThat(issue).isInstanceOf(BlankField::class.java)
        }

        @Test
        fun `When a too long name is validated, then the 'TooLongField' issue is returned`() {
            val request =
                validCreateRequestBuilder
                    .setName("a".repeat(PROJECT_NAME_MAX_LENGTH + 1))
                    .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isLeft()
            val value = (result as Either.Left).value
            assertThat(value.size).isEqualTo(1)
            val issue = value.first()
            assertThat(issue).isInstanceOf(TooLongField::class.java)
        }
    }
}
