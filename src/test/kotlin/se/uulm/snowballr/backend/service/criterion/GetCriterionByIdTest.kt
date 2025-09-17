package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetCriterionByIdTest : MainServiceTest() {
    private val criterionId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(criterionId.toString())
        .build()

    @Test
    fun `When the requesting user is a server admin, then a project criterion can be retrieved`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()
        val criterion = DataBuilder.createExampleProjectCriterion(
            id = criterionId,
            projectId = project.id,
            createdBy = adminUser.id,
        )

        mockCurrentUser(adminUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.success(criterion)

        assertDoesNotThrow { mainService.getCriterionById(request) }
    }

    @Test
    fun `When the requesting user is a project member and wants to retrieve a project criterion, then the criterion can be retrieved`() =
        runTest {
            val request = getExampleRequest()

            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject()
            val criterion = DataBuilder.createExampleProjectCriterion(
                id = criterionId,
                projectId = project.id,
                createdBy = user.id,
            )
            val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

            mockCurrentUser(user)
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.success(criterion)

            assertDoesNotThrow { mainService.getCriterionById(request) }
        }

    @Test
    fun `When the requesting user is not a member of the project and wants to retrieve a project criterion, then an UnauthorizedException is thrown`() =
        runTest {
            val request = getExampleRequest()

            val noAccessUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val criterion = DataBuilder.createExampleProjectCriterion(
                id = criterionId,
                projectId = project.id,
                createdBy = noAccessUser.id,
            )

            mockCurrentUser(noAccessUser)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
            coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.success(criterion)

            assertThrows<UnauthorizedException> { mainService.getCriterionById(request) }
        }

    @Test
    fun `When the requesting user is a server admin, then a user criterion can be retrieved`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val criterion = DataBuilder.createExampleUserCriterion(id = criterionId, createdBy = UUID.randomUUID())

        mockCurrentUser(adminUser)
        coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.success(criterion)

        assertDoesNotThrow { mainService.getCriterionById(request) }
    }

    @Test
    fun `When the requesting user is not a server admin and wants to retrieve a user criterion, which he created himself, then the criterion can be retrieved`() =
        runTest {
            val request = getExampleRequest()

            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val criterion = DataBuilder.createExampleUserCriterion(id = criterionId, createdBy = user.id)

            mockCurrentUser(user)
            coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.success(criterion)

            assertDoesNotThrow { mainService.getCriterionById(request) }
        }

    @Test
    fun `When the requesting user is not a server admin and wants to retrieve a user criterion, which he did not create himself, then an UnauthorizedException is thrown`() =
        runTest {
            val request = getExampleRequest()

            val noAccessUser = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleUserCriterion(id = criterionId, createdBy = UUID.randomUUID())

            mockCurrentUser(noAccessUser)
            coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.success(criterion)

            assertThrows<UnauthorizedException> { mainService.getCriterionById(request) }
        }

    @Test
    fun `When an error occurs while the criterion is retrieved, then a TestSpecificException is thrown`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        mockCurrentUser(adminUser)
        coEvery { criterionRepoMock.getCriterionById(criterionId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getCriterionById(request) }
    }
}
