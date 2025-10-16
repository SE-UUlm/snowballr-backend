package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.InvalidId
import snowballr.ProjectOuterClass.Project.Member.Remove
import java.util.UUID

class ProjectMemberValidatorTest {
    @Nested
    inner class RemoveRequest {
        private val validRemoveRequestBuilder: Remove.Builder =
            Remove
                .newBuilder()
                .setProjectId(UUID.randomUUID().toString())
                .setUserId(UUID.randomUUID().toString())

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
            val request = validRemoveRequestBuilder.setUserId("invalid-id").build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }
    }
}
