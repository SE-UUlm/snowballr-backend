package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.InvalidId
import se.uulm.snowballr.backend.model.OutOfRangeValue
import se.uulm.snowballr.backend.model.TooLongField
import se.uulm.snowballr.backend.validation.ProjectPaperValidator.QUERY_MAX_LENGTH
import snowballr.ProjectOuterClass.Project
import java.util.UUID

class ProjectPaperValidatorTest {
    @Nested
    inner class GetRequest {
        private val validGetRequestBuilder: Project.Paper.Get.Builder =
            Project.Paper.Get
                .newBuilder()
                .setProjectId(UUID.randomUUID().toString())
                .setRelativeProjectPaperId(0L.toString())

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
        fun `When a blank relative project paper ID is validated, then the 'InvalidId' issue is returned`() {
            val request =
                validGetRequestBuilder
                    .setRelativeProjectPaperId("")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the relative project paper ID can not be converted to a Long, then the 'InvalidId' issue is returned`() {
            val request =
                validGetRequestBuilder
                    .setRelativeProjectPaperId("a")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the relative project paper ID is negative, then the 'InvalidId' issue is returned`() {
            val request =
                validGetRequestBuilder
                    .setRelativeProjectPaperId((-1L).toString())
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }
    }

    @Nested
    inner class AddRequest {
        private val validAddRequestBuilder: Project.Paper.Add.Builder =
            Project.Paper.Add
                .newBuilder()
                .setProjectId(UUID.randomUUID().toString())
                .setPaperId(UUID.randomUUID().toString())
                .setStage(0)

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validAddRequestBuilder.build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When an invalid project ID is validated, then the 'InvalidId' issue is returned`() {
            val request = validAddRequestBuilder.setProjectId("invalid-id").build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When an invalid paper ID is validated, then the 'InvalidId' issue is returned`() {
            val request = validAddRequestBuilder.setPaperId("invalid-id").build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When an invalid stage is validated, then the 'OutOfRange' issue is returned`() {
            val request = validAddRequestBuilder.setStage(-1).build()
            val result = validateRequest(request)

            assertInvalidResult<OutOfRangeValue<Long>>(result)
        }
    }

    @Nested
    inner class SearchQueryRequest {
        private val validSearchQueryRequestBuilder: Project.Paper.SearchQuery.Builder =
            Project.Paper.SearchQuery
                .newBuilder()
                .setProjectId(UUID.randomUUID().toString())
                .setQuery("Foo")

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validSearchQueryRequestBuilder.build()

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When an invalid project ID is validated, then the 'InvalidId' issue is returned`() {
            val request = validSearchQueryRequestBuilder.setProjectId("invalid-id").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When a blank query validated, then the 'BlankField' issue is returned`() {
            val request = validSearchQueryRequestBuilder.setQuery("").build()

            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }

        @Test
        fun `When a too long query validated, then the 'TooLongField' issue is returned`() {
            val request = validSearchQueryRequestBuilder.setQuery("a".repeat(QUERY_MAX_LENGTH + 1)).build()

            val result = validateRequest(request)

            assertInvalidResult<TooLongField>(result)
        }
    }
}
