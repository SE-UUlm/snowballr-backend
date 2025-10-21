package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.EnumUnspecified
import se.uulm.snowballr.backend.model.InvalidId
import snowballr.ReviewOuterClass
import snowballr.ReviewOuterClass.Review.Create
import java.util.UUID

class ReviewValidatorTest {
    @Nested
    inner class CreateRequest {
        private val validCreateRequestBuilder: Create.Builder =
            Create
                .newBuilder()
                .setProjectPaperId(UUID.randomUUID().toString())
                .setDecision(ReviewOuterClass.ReviewDecision.REVIEW_DECISION_ACCEPTED)
                .addAllSelectedCriteriaIds(listOf(UUID.randomUUID().toString()))

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validCreateRequestBuilder.build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When an invalid project paper ID is validated, then the 'InvalidID' issue is returned`() {
            val request =
                validCreateRequestBuilder
                    .setProjectPaperId("invalid-id")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When an invalid decision is validated, then the 'EnumUnspecified' issue is returned`() {
            val request =
                validCreateRequestBuilder
                    .setDecision(ReviewOuterClass.ReviewDecision.REVIEW_DECISION_UNSPECIFIED)
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<EnumUnspecified>(result)
        }

        @Test
        fun `When the list of criteria IDs contains an invalid ID and is validated, then the 'InvalidID' issue is returned`() {
            val request =
                validCreateRequestBuilder
                    .addAllSelectedCriteriaIds(listOf(UUID.randomUUID().toString(), "invalid-id"))
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the list of criteria IDs is empty and is validated, then no issue is returned`() {
            val request =
                validCreateRequestBuilder
                    .clearSelectedCriteriaIds()
                    .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }
    }
}
