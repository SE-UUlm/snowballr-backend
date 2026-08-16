package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.EnumUnspecified
import se.uulm.snowballr.backend.model.InvalidEmail
import se.uulm.snowballr.backend.model.InvalidId
import snowballr.ProjectOuterClass.MemberRole
import snowballr.ProjectOuterClass.Project
import snowballr.ProjectOuterClass.Project.Member.Remove
import java.util.UUID

class ProjectMemberValidatorTest {
    @Nested
    inner class RemoveRequest {
        private val validRemoveRequestBuilder: Remove.Builder =
            Remove
                .newBuilder()
                .setProjectId(UUID.randomUUID().toString())
                .setUserEmail("user@example.com")

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validRemoveRequestBuilder.build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When a request with an invalid project ID is validated, then the 'InvalidId' issue is returned`() {
            val request = validRemoveRequestBuilder.setProjectId("invalid-id").build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When a request with an invalid user ID is validated, then the 'InvalidId' issue is returned`() {
            val request = validRemoveRequestBuilder.setUserEmail("invalid-email").build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidEmail>(result)
        }
    }

    @Nested
    inner class InviteRequest {
        private val validInviteRequestBuilder = Project.Member.Invite.newBuilder()
            .setProjectId(UUID.randomUUID().toString())
            .setUserEmail("test.user@example.com")

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validInviteRequestBuilder.build()

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When the project ID is blank, then the 'BlankField' issue is returned`() {
            val request = validInviteRequestBuilder.setProjectId("").build()

            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }

        @Test
        fun `When the project ID is invalid, then the 'InvalidId' issue is returned`() {
            val request = validInviteRequestBuilder.setProjectId("invalid-id").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the user email is invalid, then the 'InvalidEmail' issue is returned`() {
            val request = validInviteRequestBuilder.setUserEmail("invalid-email").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidEmail>(result)
        }
    }

    @Nested
    inner class AcceptRequest {
        private val validAcceptRequestBuilder = Project.Member.Accept.newBuilder()
            .setToken("valid-token")

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validAcceptRequestBuilder.build()

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When the token is blank, then the 'BlankField' issue is returned`() {
            val request = validAcceptRequestBuilder.setToken("").build()

            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }
    }

    @Nested
    inner class MemberUpdateRequest {
        private val validMemberUpdateRequestBuilder = Project.Member.Update.newBuilder()
            .setProjectId(UUID.randomUUID().toString())
            .setUserId(UUID.randomUUID().toString())
            .setNewRole(MemberRole.MEMBER_ROLE_ADMIN)

        @Test
        fun `When a valid request is validated, the no issue is returned`() {
            val request = validMemberUpdateRequestBuilder.build()

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When the project ID is blank, then the 'InvalidId' issue is returned`() {
            val request = validMemberUpdateRequestBuilder.setProjectId("").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the project ID is invalid, then the 'InvalidId' issue is returned`() {
            val request = validMemberUpdateRequestBuilder.setProjectId("invalid-id").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the user ID is blank, then the 'InvalidId' issue is returned`() {
            val request = validMemberUpdateRequestBuilder.setUserId("").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the user ID is invalid, then the 'InvalidId' issue is returned`() {
            val request = validMemberUpdateRequestBuilder.setUserId("invalid-id").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the new role is unspecified, then the 'EnumUnspecified' issue is returned`() {
            val request = validMemberUpdateRequestBuilder.setNewRole(MemberRole.MEMBER_ROLE_UNSPECIFIED).build()

            val result = validateRequest(request)

            assertInvalidResult<EnumUnspecified>(result)
        }
    }
}
