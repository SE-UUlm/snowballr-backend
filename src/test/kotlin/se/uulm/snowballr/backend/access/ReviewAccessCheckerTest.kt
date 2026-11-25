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
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import java.util.UUID

class ReviewAccessCheckerTest {
    private val projectMemberRepo = mockk<IProjectMemberTableRepo>()
    private val projectAccessChecker = mockk<IProjectAccessChecker>()

    private val accessChecker = ReviewAccessChecker(projectMemberRepo, projectAccessChecker)

    @Nested
    inner class IsAllowedToCreateReview {
        @Test
        fun `When isAllowedToReadProject allows and the project is active, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val project = DataBuilder.createExampleProject(
                id = projectId,
                status = ProjectStatus.ACTIVE,
            )

            coJustRun { projectAccessChecker.isAllowedToReadProject(user, projectId) }

            assertDoesNotThrow { accessChecker.isAllowedToCreateReview(user, projectId, Result.success(project)) }
        }

        @Test
        fun `When isAllowedToReadProject denies access, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val project = DataBuilder.createExampleProject(
                id = projectId,
                status = ProjectStatus.ACTIVE,
            )

            coEvery { projectAccessChecker.isAllowedToReadProject(user, projectId) } throws TestSpecificException()

            assertThrows<TestSpecificException> {
                accessChecker.isAllowedToCreateReview(user, projectId, Result.success(project))
            }
        }

        @Test
        fun `When isAllowedToReadProject allows but the project is not active, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val project = DataBuilder.createExampleProject(
                id = projectId,
                status = ProjectStatus.DELETED,
            )

            coJustRun { projectAccessChecker.isAllowedToReadProject(user, projectId) }

            assertThrows<EntityNotActiveException> {
                accessChecker.isAllowedToCreateReview(user, projectId, Result.success(project))
            }
        }
    }

    @Nested
    inner class IsAllowedToReadReview {
        @Test
        fun `When the user is a member of the project the review belongs to, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val review = DataBuilder.createExampleReview(projectId = projectId)

            coEvery { projectMemberRepo.isProjectMember(projectId, user.id) } returns true

            assertDoesNotThrow { accessChecker.isAllowedToReadReview(user, review) }
        }

        @Test
        fun `When the user is a server admin but not a project member, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.ADMIN)
            val projectId = UUID.randomUUID()
            val review = DataBuilder.createExampleReview(projectId = projectId)

            coEvery { projectMemberRepo.isProjectMember(projectId, user.id) } returns false

            assertDoesNotThrow { accessChecker.isAllowedToReadReview(user, review) }
        }

        @Test
        fun `When the user is not a project member and not a server admin, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val review = DataBuilder.createExampleReview(projectId = projectId)

            coEvery { projectMemberRepo.isProjectMember(projectId, user.id) } returns false

            assertThrows<UnauthorizedReadException> { accessChecker.isAllowedToReadReview(user, review) }
        }
    }
}
