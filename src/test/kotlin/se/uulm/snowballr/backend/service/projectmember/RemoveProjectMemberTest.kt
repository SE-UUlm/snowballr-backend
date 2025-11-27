package se.uulm.snowballr.backend.service.projectmember

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass
import java.util.UUID

class RemoveProjectMemberTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()
    private val userEmail = "user@example.com"

    private fun getExampleRequest() = ProjectOuterClass.Project.Member.Remove.newBuilder()
        .setProjectId(projectId.toString())
        .setUserEmail(userEmail)
        .build()

    @Test
    fun `When a server admin removes a project member, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        val userToRemove = DataBuilder.createExampleUser(
            email = userEmail,
            status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE,
        )
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectMember1 = DataBuilder.createExampleProjectMember(projectId = project.id, userId = userToRemove.id)
        val projectMember2 = DataBuilder.createExampleProjectMember(projectId = project.id, userId = UUID.randomUUID())

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.success(userToRemove)
        coEvery { projectMemberRepoMock.isProjectMember(projectId, userToRemove.id) } returns true
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery {
            projectMemberRepoMock.getProjectMembers(project.id)
        } returns listOf(projectMember1, projectMember2)
        coEvery { projectMemberRepoMock.removeProjectMember(project.id, userToRemove.id) } returns Unit

        assertDoesNotThrow { mainService.removeProjectMember(getExampleRequest()) }
    }

    @Test
    fun `When a project admin removes another project member, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val userToRemove = DataBuilder.createExampleUser(
            email = userEmail,
            status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE,
        )
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectAdmin = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = userToRemove.id)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.success(userToRemove)
        coEvery { projectMemberRepoMock.isProjectMember(projectId, userToRemove.id) } returns true
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectAdmin)
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery {
            projectMemberRepoMock.getProjectMembers(project.id)
        } returns listOf(projectMember, projectAdmin)
        coEvery { projectMemberRepoMock.removeProjectMember(project.id, userToRemove.id) } returns Unit

        assertDoesNotThrow { mainService.removeProjectMember(getExampleRequest()) }
    }

    @Test
    fun `When a project admin removes themselves and they are not the last project admin, then no exception is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(email = userEmail)
            val otherUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(id = projectId)
            val projectAdmin1 = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
            val projectAdmin2 = DataBuilder.createExampleProjectMember(projectId = project.id, userId = otherUser.id)

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserByEmail(currentUser.email) } returns Result.success(currentUser)
            coEvery { projectMemberRepoMock.isProjectMember(project.id, currentUser.id) } returns true
            coEvery {
                projectMemberRepoMock.getProjectMembers(project.id)
            } returns listOf(projectAdmin1, projectAdmin2)
            coEvery {
                projectMemberRepoMock.getAllProjectAdmins(project.id)
            } returns listOf(projectAdmin1, projectAdmin2)
            coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
            coEvery {
                projectMemberRepoMock.removeProjectMember(project.id, currentUser.id)
            } returns Unit

            assertDoesNotThrow { mainService.removeProjectMember(getExampleRequest()) }
        }

    @Test
    fun `When a non project admin removes themselves and they are not the last project member, then no exception is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(email = userEmail)
            val otherUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(id = projectId)
            val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
            val projectAdmin = DataBuilder.createExampleProjectMember(projectId = project.id, userId = otherUser.id)

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.success(currentUser)
            coEvery { projectMemberRepoMock.isProjectMember(projectId, currentUser.id) } returns true
            coEvery {
                projectMemberRepoMock.getProjectMembers(project.id)
            } returns listOf(projectMember, projectAdmin)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectAdmin)
            coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
            coEvery {
                projectMemberRepoMock.removeProjectMember(project.id, currentUser.id)
            } returns Unit

            assertDoesNotThrow { mainService.removeProjectMember(getExampleRequest()) }
        }

    @Test
    fun `When a project admin removes a project member from a non-existent project, then a NotFoundException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.success(currentUser)
            coEvery { projectMemberRepoMock.isProjectMember(projectId, currentUser.id) } returns true
            coEvery { projectRepoMock.doesProjectExistById(projectId) } returns false

            assertThrows<NotFoundException> {
                mainService.removeProjectMember(getExampleRequest())
            }
        }

    @Test
    fun `When a project admin removes a non-existent project member, then a NotFoundException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val projectAdmin = DataBuilder.createExampleProjectMember(projectId = projectId, userId = currentUser.id)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.failure(TestSpecificException())
        coEvery { projectMemberRepoMock.getAllProjectAdmins(projectId) } returns listOf(projectAdmin)
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(userEmail, projectId)
        } returns Result.failure(TestSpecificException())

        assertThrows<NotFoundException> {
            mainService.removeProjectMember(getExampleRequest())
        }
    }

    @Test
    fun `When a normal project member removes another project member, then an UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
            val otherUser = DataBuilder.createExampleUser(
                email = userEmail,
                status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE,
            )
            val project = DataBuilder.createExampleProject(id = projectId)

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserByEmail(otherUser.email) } returns Result.success(otherUser)
            coEvery { projectMemberRepoMock.isProjectMember(projectId, otherUser.id) } returns true
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

            assertThrows<UnauthorizedException> {
                mainService.removeProjectMember(getExampleRequest())
            }
        }

    @Test
    fun `When a project admin removes themselves, but is the last project admin in the project, then a FailedPreconditionException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(email = userEmail)
            val otherUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(id = projectId)
            val projectAdmin = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
            val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = otherUser.id)

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.success(currentUser)
            coEvery { projectMemberRepoMock.isProjectMember(projectId, currentUser.id) } returns true
            coEvery {
                projectMemberRepoMock.getProjectMembers(project.id)
            } returns listOf(projectAdmin, projectMember)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectAdmin)
            coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true

            assertThrows<FailedPreconditionException> {
                mainService.removeProjectMember(getExampleRequest())
            }
        }

    @Test
    fun `When a project member to be deleted is the last member, then no exception is thrown and the project is marked as deleted`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(email = userEmail)
            val project = DataBuilder.createExampleProject(id = projectId)
            val lastProjectMember = DataBuilder.createExampleProjectMember(
                projectId = project.id,
                userId = currentUser.id,
            )

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.success(currentUser)
            coEvery { projectMemberRepoMock.isProjectMember(projectId, currentUser.id) } returns true
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(lastProjectMember)
            coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
            coEvery { projectRepoMock.softDeleteProject(project.id) } returns Unit
            coEvery {
                projectMemberRepoMock.removeProjectMember(project.id, currentUser.id)
            } returns Unit

            assertDoesNotThrow { mainService.removeProjectMember(getExampleRequest()) }
            coVerify(exactly = 1) { projectRepoMock.softDeleteProject(project.id) }
        }

    @Test
    fun `When a project admin removes an invitee, then the invitee is removed`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val projectAdmin = DataBuilder.createExampleProjectMember(projectId = projectId, userId = currentUser.id)
        val token = DataBuilder.createExampleInvitationToken(
            email = userEmail,
            projectId = projectId,
        )

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.failure(TestSpecificException())
        coEvery { projectMemberRepoMock.getAllProjectAdmins(projectId) } returns listOf(projectAdmin)
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(userEmail, projectId)
        } returns Result.success(token)
        coEvery { invitationTokenRepoMock.deleteInvitationToken(token.token) } returns Unit

        assertDoesNotThrow { mainService.removeProjectMember(getExampleRequest()) }
    }

    @Test
    fun `When a server admin removes an invitee, then the invitee is removed`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        val token = DataBuilder.createExampleInvitationToken(
            email = userEmail,
            projectId = projectId,
        )

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.failure(TestSpecificException())
        coEvery { projectMemberRepoMock.getAllProjectAdmins(projectId) } returns emptyList()
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(userEmail, projectId)
        } returns Result.success(token)
        coEvery { invitationTokenRepoMock.deleteInvitationToken(token.token) } returns Unit

        assertDoesNotThrow { mainService.removeProjectMember(getExampleRequest()) }
    }

    @Test
    fun `When a normal project member removes an invitee, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.failure(TestSpecificException())
        coEvery { projectMemberRepoMock.getAllProjectAdmins(projectId) } returns emptyList()

        assertThrows<UnauthorizedException> {
            mainService.removeProjectMember(getExampleRequest())
        }
    }

    @Test
    fun `When retrieving the invitation token fails, then a NotFoundException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val projectAdmin = DataBuilder.createExampleProjectMember(projectId = projectId, userId = currentUser.id)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.failure(TestSpecificException())
        coEvery { projectMemberRepoMock.getAllProjectAdmins(projectId) } returns listOf(projectAdmin)
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(userEmail, projectId)
        } returns Result.failure(TestSpecificException())

        assertThrows<NotFoundException> {
            mainService.removeProjectMember(getExampleRequest())
        }
    }

    @Test
    fun `When the user to be removed is not a project member and has no invitation, then nothing is removed`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
            val otherUser = DataBuilder.createExampleUser(email = userEmail)

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserByEmail(otherUser.email) } returns Result.success(otherUser)
            coEvery { projectMemberRepoMock.isProjectMember(projectId, otherUser.id) } returns false

            assertDoesNotThrow { mainService.removeProjectMember(getExampleRequest()) }
        }
}
