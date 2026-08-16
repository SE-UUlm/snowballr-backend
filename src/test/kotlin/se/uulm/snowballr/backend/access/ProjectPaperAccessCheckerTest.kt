package se.uulm.snowballr.backend.access

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.access.rules.AccessRule
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import java.util.UUID

class ProjectPaperAccessCheckerTest {
    private val projectAccessChecker = mockk<IProjectAccessChecker>()

    private val accessChecker = ProjectPaperAccessChecker(projectAccessChecker)

    private val successAccessRule = AccessRule<UUID> { _, _ -> true }
    private val failureAccessRule = AccessRule<UUID> { _, _ -> throw UnauthorizedTestException() }

    @Nested
    inner class IsAllowedToAddPaperToProject {
        @Test
        fun `When isProjectOrServerAdmin allows access and the project is active, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.ACTIVE)
            val projectResult = Result.success(project)

            every { projectAccessChecker.isProjectOrServerAdmin(AccessType.CREATE) } returns successAccessRule

            assertDoesNotThrow { accessChecker.isAllowedToAddPaperToProject(user, project.id, projectResult) }
        }

        @Test
        fun `When isProjectOrServerAdmin denies access, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(status = ProjectStatus.ACTIVE)
            val projectResult = Result.success(project)

            every { projectAccessChecker.isProjectOrServerAdmin(AccessType.CREATE) } returns failureAccessRule

            assertThrows<UnauthorizedTestException> {
                accessChecker.isAllowedToAddPaperToProject(user, project.id, projectResult)
            }
        }

        @Test
        fun `When isProjectOrServerAdmin allows access but the project is not active, then access is denied`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val project = DataBuilder.createExampleProject(status = ProjectStatus.ARCHIVED)
                val projectResult = Result.success(project)

                every { projectAccessChecker.isProjectOrServerAdmin(AccessType.CREATE) } returns successAccessRule

                assertThrows<EntityNotActiveException> {
                    accessChecker.isAllowedToAddPaperToProject(user, project.id, projectResult)
                }
            }
    }
}
