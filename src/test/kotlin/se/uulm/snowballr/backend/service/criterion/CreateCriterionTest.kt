package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.utils.GrpcEnumSourceTest
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.UserOuterClass.UserRole
import snowballr.CriterionOuterClass.Criterion as GrpcCriterion

class CreateCriterionTest : MainServiceTest() {
    private fun getProjectCriterionRequest(projectId: String): GrpcCriterion.Create {
        return GrpcCriterion.Create.newBuilder()
            .setTag("Tag")
            .setName("Criterion")
            .setDescription("Description")
            .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
            .setProjectId(projectId)
            .build()
    }

    private fun getUserCriterionRequest(): GrpcCriterion.Create {
        return GrpcCriterion.Create.newBuilder()
            .setTag("Tag")
            .setName("Criterion")
            .setDescription("Description")
            .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
            .build()
    }

    @Test
    fun `When an admin user creates a project criterion, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()

        val criterion = DataBuilder.createExampleProjectCriterion(projectId = project.id)
        val request = getProjectCriterionRequest(project.id.toString())

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { criterionRepoMock.createCriterion(request, user.id) } returns criterion

        assertDoesNotThrow { mainService.createCriterion(request) }
    }

    @Test
    fun `When a project admin creates a project criterion, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

        val criterion = DataBuilder.createExampleProjectCriterion(projectId = project.id)
        val request = getProjectCriterionRequest(project.id.toString())

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { criterionRepoMock.createCriterion(request, user.id) } returns criterion

        assertDoesNotThrow { mainService.createCriterion(request) }
    }

    @Test
    fun `When a non project admin creates a project criterion, then an UnauthorizedException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        val request = getProjectCriterionRequest(project.id.toString())

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.createCriterion(request) }
    }

    @GrpcEnumSourceTest(
        ProjectStatus::class,
        excludes = ["PROJECT_STATUS_ACTIVE", "PROJECT_STATUS_ACTIVE_LOCKED", "PROJECT_STATUS_UNSPECIFIED"],
    )
    fun `When a project admin creates a project criterion for a non active project, then a FailedPreconditionException is thrown`(
        status: ProjectStatus,
    ) = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(
            status = status,
        )
        val projectAdmin = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

        val request = getProjectCriterionRequest(project.id.toString())

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectAdmin)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertThrows<FailedPreconditionException> { mainService.createCriterion(request) }
    }

    @Test
    fun `When an user creates a user criterion, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

        val criterion = DataBuilder.createExampleUserCriterion()
        val request = getUserCriterionRequest()

        mockCurrentUser(user)
        coEvery { criterionRepoMock.createCriterion(request, user.id) } returns criterion

        assertDoesNotThrow { mainService.createCriterion(request) }
    }
}
