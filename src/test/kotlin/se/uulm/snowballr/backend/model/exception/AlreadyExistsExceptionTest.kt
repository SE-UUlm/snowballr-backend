package se.uulm.snowballr.backend.model.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.exception.alreadyexists.DuplicateReviewException
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicatePaperException
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateProjectPaperException
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateUserException
import java.util.UUID

class AlreadyExistsExceptionTest {
    @Nested
    inner class DuplicateEntityExceptions {
        @Test
        fun `When creating a DuplicatePaperException, then the message is correctly formatted`() {
            val exception = DuplicatePaperException(listOf(ExternalId(ExternalIdType.DOI, "foo/bar")))

            assertEquals("Paper with external ID 'external-id' already exists.", exception.message)
        }

        @Test
        fun `When creating a DuplicateProjectPaperException, then the message is correctly formatted`() {
            val projectId = UUID.randomUUID()
            val paperId = UUID.randomUUID()
            val exception = DuplicateProjectPaperException(projectId, paperId)

            assertEquals("Project paper with ID '$projectId' and '$paperId' already exists.", exception.message)
        }

        @Test
        fun `When creating a DuplicateUserException, then the message is correctly formatted`() {
            val email = "john.doe@example.com"
            val exception = DuplicateUserException(email)

            assertEquals("User with email '$email' already exists.", exception.message)
        }
    }

    @Nested
    inner class DuplicateReviewExceptions {
        @Test
        fun `When creating a DuplicateReviewException, then the message is correctly formatted`() {
            val projectPaperId = UUID.randomUUID()
            val userId = UUID.randomUUID()
            val exception = DuplicateReviewException(projectPaperId, userId)

            assertEquals(
                "Project paper with ID '$projectPaperId' was already reviewed by user with ID '$userId'.",
                exception.message,
            )
        }
    }
}
