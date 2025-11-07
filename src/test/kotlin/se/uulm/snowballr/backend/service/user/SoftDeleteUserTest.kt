package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class SoftDeleteUserTest : MainServiceTest() {
    private val requestedUserId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestedUserId.toString()).build()

    @Test
    fun `When retrieving user to delete fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When user is not admin and tries to delete another user, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val userToDelete = DataBuilder.createExampleUser(id = requestedUserId)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.success(userToDelete)

        assertThrows<UnauthorizedException> { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When trying to delete another admin user, then a FailedPreconditionException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val userToDelete = DataBuilder.createExampleUser(id = requestedUserId, role = UserRole.USER_ROLE_ADMIN)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.success(userToDelete)

        assertThrows<FailedPreconditionException> { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When admin soft-deletes other user, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val userToDelete = DataBuilder.createExampleUser(id = requestedUserId)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.success(userToDelete)
        coEvery { projectRepoMock.getUserProjects(requestedUserId, any()) } returns emptyList()
        coEvery { userRepoMock.softDeleteUser(requestedUserId) } returns Unit

        assertDoesNotThrow { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When user tries to delete own account, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser(id = requestedUserId, role = UserRole.USER_ROLE_DEFAULT)

        mockCurrentUser(currentUser)
        coEvery { projectRepoMock.getUserProjects(requestedUserId, any()) } returns emptyList()
        coEvery { userRepoMock.softDeleteUser(currentUser.id) } returns Unit

        assertDoesNotThrow { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When admin tries to delete own account, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser(id = requestedUserId, role = UserRole.USER_ROLE_ADMIN)

        mockCurrentUser(currentUser)
        coEvery { projectRepoMock.getUserProjects(requestedUserId, any()) } returns emptyList()
        coEvery { userRepoMock.softDeleteUser(currentUser.id) } returns Unit

        assertDoesNotThrow { mainService.softDeleteUser(getExampleRequest()) }
    }

    @Test
    fun `When trying to delete a user that still is the last project admin in any project, then a FailedPrecondition is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(id = requestedUserId, role = UserRole.USER_ROLE_ADMIN)
            val userToDelete = DataBuilder.createExampleUser(id = requestedUserId)
            val project = DataBuilder.createExampleProject()
            val projectAdmin = DataBuilder.createExampleProjectMember(projectId = project.id, userId = requestedUserId)

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.success(userToDelete)
            coEvery { projectRepoMock.getUserProjects(requestedUserId, any()) } returns listOf(project)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectAdmin)

            assertThrows<FailedPreconditionException> { mainService.softDeleteUser(getExampleRequest()) }
        }

    @Test
    fun `When trying to delete a user that is not the last project admin in a project, then no exception is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(id = requestedUserId, role = UserRole.USER_ROLE_ADMIN)
            val userToDelete = DataBuilder.createExampleUser(id = requestedUserId)
            val project = DataBuilder.createExampleProject()
            val projectAdmin = DataBuilder.createExampleProjectMember(
                projectId = project.id,
                userId = UUID.randomUUID(),
            )
            val secondProjectAdmin = DataBuilder.createExampleProjectMember(
                projectId = project.id,
                userId = currentUser.id,
            )

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.success(userToDelete)
            coEvery { projectRepoMock.getUserProjects(requestedUserId, any()) } returns listOf(project)
            coEvery {
                projectMemberRepoMock.getAllProjectAdmins(project.id)
            } returns listOf(projectAdmin, secondProjectAdmin)
            coEvery { userRepoMock.softDeleteUser(currentUser.id) } returns Unit

            assertDoesNotThrow { mainService.softDeleteUser(getExampleRequest()) }
        }
}
