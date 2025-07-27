package se.uulm.snowballr.backend.validation

import com.google.protobuf.FieldMask
import com.google.protobuf.util.FieldMaskUtil
import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.EnumUnspecified
import se.uulm.snowballr.backend.model.InvalidFieldMask
import se.uulm.snowballr.backend.model.InvalidId
import se.uulm.snowballr.backend.model.TooLongField
import snowballr.CriterionOuterClass.Criterion
import snowballr.CriterionOuterClass.Criterion.Create
import snowballr.CriterionOuterClass.Criterion.Update
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

    @Nested
    inner class UpdateRequest {
        private val validUpdatedCriterion: Criterion.Builder = Criterion
            .newBuilder()
            .setId(UUID.randomUUID().toString())
            .setTag("Test Tag")
            .setName("Test Criterion")
            .setDescription("Test Description")
            .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
        private val validFieldMask: FieldMask = FieldMaskUtil
            .fromStringList(
                listOf(
                    "criterion.tag",
                    "criterion.name",
                    "criterion.description",
                    "criterion.category",
                ),
            )

        private val validUpdateRequestBuilder: Update.Builder =
            Update
                .newBuilder()
                .setCriterion(validUpdatedCriterion)
                .setMask(validFieldMask)

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validUpdateRequestBuilder.build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When a blank field mask is validated, then the 'InvalidFieldMask' issue is returned`() {
            val inValidFieldMask = FieldMaskUtil.fromStringList(emptyList())
            val request =
                validUpdateRequestBuilder
                    .setMask(inValidFieldMask)
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidFieldMask>(result)
        }

        @Test
        fun `When a field mask containing a non-existing field is validated, then the 'InvalidFieldMask' issue is returned`() {
            val request =
                validUpdateRequestBuilder
                    .setMask(FieldMaskUtil.fromStringList(listOf("non_existing_field")))
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidFieldMask>(result)
        }

        @Test
        fun `When an invalid ID is validated, then the 'InvalidId' issue is returned`() {
            val project = validUpdatedCriterion.setId("invalid-id").build()
            val request = validUpdateRequestBuilder
                .setCriterion(project)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "tag",
                "name",
                "description",
            ],
        )
        fun `When a blank field is validated and specified in the field mask, then the 'BlankField' issue is returned`(
            fieldName: String,
        ) {
            val fieldDescriptor = Criterion.getDescriptor().findFieldByName(fieldName)
            val updatedCriterion = validUpdatedCriterion.setField(fieldDescriptor, " ").build()

            val request = validUpdateRequestBuilder
                .setCriterion(updatedCriterion)
                .setMask(FieldMaskUtil.fromStringList(listOf("criterion.$fieldName")))
                .build()

            val result = validateRequest(request)
            assertInvalidResult<BlankField>(result)
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "tag",
                "name",
                "description",
            ],
        )
        fun `When a blank field is validated but not specified in the field mask, then no issue is returned`(
            fieldName: String,
        ) {
            val fieldDescriptor = Criterion.getDescriptor().findFieldByName(fieldName)
            val updatedCriterion = validUpdatedCriterion.setField(fieldDescriptor, " ").build()
            val fieldMask = FieldMaskUtil.fromStringList(listOf("criterion.category"))
            val request = validUpdateRequestBuilder
                .setCriterion(updatedCriterion)
                .setMask(fieldMask)
                .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
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
            val fieldDescriptor = Criterion.getDescriptor().findFieldByName(fieldName)
            val updatedCriterion = validUpdatedCriterion.setField(fieldDescriptor, "a".repeat(length + 1)).build()

            val request = validUpdateRequestBuilder
                .setCriterion(updatedCriterion)
                .setMask(FieldMaskUtil.fromStringList(listOf("criterion.$fieldName")))
                .build()

            val result = validateRequest(request)
            assertInvalidResult<TooLongField>(result)
        }

        @Test
        fun `When an invalid category is provided and specified in the field mask, then the 'EnumUnspecified' issue is returned`() {
            val criterion = validUpdatedCriterion.setCategory(CriterionCategory.CRITERION_CATEGORY_UNSPECIFIED).build()

            val request = validUpdateRequestBuilder
                .setCriterion(criterion)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<EnumUnspecified>(result)
        }

        @Test
        fun `When an invalid category is provided but not specified in the field mask, then no issue is returned`() {
            val criterion = validUpdatedCriterion.setCategory(CriterionCategory.CRITERION_CATEGORY_UNSPECIFIED).build()

            val fieldMask = FieldMaskUtil.fromStringList(listOf("criterion.tag"))
            val request = validUpdateRequestBuilder
                .setCriterion(criterion)
                .setMask(fieldMask)
                .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }
    }
}
