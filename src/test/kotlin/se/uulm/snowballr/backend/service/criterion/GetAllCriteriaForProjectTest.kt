package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetAllCriteriaForProjectTest : MainServiceTest() {
    private val requestId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(requestId.toString())
        .build()

    @Test
    fun `When an error occurs while user context is retrieved, then an exception is thrown`() = runTest {
        val request = getExampleRequest()

        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllCriteriaForProject(request) }
    }

    @Test
    fun `When an error occurs while user is retrieved, then an exception is thrown`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns adminUser.id
        coEvery { userRepoMock.getUserById(adminUser.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllCriteriaForProject(request) }
    }

    @Test
    fun `When an error occurs while project is retrieved, then an exception is thrown`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns adminUser.id
        coEvery { userRepoMock.getUserById(adminUser.id) } returns adminUser
        coEvery { projectRepoMock.getProjectById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllCriteriaForProject(request) }
    }

    @Test
    fun `When an error occurs while project members are retrieved, then an exception is thrown`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(id = requestId)

        every { GrpcContext.getUserIdFromContext() } returns adminUser.id
        coEvery { userRepoMock.getUserById(adminUser.id) } returns adminUser
        coEvery { projectRepoMock.getProjectById(any()) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllCriteriaForProject(request) }
    }

    @Test
    fun `When an error occurs while the criteria are retrieved, then an exception is thrown`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(id = requestId)
        val projectMember = DataBuilder.createExampleProjectMember(userId = adminUser.id, projectId = project.id)

        every { GrpcContext.getUserIdFromContext() } returns adminUser.id
        coEvery { userRepoMock.getUserById(adminUser.id) } returns adminUser
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { criterionRepoMock.getAllProjectCriteria(project.id) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllCriteriaForProject(request) }
    }

    @Test
    fun `When the requesting user is a server admin, then no exception is thrown`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val criterion = DataBuilder.createExampleProjectCriterion(
            projectId = requestId,
            createdBy = adminUser.id,
        )
        val project = DataBuilder.createExampleProject(id = requestId)

        every { GrpcContext.getUserIdFromContext() } returns adminUser.id
        coEvery { userRepoMock.getUserById(adminUser.id) } returns adminUser
        coEvery { projectRepoMock.getProjectById(any()) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(any()) } returns emptyList()
        coEvery { criterionRepoMock.getAllProjectCriteria(requestId) } returns listOf(criterion)

        assertDoesNotThrow { mainService.getAllCriteriaForProject(request) }
    }

    @Test
    fun `When the requesting user is a project member and wants to retrieve all project criteria, then no exception is thrown`() =
        runTest {
            val request = getExampleRequest()

            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val criterion = DataBuilder.createExampleProjectCriterion(
                projectId = requestId,
                createdBy = user.id,
            )
            val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = requestId)
            val project = DataBuilder.createExampleProject(id = requestId)

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { projectRepoMock.getProjectById(requestId) } returns project
            coEvery { projectMemberRepoMock.getProjectMembers(requestId) } returns listOf(projectMember)
            coEvery { criterionRepoMock.getAllProjectCriteria(requestId) } returns listOf(criterion)

            assertDoesNotThrow { mainService.getAllCriteriaForProject(request) }
        }

    @Test
    fun `When the requesting user is a non project member and wants to retrieve all project criteria, then an unauthorized exception is thrown`() =
        runTest {
            val request = getExampleRequest()

            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject(id = requestId)

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { projectRepoMock.getProjectById(requestId) } returns project
            coEvery { projectMemberRepoMock.getProjectMembers(requestId) } returns emptyList()

            assertThrows<UnauthorizedException> { mainService.getAllCriteriaForProject(request) }
        }
}
