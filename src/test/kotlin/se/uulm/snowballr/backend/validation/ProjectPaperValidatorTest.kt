package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.InvalidId
import se.uulm.snowballr.backend.model.NotConvertableValue
import snowballr.ProjectOuterClass.Project
import java.util.UUID

class ProjectPaperValidatorTest {
    @Nested
    inner class GetRequest {
        private val validGetRequestBuilder: Project.Paper.Get.Builder =
            Project.Paper.Get
                .newBuilder()
                .setProjectId(UUID.randomUUID().toString())
                .setRelativeProjectPaperId(kotlin.random.Random.nextLong().toString())

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validGetRequestBuilder.build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When an invalid project ID is validated, then the 'InvalidId' issue is returned`() {
            val request = validGetRequestBuilder.setProjectId("invalid-id").build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When a blank relative project paper ID is validated, then the 'BlankField' issue is returned`() {
            val request =
                validGetRequestBuilder
                    .setRelativeProjectPaperId("")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }

        @Test
        fun `When the relative project paper ID can not be converted to a Long, then the 'NotConvertableValue' issue is returned`() {
            val request =
                validGetRequestBuilder
                    .setRelativeProjectPaperId("a")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<NotConvertableValue>(result)
        }
    }
}
