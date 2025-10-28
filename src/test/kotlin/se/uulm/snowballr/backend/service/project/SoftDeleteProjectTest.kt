package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import kotlin.reflect.KFunction

class SoftDeleteProjectTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()
    private val validSoftDeleteProjectRequest = Base.Id.newBuilder()
        .setId(projectId.toString())

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
    }

    @Test
    fun `When a server admin deletes a project, then no exception is thrown`() = runTest {
        mockSoftDeleteProject(true, projectId)

        assertDoesNotThrow { mainService.softDeleteProject(validSoftDeleteProjectRequest.build()) }
    }

    @Test
    fun `When a project admin deletes a project, then no exception is thrown`() = runTest {
        mockSoftDeleteProject(false, projectId)

        assertDoesNotThrow { mainService.softDeleteProject(validSoftDeleteProjectRequest.build()) }
    }

    @Test
    fun `When a normal project member deletes a project, then an UnauthorizedException is thrown`() = runTest {
        mockSoftDeleteProject(false, projectId, projectMemberRepoMock::getAllProjectAdmins)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(projectId) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.softDeleteProject(validSoftDeleteProjectRequest.build()) }
        coVerify(exactly = 0) { projectRepoMock.softDeleteProject(any()) }
    }

    @Test
    fun `When the project to delete does not exist, then a NotFoundException is thrown`() = runTest {
        val nonExistentProjectId = UUID.randomUUID()

        mockSoftDeleteProject(false, nonExistentProjectId, projectRepoMock::doesProjectExistById)
        coEvery { projectRepoMock.doesProjectExistById(nonExistentProjectId) } returns false

        val invalidSoftDeleteProjectRequest = validSoftDeleteProjectRequest
            .setId(nonExistentProjectId.toString())

        assertThrows<NotFoundException> { mainService.softDeleteProject(invalidSoftDeleteProjectRequest.build()) }
        coVerify(exactly = 0) { projectRepoMock.softDeleteProject(any()) }
    }
}
