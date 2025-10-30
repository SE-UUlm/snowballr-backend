package se.uulm.snowballr.backend.service.projectmember

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass
import java.util.UUID

class RemoveProjectMemberTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private fun getExampleRequest() = ProjectOuterClass.Project.Member.Remove.newBuilder()
        .setProjectId(projectId.toString())
        .setUserId(userId.toString())
        .build()

    @Test
    fun `When a server admin removes a project member, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        val userToRemove = DataBuilder.createExampleUser(
            id = userId,
            status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE,
        )
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectMember1 = DataBuilder.createExampleProjectMember(projectId = project.id, userId = userId)
        val projectMember2 = DataBuilder.createExampleProjectMember(projectId = project.id, userId = UUID.randomUUID())

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery { userRepoMock.doesUserExistById(userId) } returns true
        coEvery {
            projectMemberRepoMock.getProjectMembers(project.id)
        } returns listOf(projectMember1, projectMember2)
        coEvery { userRepoMock.getUserById(userId) } returns Result.success(userToRemove)
        coEvery { projectMemberRepoMock.removeProjectMember(project.id, userId) } returns Unit

        assertDoesNotThrow { mainService.removeProjectMember(getExampleRequest()) }
    }

    @Test
    fun `When a project admin removes another project member, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val userToRemove = DataBuilder.createExampleUser(
            id = userId,
            status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE,
        )
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectAdmin = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
        val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = userId)

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectAdmin)
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery { userRepoMock.doesUserExistById(userId) } returns true
        coEvery {
            projectMemberRepoMock.getProjectMembers(project.id)
        } returns listOf(projectMember, projectAdmin)
        coEvery { userRepoMock.getUserById(userId) } returns Result.success(userToRemove)
        coEvery { projectMemberRepoMock.removeProjectMember(project.id, userId) } returns Unit

        assertDoesNotThrow { mainService.removeProjectMember(getExampleRequest()) }
    }

    @Test
    fun `When a project admin removes themselves and they are not the last project admin, then no exception is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(id = userId)
            val otherUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(id = projectId)
            val projectAdmin1 = DataBuilder.createExampleProjectMember(projectId = project.id, userId = userId)
            val projectAdmin2 = DataBuilder.createExampleProjectMember(projectId = project.id, userId = otherUser.id)

            mockCurrentUser(currentUser)
            coEvery {
                projectMemberRepoMock.getProjectMembers(project.id)
            } returns listOf(projectAdmin1, projectAdmin2)
            coEvery {
                projectMemberRepoMock.getAllProjectAdmins(project.id)
            } returns listOf(projectAdmin1, projectAdmin2)
            coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
            coEvery { userRepoMock.doesUserExistById(currentUser.id) } returns true
            coEvery {
                projectMemberRepoMock.removeProjectMember(project.id, currentUser.id)
            } returns Unit

            assertDoesNotThrow { mainService.removeProjectMember(getExampleRequest()) }
        }

    @Test
    fun `When a non project admin removes themselves and they are not the last project member, then no exception is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(id = userId)
            val otherUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(id = projectId)
            val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = userId)
            val projectAdmin = DataBuilder.createExampleProjectMember(projectId = project.id, userId = otherUser.id)

            mockCurrentUser(currentUser)
            coEvery {
                projectMemberRepoMock.getProjectMembers(project.id)
            } returns listOf(projectMember, projectAdmin)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectAdmin)
            coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
            coEvery { userRepoMock.doesUserExistById(currentUser.id) } returns true
            coEvery {
                projectMemberRepoMock.removeProjectMember(project.id, currentUser.id)
            } returns Unit

            assertDoesNotThrow { mainService.removeProjectMember(getExampleRequest()) }
        }

    @Test
    fun `When a project admin removes a project member from a nonexisting project, then a NotFoundException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
            val projectAdmin = DataBuilder.createExampleProjectMember(projectId = projectId, userId = currentUser.id)

            mockCurrentUser(currentUser)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(projectId) } returns listOf(projectAdmin)
            coEvery { projectRepoMock.doesProjectExistById(projectId) } returns false

            assertThrows<SnowballRException.NotFoundException> {
                mainService.removeProjectMember(getExampleRequest())
            }
        }

    @Test
    fun `When a project admin removes a nonexisting project member, then a NotFoundException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectAdmin = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectAdmin)
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery { userRepoMock.doesUserExistById(userId) } returns false

        assertThrows<SnowballRException.NotFoundException> {
            mainService.removeProjectMember(getExampleRequest())
        }
    }

    @Test
    fun `When a normal project member removes another project member, then an UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject(id = projectId)

            mockCurrentUser(currentUser)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

            assertThrows<SnowballRException.UnauthorizedException> {
                mainService.removeProjectMember(getExampleRequest())
            }
        }

    @Test
    fun `When a project admin removes themselves, but is the last project admin in the project, then a FailedPreconditionException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(id = userId)
            val otherUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject(id = projectId)
            val projectAdmin = DataBuilder.createExampleProjectMember(projectId = project.id, userId = currentUser.id)
            val projectMember = DataBuilder.createExampleProjectMember(projectId = project.id, userId = otherUser.id)

            mockCurrentUser(currentUser)
            coEvery {
                projectMemberRepoMock.getProjectMembers(project.id)
            } returns listOf(projectAdmin, projectMember)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectAdmin)
            coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
            coEvery { userRepoMock.doesUserExistById(userId) } returns true

            assertThrows<SnowballRException.FailedPreconditionException> {
                mainService.removeProjectMember(getExampleRequest())
            }
        }

    @Test
    fun `When a project member to be deleted is the last member, then no exception is thrown and the project is marked as deleted`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(id = userId)
            val project = DataBuilder.createExampleProject(id = projectId)
            val lastProjectMember = DataBuilder.createExampleProjectMember(
                projectId = project.id,
                userId = currentUser.id,
            )

            mockCurrentUser(currentUser)
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(lastProjectMember)
            coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
            coEvery { userRepoMock.doesUserExistById(currentUser.id) } returns true
            coEvery { projectRepoMock.softDeleteProject(project.id) } returns Unit
            coEvery {
                projectMemberRepoMock.removeProjectMember(project.id, currentUser.id)
            } returns Unit

            assertDoesNotThrow { mainService.removeProjectMember(getExampleRequest()) }
            coVerify(exactly = 1) { projectRepoMock.softDeleteProject(project.id) }
        }
}
