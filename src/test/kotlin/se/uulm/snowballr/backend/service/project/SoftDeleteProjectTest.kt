package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import kotlin.reflect.KFunction

class SoftDeleteProjectTest : MainServiceTest() {
    private fun mockSoftDeleteProject(useAdminUser: Boolean, projectId: UUID, stopBefore: KFunction<*>? = null) {
        val currentUser = DataBuilder.createExampleUser(
            role = if (useAdminUser) {
                UserRole.USER_ROLE_ADMIN
            } else {
                UserRole.USER_ROLE_DEFAULT
            },
        )
        val projectAdmin = DataBuilder.createExampleProjectMember(
            projectId = projectId,
            userId = currentUser.id,
            role = ProjectOuterClass.MemberRole.MEMBER_ROLE_ADMIN,
        )

        mockCurrentUser(currentUser)

        if (stopBefore == projectMemberRepoMock::getAllProjectAdmins) {
            return
        }
        coEvery { projectMemberRepoMock.getAllProjectAdmins(projectId) } returns listOf(projectAdmin)

        if (stopBefore == projectRepoMock::doesProjectExistById) {
            return
        }
        coEvery { projectRepoMock.doesProjectExistById(projectId) } returns true

        coEvery { projectRepoMock.softDeleteProject(projectId) } returns Unit
        coEvery { invitationTokenRepoMock.deleteInvitationTokensForProject(projectId) } returns Unit
    }

    @Test
    fun `When a server admin deletes a project, then no exception is thrown`() = runTest {
        val projectId = UUID.randomUUID()

        mockSoftDeleteProject(true, projectId)

        assertDoesNotThrow { mainService.softDeleteProject(projectId) }
    }

    @Test
    fun `When a project admin deletes a project, then no exception is thrown`() = runTest {
        val projectId = UUID.randomUUID()

        mockSoftDeleteProject(false, projectId)

        assertDoesNotThrow { mainService.softDeleteProject(projectId) }
    }

    @Test
    fun `When a normal project member deletes a project, then an UnauthorizedException is thrown`() = runTest {
        val projectId = UUID.randomUUID()

        mockSoftDeleteProject(false, projectId, stopBefore = projectMemberRepoMock::getAllProjectAdmins)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(projectId) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.softDeleteProject(projectId) }
        coVerify(exactly = 0) { projectRepoMock.softDeleteProject(any()) }
    }

    @Test
    fun `When the project to delete does not exist, then a NotFoundException is thrown`() = runTest {
        val nonExistentProjectId = UUID.randomUUID()

        mockSoftDeleteProject(false, nonExistentProjectId, stopBefore = projectRepoMock::doesProjectExistById)
        coEvery { projectRepoMock.doesProjectExistById(nonExistentProjectId) } returns false

        assertThrows<NotFoundException> { mainService.softDeleteProject(nonExistentProjectId) }
        coVerify(exactly = 0) { projectRepoMock.softDeleteProject(any()) }
    }
}
