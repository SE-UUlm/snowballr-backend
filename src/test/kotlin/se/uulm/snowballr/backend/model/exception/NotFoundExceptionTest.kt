package se.uulm.snowballr.backend.model.exception

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.exception.notfound.EntityNotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.InvitationTokenNotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.VerificationTokenNotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.PaperNotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectPaperNotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundByEmailException
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundException
import java.util.UUID
import kotlin.test.assertEquals

class NotFoundExceptionTest {
    private val testId = UUID.randomUUID()

    @Nested
    inner class EntityNotFoundExceptions {
        @Test
        fun `When creating a EntityNotFoundException, then the message is correctly formatted`() {
            val id = "id-1"
            val exception = EntityNotFoundException(
                EntityType.USER,
                id,
                identifierType = IdentifierType.ID,
                location = " in test",
            )

            assertEquals("User with ID '$id' not found in test.", exception.message)
        }

        @Test
        fun `When creating a PaperNotFoundException, then the message is correctly formatted`() {
            val exception = PaperNotFoundException(testId)

            assertEquals("Paper with ID '$testId' not found.", exception.message)
        }

        @Test
        fun `When creating a ProjectNotFoundException, then the message is correctly formatted`() {
            val exception = ProjectNotFoundException(testId)

            assertEquals("Project with ID '$testId' not found.", exception.message)
        }

        @Test
        fun `When creating a ProjectPaperNotFoundException, then the message is correctly formatted`() {
            val localId = 123L
            val exception = ProjectPaperNotFoundException(localId, testId)

            assertEquals(
                "Project paper with local ID '$localId' not found in the project with ID $testId.",
                exception.message,
            )
        }

        @Test
        fun `When creating a UserNotFoundByEmailException, then the message is correctly formatted`() {
            val email = "test@example.com"
            val exception = UserNotFoundByEmailException(email)

            assertEquals("User with email '$email' not found.", exception.message)
        }

        @Test
        fun `When creating a UserNotFoundException, then the message is correctly formatted`() {
            val exception = UserNotFoundException(testId)

            assertEquals("User with ID '$testId' not found.", exception.message)
        }
    }

    @Test
    fun `When creating a InvitationTokenNotFoundException, the the message is correctly formatted`() {
        val exception = InvitationTokenNotFoundException()

        assertEquals("Invitation token not found.", exception.message)
    }

    @Test
    fun `When creating a VerificationTokenNotFoundException, the the message is correctly formatted`() {
        val exception = VerificationTokenNotFoundException()

        assertEquals("Verification token not found.", exception.message)
    }
}
