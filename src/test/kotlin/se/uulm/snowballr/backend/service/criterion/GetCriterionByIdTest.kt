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

class GetCriterionByIdTest : MainServiceTest() {
    private val requestId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(requestId.toString())
        .build()

    @Test
    fun `When the requesting user is a server admin, then a project criterion can be retrieved`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()
        val criterion = DataBuilder.createExampleProjectCriterion(
            id = requestId,
            projectId = project.id,
            createdBy = adminUser.id,
        )

        every { GrpcContext.getUserIdFromContext() } returns adminUser.id
        coEvery { userRepoMock.getUserById(adminUser.id) } returns adminUser
        coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertDoesNotThrow { mainService.getCriterionById(request) }
    }

    @Test
    fun `When the requesting user is a project member and wants to retrieve a project criterion, then the criterion can be retrieved`() =
        runTest {
            val request = getExampleRequest()

            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject()
            val criterion = DataBuilder.createExampleProjectCriterion(
                id = requestId,
                projectId = project.id,
                createdBy = user.id,
            )
            val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion
            coEvery { projectRepoMock.getProjectById(project.id) } returns project
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)

            assertDoesNotThrow { mainService.getCriterionById(request) }
        }

    @Test
    fun `When the requesting user is not a member of the project and wants to retrieve a project criterion, then an unauthorized exception is thrown`() =
        runTest {
            val request = getExampleRequest()

            val noAccessUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val criterion = DataBuilder.createExampleProjectCriterion(
                id = requestId,
                projectId = project.id,
                createdBy = noAccessUser.id,
            )

            every { GrpcContext.getUserIdFromContext() } returns noAccessUser.id
            coEvery { userRepoMock.getUserById(noAccessUser.id) } returns noAccessUser
            coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion
            coEvery { projectRepoMock.getProjectById(project.id) } returns project
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

            assertThrows<UnauthorizedException.Single> { mainService.getCriterionById(request) }
        }

    @Test
    fun `When the requesting user is a server admin, then a user criterion can be retrieved`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val criterion = DataBuilder.createExampleUserCriterion(id = requestId, createdBy = UUID.randomUUID())

        every { GrpcContext.getUserIdFromContext() } returns adminUser.id
        coEvery { userRepoMock.getUserById(adminUser.id) } returns adminUser
        coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion

        assertDoesNotThrow { mainService.getCriterionById(request) }
    }

    @Test
    fun `When the requesting user is not a server admin and wants to retrieve a user criterion, which he created himself, then the criterion can be retrieved`() =
        runTest {
            val request = getExampleRequest()

            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val criterion = DataBuilder.createExampleUserCriterion(id = requestId, createdBy = user.id)

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns user
            coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion

            assertDoesNotThrow { mainService.getCriterionById(request) }
        }

    @Test
    fun `When the requesting user is not a server admin and wants to retrieve a user criterion, which he did not create himself, then an unauthorized exception is thrown`() =
        runTest {
            val request = getExampleRequest()

            val noAccessUser = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleUserCriterion(id = requestId, createdBy = UUID.randomUUID())

            every { GrpcContext.getUserIdFromContext() } returns noAccessUser.id
            coEvery { userRepoMock.getUserById(noAccessUser.id) } returns noAccessUser
            coEvery { criterionRepoMock.getCriterionById(requestId) } returns criterion

            assertThrows<UnauthorizedException.Single> { mainService.getCriterionById(request) }
        }

    @Test
    fun `When an error occurs while the criterion is retrieved, then an exception is thrown`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns adminUser.id
        coEvery { userRepoMock.getUserById(adminUser.id) } returns adminUser
        coEvery { criterionRepoMock.getCriterionById(requestId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getCriterionById(request) }
    }
}
