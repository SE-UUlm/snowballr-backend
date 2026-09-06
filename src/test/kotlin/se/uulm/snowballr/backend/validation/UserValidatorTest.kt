package se.uulm.snowballr.backend.validation

import com.google.protobuf.FieldMask
import com.google.protobuf.util.FieldMaskUtil
import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.EnumUnspecified
import se.uulm.snowballr.backend.model.InvalidEmail
import se.uulm.snowballr.backend.model.InvalidFieldMask
import se.uulm.snowballr.backend.model.InvalidId
import se.uulm.snowballr.backend.model.TooLongField
import se.uulm.snowballr.backend.model.TooLongList
import se.uulm.snowballr.backend.model.dto.user.UserField
import se.uulm.snowballr.backend.validation.UserValidator.MAX_CRITERIA_COUNT
import se.uulm.snowballr.backend.validation.UserValidator.getGrpcPathsForUserField
import se.uulm.snowballr.backend.validation.UserValidator.isValidUserSettingsUpdateField
import se.uulm.snowballr.backend.validation.UserValidator.isValidUserUpdateField
import snowballr.CriterionOuterClass.Criterion
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass.Project
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import snowballr.UserOuterClass.User
import snowballr.UserOuterClass.User.Update
import snowballr.UserOuterClass.UserRole
import snowballr.UserSettingsOuterClass.UserSettings
import java.util.UUID

class UserValidatorTest {
    companion object {
        @JvmStatic
        fun invalidUserUpdateFields() = UserField.entries
            .filterNot { isValidUserUpdateField(it) }
            .map { Arguments.of(getGrpcPathsForUserField(it)) }

        @JvmStatic
        fun invalidUserSettingsUpdateFields() = UserField.entries
            .filterNot { isValidUserSettingsUpdateField(it) }
            .map { Arguments.of(getGrpcPathsForUserField(it)) }
    }

    @Nested
    inner class UpdateRequest {
        private val validUpdatedUser: User.Builder = User.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setEmail("test.user@example.com")
            .setFirstName("test")
            .setLastName("user")
            .setRole(UserRole.USER_ROLE_DEFAULT)

        private val validFieldMask: FieldMask = FieldMaskUtil
            .fromStringList(
                listOf("user.id") + UserField.entries
                    .filter { isValidUserUpdateField(it) }
                    .map { getGrpcPathsForUserField(it) },
            )

        private val validUpdateRequestBuilder: Update.Builder =
            Update
                .newBuilder()
                .setUser(validUpdatedUser)
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
        fun `When a field mask containing a nonexistent field is validated, then the 'InvalidFieldMask' issue is returned`() {
            val request =
                validUpdateRequestBuilder
                    .setMask(FieldMaskUtil.fromStringList(listOf("non_existent_field")))
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidFieldMask>(result)
        }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.validation.UserValidatorTest#invalidUserUpdateFields")
        fun `When a field mask contains an invalid field, then the 'InvalidFieldMask' issue is returned`(
            field: String,
        ) {
            val request = validUpdateRequestBuilder.setMask(FieldMaskUtil.fromStringList(listOf(field))).build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidFieldMask>(result)
        }

        @Test
        fun `When an invalid ID is validated, then the 'InvalidId' issue is returned`() {
            val user = validUpdatedUser.setId("invalid-id").build()
            val request = validUpdateRequestBuilder
                .setUser(user)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When an invalid email is provided and specified in the field mask, then the 'InvalidEmail' issue is returned`() {
            val user = validUpdatedUser.setEmail("wrong-email").build()
            val request = validUpdateRequestBuilder
                .setUser(user)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidEmail>(result)
        }

        @Test
        fun `When an invalid email is provided but not specified in the field mask, then no issue is returned`() {
            val user = validUpdatedUser.setEmail("wrong-email").build()
            val fieldMask = FieldMaskUtil.fromStringList(listOf("user.first_name"))
            val request = validUpdateRequestBuilder
                .setUser(user)
                .setMask(fieldMask)
                .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When an invalid firstname is provided and specified in the field mask, then an issue is returned`() {
            val user = validUpdatedUser.setFirstName("  ").build()
            val request = validUpdateRequestBuilder
                .setUser(user)
                .setMask(validFieldMask)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }

        @Test
        fun `When an invalid firstname is provided but not specified in the field mask, then no issue is returned`() {
            val user = validUpdatedUser.setFirstName("  ").build()
            val fieldMask = FieldMaskUtil.fromStringList(listOf("user.last_name"))
            val request = validUpdateRequestBuilder
                .setUser(user)
                .setMask(fieldMask)
                .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When an invalid lastname is provided and specified in the field mask, then an issue is returned`() {
            val user = validUpdatedUser.setLastName("user".repeat(26)).build()
            val request = validUpdateRequestBuilder
                .setUser(user)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<TooLongField>(result)
        }

        @Test
        fun `When an invalid lastname is provided but not specified in the field mask, then no issue is returned`() {
            val user = validUpdatedUser.setLastName("user".repeat(26)).build()
            val fieldMask = FieldMaskUtil.fromStringList(listOf("user.first_name"))
            val request = validUpdateRequestBuilder
                .setUser(user)
                .setMask(fieldMask)
                .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When an invalid role is provided and specified in the field mask, then the 'EnumUnspecified' issue is returned`() {
            val user = validUpdatedUser.setRole(UserRole.USER_ROLE_UNSPECIFIED).build()
            val request = validUpdateRequestBuilder
                .setUser(user)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<EnumUnspecified>(result)
        }

        @Test
        fun `When an invalid role is provided but not specified in the field mask, then no issue is returned`() {
            val user = validUpdatedUser.setRole(UserRole.USER_ROLE_UNSPECIFIED).build()
            val fieldMask = FieldMaskUtil.fromStringList(listOf("user.first_name"))
            val request = validUpdateRequestBuilder
                .setUser(user)
                .setMask(fieldMask)
                .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }
    }

    /**
     * Note: We don't test the validation of the project settings since they are already tested by the ProjectValidator
     * tests. Furthermore, we don't test the validation of the criteria since they are already tested by the
     * CriterionValidator tests.
     */
    @Nested
    inner class UpdateSettingsRequest {
        private val validDecisionMatrixBuilder: ReviewDecisionMatrix.Builder =
            ReviewDecisionMatrix.newBuilder()
                .setNumberOfReviewers(3)

        private val validProjectSettingsBuilder: Project.Settings.Builder = Project.Settings.newBuilder()
            .setSimilarityThreshold(1F)
            .setSnowballingType(SnowballingType.SNOWBALLING_TYPE_FORWARD)
            .setReviewMaybeAllowed(false)
            .setDecisionMatrix(validDecisionMatrixBuilder.build())

        private val validCriterionBuilder: Criterion.Builder = Criterion
            .newBuilder()
            .setId(UUID.randomUUID().toString())
            .setTag("Test Tag")
            .setName("Test Criterion")
            .setDescription("Test Description")
            .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)

        private val validSettingsBuilder: UserSettings.Builder = UserSettings.newBuilder()
            .setShowHotkeys(true)
            .setReviewMode(true)
            .setDefaultCriteria(Criterion.List.newBuilder().addAllCriteria(listOf(validCriterionBuilder.build())))
            .setDefaultProjectSettings(validProjectSettingsBuilder.build())

        private val validFieldMask: FieldMask = FieldMaskUtil
            .fromStringList(
                UserField.entries
                    .filter { isValidUserSettingsUpdateField(it) }
                    .map { getGrpcPathsForUserField(it) },
            )

        private val validUpdateRequestBuilder: UserSettings.Update.Builder =
            UserSettings.Update
                .newBuilder()
                .setUserSettings(validSettingsBuilder)
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
        fun `When a field mask containing a nonexistent field is validated, then the 'InvalidFieldMask' issue is returned`() {
            val request =
                validUpdateRequestBuilder
                    .setMask(FieldMaskUtil.fromStringList(listOf("non_existent_field")))
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidFieldMask>(result)
        }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.validation.UserValidatorTest#invalidUserSettingsUpdateFields")
        fun `When a field mask contains an invalid field, then the 'InvalidFieldMask' issue is returned`(
            field: String,
        ) {
            val request = validUpdateRequestBuilder.setMask(FieldMaskUtil.fromStringList(listOf(field))).build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidFieldMask>(result)
        }

        @Test
        fun `When the default criteria list is too long, then a 'TooLongList' issue is returned`() {
            val criteria = mutableListOf<Criterion>()
            (1..MAX_CRITERIA_COUNT + 1).forEach { _ ->
                criteria.add(validCriterionBuilder.build())
            }
            val request = validUpdateRequestBuilder
                .setUserSettings(
                    validSettingsBuilder.setDefaultCriteria(Criterion.List.newBuilder().addAllCriteria(criteria)),
                )
                .build()

            val result = validateRequest(request)

            assertInvalidResult<TooLongList>(result)
        }

        @Test
        fun `When the criteria list is empty, then no issue is returned`() {
            val request = validUpdateRequestBuilder
                .setUserSettings(validSettingsBuilder.clearDefaultCriteria())
                .build()

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }
    }
}
