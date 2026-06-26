package se.uulm.snowballr.backend.access

import io.mockk.coEvery
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
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.projectmember.MemberRole
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import java.util.UUID

class CriterionAccessCheckerTest {
    private val projectMemberRepo = mockk<IProjectMemberTableRepo>()
    private val projectAccessChecker = mockk<IProjectAccessChecker>()

    private val accessChecker = CriterionAccessChecker(projectMemberRepo, projectAccessChecker)

    private val successAccessRule = AccessRule<UUID> { _, _ -> true }
    private val failureAccessRule = AccessRule<UUID> { _, _ -> throw UnauthorizedTestException() }

    @Nested
    inner class IsAllowedToCreateProjectCriterion {
        @Test
        fun `When isProjectOrServerAdmin and isProjectActiveById both allow access, then access is allowed`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()

                every {
                    projectAccessChecker.isProjectOrServerAdmin(AccessType.CREATE, EntityType.CRITERION)
                } returns successAccessRule
                every { projectAccessChecker.isProjectActiveById() } returns successAccessRule

                assertDoesNotThrow { accessChecker.isAllowedToCreateProjectCriterion(user, projectId) }
            }

        @Test
        fun `When isProjectOrServerAdmin denies access, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()

            every {
                projectAccessChecker.isProjectOrServerAdmin(AccessType.CREATE, EntityType.CRITERION)
            } returns failureAccessRule

            assertThrows<UnauthorizedTestException> { accessChecker.isAllowedToCreateProjectCriterion(user, projectId) }
        }

        @Test
        fun `When isProjectActiveById allows, but isProjectOrServerAdmin denies access, then access is denied`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()

                every {
                    projectAccessChecker.isProjectOrServerAdmin(AccessType.CREATE, EntityType.CRITERION)
                } returns failureAccessRule
                every { projectAccessChecker.isProjectActiveById() } returns successAccessRule

                assertThrows<UnauthorizedTestException> {
                    accessChecker.isAllowedToCreateProjectCriterion(user, projectId)
                }
            }
    }

    @Nested
    inner class IsAllowedToReadCriterion {
        @Test
        fun `When the user is the creator of the criterion, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleUserCriterion(createdBy = user.id)

            assertDoesNotThrow { accessChecker.isAllowedToReadCriterion(user, criterion) }
        }

        @Test
        fun `When the user is in the project of the criterion, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val criterion = DataBuilder.createExampleProjectCriterion(projectId = projectId)

            coEvery { projectMemberRepo.isProjectMember(projectId, user.id) } returns true

            assertDoesNotThrow { accessChecker.isAllowedToReadCriterion(user, criterion) }
        }

        @Test
        fun `When the user is a server admin, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val projectId = UUID.randomUUID()
            val criterion = DataBuilder.createExampleProjectCriterion(projectId = projectId)

            coEvery { projectMemberRepo.isProjectMember(projectId, user.id) } returns false

            assertDoesNotThrow { accessChecker.isAllowedToReadCriterion(user, criterion) }
        }

        @Test
        fun `When the user is not the creator, not in the project, and not a server admin, then access is denied`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()
                val criterion = DataBuilder.createExampleProjectCriterion(projectId = projectId)

                coEvery { projectMemberRepo.isProjectMember(projectId, user.id) } returns false

                assertThrows<UnauthorizedReadException> { accessChecker.isAllowedToReadCriterion(user, criterion) }
            }

        @Test
        fun `When the user is not the creator of a user criterion and not a server admin, then access is denied`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val criterion = DataBuilder.createExampleUserCriterion()

                assertThrows<UnauthorizedReadException> { accessChecker.isAllowedToReadCriterion(user, criterion) }
            }
    }

    @Nested
    inner class IsAllowedToUpdateCriterion {
        @Test
        fun `When the user is the creator of the criterion, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleUserCriterion(createdBy = user.id)

            assertDoesNotThrow { accessChecker.isAllowedToUpdateCriterion(user, criterion) }
        }

        @Test
        fun `When the user is an admin in the active project of the criterion, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val projectAdmin = DataBuilder.createExampleProjectMember(
                userId = user.id,
                projectId = projectId,
                role = MemberRole.MEMBER_ROLE_ADMIN,
            )
            val criterion = DataBuilder.createExampleProjectCriterion(projectId = projectId)

            coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns listOf(projectAdmin)
            coEvery { projectAccessChecker.isProjectActiveById() } returns successAccessRule

            assertDoesNotThrow { accessChecker.isAllowedToUpdateCriterion(user, criterion) }
        }

        @Test
        fun `When the user is an admin in the inactive project of the criterion, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val projectAdmin = DataBuilder.createExampleProjectMember(
                userId = user.id,
                projectId = projectId,
                role = MemberRole.MEMBER_ROLE_ADMIN,
            )
            val criterion = DataBuilder.createExampleProjectCriterion(projectId = projectId)

            coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns listOf(projectAdmin)
            coEvery { projectAccessChecker.isProjectActiveById() } returns failureAccessRule

            assertThrows<UnauthorizedTestException> { accessChecker.isAllowedToUpdateCriterion(user, criterion) }
        }

        @Test
        fun `When the user is a server admin, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val criterion = DataBuilder.createExampleUserCriterion(createdBy = user.id)

            assertDoesNotThrow { accessChecker.isAllowedToUpdateCriterion(user, criterion) }
        }

        @Test
        fun `When the user is not the creator, not an admin in the project, and not a server admin, then access is denied`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()
                val criterion = DataBuilder.createExampleProjectCriterion(projectId = projectId)

                coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns emptyList()

                assertThrows<UnauthorizedUpdateException> { accessChecker.isAllowedToUpdateCriterion(user, criterion) }
            }

        @Test
        fun `When the user is not the creator of a user criterion and not a server admin, then access is denied`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val criterion = DataBuilder.createExampleUserCriterion()

                assertThrows<UnauthorizedUpdateException> { accessChecker.isAllowedToUpdateCriterion(user, criterion) }
            }
    }
}
