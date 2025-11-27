package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetAllCriteriaForProjectTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(projectId.toString())
        .build()

    @Test
    fun `When the project doesn't exist, then a NotFoundException is thrown`() = runTest {
        val request = getExampleRequest()

        val project = DataBuilder.createExampleProject(id = projectId)
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        mockCurrentUser(adminUser)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { projectRepoMock.doesProjectExistById(projectId) } returns false

        assertThrows<NotFoundException> { mainService.getAllCriteriaForProject(request) }
    }

    @Test
    fun `When the requesting user is a server admin, then no exception is thrown`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val criterion = DataBuilder.createExampleProjectCriterion(
            projectId = projectId,
            createdBy = adminUser.id,
        )
        val project = DataBuilder.createExampleProject(id = projectId)

        mockCurrentUser(adminUser)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery { criterionRepoMock.getAllProjectCriteria(project.id) } returns listOf(criterion)

        assertDoesNotThrow { mainService.getAllCriteriaForProject(request) }
    }

    @Test
    fun `When the requesting user is a project member and wants to retrieve all project criteria, then no exception is thrown`() =
        runTest {
            val request = getExampleRequest()

            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val criterion = DataBuilder.createExampleProjectCriterion(
                projectId = projectId,
                createdBy = user.id,
            )
            val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = projectId)
            val project = DataBuilder.createExampleProject(id = projectId)

            mockCurrentUser(user)
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
            coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
            coEvery { criterionRepoMock.getAllProjectCriteria(project.id) } returns listOf(criterion)

            assertDoesNotThrow { mainService.getAllCriteriaForProject(request) }
        }

    @Test
    fun `When the requesting user is a non project member and wants to retrieve all project criteria, then an UnauthorizedException is thrown`() =
        runTest {
            val request = getExampleRequest()

            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject(id = projectId)

            mockCurrentUser(user)
            coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

            assertThrows<UnauthorizedException> { mainService.getAllCriteriaForProject(request) }
        }
}
