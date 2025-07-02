package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.EnumUnspecified
import se.uulm.snowballr.backend.model.InvalidId
import se.uulm.snowballr.backend.model.TooLongField
import snowballr.CriterionOuterClass.Criterion.Create
import snowballr.CriterionOuterClass.CriterionCategory
import java.util.UUID

class CriterionValidatorTest {
    @Nested
    inner class CreateRequest {
        private val validCreateRequestBuilder: Create.Builder =
            Create
                .newBuilder()
                .setProjectId(UUID.randomUUID().toString())
                .setTag("C1")
                .setName("Valid Criterion")
                .setDescription("A valid criterion")
                .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validCreateRequestBuilder.build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When a request with an invalid project ID is validated, then the 'InvalidId' issue is returned`() {
            val request = validCreateRequestBuilder.setProjectId("invalid-id").build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @ParameterizedTest
        @ValueSource(strings = ["tag", "name", "description"])
        fun `When a blank field is validated, then the 'BlankField' issue is returned`(fieldName: String) {
            val request =
                validCreateRequestBuilder
                    .setField(Create.getDescriptor().findFieldByName(fieldName), "")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "tag:${CRITERION_TAG_MAX_LENGTH}",
                "name:${CRITERION_NAME_MAX_LENGTH}",
                "description:${CRITERION_DESCRIPTION_MAX_LENGTH}",
            ],
            delimiter = ':',
        )
        fun `When a too long field is validated, then the 'TooLongField' issue is returned`(
            fieldName: String,
            length: Int,
        ) {
            val request =
                validCreateRequestBuilder
                    .setField(Create.getDescriptor().findFieldByName(fieldName), "a".repeat(length + 1))
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<TooLongField>(result)
        }

        @Test
        fun `When the category is 'UNSPECIFIED' and is validated, then the 'EnumUnspecified' issue is returned`() {
            val request =
                validCreateRequestBuilder
                    .setCategory(CriterionCategory.CRITERION_CATEGORY_UNSPECIFIED)
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<EnumUnspecified>(result)
        }
    }
}
