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
import java.util.UUID

class ProjectMemberAccessCheckerTest {
    private val projectAccessChecker = mockk<IProjectAccessChecker>()

    private val accessChecker = ProjectMemberAccessChecker(projectAccessChecker)

    private val successAccessRule = AccessRule<UUID> { _, _ -> true }
    private val failureAccessRule = AccessRule<UUID> { _, _ -> throw UnauthorizedTestException() }

    @Nested
    inner class IsAllowedToUpdateMemberRole {
        @Test
        fun `When isProjectOrServerAdmin and isProjectExistent both allow access, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()

            every { projectAccessChecker.isProjectOrServerAdmin(AccessType.UPDATE) } returns successAccessRule
            every { projectAccessChecker.isProjectExistent() } returns successAccessRule

            assertDoesNotThrow { accessChecker.isAllowedToUpdateMemberRole(user, projectId) }
        }

        @Test
        fun `When isProjectOrServerAdmin denies access, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()

            every { projectAccessChecker.isProjectOrServerAdmin(AccessType.UPDATE) } returns failureAccessRule
            every { projectAccessChecker.isProjectExistent() } returns successAccessRule

            assertThrows<UnauthorizedTestException> { accessChecker.isAllowedToUpdateMemberRole(user, projectId) }
        }

        @Test
        fun `When isProjectOrServerAdmin allows but isProjectExistent denies access, then access is denied`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()

                every { projectAccessChecker.isProjectOrServerAdmin(AccessType.UPDATE) } returns successAccessRule
                every { projectAccessChecker.isProjectExistent() } returns failureAccessRule

                assertThrows<UnauthorizedTestException> { accessChecker.isAllowedToUpdateMemberRole(user, projectId) }
            }
    }

    @Nested
    inner class IsAllowedToRemoveMember {
        @Test
        fun `When the user is the same as the member being removed, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()

            every { projectAccessChecker.isProjectOrServerAdmin(AccessType.DELETE) } returns successAccessRule

            assertDoesNotThrow { accessChecker.isAllowedToRemoveMember(user, user.id, projectId) }
        }

        @Test
        fun `When the user is a project or server admin but not the same member, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val memberUserId = UUID.randomUUID()
            val projectId = UUID.randomUUID()

            every { projectAccessChecker.isProjectOrServerAdmin(AccessType.DELETE) } returns successAccessRule

            assertDoesNotThrow { accessChecker.isAllowedToRemoveMember(user, memberUserId, projectId) }
        }

        @Test
        fun `When the user is neither the same member nor a project or server admin, then access is denied`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val memberUserId = UUID.randomUUID()
                val projectId = UUID.randomUUID()

                every { projectAccessChecker.isProjectOrServerAdmin(AccessType.DELETE) } returns failureAccessRule

                assertThrows<UnauthorizedTestException> {
                    accessChecker.isAllowedToRemoveMember(user, memberUserId, projectId)
                }
            }
    }

    @Nested
    inner class IsAllowedToRemoveInvitation {
        @Test
        fun `When the user is a project or server admin, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()

            every { projectAccessChecker.isProjectOrServerAdmin(AccessType.DELETE) } returns successAccessRule

            assertDoesNotThrow { accessChecker.isAllowedToRemoveInvitation(user, projectId) }
        }

        @Test
        fun `When the user is not a project or server admin, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()

            every { projectAccessChecker.isProjectOrServerAdmin(AccessType.DELETE) } returns failureAccessRule

            assertThrows<UnauthorizedTestException> { accessChecker.isAllowedToRemoveInvitation(user, projectId) }
        }
    }
}
