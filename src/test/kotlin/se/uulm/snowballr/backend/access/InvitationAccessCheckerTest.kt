package se.uulm.snowballr.backend.access

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException

class InvitationAccessCheckerTest {
    private val projectAccessChecker = mockk<IProjectAccessChecker>()

    private val accessChecker = InvitationAccessChecker(projectAccessChecker)

    @Nested
    inner class IsAllowedToInviteUserToProject {
        @Test
        fun `When isProjectOrServerAdmin allows access and the project is active, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_ACTIVE)
            val projectResult = Result.success(project)

            coJustRun { projectAccessChecker.isProjectOrServerAdmin(user, project.id, AccessType.READ) }

            assertDoesNotThrow { accessChecker.isAllowedToInviteUserToProject(user, project.id, projectResult) }
        }

        @Test
        fun `When isProjectOrServerAdmin denies access, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_ACTIVE)
            val projectResult = Result.success(project)

            coEvery {
                projectAccessChecker.isProjectOrServerAdmin(user, project.id, AccessType.READ)
            } throws UnauthorizedTestException()

            assertThrows<UnauthorizedTestException> {
                accessChecker.isAllowedToInviteUserToProject(user, project.id, projectResult)
            }
        }

        @Test
        fun `When isProjectOrServerAdmin allows access, but the project is inactive, then access is allowed`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_ARCHIVED)
                val projectResult = Result.success(project)

                coJustRun { projectAccessChecker.isProjectOrServerAdmin(user, project.id, AccessType.READ) }

                assertThrows<EntityNotActiveException> {
                    accessChecker.isAllowedToInviteUserToProject(user, project.id, projectResult)
                }
            }
    }
}
