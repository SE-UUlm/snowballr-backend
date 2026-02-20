package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserRole

class GetAllCriteriaForProjectTest : MainServiceTest() {
    @Test
    fun `When the project doesn't exist, then a ProjectNotFoundException is thrown`() = runTest {
        val project = DataBuilder.createExampleProject()
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        mockCurrentUser(adminUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.failure(TestSpecificException())

        assertThrows<ProjectNotFoundException> { mainService.getAllCriteriaForProject(project.id) }
    }

    @Test
    fun `When the requesting user is a server admin, then no exception is thrown`() = runTest {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()
        val criterion = DataBuilder.createExampleProjectCriterion(projectId = project.id, createdBy = adminUser.id)

        mockCurrentUser(adminUser)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, adminUser.id) } returns false
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { criterionRepoMock.getAllProjectCriteria(project.id) } returns listOf(criterion)

        assertDoesNotThrow { mainService.getAllCriteriaForProject(project.id) }
    }

    @Test
    fun `When the requesting user is a project member and wants to retrieve all project criteria, then no exception is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject()
            val criterion = DataBuilder.createExampleProjectCriterion(projectId = project.id, createdBy = user.id)

            mockCurrentUser(user)
            coEvery { projectMemberRepoMock.isProjectMember(project.id, user.id) } returns true
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { criterionRepoMock.getAllProjectCriteria(project.id) } returns listOf(criterion)

            assertDoesNotThrow { mainService.getAllCriteriaForProject(project.id) }
        }

    @Test
    fun `When the requesting user is a non project member and wants to retrieve all project criteria, then an UnauthorizedException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(user)
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { projectMemberRepoMock.isProjectMember(project.id, user.id) } returns false

            assertThrows<UnauthorizedException> { mainService.getAllCriteriaForProject(project.id) }
        }
}
