package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetCriterionByIdTest : MainServiceTest() {
    @Test
    fun `When the requesting user is a server admin, then a project criterion can be retrieved`() = runTest {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()
        val criterion = DataBuilder.createExampleProjectCriterion(projectId = project.id, createdBy = adminUser.id)

        mockCurrentUser(adminUser)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)

        assertDoesNotThrow { mainService.getCriterionById(criterion.id) }
    }

    @Test
    fun `When the requesting user is a project member and wants to retrieve a project criterion, then the criterion can be retrieved`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject()
            val criterion = DataBuilder.createExampleProjectCriterion(projectId = project.id, createdBy = user.id)
            val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

            mockCurrentUser(user)
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
            coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)

            assertDoesNotThrow { mainService.getCriterionById(criterion.id) }
        }

    @Test
    fun `When the requesting user is not a member of the project and wants to retrieve a project criterion, then an UnauthorizedException is thrown`() =
        runTest {
            val noAccessUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val criterion = DataBuilder.createExampleProjectCriterion(
                projectId = project.id,
                createdBy = noAccessUser.id,
            )

            mockCurrentUser(noAccessUser)
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
            coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)

            assertThrows<UnauthorizedException> { mainService.getCriterionById(criterion.id) }
        }

    @Test
    fun `When the requesting user is a server admin, then a user criterion can be retrieved`() = runTest {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val criterion = DataBuilder.createExampleUserCriterion(createdBy = UUID.randomUUID())

        mockCurrentUser(adminUser)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)

        assertDoesNotThrow { mainService.getCriterionById(criterion.id) }
    }

    @Test
    fun `When the requesting user is not a server admin and wants to retrieve a user criterion, which he created himself, then the criterion can be retrieved`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val criterion = DataBuilder.createExampleUserCriterion(createdBy = user.id)

            mockCurrentUser(user)
            coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)

            assertDoesNotThrow { mainService.getCriterionById(criterion.id) }
        }

    @Test
    fun `When the requesting user is not a server admin and wants to retrieve a user criterion, which he did not create himself, then an UnauthorizedException is thrown`() =
        runTest {
            val noAccessUser = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleUserCriterion(createdBy = UUID.randomUUID())

            mockCurrentUser(noAccessUser)
            coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.success(criterion)

            assertThrows<UnauthorizedException> { mainService.getCriterionById(criterion.id) }
        }

    @Test
    fun `When an error occurs while the criterion is retrieved, then a TestSpecificException is thrown`() = runTest {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val criterion = DataBuilder.createExampleUserCriterion()

        mockCurrentUser(adminUser)
        coEvery { criterionRepoMock.getCriterionById(criterion.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getCriterionById(criterion.id) }
    }
}
