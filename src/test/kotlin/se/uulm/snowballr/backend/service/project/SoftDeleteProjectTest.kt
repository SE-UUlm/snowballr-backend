package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import java.util.UUID

class SoftDeleteProjectTest : ProjectServiceTest() {
    @Test
    fun `When a user deletes a project, but has no access, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val projectId = UUID.randomUUID()

        mockCurrentUser(user)
        coEvery {
            projectAccessCheckerMock.isProjectOrServerAdmin(user, projectId, AccessType.DELETE)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.softDeleteProject(projectId) }

        coVerify(exactly = 0) { projectRepoMock.softDeleteProject(projectId) }
        coVerify(exactly = 0) { invitationTokenRepoMock.deleteInvitationTokensForProject(projectId) }
    }

    @Test
    fun `When a user deletes a non-existent project, then a ProjectNotFoundException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val projectId = UUID.randomUUID()

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, projectId, AccessType.DELETE) }
        coEvery { projectRepoMock.doesProjectExistById(projectId) } returns false

        assertThrows<ProjectNotFoundException> { service.softDeleteProject(projectId) }

        coVerify(exactly = 0) { projectRepoMock.softDeleteProject(projectId) }
        coVerify(exactly = 0) { invitationTokenRepoMock.deleteInvitationTokensForProject(projectId) }
    }

    @Test
    fun `When a user deletes a project and has access, then the project and any invitation tokens for it are deleted successfully`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()

            mockCurrentUser(user)
            coJustRun { projectAccessCheckerMock.isProjectOrServerAdmin(user, projectId, AccessType.DELETE) }
            coEvery { projectRepoMock.doesProjectExistById(projectId) } returns true
            coJustRun { projectRepoMock.softDeleteProject(projectId) }
            coJustRun { invitationTokenRepoMock.deleteInvitationTokensForProject(projectId) }

            service.softDeleteProject(projectId)

            coVerify(exactly = 1) { projectRepoMock.softDeleteProject(projectId) }
            coVerify(exactly = 1) { invitationTokenRepoMock.deleteInvitationTokensForProject(projectId) }
        }
}
