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
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.utils.GrpcEnumSourceTest
import snowballr.CriterionOuterClass
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class CreateCriterionTest : MainServiceTest() {
    private fun getProjectCriterionRequest(projectId: String): CriterionOuterClass.Criterion.Create {
        return CriterionOuterClass.Criterion.Create.newBuilder()
            .setTag("Tag")
            .setName("Criterion")
            .setDescription("Description")
            .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
            .setProjectId(projectId)
            .build()
    }

    private fun getUserCriterionRequest(): CriterionOuterClass.Criterion.Create {
        return CriterionOuterClass.Criterion.Create.newBuilder()
            .setTag("Tag")
            .setName("Criterion")
            .setDescription("Description")
            .setCategory(CriterionCategory.CRITERION_CATEGORY_EXCLUSION)
            .build()
    }

    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        val request = CriterionOuterClass.Criterion.Create.getDefaultInstance()

        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.createCriterion(request) }
    }

    @Test
    fun `When an admin user creates a project criterion, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()

        val criterion = DataBuilder.createExampleProjectCriterion(projectId = project.id)
        val request = getProjectCriterionRequest(project.id.toString())

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(GrpcContext.getUserIdFromContext()) } returns user
        coEvery { criterionRepoMock.createCriterion(any(), any()) } returns criterion
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

        assertDoesNotThrow { mainService.createCriterion(request) }
    }

    @Test
    fun `When a project admin creates a project criterion, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

        val criterion = DataBuilder.createExampleProjectCriterion(projectId = project.id)
        val request = getProjectCriterionRequest(project.id.toString())

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(GrpcContext.getUserIdFromContext()) } returns user
        coEvery { criterionRepoMock.createCriterion(any(), any()) } returns criterion

        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)

        assertDoesNotThrow { mainService.createCriterion(request) }
    }

    @Test
    fun `When a non project admin creates a project criterion, then an unauthorized exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        val request = getProjectCriterionRequest(project.id.toString())

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(GrpcContext.getUserIdFromContext()) } returns user
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

        assertThrows<SnowballRException.UnauthorizedException.Single> { mainService.createCriterion(request) }
    }

    @GrpcEnumSourceTest(
        ProjectOuterClass.ProjectStatus::class,
        excludes = ["PROJECT_STATUS_ACTIVE", "PROJECT_STATUS_UNSPECIFIED"],
    )
    fun `When a project admin creates a project criterion for a non active project, then a failed precondition exception is thrown`(
        status: ProjectOuterClass.ProjectStatus,
    ) = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(
            status = status,
        )
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

        val request = getProjectCriterionRequest(project.id.toString())

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(GrpcContext.getUserIdFromContext()) } returns user
        coEvery { projectRepoMock.getProjectById(project.id) } returns project
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectMember)

        assertThrows<SnowballRException.FailedPreconditionException> { mainService.createCriterion(request) }
    }

    @Test
    fun `When an user creates a user criterion, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

        val criterion = DataBuilder.createExampleUserCriterion()
        val request = getUserCriterionRequest()

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(GrpcContext.getUserIdFromContext()) } returns user
        coEvery { criterionRepoMock.createCriterion(any(), user.id) } returns criterion

        assertDoesNotThrow { mainService.createCriterion(request) }
    }

    @Test
    fun `When an error occurs while a criterion is created, then an exception is thrown`() = runTest {
        val request = CriterionOuterClass.Criterion.Create.getDefaultInstance()
        val userId = UUID.randomUUID()
        val user = DataBuilder.createExampleUser(id = userId)

        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(GrpcContext.getUserIdFromContext()) } returns user
        coEvery { criterionRepoMock.createCriterion(any(), any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.createCriterion(request) }
    }

    @Test
    fun `When a criterion is correctly created, then no exception is thrown`() = runTest {
        val request = CriterionOuterClass.Criterion.Create.getDefaultInstance()
        val user = DataBuilder.createExampleUser(id = UUID.randomUUID())
        val criterion = DataBuilder.createExampleProjectCriterion()

        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(GrpcContext.getUserIdFromContext()) } returns user
        coEvery { criterionRepoMock.createCriterion(any(), any()) } returns criterion

        assertDoesNotThrow { mainService.createCriterion(request) }
    }
}
