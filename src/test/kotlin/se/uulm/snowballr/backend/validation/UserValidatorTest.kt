package se.uulm.snowballr.backend.validation

import com.google.protobuf.FieldMask
import com.google.protobuf.util.FieldMaskUtil
import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.EnumUnspecified
import se.uulm.snowballr.backend.model.InvalidEmail
import se.uulm.snowballr.backend.model.InvalidFieldMask
import se.uulm.snowballr.backend.model.InvalidId
import se.uulm.snowballr.backend.model.TooLongField
import snowballr.UserOuterClass.User
import snowballr.UserOuterClass.User.Update
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class UserValidatorTest {
    @Nested
    inner class UpdateRequest {
        private val validUpdatedUser: User.Builder = User.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setEmail("test.user@example.com")
            .setFirstName("test")
            .setLastName("user")
            .setRole(UserRole.USER_ROLE_DEFAULT)
        private val validFieldMask: FieldMask = FieldMaskUtil
            .fromStringList(listOf("user.id", "user.email", "user.first_name", "user.last_name", "user.role"))

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

        @Test
        fun `When a field mask contains the 'status' field, then the 'InvalidFieldMask' issue is returned`() {
            val request = validUpdateRequestBuilder.setMask(FieldMaskUtil.fromStringList(listOf("user.status"))).build()
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
}
